package com.ethanstudio.snapsheet.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/** Lưu và đọc tài liệu. Ảnh trang và PDF nằm trong filesDir/docs/<id>/, chỉ app này đọc được. */
class DocRepository(private val context: Context, private val dao: DocDao) {
    val docs: Flow<List<Doc>> = dao.observeAll()

    fun observe(id: Long): Flow<Doc?> = dao.observe(id)

    fun dir(id: Long): File = File(context.filesDir, "docs/$id")

    fun pdfFile(id: Long): File = File(dir(id), "doc.pdf")

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
    }
}
