package com.ethanstudio.snapsheet

import android.app.Application
import com.ethanstudio.snapsheet.auth.AuthRepository
import com.ethanstudio.snapsheet.billing.BillingManager
import com.ethanstudio.snapsheet.data.AppDatabase
import com.ethanstudio.snapsheet.data.DocRepository
import com.ethanstudio.snapsheet.data.ProStore
import com.ethanstudio.snapsheet.data.SessionStore
import com.ethanstudio.snapsheet.data.SignatureStore
import com.ethanstudio.snapsheet.i18n.AppLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Giữ các đối tượng dùng chung (thay cho thư viện DI vì app nhỏ). */
class SnapSheetApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val database: AppDatabase by lazy { AppDatabase.create(this) }
    val docs: DocRepository by lazy { DocRepository(this, database.docDao(), database.folderDao()) }
    val proStore: ProStore by lazy { ProStore(this) }
    val session: SessionStore by lazy { SessionStore(this) }
    val signatures: SignatureStore by lazy { SignatureStore(this) }
    val auth: AuthRepository by lazy { AuthRepository(this) }
    val billing: BillingManager by lazy {
        BillingManager(this, appScope) { isPro, kind -> proStore.setPro(isPro, kind) }
    }

    override fun onCreate() {
        super.onCreate()
        AppLocale.migrateToSystem(this)
        billing.start()
        // Hoàn tất hoặc dọn các lần sửa trang bị ngắt giữa chừng (tắt app, hết pin).
        appScope.launch(Dispatchers.IO) { runCatching { docs.recoverPendingEdits() } }
    }
}
