import type { FastDate } from '../types';
import { FastCalendar, daysBetween, todayFastDate } from './fastCalendar';

export type BibleTestament = 'nt' | 'ot';

export interface BibleBook {
  id: string;
  nameAr: string;
  chapters: number;
  testament: BibleTestament;
}

export interface TodayBibleReading {
  bookId: string;
  bookNameAr: string;
  chapterNumber: number;
  totalChaptersInBook: number;
  dayNumber: number;
  isTrialPreview: boolean;
  continuedFromStartBook: boolean;
}

/**
 * Complete canonical Arabic Bible books (New Testament first for quick Nativity Fast access,
 * followed by Old Testament in canonical order).
 */
export const BIBLE_BOOKS: BibleBook[] = [
  // العهد الجديد (27 سفراً)
  { id: 'matthew', nameAr: 'إنجيل معلمنا متى', chapters: 28, testament: 'nt' },
  { id: 'mark', nameAr: 'إنجيل معلمنا مرقس', chapters: 16, testament: 'nt' },
  { id: 'luke', nameAr: 'إنجيل معلمنا لوقا', chapters: 24, testament: 'nt' },
  { id: 'john', nameAr: 'إنجيل معلمنا يوحنا', chapters: 21, testament: 'nt' },
  { id: 'acts', nameAr: 'سفر أعمال الرسل', chapters: 28, testament: 'nt' },
  { id: 'romans', nameAr: 'رسالة رومية', chapters: 16, testament: 'nt' },
  { id: '1corinthians', nameAr: 'رسالة كورنثوس الأولى', chapters: 16, testament: 'nt' },
  { id: '2corinthians', nameAr: 'رسالة كورنثوس الثانية', chapters: 13, testament: 'nt' },
  { id: 'galatians', nameAr: 'رسالة غلاطية', chapters: 6, testament: 'nt' },
  { id: 'ephesians', nameAr: 'رسالة أفسس', chapters: 6, testament: 'nt' },
  { id: 'philippians', nameAr: 'رسالة فيلبي', chapters: 4, testament: 'nt' },
  { id: 'colossians', nameAr: 'رسالة كولوسي', chapters: 4, testament: 'nt' },
  { id: '1thessalonians', nameAr: 'رسالة تسالونيكي الأولى', chapters: 5, testament: 'nt' },
  { id: '2thessalonians', nameAr: 'رسالة تسالونيكي الثانية', chapters: 3, testament: 'nt' },
  { id: '1timothy', nameAr: 'رسالة تيموثاوس الأولى', chapters: 6, testament: 'nt' },
  { id: '2timothy', nameAr: 'رسالة تيموثاوس الثانية', chapters: 4, testament: 'nt' },
  { id: 'titus', nameAr: 'رسالة تيطس', chapters: 3, testament: 'nt' },
  { id: 'philemon', nameAr: 'رسالة فليمون', chapters: 1, testament: 'nt' },
  { id: 'hebrews', nameAr: 'رسالة العبرانيين', chapters: 13, testament: 'nt' },
  { id: 'james', nameAr: 'رسالة معلمنا يعقوب', chapters: 5, testament: 'nt' },
  { id: '1peter', nameAr: 'رسالة بطرس الأولى', chapters: 5, testament: 'nt' },
  { id: '2peter', nameAr: 'رسالة بطرس الثانية', chapters: 3, testament: 'nt' },
  { id: '1john', nameAr: 'رسالة يوحنا الأولى', chapters: 5, testament: 'nt' },
  { id: '2john', nameAr: 'رسالة يوحنا الثانية', chapters: 1, testament: 'nt' },
  { id: '3john', nameAr: 'رسالة يوحنا الثالثة', chapters: 1, testament: 'nt' },
  { id: 'jude', nameAr: 'رسالة معلمنا يهوذا', chapters: 1, testament: 'nt' },
  { id: 'revelation', nameAr: 'سفر رؤيا يوحنا اللاهوتي', chapters: 22, testament: 'nt' },

  // العهد القديم (39 سفراً)
  { id: 'genesis', nameAr: 'سفر التكوين', chapters: 50, testament: 'ot' },
  { id: 'exodus', nameAr: 'سفر الخروج', chapters: 40, testament: 'ot' },
  { id: 'leviticus', nameAr: 'سفر اللاويين', chapters: 27, testament: 'ot' },
  { id: 'numbers', nameAr: 'سفر العدد', chapters: 36, testament: 'ot' },
  { id: 'deuteronomy', nameAr: 'سفر التثنية', chapters: 34, testament: 'ot' },
  { id: 'joshua', nameAr: 'سفر يشوع', chapters: 24, testament: 'ot' },
  { id: 'judges', nameAr: 'سفر القضاة', chapters: 21, testament: 'ot' },
  { id: 'ruth', nameAr: 'سفر راعوث', chapters: 4, testament: 'ot' },
  { id: '1samuel', nameAr: 'سفر صموئيل الأول', chapters: 31, testament: 'ot' },
  { id: '2samuel', nameAr: 'سفر صموئيل الثاني', chapters: 24, testament: 'ot' },
  { id: '1kings', nameAr: 'سفر الملوك الأول', chapters: 22, testament: 'ot' },
  { id: '2kings', nameAr: 'سفر الملوك الثاني', chapters: 25, testament: 'ot' },
  { id: '1chronicles', nameAr: 'سفر أخبار الأيام الأول', chapters: 29, testament: 'ot' },
  { id: '2chronicles', nameAr: 'سفر أخبار الأيام الثاني', chapters: 36, testament: 'ot' },
  { id: 'ezra', nameAr: 'سفر عزرا', chapters: 10, testament: 'ot' },
  { id: 'nehemiah', nameAr: 'سفر نحميا', chapters: 13, testament: 'ot' },
  { id: 'esther', nameAr: 'سفر أستير', chapters: 10, testament: 'ot' },
  { id: 'job', nameAr: 'سفر أيوب', chapters: 42, testament: 'ot' },
  { id: 'psalms', nameAr: 'سفر المزامير', chapters: 150, testament: 'ot' },
  { id: 'proverbs', nameAr: 'سفر الأمثال', chapters: 31, testament: 'ot' },
  { id: 'ecclesiastes', nameAr: 'سفر الجامعة', chapters: 12, testament: 'ot' },
  { id: 'song', nameAr: 'سفر نشيد الأنشاد', chapters: 8, testament: 'ot' },
  { id: 'isaiah', nameAr: 'سفر إشعياء النبي', chapters: 66, testament: 'ot' },
  { id: 'jeremiah', nameAr: 'سفر إرميا النبي', chapters: 52, testament: 'ot' },
  { id: 'lamentations', nameAr: 'سفر مراثي إرميا', chapters: 5, testament: 'ot' },
  { id: 'ezekiel', nameAr: 'سفر حزقيال النبي', chapters: 48, testament: 'ot' },
  { id: 'daniel', nameAr: 'سفر دانيال النبي', chapters: 12, testament: 'ot' },
  { id: 'hosea', nameAr: 'سفر هوشع', chapters: 14, testament: 'ot' },
  { id: 'joel', nameAr: 'سفر يوئيل', chapters: 3, testament: 'ot' },
  { id: 'amos', nameAr: 'سفر عاموس', chapters: 9, testament: 'ot' },
  { id: 'obadiah', nameAr: 'سفر عوبديا', chapters: 1, testament: 'ot' },
  { id: 'jonah', nameAr: 'سفر يونان', chapters: 4, testament: 'ot' },
  { id: 'micah', nameAr: 'سفر ميخا', chapters: 7, testament: 'ot' },
  { id: 'nahum', nameAr: 'سفر ناحوم', chapters: 3, testament: 'ot' },
  { id: 'habakkuk', nameAr: 'سفر حبقوق', chapters: 3, testament: 'ot' },
  { id: 'zephaniah', nameAr: 'سفر صفنيا', chapters: 3, testament: 'ot' },
  { id: 'haggai', nameAr: 'سفر حجي', chapters: 2, testament: 'ot' },
  { id: 'zechariah', nameAr: 'سفر زكريا', chapters: 14, testament: 'ot' },
  { id: 'malachi', nameAr: 'سفر ملاخي', chapters: 4, testament: 'ot' },
];

