# Kế hoạch nhóm A (đang làm dở, lưu lại để làm tiếp)

Trạng thái 2026-10-01: Ethan bảo dừng giữa chặng 2 (coder). Đã có một phần A1 (Room v2, JpegPdfWriter, PageOps, Compress, EditRecovery, Folder) và chuỗi; chưa xong A2/B. Làm tiếp: chép file này về .bangiao/ke-hoach.md, yêu cầu về .bangiao/yeu-cau.md, rồi chạy lại coder.

---
# Yêu cầu

- **Nguyên văn (Ethan):** "làm hết nhóm A" + "Đề xuất" (đồng ý chia Miễn phí / Pro theo cột Đề xuất).
- Ngữ cảnh: Ethan muốn GIỮ giao diện V1 hiện tại (trắng + gradient xanh), chỉ học CHỨC NĂNG từ app tham khảo (CamScanner). Không copy giao diện/nhận diện của app đó.
- **Slug:** snap-sheet · **Nhánh:** claude/project-thread-yqbwdq · **Commit bắt đầu:** 13583c65b6eb6ebdc285a99a79c250f62b8473ff · **Ngày:** 2026-10-01

## Nhóm A (11 chức năng) — nhạc trưởng chia 2 đợt để mỗi đợt CI xanh được

| # | Chức năng | Gói | Đợt |
|---|---|---|---|
| 1 | Chọn nhiều tài liệu (chia sẻ / xóa / chuyển thư mục cùng lúc) | Miễn phí | 1 |
| 2 | Gộp nhiều tài liệu thành 1 PDF (tạo tài liệu mới, giữ bản gốc) | Pro | 1 |
| 3 | Sắp xếp lại thứ tự trang, xóa trang trong tài liệu | Miễn phí | 1 |
| 4 | Thêm trang vào tài liệu cũ (quét tiếp / thêm ảnh) — vẫn tôn trọng giới hạn 5 trang free / 30 trang Pro | Miễn phí | 1 |
| 8 | In tài liệu (PrintManager của Android, không quyền) | Miễn phí | 1 |
| 10 | Thư mục (tạo, đổi tên, xóa thư mục; chuyển tài liệu vào thư mục) | Miễn phí | 1 |
| 11 | Nén PDF (chọn mức: nhỏ / vừa / gốc; chia sẻ bản nén) | Miễn phí | 1 |
| 5 | Ký tên (vẽ chữ ký, lưu, dán lên trang, kéo/chỉnh cỡ) | Pro | 2 |
| 6 | Đóng dấu chữ mờ (watermark chữ) | Pro | 2 |
| 7 | Nhập file PDF có sẵn (SAF, PdfRenderer) | Miễn phí | 2 |
| 9 | Quét mã QR/mã vạch (Google code scanner, không xin quyền camera) | Miễn phí | 2 |

**Dây chuyền này làm ĐỢT 1** (1, 2, 3, 4, 8, 10, 11). Đợt 2 chạy dây chuyền riêng ngay sau.

## Ràng buộc
- Giao diện theo phong cách V1 đã có trong app (dùng lại component trong ui/common, Gradients, icon cùng kiểu nét 2px). Không đổi bố cục các màn đã duyệt ngoài phần cần thêm (thanh chọn nhiều, menu trang, nút thư mục, nút in/nén/gộp).
- Pro: gộp PDF là Pro (người miễn phí bấm → màn mua). Các mục còn lại miễn phí. Không đổi giới hạn free (5 trang/tài liệu, 3 lượt xuất/ngày, không OCR). In và chia sẻ bản nén tính là một lượt xuất như các kiểu xuất khác (planner xác nhận và ghi rõ).
- Thư mục cần đổi schema Room (version 1 → 2): BẮT BUỘC có Migration (không dùng fallbackToDestructiveMigration — mất dữ liệu người dùng). Viết test cho logic thuần liên quan.
- Chạy offline, không thêm quyền, không thêm SDK bên thứ ba. Không đụng billing product ID, Firebase, CI, keystore.
- Chuỗi mới đủ 12 locale. Không chạy ./gradlew. Không có chữ giữ chỗ.
- Mọi thao tác ghi file (gộp, nén, sắp trang, xóa trang) phải an toàn: ghi ra file tạm rồi mới thay, lỗi giữa chừng không làm hỏng tài liệu gốc; không crash khi hết bộ nhớ (bắt OutOfMemoryError khi giải mã ảnh, giảm kích thước).

---
# Kế hoạch · snap-sheet · ĐỢT 1 (chọn nhiều, gộp PDF, sắp/xóa trang, thêm trang, in, thư mục, nén)

## CÂU HỎI CẦN ETHAN
Không có

## GIẢ ĐỊNH
1. **Lượt xuất (bản miễn phí, 3/ngày):** In = 1 lượt. Chia sẻ bản nén (kể cả mức "Gốc") = 1 lượt. Chia sẻ nhiều tài liệu cùng lúc = **mỗi tài liệu 1 lượt** (chia sẻ 3 tài liệu tốn 3 lượt; không đủ lượt thì chặn cả lần và mở màn mua). Lý do: nếu tính 1 lượt cho cả nhóm thì người dùng lách được giới hạn đã chốt. Gộp PDF (Pro), sắp/xóa/thêm trang, chuyển thư mục: không tính lượt (không đưa file ra khỏi app). Hủy hộp thoại in/chia sẻ vẫn mất lượt (giống hành vi hiện có).
2. **Nén không dùng `PdfDocument`.** `PdfDocument` của Android nhúng ảnh dạng không nén mất dữ liệu (deflate), nên PDF thường TO hơn ảnh JPEG gốc (ví dụ JPEG 396 KB → PDF 2,9 MB). Giảm chất lượng JPEG rồi vẽ lại bằng `PdfDocument` gần như vô ích. Thay vào đó viết một bộ ghi PDF nhỏ bằng Kotlin thuần (`JpegPdfWriter`) nhúng thẳng byte JPEG (`/DCTDecode`). Bộ ghi này dùng cho cả nén, dựng lại PDF sau khi sắp/xóa/thêm trang, và gộp. Không thêm thư viện. `PdfBuilder` cũ giữ nguyên cho luồng nhập ảnh hiện có.
3. **Sắp trang bằng nút Lên/Xuống**, không kéo thả. Kéo thả trong Compose cần tự viết xử lý cử chỉ, dễ lỗi build và khó dùng với cỡ chữ lớn và TalkBack.
4. **Thứ tự gộp = thứ tự người dùng chạm chọn.** Vòng tròn chọn hiện số thứ tự (1, 2, 3…). Tên tài liệu gộp: `defaultDocName(<merge_default_name>, now)`, ví dụ "Merged 2026-10-01 09.37". Tài liệu gộp tối đa 30 trang (`PRO_PAGES`). Bản gốc giữ nguyên.
5. **Thư mục chỉ một cấp**, mỗi tài liệu thuộc 0 hoặc 1 thư mục. Tab Files có chip "All" (mọi tài liệu, kể cả tài liệu trong thư mục) và mỗi thư mục một chip. Home và các danh sách khác không đổi. Xóa thư mục thì KHÔNG xóa tài liệu (chỉ gỡ khỏi thư mục). Tên thư mục: cắt khoảng trắng, tối đa 40 ký tự, không được trùng (không phân biệt hoa thường).
6. **Không xóa được trang cuối cùng** (tài liệu phải có ≥ 1 trang). Muốn bỏ hết thì xóa cả tài liệu.
7. **Thêm trang** luôn nối vào cuối tài liệu. Số trang được thêm = giới hạn (5 free / 30 Pro) − số trang hiện có. Nếu = 0: bản free mở màn mua, bản Pro báo đã tối đa. Tài liệu cũ đang nhiều hơn 5 trang (Pro đã hết hạn) vẫn sắp/xóa trang và nén được, chỉ không thêm trang được.
8. **Mức nén:** Nhỏ = cạnh dài 1200 px, JPEG 60. Vừa = 1800 px, JPEG 75 (mặc định). Gốc = gửi nguyên `doc.pdf`. Ảnh chỉ thu nhỏ, không phóng to. Nếu file nén không nhỏ hơn bản gốc thì gửi bản gốc và báo `compress_no_gain`.
9. **File nén** ghi vào `filesDir/docs/<id>/compressed.pdf` (FileProvider đã chia sẻ được `docs/`, không cần sửa `file_paths.xml`). File này bị ghi đè mỗi lần nén và tự mất khi sửa trang hoặc xóa tài liệu.
10. **Lối vào gộp:** thanh chọn nhiều trong Files, và thêm một thẻ "Merge PDFs" (PRO) trong tab Tools (bấm vào thì chuyển sang Files ở chế độ chọn). Đây là "nút gộp" mà yêu cầu nêu; không thêm gì khác vào Tools.

## Mục tiêu
Trong tab Files, người dùng tạo thư mục, chọn nhiều tài liệu để chia sẻ, xóa, chuyển thư mục hoặc gộp thành một PDF (Pro). Trong màn tài liệu, người dùng thêm trang (quét tiếp hoặc thêm ảnh), sắp lại hoặc xóa trang, in, nén rồi chia sẻ bản nhỏ, và chuyển tài liệu vào thư mục. Dữ liệu cũ còn nguyên sau khi cập nhật, và lỗi giữa chừng không làm hỏng tài liệu gốc.

## Thứ tự làm (nếu một lượt coder không đủ thì cắt ở ranh giới A | B)
- **A1 · Dữ liệu (bắt buộc làm trước):** Room v2 + Migration, Folder, `JpegInfo`, `JpegPdfWriter`, `PageOps`, `Compress`, `EditRecovery`, các hàm Repository, toàn bộ unit test.
- **A2 · Màn tài liệu:** hàng nút thứ hai (Thêm trang, Sửa trang, In, Nén), màn Sửa trang, nút Chuyển thư mục trên đầu trang, `MoveToFolderSheet`, `FolderNameDialog`.
- **B · Files + Tools:** chip thư mục (tạo/đổi tên/xóa), chế độ chọn nhiều + thanh hành động (Chia sẻ, Chuyển, Gộp, Xóa), thẻ Merge trong Tools.
- Chuỗi: thêm đủ 12 ngôn ngữ ngay trong lượt A (kể cả chuỗi của B) để CI không báo thiếu bản dịch.

## File cần tạo hoặc sửa
Gốc code: `apps/snap-sheet/app/src/main/java/com/ethanstudio/snapsheet/` (viết tắt `…/`). Gốc test: `apps/snap-sheet/app/src/test/java/com/ethanstudio/snapsheet/` (viết tắt `test/`).

**Dữ liệu**
- `…/data/Doc.kt` — sửa: thêm `val folderId: Long? = null` ở CUỐI constructor (để không làm vỡ các lời gọi theo vị trí như `Doc(1, "x", 3, 2)` trong test); thêm `filterByFolder`.
- `…/data/Folder.kt` — tạo: entity `Folder` + `FolderNameCheck` + `checkFolderName`.
- `…/data/FolderDao.kt` — tạo.
- `…/data/DocDao.kt` — sửa: thêm `get`, `getAll`, `setPageCount`, `setFolder`.
- `…/data/SchemaSql.kt` — tạo: hằng SQL của migration (không import Room, để test JVM gọn).
- `…/data/AppDatabase.kt` — sửa: `version = 2`, thêm `Folder::class`, `folderDao()`, `MIGRATION_1_2`, `.addMigrations(MIGRATION_1_2)`. TUYỆT ĐỐI không gọi `fallbackToDestructiveMigration`.
- `…/data/JpegInfo.kt` — tạo: đọc kích thước và số kênh màu từ header JPEG (thuần Kotlin).
- `…/data/JpegPdfWriter.kt` — tạo: ghi PDF nhúng thẳng JPEG (thuần Kotlin, chỉ dùng `java.io`).
- `…/data/PageOps.kt` — tạo: hàm thuần cho sắp/xóa trang, chọn nhiều, kiểm tra gộp.
- `…/data/Compress.kt` — tạo: `CompressLevel`, `scaledSize`, `shouldShareOriginal`.
- `…/data/EditRecovery.kt` — tạo: bảng quyết định khôi phục khi app bị tắt giữa lúc ghi.
- `…/data/FreeLimits.kt` — sửa: thêm `pagesCanAdd`, `canConsume`, `consumeMany`.
- `…/data/ProStore.kt` — sửa: thêm `tryConsumeExports(count, today)`; `tryConsumeExport(today)` gọi lại hàm này với `count = 1`.
- `…/data/DocRepository.kt` — sửa: nhận thêm `FolderDao`; thêm các hàm sửa trang, thêm trang, gộp, nén, thư mục, khôi phục (xem mục dưới). Giữ nguyên `saveScan`, `saveImages`, `rename`, `delete`.
- `…/SnapSheetApp.kt` — sửa: tạo `AppDatabase` một lần, truyền `docDao()` và `folderDao()` vào `DocRepository`; trong `onCreate` chạy `appScope.launch(Dispatchers.IO) { docs.recoverPendingEdits() }`.

