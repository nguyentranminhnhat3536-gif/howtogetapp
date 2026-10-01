package com.ethanstudio.snapsheet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.SnapSheetApp
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.i18n.AppLocale
import com.ethanstudio.snapsheet.scan.ScanActions
import com.ethanstudio.snapsheet.scan.ScanMode
import com.ethanstudio.snapsheet.scan.rememberScanActions
import com.ethanstudio.snapsheet.ui.account.AccountScreen
import com.ethanstudio.snapsheet.ui.account.LanguageDialog
import com.ethanstudio.snapsheet.ui.account.languageNameRes
import com.ethanstudio.snapsheet.ui.auth.AuthEvent
import com.ethanstudio.snapsheet.ui.auth.AuthViewModel
import com.ethanstudio.snapsheet.ui.auth.CheckEmailScreen
import com.ethanstudio.snapsheet.ui.auth.ForgotPasswordScreen
import com.ethanstudio.snapsheet.ui.auth.SignInScreen
import com.ethanstudio.snapsheet.ui.auth.SignUpScreen
import com.ethanstudio.snapsheet.ui.auth.WelcomeScreen
import com.ethanstudio.snapsheet.ui.common.AppViewModels
import com.ethanstudio.snapsheet.ui.doc.DocScreen
import com.ethanstudio.snapsheet.ui.doc.DocViewModel
import com.ethanstudio.snapsheet.ui.files.FilesScreen
import com.ethanstudio.snapsheet.ui.home.HomeScreen
import com.ethanstudio.snapsheet.ui.main.MainEvent
import com.ethanstudio.snapsheet.ui.main.MainViewModel
import com.ethanstudio.snapsheet.ui.ocr.OcrLockedContent
import com.ethanstudio.snapsheet.ui.ocr.OcrPickSheet
import com.ethanstudio.snapsheet.ui.ocr.OcrScreen
import com.ethanstudio.snapsheet.ui.ocr.OcrViewModel
import com.ethanstudio.snapsheet.ui.paywall.PaywallScreen
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients
import com.ethanstudio.snapsheet.ui.theme.NavMuted
import com.ethanstudio.snapsheet.ui.tools.ToolsScreen
import com.ethanstudio.snapsheet.util.findActivity
import com.ethanstudio.snapsheet.util.openEmailApp
import com.ethanstudio.snapsheet.util.openStoreListing
import com.ethanstudio.snapsheet.util.openUrl
import com.ethanstudio.snapsheet.util.sendFeedbackEmail
import com.ethanstudio.snapsheet.util.shareText
import java.io.File

private const val ROUTE_MAIN = "main"
private const val ROUTE_DOC = "doc/{id}"
private const val ROUTE_PAYWALL = "paywall"
private const val ROUTE_OCR = "ocr/{id}"
private const val ROUTE_OCR_LOCKED = "ocr-locked"
private const val ROUTE_WELCOME = "welcome"
private const val ROUTE_SIGN_IN = "signin"
private const val ROUTE_SIGN_UP = "signup"
private const val ROUTE_CHECK_EMAIL = "check-email"
private const val ROUTE_FORGOT = "forgot"

/** Chỉ quay lại khi vẫn đang ở [route]: bấm Back 2 lần liền hoặc sự kiện tới muộn không làm rỗng ngăn màn hình. */
private fun NavHostController.popIfOn(route: String) {
    if (currentDestination?.route == route) popBackStack()
}

