import type { FastDate } from '../types';
import { todayFastDate, toIsoString } from './fastCalendar';

export const INCARNATION_BOOK_PDF_PATH =
  '/assets/aistudio/24_______________________________________christianlib_com_fa7aebe4.pdf';

export interface WeeklySyllabusItem {
  weekNumber: number;
  titleAr: string;
  dateRangeAr: string;
  startIso: string; // yyyy-MM-dd
  endIso: string;   // yyyy-MM-dd
  bibleRangeAr: string;
  incarnationBookAr: string;
  pdfStartPage: number; // 1-indexed PDF page in attached book
  pdfEndPage: number;   // 1-indexed PDF page in attached book
  examLabelAr: string;
  examDateAr: string;
  isFinalWeek?: boolean;
}

export const WEEKLY_SYLLABUS: WeeklySyllabusItem[] = [
  {
    weekNumber: 1,
    titleAr: 'الأسبوع 1',
    dateRangeAr: '30 نوفمبر – 6 ديسمبر',
    startIso: '2026-11-30',
    endIso: '2026-12-06',
    bibleRangeAr: 'إنجيل لوقا (إصحاح 1 – 7) — إصحاح يومياً',
    incarnationBookAr: 'سيرة القديس أثناسيوس + التمهيد + الفصل الأول (ص 7 – 20 | الفقرات 1 : 7)',
    pdfStartPage: 7,
    pdfEndPage: 19,
    examLabelAr: 'موعد المسابقة',
    examDateAr: 'الأحد 6 ديسمبر',
  },
  {
    weekNumber: 2,
    titleAr: 'الأسبوع 2',
    dateRangeAr: '7 ديسمبر – 13 ديسمبر',
    startIso: '2026-12-07',
    endIso: '2026-12-13',
    bibleRangeAr: 'إنجيل لوقا (إصحاح 8 – 14) — إصحاح يومياً',
    incarnationBookAr: 'الفصل الثاني والفصل الثالث (ص 21 – 32 | الفقرات 8 : 16)',
    pdfStartPage: 20,
    pdfEndPage: 31,
    examLabelAr: 'موعد المسابقة',
    examDateAr: 'الأحد 13 ديسمبر',
  },
  {
    weekNumber: 3,
    titleAr: 'الأسبوع 3',
    dateRangeAr: '14 ديسمبر – 20 ديسمبر',
    startIso: '2026-12-14',
    endIso: '2026-12-20',
    bibleRangeAr: 'إنجيل لوقا (إصحاح 15 – 21) — إصحاح يومياً',
    incarnationBookAr: 'الفصل الرابع كاملاً (ص 33 – 53 | الفقرات 17 : 32)',
    pdfStartPage: 32,
    pdfEndPage: 52,
    examLabelAr: 'موعد المسابقة',
    examDateAr: 'الأحد 20 ديسمبر',
  },
  {
    weekNumber: 4,
    titleAr: 'الأسبوع 4',
    dateRangeAr: '21 ديسمبر – 27 ديسمبر',
    startIso: '2026-12-21',
    endIso: '2026-12-27',
    bibleRangeAr: 'لوقا (22 – 24) + أفسس (1 – 4) — إصحاح يومياً',
    incarnationBookAr: 'الفصل الخامس كاملاً (ص 55 – 66 | الفقرات 33 : 40)',
    pdfStartPage: 53,
    pdfEndPage: 64,
    examLabelAr: 'موعد المسابقة',
    examDateAr: 'الأحد 27 ديسمبر',
  },
  {
    weekNumber: 5,
    titleAr: 'الأسبوع 5 والختامي',
    dateRangeAr: '28 ديسمبر – 5 يناير',
    startIso: '2026-12-28',
    endIso: '2027-01-05',
    bibleRangeAr: 'أفسس (5 – 6) + فيلبي (1 – 4) + كولوسي (1 – 4)',
    incarnationBookAr: 'الفصل السادس كاملاً (ص 67 – 87 | الفقرات 41 : 57)',
    pdfStartPage: 65,
    pdfEndPage: 85,
    examLabelAr: 'موعد المسابقة الختامية الكبرى',
    examDateAr: 'الثلاثاء 5 يناير',
    isFinalWeek: true,
  },
];

export interface ActiveSyllabusStatus {
  activeWeek: WeeklySyllabusItem;
  phase: 'upcoming' | 'active' | 'completed';
}

export function getActiveSyllabusWeek(today: FastDate = todayFastDate()): ActiveSyllabusStatus {
  const iso = toIsoString(today);
  if (iso < WEEKLY_SYLLABUS[0].startIso) {
    return { activeWeek: WEEKLY_SYLLABUS[0], phase: 'upcoming' };
  }
  const last = WEEKLY_SYLLABUS[WEEKLY_SYLLABUS.length - 1];
  if (iso > last.endIso) {
    return { activeWeek: last, phase: 'completed' };
  }
  const matched =
    WEEKLY_SYLLABUS.find((w) => iso >= w.startIso && iso <= w.endIso) ?? WEEKLY_SYLLABUS[0];
  return { activeWeek: matched, phase: 'active' };
}