**Tiện ích**
- `…/util/Print.kt` — tạo: `PdfFilePrintAdapter`, `Context.printPdf`, `printJobFileName`.
- `…/util/Activities.kt` — sửa: thêm `evictThumbnails(dir: File)` để xóa ảnh trong bộ nhớ đệm có đường dẫn nằm dưới `dir` (dùng `bitmapCache.snapshot().keys`).

**Giao diện**
- `…/ui/common/Components.kt` — sửa: `PageThumbnail` thêm tham số `stamp: Long = 0L` và dùng làm khóa của `produceState(null, file, stamp)`; `DocRow` thêm tham số cho chế độ chọn (chữ ký ở mục dưới); thêm `BusyOverlay(text: String)` (chuyển nguyên `SavingOverlay` từ AppRoot sang, nhận chữ làm tham số).
- `…/ui/common/ScanChoiceSheet.kt` — tạo: chuyển `ScanSheet` + `SheetCard` từ `AppRoot.kt` sang đây và để public. `ScanChoiceSheet` nhận thêm `title`, `cameraSub`, `photosSub` (String). AppRoot gọi với `sheet_start` / `sheet_camera_sub` / `sheet_photos_sub` nên giao diện cũ không đổi.
- `…/ui/common/FolderSheets.kt` — tạo: `MoveToFolderSheet`, `FolderNameDialog`.
- `…/ui/doc/DocViewModel.kt` — sửa: thêm in, nén, thêm trang, chuyển thư mục, `working`, `revision`, `folders`.
- `…/ui/doc/DocScreen.kt` — sửa: nút thư mục trên header, hàng nút thứ hai, sheet thêm trang, sheet nén, sheet chuyển thư mục, overlay.
- `…/ui/doc/PagesViewModel.kt` — tạo.
- `…/ui/doc/PagesScreen.kt` — tạo: màn Sửa trang.
- `…/ui/main/MainViewModel.kt` — sửa: thư mục, bộ lọc thư mục, chọn nhiều, chia sẻ/xóa/chuyển/gộp.
- `…/ui/files/FilesScreen.kt` — sửa: chip thư mục, nút Select, chế độ chọn, thanh hành động, các hộp thoại.
- `…/ui/tools/ToolsScreen.kt` — sửa: thêm `ToolTile` "Merge PDFs" (PRO khi chưa Pro), tham số `onMerge`.
- `…/ui/common/ViewModels.kt` — sửa: thêm `initializer { PagesViewModel(app().docs, createSavedStateHandle()) }`.
- `…/ui/AppRoot.kt` — sửa: route `pages/{id}`, tham số mới cho DocScreen/FilesScreen/ToolsScreen, xử lý `MainEvent` mới, bỏ chọn khi rời tab Files, dùng `ScanChoiceSheet` và `BusyOverlay`.

**Tài nguyên**
- `apps/snap-sheet/app/src/main/res/drawable/ic_print.xml`, `ic_compress.xml`, `ic_arrow_up.xml`, `ic_arrow_down.xml`, `ic_merge.xml` — tạo. Viewport 24, `strokeWidth 2`, `strokeLineCap/Join round`, `fillColor #00000000`, `strokeColor #FF000000` giống `ic_trash.xml`. pathData:
  - `ic_arrow_up`: `M12 19V5M6 11l6-6 6 6`
  - `ic_arrow_down`: `M12 5v14M6 13l6 6 6-6`
  - `ic_print`: `M7 9V3h10v6M7 17H5a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-2M7 14h10v7H7z`
  - `ic_compress`: `M4 14h6v6M20 10h-6V4M14 10l7-7M3 21l7-7`
  - `ic_merge`: `M6 3v5a6 6 0 0 0 6 6v7M18 3v5a6 6 0 0 1-6 6M9 18l3 3 3-3`
- `apps/snap-sheet/app/src/main/res/values*/strings.xml` (12 file: values, -vi, -es, -pt-rBR, -fr, -de, -in, -ru, -tr, -ja, -ko, -hi) — thêm chuỗi ở mục "Chữ hiển thị".

**Test (mới):** `test/data/PageOpsTest.kt`, `test/data/JpegInfoTest.kt`, `test/data/JpegPdfWriterTest.kt`, `test/data/CompressTest.kt`, `test/data/FolderTest.kt`, `test/data/EditRecoveryTest.kt`, `test/data/SchemaSqlTest.kt`, `test/util/PrintNameTest.kt`; sửa `test/data/FreeLimitsTest.kt` (thêm ca).

**Hồ sơ:** `apps/snap-sheet/APP.md` — cập nhật Màn hình (Sửa trang, thư mục, chọn nhiều), Dữ liệu (bảng `folders`, cột `docs.folderId`, schema v2), Nhật ký quyết định (các giả định 1–3 ở trên), "Để sau" bỏ "thư mục".

## Hàm, lớp, dữ liệu

### Room v1 → v2
```kotlin
// data/Folder.kt
@Entity(tableName = "folders")
data class Folder(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val createdAt: Long)

sealed interface FolderNameCheck { data class Ok(val name: String) : FolderNameCheck; data object Empty : FolderNameCheck; data object Taken : FolderNameCheck }
const val FOLDER_NAME_MAX = 40
/** trim, cắt còn 40 ký tự; rỗng → Empty; trùng tên (ignoreCase) với thư mục KHÁC [editingId] → Taken. */
fun checkFolderName(input: String, folders: List<Folder>, editingId: Long? = null): FolderNameCheck

// data/Doc.kt
data class Doc(..., val pageCount: Int, val folderId: Long? = null)   // KHÔNG thêm @ColumnInfo(defaultValue), KHÔNG @Index, KHÔNG ForeignKey
/** null = tất cả; khác null = chỉ tài liệu có folderId bằng giá trị đó. */
fun filterByFolder(docs: List<Doc>, folderId: Long?): List<Doc>

// data/SchemaSql.kt  (không import gì của Room)
val MIGRATION_1_2_SQL: List<String> = listOf(
    "CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
    "ALTER TABLE `docs` ADD COLUMN `folderId` INTEGER",
)

// data/AppDatabase.kt
@Database(entities = [Doc::class, Folder::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun docDao(): DocDao
    abstract fun folderDao(): FolderDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {           // androidx.room.migration.Migration
            override fun migrate(db: SupportSQLiteDatabase) {     // androidx.sqlite.db.SupportSQLiteDatabase
                MIGRATION_1_2_SQL.forEach(db::execSQL)
            }
        }
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "snapsheet.db").addMigrations(MIGRATION_1_2).build()
    }
}
```
Ghi chú: cột `folderId` thêm bằng `ALTER TABLE … ADD COLUMN folderId INTEGER` (cho phép null, không default) khớp với `Long? = null` không chú thích, nên Room kiểm schema sẽ qua. Dữ liệu cũ: mọi tài liệu có `folderId = NULL`, tức nằm ở "All".

```kotlin
// data/FolderDao.kt
@Dao
abstract class FolderDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE") abstract fun observeAll(): Flow<List<Folder>>
    @Insert abstract suspend fun insert(folder: Folder): Long
    @Query("UPDATE folders SET name = :name WHERE id = :id") abstract suspend fun rename(id: Long, name: String)
    @Query("UPDATE docs SET folderId = NULL WHERE folderId = :id") abstract suspend fun clearDocs(id: Long)
    @Query("DELETE FROM folders WHERE id = :id") abstract suspend fun deleteRow(id: Long)
    @Transaction open suspend fun deleteKeepDocs(id: Long) { clearDocs(id); deleteRow(id) }
}

// data/DocDao.kt (thêm)
@Query("SELECT * FROM docs WHERE id = :id") suspend fun get(id: Long): Doc?
@Query("SELECT * FROM docs") suspend fun getAll(): List<Doc>
@Query("UPDATE docs SET pageCount = :count WHERE id = :id") suspend fun setPageCount(id: Long, count: Int)
@Query("UPDATE docs SET folderId = :folderId WHERE id IN (:ids)") suspend fun setFolder(ids: List<Long>, folderId: Long?)
```
Dùng các câu `UPDATE` có đích rõ ràng (không dùng `dao.update(doc.copy(...))`) để đổi số trang không ghi đè thư mục và ngược lại.

### Hàm thuần
```kotlin
// data/FreeLimits.kt (thêm)
/** Số trang còn được thêm; không âm. */
fun pagesCanAdd(isPro: Boolean, current: Int): Int = (pageLimit(isPro) - current).coerceAtLeast(0)
/** Còn đủ [count] lượt hôm nay không (Pro: luôn true; count <= 0: true). */
fun canConsume(isPro: Boolean, usage: Usage, today: Long, count: Int): Boolean
/** Ghi nhận [count] lượt; sang ngày mới thì đếm lại từ count. */
fun consumeMany(usage: Usage, today: Long, count: Int): Usage

// data/ProStore.kt
suspend fun tryConsumeExports(count: Int, today: Long): Boolean   // nguyên tử trong một store.edit, dùng canConsume/consumeMany
suspend fun tryConsumeExport(today: Long): Boolean = tryConsumeExports(1, today)

// data/PageOps.kt
/** order: danh sách số trang GỐC (1-based) theo thứ tự mới. */
fun moveUp(order: List<Int>, index: Int): List<Int>      // index 0 hoặc ngoài khoảng → trả nguyên
fun moveDown(order: List<Int>, index: Int): List<Int>    // index cuối hoặc ngoài khoảng → trả nguyên
fun removePage(order: List<Int>, index: Int): List<Int>  // size == 1 hoặc ngoài khoảng → trả nguyên
/** Không rỗng, không trùng, mọi phần tử trong 1..pageCount. */
fun isValidOrder(order: List<Int>, pageCount: Int): Boolean
fun identityOrder(pageCount: Int): List<Int> = (1..pageCount).toList()

fun toggleSelection(selection: List<Long>, id: Long): List<Long>   // có thì bỏ, chưa có thì thêm vào cuối
fun pruneSelection(selection: List<Long>, existing: Set<Long>): List<Long>
/** Tài liệu theo đúng thứ tự chọn, bỏ id không còn. */
fun orderedSelection(selection: List<Long>, docs: List<Doc>): List<Doc>

sealed interface MergeCheck { data object NeedTwo : MergeCheck; data class TooMany(val total: Int) : MergeCheck; data class Ok(val total: Int) : MergeCheck }
fun checkMerge(docs: List<Doc>, maxPages: Int = FreeLimits.PRO_PAGES): MergeCheck  // < 2 → NeedTwo; tổng > maxPages → TooMany

// data/Compress.kt
enum class CompressLevel(val maxSide: Int, val quality: Int) { SMALL(1200, 60), MEDIUM(1800, 75), ORIGINAL(0, 100) }
/** Thu nhỏ giữ tỉ lệ để cạnh dài ≤ maxSide; không phóng to; mỗi cạnh ≥ 1. require(width > 0 && height > 0 && maxSide > 0). */
fun scaledSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int>
fun shouldShareOriginal(originalBytes: Long, compressedBytes: Long): Boolean = compressedBytes >= originalBytes

// data/JpegInfo.kt
data class JpegInfo(val width: Int, val height: Int, val components: Int)
/** Đọc marker SOF (C0–CF trừ C4, C8, CC). Không phải JPEG / cụt / gặp SOS hoặc EOI trước SOF / cao hoặc rộng = 0 → null. */
fun readJpegInfo(bytes: ByteArray): JpegInfo?

// data/JpegPdfWriter.kt
fun interface JpegSource { fun read(): ByteArray }
object JpegPdfWriter {
    const val PAGE_WIDTH = 595
    /** Chiều cao trang (điểm) giống PdfBuilder: (595 * h / w).toInt().coerceAtLeast(1). */
    fun pageHeight(widthPx: Int, heightPx: Int): Int
    /** Ghi PDF 1.4, mỗi JPEG một trang. Đọc từng ảnh một (không giữ hết trong RAM).
     *  require(pages.isNotEmpty()); ảnh không đọc được info hoặc components ∉ {1, 3} → IllegalArgumentException. */
    fun write(pages: List<JpegSource>, out: OutputStream)
}

// data/EditRecovery.kt
enum class Recovery { NOTHING, DELETE_NEW, PROMOTE_NEW, FINISH_COMMIT, RESTORE_OLD }
/** live = docs/<id>, new = docs/<id>.new, old = docs/<id>.old. Xét theo thứ tự, dòng đầu khớp thì chọn. */
fun recoveryAction(hasLive: Boolean, hasNew: Boolean, hasOld: Boolean): Recovery
//  1. live & old           → FINISH_COMMIT  (đã đổi tên xong: xóa old, xóa new nếu còn, cập nhật số trang theo file thực có)
//  2. !live & new & old    → PROMOTE_NEW    (tắt giữa 2 lần đổi tên: new → live, cập nhật số trang, xóa old)
//  3. !live & !new & old   → RESTORE_OLD    (old → live, cập nhật số trang)
//  4. new (& !old)         → DELETE_NEW     (tắt lúc đang dựng bản mới, hoặc tài liệu đã bị xóa: chỉ xóa new)
//  5. còn lại              → NOTHING

// util/Print.kt
/** "<tên>.pdf", thay / \ : * ? " < > | và ký tự điều khiển bằng "_", cắt tên còn 80 ký tự; tên rỗng → "document.pdf". */
fun printJobFileName(name: String): String
```

