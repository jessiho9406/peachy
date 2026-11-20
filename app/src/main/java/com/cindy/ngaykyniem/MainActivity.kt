package com.cindy.ngaykyniem

import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate

class MainActivity : Activity() {

    private lateinit var list: LinearLayout
    private lateinit var empty: View
    private var idleAnims = mutableListOf<Animator>()
    private var firstShow = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fitSystemBars()
        list = findViewById(R.id.list)
        empty = findViewById(R.id.empty)

        val fab = findViewById<TextView>(R.id.fab)
        fab.background = chunky(this, getColor(R.color.hot), 28f)
        fab.elevation = 0f
        Anim.pressable(fab)
        fab.setOnClickListener { startActivity(Intent(this, EditActivity::class.java)) }

        val tip = findViewById<TextView>(R.id.tip)
        tip.background = chunky(this, 0xFFFFFFFF.toInt(), 20f)
        tip.setText(R.string.tip)

        findViewById<TextView>(R.id.tagline).background = rounded(this, 10f, getColor(R.color.lime))

        val header = findViewById<ImageView>(R.id.header_mascot)
        header.setOnClickListener { Anim.jelly(it) }
        findViewById<ImageView>(R.id.empty_mascot).setOnClickListener { Anim.jelly(it) }
    }

    override fun onResume() {
        super.onResume()
        render()
        WidgetProvider.updateAll(this)
        startIdle()
    }

    override fun onPause() {
        super.onPause()
        idleAnims.forEach { it.cancel() }
        idleAnims.clear()
    }

    private fun startIdle() {
        idleAnims.forEach { it.cancel() }
        idleAnims.clear()
        Anim.idle(findViewById(R.id.header_mascot))?.let { idleAnims.add(it) }
        if (empty.visibility == View.VISIBLE) {
            Anim.idle(findViewById(R.id.empty_mascot))?.let { idleAnims.add(it) }
        }
    }

    private fun render() {
        val today = LocalDate.now()
        var todayText = Fmt.today(this, today)
        if (Fmt.usesLunar(this)) {
            val l = Lunar.solarToLunar(today.dayOfMonth, today.monthValue, today.year)
            todayText += " · " + Fmt.lunar(this, l)
        }
        findViewById<TextView>(R.id.today).text = todayText

        val events = Store.load(this).sortedBy { Calc(it, today).sortKey }
        list.removeAllViews()
        empty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE

        val inflater = LayoutInflater.from(this)
        events.forEachIndexed { i, e ->
            val card = inflater.inflate(R.layout.widget_wide, list, false)
            WidgetContent.of(this, e, today).apply(card)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpi(84))
            lp.bottomMargin = dpi(12)
            card.layoutParams = lp
            card.elevation = dp(5f)
            card.isClickable = true
            card.contentDescription = e.name
            Anim.pressable(card)
            card.setOnClickListener {
                startActivity(Intent(this, EditActivity::class.java).putExtra(EditActivity.EXTRA_ID, e.id))
            }
            card.findViewById<View>(R.id.w_mascot).setOnClickListener { Anim.jelly(it) }
            list.addView(card)
            if (firstShow) Anim.popIn(card, 60L * i)
        }
        firstShow = false
    }
}
