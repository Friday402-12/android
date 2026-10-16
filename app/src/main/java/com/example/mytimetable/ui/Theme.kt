package com.example.mytimetable.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EFFF),
    onPrimaryContainer = Color(0xFF14336E),
    secondary = Color(0xFFE8590C),
    onSecondary = Color.White,
    background = Color(0xFFF2F3F7),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF7F8FB),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE6E8EF),
    outlineVariant = Color(0xFFE6E8EF)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6B9BFF),
    onPrimary = Color(0xFF0B1020),
    primaryContainer = Color(0xFF1C2950),
    onPrimaryContainer = Color(0xFFD8E4FF),
    secondary = Color(0xFFFF8A4C),
    onSecondary = Color(0xFF2A1405),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFEEF2FF),
    surface = Color(0xFF151B2E),
    onSurface = Color(0xFFEEF2FF),
    surfaceVariant = Color(0xFF1B2233),
    onSurfaceVariant = Color(0xFF9AA4BF),
    outline = Color(0xFF26314D),
    outlineVariant = Color(0xFF26314D)
)

@Composable
fun MyTimetableTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content
    )
}

/** 课程配色的背景/前景色对 */
data class CourseColors(val bg: Color, val fg: Color, val border: Color)

/** 与网页版一致的配色：同一门课永远同色 */
object CoursePalette {

    private val LIGHT = listOf(
        CourseColors(Color(0xFFE7F1FF), Color(0xFF15427F), Color(0xFFBCD8FF)),
        CourseColors(Color(0xFFE8FBF1), Color(0xFF0D5B3E), Color(0xFFB6ECD3)),
        CourseColors(Color(0xFFFFF3E2), Color(0xFF8A4B06), Color(0xFFFFD9AB)),
        CourseColors(Color(0xFFFDEAF3), Color(0xFF8B1F52), Color(0xFFF8C4DD)),
        CourseColors(Color(0xFFF0ECFF), Color(0xFF41258F), Color(0xFFD3C8FF)),
        CourseColors(Color(0xFFE6F8FB), Color(0xFF0B4F5C), Color(0xFFB3E6EF)),
        CourseColors(Color(0xFFFDF6D9), Color(0xFF6B5300), Color(0xFFF0E0A3)),
        CourseColors(Color(0xFFEEF1F6), Color(0xFF33415C), Color(0xFFCDD6E6)),
        CourseColors(Color(0xFFFFEAEA), Color(0xFF8C2020), Color(0xFFFFC6C6))
    )

    private val DARK = listOf(
        CourseColors(Color(0xFF13294D), Color(0xFFBCD8FF), Color(0xFF254574)),
        CourseColors(Color(0xFF0F3327), Color(0xFFA8ECC9), Color(0xFF1D5340)),
        CourseColors(Color(0xFF3D2A12), Color(0xFFFFD9AB), Color(0xFF63431D)),
        CourseColors(Color(0xFF3A1730), Color(0xFFF8C4DD), Color(0xFF5D2949)),
        CourseColors(Color(0xFF241D4D), Color(0xFFD3C8FF), Color(0xFF3C3272)),
        CourseColors(Color(0xFF0D3138), Color(0xFFB3E6EF), Color(0xFF1B4D57)),
        CourseColors(Color(0xFF332C10), Color(0xFFF0E0A3), Color(0xFF54491C)),
        CourseColors(Color(0xFF1B2233), Color(0xFFCDD6E6), Color(0xFF2F3A52)),
        CourseColors(Color(0xFF3D1A1A), Color(0xFFFFC6C6), Color(0xFF602828))
    )

    private val cache = HashMap<String, Int>()

    private fun indexOf(name: String): Int = cache.getOrPut(name) {
        var h = 0
        name.forEach { h = (h * 31 + it.code) % 9973 }
        h % LIGHT.size
    }

    @Composable
    fun of(name: String): CourseColors {
        val i = indexOf(name)
        return if (isSystemInDarkTheme()) DARK[i] else LIGHT[i]
    }
}
