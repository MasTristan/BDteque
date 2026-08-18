package com.bdshelf.app.domain

import androidx.compose.ui.graphics.Color

fun Long.toSpineColor(): Color = Color(this.toInt())