**Định dạng `JpegPdfWriter.write`** (ghi byte ASCII/ISO-8859-1, đếm offset từng object):
- Đầu: `%PDF-1.4\n` rồi `%` + byte `0xE2 0xE3 0xCF 0xD3` + `\n`.
- obj 1: `<< /Type /Catalog /Pages 2 0 R >>`; obj 2: `<< /Type /Pages /Kids [3 0 R 6 0 R …] /Count N >>`.
- Trang i (từ 0): trang = `3+3i`, nội dung = `4+3i`, ảnh = `5+3i`.
  - Trang: `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 H] /Resources << /XObject << /Im0 {ảnh} 0 R >> >> /Contents {nội dung} 0 R >>`.
  - Nội dung: chuỗi `q\n595 0 0 H 0 0 cm\n/Im0 Do\nQ\n`, ghi `<< /Length L >>\nstream\n…\nendstream`.
  - Ảnh: `<< /Type /XObject /Subtype /Image /Width w /Height h /ColorSpace /DeviceRGB|/DeviceGray /BitsPerComponent 8 /Filter /DCTDecode /Length len >>\nstream\n<byte JPEG>\nendstream`.
  - Mỗi object: `{n} 0 obj\n…\nendobj\n`.
- `xref\n0 {tổng+1}\n0000000000 65535 f \n` rồi mỗi object `%010d 00000 n \n` (đúng 20 byte mỗi dòng).
- `trailer\n<< /Size {tổng+1} /Root 1 0 R >>\nstartxref\n{offset của xref}\n%%EOF\n`.
- Ảnh trang nguồn không có xoay EXIF (ảnh quét ML Kit đã thẳng; ảnh nhập đã được xoay và ghi lại khi lưu) nên không cần xử lý EXIF.

### DocRepository (thêm)
```kotlin
class DocRepository(private val context: Context, private val dao: DocDao, private val folderDao: FolderDao) {
    val folders: Flow<List<Folder>> = folderDao.observeAll()
    /** Tăng sau mỗi lần ghi lại trang (sửa/thêm) để giao diện đọc lại ảnh. */
    val pageVersion: StateFlow<Long>

    suspend fun editPages(id: Long, order: List<Int>)                  // sắp + xóa
    suspend fun addScannedPages(id: Long, uris: List<Uri>): Int        // trả số trang đã thêm
    suspend fun addImages(id: Long, uris: List<Uri>): Int
    suspend fun merge(ids: List<Long>, name: String): Long             // id tài liệu mới
    suspend fun compressed(id: Long, level: CompressLevel): File       // ORIGINAL → trả pdfFile(id)
    suspend fun deleteMany(ids: List<Long>)                            // gọi delete(id) từng cái; lỗi một cái thì vẫn làm tiếp rồi ném lỗi đầu tiên
    suspend fun moveToFolder(ids: List<Long>, folderId: Long?)
    suspend fun createFolder(name: String): Long
    suspend fun renameFolder(id: Long, name: String)
    suspend fun deleteFolder(id: Long)                                 // folderDao.deleteKeepDocs
    suspend fun recoverPendingEdits()
}
```
**Ghi an toàn (dùng chung cho editPages / addScannedPages / addImages)**, hàm private `rewritePages(id: Long, sources: List<PageSource>)` với `sealed interface PageSource { data class Existing(val file: File); data class Scanned(val uri: Uri); data class Photo(val uri: Uri) }`. Chạy trên `Dispatchers.IO`:
1. `require(sources.size in 1..FreeLimits.PRO_PAGES)`. Đọc `dao.get(id)`; null thì báo lỗi.
2. `staging = docs/<id>.new`: xóa nếu đã có, rồi `mkdirs`.
3. Ghi lần lượt `staging/page_k.jpg`: Existing thì copy file; Scanned thì `copy(uri)`; Photo thì `importImage(uri)` (hàm sẵn có). Sau đó với mỗi file gọi `ensureSupportedJpeg(file)`: đọc byte, nếu `readJpegInfo` = null hoặc components ∉ {1,3} thì giải mã lại (`decodeSafely`, cạnh tối đa 2000) và ghi JPEG 88 đè lên.
4. Ghi `staging/doc.pdf` bằng `JpegPdfWriter` (ghi ra `doc.pdf.tmp` rồi `renameTo`).
5. **Commit** trong `withContext(NonCancellable)`: `live.renameTo(old)` → `staging.renameTo(live)` → `dao.setPageCount(id, n)` → `old.deleteRecursively()` → `evictThumbnails(live)` → `pageVersion.value++`. Nếu một lần `renameTo` trả false thì hoàn tác (old → live) và ném `IOException`.
6. Nếu lỗi ở bước 1–4 (kể cả `OutOfMemoryError`, bắt riêng rồi bọc thành `IOException`): xóa `staging`, ném lỗi. Bản gốc không bị đụng tới.

Cụ thể từng hàm:
- `editPages`: `require(isValidOrder(order, doc.pageCount))`, sources = `order.map { Existing(file page_it) }`.
- `addScannedPages` / `addImages`: `allowed = PRO_PAGES - doc.pageCount` (giới hạn theo gói đã được ViewModel cắt trước), lấy `uris.take(allowed)`; sources = trang cũ + trang mới. Bản PDF do ML Kit tạo bị bỏ qua, PDF được dựng lại bằng writer.
- `merge`: dùng lại `create(name, total) { dir -> … }` sẵn có (tạo bản ghi trước, lỗi thì tự xóa). Copy trang của từng tài liệu theo thứ tự `ids` thành `page_1..total`, gọi `ensureSupportedJpeg`, rồi writer ra `doc.pdf`. `require(checkMerge(docs) is MergeCheck.Ok)`. Tài liệu mới có `folderId = null`.
- `compressed`: dùng `cacheDir/compress/<id>/` cho ảnh tạm (xóa trước và trong `finally`). Mỗi trang: `decodeSafely(file, level.maxSide)` → `Bitmap.createScaledBitmap` tới `scaledSize` (nếu khác cỡ) → `compress(JPEG, level.quality)` → recycle. Writer ghi `docs/<id>/compressed.pdf.tmp` rồi `renameTo(compressed.pdf)`.
- `decodeSafely(file, maxSide): Bitmap` (private): đọc bounds, `inSampleSize = sampleSizeFor(longSide, maxSide)`. Bắt `OutOfMemoryError` thì nhân đôi sample và thử lại, tối đa 3 lần, sau đó ném `IOException`. Giải mã ra null thì ném `IOException`.
- `recoverPendingEdits`: xóa `cacheDir/compress`. Duyệt `filesDir/docs` tìm thư mục tên `<số>.new` hoặc `<số>.old`, với mỗi id gọi `recoveryAction` rồi làm theo. Với FINISH_COMMIT / PROMOTE_NEW / RESTORE_OLD thì `dao.setPageCount(id, đếm page_1.jpg, page_2.jpg… liên tiếp)` nếu `dao.get(id)` còn. Mọi lỗi trong hàm này chỉ bỏ qua (`runCatching`), không được làm crash lúc mở app.

### ViewModel và trạng thái UI
```kotlin
// ui/doc/DocViewModel.kt
enum class ExportKind { OPEN_PDF, SHARE_PDF, SHARE_IMAGES, PRINT }
data class DocUiState(
    val doc: Doc? = null, val pro: ProState = ProState(), val loaded: Boolean = false, val deleted: Boolean = false,
    val folders: List<Folder> = emptyList(),
    /** Đang ghi (thêm trang / nén / chuyển): hiện overlay, khóa nút. null = rảnh. */
    @StringRes val working: Int? = null,
    val revision: Long = 0,
) {
    val exportsLeft: Int /* giữ nguyên */
    val pagesCanAdd: Int get() = FreeLimits.pagesCanAdd(pro.isPro, doc?.pageCount ?: 0)
}

sealed interface DocEvent {
    data class Export(val kind: ExportKind) : DocEvent
    data class NeedPro(@StringRes val reason: Int) : DocEvent
    data class Message(@StringRes val res: Int) : DocEvent
    data object Deleted : DocEvent
    /** Chia sẻ file nén; before/after (byte) để báo "2.1 MB → 640 KB"; noGain = đã gửi bản gốc. */
    data class ShareCompressed(val file: File, val before: Long, val after: Long, val noGain: Boolean) : DocEvent
    data class Moved(val folderName: String?) : DocEvent   // null = đã bỏ khỏi thư mục
    data object PageLimitPro : DocEvent                    // Pro đã đủ 30 trang
}
// thêm hàm:
fun requestAddPages(): Boolean          // pagesCanAdd > 0 → true (UI mở sheet); free = 0 → NeedPro(limit_pages_free); Pro = 0 → PageLimitPro
fun addScanned(uris: List<Uri>)         // cắt uris.take(pagesCanAdd), working = R.string.saving, Message(pages_added) | Message(error_edit)
fun addPhotos(uris: List<Uri>)
fun compressAndShare(level: CompressLevel)
//   ORIGINAL: như export(SHARE_PDF).
//   SMALL/MEDIUM: nếu !pro && exportsLeft == 0 → NeedPro(limit_exports) và không nén;
//   working = compress_working; file = repo.compressed(); tryConsumeExport → false thì NeedPro;
//   noGain = shouldShareOriginal(...) → nếu true thì gửi pdfFile(); Message(error_compress) khi lỗi.
fun moveTo(folderId: Long?)
fun createFolderAndMove(name: String)   // tên đã qua checkFolderName.Ok ở dialog
// revision = repo.pageVersion; folders = repo.folders (combine thêm vào uiState).

// ui/doc/PagesViewModel.kt  (route "pages/{id}")
data class PagesUiState(val doc: Doc? = null, val loaded: Boolean = false, val order: List<Int> = emptyList(), val saving: Boolean = false) {
    val dirty: Boolean get() = doc != null && order != identityOrder(doc.pageCount)
}
sealed interface PagesEvent { data object Saved : PagesEvent; data class Message(@StringRes val res: Int) : PagesEvent }
class PagesViewModel(private val repo: DocRepository, private val handle: SavedStateHandle) : ViewModel() {
    val uiState: StateFlow<PagesUiState>
    // Bản nháp lưu trong handle["order"] (IntArray) nên xoay màn hình không mất. Khi tài liệu tải xong:
    //   nháp hợp lệ (isValidOrder) thì dùng, không thì identityOrder.
    fun pageFile(doc: Doc, page: Int): File
    fun moveUp(index: Int); fun moveDown(index: Int)
    fun remove(index: Int)   // size == 1 → Message(pages_last_page)
    fun save()               // !dirty → Saved ngay; else saving = true; repo.editPages; Saved | Message(error_edit)
}

// ui/main/MainViewModel.kt
data class MainUiState(
    /* các trường cũ giữ nguyên */,
    val folders: List<Folder> = emptyList(),
    /** null = All. Nếu thư mục đã bị xóa thì coi như null. */
    val folderFilter: Long? = null,
    val selecting: Boolean = false,
    /** id theo thứ tự chạm chọn (dùng cho thứ tự gộp). */
    val selection: List<Long> = emptyList(),
)
// docs = sortDocs(filterByFolder(filterDocs(all, q), folderFilter), sort).
// Gom query, sort, folderFilter, selecting, selection, saving vào MỘT MutableStateFlow<FilesUi> riêng để
// combine(repo.docs, repo.folders, proStore.state, filesUi) chỉ còn 4 luồng (combine có kiểu chỉ tới 5).
// selection luôn được pruneSelection theo id còn tồn tại.

sealed interface MainEvent {
    /* cũ */
    data class ShareFiles(val files: List<File>) : MainEvent
    data class NeedPro(@StringRes val reason: Int?) : MainEvent   // null: chỉ mở màn mua
    data class Moved(val folderName: String?) : MainEvent
}
fun setFolderFilter(id: Long?)
fun startSelection(); fun clearSelection(); fun toggleSelect(id: Long)
fun shareSelected()   // lọc tài liệu có pdf tồn tại (IO); rỗng → Message(error_open);
                      // proStore.tryConsumeExports(n) → ShareFiles | NeedPro(limit_exports_many); xong thì clearSelection
fun deleteSelected()  // repo.deleteMany; lỗi → Message(error_delete); clearSelection
fun moveSelected(folderId: Long?); fun createFolderAndMoveSelected(name: String)
fun mergeSelected(namePrefix: String)
//   !pro → NeedPro(null); checkMerge: NeedTwo → Message(merge_need_two), TooMany → Message(merge_too_many);
//   Ok → saving = true; repo.merge(ids, defaultDocName(prefix, now)) → clearSelection → OpenDoc(id); lỗi → Message(error_merge)
fun createFolder(name: String); fun renameFolder(id: Long, name: String); fun deleteFolder(id: Long)
// Lỗi thư mục/chuyển → Message(err_unknown) (chuỗi đã có).

// ui/common/Components.kt
@Composable
fun DocRow(
    doc: Doc, firstPage: File, subtitle: String, onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,      // có thì dùng Modifier.combinedClickable (thêm @OptIn(ExperimentalFoundationApi::class))
    selectMode: Boolean = false,            // true: thay chevron bằng vòng chọn
    selectedOrder: Int? = null,             // null = chưa chọn; ≥ 1 = thứ tự chọn (hiện trong vòng)
)   // Giá trị mặc định giữ nguyên hành vi ở Home và OcrPickSheet.
```
`MainEvent.Message` hiện chỉ nhận `@StringRes`. Các chuỗi có tham số (`merge_too_many`, `move_done`) thì thêm `data class MessageArgs(@StringRes val res: Int, val args: List<Any>)` vào cả `MainEvent` và `DocEvent`, rồi gọi `context.getString(res, *args.toTypedArray())`.

