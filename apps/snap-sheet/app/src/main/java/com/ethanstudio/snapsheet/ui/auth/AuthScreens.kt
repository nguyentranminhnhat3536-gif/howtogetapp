package com.ethanstudio.snapsheet.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.toggleable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.auth.AuthError
import com.ethanstudio.snapsheet.auth.AuthValidation
import com.ethanstudio.snapsheet.util.findActivity

/** Màu của nhóm màn đăng nhập (nền tối, thẻ kính mờ) lấy từ theme tối của Scan Design System. */
private object AuthColors {
    val Ground = Color(0xFF0F1114)
    val Ink = Color(0xFFF4F5F7)
    val InkSoft = Color(0xD1F4F5F7)
    val Primary = Color(0xFF5B96FA)
    val OnPrimary = Color(0xFF07142B)
    val Link = Color(0xFF8DB4FF)
    val Error = Color(0xFFFF8A80)
    val Blob1 = Color(0xFF3880F8)
    val Blob2 = Color(0xFF235FDC)
    val Glass = Color(0x990F1114)
    val Border = Color(0x3DF4F5F7)
    val FieldBorder = Color(0x99F4F5F7)
}

@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.INVALID_EMAIL -> R.string.err_invalid_email
    AuthError.PASSWORD_REQUIRED -> R.string.err_password_required
    AuthError.WEAK_PASSWORD -> R.string.err_weak_password
    AuthError.PASSWORD_MISMATCH -> R.string.err_mismatch
    AuthError.TERMS_REQUIRED -> R.string.err_terms
    AuthError.WRONG_CREDENTIALS -> R.string.err_wrong
    AuthError.EMAIL_IN_USE -> R.string.err_in_use
    AuthError.USER_NOT_FOUND -> R.string.err_not_found
    AuthError.TOO_MANY_REQUESTS -> R.string.err_too_many
    AuthError.NETWORK -> R.string.err_network
    AuthError.RECENT_LOGIN_REQUIRED -> R.string.err_recent
    AuthError.NOT_CONFIGURED -> R.string.err_not_configured
    AuthError.PROVIDER_DISABLED -> R.string.err_provider_disabled
    AuthError.GOOGLE_FAILED -> R.string.err_google
    AuthError.CANCELED, AuthError.UNKNOWN -> R.string.err_unknown
}

/** Nền tối với các khối màu nhòe, thẻ kính mờ ở giữa. Cuộn được và né bàn phím. */
@Composable
private fun AuthBackground(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(AuthColors.Ground)) {
        Box(Modifier.offset(x = (-70).dp, y = 80.dp).size(250.dp).blur(48.dp, BlurredEdgeTreatment.Unbounded).background(AuthColors.Blob1, CircleShape))
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 60.dp, y = (-90).dp).size(230.dp).blur(48.dp, BlurredEdgeTreatment.Unbounded).background(AuthColors.Blob2, RoundedCornerShape(40.dp)))
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
        ) {
            AppMark()
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
                shape = RoundedCornerShape(20.dp),
                color = AuthColors.Glass,
                border = BorderStroke(1.dp, AuthColors.Border),
                contentColor = AuthColors.Ink,
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            }
        }
    }
}

@Composable
private fun AppMark() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(R.drawable.ic_scan), null, Modifier.size(40.dp), tint = AuthColors.Ink)
        Text(stringResource(R.string.app_name_short), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AuthColors.Ink)
    }
}

@Composable
private fun Title(text: String) = Text(text, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = AuthColors.Ink)

@Composable
private fun Body(text: String, center: Boolean = false) =
    Text(text, fontSize = 16.sp, lineHeight = 22.sp, color = AuthColors.InkSoft, textAlign = if (center) TextAlign.Center else TextAlign.Start, modifier = Modifier.fillMaxWidth())

@Composable
private fun ErrorText(error: AuthError?, code: String? = null) {
    if (error != null && error != AuthError.CANCELED) {
        val base = stringResource(error.messageRes())
        val text = if (error == AuthError.UNKNOWN && code != null) stringResource(R.string.err_with_code, base, code) else base
        Text(text, fontSize = 14.sp, lineHeight = 20.sp, color = AuthColors.Error)
    }
}

@Composable
private fun PrimaryButton(text: String, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthColors.Primary,
            contentColor = AuthColors.OnPrimary,
            disabledContainerColor = AuthColors.Primary.copy(alpha = 0.5f),
            disabledContentColor = AuthColors.OnPrimary,
        ),
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(22.dp), color = AuthColors.OnPrimary, strokeWidth = 2.5.dp)
        } else {
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OutlineButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, AuthColors.FieldBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AuthColors.Ink),
    ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AuthColors.Link, textAlign = TextAlign.Center)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AuthColors.Ink,
    unfocusedTextColor = AuthColors.Ink,
    focusedBorderColor = AuthColors.Link,
    unfocusedBorderColor = AuthColors.FieldBorder,
    focusedLabelColor = AuthColors.Link,
    unfocusedLabelColor = AuthColors.InkSoft,
    cursorColor = AuthColors.Link,
    focusedContainerColor = Color(0x0FF4F5F7),
    unfocusedContainerColor = Color(0x0FF4F5F7),
)

