package com.ethanstudio.snapsheet.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.ethanstudio.snapsheet.util.evictThumbnails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Lưu và đọc tài liệu. Ảnh trang và PDF nằm trong filesDir/docs/<id>/, chỉ app này đọc được. */
class DocRepository(private val context: Context, private val dao: DocDao, private val folderDao: FolderDao) {
    val docs: Flow<List<Doc>> = dao.observeAll()
    val folders: Flow<List<Folder>> = folderDao.observeAll()

    private val _pageVersion = MutableStateFlow(0L)

    /** Tăng sau mỗi lần ghi lại trang (sửa/thêm) để giao diện đọc lại ảnh. */
    val pageVersion: StateFlow<Long> = _pageVersion.asStateFlow()

    private val docsRoot: File get() = File(context.filesDir, "docs")

    fun observe(id: Long): Flow<Doc?> = dao.observe(id)

    fun dir(id: Long): File = File(context.filesDir, "docs/$id")

    fun pdfFile(id: Long): File = File(dir(id), "doc.pdf")

    /** Thư mục dựng bản mới khi sửa trang. */
    private fun stagingDir(id: Long): File = File(docsRoot, "$id.new")

    /** Bản cũ giữ tạm trong lúc thay bằng bản mới. */
    private fun backupDir(id: Long): File = File(docsRoot, "$id.old")

    fun pageFiles(doc: Doc): List<File> = (1..doc.pageCount).map { File(dir(doc.id), "page_$it.jpg") }

    /** Lưu kết quả của trình quét: ảnh từng trang và (nếu có) file PDF do ML Kit tạo sẵn. */
    suspend fun saveScan(pageUris: List<Uri>, pdfUri: Uri?, name: String): Long =
        create(name, pageUris.size) { dir ->
            pageUris.forEachIndexed { i, uri -> copy(uri, File(dir, "page_${i + 1}.jpg")) }
            if (pdfUri != null) {
                copy(pdfUri, File(dir, "doc.pdf"))
            } else {
                PdfBuilder.build((1..pageUris.size).map { File(dir, "page_$it.jpg") }, File(dir, "doc.pdf"))
            }
        }

    /** Lưu ảnh chọn từ thư viện: thu nhỏ, xoay đúng chiều, rồi ghép thành PDF. */
    suspend fun saveImages(uris: List<Uri>, name: String): Long =
        create(name, uris.size) { dir ->
            uris.forEachIndexed { i, uri -> importImage(uri, File(dir, "page_${i + 1}.jpg")) }
            PdfBuilder.build((1..uris.size).map { File(dir, "page_$it.jpg") }, File(dir, "doc.pdf"))
        }

    suspend fun rename(id: Long, doc: Doc, newName: String) {
        dao.update(doc.copy(id = id, name = newName))
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dao.delete(id)
        dir(id).deleteRecursively()
    }

    /** Sắp lại và/hoặc bỏ bớt trang. [order]: số trang gốc theo thứ tự mới. */
    suspend fun editPages(id: Long, order: List<Int>) {
        val doc = dao.get(id) ?: throw IOException("Document $id not found")
        require(isValidOrder(order, doc.pageCount)) { "Invalid page order" }
        rewritePages(id, order.map { PageSource.Existing(File(dir(id), "page_$it.jpg")) })
    }

    /** Nối các trang vừa quét vào cuối tài liệu. Trả về số trang đã thêm. */
    suspend fun addScannedPages(id: Long, uris: List<Uri>): Int = addPages(id, uris) { PageSource.Scanned(it) }

    /** Nối các ảnh chọn từ thư viện vào cuối tài liệu. Trả về số trang đã thêm. */
    suspend fun addImages(id: Long, uris: List<Uri>): Int = addPages(id, uris) { PageSource.Photo(it) }

    private suspend fun addPages(id: Long, uris: List<Uri>, source: (Uri) -> PageSource): Int {
        val doc = dao.get(id) ?: throw IOException("Document $id not found")
        // Giới hạn theo gói (5 / 30) đã được ViewModel cắt trước; ở đây chỉ chặn mức tối đa tuyệt đối.
        val added = uris.take((FreeLimits.PRO_PAGES - doc.pageCount).coerceAtLeast(0))
        if (added.isEmpty()) return 0
        rewritePages(id, pageFiles(doc).map { PageSource.Existing(it) } + added.map(source))
        return added.size
    }