### Giao diện (theo phong cách V1, dùng lại component có sẵn)
**DocScreen**
- `DocHeader`: thứ tự icon là [Back] tên [ic_folder "doc_move"] [ic_edit] [ic_trash]. Nút thư mục mở `MoveToFolderSheet`.
- Sau `ActionRow` cũ thêm `ActionRow2` cùng kiểu `ActionCircle` (padding top 14.dp, 4 ô `weight(1f)`, không nhãn PRO): ic_plus "doc_add_pages", ic_pages "doc_edit_pages", ic_print "doc_print", ic_compress "doc_compress". `ExportsBar` vẫn ở cuối.
- Thêm trang: `if (vm.requestAddPages())` thì mở `ScanChoiceSheet(title = doc_add_pages, cameraSub = add_scan_sub, photosSub = add_photos_sub)`. Camera gọi `actions.scan(ScanMode.BATCH)`, ảnh gọi `actions.importPhotos()`, trong đó `actions = rememberScanActions(pageLimit = state.pagesCanAdd.coerceAtLeast(1), onScanned = { pages, _ -> vm.addScanned(pages) }, onPhotos = vm::addPhotos, onError = { snackbar error_scan })`.
- `NeedPro(limit_pages_free)`: Toast `getString(limit_pages_free, FreeLimits.FREE_PAGES, FreeLimits.PRO_PAGES)` rồi `onNeedPro()`. Riêng `NeedPro(limit_exports)` giữ cách cũ (tham số `DAILY_EXPORTS`). `PageLimitPro`: snackbar `limit_pages_pro` với `PRO_PAGES`.
- Sửa trang: `onEditPages(doc.id)` điều hướng tới `pages/{id}`.
- In: `vm.export(ExportKind.PRINT)`. Khi nhận `Export(PRINT)` thì `context.printPdf(vm.pdfFile(), doc.name)`; trả false thì snackbar `error_print` (file mất thì `error_open`).
- Nén: mở `CompressSheet` (private, `ModalBottomSheet` bo 28.dp giống ScanSheet). Gồm tiêu đề `compress_title` (20.sp Bold), `compress_body`, 3 hàng chọn (`RadioButton` + tiêu đề 16.sp SemiBold + mô tả 13.5.sp; cả hàng bấm được với `Modifier.selectable(role = Role.RadioButton)`, cao tối thiểu 56.dp), `GradientButton(compress_share)` rộng hết, `TextButton(cancel)`. Mức chọn lưu bằng `rememberSaveable`, mặc định MEDIUM. Khi nhận `ShareCompressed`: `shareFiles(listOf(file), "application/pdf", chooser)`, snackbar `compress_no_gain` hoặc `compress_result` với `Formatter.formatShortFileSize(before/after)`.
- `Moved`: snackbar `move_done` (tên thư mục) hoặc `move_removed`.
- `state.working != null` thì hiện `BusyOverlay(stringResource(working))`.
- `PageThumbnail(..., stamp = state.revision)` trong `PagePager`.

**PagesScreen** (route `pages/{id}`)
- Thanh trên giống `OcrLockedScreen`: Back + tiêu đề `doc_edit_pages`. Back hoặc nút Back hệ thống (`BackHandler`) khi `dirty` thì mở `AlertDialog(pages_discard_title, pages_discard_body, confirm = pages_discard màu error, dismiss = cancel)`.
- Dưới thanh: `pages_hint` (14.sp, onSurfaceVariant, padding 20.dp).
- `LazyColumn`, key = số trang gốc. Mỗi hàng (cao tối thiểu 88.dp, padding ngang 20.dp) gồm `PageThumbnail` 56×72.dp bo 8.dp viền `outlineVariant`, `Text(doc_page, vị trí + 1)` 16.sp SemiBold `weight(1f)`, `IconButton` ic_arrow_up (`page_move_up`, tắt ở hàng đầu), ic_arrow_down (`page_move_down`, tắt ở hàng cuối), ic_trash (`page_delete`, tint error).
- Dưới cùng: `GradientButton(save)` rộng hết, `navigationBarsPadding`, `enabled = dirty`, `busy = saving`. Khi `Saved` thì `nav.popIfOn(ROUTE_PAGES)`, và DocScreen tự cập nhật nhờ Flow `observe(id)` + `revision`. Message hiện bằng snackbar của màn này.
- Tài liệu null sau khi tải xong thì quay lại.

**FilesScreen**
- Trong `FilesHeader`, dưới hàng sort chip:
  - Hàng sort: `SortChip` Newest, Name, `Spacer(weight)`, rồi `TextButton(select)` (màu Accent, chỉ hiện khi `state.docs` không rỗng và chưa ở chế độ chọn).
  - Hàng chip thư mục, cuộn ngang (`Row(Modifier.horizontalScroll(rememberScrollState()), spacedBy(8.dp))`): `FilterChip` "folder_all", mỗi thư mục một `FilterChip` (label maxLines 1, ellipsis, `widthIn(max = 160.dp)`, leadingIcon ic_folder 18.dp), cuối hàng là `AssistChip` ic_plus + "folder_new" để mở `FolderNameDialog(title = folder_new, confirm = folder_create)`. Chip dùng màu như `SortChip`.
  - Khi đang lọc theo thư mục: thêm một hàng căn phải gồm `TextButton(doc_rename)` (mở `FolderNameDialog(folder_rename_title, save)`) và `TextButton(doc_delete, màu error)` (mở `AlertDialog(folder_delete_title, folder_delete_body)`).
- Đang lọc thư mục mà rỗng và ô tìm kiếm trống: hiện `folder_empty` (cùng kiểu chữ với `empty_search`).
- **Chế độ chọn:** long-press một `DocRow` thì `startSelection` + chọn hàng đó. Nút Select cũng vào chế độ chọn. Khi đang chọn thì chạm hàng = `toggleSelect`. `BackHandler(enabled = selecting) { clearSelection() }`.
- `DocRow` khi `selectMode`: vị trí chevron thay bằng vòng 26.dp. Đã chọn: nền `Gradients.Primary`, số thứ tự trắng 13.sp Bold. Chưa chọn: viền 2.dp `outline`. Cả hàng có `semantics { selected = … }`.
- Thanh hành động: `Box` bọc `LazyColumn` (đặt `contentPadding(bottom = 120.dp)` khi đang chọn). `SelectionBar` gắn `Alignment.BottomCenter`, padding 12.dp, là `Surface` bo 24.dp, `shadowElevation 8.dp`, màu `surface`. Bên trong:
  - Hàng 1: `Text(plural selected_count)` 16.sp SemiBold `weight(1f)`, `IconButton` ic_close ("close") gọi `clearSelection`.
  - Hàng 2: 4 nút tròn theo kiểu `ShortcutCircle` (vòng 48.dp, nhãn `maxLines = 2`): ic_share "share_chooser", ic_folder "sel_move", ic_merge "sel_merge" (+ `ProTag` khi !pro), ic_trash "doc_delete".
  - Chưa chọn gì thì 4 nút mờ (alpha 0.45) và không bấm được.
- Chuyển: `MoveToFolderSheet`. Gộp (Pro): `AlertDialog(merge_title)`, text gồm `merge_body` và dòng `join_dot(plural docs_count, plural pages tổng)`, confirm `sel_merge`. Xóa: `AlertDialog(delete_many_title, delete_many_body)`, confirm `doc_delete` màu error.
- Mọi cờ hộp thoại dùng `rememberSaveable`.

**ToolsScreen:** thêm `item { ToolTile(R.drawable.ic_merge, tool_merge_title, tool_merge_desc, !isPro, onMerge) }` ngay sau ô OCR. Trong AppRoot, `onMerge` làm như sau: !pro thì `nav.navigate(ROUTE_PAYWALL)`; `allDocs.size < 2` thì message `merge_need_two`; còn lại thì `tab = 1; vm.startSelection(); vm.message(tool_merge_pick)`.

**FolderSheets.kt**
- `MoveToFolderSheet(folders: List<Folder>, currentFolderId: Long?, onPick: (Long?) -> Unit, onNewFolder: () -> Unit, onDismiss: () -> Unit)`: `ModalBottomSheet` bo 28.dp, tiêu đề `doc_move` 20.sp Bold. Bên dưới là hàng "move_none" rồi mỗi thư mục một hàng (ic_folder, tên ellipsis, ic_check màu Accent nếu đang ở đó; hàng cao tối thiểu 56.dp), cuối danh sách là hàng ic_plus "folder_new". Dùng `LazyColumn` + `navigationBarsPadding`. Từ màn chọn nhiều thì `currentFolderId = null`.
- `FolderNameDialog(title: String, confirmLabel: String, initial: String, folders: List<Folder>, editingId: Long?, onDismiss: () -> Unit, onConfirm: (String) -> Unit)`: `AlertDialog` + `OutlinedTextField` (label `folder_name_label`, singleLine, `value.take(FOLDER_NAME_MAX)`, `rememberSaveable`). `checkFolderName` = Taken thì `supportingText = folder_name_taken`, `isError = true`. Nút xác nhận chỉ bật khi Ok.

**AppRoot**
- `ROUTE_PAGES = "pages/{id}"` (`NavType.LongType`), gọi `PagesScreen(viewModel(factory = AppViewModels.Factory), onBack = { nav.popIfOn(ROUTE_PAGES) })`.
- `DocScreen` thêm `onEditPages = { nav.navigate("pages/$it") }`.
- `FilesScreen` thêm tham số: `onFolder`, `onToggle`, `onStartSelect`, `onClearSelect`, `onShareSel`, `onDeleteSel`, `onMoveSel`, `onCreateFolderAndMove`, `onMergeSel`, `onCreateFolder`, `onRenameFolder`, `onDeleteFolder`, nối thẳng tới các hàm của `MainViewModel`.
- `onTab = { if (it != 1) vm.clearSelection(); tab = it }`.
- `MainEvent.ShareFiles` → `shareFiles(files, "application/pdf", shareLabel)`, false thì message `error_open`. `NeedPro(reason)` → có reason thì Toast `getString(reason)` rồi mở `ROUTE_PAYWALL`. `Moved` → snackbar như ở DocScreen. `MessageArgs` → snackbar.
- `ScanSheet` thay bằng `ScanChoiceSheet(...)`; `SavingOverlay()` thay bằng `BusyOverlay(stringResource(R.string.saving))`.

**util/Print.kt**
```kotlin
/** In file PDF có sẵn bằng PrintManager của hệ thống (không cần quyền). false nếu không có Activity/PrintManager hoặc file không còn. */
fun Context.printPdf(file: File, docName: String): Boolean
class PdfFilePrintAdapter(private val file: File, private val fileName: String) : PrintDocumentAdapter()
```
- `printPdf`: `file.isFile` thì tiếp. `val activity = findActivity() ?: return false`. Gọi `activity.getSystemService(PrintManager::class.java)?.print(docName, PdfFilePrintAdapter(file, printJobFileName(docName)), null)` bọc trong `try { …; true } catch (e: Exception) { false }`. PrintManager phải lấy từ Activity, không lấy từ applicationContext.
- `onLayout`: `cancellationSignal?.isCanceled == true` thì `onLayoutCancelled()`. Còn lại thì `onLayoutFinished(PrintDocumentInfo.Builder(fileName).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN).build(), true)`.
- `onWrite`: chép trong `Thread { … }.start()` (không chép trên luồng chính): `FileInputStream(file).use { i -> FileOutputStream(destination.fileDescriptor).use { o -> i.copyTo(o) } }`. Nếu đã hủy thì `onWriteCancelled()`, không thì `onWriteFinished(arrayOf(PageRange.ALL_PAGES))`. `IOException` thì `onWriteFailed(e.message)`.

## Chữ hiển thị
Thêm vào `values/strings.xml` và đủ 11 file dịch. Chuỗi có sẵn dùng lại (không thêm): `save`, `cancel`, `close`, `doc_rename`, `doc_delete`, `doc_page`, `share_chooser`, `saving`, `sheet_camera`, `sheet_photos`, `error_open`, `error_scan`, `error_delete`, `err_unknown`, `limit_exports`, `join_dot`, plurals `pages`.

