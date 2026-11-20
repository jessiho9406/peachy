package com.cindy.ngaykyniem

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateOne(context, manager, it) }
        scheduleMidnight(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateOne(context, manager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { Store.removeWidget(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> updateAll(context)

            ACTION_PINNED -> {
                val wid = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                val eid = intent.getStringExtra(EXTRA_EVENT)
                if (wid != AppWidgetManager.INVALID_APPWIDGET_ID && eid != null) {
                    Store.setWidgetEvent(context, wid, eid)
                    updateOne(context, AppWidgetManager.getInstance(context), wid)
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.cindy.ngaykyniem.REFRESH"
        const val ACTION_PINNED = "com.cindy.ngaykyniem.PINNED"
        const val EXTRA_EVENT = "event_id"

        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, WidgetProvider::class.java))
            ids.forEach { updateOne(ctx, mgr, it) }
            if (ids.isNotEmpty()) scheduleMidnight(ctx)
        }

        fun updateOne(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val events = Store.load(ctx)
            var e = events.firstOrNull { it.id == Store.widgetEvent(ctx, id) }
            if (e == null && events.isNotEmpty()) {
                e = events.first()
                Store.setWidgetEvent(ctx, id, e.id)
            }

            val opts = mgr.getAppWidgetOptions(id)
            val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val wide = minW == 0 || minW >= 200
            val rv = RemoteViews(ctx.packageName, if (wide) R.layout.widget_wide else R.layout.widget_small)
            val content = if (e != null) WidgetContent.of(e, LocalDate.now()) else WidgetContent.empty()
            content.apply(rv)

            val open = if (e != null) {
                Intent(ctx, EditActivity::class.java).putExtra(EditActivity.EXTRA_ID, e.id)
            } else {
                Intent(ctx, EditActivity::class.java)
            }
            open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pi = PendingIntent.getActivity(
                ctx, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            rv.setOnClickPendingIntent(R.id.w_root, pi)
            mgr.updateAppWidget(id, rv)
        }

        /** Hẹn cập nhật lại ngay sau nửa đêm để số ngày nhảy đúng. */
        fun scheduleMidnight(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val zone = ZoneId.systemDefault()
            val next = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 5_000
            val pi = PendingIntent.getBroadcast(
                ctx, 1,
                Intent(ctx, WidgetProvider::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAndAllowWhileIdle(AlarmManager.RTC, next, pi)
        }

        /** Xin launcher ghim widget cho sự kiện này. Trả về false nếu launcher không hỗ trợ. */
        fun requestPin(act: Activity, eventId: String): Boolean {
            val mgr = AppWidgetManager.getInstance(act)
            if (!mgr.isRequestPinAppWidgetSupported) return false
            val flags = if (Build.VERSION.SDK_INT >= 31) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val cb = PendingIntent.getBroadcast(
                act, eventId.hashCode(),
                Intent(act, WidgetProvider::class.java).setAction(ACTION_PINNED).putExtra(EXTRA_EVENT, eventId),
                flags
            )
            return mgr.requestPinAppWidget(ComponentName(act, WidgetProvider::class.java), null, cb)
        }
    }
}
