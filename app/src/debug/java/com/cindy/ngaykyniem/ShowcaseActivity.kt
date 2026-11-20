package com.cindy.ngaykyniem

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate

/** Chỉ dùng để chụp ảnh CH Play: dựng đúng layout widget thật trên nền giống màn hình chính. */
class ShowcaseActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF9DB7FF.toInt(), 0xFFFFB6C8.toInt(), 0xFFFFE2B8.toInt())
        )
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dpi(18), dpi(70), dpi(18), dpi(24))
        root.addView(col, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val clock = TextView(this)
        clock.text = "09:41"
        clock.textSize = 64f
        clock.setTextColor(0xFFFFFFFF.toInt())
        clock.fontFeatureSettings = "tnum"
        clock.typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
        clock.gravity = Gravity.CENTER_HORIZONTAL
        col.addView(clock, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpi(36) })

        val events = Store.load(this)
        val today = LocalDate.now()
        val inflater = LayoutInflater.from(this)
        fun wide(i: Int) {
            val e = events.getOrNull(i) ?: return
            val v = inflater.inflate(R.layout.widget_wide, col, false)
            WidgetContent.of(this, e, today).apply(v)
            v.elevation = dp(6f)
            col.addView(v, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpi(76)).apply { bottomMargin = dpi(16) })
        }
        wide(0)
        wide(3)
        wide(4)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        for (i in intArrayOf(1, 2)) {
            val e = events.getOrNull(i) ?: continue
            val v = inflater.inflate(R.layout.widget_small, row, false)
            WidgetContent.of(this, e, today).apply(v)
            v.elevation = dp(6f)
            row.addView(v, LinearLayout.LayoutParams(0, dpi(150), 1f).apply { if (i == 2) marginStart = dpi(16) })
        }
        col.addView(row)
        setContentView(root)
    }
}
