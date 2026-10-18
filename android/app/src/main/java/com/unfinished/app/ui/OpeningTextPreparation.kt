package com.unfinished.app.ui

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/** Warm Minikin glyph/shaping caches, not just Typeface objects. No user content. */
internal fun prepareOpeningText(measurer: TextMeasurer) {
    val regular = TextStyle(fontFamily = OpeningChineseFont)
    val medium = regular.copy(fontWeight = FontWeight.Medium)
    // Keep the approved opening's original TextStyle/default-font resolution.
    // The subset is only for the home UI that already used EmbeddedChineseFont.
    val opening = TextStyle(fontFamily = FontFamily.Default)
    val openingMedium = opening.copy(fontWeight = FontWeight.Medium)
    measurer.measure("一片承载记忆的时间海域", opening.copy(fontSize = 13.sp, letterSpacing = 1.4.sp))
    measurer.measure("未完成", openingMedium.copy(fontSize = 42.sp, lineHeight = 50.sp, letterSpacing = 1.sp))
    measurer.measure("过去不能修改，\n但可以重新想起。", openingMedium.copy(fontSize = 16.sp, lineHeight = 29.sp))
    // These are the three existing home projections. Warm all three so a Room
    // emission cannot introduce a previously unseen CJK run during the tide.
    measurer.measure("一段过去，\n抵达岸边。", regular.copy(fontSize = 31.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp))
    measurer.measure("留下一段，\n给以后的自己。", regular.copy(fontSize = 32.sp, lineHeight = 41.sp, letterSpacing = (-0.8).sp))
    measurer.measure("今天海面\n很安静。", regular.copy(fontSize = 34.sp, lineHeight = 43.sp, letterSpacing = (-1).sp))
    measurer.measure("潮水带来一页过去 潮水退去，痕迹留下 先留下一些东西，剩下的交给时间 夜色 · 岸边", regular.copy(fontSize = 12.sp))
    measurer.measure("一件小事，也值得被好好留住。", regular.copy(fontSize = 14.sp, lineHeight = 23.sp))
    measurer.measure("可以留下一点，也可以只是停一会儿。", regular.copy(fontSize = 14.sp, lineHeight = 23.sp))
    measurer.measure("想一会儿 再给一点 直接看看 让它再待一会 + 留下一段 过去", medium.copy(fontSize = 14.sp))
}
