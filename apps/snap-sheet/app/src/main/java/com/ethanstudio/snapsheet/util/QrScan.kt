package com.ethanstudio.snapsheet.util

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Quét mã QR / mã vạch bằng màn quét của Google Play services (không cần quyền camera, không lưu kết quả).
 * [onResult] nhận chữ của mã (có thể rỗng); [onCanceled] khi người dùng đóng màn quét; [onError] khi không mở được.
 */
fun Context.startQrScan(onResult: (String) -> Unit, onCanceled: () -> Unit, onError: () -> Unit) {
    val activity = findActivity()
    if (activity == null) {
        onError()
        return
    }
    try {
        val options = GmsBarcodeScannerOptions.Builder().enableAutoZoom().build()
        GmsBarcodeScanning.getClient(activity, options)
            .startScan()
            .addOnSuccessListener { barcode -> onResult(barcode.rawValue ?: barcode.displayValue ?: "") }
            .addOnCanceledListener { onCanceled() }
            .addOnFailureListener { onError() }
    } catch (e: RuntimeException) {
        onError()
    }
}
