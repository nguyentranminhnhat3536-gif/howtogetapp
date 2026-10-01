package com.example.factorytemplate.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Mở một link. Trả về false nếu máy không có app nào mở được. */
fun Context.openUrl(url: String): Boolean =
    startSafely(Intent(Intent.ACTION_VIEW, url.toUri()))

/** Mở trang của app trên Google Play (để người dùng đánh giá). */
fun Context.openStoreListing(): Boolean =
    startSafely(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())) ||
        openUrl("https://play.google.com/store/apps/details?id=$packageName")

/** Mở app email, điền sẵn người nhận và tiêu đề. */
fun Context.sendFeedbackEmail(email: String, subject: String): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    return startSafely(intent)
}

private fun Context.startSafely(intent: Intent): Boolean =
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
