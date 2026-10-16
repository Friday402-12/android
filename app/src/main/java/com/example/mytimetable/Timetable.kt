package com.example.mytimetable

import java.time.LocalDate

/** 单个周次区间。teacher / room 为空时沿用课程本身的默认值。 */
data class Span(
    val from: Int,
    val to: Int,
    val teacher: String? = null,
    val room: String? = null
)

/** 一门课。day: 0=周一 … 6=周日；sec: 1..5 对应五大节；weekType: all / odd / even */
data class Course(
    val id: String,
    val name: String,
    val code: String = "",
    val code2: String = "",
    val teacher: String = "",
    val room: String = "",
    val day: Int = 0,
    val sec: Int = 1,
    val weekType: String = "all",
    val spans: List<Span> = emptyList()
) {
    /** 第 w 周是否包含在任一区间内 */
    fun inWeek(w: Int): Boolean = spans.any { w >= it.from && w <= it.to && typeOk(w) }

    fun typeOk(w: Int): Boolean = when (weekType) {
        "odd" -> w % 2 == 1
        "even" -> w % 2 == 0
        else -> true
    }

    /** 第 w 周的具体安排（老师/教室可能随周次不同） */
    fun spanAt(w: Int): Span? = spans.firstOrNull { w >= it.from && w <= it.to && typeOk(w) }

    fun teacherAt(w: Int): String = spanAt(w)?.teacher ?: teacher
    fun roomAt(w: Int): String = spanAt(w)?.room ?: room

    /** 例如 "4-17[周]"；单双周额外标注 */
    fun weeksText(): String {
        if (spans.isEmpty()) return "未设周次"
        val suffix = when (weekType) {
            "odd" -> "单周"
            "even" -> "双周"
            else -> ""
        }
        return spans.joinToString("、") { s ->
            if (s.from == s.to) "${s.from}[周]$suffix" else "${s.from}-${s.to}[周]$suffix"
        }
    }

    /** 例如 "4-17[周]"，用于列表里的紧凑显示 */
    fun weeksRangeText(): String {
        if (spans.isEmpty()) return "—"
        val f = spans.minOf { it.from }
        val t = spans.maxOf { it.to }
        return if (f == t) "$f[周]" else "$f-$t[周]"
    }
}

/** 一大节 */
data class Section(
    val id: Int,
    val name: String,
    val start: String,
    val end: String,
    val periods: String
) {
    val time: String get() = "$start-$end"

    fun startMinutes(): Int {
        val (h, m) = start.split(":")
        return h.toInt() * 60 + m.toInt()
    }

    fun endMinutes(): Int {
        val (h, m) = end.split(":")
        return h.toInt() * 60 + m.toInt()
    }

    fun startDateTime(d: LocalDate): java.time.LocalDateTime =
        d.atStartOfDay().plusMinutes(startMinutes().toLong())

    fun endDateTime(d: LocalDate): java.time.LocalDateTime =
        d.atStartOfDay().plusMinutes(endMinutes().toLong())
}

/** 整张课表 */
data class Timetable(
    val term: String = "2026-2027 学年 · 第一学期",
    val startDate: LocalDate = LocalDate.of(2026, 8, 31),
    val weeks: Int = 20,
    val courses: List<Course> = emptyList(),
    val notes: List<Note> = emptyList()
) {
    fun coursesOfDay(day: Int, week: Int): List<Course> =
        courses.filter { it.day == day && it.inWeek(week) }.sortedBy { it.sec }

    fun coursesAt(day: Int, sec: Int, week: Int): List<Course> =
        courses.filter { it.day == day && it.sec == sec && it.inWeek(week) }

    fun mondayOfWeek(week: Int): LocalDate = startDate.plusWeeks((week - 1).toLong())

    fun dateOf(week: Int, day: Int): LocalDate = mondayOfWeek(week).plusDays(day.toLong())

    /** 某天属于第几周（可能 <1 或 >weeks，表示不在学期内） */
    fun weekOf(date: LocalDate): Int =
        (java.time.temporal.ChronoUnit.DAYS.between(startDate, date) / 7).toInt() + 1

    fun currentWeek(): Int = weekOf(LocalDate.now())

    /** 今天的星期序号：0=周一 … 6=周日 */
    fun todayDayIndex(): Int = (LocalDate.now().dayOfWeek.value + 6) % 7

    fun clampWeek(w: Int): Int = w.coerceIn(1, if (weeks < 1) 1 else weeks)
}

/** 备注 / 无课表课程 */
data class Note(val title: String, val detail: String)
