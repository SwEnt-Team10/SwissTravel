package com.android.sample

import kotlin.math.sqrt

data class Point(val x: Double, val y: Double) {

  fun distanceTo(p: Point): Double {
    val dx = x - p.x
    val dy = y - p.y
    return sqrt(dx * dx + dy * dy)
  }
}
