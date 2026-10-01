package com.ethanstudio.snapsheet

import android.app.Application
import com.ethanstudio.snapsheet.billing.BillingManager
import com.ethanstudio.snapsheet.data.AppDatabase
import com.ethanstudio.snapsheet.data.DocRepository
import com.ethanstudio.snapsheet.data.ProStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Giữ các đối tượng dùng chung (thay cho thư viện DI vì app nhỏ). */
class SnapSheetApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val docs: DocRepository by lazy { DocRepository(this, AppDatabase.create(this).docDao()) }
    val proStore: ProStore by lazy { ProStore(this) }
    val billing: BillingManager by lazy {
        BillingManager(this, appScope) { isPro, kind -> proStore.setPro(isPro, kind) }
    }

    override fun onCreate() {
        super.onCreate()
        billing.start()
    }
}
