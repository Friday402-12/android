package com.example.mytimetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytimetable.Course
import com.example.mytimetable.DefaultData
import com.example.mytimetable.Section
import com.example.mytimetable.Timetable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val MD = DateTimeFormatter.ofPattern("M/d")

private val COL_TIME = 50.dp
private val COL_DAY = 94.dp
private val ROW_HEIGHT = 104.dp

/** 某天的一节课 */
data class Session(
    val course: Course,
    val section: Section,
    val date: LocalDate,
    val week: Int
)

fun dayName(index: Int): String = DAY_NAMES[index.coerceIn(0, 6)]

@Composable
fun WeekScreen(
    timetable: Timetable,
    week: Int,
    onCourseClick: (Course) -> Unit
) {
    val today = LocalDate.now()
    val todayIdx = timetable.todayDayIndex()
    val isThisWeek = week == timetable.currentWeek()

    Column(modifier = Modifier.padding(top = 10.dp)) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {

                // ---------------- 表头 ----------------
                Row {
                    Column(
                        modifier = Modifier
                            .width(COL_TIME)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "周次",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$week",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    for (d in 0 until 7) {
                        val date = timetable.dateOf(week, d)
                        val highlight = isThisWeek && d == todayIdx
                        Column(
                            modifier = Modifier
                                .width(COL_DAY)
                                .background(
                                    if (highlight) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .padding(vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                DAY_NAMES[d],
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (highlight) Color.White
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                date.format(MD),
                                fontSize = 10.sp,
                                color = if (highlight) Color.White.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ---------------- 五大节 ----------------
                DefaultData.SECTIONS.forEach { sec ->
                    Row {
                        Column(
                            modifier = Modifier
                                .width(COL_TIME)
                                .height(ROW_HEIGHT)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                sec.name.replace("大节", ""),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                sec.start,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                sec.end,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        for (d in 0 until 7) {
                            val list = timetable.coursesAt(d, sec.id, week)
                            val isToday = isThisWeek && d == todayIdx
                            Column(
                                modifier = Modifier
                                    .width(COL_DAY)
                                    .height(ROW_HEIGHT)
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primaryContainer
                                        else if (d >= 5) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                    .padding(3.dp)
                            ) {
                                list.forEach { c ->
                                    CourseChip(course = c) { onCourseClick(c) }
                                    Spacer(Modifier.height(3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ---------------- 图例 ----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            timetable.courses.map { it.name }.distinct().forEach { name ->
                val col = CoursePalette.of(name)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(col.border)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        name,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CourseChip(course: Course, onClick: () -> Unit) {
    val col = CoursePalette.of(course.name)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(col.bg)
            .border(0.7.dp, col.border, RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(horizontal = 5.dp, vertical = 4.dp)
    ) {
        Text(
            text = course.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = col.fg,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
        Text(
            text = course.room,
            fontSize = 9.sp,
            color = col.fg.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TodayScreen(
    timetable: Timetable,
    todaySessions: List<Session>,
    next: Session?,
    onCourseClick: (Course) -> Unit
) {
    val today = LocalDate.now()
    val week = timetable.weekOf(today)

    Column(modifier = Modifier.padding(top = 10.dp)) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (next == null) {
                    Text(
                        "今天没有课了 ☕",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "第 $week 周 · 今天共 ${todaySessions.size} 节",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                } else {
                    Text(
                        "下一节 · ${next.section.name} ${next.section.time}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        next.course.name,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "@${next.course.room} · ${next.course.teacherAt(next.week)}",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Spacer(Modifier.height(10.dp))
                    CountdownText(next)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = dayName(today.dayOfWeek.value - 1) + " · " +
                        today.format(DateTimeFormatter.ofPattern("M月d日")) + " · 第 $week 周",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))

                if (todaySessions.isEmpty()) {
                    Text(
                        "今天没有课",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                } else {
                    todaySessions.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onCourseClick(s.course) }
                                .padding(vertical = 9.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                Modifier
                                    .width(4.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CoursePalette.of(s.course.name).border)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "${s.section.name} · ${s.section.time} · ${s.section.periods}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    s.course.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "@${s.course.room} · ${s.course.teacherAt(s.week)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownText(session: Session) {
    val now = LocalDateTime.now()
    val start = session.section.startDateTime(session.date)
    val end = session.section.endDateTime(session.date)
    val text = when {
        now.isBefore(start) -> {
            val min = Duration.between(now, start).toMinutes()
            if (min >= 60) "${min / 60} 小时 ${min % 60} 分钟后上课" else "$min 分钟后上课"
        }
        now.isBefore(end) -> {
            val min = Duration.between(now, end).toMinutes()
            "正在上课 · 还有 $min 分钟下课"
        }
        else -> "本节已结束"
    }
    Text(
        text = text,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}