/* ---------------- Official 37-Day Daily Bible Reading Plan (In-App Reader) ---------------- */

export interface OfficialBibleDayPlan {
  dayNumber: number; // 1..37
  weekNumber: number; // 1..5
  dateIso: string; // yyyy-MM-dd
  dateLabelAr: string; // e.g. "30 نوفمبر"
  bookId: number; // Bolls SVD: Luke=42, Eph=49, Phil=50, Col=51
  bookNameAr: string;
  chapters: number[];
  shortLabelAr: string;
  fullLabelAr: string;
}

const PLAN_MONTH_AR: Record<number, string> = {
  1: 'يناير',
  11: 'نوفمبر',
  12: 'ديسمبر',
};

function addDaysFromNov30(offset: number): { iso: string; labelAr: string } {
  const d = new Date(Date.UTC(2026, 10, 30 + offset, 12, 0, 0));
  const year = d.getUTCFullYear();
  const month = d.getUTCMonth() + 1;
  const day = d.getUTCDate();
  const iso = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  return { iso, labelAr: `${day} ${PLAN_MONTH_AR[month] ?? month}` };
}

const RAW_BIBLE_DAYS: Array<{
  bookId: number;
  bookNameAr: string;
  chapters: number[];
  shortLabelAr: string;
  fullLabelAr: string;
}> = [
  // Luke 1..24 (Days 1..24)
  ...Array.from({ length: 24 }, (_, idx) => {
    const ch = idx + 1;
    return {
      bookId: 42,
      bookNameAr: 'إنجيل لوقا',
      chapters: [ch],
      shortLabelAr: `لوقا ${ch}`,
      fullLabelAr: `إنجيل لوقا — الإصحاح ${ch}`,
    };
  }),
  // Ephesians 1..6 (Days 25..30)
  ...Array.from({ length: 6 }, (_, idx) => {
    const ch = idx + 1;
    return {
      bookId: 49,
      bookNameAr: 'رسالة أفسس',
      chapters: [ch],
      shortLabelAr: `أفسس ${ch}`,
      fullLabelAr: `رسالة أفسس — الإصحاح ${ch}`,
    };
  }),
  // Philippians 1..4 (Days 31..34)
  ...Array.from({ length: 4 }, (_, idx) => {
    const ch = idx + 1;
    return {
      bookId: 50,
      bookNameAr: 'رسالة فيلبي',
      chapters: [ch],
      shortLabelAr: `فيلبي ${ch}`,
      fullLabelAr: `رسالة فيلبي — الإصحاح ${ch}`,
    };
  }),
  // Colossians 1..2 + 3-4 (Days 35..37)
  {
    bookId: 51,
    bookNameAr: 'رسالة كولوسي',
    chapters: [1],
    shortLabelAr: 'كولوسي 1',
    fullLabelAr: 'رسالة كولوسي — الإصحاح 1',
  },
  {
    bookId: 51,
    bookNameAr: 'رسالة كولوسي',
    chapters: [2],
    shortLabelAr: 'كولوسي 2',
    fullLabelAr: 'رسالة كولوسي — الإصحاح 2',
  },
  {
    bookId: 51,
    bookNameAr: 'رسالة كولوسي',
    chapters: [3, 4],
    shortLabelAr: 'كولوسي 3 – 4',
    fullLabelAr: 'رسالة كولوسي — الإصحاحان 3 و 4',
  },
];

export const OFFICIAL_DAILY_BIBLE_PLAN: OfficialBibleDayPlan[] = RAW_BIBLE_DAYS.map((entry, idx) => {
  const dayNumber = idx + 1;
  const weekNumber = Math.min(Math.floor(idx / 7) + 1, 5);
  const { iso, labelAr } = addDaysFromNov30(idx);
  return {
    dayNumber,
    weekNumber,
    dateIso: iso,
    dateLabelAr: labelAr,
    ...entry,
  };
});

export function getTodayOfficialBiblePlan(today: FastDate = todayFastDate()): OfficialBibleDayPlan {
  const iso = toIsoString(today);
  if (iso <= OFFICIAL_DAILY_BIBLE_PLAN[0].dateIso) {
    return OFFICIAL_DAILY_BIBLE_PLAN[0];
  }
  const last = OFFICIAL_DAILY_BIBLE_PLAN[OFFICIAL_DAILY_BIBLE_PLAN.length - 1];
  if (iso >= last.dateIso) {
    return last;
  }
  return OFFICIAL_DAILY_BIBLE_PLAN.find((d) => d.dateIso === iso) ?? OFFICIAL_DAILY_BIBLE_PLAN[0];
}

