package com.ethanstudio.snapsheet.scan

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.ethanstudio.snapsheet.util.findActivity
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/** Cách quét. [maxPages] là số trang tối đa của riêng cách đó (thẻ căn cước: 2 mặt). */
enum class ScanMode(val maxPages: Int) { SINGLE(1), BATCH(Int.MAX_VALUE), ID_CARD(2) }

/** Hai hành động quét mà giao diện gọi: quét bằng camera và nhập ảnh từ thư viện. */
class ScanActions(val scan: (ScanMode) -> Unit, val importPhotos: () -> Unit)

/**
 * Quét bằng ML Kit Document Scanner (giao diện do Google Play services cung cấp, tự cắt viền, lọc màu;
 * không cần quyền camera). Nhập ảnh dùng trình chọn ảnh của hệ thống (cũng không cần quyền).
 */
@Composable
fun rememberScanActions(
    pageLimit: Int,
    onScanned: (pages: List<Uri>, pdf: Uri?) -> Unit,
    onPhotos: (List<Uri>) -> Unit,
    onError: () -> Unit,
    onCanceled: () -> Unit = {},
): ScanActions {
    val context = LocalContext.current
    val limit by rememberUpdatedState(pageLimit)
    val scanned by rememberUpdatedState(onScanned)
    val photos by rememberUpdatedState(onPhotos)
    val failed by rememberUpdatedState(onError)
    val canceled by rememberUpdatedState(onCanceled)

    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val parsed = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pages = parsed?.pages.orEmpty().map { it.imageUri }
            if (pages.isNotEmpty()) scanned(pages, parsed?.pdf?.uri) else canceled()
        } else {
            canceled()
        }
    }
    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = FreeMaxPick),
    ) { uris -> if (uris.isNotEmpty()) photos(uris.take(limit)) }

    return ScanActions(
        scan = { mode ->
            val activity = context.findActivity()
            if (activity == null) {
                failed()
            } else {
                val options = GmsDocumentScannerOptions.Builder()
                    .setGalleryImportAllowed(true)
                    .setPageLimit(minOf(mode.maxPages, limit))
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                    .build()
                GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
                    .addOnSuccessListener { sender -> scanLauncher.launch(IntentSenderRequest.Builder(sender).build()) }
                    .addOnFailureListener { failed() }
            }
        },
        importPhotos = {
            pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

/** Số ảnh tối đa chọn một lần (bằng giới hạn trang của bản Pro; sau đó cắt theo giới hạn hiện tại). */
private const val FreeMaxPick = 30
