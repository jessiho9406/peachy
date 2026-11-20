package com.cindy.ngaykyniem

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import java.time.LocalDate

/** Chỉ dùng để chụp ảnh CH Play: nạp 5 ngày mẫu theo ngôn ngữ máy rồi mở màn hình cần chụp. */
class DemoActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("seed", true)) seed()
        val first = Store.load(this).firstOrNull()
        val next = when (intent.getStringExtra("screen") ?: "main") {
            "edit" -> Intent(this, EditActivity::class.java).putExtra(EditActivity.EXTRA_ID, first?.id)
            "widgets" -> Intent(this, ShowcaseActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(next)
        finish()
    }

    private fun seed() {
        Store.clear(this)
        val names = resources.getStringArray(R.array.demo_names)
        val repeats = resources.getIntArray(R.array.demo_repeat)
        val dates = listOf(
            LocalDate.of(2020, 11, 25),
            LocalDate.of(2018, 2, 14),
            LocalDate.of(2023, 1, 15),
            LocalDate.of(2000, 3, 10),
            LocalDate.now().plusDays(42)
        )
        val mascots = intArrayOf(0, 4, 1, 3, 2)
        val themes = intArrayOf(0, 2, 3, 5, 1)
        for (i in 0 until minOf(names.size, dates.size)) {
            Store.upsert(
                this,
                Event(
                    id = "demo$i",
                    name = names[i],
                    date = dates[i],
                    repeat = repeats.getOrElse(i) { 0 },
                    first = false,
                    mascot = mascots[i],
                    theme = themes[i]
                )
            )
        }
    }
}