| key | English | Tiếng Việt |
|---|---|---|
| doc_add_pages | Add pages | Thêm trang |
| doc_edit_pages | Edit pages | Sửa trang |
| doc_print | Print | In |
| doc_compress | Compress | Nén |
| doc_move | Move to folder | Chuyển vào thư mục |
| add_scan_sub | Scan more pages with the camera | Quét thêm trang bằng camera |
| add_photos_sub | Add photos to the end of this document | Thêm ảnh vào cuối tài liệu này |
| limit_pages_free | This document has %1$d pages, the free limit. Go Pro for up to %2$d pages. | Tài liệu này đã có %1$d trang, mức tối đa của bản miễn phí. Nâng cấp Pro để có tới %2$d trang. |
| limit_pages_pro | This document already has the maximum of %1$d pages. | Tài liệu này đã đủ tối đa %1$d trang. |
| pages_added | Pages added. | Đã thêm trang. |
| error_edit | Could not update the document. Your original is unchanged. | Không cập nhật được tài liệu. Bản gốc vẫn còn nguyên. |
| pages_hint | Use the arrows to reorder pages. Tap Save to keep your changes. | Dùng mũi tên để đổi thứ tự trang. Bấm Lưu để giữ thay đổi. |
| page_move_up | Move up | Lên trên |
| page_move_down | Move down | Xuống dưới |
| page_delete | Delete page | Xóa trang |
| pages_last_page | A document needs at least one page. | Tài liệu cần ít nhất một trang. |
| pages_discard_title | Discard changes? | Bỏ thay đổi? |
| pages_discard_body | The new page order and deleted pages will not be saved. | Thứ tự trang mới và các trang đã xóa sẽ không được lưu. |
| pages_discard | Discard | Bỏ |
| pages_saved | Pages updated. | Đã cập nhật trang. |
| compress_title | Compress PDF | Nén PDF |
| compress_body | Make the file smaller before sharing. The saved document is not changed. | Làm file nhỏ hơn trước khi chia sẻ. Tài liệu đã lưu không bị thay đổi. |
| compress_small | Small | Nhỏ |
| compress_small_sub | Smallest file, good for email and chat | File nhỏ nhất, hợp gửi email và tin nhắn |
| compress_medium | Medium | Vừa |
| compress_medium_sub | Balanced size and sharpness | Cân bằng dung lượng và độ nét |
| compress_original | Original | Gốc |
| compress_original_sub | Full quality, no compression | Giữ nguyên chất lượng, không nén |
| compress_share | Compress and share | Nén và chia sẻ |
| compress_working | Compressing… | Đang nén… |
| compress_result (translatable="false", chỉ ở values/) | %1$s → %2$s | (dùng chung) |
| compress_no_gain | This PDF is already small, so the original was shared. | File PDF này đã nhỏ nên đã chia sẻ bản gốc. |
| error_compress | Could not compress this PDF. | Không nén được file PDF này. |
| error_print | Printing is not available on this device. | Máy này không hỗ trợ in. |
| folder_all | All | Tất cả |
| folder_new | New folder | Thư mục mới |
| folder_name_label | Folder name | Tên thư mục |
| folder_create | Create | Tạo |
| folder_rename_title | Rename folder | Đổi tên thư mục |
| folder_delete_title | Delete this folder? | Xóa thư mục này? |
| folder_delete_body | Documents in it are not deleted. You can still find them in All. | Tài liệu bên trong không bị xóa. Bạn vẫn thấy chúng ở mục Tất cả. |
| folder_name_taken | A folder with this name already exists. | Đã có thư mục trùng tên. |
| folder_empty | No documents in this folder yet. Select documents and tap Move. | Thư mục này chưa có tài liệu. Chọn tài liệu rồi bấm Chuyển. |
| move_none | No folder | Không thuộc thư mục nào |
| move_done | Moved to %1$s | Đã chuyển vào %1$s |
| move_removed | Removed from folder. | Đã bỏ khỏi thư mục. |
| select | Select | Chọn |
| sel_move | Move | Chuyển |
| sel_merge | Merge | Gộp |
| delete_many_title | Delete the selected documents? | Xóa các tài liệu đã chọn? |
| delete_many_body | Their PDFs and pages will be removed from this device. | File PDF và các trang của chúng sẽ bị xóa khỏi máy này. |
| merge_title | Merge into one PDF | Gộp thành một PDF |
| merge_body | Pages are joined in the order you selected. The original documents are kept. | Các trang được nối theo thứ tự bạn đã chọn. Tài liệu gốc vẫn được giữ. |
| merge_need_two | Select at least 2 documents to merge. | Chọn ít nhất 2 tài liệu để gộp. |
| merge_too_many | A merged document can have up to %1$d pages. Select fewer documents. | Tài liệu gộp tối đa %1$d trang. Hãy chọn ít tài liệu hơn. |
| merge_default_name | Merged | Gộp |
| error_merge | Could not merge these documents. | Không gộp được các tài liệu này. |
| limit_exports_many | Not enough free exports left today. Go Pro for unlimited exports. | Hôm nay không còn đủ lượt xuất miễn phí. Nâng cấp Pro để xuất không giới hạn. |
| tool_merge_title | Merge PDFs | Gộp PDF |
| tool_merge_desc | Join documents into one PDF | Nối nhiều tài liệu thành một PDF |
| tool_merge_pick | Select documents, then tap Merge. | Chọn tài liệu rồi bấm Gộp. |
| plurals selected_count | one: %1$d selected · other: %1$d selected | other: Đã chọn %1$d |
| plurals docs_count | one: %1$d document · other: %1$d documents | other: %1$d tài liệu |

### Bản dịch 10 ngôn ngữ còn lại (dán đúng; dấu nháy đơn đã escape `\'`)
Thứ tự key giống bảng trên. Với plurals: ngôn ngữ có `one` thì ghi cả `one` và `other`; ja, ko, in, vi chỉ có `other`; ru có đủ `one/few/many/other`.

**values-es**
doc_add_pages=Añadir páginas · doc_edit_pages=Editar páginas · doc_print=Imprimir · doc_compress=Comprimir · doc_move=Mover a carpeta · add_scan_sub=Escanea más páginas con la cámara · add_photos_sub=Añade fotos al final de este documento · limit_pages_free=Este documento tiene %1$d páginas, el límite gratuito. Hazte Pro para tener hasta %2$d páginas. · limit_pages_pro=Este documento ya tiene el máximo de %1$d páginas. · pages_added=Páginas añadidas. · error_edit=No se pudo actualizar el documento. El original no ha cambiado. · pages_hint=Usa las flechas para cambiar el orden. Toca Guardar para conservar los cambios. · page_move_up=Subir · page_move_down=Bajar · page_delete=Eliminar página · pages_last_page=Un documento necesita al menos una página. · pages_discard_title=¿Descartar los cambios? · pages_discard_body=No se guardarán el nuevo orden ni las páginas eliminadas. · pages_discard=Descartar · pages_saved=Páginas actualizadas. · compress_title=Comprimir PDF · compress_body=Reduce el tamaño del archivo antes de compartirlo. El documento guardado no cambia. · compress_small=Pequeño · compress_small_sub=Archivo más ligero, ideal para correo y chat · compress_medium=Mediano · compress_medium_sub=Equilibrio entre tamaño y nitidez · compress_original=Original · compress_original_sub=Calidad completa, sin comprimir · compress_share=Comprimir y compartir · compress_working=Comprimiendo… · compress_no_gain=Este PDF ya es pequeño, así que se compartió el original. · error_compress=No se pudo comprimir este PDF. · error_print=Este dispositivo no permite imprimir. · folder_all=Todo · folder_new=Nueva carpeta · folder_name_label=Nombre de la carpeta · folder_create=Crear · folder_rename_title=Cambiar nombre de la carpeta · folder_delete_title=¿Eliminar esta carpeta? · folder_delete_body=Los documentos que contiene no se eliminan. Seguirán en Todo. · folder_name_taken=Ya existe una carpeta con ese nombre. · folder_empty=Esta carpeta aún no tiene documentos. Selecciona documentos y toca Mover. · move_none=Sin carpeta · move_done=Movido a %1$s · move_removed=Quitado de la carpeta. · select=Seleccionar · sel_move=Mover · sel_merge=Unir · delete_many_title=¿Eliminar los documentos seleccionados? · delete_many_body=Sus PDF y páginas se eliminarán de este dispositivo. · merge_title=Unir en un solo PDF · merge_body=Las páginas se unen en el orden en que las seleccionaste. Los documentos originales se conservan. · merge_need_two=Selecciona al menos 2 documentos para unir. · merge_too_many=Un documento unido puede tener hasta %1$d páginas. Selecciona menos documentos. · merge_default_name=Unido · error_merge=No se pudieron unir estos documentos. · limit_exports_many=No te quedan suficientes exportaciones gratis hoy. Hazte Pro para exportar sin límites. · tool_merge_title=Unir PDF · tool_merge_desc=Junta documentos en un solo PDF · tool_merge_pick=Selecciona documentos y toca Unir. · selected_count: one=%1$d seleccionado, other=%1$d seleccionados · docs_count: one=%1$d documento, other=%1$d documentos

**values-pt-rBR**
doc_add_pages=Adicionar páginas · doc_edit_pages=Editar páginas · doc_print=Imprimir · doc_compress=Comprimir · doc_move=Mover para pasta · add_scan_sub=Digitalize mais páginas com a câmera · add_photos_sub=Adicione fotos ao final deste documento · limit_pages_free=Este documento tem %1$d páginas, o limite grátis. Assine o Pro para ter até %2$d páginas. · limit_pages_pro=Este documento já tem o máximo de %1$d páginas. · pages_added=Páginas adicionadas. · error_edit=Não foi possível atualizar o documento. O original não foi alterado. · pages_hint=Use as setas para mudar a ordem. Toque em Salvar para manter as alterações. · page_move_up=Mover para cima · page_move_down=Mover para baixo · page_delete=Excluir página · pages_last_page=Um documento precisa de pelo menos uma página. · pages_discard_title=Descartar alterações? · pages_discard_body=A nova ordem e as páginas excluídas não serão salvas. · pages_discard=Descartar · pages_saved=Páginas atualizadas. · compress_title=Comprimir PDF · compress_body=Deixe o arquivo menor antes de compartilhar. O documento salvo não muda. · compress_small=Pequeno · compress_small_sub=Arquivo menor, ideal para e-mail e chat · compress_medium=Médio · compress_medium_sub=Equilíbrio entre tamanho e nitidez · compress_original=Original · compress_original_sub=Qualidade total, sem compressão · compress_share=Comprimir e compartilhar · compress_working=Comprimindo… · compress_no_gain=Este PDF já é pequeno, então o original foi compartilhado. · error_compress=Não foi possível comprimir este PDF. · error_print=Este aparelho não permite imprimir. · folder_all=Todos · folder_new=Nova pasta · folder_name_label=Nome da pasta · folder_create=Criar · folder_rename_title=Renomear pasta · folder_delete_title=Excluir esta pasta? · folder_delete_body=Os documentos dentro dela não são excluídos. Eles continuam em Todos. · folder_name_taken=Já existe uma pasta com esse nome. · folder_empty=Esta pasta ainda não tem documentos. Selecione documentos e toque em Mover. · move_none=Sem pasta · move_done=Movido para %1$s · move_removed=Removido da pasta. · select=Selecionar · sel_move=Mover · sel_merge=Juntar · delete_many_title=Excluir os documentos selecionados? · delete_many_body=Os PDFs e as páginas deles serão removidos deste aparelho. · merge_title=Juntar em um só PDF · merge_body=As páginas são unidas na ordem em que você selecionou. Os documentos originais são mantidos. · merge_need_two=Selecione pelo menos 2 documentos para juntar. · merge_too_many=Um documento unido pode ter até %1$d páginas. Selecione menos documentos. · merge_default_name=Unido · error_merge=Não foi possível juntar estes documentos. · limit_exports_many=Você não tem exportações grátis suficientes hoje. Assine o Pro para exportar sem limites. · tool_merge_title=Juntar PDFs · tool_merge_desc=Una documentos em um só PDF · tool_merge_pick=Selecione documentos e toque em Juntar. · selected_count: one=%1$d selecionado, other=%1$d selecionados · docs_count: one=%1$d documento, other=%1$d documentos

