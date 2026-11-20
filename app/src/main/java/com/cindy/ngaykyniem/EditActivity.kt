package com.cindy.ngaykyniem

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate

class EditActivity : Activity() {

    companion object {
        const val EXTRA_ID = "event_id"
    }

    private var editing: Event? = null
    private var date: LocalDate = LocalDate.now()
    private var repeat = 1
    private var mascot = 0
    private var theme = 0
    private var armDelete = false

    private lateinit var preview: View
    private lateinit var nameInput: EditText
    private lateinit var firstSwitch: Switch
    private lateinit var dateBtn: TextView
    private lateinit var lunarHint: TextView
    private val repeatViews = mutableListOf<TextView>()
    private val mascotViews = mutableListOf<ImageView>()
    private val themeViews = mutableListOf<View>()

    private val accent by lazy { getColor(R.color.accent) }
    private val line by lazy { getColor(R.color.line) }
    private val white = 0xFFFFFFFF.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        editing = Store.get(this, intent.getStringExtra(EXTRA_ID))
        editing?.let {
            date = it.date; repeat = it.repeat; mascot = it.mascot; theme = it.theme
        }

        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.title).setText(if (editing == null) R.string.title_add else R.string.title_edit)

        // Xem trước widget
        val holder = findViewById<FrameLayout>(R.id.preview_holder)
        preview = LayoutInflater.from(this).inflate(R.layout.widget_wide, holder, false)
        holder.addView(preview)
        preview.findViewById<View>(R.id.w_mascot).setOnClickListener { Anim.jelly(it) }

        nameInput = findViewById(R.id.name)
        nameInput.background = chunky(this, white, 16f, shadowDp = 3f)
        nameInput.setText(editing?.name ?: "")
        nameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = refreshPreview()
        })

        dateBtn = findViewById(R.id.date_btn)
        dateBtn.background = chunky(this, white, 16f, shadowDp = 3f)
        lunarHint = findViewById(R.id.lunar_hint)
        Anim.pressable(dateBtn)
        dateBtn.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                date = LocalDate.of(y, m + 1, d)
                refreshAll()
            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        }

        firstSwitch = findViewById(R.id.first)
        firstSwitch.isChecked = editing?.first ?: false
        firstSwitch.setOnCheckedChangeListener { _, _ -> refreshPreview() }

        buildRepeat()
        buildMascots()
        buildThemes()

        val save = findViewById<TextView>(R.id.save)
        save.background = chunky(this, getColor(R.color.hot), 27f)
        Anim.pressable(save)
        save.setOnClickListener { if (doSave() != null) finish() }

        val pin = findViewById<TextView>(R.id.pin)
        pin.background = chunky(this, getColor(R.color.lime), 27f)
        Anim.pressable(pin)
        pin.setOnClickListener {
            val e = doSave() ?: return@setOnClickListener
            if (!WidgetProvider.requestPin(this, e.id)) {
                Toast.makeText(
                    this,
                    R.string.pin_unsupported,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val del = findViewById<TextView>(R.id.del)
        if (editing != null) {
            del.visibility = View.VISIBLE
            del.setOnClickListener {
                if (!armDelete) {
                    armDelete = true
                    del.setText(R.string.delete_confirm)
                    del.background = rounded(this, 26f, 0x14C0485A, 0, 0f)
                    Anim.bump(del)
                } else {
                    Store.delete(this, editing!!.id)
                    WidgetProvider.updateAll(this)
                    Toast.makeText(this, R.string.toast_deleted, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        refreshAll()
    }

    private fun buildRepeat() {
        val group = findViewById<LinearLayout>(R.id.repeat_group)
        listOf(R.string.repeat_none, R.string.repeat_solar, R.string.repeat_lunar).forEachIndexed { i, label ->
            val t = TextView(this)
            t.setText(label)
            t.gravity = Gravity.CENTER
            t.textSize = 14f
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            if (i > 0) lp.marginStart = dpi(8)
            t.layoutParams = lp
            t.setOnClickListener {
                repeat = i
                Anim.bump(t)
                refreshAll()
            }
            group.addView(t)
            repeatViews.add(t)
        }
    }

    private fun buildMascots() {
        val group = findViewById<LinearLayout>(R.id.mascot_group)
        val names = resources.getStringArray(R.array.mascot_names)
        Look.MASCOTS.forEachIndexed { i, res ->
            val iv = ImageView(this)
            iv.setImageResource(res)
            iv.contentDescription = names.getOrNull(i)
            val pad = dpi(6)
            iv.setPadding(pad, pad, pad, pad)
            val lp = LinearLayout.LayoutParams(0, dpi(72), 1f)
            if (i > 0) lp.marginStart = dpi(10)
            iv.layoutParams = lp
            iv.setOnClickListener {
                mascot = i
                Anim.jelly(iv)
                refreshAll()
                Anim.jelly(preview.findViewById(R.id.w_mascot))
            }
            group.addView(iv)
            mascotViews.add(iv)
        }
    }

    private fun buildThemes() {
        val group = findViewById<LinearLayout>(R.id.theme_group)
        val names = resources.getStringArray(R.array.theme_names)
        Look.SWATCH.forEachIndexed { i, c ->
            val v = View(this)
            v.contentDescription = names.getOrNull(i)
            val lp = LinearLayout.LayoutParams(0, dpi(44), 1f)
            if (i > 0) lp.marginStart = dpi(10)
            v.layoutParams = lp
            v.setOnClickListener {
                theme = i
                Anim.bump(v)
                refreshAll()
            }
            group.addView(v)
            themeViews.add(v)
            v.tag = c
        }
    }

    private fun refreshAll() {
        dateBtn.text = "📅  " + Fmt.today(this, date)
        val l = Lunar.solarToLunar(date.dayOfMonth, date.monthValue, date.year)
        lunarHint.text = getString(R.string.lunar_hint, Fmt.lunar(this, l), l[2])

        repeatViews.forEachIndexed { i, t ->
            val on = i == repeat
            t.background = if (on) chunky(this, accent, 14f, shadowDp = 3f) else chunky(this, white, 14f, shadowDp = 3f)
            t.setTextColor(if (on) white else getColor(R.color.ink))
        }
        mascotViews.forEachIndexed { i, v ->
            v.background = if (i == mascot) chunky(this, getColor(R.color.lime), 20f, shadowDp = 3f)
            else rounded(this, 20f, white, 0x3324122E, 1.5f)
        }
        themeViews.forEachIndexed { i, v ->
            val c = v.tag as IntArray
            val d = GradientDrawable(GradientDrawable.Orientation.TL_BR, c)
            d.shape = GradientDrawable.RECTANGLE
            d.cornerRadius = dp(14f)
            if (i == theme) d.setStroke(dpi(3), accent) else d.setStroke(dpi(1), 0x3324122E)
            v.background = d
        }
        refreshPreview()
    }

    private fun currentEvent(): Event = Event(
        id = editing?.id ?: "preview",
        name = nameInput.text.toString().trim().ifEmpty { getString(R.string.default_name) },
        date = date,
        repeat = repeat,
        first = firstSwitch.isChecked,
        mascot = mascot,
        theme = theme
    )

    private fun refreshPreview() {
        WidgetContent.of(this, currentEvent()).apply(preview)
    }

    /** Lưu và trả về sự kiện; null nếu thiếu tên. */
    private fun doSave(): Event? {
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            nameInput.error = getString(R.string.err_name)
            nameInput.requestFocus()
            return null
        }
        val e = currentEvent().copy(id = editing?.id ?: Event.newId(), name = name)
        Store.upsert(this, e)
        editing = e
        WidgetProvider.updateAll(this)
        setResult(RESULT_OK, Intent().putExtra(EXTRA_ID, e.id))
        Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show()
        return e
    }
}
