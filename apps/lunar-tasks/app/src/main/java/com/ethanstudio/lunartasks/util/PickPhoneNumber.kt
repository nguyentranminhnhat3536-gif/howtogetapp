package com.ethanstudio.lunartasks.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.result.contract.ActivityResultContract

/**
 * Mở danh bạ để chọn MỘT số điện thoại. App chỉ được đọc đúng số người dùng chọn,
 * nên không cần quyền READ_CONTACTS.
 */
class PickPhoneNumber : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK).setType(Phone.CONTENT_TYPE)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

/** Đọc tên và số của mục vừa chọn. Trả về null nếu không đọc được. */
fun Context.readPickedPhone(uri: Uri): Pair<String, String>? =
    try {
        contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) (cursor.getString(0).orEmpty()) to (cursor.getString(1).orEmpty()) else null
        }
    } catch (e: SecurityException) {
        null
    }
