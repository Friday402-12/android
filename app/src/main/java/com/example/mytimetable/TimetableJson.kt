package com.example.mytimetable

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** 课表 ⇄ JSON（用系统自带的 org.json，不引入额外依赖） */
object TimetableJson {

    fun encode(t: Timetable): String {
        val root = JSONObject()
        root.put("term", t.term)
        root.put("startDate", t.startDate.toString())
        root.put("weeks", t.weeks)

        val arr = JSONArray()
        t.courses.forEach { c ->
            val o = JSONObject()
            o.put("id", c.id)
            o.put("name", c.name)
            o.put("code", c.code)
            o.put("code2", c.code2)
            o.put("teacher", c.teacher)
            o.put("room", c.room)
            o.put("day", c.day)
            o.put("sec", c.sec)
            o.put("weekType", c.weekType)
            val spans = JSONArray()
            c.spans.forEach { s ->
                val so = JSONObject()
                so.put("from", s.from)
                so.put("to", s.to)
                s.teacher?.let { so.put("teacher", it) }
                s.room?.let { so.put("room", it) }
                spans.put(so)
            }
            o.put("spans", spans)
            arr.put(o)
        }
        root.put("courses", arr)

        val notes = JSONArray()
        t.notes.forEach { n ->
            val no = JSONObject()
            no.put("title", n.title)
            no.put("detail", n.detail)
            notes.put(no)
        }
        root.put("notes", notes)
        return root.toString()
    }

    fun decode(raw: String): Timetable {
        val root = JSONObject(raw)

        val courses = mutableListOf<Course>()
        val arr = root.optJSONArray("courses") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val spans = mutableListOf<Span>()
            val sa = o.optJSONArray("spans") ?: JSONArray()
            for (j in 0 until sa.length()) {
                val so = sa.getJSONObject(j)
                spans.add(
                    Span(
                        from = so.optInt("from", 1),
                        to = so.optInt("to", 1),
                        teacher = so.optString("teacher", "").ifBlank { null },
                        room = so.optString("room", "").ifBlank { null }
                    )
                )
            }
            courses.add(
                Course(
                    id = o.optString("id", "u$i"),
                    name = o.optString("name", "未命名"),
                    code = o.optString("code", ""),
                    code2 = o.optString("code2", ""),
                    teacher = o.optString("teacher", ""),
                    room = o.optString("room", ""),
                    day = o.optInt("day", 0),
                    sec = o.optInt("sec", 1),
                    weekType = o.optString("weekType", "all"),
                    spans = spans
                )
            )
        }

        val notes = mutableListOf<Note>()
        val na = root.optJSONArray("notes") ?: JSONArray()
        for (i in 0 until na.length()) {
            val no = na.getJSONObject(i)
            notes.add(Note(no.optString("title", ""), no.optString("detail", "")))
        }

        val start = try {
            LocalDate.parse(root.optString("startDate", DefaultData.START_DATE))
        } catch (e: Exception) {
            LocalDate.parse(DefaultData.START_DATE)
        }

        return Timetable(
            term = root.optString("term", "2026-2027 学年 · 第一学期"),
            startDate = start,
            weeks = root.optInt("weeks", DefaultData.WEEKS),
            courses = courses.ifEmpty { DefaultData.courses() },
            notes = notes.ifEmpty { DefaultData.NOTES }
        )
    }
}