**values-fr**
doc_add_pages=Ajouter des pages · doc_edit_pages=Modifier les pages · doc_print=Imprimer · doc_compress=Compresser · doc_move=Déplacer vers un dossier · add_scan_sub=Numérisez d\'autres pages avec l\'appareil photo · add_photos_sub=Ajoutez des photos à la fin de ce document · limit_pages_free=Ce document a %1$d pages, la limite gratuite. Passez à Pro pour aller jusqu\'à %2$d pages. · limit_pages_pro=Ce document a déjà le maximum de %1$d pages. · pages_added=Pages ajoutées. · error_edit=Impossible de mettre à jour le document. L\'original n\'a pas changé. · pages_hint=Utilisez les flèches pour changer l\'ordre. Touchez Enregistrer pour garder vos modifications. · page_move_up=Monter · page_move_down=Descendre · page_delete=Supprimer la page · pages_last_page=Un document doit avoir au moins une page. · pages_discard_title=Ignorer les modifications ? · pages_discard_body=Le nouvel ordre et les pages supprimées ne seront pas enregistrés. · pages_discard=Ignorer · pages_saved=Pages mises à jour. · compress_title=Compresser le PDF · compress_body=Réduisez la taille du fichier avant de le partager. Le document enregistré ne change pas. · compress_small=Petit · compress_small_sub=Fichier le plus léger, idéal pour e-mail et messagerie · compress_medium=Moyen · compress_medium_sub=Bon équilibre entre taille et netteté · compress_original=Original · compress_original_sub=Qualité complète, sans compression · compress_share=Compresser et partager · compress_working=Compression… · compress_no_gain=Ce PDF est déjà léger, l\'original a donc été partagé. · error_compress=Impossible de compresser ce PDF. · error_print=L\'impression n\'est pas disponible sur cet appareil. · folder_all=Tout · folder_new=Nouveau dossier · folder_name_label=Nom du dossier · folder_create=Créer · folder_rename_title=Renommer le dossier · folder_delete_title=Supprimer ce dossier ? · folder_delete_body=Les documents qu\'il contient ne sont pas supprimés. Ils restent dans Tout. · folder_name_taken=Un dossier porte déjà ce nom. · folder_empty=Ce dossier est vide. Sélectionnez des documents et touchez Déplacer. · move_none=Aucun dossier · move_done=Déplacé vers %1$s · move_removed=Retiré du dossier. · select=Sélectionner · sel_move=Déplacer · sel_merge=Fusionner · delete_many_title=Supprimer les documents sélectionnés ? · delete_many_body=Leurs PDF et leurs pages seront supprimés de cet appareil. · merge_title=Fusionner en un seul PDF · merge_body=Les pages sont assemblées dans l\'ordre de votre sélection. Les documents d\'origine sont conservés. · merge_need_two=Sélectionnez au moins 2 documents à fusionner. · merge_too_many=Un document fusionné peut compter jusqu\'à %1$d pages. Sélectionnez moins de documents. · merge_default_name=Fusion · error_merge=Impossible de fusionner ces documents. · limit_exports_many=Il ne vous reste pas assez d\'exports gratuits aujourd\'hui. Passez à Pro pour des exports illimités. · tool_merge_title=Fusionner des PDF · tool_merge_desc=Réunir des documents en un seul PDF · tool_merge_pick=Sélectionnez des documents, puis touchez Fusionner. · selected_count: one=%1$d sélectionné, other=%1$d sélectionnés · docs_count: one=%1$d document, other=%1$d documents

**values-de**
doc_add_pages=Seiten hinzufügen · doc_edit_pages=Seiten bearbeiten · doc_print=Drucken · doc_compress=Komprimieren · doc_move=In Ordner verschieben · add_scan_sub=Weitere Seiten mit der Kamera scannen · add_photos_sub=Fotos am Ende dieses Dokuments anfügen · limit_pages_free=Dieses Dokument hat %1$d Seiten, das Limit der Gratisversion. Mit Pro sind bis zu %2$d Seiten möglich. · limit_pages_pro=Dieses Dokument hat bereits die maximale Anzahl von %1$d Seiten. · pages_added=Seiten hinzugefügt. · error_edit=Das Dokument konnte nicht aktualisiert werden. Das Original ist unverändert. · pages_hint=Ändere die Reihenfolge mit den Pfeilen. Tippe auf Speichern, um die Änderungen zu behalten. · page_move_up=Nach oben · page_move_down=Nach unten · page_delete=Seite löschen · pages_last_page=Ein Dokument braucht mindestens eine Seite. · pages_discard_title=Änderungen verwerfen? · pages_discard_body=Die neue Reihenfolge und gelöschte Seiten werden nicht gespeichert. · pages_discard=Verwerfen · pages_saved=Seiten aktualisiert. · compress_title=PDF komprimieren · compress_body=Mach die Datei vor dem Teilen kleiner. Das gespeicherte Dokument bleibt unverändert. · compress_small=Klein · compress_small_sub=Kleinste Datei, gut für E-Mail und Chat · compress_medium=Mittel · compress_medium_sub=Ausgewogen zwischen Größe und Schärfe · compress_original=Original · compress_original_sub=Volle Qualität, keine Komprimierung · compress_share=Komprimieren und teilen · compress_working=Wird komprimiert… · compress_no_gain=Dieses PDF ist bereits klein, daher wurde das Original geteilt. · error_compress=Dieses PDF konnte nicht komprimiert werden. · error_print=Drucken ist auf diesem Gerät nicht verfügbar. · folder_all=Alle · folder_new=Neuer Ordner · folder_name_label=Ordnername · folder_create=Erstellen · folder_rename_title=Ordner umbenennen · folder_delete_title=Diesen Ordner löschen? · folder_delete_body=Die Dokumente darin werden nicht gelöscht. Du findest sie weiter unter Alle. · folder_name_taken=Es gibt bereits einen Ordner mit diesem Namen. · folder_empty=Dieser Ordner ist noch leer. Wähle Dokumente aus und tippe auf Verschieben. · move_none=Kein Ordner · move_done=Verschoben nach %1$s · move_removed=Aus dem Ordner entfernt. · select=Auswählen · sel_move=Verschieben · sel_merge=Zusammenführen · delete_many_title=Ausgewählte Dokumente löschen? · delete_many_body=Ihre PDFs und Seiten werden von diesem Gerät entfernt. · merge_title=Zu einem PDF zusammenführen · merge_body=Die Seiten werden in der Reihenfolge deiner Auswahl verbunden. Die Originaldokumente bleiben erhalten. · merge_need_two=Wähle mindestens 2 Dokumente zum Zusammenführen aus. · merge_too_many=Ein zusammengeführtes Dokument kann bis zu %1$d Seiten haben. Wähle weniger Dokumente aus. · merge_default_name=Zusammengeführt · error_merge=Diese Dokumente konnten nicht zusammengeführt werden. · limit_exports_many=Heute sind nicht mehr genug kostenlose Exporte übrig. Mit Pro exportierst du unbegrenzt. · tool_merge_title=PDFs zusammenführen · tool_merge_desc=Dokumente zu einem PDF verbinden · tool_merge_pick=Wähle Dokumente aus und tippe dann auf Zusammenführen. · selected_count: one=%1$d ausgewählt, other=%1$d ausgewählt · docs_count: one=%1$d Dokument, other=%1$d Dokumente

**values-in**
doc_add_pages=Tambah halaman · doc_edit_pages=Edit halaman · doc_print=Cetak · doc_compress=Kompres · doc_move=Pindahkan ke folder · add_scan_sub=Pindai halaman lain dengan kamera · add_photos_sub=Tambahkan foto di akhir dokumen ini · limit_pages_free=Dokumen ini sudah %1$d halaman, batas versi gratis. Upgrade ke Pro untuk hingga %2$d halaman. · limit_pages_pro=Dokumen ini sudah mencapai batas %1$d halaman. · pages_added=Halaman ditambahkan. · error_edit=Dokumen tidak dapat diperbarui. Dokumen asli tidak berubah. · pages_hint=Gunakan panah untuk mengubah urutan. Ketuk Simpan untuk menyimpan perubahan. · page_move_up=Naikkan · page_move_down=Turunkan · page_delete=Hapus halaman · pages_last_page=Dokumen harus memiliki minimal satu halaman. · pages_discard_title=Buang perubahan? · pages_discard_body=Urutan baru dan halaman yang dihapus tidak akan disimpan. · pages_discard=Buang · pages_saved=Halaman diperbarui. · compress_title=Kompres PDF · compress_body=Perkecil ukuran file sebelum dibagikan. Dokumen yang tersimpan tidak berubah. · compress_small=Kecil · compress_small_sub=File terkecil, cocok untuk email dan chat · compress_medium=Sedang · compress_medium_sub=Seimbang antara ukuran dan ketajaman · compress_original=Asli · compress_original_sub=Kualitas penuh, tanpa kompresi · compress_share=Kompres dan bagikan · compress_working=Mengompres… · compress_no_gain=PDF ini sudah kecil, jadi file asli yang dibagikan. · error_compress=PDF ini tidak dapat dikompres. · error_print=Perangkat ini tidak mendukung pencetakan. · folder_all=Semua · folder_new=Folder baru · folder_name_label=Nama folder · folder_create=Buat · folder_rename_title=Ganti nama folder · folder_delete_title=Hapus folder ini? · folder_delete_body=Dokumen di dalamnya tidak dihapus. Anda tetap bisa menemukannya di Semua. · folder_name_taken=Folder dengan nama ini sudah ada. · folder_empty=Folder ini belum berisi dokumen. Pilih dokumen lalu ketuk Pindahkan. · move_none=Tanpa folder · move_done=Dipindahkan ke %1$s · move_removed=Dikeluarkan dari folder. · select=Pilih · sel_move=Pindahkan · sel_merge=Gabungkan · delete_many_title=Hapus dokumen yang dipilih? · delete_many_body=PDF dan halamannya akan dihapus dari perangkat ini. · merge_title=Gabungkan jadi satu PDF · merge_body=Halaman digabung sesuai urutan pilihan Anda. Dokumen asli tetap disimpan. · merge_need_two=Pilih minimal 2 dokumen untuk digabungkan. · merge_too_many=Dokumen gabungan maksimal %1$d halaman. Pilih lebih sedikit dokumen. · merge_default_name=Gabungan · error_merge=Dokumen ini tidak dapat digabungkan. · limit_exports_many=Sisa ekspor gratis hari ini tidak cukup. Upgrade ke Pro untuk ekspor tanpa batas. · tool_merge_title=Gabungkan PDF · tool_merge_desc=Satukan dokumen menjadi satu PDF · tool_merge_pick=Pilih dokumen, lalu ketuk Gabungkan. · selected_count: other=%1$d dipilih · docs_count: other=%1$d dokumen

**values-ru**
doc_add_pages=Добавить страницы · doc_edit_pages=Изменить страницы · doc_print=Печать · doc_compress=Сжать · doc_move=Переместить в папку · add_scan_sub=Отсканируйте ещё страницы камерой · add_photos_sub=Добавьте фото в конец этого документа · limit_pages_free=В документе %1$d стр. — это предел бесплатной версии. С Pro можно до %2$d стр. · limit_pages_pro=В документе уже максимум страниц: %1$d. · pages_added=Страницы добавлены. · error_edit=Не удалось обновить документ. Оригинал не изменён. · pages_hint=Меняйте порядок страниц стрелками. Нажмите «Сохранить», чтобы сохранить изменения. · page_move_up=Выше · page_move_down=Ниже · page_delete=Удалить страницу · pages_last_page=В документе должна быть хотя бы одна страница. · pages_discard_title=Отменить изменения? · pages_discard_body=Новый порядок и удалённые страницы не будут сохранены. · pages_discard=Отменить · pages_saved=Страницы обновлены. · compress_title=Сжать PDF · compress_body=Уменьшите файл перед отправкой. Сохранённый документ не изменится. · compress_small=Маленький · compress_small_sub=Самый лёгкий файл для почты и чатов · compress_medium=Средний · compress_medium_sub=Баланс размера и чёткости · compress_original=Оригинал · compress_original_sub=Полное качество, без сжатия · compress_share=Сжать и отправить · compress_working=Сжатие… · compress_no_gain=Этот PDF уже небольшой, поэтому отправлен оригинал. · error_compress=Не удалось сжать этот PDF. · error_print=Печать на этом устройстве недоступна. · folder_all=Все · folder_new=Новая папка · folder_name_label=Название папки · folder_create=Создать · folder_rename_title=Переименовать папку · folder_delete_title=Удалить эту папку? · folder_delete_body=Документы из неё не удаляются. Они останутся в разделе «Все». · folder_name_taken=Папка с таким названием уже есть. · folder_empty=В этой папке пока нет документов. Выберите документы и нажмите «Переместить». · move_none=Без папки · move_done=Перемещено в «%1$s» · move_removed=Убрано из папки. · select=Выбрать · sel_move=Переместить · sel_merge=Объединить · delete_many_title=Удалить выбранные документы? · delete_many_body=Их PDF и страницы будут удалены с этого устройства. · merge_title=Объединить в один PDF · merge_body=Страницы соединяются в порядке выбора. Исходные документы сохраняются. · merge_need_two=Выберите хотя бы 2 документа для объединения. · merge_too_many=В объединённом документе может быть до %1$d стр. Выберите меньше документов. · merge_default_name=Объединённый · error_merge=Не удалось объединить эти документы. · limit_exports_many=На сегодня недостаточно бесплатных экспортов. С Pro экспорт без ограничений. · tool_merge_title=Объединить PDF · tool_merge_desc=Собрать документы в один PDF · tool_merge_pick=Выберите документы и нажмите «Объединить». · selected_count: one/few/many/other=Выбрано: %1$d · docs_count: one=%1$d документ, few=%1$d документа, many=%1$d документов, other=%1$d документа