    /** Gộp các tài liệu (theo đúng thứ tự [ids]) thành một tài liệu mới. Bản gốc giữ nguyên. Trả về id mới. */
    suspend fun merge(ids: List<Long>, name: String): Long {
        val parts = withContext(Dispatchers.IO) { ids.mapNotNull { dao.get(it) } }
        if (parts.size != ids.size) throw IOException("Some documents are gone")
        val check = checkMerge(parts)
        require(check is MergeCheck.Ok) { "Cannot merge: $check" }
        return create(name, check.total) { dir ->
            rethrowOutOfMemory {
                var n = 0
                parts.forEach { doc ->
                    pageFiles(doc).forEach { page ->
                        n++
                        val dest = File(dir, "page_$n.jpg")
                        page.copyTo(dest, overwrite = true)
                        ensureSupportedJpeg(dest)
                    }
                }
                writePdf((1..n).map { File(dir, "page_$it.jpg") }, File(dir, "doc.pdf"))
            }
        }
    }

    /**
     * Bản PDF nén để chia sẻ, ghi vào docs/<id>/compressed.pdf (ghi đè mỗi lần). Tài liệu đã lưu không đổi.
     * [CompressLevel.ORIGINAL]: trả về PDF gốc.
     */
    suspend fun compressed(id: Long, level: CompressLevel): File {
        if (level == CompressLevel.ORIGINAL) return pdfFile(id)
        return withContext(Dispatchers.IO) {
            val doc = dao.get(id) ?: throw IOException("Document $id not found")
            val work = File(context.cacheDir, "compress/$id")
            val out = File(dir(id), "compressed.pdf")
            val tmp = File(dir(id), "compressed.pdf.tmp")
            try {
                work.deleteRecursively()
                if (!work.mkdirs()) throw IOException("Cannot create ${work.name}")
                val pages = pageFiles(doc).mapIndexed { i, page ->
                    ensureActive()
                    File(work, "page_${i + 1}.jpg").also { shrink(page, it, level) }
                }
                tmp.outputStream().buffered().use { JpegPdfWriter.write(pages.map { f -> JpegSource { f.readBytes() } }, it) }
                out.delete()
                if (!tmp.renameTo(out)) throw IOException("Cannot save compressed PDF")
                out
            } catch (e: OutOfMemoryError) {
                throw IOException("Out of memory", e)
            } finally {
                work.deleteRecursively()
                tmp.delete()
            }
        }
    }

