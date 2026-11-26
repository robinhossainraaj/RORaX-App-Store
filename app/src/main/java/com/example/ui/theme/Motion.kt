package com.example.ui.theme

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.CubicBezierEasing

/** Material You motion values used by this app. */
object AppMotion {
    const val Short2 = 100
    const val Short4 = 200
    const val Medium2 = 300
    const val Medium4 = 400
    const val Long1 = 450
    const val Long2 = 500

    const val OvershootTension = 2.0f

    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    private val Overshoot = OvershootInterpolator(OvershootTension)
    val OvershootEasing: (Float) -> Float = { fraction ->
        Overshoot.getInterpolation(fraction)
    }
}
