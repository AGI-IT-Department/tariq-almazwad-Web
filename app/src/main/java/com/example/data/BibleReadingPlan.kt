package com.example.data

enum class BibleTestament {
    NT,
    OT
}

data class BibleBook(
    val id: String,
    val nameAr: String,
    val chapters: Int,
    val testament: BibleTestament
)

data class TodayBibleReading(
    val bookId: String,
    val bookNameAr: String,
    val chapterNumber: Int,
    val totalChaptersInBook: Int,
    val dayNumber: Int,
    val isTrialPreview: Boolean,
    val continuedFromStartBook: Boolean
)

object BibleReadingPlan {
    const val DEFAULT_BOOK_ID = "luke"

    /**
     * Complete canonical Arabic Bible books (New Testament first for quick Nativity Fast access,
     * followed by Old Testament in canonical order).
     */
    val allBooks: List<BibleBook> = listOf(
        // العهد الجديد (27 سفراً)
        BibleBook("matthew", "إنجيل معلمنا متى", 28, BibleTestament.NT),
        BibleBook("mark", "إنجيل معلمنا مرقس", 16, BibleTestament.NT),
        BibleBook("luke", "إنجيل معلمنا لوقا", 24, BibleTestament.NT),
        BibleBook("john", "إنجيل معلمنا يوحنا", 21, BibleTestament.NT),
        BibleBook("acts", "سفر أعمال الرسل", 28, BibleTestament.NT),
        BibleBook("romans", "رسالة رومية", 16, BibleTestament.NT),
        BibleBook("1corinthians", "رسالة كورنثوس الأولى", 16, BibleTestament.NT),
        BibleBook("2corinthians", "رسالة كورنثوس الثانية", 13, BibleTestament.NT),
        BibleBook("galatians", "رسالة غلاطية", 6, BibleTestament.NT),
        BibleBook("ephesians", "رسالة أفسس", 6, BibleTestament.NT),
        BibleBook("philippians", "رسالة فيلبي", 4, BibleTestament.NT),
        BibleBook("colossians", "رسالة كولوسي", 4, BibleTestament.NT),
        BibleBook("1thessalonians", "رسالة تسالونيكي الأولى", 5, BibleTestament.NT),
        BibleBook("2thessalonians", "رسالة تسالونيكي الثانية", 3, BibleTestament.NT),
        BibleBook("1timothy", "رسالة تيموثاوس الأولى", 6, BibleTestament.NT),
        BibleBook("2timothy", "رسالة تيموثاوس الثانية", 4, BibleTestament.NT),
        BibleBook("titus", "رسالة تيطس", 3, BibleTestament.NT),
        BibleBook("philemon", "رسالة فليمون", 1, BibleTestament.NT),
        BibleBook("hebrews", "رسالة العبرانيين", 13, BibleTestament.NT),
        BibleBook("james", "رسالة معلمنا يعقوب", 5, BibleTestament.NT),
        BibleBook("1peter", "رسالة بطرس الأولى", 5, BibleTestament.NT),
        BibleBook("2peter", "رسالة بطرس الثانية", 3, BibleTestament.NT),
        BibleBook("1john", "رسالة يوحنا الأولى", 5, BibleTestament.NT),
        BibleBook("2john", "رسالة يوحنا الثانية", 1, BibleTestament.NT),
        BibleBook("3john", "رسالة يوحنا الثالثة", 1, BibleTestament.NT),
        BibleBook("jude", "رسالة معلمنا يهوذا", 1, BibleTestament.NT),
        BibleBook("revelation", "سفر رؤيا يوحنا اللاهوتي", 22, BibleTestament.NT),

        // العهد القديم (39 سفراً)
        BibleBook("genesis", "سفر التكوين", 50, BibleTestament.OT),
        BibleBook("exodus", "سفر الخروج", 40, BibleTestament.OT),
        BibleBook("leviticus", "سفر اللاويين", 27, BibleTestament.OT),
        BibleBook("numbers", "سفر العدد", 36, BibleTestament.OT),
        BibleBook("deuteronomy", "سفر التثنية", 34, BibleTestament.OT),
        BibleBook("joshua", "سفر يشوع", 24, BibleTestament.OT),
        BibleBook("judges", "سفر القضاة", 21, BibleTestament.OT),
        BibleBook("ruth", "سفر راعوث", 4, BibleTestament.OT),
        BibleBook("1samuel", "سفر صموئيل الأول", 31, BibleTestament.OT),
        BibleBook("2samuel", "سفر صموئيل الثاني", 24, BibleTestament.OT),
        BibleBook("1kings", "سفر الملوك الأول", 22, BibleTestament.OT),
        BibleBook("2kings", "سفر الملوك الثاني", 25, BibleTestament.OT),
        BibleBook("1chronicles", "سفر أخبار الأيام الأول", 29, BibleTestament.OT),
        BibleBook("2chronicles", "سفر أخبار الأيام الثاني", 36, BibleTestament.OT),
        BibleBook("ezra", "سفر عزرا", 10, BibleTestament.OT),
        BibleBook("nehemiah", "سفر نحميا", 13, BibleTestament.OT),
        BibleBook("esther", "سفر أستير", 10, BibleTestament.OT),
        BibleBook("job", "سفر أيوب", 42, BibleTestament.OT),
        BibleBook("psalms", "سفر المزامير", 150, BibleTestament.OT),
        BibleBook("proverbs", "سفر الأمثال", 31, BibleTestament.OT),
        BibleBook("ecclesiastes", "سفر الجامعة", 12, BibleTestament.OT),
        BibleBook("song", "سفر نشيد الأنشاد", 8, BibleTestament.OT),
        BibleBook("isaiah", "سفر إشعياء النبي", 66, BibleTestament.OT),
        BibleBook("jeremiah", "سفر إرميا النبي", 52, BibleTestament.OT),
        BibleBook("lamentations", "سفر مراثي إرميا", 5, BibleTestament.OT),
        BibleBook("ezekiel", "سفر حزقيال النبي", 48, BibleTestament.OT),
        BibleBook("daniel", "سفر دانيال النبي", 12, BibleTestament.OT),
        BibleBook("hosea", "سفر هوشع", 14, BibleTestament.OT),
        BibleBook("joel", "سفر يوئيل", 3, BibleTestament.OT),
        BibleBook("amos", "سفر عاموس", 9, BibleTestament.OT),
        BibleBook("obadiah", "سفر عوبديا", 1, BibleTestament.OT),
        BibleBook("jonah", "سفر يونان", 4, BibleTestament.OT),
        BibleBook("micah", "سفر ميخا", 7, BibleTestament.OT),
        BibleBook("nahum", "سفر ناحوم", 3, BibleTestament.OT),
        BibleBook("habakkuk", "سفر حبقوق", 3, BibleTestament.OT),
        BibleBook("zephaniah", "سفر صفنيا", 3, BibleTestament.OT),
        BibleBook("haggai", "سفر حجي", 2, BibleTestament.OT),
        BibleBook("zechariah", "سفر زكريا", 14, BibleTestament.OT),
        BibleBook("malachi", "سفر ملاخي", 4, BibleTestament.OT)
    )