**values-tr**
doc_add_pages=Sayfa ekle · doc_edit_pages=Sayfaları düzenle · doc_print=Yazdır · doc_compress=Sıkıştır · doc_move=Klasöre taşı · add_scan_sub=Kamerayla daha fazla sayfa tara · add_photos_sub=Bu belgenin sonuna fotoğraf ekle · limit_pages_free=Bu belge %1$d sayfa ile ücretsiz sınıra ulaştı. %2$d sayfaya kadar için Pro\'ya geçin. · limit_pages_pro=Bu belge zaten en fazla %1$d sayfaya sahip. · pages_added=Sayfalar eklendi. · error_edit=Belge güncellenemedi. Orijinal değişmedi. · pages_hint=Sırayı değiştirmek için okları kullanın. Değişiklikleri korumak için Kaydet\'e dokunun. · page_move_up=Yukarı taşı · page_move_down=Aşağı taşı · page_delete=Sayfayı sil · pages_last_page=Bir belgede en az bir sayfa olmalı. · pages_discard_title=Değişiklikler silinsin mi? · pages_discard_body=Yeni sayfa sırası ve silinen sayfalar kaydedilmeyecek. · pages_discard=Vazgeç · pages_saved=Sayfalar güncellendi. · compress_title=PDF\'yi sıkıştır · compress_body=Paylaşmadan önce dosyayı küçültün. Kayıtlı belge değişmez. · compress_small=Küçük · compress_small_sub=En küçük dosya, e-posta ve sohbet için ideal · compress_medium=Orta · compress_medium_sub=Boyut ve netlik dengeli · compress_original=Orijinal · compress_original_sub=Tam kalite, sıkıştırma yok · compress_share=Sıkıştır ve paylaş · compress_working=Sıkıştırılıyor… · compress_no_gain=Bu PDF zaten küçük, bu yüzden orijinali paylaşıldı. · error_compress=Bu PDF sıkıştırılamadı. · error_print=Bu cihazda yazdırma kullanılamıyor. · folder_all=Tümü · folder_new=Yeni klasör · folder_name_label=Klasör adı · folder_create=Oluştur · folder_rename_title=Klasörü yeniden adlandır · folder_delete_title=Bu klasör silinsin mi? · folder_delete_body=İçindeki belgeler silinmez. Onları Tümü bölümünde bulabilirsiniz. · folder_name_taken=Bu adda bir klasör zaten var. · folder_empty=Bu klasörde henüz belge yok. Belgeleri seçip Taşı\'ya dokunun. · move_none=Klasör yok · move_done=%1$s klasörüne taşındı · move_removed=Klasörden çıkarıldı. · select=Seç · sel_move=Taşı · sel_merge=Birleştir · delete_many_title=Seçili belgeler silinsin mi? · delete_many_body=PDF\'leri ve sayfaları bu cihazdan kaldırılacak. · merge_title=Tek PDF\'de birleştir · merge_body=Sayfalar seçtiğiniz sırayla birleştirilir. Orijinal belgeler korunur. · merge_need_two=Birleştirmek için en az 2 belge seçin. · merge_too_many=Birleştirilmiş belge en fazla %1$d sayfa olabilir. Daha az belge seçin. · merge_default_name=Birleştirilmiş · error_merge=Bu belgeler birleştirilemedi. · limit_exports_many=Bugün yeterli ücretsiz dışa aktarma hakkınız kalmadı. Sınırsız dışa aktarma için Pro\'ya geçin. · tool_merge_title=PDF birleştir · tool_merge_desc=Belgeleri tek PDF\'de topla · tool_merge_pick=Belgeleri seçin, ardından Birleştir\'e dokunun. · selected_count: one=%1$d seçildi, other=%1$d seçildi · docs_count: one=%1$d belge, other=%1$d belge

**values-ja**
doc_add_pages=ページを追加 · doc_edit_pages=ページを編集 · doc_print=印刷 · doc_compress=圧縮 · doc_move=フォルダに移動 · add_scan_sub=カメラでページを追加スキャン · add_photos_sub=この書類の最後に写真を追加 · limit_pages_free=この書類は%1$dページで、無料版の上限です。Proなら最大%2$dページまで使えます。 · limit_pages_pro=この書類はすでに上限の%1$dページです。 · pages_added=ページを追加しました。 · error_edit=書類を更新できませんでした。元の書類はそのままです。 · pages_hint=矢印で順番を変えられます。変更を残すには「保存」をタップしてください。 · page_move_up=上へ移動 · page_move_down=下へ移動 · page_delete=ページを削除 · pages_last_page=書類には1ページ以上が必要です。 · pages_discard_title=変更を破棄しますか？ · pages_discard_body=新しいページ順と削除したページは保存されません。 · pages_discard=破棄 · pages_saved=ページを更新しました。 · compress_title=PDFを圧縮 · compress_body=共有する前にファイルを小さくします。保存済みの書類は変わりません。 · compress_small=小 · compress_small_sub=最小サイズ。メールやチャット向け · compress_medium=中 · compress_medium_sub=サイズと鮮明さのバランス · compress_original=元のまま · compress_original_sub=圧縮なし、元の画質 · compress_share=圧縮して共有 · compress_working=圧縮中… · compress_no_gain=このPDFはすでに小さいため、元のファイルを共有しました。 · error_compress=このPDFを圧縮できませんでした。 · error_print=この端末では印刷できません。 · folder_all=すべて · folder_new=新しいフォルダ · folder_name_label=フォルダ名 · folder_create=作成 · folder_rename_title=フォルダ名を変更 · folder_delete_title=このフォルダを削除しますか？ · folder_delete_body=中の書類は削除されません。「すべて」から引き続き見られます。 · folder_name_taken=同じ名前のフォルダがすでにあります。 · folder_empty=このフォルダにはまだ書類がありません。書類を選んで「移動」をタップしてください。 · move_none=フォルダなし · move_done=%1$sに移動しました · move_removed=フォルダから外しました。 · select=選択 · sel_move=移動 · sel_merge=結合 · delete_many_title=選択した書類を削除しますか？ · delete_many_body=PDFとページがこの端末から削除されます。 · merge_title=1つのPDFに結合 · merge_body=選択した順にページをつなげます。元の書類は残ります。 · merge_need_two=結合するには書類を2つ以上選んでください。 · merge_too_many=結合した書類は最大%1$dページです。選ぶ書類を減らしてください。 · merge_default_name=結合 · error_merge=書類を結合できませんでした。 · limit_exports_many=今日の無料エクスポートが足りません。Proなら無制限にエクスポートできます。 · tool_merge_title=PDFを結合 · tool_merge_desc=複数の書類を1つのPDFに · tool_merge_pick=書類を選んで「結合」をタップしてください。 · selected_count: other=%1$d件選択中 · docs_count: other=%1$d件の書類

**values-ko**
doc_add_pages=페이지 추가 · doc_edit_pages=페이지 편집 · doc_print=인쇄 · doc_compress=압축 · doc_move=폴더로 이동 · add_scan_sub=카메라로 페이지 더 스캔하기 · add_photos_sub=이 문서 끝에 사진 추가 · limit_pages_free=이 문서는 무료 한도인 %1$d페이지입니다. Pro로 업그레이드하면 최대 %2$d페이지까지 가능합니다. · limit_pages_pro=이 문서는 이미 최대 %1$d페이지입니다. · pages_added=페이지를 추가했습니다. · error_edit=문서를 업데이트하지 못했습니다. 원본은 그대로입니다. · pages_hint=화살표로 순서를 바꾸세요. 변경 사항을 유지하려면 저장을 누르세요. · page_move_up=위로 이동 · page_move_down=아래로 이동 · page_delete=페이지 삭제 · pages_last_page=문서에는 페이지가 하나 이상 있어야 합니다. · pages_discard_title=변경 사항을 버릴까요? · pages_discard_body=새 페이지 순서와 삭제한 페이지가 저장되지 않습니다. · pages_discard=버리기 · pages_saved=페이지를 업데이트했습니다. · compress_title=PDF 압축 · compress_body=공유하기 전에 파일 크기를 줄입니다. 저장된 문서는 바뀌지 않습니다. · compress_small=작게 · compress_small_sub=가장 작은 파일, 이메일과 채팅에 적합 · compress_medium=보통 · compress_medium_sub=크기와 선명도의 균형 · compress_original=원본 · compress_original_sub=원래 화질, 압축 없음 · compress_share=압축해서 공유 · compress_working=압축 중… · compress_no_gain=이 PDF는 이미 작아서 원본을 공유했습니다. · error_compress=이 PDF를 압축하지 못했습니다. · error_print=이 기기에서는 인쇄할 수 없습니다. · folder_all=전체 · folder_new=새 폴더 · folder_name_label=폴더 이름 · folder_create=만들기 · folder_rename_title=폴더 이름 바꾸기 · folder_delete_title=이 폴더를 삭제할까요? · folder_delete_body=안에 있는 문서는 삭제되지 않습니다. 전체에서 계속 볼 수 있습니다. · folder_name_taken=같은 이름의 폴더가 이미 있습니다. · folder_empty=이 폴더에 아직 문서가 없습니다. 문서를 선택하고 이동을 누르세요. · move_none=폴더 없음 · move_done=%1$s(으)로 이동했습니다 · move_removed=폴더에서 뺐습니다. · select=선택 · sel_move=이동 · sel_merge=합치기 · delete_many_title=선택한 문서를 삭제할까요? · delete_many_body=PDF와 페이지가 이 기기에서 삭제됩니다. · merge_title=하나의 PDF로 합치기 · merge_body=선택한 순서대로 페이지를 이어 붙입니다. 원본 문서는 그대로 남습니다. · merge_need_two=합칠 문서를 2개 이상 선택하세요. · merge_too_many=합친 문서는 최대 %1$d페이지까지 가능합니다. 문서를 더 적게 선택하세요. · merge_default_name=합본 · error_merge=문서를 합치지 못했습니다. · limit_exports_many=오늘 남은 무료 내보내기가 부족합니다. Pro로 업그레이드하면 무제한으로 내보낼 수 있습니다. · tool_merge_title=PDF 합치기 · tool_merge_desc=여러 문서를 하나의 PDF로 · tool_merge_pick=문서를 선택한 뒤 합치기를 누르세요. · selected_count: other=%1$d개 선택됨 · docs_count: other=문서 %1$d개

**values-hi**
doc_add_pages=पेज जोड़ें · doc_edit_pages=पेज संपादित करें · doc_print=प्रिंट करें · doc_compress=कंप्रेस करें · doc_move=फ़ोल्डर में ले जाएँ · add_scan_sub=कैमरे से और पेज स्कैन करें · add_photos_sub=इस दस्तावेज़ के अंत में फ़ोटो जोड़ें · limit_pages_free=इस दस्तावेज़ में %1$d पेज हैं, जो फ़्री सीमा है। %2$d पेज तक के लिए Pro लें। · limit_pages_pro=इस दस्तावेज़ में पहले से अधिकतम %1$d पेज हैं। · pages_added=पेज जोड़ दिए गए। · error_edit=दस्तावेज़ अपडेट नहीं हो सका। मूल दस्तावेज़ में कोई बदलाव नहीं हुआ। · pages_hint=क्रम बदलने के लिए तीरों का उपयोग करें। बदलाव रखने के लिए सेव करें पर टैप करें। · page_move_up=ऊपर ले जाएँ · page_move_down=नीचे ले जाएँ · page_delete=पेज हटाएँ · pages_last_page=दस्तावेज़ में कम से कम एक पेज होना चाहिए। · pages_discard_title=बदलाव छोड़ दें? · pages_discard_body=नया पेज क्रम और हटाए गए पेज सेव नहीं होंगे। · pages_discard=छोड़ें · pages_saved=पेज अपडेट हो गए। · compress_title=PDF कंप्रेस करें · compress_body=शेयर करने से पहले फ़ाइल छोटी करें। सेव किया गया दस्तावेज़ नहीं बदलेगा। · compress_small=छोटा · compress_small_sub=सबसे छोटी फ़ाइल, ईमेल और चैट के लिए अच्छी · compress_medium=मध्यम · compress_medium_sub=आकार और स्पष्टता में संतुलन · compress_original=मूल · compress_original_sub=पूरी क्वालिटी, कोई कंप्रेशन नहीं · compress_share=कंप्रेस करके शेयर करें · compress_working=कंप्रेस हो रहा है… · compress_no_gain=यह PDF पहले से छोटी है, इसलिए मूल फ़ाइल शेयर की गई। · error_compress=यह PDF कंप्रेस नहीं हो सकी। · error_print=इस डिवाइस पर प्रिंटिंग उपलब्ध नहीं है। · folder_all=सभी · folder_new=नया फ़ोल्डर · folder_name_label=फ़ोल्डर का नाम · folder_create=बनाएँ · folder_rename_title=फ़ोल्डर का नाम बदलें · folder_delete_title=यह फ़ोल्डर हटाएँ? · folder_delete_body=इसके अंदर के दस्तावेज़ नहीं हटेंगे। वे सभी में मिलते रहेंगे। · folder_name_taken=इस नाम का फ़ोल्डर पहले से है। · folder_empty=इस फ़ोल्डर में अभी कोई दस्तावेज़ नहीं है। दस्तावेज़ चुनें और ले जाएँ पर टैप करें। · move_none=कोई फ़ोल्डर नहीं · move_done=%1$s में ले जाया गया · move_removed=फ़ोल्डर से हटा दिया गया। · select=चुनें · sel_move=ले जाएँ · sel_merge=मर्ज करें · delete_many_title=चुने गए दस्तावेज़ हटाएँ? · delete_many_body=उनकी PDF और पेज इस डिवाइस से हटा दिए जाएँगे। · merge_title=एक PDF में मर्ज करें · merge_body=पेज आपके चुने गए क्रम में जोड़े जाते हैं। मूल दस्तावेज़ बने रहते हैं। · merge_need_two=मर्ज करने के लिए कम से कम 2 दस्तावेज़ चुनें। · merge_too_many=मर्ज किए गए दस्तावेज़ में अधिकतम %1$d पेज हो सकते हैं। कम दस्तावेज़ चुनें। · merge_default_name=मर्ज किया गया · error_merge=ये दस्तावेज़ मर्ज नहीं हो सके। · limit_exports_many=आज के लिए पर्याप्त फ़्री एक्सपोर्ट नहीं बचे। असीमित एक्सपोर्ट के लिए Pro लें। · tool_merge_title=PDF मर्ज करें · tool_merge_desc=कई दस्तावेज़ों को एक PDF में जोड़ें · tool_merge_pick=दस्तावेज़ चुनें, फिर मर्ज करें पर टैप करें। · selected_count: one=%1$d चुना गया, other=%1$d चुने गए · docs_count: one=%1$d दस्तावेज़, other=%1$d दस्तावेज़

