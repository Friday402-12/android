package com.example.mytimetable

import android.content.Context

/** 从两张照片（教务处网页版 + Excel 导出，以网页版为准）整理出的原始课表。 */
object DefaultData {

    const val START_DATE = "2026-08-31"   // 第 1 周周一（反推自「现在是第七周」）
    const val WEEKS = 20

    val SECTIONS = listOf(
        Section(1, "第一大节", "08:00", "09:40", "第01-02节"),
        Section(2, "第二大节", "10:00", "11:40", "第03-04节"),
        Section(3, "第三大节", "14:30", "16:10", "第05-06节"),
        Section(4, "第四大节", "16:30", "18:10", "第07-08节"),
        Section(5, "第五大节", "19:30", "21:10", "第09-10节")
    )

    fun section(id: Int): Section = SECTIONS.firstOrNull { it.id == id } ?: SECTIONS[0]

    val NOTES = listOf(
        Note(
            "军事技能",
            "71510103-1 张冰华(思政) · 第 2-3 周 · 周一到周五（不含晚上）"
        ),
        Note(
            "国家安全教育（无课表课程）",
            "21510104-3 · 授课对象：智制2601-2603班、电子2601-2602班、通信2601-2604班、" +
                "集成2601-2602班、机械2601-2608班、产品2601班、输电等"
        ),
        Note(
            "数据说明",
            "课表以教务处网页版（jwxt.ncepu.edu.cn）为准；Excel 导出缺少「思想道德与法治」「体育(1)」，已按网页版补全。"
        )
    )

    fun courses(): List<Course> = listOf(
        /* ---------------- 周一 ---------------- */
        Course("c01", "C/C++程序设计", "20910450-6", "课堂派J53HWK", "李静", "教十一楼A323", 0, 1,
            spans = listOf(Span(4, 17))),
        Course("c02", "高等数学(1)", "71010101-6", "课堂派W6L7EN", "严艳", "教十楼B座302", 0, 2,
            spans = listOf(Span(4, 18))),
        Course("c03", "线性代数", "81010103-7", "课堂派C7EKBQ", "王涛", "教九楼B座201", 0, 3,
            spans = listOf(Span(4, 15))),
        Course("c04", "大学生心理健康", "g0810330-2", "课堂派87S8VK", "冯海婧(思政)", "教九楼B座201", 0, 4,
            spans = listOf(Span(4, 11))),

        /* ---------------- 周二：通信工程专业导论由 4 位老师按周次轮换 ---------------- */
        Course("c10", "通信工程专业导论", "50310301-1", "课堂派9RBF3T", "陈智雄,岐晓蕾", "教十楼B座402", 1, 1,
            spans = listOf(Span(11, 13))),
        Course("c11", "通信工程专业导论", "50310301-1", "课堂派9RBF3T", "刘涛,岐晓蕾", "教十楼B座402", 1, 1,
            spans = listOf(Span(14, 15))),
        Course("c12", "通信工程专业导论", "50310301-1", "课堂派9RBF3T", "韩东升,岐晓蕾", "教十楼B座402", 1, 1,
            spans = listOf(Span(16, 17))),
        Course("c13", "通信工程专业导论", "50310301-1", "课堂派9RBF3T", "张珂,岐晓蕾", "教十楼B座402", 1, 1,
            spans = listOf(Span(18, 18))),
        Course("c14", "通用英语", "20710001-25", "课堂派E9HPWC", "安国平", "教十一楼A104", 1, 2,
            spans = listOf(Span(4, 19))),
        Course("c15", "形势与政策(1)", "71410211-28", "课堂派FLEB84", "张骞(思政)", "教十一楼A309", 1, 4,
            spans = listOf(Span(15, 16))),
        Course("c16", "军事理论", "51510102-5", "课堂派YXVLWC", "陈怡帆(思政)", "教八楼401", 1, 5,
            spans = listOf(Span(4, 16))),

        /* ---------------- 周三 ---------------- */
        Course("c20", "高等数学(1)", "71010101-6", "课堂派W6L7EN", "严艳", "教十楼B座302", 2, 1,
            spans = listOf(Span(4, 18))),
        Course("c21", "线性代数", "81010103-7", "课堂派C7EKBQ", "王涛", "教九楼B座201", 2, 3,
            spans = listOf(Span(4, 15))),
        Course("c22", "C/C++程序设计", "20910450-6", "课堂派J53HWK", "李静", "教十一楼A323", 2, 4,
            spans = listOf(Span(4, 17))),

        /* ---------------- 周四（Excel 版缺失，按网页版补全） ---------------- */
        Course("c30", "思想道德与法治", "21410411-11", "课堂派SGTGWE", "张蓓蓓", "教十一楼A206", 3, 2,
            spans = listOf(Span(4, 19))),
        Course("c31", "体育(1)", "71210101-85", "匹克球, 课堂派JD34FE", "刘泽", "二校东羽毛球馆(三餐)", 3, 3,
            spans = listOf(Span(4, 18))),

        /* ---------------- 周五 ---------------- */
        Course("c40", "高等数学(1)", "71010101-6", "课堂派W6L7EN", "严艳", "教十楼B座302", 4, 1,
            spans = listOf(Span(4, 17))),
        Course("c41", "通用英语", "20710001-25", "课堂派E9HPWC", "安国平", "教十一楼A104", 4, 2,
            spans = listOf(Span(4, 19)))
    )

    fun timetable(): Timetable = Timetable(
        term = "2026-2027 学年 · 第一学期",
        startDate = java.time.LocalDate.parse(START_DATE),
        weeks = WEEKS,
        courses = courses(),
        notes = NOTES
    )
}

/** 本机持久化：课表 JSON + 提醒设置。 */
class Repo(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("timetable", Context.MODE_PRIVATE)

    var timetable: Timetable = load()
        private set

    private fun load(): Timetable = try {
        val raw = sp.getString("data", null)
        if (raw.isNullOrBlank()) DefaultData.timetable() else TimetableJson.decode(raw)
    } catch (e: Exception) {
        DefaultData.timetable()
    }

    fun save(t: Timetable) {
        timetable = t
        sp.edit().putString("data", TimetableJson.encode(t)).apply()
    }

    fun resetToDefault() {
        save(DefaultData.timetable())
    }

    /* ---------------- 提醒设置 ---------------- */

    var remindersOn: Boolean
        get() = sp.getBoolean("remindersOn", false)
        set(v) = sp.edit().putBoolean("remindersOn", v).apply()

    /** 提前多少分钟提醒 */
    var leadMinutes: Int
        get() = sp.getInt("lead", 10)
        set(v) = sp.edit().putInt("lead", v).apply()
}