/** Khung chung của app: điều hướng giữa tab chính, màn tài liệu và màn mua Pro. */
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as SnapSheetApp
    val vm: MainViewModel = viewModel(factory = AppViewModels.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val billing by vm.billingState.collectAsStateWithLifecycle()
    val authVm: AuthViewModel = viewModel(factory = AppViewModels.Factory)
    val authState by authVm.state.collectAsStateWithLifecycle()
    val user by authVm.user.collectAsStateWithLifecycle()
    val onboarded by authVm.onboarded.collectAsStateWithLifecycle(initialValue = null)
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var languageOpen by rememberSaveable { mutableStateOf(false) }
    var ocrPickOpen by rememberSaveable { mutableStateOf(false) }
    // Đọc lại mỗi khi cấu hình đổi (Activity được tạo lại sau khi đổi ngôn ngữ).
    val configuration = LocalConfiguration.current
    val currentLanguage = remember(configuration) { AppLocale.current(context) }
    val languageLabel = languageNameRes(currentLanguage)?.let { stringResource(it) } ?: stringResource(R.string.language_system)

    val prefix = stringResource(R.string.scan_default_name)
    val actions = rememberScanActions(
        pageLimit = FreeLimits.pageLimit(state.pro.isPro),
        onScanned = { pages, pdf -> vm.saveScan(pages, pdf, prefix) },
        onPhotos = { vm.savePhotos(it, prefix) },
        onError = {
            vm.clearOcrAfterScan()
            vm.message(R.string.error_scan)
        },
        onCanceled = vm::clearOcrAfterScan,
    )

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is MainEvent.OpenDoc -> {
                    nav.navigate("doc/${event.id}")
                    if (event.extract) nav.navigate("ocr/${event.id}")
                }
                is MainEvent.Message -> snackbar.showSnackbar(context.getString(event.res))
                MainEvent.PurchaseDone -> nav.popIfOn(ROUTE_PAYWALL)
            }
        }
    }

    LaunchedEffect(authVm) {
        authVm.events.collect { event ->
            when (event) {
                AuthEvent.EnterApp -> nav.navigate(ROUTE_MAIN) { popUpTo(nav.graph.id) { inclusive = true } }
                AuthEvent.VerificationSent -> nav.navigate(ROUTE_CHECK_EMAIL) { popUpTo(nav.graph.id) { inclusive = true } }
                AuthEvent.ResetSent -> {
                    nav.popIfOn(ROUTE_FORGOT)
                    snackbar.showSnackbar(context.getString(R.string.auth_reset_sent))
                }
                AuthEvent.ResentVerification -> snackbar.showSnackbar(context.getString(R.string.auth_resent))
                AuthEvent.SignedOut -> snackbar.showSnackbar(context.getString(R.string.account_signed_out))
                AuthEvent.Deleted -> snackbar.showSnackbar(context.getString(R.string.account_deleted))
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

    // Chờ đọc xong "đã qua màn chào chưa" rồi mới chọn màn đầu tiên.
    val wasOnboarded = onboarded ?: run {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val start = remember { if (wasOnboarded || user != null) ROUTE_MAIN else ROUTE_WELCOME }

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = start) {
            composable(ROUTE_WELCOME) {
                WelcomeScreen(
                    onSignIn = { nav.navigate(ROUTE_SIGN_IN) },
                    onSignUp = { nav.navigate(ROUTE_SIGN_UP) },
                    onGoogle = { context.findActivity()?.let(authVm::signInWithGoogle) },
                    onSkip = authVm::continueWithoutAccount,
                    busy = authState.busy,
                    error = authState.error,
                    errorCode = authState.errorCode,
                )
            }
            composable(ROUTE_SIGN_IN) {
                SignInScreen(authVm, onForgot = { nav.navigate(ROUTE_FORGOT) }, onSignUp = { nav.navigate(ROUTE_SIGN_UP) { popUpTo(ROUTE_SIGN_IN) { inclusive = true } } })
            }
            composable(ROUTE_SIGN_UP) {
                SignUpScreen(
                    authVm,
                    onSignIn = { nav.navigate(ROUTE_SIGN_IN) { popUpTo(ROUTE_SIGN_UP) { inclusive = true } } },
                    onPrivacy = { report(context.openUrl(privacyUrl)) },
                )
            }
            composable(ROUTE_CHECK_EMAIL) {
                CheckEmailScreen(
                    authVm,
                    onOpenEmail = { report(context.openEmailApp()) },
                    onContinue = {
                        authVm.refreshUser()
                        nav.navigate(ROUTE_MAIN) { popUpTo(nav.graph.id) { inclusive = true } }
                    },
                )
            }
            composable(ROUTE_FORGOT) {
                ForgotPasswordScreen(authVm, onBack = { nav.popIfOn(ROUTE_FORGOT) })
            }
            composable(ROUTE_MAIN) {
                MainTabs(
                    tab = tab,
                    onTab = { tab = it },
                    onScanButton = { sheetOpen = true },
                ) { padding ->
                    when (tab) {
                        0 -> HomeScreen(
                            state = state,
                            userName = user?.shownName,
                            actions = actions,
                            pageFile = ::pageFile,
                            onOpenDoc = { nav.navigate("doc/$it") },
                            onSeeAll = { tab = 1 },
                            onScanCard = { sheetOpen = true },
                            onAccount = { tab = 3 },
                            modifier = padding,
                        )
                        1 -> FilesScreen(
                            state = state,
                            onQuery = vm::setQuery,
                            onSort = vm::setSort,
                            pageFile = ::pageFile,
                            pdfFile = { app.docs.pdfFile(it) },
                            onOpenDoc = { nav.navigate("doc/$it") },
                            modifier = padding,
                        )
                        2 -> ToolsScreen(
                            isPro = state.pro.isPro,
                            actions = actions,
                            onOcr = {
                                if (state.pro.isPro) ocrPickOpen = true else nav.navigate(ROUTE_OCR_LOCKED)
                            },
                            onShare = {
                                if (state.allDocs.isEmpty()) {
                                    vm.message(R.string.tool_need_doc)
                                } else {
                                    tab = 1
                                    vm.message(R.string.tool_pick_share)
                                }
                            },
                            modifier = padding,
                        )
                        else -> AccountScreen(
                            state = state,
                            user = user,
                            authBusy = authState.busy,
                            authError = authState.error,
                            version = version,
                            onSignIn = { nav.navigate(ROUTE_SIGN_IN) },
                            onGetPro = { nav.navigate(ROUTE_PAYWALL) },
                            onRestore = vm::restore,
                            onRate = { report(context.openStoreListing()) },
                            onShareApp = { report(context.shareText(shareBody, shareLabel)) },
                            onContact = { report(context.sendFeedbackEmail(supportEmail, feedbackSubject)) },
                            onPrivacy = { report(context.openUrl(privacyUrl)) },
                            onResend = authVm::resendVerification,
                            onSignOut = authVm::signOut,
                            onDelete = authVm::deleteAccount,
                            languageLabel = languageLabel,
                            onLanguage = { languageOpen = true },
                            modifier = padding,
                        )
                    }
                }
            }
            composable(ROUTE_DOC, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                val docViewModel: DocViewModel = viewModel(factory = AppViewModels.Factory)
                DocScreen(
                    docViewModel,
                    onBack = { nav.popIfOn(ROUTE_DOC) },
                    onNeedPro = { nav.navigate(ROUTE_PAYWALL) },
                    onOpenOcr = { nav.navigate("ocr/$it") },
                )
            }
            composable(ROUTE_OCR, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                val ocrViewModel: OcrViewModel = viewModel(factory = AppViewModels.Factory)
                OcrScreen(
                    ocrViewModel,
                    onBack = { nav.popIfOn(ROUTE_OCR) },
                    onSeePlans = { nav.navigate(ROUTE_PAYWALL) },
                    onScanAgain = {
                        nav.popIfOn(ROUTE_OCR)
                        vm.scanForOcr()
                        actions.scan(ScanMode.BATCH)
                    },
                )
            }
            composable(ROUTE_OCR_LOCKED) {
                LaunchedEffect(state.pro.isPro) {
                    if (state.pro.isPro) {
                        nav.popIfOn(ROUTE_OCR_LOCKED)
                        ocrPickOpen = true
                    }
                }
                OcrLockedScreen(
                    onBack = { nav.popIfOn(ROUTE_OCR_LOCKED) },
                    onSeePlans = { nav.navigate(ROUTE_PAYWALL) },
                )
            }
            composable(ROUTE_PAYWALL) {
                PaywallScreen(
                    billing = billing,
                    onClose = { nav.popIfOn(ROUTE_PAYWALL) },
                    onBuy = { plan -> context.findActivity()?.let { vm.buy(it, plan) } },
                    onRestore = vm::restore,
                    onPrivacy = { report(context.openUrl(privacyUrl)) },
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
    if (ocrPickOpen) {
        OcrPickSheet(
            docs = state.allDocs,
            pageFile = ::pageFile,
            onScanNew = {
                ocrPickOpen = false
                vm.scanForOcr()
                actions.scan(ScanMode.BATCH)
            },
            onPick = {
                ocrPickOpen = false
                nav.navigate("ocr/$it")
            },
            onDismiss = { ocrPickOpen = false },
        )
    }
    if (languageOpen) {
        LanguageDialog(
            current = currentLanguage,
            onPick = { tag ->
                languageOpen = false
                context.findActivity()?.let { AppLocale.apply(it, tag) }
            },
            onDismiss = { languageOpen = false },
        )
    }
}

/** Extract text mở từ Tools khi chưa có Pro: đầu trang có nút quay lại, bên dưới là bảng PRO. */
@Composable
private fun OcrLockedScreen(onBack: () -> Unit, onSeePlans: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(colors.background).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_back), stringResource(R.string.doc_back), tint = colors.onSurface)
                }
                Text(
                    stringResource(R.string.doc_extract),
                    Modifier.weight(1f).padding(horizontal = 4.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            OcrLockedContent(onSeePlans = onSeePlans, onNotNow = onBack, modifier = Modifier.fillMaxSize())
        }
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
        Surface(Modifier.padding(top = 28.dp).fillMaxWidth(), color = colors.surface) {
            Column {
                HorizontalDivider(color = colors.outlineVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
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
            Modifier.align(Alignment.TopCenter).size(76.dp).clip(CircleShape).background(Gradients.ButtonStart.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(58.dp).clip(CircleShape).background(Gradients.Primary).clickable(role = Role.Button, onClick = onScan),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.nav_scan), Modifier.size(28.dp), tint = Color.White)
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: Int, label: Int, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) Accent else NavMuted
    Column(
        Modifier.weight(1f).heightIn(min = 64.dp).clickable(role = Role.Tab, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(icon), null, tint = tint)
        Text(
            stringResource(label),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = tint,
            maxLines = 1,
        )
    }
}

/** Bảng chọn nguồn khi bấm nút + hoặc thẻ "Scan document": quét bằng camera hoặc nhập ảnh. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanSheet(onCamera: () -> Unit, onPhotos: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.sheet_start), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            SheetCard(
                icon = R.drawable.ic_scan,
                title = R.string.sheet_camera,
                sub = R.string.sheet_camera_sub,
                primary = true,
                onClick = onCamera,
            )
            SheetCard(
                icon = R.drawable.ic_photo,
                title = R.string.sheet_photos,
                sub = R.string.sheet_photos_sub,
                primary = false,
                onClick = onPhotos,
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.cancel), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** Một lựa chọn trong bảng Scan. [primary]: thẻ nền gradient xanh chữ trắng; không thì thẻ trắng viền mảnh. */
@Composable
private fun SheetCard(icon: Int, title: Int, sub: Int, primary: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val base = Modifier.fillMaxWidth().clip(shape)
    val background = if (primary) base.background(Gradients.Primary) else base.background(colors.surface).border(1.dp, colors.outlineVariant, shape)
    Row(
        background.clickable(role = Role.Button, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val iconBox = Modifier.size(50.dp)
        Box(
            if (primary) iconBox.background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)) else iconBox.background(Gradients.soft(), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), null, Modifier.size(26.dp), tint = if (primary) Color.White else Accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(title),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (primary) Color.White else colors.onSurface,
            )
            Text(
                stringResource(sub),
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                color = if (primary) Color.White.copy(alpha = 0.9f) else colors.onSurfaceVariant,
            )
        }
    }
}