**values-vi** (plurals): selected_count other=Đã chọn %1$d · docs_count other=%1$d tài liệu. Các chuỗi khác lấy từ cột Tiếng Việt ở bảng trên.

Lưu ý XML: `…` là ký tự U+2026 (giống `saving` hiện có). Không có `%` trần nào ngoài `%1$d`/`%2$d`/`%1$s`. Không chuỗi nào bắt đầu bằng `?` hay `@`. Dấu `'` đã escape ở fr/tr.

## Trường hợp biên
- **Cập nhật từ bản cũ (v1 → v2):** mọi tài liệu cũ còn nguyên, `folderId = NULL`, hiện ở "All". Không có `fallbackToDestructiveMigration`. Migration chỉ có `CREATE TABLE` + `ADD COLUMN`, không `DROP`/`DELETE`.
- **Tắt app / hết pin giữa lúc ghi trang:** bản gốc không bị đụng cho tới bước commit. Commit chạy `NonCancellable`. Lần mở sau, `recoverPendingEdits` dọn dẹp hoặc hoàn tất theo `recoveryAction`.
- **Rời màn hình giữa lúc ghi** (Back khi đang thêm trang hoặc nén): coroutine bị hủy ở bước dựng thì xóa staging/tmp, bản gốc còn nguyên.
- **Hết bộ nhớ khi giải mã ảnh:** `decodeSafely` bắt `OutOfMemoryError`, tăng sample, tối đa 3 lần. Thất bại thì báo `error_edit` / `error_compress` / `error_merge`, không crash.
- **Hết dung lượng máy / IOException:** báo lỗi tương ứng, staging bị xóa.
- **File trang bị mất hoặc không phải JPEG chuẩn** (PNG, CMYK): `ensureSupportedJpeg` mã hóa lại; không đọc được thì báo lỗi, không ghi đè bản gốc.
- **Rỗng:** Files không có tài liệu thì ẩn nút Select. Thư mục rỗng thì hiện `folder_empty`. Chưa có thư mục thì chỉ có chip "All" + "New folder". `MoveToFolderSheet` không có thư mục thì chỉ còn "No folder" + "New folder".
- **Rất dài:** tên thư mục tối đa 40 ký tự, chip ellipsis `widthIn(max = 160.dp)`. Tên tài liệu dài trong hộp thoại gộp và trong `printJobFileName` đều được cắt. Tối đa 30 trang trong màn Sửa trang (LazyColumn).
- **Xoay màn hình:** nháp thứ tự trang nằm trong `SavedStateHandle`. Chọn nhiều nằm trong `MainViewModel`. Cờ hộp thoại/sheet và mức nén dùng `rememberSaveable`. Overlay `working` nằm trong ViewModel. Sự kiện chia sẻ đi qua `Channel` nên không mất khi tạo lại màn hình.
- **Chế độ tối:** app luôn dùng giao diện sáng, nhưng vẫn chỉ dùng màu `MaterialTheme.colorScheme` / `Gradients` / `Accent`, không gõ cứng màu mới.
- **Cỡ chữ 200%:** dùng `heightIn(min)`, không đặt chiều cao cố định cho hàng/nút có chữ. Nhãn dưới nút tròn được xuống 2 dòng. Thanh chọn nhiều không cắt chữ (nhãn `maxLines = 2`).
- **Quyền bị từ chối:** không xin quyền nào mới. Không có app in hoặc dịch vụ in thì `printPdf` trả false và hiện `error_print`.
- **Mất mạng:** mọi chức năng chạy offline. Trạng thái Pro dùng DataStore như hiện nay.
- **Giới hạn trang:** thêm trang thì cắt theo `pagesCanAdd`. Bản free đã đủ 5 trang thì mở màn mua. Gộp quá 30 trang thì báo `merge_too_many`. Bản free bấm Gộp thì mở màn mua (không báo lỗi).
- **Hết lượt xuất:** In / Nén bị chặn trước khi làm, rồi mở màn mua. Chia sẻ nhiều cần đủ n lượt.
- **Chọn nhiều mà tài liệu bị xóa ở nơi khác:** `pruneSelection`. Thư mục đang lọc bị xóa thì quay về "All".
- **Chạm nhanh hai lần** (Lưu / Nén / Gộp): nút tắt khi `working`/`saving`/`busy`.
- **Tài liệu không còn khi mở màn Sửa trang:** quay lại. Số trang đổi giữa chừng khiến `isValidOrder` sai thì báo `error_edit`.
- **Ảnh thu nhỏ cũ sau khi sắp trang:** `evictThumbnails(live)` + `stamp = revision`.

## Quy ước bám theo
- Màn hình: `…/ui/doc/DocScreen.kt` (header, `ActionCircle`, AlertDialog, snackbar + events), `…/ui/ocr/OcrPickSheet.kt` (ModalBottomSheet), `…/ui/files/FilesScreen.kt` (`SortChip`, `docGroup`), `…/ui/AppRoot.kt` (`OcrLockedScreen` cho thanh trên, `popIfOn`).
- ViewModel: `…/ui/doc/DocViewModel.kt` (Channel events, `combine…stateIn`, bắt `CancellationException` riêng), `…/ui/main/MainViewModel.kt`.
- Repository: `…/data/DocRepository.kt` (`create`, `copy`, `importImage`, `withContext(Dispatchers.IO)`).
- Hàm thuần + test: `…/data/FreeLimits.kt` ↔ `test/data/FreeLimitsTest.kt`, `…/data/DocList.kt` ↔ `test/data/DocListTest.kt`, `test/data/DocTest.kt` (JUnit 4, `org.junit.Assert.*`).

## Test cần có
Tất cả chạy JVM, JUnit 4, không cần Android.
- `PageOpsTest`: `moveUp` ở giữa đổi chỗ đúng; `moveUp(…, 0)` và `moveDown` ở hàng cuối trả nguyên; `removePage` giữ thứ tự còn lại; **ca phải thất bại:** `removePage(listOf(1), 0)` trả nguyên (không cho rỗng); `isValidOrder` sai với danh sách rỗng, trùng, số 0, số > pageCount, đúng với tập con đảo thứ tự; `toggleSelection` thêm vào cuối rồi bỏ ra; `pruneSelection`; `orderedSelection` giữ thứ tự chọn và bỏ id mất; `checkMerge`: 1 tài liệu → NeedTwo, 2 tài liệu tổng 30 → Ok(30), tổng 31 → TooMany(31).
- `FreeLimitsTest` (thêm): `pagesCanAdd(false, 3) == 2`, `(false, 5) == 0`, `(false, 8) == 0` (không âm), `(true, 29) == 1`; `canConsume(false, Usage(today, 1), today, 2) == true`, `…3) == false`; Pro luôn true; ngày mới thì đếm lại; `consumeMany(Usage(today-1, 3), today, 2) == Usage(today, 2)`.
- `CompressTest`: `scaledSize(4000, 3000, 1200) == (1200, 900)`; dọc `(1500, 3000, 1800) == (900, 1800)`; nhỏ hơn max thì giữ nguyên; ảnh rất hẹp thì cạnh ≥ 1; **thất bại:** `scaledSize(0, 10, 1200)` ném `IllegalArgumentException`; `shouldShareOriginal(100, 100) == true`, `(100, 99) == false`.
- `JpegInfoTest` (tự dựng mảng byte): SOI + APP0 + SOF0 (3 kênh, 640×480) → đúng; SOF2 1 kênh → components 1; có byte đệm 0xFF trước marker vẫn đọc được; không bắt đầu bằng FFD8 → null; bị cụt giữa SOF → null; gặp SOS trước SOF → null; SOF với cao = 0 → null.
- `JpegPdfWriterTest` (JPEG giả = SOI + SOF0 + vài byte + EOI): 2 trang thì output bắt đầu `%PDF-1.4`, kết thúc `%%EOF\n`, chứa `/Count 2`, chứa nguyên byte của từng JPEG; mỗi offset trong bảng xref trỏ đúng tới `"{n} 0 obj"`; `startxref` trỏ đúng `xref`; `pageHeight(1000, 2000) == 1190`; **thất bại:** danh sách rỗng ném IAE; JPEG 4 kênh ném IAE; byte không phải JPEG ném IAE.
- `FolderTest`: `checkFolderName("  ", …) == Empty`; trùng "Work" với "work" → Taken; đổi tên thư mục thành chính tên nó (editingId trùng) → Ok; 60 ký tự → Ok với độ dài 40; `filterByFolder(docs, null)` giữ hết, `filterByFolder(docs, 2)` chỉ còn tài liệu thư mục 2.
- `EditRecoveryTest`: mỗi dòng của bảng quyết định một ca: `(true, *, true) == FINISH_COMMIT` (thử cả new true/false); `(false, true, true) == PROMOTE_NEW`; `(false, false, true) == RESTORE_OLD`; `(true, true, false) == DELETE_NEW`; `(false, true, false) == DELETE_NEW`; `(true, false, false) == NOTHING`; `(false, false, false) == NOTHING`.
- `SchemaSqlTest`: `MIGRATION_1_2_SQL.size == 2`; có câu bắt đầu `ALTER TABLE \`docs\` ADD COLUMN \`folderId\` INTEGER`; có `CREATE TABLE IF NOT EXISTS \`folders\``; không câu nào chứa `DROP` hay `DELETE`.
- `PrintNameTest`: `printJobFileName("Hóa đơn 10/2026") == "Hóa đơn 10_2026.pdf"`; rỗng/khoảng trắng → `"document.pdf"`; tên 200 ký tự thì phần tên dài 80.
- Migration thật trên SQLite không chạy được trên JVM (không thêm thư viện test). Phải thử tay ở DUYỆT-2: cài APK bản cũ, tạo 2–3 tài liệu, rồi cài đè APK mới (cùng khóa debug cố định trên CI) và kiểm tra tài liệu còn đủ.

## Ảnh hưởng chính sách
Không có. Không thêm quyền trong manifest, không thêm SDK, không gửi dữ liệu mới đi đâu. Tên thư mục chỉ lưu trong Room trên máy, giống tài liệu. In và chia sẻ chỉ diễn ra khi người dùng chủ động bấm, qua hộp thoại của hệ thống. Không cần sửa `docs/snap-sheet/privacy-policy.html` hay khai lại Data safety.

## Ngoài phạm vi
- Toàn bộ ĐỢT 2: ký tên, watermark, nhập PDF có sẵn, quét QR/mã vạch.
- Kéo thả trang, xoay trang, cắt lại trang, sửa trang đơn lẻ.
- Thư mục lồng nhau, màu/icon thư mục, sắp xếp thư mục thủ công, thư mục ở Home hoặc OcrPickSheet.
- Đổi `PdfBuilder` hoặc luồng `saveScan`/`saveImages` hiện có. Đổi tên file khi chia sẻ (vẫn là `doc.pdf` / `compressed.pdf`).
- Đổi giới hạn free/Pro, product ID billing, Firebase, CI, keystore, `versionCode`. Thêm thư viện hoặc nâng version.
- Chọn nhiều ở Home. Nén ảnh khi chia sẻ ảnh (chỉ nén PDF). Lưu bản nén thành tài liệu mới.
- Đổi bố cục các màn đã duyệt ngoài những chỗ đã nêu.