    fun getBookById(bookId: String): BibleBook =
        allBooks.firstOrNull { it.id == bookId }
            ?: allBooks.firstOrNull { it.id == DEFAULT_BOOK_ID }
            ?: allBooks.first()

    private fun parseIsoFastDate(iso: String): FastDate? {
        if (iso.length != 10) return null
        val parts = iso.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return FastDate(y, m, d)
    }

    /**
     * Computes today's Bible chapter starting from Chapter 1 of the chosen book on Day 1
     * of the Nativity Fast (30/11/2026). During trial mode before 30/11/2026, counts from
     * [startDateIso] (defaulting to today = Day 1) so daily progression works immediately,
     * then resets cleanly to Chapter 1 on Day 1 when the official challenge starts.
     */
    fun getTodayReading(
        startBookId: String,
        startDateIso: String = "",
        today: FastDate = FastDate.today()
    ): TodayBibleReading {
        val isTrialPreview = FastCalendar.isTrialMode(today)
        val dayNumber = if (isTrialPreview) {
            val parsedStart = parseIsoFastDate(startDateIso)
            if (parsedStart != null) {
                (FastDate.daysBetween(parsedStart, today).toInt() + 1).coerceAtLeast(1)
            } else {
                1
            }
        } else {
            FastCalendar.currentFastDayIndex(today).coerceAtLeast(1)
        }

        val startIdx = allBooks.indexOfFirst { it.id == startBookId }.let { if (it >= 0) it else 2 }
        var remainingOffset = dayNumber - 1
        var currentIdx = startIdx

        while (remainingOffset >= allBooks[currentIdx].chapters) {
            remainingOffset -= allBooks[currentIdx].chapters
            currentIdx = (currentIdx + 1) % allBooks.size
        }

        val currentBook = allBooks[currentIdx]
        return TodayBibleReading(
            bookId = currentBook.id,
            bookNameAr = currentBook.nameAr,
            chapterNumber = remainingOffset + 1,
            totalChaptersInBook = currentBook.chapters,
            dayNumber = dayNumber,
            isTrialPreview = isTrialPreview,
            continuedFromStartBook = currentIdx != startIdx
        )
    }

    fun formatReminderNotification(reading: TodayBibleReading): Pair<String, String> {
        val dayTag = if (reading.isTrialPreview) {
            "يوم ${reading.dayNumber} (تجريبي)"
        } else {
            "اليوم ${reading.dayNumber} من التحدي"
        }
        val title = "ورد الكتاب المقدس اليوم 📖"
        val body = "إصحاح اليوم: ${reading.bookNameAr} — الإصحاح ${reading.chapterNumber} ($dayTag). اقرأه بتأمل وسجّل إنجازك في تحدي طريق المزود!"
        return title to body
    }
}
