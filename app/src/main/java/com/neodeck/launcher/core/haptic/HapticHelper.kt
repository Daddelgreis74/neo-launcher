package com.neodeck.launcher.core.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import java.lang.ref.WeakReference

class HapticHelper(private val context: Context) {

    private var targetViewRef: WeakReference<View>? = null

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun attachView(view: View) {
        targetViewRef = WeakReference(view)
    }

    fun performClick(enabled: Boolean = true) {
        if (!enabled) return

        // 1. Try View-based system haptic feedback first (native OS integration)
        val view = targetViewRef?.get()
        val viewHandled = view?.let {
            val flags = HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, flags) ||
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, flags)
        } ?: false

        if (viewHandled) return

        // 2. Hardware Vibrator fallback
        vibrateInternal(
            durationMs = 25L,
            amplitude = 180,
            predefinedEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_CLICK else null
        )
    }

    fun performTick(enabled: Boolean = true) {
        if (!enabled) return

        val view = targetViewRef?.get()
        val viewHandled = view?.let {
            val flags = HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK, flags)
        } ?: false

        if (viewHandled) return

        vibrateInternal(
            durationMs = 15L,
            amplitude = 120,
            predefinedEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK else null
        )
    }

    fun performHeavyClick(enabled: Boolean = true) {
        if (!enabled) return

        val view = targetViewRef?.get()
        val viewHandled = view?.let {
            val flags = HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, flags)
        } ?: false

        if (viewHandled) return

        vibrateInternal(
            durationMs = 45L,
            amplitude = 255,
            predefinedEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_HEAVY_CLICK else null
        )
    }

    private fun vibrateInternal(durationMs: Long, amplitude: Int, predefinedEffect: Int?) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (predefinedEffect != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    vib.areAllEffectsSupported(predefinedEffect) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES
                ) {
                    VibrationEffect.createPredefined(predefinedEffect)
                } else {
                    val safeAmp = amplitude.coerceIn(1, 255)
                    VibrationEffect.createOneShot(durationMs, safeAmp)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attributes = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_TOUCH)
                        .build()
                    vib.vibrate(effect, attributes)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