export interface BibleVerseItem {
  chapter: number;
  verse: number;
  text: string;
}

const bibleChapterCache = new Map<string, BibleVerseItem[]>();

function cleanBibleVerseHtml(raw: string): string {
  return raw
    .replace(/<S>.*?<\/S>/gi, '')
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

export async function fetchArabicBibleChapters(
  bookId: number,
  chapters: number[]
): Promise<BibleVerseItem[]> {
  const allVerses: BibleVerseItem[] = [];
  for (const chapter of chapters) {
    const cacheKey = `${bookId}:${chapter}`;
    const cached = bibleChapterCache.get(cacheKey);
    if (cached) {
      allVerses.push(...cached);
      continue;
    }
    const resp = await fetch(`https://bolls.life/get-text/SVD/${bookId}/${chapter}/`);
    if (!resp.ok) {
      throw new Error(`HTTP ${resp.status}`);
    }
    const data = (await resp.json()) as Array<{ verse?: number; text?: string }>;
    if (!Array.isArray(data) || data.length === 0) {
      throw new Error('Empty chapter response');
    }
    const parsed: BibleVerseItem[] = data
      .filter((item) => item && typeof item.text === 'string')
      .map((item, idx) => ({
        chapter,
        verse: typeof item.verse === 'number' ? item.verse : idx + 1,
        text: cleanBibleVerseHtml(String(item.text ?? '')),
      }))
      .filter((v) => v.text.length > 0);
    bibleChapterCache.set(cacheKey, parsed);
    allVerses.push(...parsed);
  }
  return allVerses;
}

/* ---------------- Backward-Compatible In-App Quiz Codec ---------------- */
// Encodes optional MCQ / True-False questions inside the existing `app_config.weekly_quiz_question`
// column so zero Supabase table or column changes are required.

export type QuizQuestionType = 'mcq' | 'tf';

export interface WeeklyQuizQuestion {
  id: string;
  type: QuizQuestionType;
  prompt: string;
  options: string[];
  correctIndex: number;
}

export interface DecodedQuizConfig {
  introText: string;
  questions: WeeklyQuizQuestion[];
}

const QUIZ_JSON_MARKER = '\n<!--QUIZ_JSON-->\n';

export function decodeQuizConfig(raw: string): DecodedQuizConfig {
  const safeRaw = raw || '';
  const idx = safeRaw.indexOf(QUIZ_JSON_MARKER);
  if (idx === -1) {
    return { introText: safeRaw.trim(), questions: [] };
  }
  const introText = safeRaw.slice(0, idx).trim();
  const jsonPart = safeRaw.slice(idx + QUIZ_JSON_MARKER.length).trim();
  try {
    const parsed = JSON.parse(jsonPart);
    if (!Array.isArray(parsed)) {
      return { introText, questions: [] };
    }
    const questions: WeeklyQuizQuestion[] = parsed
      .filter((item) => item && typeof item === 'object' && typeof item.prompt === 'string')
      .map((item, index) => {
        const type: QuizQuestionType = item.type === 'tf' ? 'tf' : 'mcq';
        const rawOpts = Array.isArray(item.options)
          ? item.options.map((o: unknown) => String(o ?? '').trim())
          : type === 'tf'
            ? ['صح', 'خطأ']
            : [];
        const options = type === 'tf' ? ['صح', 'خطأ'] : rawOpts.filter(Boolean);
        const correctIndex =
          typeof item.correctIndex === 'number' &&
          item.correctIndex >= 0 &&
          item.correctIndex < options.length
            ? item.correctIndex
            : 0;
        return {
          id: typeof item.id === 'string' && item.id ? item.id : `q_${index + 1}`,
          type,
          prompt: String(item.prompt).trim(),
          options,
          correctIndex,
        };
      })
      .filter((q) => q.prompt.length > 0 && q.options.length >= 2);

    return { introText, questions };
  } catch {
    return { introText, questions: [] };
  }
}

export function encodeQuizConfig(introText: string, questions: WeeklyQuizQuestion[]): string {
  const cleanIntro = introText.trim();
  const validQuestions = questions
    .map((q) => {
      const options =
        q.type === 'tf'
          ? ['صح', 'خطأ']
          : q.options.map((o) => o.trim()).filter((o) => o.length > 0);
      return {
        id: q.id || `q_${Math.random().toString(36).slice(2, 8)}`,
        type: q.type,
        prompt: q.prompt.trim(),
        options,
        correctIndex: Math.min(Math.max(q.correctIndex, 0), Math.max(options.length - 1, 0)),
      };
    })
    .filter((q) => q.prompt.length > 0 && q.options.length >= 2);

  if (validQuestions.length === 0) {
    return cleanIntro;
  }
  return `${cleanIntro}${QUIZ_JSON_MARKER}${JSON.stringify(validQuestions)}`;
}