    /** Xóa nhiều tài liệu. Lỗi ở một tài liệu thì vẫn xóa tiếp các tài liệu còn lại, rồi báo lỗi đầu tiên. */
    suspend fun deleteMany(ids: List<Long>) {
        var first: Exception? = null
        ids.forEach { id ->
            try {
                delete(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (first == null) first = e
            }
        }
        first?.let { throw it }
    }

    /** Chuyển tài liệu vào thư mục [folderId]; null = bỏ khỏi thư mục. */
    suspend fun moveToFolder(ids: List<Long>, folderId: Long?) {
        if (ids.isNotEmpty()) dao.setFolder(ids, folderId)
    }

    suspend fun createFolder(name: String): Long =
        folderDao.insert(Folder(name = name, createdAt = System.currentTimeMillis()))

    suspend fun renameFolder(id: Long, name: String) = folderDao.rename(id, name)

    /** Xóa thư mục; tài liệu bên trong được giữ lại (chỉ gỡ khỏi thư mục). */
    suspend fun deleteFolder(id: Long) = folderDao.deleteKeepDocs(id)

    /**
     * Gọi khi mở app: dọn hoặc hoàn tất các lần ghi trang bị ngắt giữa chừng (tắt app, hết pin).
     * Mọi lỗi ở đây đều bỏ qua để không làm crash lúc mở app.
     */
    suspend fun recoverPendingEdits() = withContext(Dispatchers.IO) {
        runCatching { File(context.cacheDir, "compress").deleteRecursively() }
        val names = runCatching { docsRoot.list() }.getOrNull() ?: return@withContext
        val ids = names.mapNotNull { PENDING_DIR.matchEntire(it)?.groupValues?.get(1)?.toLongOrNull() }.toSet()
        ids.forEach { id ->
            try {
                recover(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Bỏ qua: lần mở app sau sẽ thử lại.
            }
        }
    }

    private suspend fun recover(id: Long) {
        val live = dir(id)
        val staging = stagingDir(id)
        val backup = backupDir(id)
        if (dao.get(id) == null) {
            // Tài liệu đã bị xóa: chỉ dọn thư mục tạm.
            staging.deleteRecursively()
            backup.deleteRecursively()
            return
        }
        when (recoveryAction(live.isDirectory, staging.isDirectory, backup.isDirectory)) {
            Recovery.NOTHING -> Unit
            Recovery.DELETE_NEW -> staging.deleteRecursively()
            Recovery.FINISH_COMMIT -> {
                backup.deleteRecursively()
                staging.deleteRecursively()
                syncPageCount(id)
            }
            Recovery.PROMOTE_NEW -> if (staging.renameTo(live)) {
                backup.deleteRecursively()
                syncPageCount(id)
            }
            Recovery.RESTORE_OLD -> if (backup.renameTo(live)) syncPageCount(id)
        }
        evictThumbnails(live)
    }

    /** Cập nhật số trang theo các file page_1.jpg, page_2.jpg… liên tiếp thực có. */
    private suspend fun syncPageCount(id: Long) {
        var count = 0
        while (File(dir(id), "page_${count + 1}.jpg").isFile) count++
        if (count > 0) dao.setPageCount(id, count)
    }

    /** Nguồn của một trang khi dựng lại tài liệu. */
    private sealed interface PageSource {
        data class Existing(val file: File) : PageSource
        data class Scanned(val uri: Uri) : PageSource
        data class Photo(val uri: Uri) : PageSource
    }

    /**
     * Ghi lại toàn bộ trang của tài liệu một cách an toàn: dựng bản mới trong docs/<id>.new, xong hết mới thay
     * bản cũ. Lỗi (kể cả hết bộ nhớ, bị hủy) trước bước thay thì bản gốc không bị đụng tới.
     */
    private suspend fun rewritePages(id: Long, sources: List<PageSource>) = withContext(Dispatchers.IO) {
        require(sources.size in 1..FreeLimits.PRO_PAGES) { "Invalid page count" }
        dao.get(id) ?: throw IOException("Document $id not found")
        val staging = stagingDir(id)
        try {
            staging.deleteRecursively()
            if (!staging.mkdirs()) throw IOException("Cannot create ${staging.name}")
            sources.forEachIndexed { i, source ->
                ensureActive()
                val dest = File(staging, "page_${i + 1}.jpg")
                when (source) {
                    is PageSource.Existing -> source.file.copyTo(dest, overwrite = true)
                    is PageSource.Scanned -> copy(source.uri, dest)
                    is PageSource.Photo -> importImage(source.uri, dest)
                }
                ensureSupportedJpeg(dest)
            }
            ensureActive()
            writePdf(sources.indices.map { File(staging, "page_${it + 1}.jpg") }, File(staging, "doc.pdf"))
        } catch (e: OutOfMemoryError) {
            staging.deleteRecursively()
            throw IOException("Out of memory", e)
        } catch (e: Throwable) {
            staging.deleteRecursively()
            throw e
        }
        withContext(NonCancellable) { commit(id, staging, sources.size) }
    }

    /** Thay bản cũ bằng bản mới: live → old, new → live, cập nhật số trang, xóa old. */
    private suspend fun commit(id: Long, staging: File, pageCount: Int) {
        val live = dir(id)
        val backup = backupDir(id)
        backup.deleteRecursively()
        if (!live.renameTo(backup)) {
            staging.deleteRecursively()
            throw IOException("Cannot move the original aside")
        }
        if (!staging.renameTo(live)) {
            backup.renameTo(live)
            staging.deleteRecursively()
            throw IOException("Cannot move the new pages in")
        }
        try {
            dao.setPageCount(id, pageCount)
        } catch (e: Exception) {
            // Không cập nhật được số trang: trả lại bản cũ cho khớp với cơ sở dữ liệu.
            if (live.renameTo(staging)) backup.renameTo(live)
            staging.deleteRecursively()
            throw e
        }
        backup.deleteRecursively()
        evictThumbnails(live)
        _pageVersion.value++
    }

    /**
     * Bảo đảm file là JPEG mà bộ ghi PDF nhúng thẳng được (xám hoặc màu). PNG, CMYK… thì giải mã rồi ghi lại JPEG.
     */
    private fun ensureSupportedJpeg(file: File) {
        val info = readJpegInfo(file.readBytes())
        if (info != null && (info.components == 1 || info.components == 3)) return
        val bitmap = decodeSafely(file, MAX_SIDE)
        try {
            file.outputStream().use { if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)) throw IOException("Cannot encode ${file.name}") }
        } finally {
            bitmap.recycle()
        }
    }

