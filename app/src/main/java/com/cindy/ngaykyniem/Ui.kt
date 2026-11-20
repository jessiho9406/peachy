package com.cindy.ngaykyniem

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator

fun Context.dp(v: Float): Float = v * resources.displayMetrics.density
fun Context.dpi(v: Int): Int = (v * resources.displayMetrics.density).toInt()

/** Hình nền bo tròn tạo bằng code (cho nút, ô nhập, chip). */
fun rounded(ctx: Context, radiusDp: Float, fill: Int, strokeColor: Int = 0, strokeDp: Float = 0f): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = ctx.dp(radiusDp)
        setColor(fill)
        if (strokeDp > 0f) setStroke(ctx.dp(strokeDp).toInt(), strokeColor)
    }

/** Nút/khung kiểu "sticker" GenZ: viền đậm + bóng đổ cứng lệch xuống. */
fun chunky(ctx: Context, fill: Int, radiusDp: Float = 18f, ink: Int = 0xFF24122E.toInt(), shadowDp: Float = 4f): Drawable {
    val off = ctx.dp(shadowDp).toInt()
    val shadow = rounded(ctx, radiusDp, ink)
    val face = rounded(ctx, radiusDp, fill, ink, 2f)
    val ld = LayerDrawable(arrayOf<Drawable>(shadow, face))
    ld.setLayerInset(0, off, off, 0, 0)
    ld.setLayerInset(1, 0, 0, off, off)
    return ld
}

/** Hiệu ứng: nhún nhẹ, dẻo như thạch, bật lên khi xuất hiện, lún xuống khi bấm. */
object Anim {
    private fun enabled(): Boolean = ValueAnimator.areAnimatorsEnabled()

    /** Nhún lên xuống + phồng nhẹ liên tục (nhịp thở của nhân vật). */
    fun idle(v: View): Animator? {
        if (!enabled()) return null
        val lift = -v.context.dp(6f)
        val a = ObjectAnimator.ofPropertyValuesHolder(
            v,
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, lift),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.97f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.03f)
        )
        a.duration = 1400
        a.repeatCount = ValueAnimator.INFINITE
        a.repeatMode = ValueAnimator.REVERSE
        a.interpolator = AccelerateDecelerateInterpolator()
        a.start()
        return a
    }

    /** Lắc dẻo kiểu thạch khi chạm vào nhân vật. */
    fun jelly(v: View) {
        if (!enabled()) return
        v.pivotY = v.height.toFloat()
        val sx = ObjectAnimator.ofFloat(v, View.SCALE_X, 1f, 1.22f, 0.9f, 1.06f, 0.98f, 1f)
        val sy = ObjectAnimator.ofFloat(v, View.SCALE_Y, 1f, 0.8f, 1.12f, 0.95f, 1.02f, 1f)
        val set = AnimatorSet()
        set.playTogether(sx, sy)
        set.duration = 650
        set.start()
    }

    /** Thẻ bật lên từ dưới khi xuất hiện. */
    fun popIn(v: View, delay: Long) {
        if (!enabled()) return
        v.alpha = 0f
        v.translationY = v.context.dp(36f)
        v.scaleX = 0.92f
        v.scaleY = 0.92f
        v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setStartDelay(delay).setDuration(460)
            .setInterpolator(OvershootInterpolator(1.6f)).start()
    }

    /** Thu phóng nhẹ khi đổi lựa chọn. */
    fun bump(v: View) {
        if (!enabled()) return
        v.animate().cancel()
        v.scaleX = 0.94f
        v.scaleY = 0.94f
        v.animate().scaleX(1f).scaleY(1f).setDuration(320).setInterpolator(OvershootInterpolator(3f)).start()
    }

    /** Lún xuống khi giữ ngón tay, bật lại khi nhả — vẫn giữ nguyên onClick. */
    @SuppressLint("ClickableViewAccessibility")
    fun pressable(v: View) {
        v.setOnTouchListener { view, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    view.animate().scaleX(0.96f).scaleY(0.96f).translationY(view.context.dp(2f))
                        .setDuration(80).setInterpolator(null).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).translationY(0f)
                        .setDuration(280).setInterpolator(OvershootInterpolator(3f)).start()
            }
            false
        }
    }
}
