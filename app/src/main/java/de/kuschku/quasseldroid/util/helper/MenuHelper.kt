package de.kuschku.quasseldroid.util.helper

import android.content.Context
import android.support.v4.graphics.drawable.DrawableCompat
import android.view.Menu
import de.kuschku.quasseldroid.R

fun Menu.retint(context: Context) {
  context.theme.styledAttributes(R.attr.colorControlNormal) {
    val color = getColor(0, 0)

    for (item in (0 until size()).map { getItem(it) }) {
      item.icon?.mutate()?.let { drawable ->
        DrawableCompat.setTint(drawable, color)
        item.icon = drawable
      }
    }
  }
}