export const DEFAULT_BIBLE_BOOK_ID = 'luke';

export function getBibleBookById(bookId: string): BibleBook {
  return (
    BIBLE_BOOKS.find((b) => b.id === bookId) ??
    BIBLE_BOOKS.find((b) => b.id === DEFAULT_BIBLE_BOOK_ID) ??
    BIBLE_BOOKS[0]
  );
}

function parseIsoFastDate(iso?: string): FastDate | null {
  if (!iso || iso.length !== 10) return null;
  const parts = iso.split('-').map((p) => Number.parseInt(p, 10));
  if (parts.length !== 3 || parts.some((n) => Number.isNaN(n))) return null;
  return { year: parts[0], month: parts[1], day: parts[2] };
}

/**
 * Computes today's Bible chapter starting from Chapter 1 of the chosen book on Day 1
 * of the Nativity Fast (30/11/2026). During trial mode prior to 30/11/2026, counts from
 * `startDateIso` (defaulting to today = Day 1) so users immediately experience daily chapter
 * progression, and automatically resets to Chapter 1 when the official fast starts.
 * When a book finishes, automatically continues to Chapter 1 of the next canonical book.
 */
export function getTodayBibleReading(
  startBookId: string,
  startDateIso?: string,
  today: FastDate = todayFastDate()
): TodayBibleReading {
  const isTrialPreview = FastCalendar.isTrialMode(today);
  let dayNumber: number;

  if (isTrialPreview) {
    const parsedStart = parseIsoFastDate(startDateIso);
    dayNumber = parsedStart ? Math.max(daysBetween(parsedStart, today) + 1, 1) : 1;
  } else {
    dayNumber = Math.max(FastCalendar.currentFastDayIndex(today), 1);
  }

  const startIdx = Math.max(
    BIBLE_BOOKS.findIndex((b) => b.id === startBookId),
    0
  );

  let remainingOffset = dayNumber - 1;
  let currentIdx = startIdx;

  while (remainingOffset >= BIBLE_BOOKS[currentIdx].chapters) {
    remainingOffset -= BIBLE_BOOKS[currentIdx].chapters;
    currentIdx = (currentIdx + 1) % BIBLE_BOOKS.length;
  }

  const currentBook = BIBLE_BOOKS[currentIdx];
  const chapterNumber = remainingOffset + 1;

  return {
    bookId: currentBook.id,
    bookNameAr: currentBook.nameAr,
    chapterNumber,
    totalChaptersInBook: currentBook.chapters,
    dayNumber,
    isTrialPreview,
    continuedFromStartBook: currentIdx !== startIdx,
  };
}

export function formatBibleReminderNotification(reading: TodayBibleReading): {
  title: string;
  body: string;
} {
  const dayTag = reading.isTrialPreview
    ? `يوم ${reading.dayNumber} (تجريبي)`
    : `اليوم ${reading.dayNumber} من التحدي`;
  return {
    title: 'ورد الكتاب المقدس اليوم 📖',
    body: `إصحاح اليوم: ${reading.bookNameAr} — الإصحاح ${reading.chapterNumber} (${dayTag}). اقرأه بتأمل وسجّل إنجازك في تحدي طريق المزود!`,
  };
}