@Composable
private fun EmailField(value: String, onChange: (String) -> Unit, imeAction: ImeAction = ImeAction.Next) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.auth_email)) },
        singleLine = true,
        shape = CircleShape,
        colors = fieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, visible: Boolean, onToggle: () -> Unit, imeAction: ImeAction = ImeAction.Done) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = CircleShape,
        colors = fieldColors(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(
                    painterResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                    stringResource(if (visible) R.string.auth_hide_pw else R.string.auth_show_pw),
                    tint = AuthColors.Ink,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OrDivider() {
    Text(stringResource(R.string.auth_or), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 14.sp, color = AuthColors.InkSoft)
}

@Composable
private fun GoogleButton(vm: AuthViewModel, busy: Boolean) {
    val context = LocalContext.current
    OutlineButton(stringResource(R.string.auth_google), enabled = !busy) {
        context.findActivity()?.let { vm.signInWithGoogle(it) }
    }
}

/** Màn chào: đăng nhập, đăng ký, hoặc dùng không cần tài khoản. */
@Composable
fun WelcomeScreen(onSignIn: () -> Unit, onSignUp: () -> Unit, onSkip: () -> Unit) {
    AuthBackground {
        Text(stringResource(R.string.auth_hello), fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, color = AuthColors.Ink)
        Body(stringResource(R.string.auth_welcome_body))
        PrimaryButton(stringResource(R.string.auth_sign_in), busy = false, onClick = onSignIn)
        OutlineButton(stringResource(R.string.auth_sign_up), onClick = onSignUp)
        LinkButton(stringResource(R.string.auth_skip), onSkip, Modifier.fillMaxWidth())
    }
}

@Composable
fun SignInScreen(vm: AuthViewModel, onForgot: () -> Unit, onSignUp: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.clearError() }
    AuthBackground {
        Title(stringResource(R.string.auth_welcome_back))
        EmailField(email, { email = it })
        PasswordField(password, { password = it }, stringResource(R.string.auth_password), visible, { visible = !visible })
        ErrorText(state.error, state.errorCode)
        PrimaryButton(stringResource(R.string.auth_sign_in), state.busy) { vm.signIn(email, password) }
        LinkButton(stringResource(R.string.auth_forgot), onForgot, Modifier.fillMaxWidth())
        OrDivider()
        GoogleButton(vm, state.busy)
        SwitchLine(stringResource(R.string.auth_no_account), stringResource(R.string.auth_sign_up), onSignUp)
    }
}

@Composable
fun SignUpScreen(vm: AuthViewModel, onSignIn: () -> Unit, onPrivacy: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    var agreed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.clearError() }
    AuthBackground {
        Title(stringResource(R.string.auth_create_title))
        EmailField(email, { email = it })
        PasswordField(password, { password = it }, stringResource(R.string.auth_password), visible, { visible = !visible }, ImeAction.Next)
        PasswordField(confirm, { confirm = it }, stringResource(R.string.auth_confirm), visible, { visible = !visible })
        Text(stringResource(R.string.auth_pw_hint, AuthValidation.MIN_PASSWORD), fontSize = 13.sp, color = AuthColors.InkSoft)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = agreed, role = Role.Checkbox, onValueChange = { agreed = it }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = agreed,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = AuthColors.Primary, uncheckedColor = AuthColors.FieldBorder, checkmarkColor = AuthColors.OnPrimary),
            )
            Text(stringResource(R.string.auth_agree), fontSize = 14.sp, lineHeight = 20.sp, color = AuthColors.Ink)
        }
        LinkButton(stringResource(R.string.auth_read_policy), onPrivacy)
        ErrorText(state.error, state.errorCode)
        PrimaryButton(stringResource(R.string.auth_create), state.busy) { vm.signUp(email, password, confirm, agreed) }
        OrDivider()
        GoogleButton(vm, state.busy)
        SwitchLine(stringResource(R.string.auth_have_account), stringResource(R.string.auth_sign_in), onSignIn)
    }
}

/** Sau khi đăng ký: nhắc kiểm tra email xác nhận. Xác nhận không bắt buộc để dùng app. */
@Composable
fun CheckEmailScreen(vm: AuthViewModel, onOpenEmail: () -> Unit, onContinue: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    AuthBackground {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(shape = CircleShape, color = AuthColors.Primary.copy(alpha = 0.2f), modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_mail), null, Modifier.size(32.dp), tint = AuthColors.Link)
                }
            }
        }
        Title(stringResource(R.string.auth_check_title))
        Body(stringResource(R.string.auth_check_body, state.pendingEmail))
        ErrorText(state.error, state.errorCode)
        PrimaryButton(stringResource(R.string.auth_open_email), busy = false, onClick = onOpenEmail)
        OutlineButton(stringResource(R.string.auth_continue), onClick = onContinue)
        LinkButton(stringResource(R.string.auth_resend), { vm.resendVerification() }, Modifier.fillMaxWidth())
    }
}

@Composable
fun ForgotPasswordScreen(vm: AuthViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.clearError() }
    AuthBackground {
        Title(stringResource(R.string.auth_forgot_title))
        Body(stringResource(R.string.auth_forgot_body))
        EmailField(email, { email = it }, ImeAction.Done)
        ErrorText(state.error, state.errorCode)
        PrimaryButton(stringResource(R.string.auth_send_link), state.busy) { vm.sendPasswordReset(email) }
        LinkButton(stringResource(R.string.auth_back), onBack, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SwitchLine(question: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(question, fontSize = 14.sp, color = AuthColors.InkSoft)
        LinkButton(action, onClick)
    }
}
