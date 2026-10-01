package com.ethanstudio.snapsheet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.SnapSheetApp
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.scan.ScanActions
import com.ethanstudio.snapsheet.scan.ScanMode
import com.ethanstudio.snapsheet.scan.rememberScanActions
import com.ethanstudio.snapsheet.ui.account.AccountScreen
import com.ethanstudio.snapsheet.ui.common.AppViewModels
import com.ethanstudio.snapsheet.ui.doc.DocScreen
import com.ethanstudio.snapsheet.ui.doc.DocViewModel
import com.ethanstudio.snapsheet.ui.files.FilesScreen
import com.ethanstudio.snapsheet.ui.home.HomeScreen
import com.ethanstudio.snapsheet.ui.main.MainEvent
import com.ethanstudio.snapsheet.ui.main.MainViewModel
import com.ethanstudio.snapsheet.ui.paywall.PaywallScreen
import com.ethanstudio.snapsheet.ui.tools.ToolsScreen
import com.ethanstudio.snapsheet.util.findActivity
import com.ethanstudio.snapsheet.util.openStoreListing
import com.ethanstudio.snapsheet.util.openUrl
import com.ethanstudio.snapsheet.util.sendFeedbackEmail
import com.ethanstudio.snapsheet.util.shareText
import java.io.File

private const val ROUTE_MAIN = "main"
private const val ROUTE_DOC = "doc/{id}"
private const val ROUTE_PAYWALL = "paywall"

/** Khung chung của app: điều hướng giữa tab chính, màn tài liệu và màn mua Pro. */
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as SnapSheetApp
    val vm: MainViewModel = viewModel(factory = AppViewModels.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val billing by vm.billingState.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }

    val prefix = stringResource(R.string.scan_default_name)
    val actions = rememberScanActions(
        pageLimit = FreeLimits.pageLimit(state.pro.isPro),
        onScanned = { pages, pdf -> vm.saveScan(pages, pdf, prefix) },
        onPhotos = { vm.savePhotos(it, prefix) },
        onError = { vm.message(R.string.error_scan) },
    )

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is MainEvent.OpenDoc -> nav.navigate("doc/${event.id}")
                is MainEvent.Message -> snackbar.showSnackbar(context.getString(event.res))
                MainEvent.PurchaseDone -> if (nav.currentDestination?.route == ROUTE_PAYWALL) nav.popBackStack()
            }
        }
    }

    fun pageFile(id: Long): File = File(app.docs.dir(id), "page_1.jpg")
    fun report(ok: Boolean) { if (!ok) vm.message(R.string.error_no_app) }
    val appName = stringResource(R.string.app_name)
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val supportEmail = stringResource(R.string.support_email)
    val feedbackSubject = stringResource(R.string.feedback_subject, appName)
    val shareLabel = stringResource(R.string.share_chooser)
    val shareBody = stringResource(R.string.share_app_text, "https://play.google.com/store/apps/details?id=${context.packageName}")
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = ROUTE_MAIN) {
            composable(ROUTE_MAIN) {
                MainTabs(
                    tab = tab,
                    onTab = { tab = it },
                    onScanButton = { sheetOpen = true },
                ) { padding ->
                    when (tab) {
                        0 -> HomeScreen(state, actions, ::pageFile, { nav.navigate("doc/$it") }, { tab = 1 }, padding)
                        1 -> FilesScreen(state, vm::setQuery, ::pageFile, { nav.navigate("doc/$it") }, padding)
                        2 -> ToolsScreen(state.pro.isPro, actions, padding)
                        else -> AccountScreen(
                            state = state,
                            version = version,
                            onGetPro = { nav.navigate(ROUTE_PAYWALL) },
                            onRestore = vm::restore,
                            onRate = { report(context.openStoreListing()) },
                            onShareApp = { report(context.shareText(shareBody, shareLabel)) },
                            onContact = { report(context.sendFeedbackEmail(supportEmail, feedbackSubject)) },
                            onPrivacy = { report(context.openUrl(privacyUrl)) },
                            modifier = padding,
                        )
                    }
                }
            }
            composable(ROUTE_DOC, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                val docViewModel: DocViewModel = viewModel(factory = AppViewModels.Factory)
                DocScreen(docViewModel, onBack = { nav.popBackStack() }, onNeedPro = { nav.navigate(ROUTE_PAYWALL) })
            }
            composable(ROUTE_PAYWALL) {
                PaywallScreen(
                    billing = billing,
                    onClose = { nav.popBackStack() },
                    onBuy = { plan -> context.findActivity()?.let { vm.buy(it, plan) } },
                    onRestore = vm::restore,
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp))
        if (state.saving) SavingOverlay()
    }

    if (sheetOpen) {
        ScanSheet(
            onCamera = { sheetOpen = false; actions.scan(ScanMode.BATCH) },
            onPhotos = { sheetOpen = false; actions.importPhotos() },
            onDismiss = { sheetOpen = false },
        )
    }
}

@Composable
private fun SavingOverlay() {
    Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator()
                Text(stringResource(R.string.saving), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun MainTabs(
    tab: Int,
    onTab: (Int) -> Unit,
    onScanButton: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { SnapBottomBar(tab, onTab, onScanButton) },
    ) { padding -> content(Modifier.padding(padding)) }
}

/** Thanh điều hướng dưới: 4 mục và nút + tròn có vòng sáng ở giữa. */
@Composable
private fun SnapBottomBar(selected: Int, onSelect: (Int) -> Unit, onScan: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth()) {
        Surface(Modifier.padding(top = 26.dp).fillMaxWidth(), color = colors.surface) {
            Column {
                HorizontalDivider(color = colors.outlineVariant)
                Row(Modifier.fillMaxWidth().height(64.dp)) {
                    NavItem(R.drawable.ic_home, R.string.nav_home, selected == 0) { onSelect(0) }
                    NavItem(R.drawable.ic_folder, R.string.nav_files, selected == 1) { onSelect(1) }
                    Spacer(Modifier.weight(1f))
                    NavItem(R.drawable.ic_tools, R.string.nav_tools, selected == 2) { onSelect(2) }
                    NavItem(R.drawable.ic_person, R.string.nav_account, selected == 3) { onSelect(3) }
                }
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
        Box(
            Modifier.align(Alignment.TopCenter).size(72.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(colors.primary).clickable(role = Role.Button, onClick = onScan),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.nav_scan), Modifier.size(28.dp), tint = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: Int, label: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(icon), null, tint = if (selected) colors.primary else colors.outline)
        Text(
            stringResource(label),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) colors.secondary else colors.onSurfaceVariant,
        )
    }
}

/** Bảng chọn nguồn khi bấm nút +: quét bằng camera hoặc nhập ảnh. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanSheet(onCamera: () -> Unit, onPhotos: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.background,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                stringResource(R.string.sheet_title),
                Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
            )
            SheetRow(R.drawable.ic_scan, R.string.sheet_camera, onCamera)
            SheetRow(R.drawable.ic_photo, R.string.sheet_photos, onPhotos)
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                Text(stringResource(R.string.cancel), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            }
        }
    }
}

@Composable
private fun ColumnScope.SheetRow(icon: Int, label: Int, onClick: () -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).height(54.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.outline)
        Text(stringResource(label), fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
