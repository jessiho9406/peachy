package com.cindy.ngaykyniem

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.UUID

/** repeat: 0 = không lặp, 1 = hằng năm dương lịch, 2 = hằng năm âm lịch */
data class Event(
    val id: String,
    val name: String,
    val date: LocalDate,
    val repeat: Int,
    val first: Boolean,
    val mascot: Int,
    val theme: Int
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("date", date.toString())
        .put("repeat", repeat).put("first", first)
        .put("mascot", mascot).put("theme", theme)

    companion object {
        fun fromJson(o: JSONObject): Event? = try {
            Event(
                id = o.getString("id"),
                name = o.optString("name", ""),
                date = LocalDate.parse(o.getString("date")),
                repeat = o.optInt("repeat", 0),
                first = o.optBoolean("first", false),
                mascot = o.optInt("mascot", 0).coerceIn(0, Look.MASCOTS.size - 1),
                theme = o.optInt("theme", 0).coerceIn(0, Look.BG.size - 1)
            )
        } catch (e: Exception) {
            null
        }

        fun newId(): String = UUID.randomUUID().toString().substring(0, 12)
    }
}

/** Lưu dữ liệu gọn trong SharedPreferences (vài chục ngày thì rất nhẹ). */
object Store {
    private const val PREF = "nkn"
    private const val KEY = "events"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun load(ctx: Context): MutableList<Event> {
        val raw = prefs(ctx).getString(KEY, "[]") ?: "[]"
        val out = mutableListOf<Event>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                Event.fromJson(arr.getJSONObject(i))?.let { out.add(it) }
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun save(ctx: Context, list: List<Event>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    fun get(ctx: Context, id: String?): Event? = if (id == null) null else load(ctx).firstOrNull { it.id == id }

    fun upsert(ctx: Context, e: Event) {
        val list = load(ctx)
        val i = list.indexOfFirst { it.id == e.id }
        if (i >= 0) list[i] = e else list.add(e)
        save(ctx, list)
    }

    fun delete(ctx: Context, id: String) {
        save(ctx, load(ctx).filter { it.id != id })
    }

    fun setWidgetEvent(ctx: Context, widgetId: Int, eventId: String) {
        prefs(ctx).edit().putString("w_$widgetId", eventId).apply()
    }

    fun widgetEvent(ctx: Context, widgetId: Int): String? = prefs(ctx).getString("w_$widgetId", null)

    fun removeWidget(ctx: Context, widgetId: Int) {
        prefs(ctx).edit().remove("w_$widgetId").apply()
    }
}

/** Kết quả tính ngày cho một sự kiện. */
class Calc(e: Event, today: LocalDate) {
    val future: Boolean
    val count: Int
    var nextAt: LocalDate? = null
    var nextIn: Int? = null
    var nextNo: Int = 0
    var milestone: Int? = null
    var milestoneIn: Int? = null
    var lunar: IntArray? = null
    val sortKey: Int

    init {
        val diff = (today.toEpochDay() - e.date.toEpochDay()).toInt()
        future = diff < 0
        count = if (future) -diff else diff + (if (e.first) 1 else 0)

        if (e.repeat == 2 || future) {
            lunar = Lunar.solarToLunar(e.date.dayOfMonth, e.date.monthValue, e.date.year)
        }
        if (e.repeat == 1) {
            for (y in today.year..today.year + 1) {
                val d = minOf(e.date.dayOfMonth, YearMonth.of(y, e.date.monthValue).lengthOfMonth())
                val o = LocalDate.of(y, e.date.monthValue, d)
                if (!o.isBefore(today) && !o.isBefore(e.date)) {
                    nextAt = o
                    nextIn = (o.toEpochDay() - today.toEpochDay()).toInt()
                    nextNo = y - e.date.year
                    break
                }
            }
        } else if (e.repeat == 2) {
            val l = lunar!!
            val ty = Lunar.solarToLunar(today.dayOfMonth, today.monthValue, today.year)[2]
            for (j in ty - 1..ty + 1) {
                val s = Lunar.occurrence(l[0], l[1], j) ?: continue
                val o = LocalDate.of(s[2], s[1], s[0])
                if (!o.isBefore(today) && !o.isBefore(e.date)) {
                    nextAt = o
                    nextIn = (o.toEpochDay() - today.toEpochDay()).toInt()
                    nextNo = j - l[2]
                    break
                }
            }
        }
        if (future) {
            nextAt = e.date
            nextIn = -diff
            nextNo = 0
        } else {
            val m = nextMilestone(count)
            milestone = m
            milestoneIn = m - count
        }
        sortKey = minOf(nextIn ?: Int.MAX_VALUE, milestoneIn ?: Int.MAX_VALUE)
    }

    companion object {
        fun nextMilestone(c: Int): Int {
            if (c < 900) return (c / 100 + 1) * 100
            return (c / 500 + 1) * 500
        }
    }
}

object Fmt {
    private val nf: NumberFormat = NumberFormat.getIntegerInstance(Locale("vi", "VN"))
    fun num(n: Int): String = nf.format(n)
    fun date(d: LocalDate): String = "%02d/%02d/%d".format(d.dayOfMonth, d.monthValue, d.year)
    fun dayMonth(d: LocalDate): String = "%02d/%02d".format(d.dayOfMonth, d.monthValue)
    fun lunar(l: IntArray): String = "%02d/%02d ÂL".format(l[0], l[1]) + (if (l[3] == 1) " (nhuận)" else "")
    private val WD = arrayOf("", "T2", "T3", "T4", "T5", "T6", "T7", "CN")
    fun weekday(d: LocalDate): String = WD[d.dayOfWeek.value]
}

/** Nhân vật + bảng màu widget. */
object Look {
    val MASCOTS = intArrayOf(R.drawable.mascot_0, R.drawable.mascot_1, R.drawable.mascot_2, R.drawable.mascot_3, R.drawable.mascot_4)
    val MASCOT_NAMES = arrayOf("Cười", "Vẫy tay", "Nháy mắt", "Ôm lá", "Mắt tim")
    val BG = intArrayOf(R.drawable.bg_w0, R.drawable.bg_w1, R.drawable.bg_w2, R.drawable.bg_w3, R.drawable.bg_w4, R.drawable.bg_w5)
    val SWATCH = arrayOf(
        intArrayOf(0xFF4A2468.toInt(), 0xFF1E0E2C.toInt()),
        intArrayOf(0xFF4D86FF.toInt(), 0xFF2140D6.toInt()),
        intArrayOf(0xFFFFF8F0.toInt(), 0xFFFFDCC4.toInt()),
        intArrayOf(0xFFEEFF7A.toInt(), 0xFFB6EE3A.toInt()),
        intArrayOf(0xFF34343A.toInt(), 0xFF121215.toInt()),
        intArrayOf(0xFF0A6B65.toInt(), 0xFF00504B.toInt())
    )
    val THEME_NAMES = arrayOf("Mận đêm", "Xanh coban", "Kem sữa", "Chanh muối", "Đen nhám", "Xanh rêu")
    /** màu tên + chữ "ngày" */
    val INK = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF24122E.toInt(), 0xFF1C2A0E.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
    /** màu con số lớn */
    val NUM = intArrayOf(0xFFFFB08A.toInt(), 0xFFFFD2B0.toInt(), 0xFFFF3D6E.toInt(), 0xFF24122E.toInt(), 0xFFFF8A5C.toInt(), 0xFFFFC9A8.toInt())
    val SUB = intArrayOf(0xFFCDB0E0.toInt(), 0xFFC9D8FF.toInt(), 0xFF8C5B6B.toInt(), 0xFF46591B.toInt(), 0xFFA9A9B3.toInt(), 0xFFBFE3DF.toInt())
}

/** Nội dung hiển thị của widget — dùng chung cho widget thật và bản xem trước trong app. */
class WidgetContent(
    val name: String,
    val num: String,
    val unit: String,
    val sub: String,
    val date: String,
    val theme: Int,
    val mascot: Int
) {
    fun apply(rv: RemoteViews) {
        rv.setInt(R.id.w_root, "setBackgroundResource", Look.BG[theme])
        rv.setImageViewResource(R.id.w_mascot, Look.MASCOTS[mascot])
        rv.setTextViewText(R.id.w_name, name)
        rv.setTextViewText(R.id.w_num, num)
        rv.setTextViewText(R.id.w_unit, unit)
        rv.setTextColor(R.id.w_num, Look.NUM[theme])
        rv.setTextColor(R.id.w_unit, Look.INK[theme])
        rv.setTextColor(R.id.w_name, Look.INK[theme])
        rv.setTextViewText(R.id.w_sub, sub)
        rv.setTextColor(R.id.w_sub, Look.SUB[theme])
        rv.setTextViewText(R.id.w_date, date)
        rv.setTextColor(R.id.w_date, Look.SUB[theme])
    }

    /** Áp vào layout widget_wide đã inflate thường (xem trước trong app). */
    fun apply(v: View) {
        v.findViewById<View>(R.id.w_root).setBackgroundResource(Look.BG[theme])
        v.findViewById<ImageView>(R.id.w_mascot).setImageResource(Look.MASCOTS[mascot])
        fun t(id: Int, s: String, c: Int) {
            v.findViewById<TextView>(id)?.let { it.text = s; it.setTextColor(c) }
        }
        t(R.id.w_name, name, Look.INK[theme])
        t(R.id.w_num, num, Look.NUM[theme])
        t(R.id.w_unit, unit, Look.INK[theme])
        t(R.id.w_sub, sub, Look.SUB[theme])
        t(R.id.w_date, date, Look.SUB[theme])
    }

    companion object {
        fun of(e: Event, today: LocalDate = LocalDate.now()): WidgetContent {
            val c = Calc(e, today)
            val num: String
            val unit: String
            val sub: String
            if (c.future) {
                num = Fmt.num(c.count)
                unit = "ngày nữa"
                sub = "${Fmt.weekday(e.date)}, ${Fmt.date(e.date)}"
            } else {
                num = Fmt.num(c.count)
                unit = "ngày"
                val nIn = c.nextIn
                val nAt = c.nextAt
                sub = if (e.repeat != 0 && nIn != null && nAt != null) {
                    if (nIn == 0) "Hôm nay kỷ niệm lần ${c.nextNo} 🎉"
                    else "Lần ${c.nextNo} · ${Fmt.dayMonth(nAt)} · còn $nIn ngày"
                } else {
                    val mIn = c.milestoneIn ?: 0
                    if (mIn == 0) "Hôm nay tròn ${Fmt.num(c.milestone ?: 0)} ngày 🎉"
                    else "Tròn ${Fmt.num(c.milestone ?: 0)} ngày · còn $mIn ngày"
                }
            }
            val l = c.lunar
            val date = if (e.repeat == 2 && l != null) "${Fmt.date(e.date)} · ${Fmt.lunar(l)}" else "Từ ${Fmt.date(e.date)}"
            return WidgetContent(e.name, num, unit, sub, if (c.future) "" else date, e.theme, e.mascot)
        }

        fun empty(): WidgetContent =
            WidgetContent("chạm để thêm ngày", "+", "", "Peachy · đếm ngày iu", "", 0, 0)
    }
}
