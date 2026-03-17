package com.example.alo.presentation.view.utils

import com.example.alo.R

object ProfileBackgrounds {
    val backgrounds = listOf(
        "bg_1" to R.drawable.bg_1,
        "bg_2" to R.drawable.bg_3,
        "bg_3" to R.drawable.bg_2
    )

    fun getDrawable(name: String?): Int {
        return backgrounds.find { it.first == name }?.second ?: R.drawable.bg_1
    }
}