    /** Thu nhỏ một trang theo mức nén rồi ghi JPEG vào [dest]. */
    private fun shrink(page: File, dest: File, level: CompressLevel) {
        val decoded = decodeSafely(page, level.maxSide)
        val (w, h) = scaledSize(decoded.width, decoded.height, level.maxSide)
        val bitmap = if (w == decoded.width && h == decoded.height) decoded else {
            Bitmap.createScaledBitmap(decoded, w, h, true).also { if (it !== decoded) decoded.recycle() }
        }
        try {
            dest.outputStream().use { if (!bitmap.compress(Bitmap.CompressFormat.JPEG, level.quality, it)) throw IOException("Cannot encode ${page.name}") }
        } finally {
            bitmap.recycle()
        }
    }

    /** Giải mã ảnh, cạnh dài xấp xỉ [maxSide]. Hết bộ nhớ thì giảm kích thước và thử lại (tối đa 3 lần). */
    private fun decodeSafely(file: File, maxSide: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Cannot read ${file.name}")
        var sample = sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight), maxSide)
        repeat(DECODE_ATTEMPTS) {
            try {
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                return BitmapFactory.decodeFile(file.path, options) ?: throw IOException("Cannot decode ${file.name}")
            } catch (e: OutOfMemoryError) {
                sample *= 2
            }
        }
        throw IOException("Out of memory decoding ${file.name}")
    }

    /** Ghép các trang JPEG thành PDF: ghi ra file tạm rồi mới đổi tên, lỗi giữa chừng không để lại file hỏng. */
    private fun writePdf(pages: List<File>, out: File) {
        val tmp = File(out.parentFile, "${out.name}.tmp")
        try {
            tmp.outputStream().buffered().use { JpegPdfWriter.write(pages.map { f -> JpegSource { f.readBytes() } }, it) }
            out.delete()
            if (!tmp.renameTo(out)) throw IOException("Cannot save ${out.name}")
        } finally {
            tmp.delete()
        }
    }

    /** Hết bộ nhớ trong [block] thì báo lỗi thường (IOException) thay vì làm crash app. */
    private inline fun <T> rethrowOutOfMemory(block: () -> T): T =
        try {
            block()
        } catch (e: OutOfMemoryError) {
            throw IOException("Out of memory", e)
        }

    private suspend fun create(name: String, pageCount: Int, fill: (File) -> Unit): Long =
        withContext(Dispatchers.IO) {
            require(pageCount > 0) { "No pages" }
            val id = dao.insert(Doc(name = name, createdAt = System.currentTimeMillis(), pageCount = pageCount))
            val dir = dir(id).apply { mkdirs() }
            try {
                fill(dir)
                id
            } catch (e: Exception) {
                dir.deleteRecursively()
                dao.delete(id)
                throw e
            }
        }

    private fun copy(from: Uri, to: File) {
        val input = context.contentResolver.openInputStream(from) ?: error("Cannot open $from")
        input.use { src -> to.outputStream().use { dst -> src.copyTo(dst) } }
    }

    private fun importImage(uri: Uri, dest: File) {
        val resolver = context.contentResolver
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight), MAX_SIDE)
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Cannot decode $uri")
        val degrees = exifDegrees(orientation)
        val bitmap = if (degrees == 0) decoded else {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also { decoded.recycle() }
        }
        try {
            dest.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val MAX_SIDE = 2000
        /** Lần giải mã đầu + tối đa 3 lần thử lại với ảnh nhỏ hơn. */
        const val DECODE_ATTEMPTS = 4
        val PENDING_DIR = Regex("""(\d+)\.(new|old)""")
    }
}
