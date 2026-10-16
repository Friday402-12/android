package com.example.mytimetable

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytimetable.ui.CourseDetailDialog
import com.example.mytimetable.ui.EditCourseDialog
import com.example.mytimetable.ui.MyTimetableTheme
import com.example.mytimetable.ui.Session
import com.example.mytimetable.ui.TodayScreen
import com.example.mytimetable.ui.WeekScreen
import com.example.mytimetable.ui.dayName
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private lateinit var repo: Repo
    private var refresh: (() -> Unit)? = null

    private val requestNotification = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        repo.remindersOn = granted
        Reminder.reschedule(this)
        refresh?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = Repo(this)
        Reminder.createChannel(this)

        setContent {
            MyTimetableTheme {
                var key by remember { mutableIntStateOf(0) }
                refresh = { key++ }
                AppRoot(
                    repo = repo,
                    refreshKey = key,
                    onRefresh = { key++ },
                    onEnableReminders = { requestNotification.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    onOpenExactAlarmSettings = { openExactAlarmSettings() },
                    onReschedule = { Reminder.reschedule(this) }
                )
            }
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (am != null && !am.canScheduleExactAlarms()) {
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: Exception) {
                    // 部分机型无此页面
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Reminder.reschedule(this)
        refresh?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(
    repo: Repo,
    refreshKey: Int,
    onRefresh: () -> Unit,
    onEnableReminders: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onReschedule: () -> Unit
) {
    val timetable = remember(refreshKey) { repo.timetable }

    var tab by remember { mutableIntStateOf(0) }
    var week by remember { mutableIntStateOf(timetable.clampWeek(timetable.currentWeek())) }
    var detailCourse by remember { mutableStateOf<Course?>(null) }
    var editCourse by remember { mutableStateOf<Course?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val todayWeek = timetable.weekOf(today)

    val todaySessions: List<Session> = remember(refreshKey) {
        if (todayWeek < 1 || todayWeek > timetable.weeks) emptyList()
        else timetable.coursesOfDay(timetable.todayDayIndex(), todayWeek)
            .map { Session(it, DefaultData.section(it.sec), today, todayWeek) }
    }

    val next: Session? = remember(refreshKey) {
        val now = LocalDateTime.now()
        var found: Session? = null
        for (d in 0 until 8) {
            if (found != null) break
            val date = today.plusDays(d.toLong())
            val w = timetable.weekOf(date)
            if (w < 1 || w > timetable.weeks) continue
            for (c in timetable.coursesOfDay((date.dayOfWeek.value + 6) % 7, w)) {
                val sec = DefaultData.section(c.sec)
                if (sec.endDateTime(date).isAfter(now)) {
                    found = Session(c, sec, date, w)
                    break
                }
            }
        }
        found
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Column {
                        Text(
                            "我的课表",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${timetable.term} · 共 ${timetable.weeks} 周",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = "课表") },
                    label = { Text("课表", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Notifications, contentDescription = "今日") },
                    label = { Text("今日", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "设置") },
                    label = { Text("设置", fontSize = 11.sp) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                0 -> WeekTab(
                    timetable = timetable,
                    week = week,
                    onPrev = { week = timetable.clampWeek(week - 1) },
                    onNext = { week = timetable.clampWeek(week + 1) },
                    onBackToToday = { week = timetable.clampWeek(timetable.currentWeek()) },
                    onCourseClick = { detailCourse = it }
                )
                1 -> TodayTab(
                    timetable = timetable,
                    todaySessions = todaySessions,
                    next = next,
                    onCourseClick = { detailCourse = it }
                )
                else -> SettingsTab(
                    repo = repo,
                    timetable = timetable,
                    onSave = { updated ->
                        repo.save(updated)
                        onReschedule()
                        onRefresh()
                    },
                    onEnableReminders = onEnableReminders,
                    onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                    onReschedule = onReschedule,
                    onEditCourse = { editCourse = it },
                    onAddCourse = { showNewDialog = true }
                )
            }
        }
    }

    detailCourse?.let { c ->
        CourseDetailDialog(
            course = c,
            week = week,
            onDismiss = { detailCourse = null },
            onEdit = {
                detailCourse = null
                editCourse = c
            }
        )
    }

    val editing = editCourse
    if (editing != null || showNewDialog) {
        EditCourseDialog(
            initial = editing,
            totalWeeks = timetable.weeks,
            defaultDay = if (week == todayWeek) timetable.todayDayIndex() else 0,
            defaultSec = 1,
            onDismiss = {
                editCourse = null
                showNewDialog = false
            },
            onSave = { saved ->
                val list = timetable.courses.toMutableList()
                val idx = list.indexOfFirst { it.id == saved.id }
                if (idx >= 0) list[idx] = saved else list.add(saved)
                repo.save(timetable.copy(courses = list))
                editCourse = null
                showNewDialog = false
                onReschedule()
                onRefresh()
            }
        )
    }
}

/* ---------------- 周次切换条 ---------------- */

@Composable
private fun WeekBar(
    timetable: Timetable,
    week: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBackToToday: () -> Unit
) {
    val isCurrent = week == timetable.currentWeek()
    val monday = timetable.mondayOfWeek(week)
    val sunday = monday.plusDays(6)
    val fmt = DateTimeFormatter.ofPattern("M/d")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPrev, enabled = week > 1) {
                    Text("‹", fontSize = 22.sp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "第 $week 周",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCurrent) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "本周",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                    }
                    Text(
                        "${monday.format(fmt)} - ${sunday.format(fmt)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = onNext, enabled = week < timetable.weeks) {
                    Text("›", fontSize = 22.sp)
                }
            }

            if (!isCurrent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onBackToToday) {
                        Text("回到本周（第 ${timetable.currentWeek()} 周）", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/* ---------------- 课表页 ---------------- */

@Composable
private fun WeekTab(
    timetable: Timetable,
    week: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBackToToday: () -> Unit,
    onCourseClick: (Course) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        WeekBar(timetable, week, onPrev, onNext, onBackToToday)
        WeekScreen(timetable = timetable, week = week, onCourseClick = onCourseClick)
        Spacer(Modifier.height(24.dp))
    }
}

/* ---------------- 今日页 ---------------- */

@Composable
private fun TodayTab(
    timetable: Timetable,
    todaySessions: List<Session>,
    next: Session?,
    onCourseClick: (Course) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        TodayScreen(
            timetable = timetable,
            todaySessions = todaySessions,
            next = next,
            onCourseClick = onCourseClick
        )
        Spacer(Modifier.height(24.dp))
    }
}

/* ---------------- 设置页 ---------------- */

@Composable
private fun SettingsTab(
    repo: Repo,
    timetable: Timetable,
    onSave: (Timetable) -> Unit,
    onEnableReminders: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onReschedule: () -> Unit,
    onEditCourse: (Course) -> Unit,
    onAddCourse: () -> Unit
) {
    var remindersOn by remember { mutableStateOf(repo.remindersOn) }
    var lead by remember { mutableIntStateOf(repo.leadMinutes) }
    val context = LocalContext.current
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        SettingsCard("学期设置") {
            SettingRow("第 1 周周一", timetable.startDate.format(fmt) + "（用于推算当前周次）") {
                Row {
                    OutlinedButton(onClick = {
                        onSave(timetable.copy(startDate = timetable.startDate.minusWeeks(1)))
                    }) { Text("− 一周", fontSize = 12.sp) }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = {
                        onSave(timetable.copy(startDate = timetable.startDate.plusWeeks(1)))
                    }) { Text("+ 一周", fontSize = 12.sp) }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow("总周数 / 当前周", "现在第 ${timetable.currentWeek()} 周 · 共 ${timetable.weeks} 周") {
                Row {
                    OutlinedButton(onClick = {
                        onSave(timetable.copy(weeks = (timetable.weeks - 1).coerceAtLeast(1)))
                    }) { Text("−", fontSize = 14.sp) }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = {
                        onSave(timetable.copy(weeks = (timetable.weeks + 1).coerceAtMost(30)))
                    }) { Text("＋", fontSize = 14.sp) }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        SettingsCard("课前提醒") {
            SettingRow(
                "开启提醒",
                if (remindersOn) "课前 $lead 分钟提醒，应用关闭后仍然有效"
                else "开启后会为未来一周的课程排闹钟"
            ) {
                Switch(
                    checked = remindersOn,
                    onCheckedChange = { want ->
                        remindersOn = want
                        repo.remindersOn = want
                        if (want) {
                            onEnableReminders()
                        } else {
                            Reminder.cancelAll(context)
                            onReschedule()
                        }
                    }
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow("提前时间", "当前 $lead 分钟") {
                Row {
                    listOf(5, 10, 15, 30).forEach { m ->
                        OutlinedButton(
                            onClick = {
                                lead = m
                                repo.leadMinutes = m
                                onReschedule()
                            },
                            modifier = Modifier.padding(end = 5.dp)
                        ) { Text("$m", fontSize = 12.sp) }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow("精确提醒", "系统若限制后台闹钟可能延迟几分钟，可点右侧授权") {
                OutlinedButton(onClick = onOpenExactAlarmSettings) {
                    Text("去授权", fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        SettingsCard("编辑课表（临时调课）") {
            timetable.courses.sortedWith(compareBy({ it.day }, { it.sec })).forEach { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            c.name,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${dayName(c.day)} ${DefaultData.section(c.sec).name} · @${c.room} · ${c.weeksRangeText()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onEditCourse(c) }) { Text("编辑", fontSize = 12.sp) }
                    TextButton(onClick = {
                        onSave(timetable.copy(courses = timetable.courses.filter { it.id != c.id }))
                    }) {
                        Text("删除", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddCourse) { Text("＋ 新增课程", fontSize = 13.sp) }
                OutlinedButton(onClick = { onSave(DefaultData.timetable()) }) {
                    Text("恢复初始课表", fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        SettingsCard("备注") {
            timetable.notes.forEach { n ->
                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    Text(
                        "· ${n.title}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        n.detail,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "节次时间：" + DefaultData.SECTIONS.joinToString("　") { "${it.name} ${it.time}" },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        control()
    }
}
