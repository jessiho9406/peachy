package com.cindy.ngaykyniem

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate

/** Mở ra khi kéo widget mới ra màn hình (hoặc giữ lâu widget → cấu hình lại). */
class WidgetConfigActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.activity_widget_config)
        fitSystemBars()
        list = findViewById(R.id.list)

        val add = findViewById<TextView>(R.id.add)
        add.background = chunky(this, getColor(R.color.lime), 27f)
        Anim.pressable(add)
        add.setOnClickListener {
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(this, EditActivity::class.java), REQ_NEW)
        }

        render()
        if (Store.load(this).isEmpty()) {
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(this, EditActivity::class.java), REQ_NEW)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_NEW && resultCode == RESULT_OK) {
            val id = data?.getStringExtra(EditActivity.EXTRA_ID)
            if (id != null) {
                choose(id)
                return
            }
        }
        render()
    }

    private fun render() {
        list.removeAllViews()
        val today = LocalDate.now()
        val current = Store.widgetEvent(this, widgetId)
        val inflater = LayoutInflater.from(this)
        Store.load(this).forEachIndexed { i, e ->
            val card = inflater.inflate(R.layout.widget_wide, list, false)
            WidgetContent.of(this, e, today).apply(card)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpi(84))
            lp.bottomMargin = dpi(12)
            card.layoutParams = lp
            card.elevation = dp(if (e.id == current) 10f else 4f)
            card.isClickable = true
            card.contentDescription = e.name
            Anim.pressable(card)
            card.setOnClickListener { choose(e.id) }
            list.addView(card)
            Anim.popIn(card, 60L * i)
        }
    }

    private fun choose(eventId: String) {
        Store.setWidgetEvent(this, widgetId, eventId)
        WidgetProvider.updateOne(this, AppWidgetManager.getInstance(this), widgetId)
        WidgetProvider.scheduleMidnight(this)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }

    companion object {
        private const val REQ_NEW = 7
    }
}
