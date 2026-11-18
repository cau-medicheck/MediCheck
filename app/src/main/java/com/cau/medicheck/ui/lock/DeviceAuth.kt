package com.cau.medicheck.ui.lock

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.cau.medicheck.domain.AppLockMethod

/**
 * 기기의 본인 확인(생체인증·PIN·패턴·비밀번호)을 띄우는 도구. (S41 앱 잠금)
 *
 * 앱이 비밀번호를 따로 받지 않고, 폰의 화면 잠금을 풀 때 쓰는 방법 그대로 확인한다.
 * 새 라이브러리 없이 안드로이드 기본 기능만 쓴다. 안드로이드 버전에 따라 길이 둘이다.
 *   Android 11 이상 : BiometricPrompt — 생체인증과 기기 PIN을 한 창에서 처리한다
 *   Android 8~10   : KeyguardManager의 "기기 잠금 확인" 화면 — 어느 방식을 골라도 PIN·패턴·비밀번호로 확인한다
 */
object DeviceAuth {

    /** 이 버전부터 "생체인증 또는 기기 PIN"을 한 창에서 고를 수 있다 */
    val SUPPORTS_BIOMETRIC_CHOICE: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** 기기에 화면 잠금(PIN·패턴·비밀번호)이 설정돼 있는지. 없으면 본인 확인을 할 방법이 없다. */
    fun isDeviceSecure(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    /**
     * 본인 확인 창을 띄운다.
     * @param done 통과하면 true, 취소·실패하면 false로 **한 번** 불린다 (화면을 그리는 흐름에서)
     */
    fun prompt(
        activity: ComponentActivity,
        method: AppLockMethod,
        title: String,
        subtitle: String,
        done: (Boolean) -> Unit
    ) {
        // 안드로이드가 결과를 두 번 알려 주는 경우가 있어도 done은 한 번만 부른다.
        var finished = false
        val once: (Boolean) -> Unit = { ok ->
            if (!finished) {
                finished = true
                done(ok)
            }
        }
        if (SUPPORTS_BIOMETRIC_CHOICE) {
            promptBiometric(activity, method, title, subtitle, once)
        } else {
            promptDeviceCredential(activity, title, subtitle, once)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun promptBiometric(
        activity: ComponentActivity,
        method: AppLockMethod,
        title: String,
        subtitle: String,
        done: (Boolean) -> Unit
    ) {
        // '기기 생체인증'을 골라도 기기 PIN을 함께 허용한다 — 지문이 등록돼 있지 않거나 인식이 안 될 때 갇히지 않게.
        val allowed = when (method) {
            AppLockMethod.BIOMETRIC ->
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            AppLockMethod.DEVICE_CREDENTIAL -> BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = done(true)

            // 취소했거나, 여러 번 틀려 잠겼거나, 확인할 수단이 없는 경우. (지문 한 번 틀린 것은 창이 그대로라 여기로 오지 않는다)
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = done(false)
        }
        try {
            BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(allowed)
                .build()
                .authenticate(CancellationSignal(), activity.mainExecutor, callback)
        } catch (e: RuntimeException) {
            done(false)   // 드물게 이 기기가 창을 만들지 못하는 경우. 앱이 꺼지지 않게 "확인 못 함"으로 돌린다.
        }
    }

    private fun promptDeviceCredential(
        activity: ComponentActivity,
        title: String,
        subtitle: String,
        done: (Boolean) -> Unit
    ) {
        @Suppress("DEPRECATION")   // Android 10에서 "권장하지 않음" 표시가 붙었지만 8~10에서는 이 방법뿐이다
        val intent: Intent? = activity.getSystemService(KeyguardManager::class.java)
            ?.createConfirmDeviceCredentialIntent(title, subtitle)
        if (intent == null) {
            done(false)
            return
        }
        // "확인 화면에 다녀오면 결과를 알려 달라"고 등록한다. 결과를 받으면 등록을 지운다.
        var launcher: ActivityResultLauncher<Intent>? = null
        launcher = activity.activityResultRegistry.register(
            "device-auth", ActivityResultContracts.StartActivityForResult()
        ) { result ->
            launcher?.unregister()
            done(result.resultCode == Activity.RESULT_OK)
        }
        try {
            launcher.launch(intent)
        } catch (e: RuntimeException) {
            launcher.unregister()
            done(false)
        }
    }
}

/**
 * 지금 화면이 속한 Activity를 찾는다. 못 찾으면 null.
 * Compose의 Context는 Activity를 여러 겹 감싼 것일 수 있어서, 껍질을 하나씩 벗겨 가며 찾는다.
 */
fun Context.findComponentActivity(): ComponentActivity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}
