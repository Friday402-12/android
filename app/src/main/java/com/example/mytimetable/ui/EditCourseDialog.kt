package com.example.mytimetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytimetable.Course
import com.example.mytimetable.DefaultData
import com.example.mytimetable.Span

private val DAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/** 周次勾选后压缩成区间： [1,2,3,5] -> [Span(1,3), Span(5,5)] */
fun groupWeeks(sorted: List<Int>): List<Span> {
    if (sorted.isEmpty()) return emptyList()
    val out = mutableListOf<Span>()
    var start = sorted[0]
    var prev = sorted[0]
    for (i in 1 until sorted.size) {
        val cur = sorted[i]
        if (cur == prev + 1) {
            prev = cur
        } else {
            out.add(Span(start, prev))
            start = cur
            prev = cur
        }
    }
    out.add(Span(start, prev))
    return out
}

/** 把区间展开成周次列表文字，例如 1-4、6-7 */
fun weeksToText(spans: List<Span>): String =
    spans.joinToString("、") { if (it.from == it.to) "${it.from}" else "${it.from}-${it.to}" }

@Composable
fun EditCourseDialog(
    initial: Course?,
    totalWeeks: Int,
    defaultDay: Int,
    defaultSec: Int,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit
) {
    val editing = initial != null
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var teacher by remember { mutableStateOf(initial?.teacher ?: "") }
    var room by remember { mutableStateOf(initial?.room ?: "") }
    var day by remember { mutableStateOf(initial?.day ?: defaultDay) }
    var sec by remember { mutableStateOf(initial?.sec ?: defaultSec) }
    var weekType by remember { mutableStateOf(initial?.weekType ?: "all") }
    var error by remember { mutableStateOf<String?>(null) }

    val selected = remember {
        mutableStateListOf<Int>().apply {
            val spans = initial?.spans ?: listOf(Span(1, totalWeeks))
            for (w in 1..totalWeeks) {
                if (spans.any { w >= it.from && w <= it.to }) add(w)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing) "编辑课程" else "新增课程") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("课程名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("教师") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("上课地点") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Label("星期")
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    DAY_NAMES.forEachIndexed { i, n ->
                        PickChip(text = n, selected = day == i) { day = i }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Label("大节")
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    DefaultData.SECTIONS.forEach { s ->
                        PickChip(
                            text = "${s.name}  ${s.time}",
                            selected = sec == s.id,
                            fullWidth = true
                        ) { sec = s.id }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Label("单双周")
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    PickChip("每周", weekType == "all") { weekType = "all" }
                    PickChip("仅单周", weekType == "odd") { weekType = "odd" }
                    PickChip("仅双周", weekType == "even") { weekType = "even" }
                }

                Spacer(Modifier.height(12.dp))
                Label("上课周次（点选）　已选 ${weeksToText(groupWeeks(selected.sorted()))}")
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    PickChip("全选", false) {
                        selected.clear(); (1..totalWeeks).forEach { selected.add(it) }
                    }
                    PickChip("单周", false) {
                        selected.clear(); (1..totalWeeks).filter { it % 2 == 1 }.forEach { selected.add(it) }
                    }
                    PickChip("双周", false) {
                        selected.clear(); (1..totalWeeks).filter { it % 2 == 0 }.forEach { selected.add(it) }
                    }
                    PickChip("清空", false) { selected.clear() }
                }
                Spacer(Modifier.height(6.dp))

                // 周次网格：每行 10 个
                (1..totalWeeks).chunked(10).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { w ->
                            val on = selected.contains(w)
                            Box(
                                modifier = Modifier
                                    .size(width = 30.dp, height = 30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (on) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { if (on) selected.remove(w) else selected.add(w) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$w",
                                    fontSize = 12.sp,
                                    color = if (on) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                }

                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = name.trim()
                val weeks = selected.sorted()
                when {
                    n.isEmpty() -> error = "请填写课程名称"
                    weeks.isEmpty() -> error = "请至少选择一个周次"
                    else -> onSave(
                        Course(
                            id = initial?.id ?: "u" + System.currentTimeMillis(),
                            name = n,
                            code = initial?.code ?: "",
                            code2 = initial?.code2 ?: "",
                            teacher = teacher.trim(),
                            room = room.trim(),
                            day = day,
                            sec = sec,
                            weekType = weekType,
                            spans = groupWeeks(weeks)
                        )
                    )
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 5.dp)
    )
}

@Composable
private fun PickChip(text: String, selected: Boolean, fullWidth: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(9.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 课程详情弹窗 */
@Composable
fun CourseDetailDialog(
    course: Course,
    week: Int,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val inWeek = course.inWeek(week)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(course.name) },
        text = {
            Column {
                DetailRow("课程号", course.code.ifBlank { "—" })
                DetailRow("课堂派", course.code2.ifBlank { "—" })
                DetailRow("教师", course.teacherAt(week).ifBlank { "—" })
                DetailRow(
                    "时间",
                    "${dayName(course.day)} ${DefaultData.section(course.sec).name} " +
                        DefaultData.section(course.sec).time
                )
                DetailRow("地点", course.roomAt(week).ifBlank { "—" })
                DetailRow("周次", course.weeksText())
                DetailRow(
                    "本周",
                    "第 $week 周" + if (inWeek) " · 有课" else " · 无课"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        dismissButton = {
            TextButton(onClick = onEdit) { Text("编辑") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )
        Text(
            value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(220.dp)
        )
    }
}
