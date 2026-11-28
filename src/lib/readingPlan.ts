import type { FastDate } from '../types';
import { compareFastDate, daysBetween, FastCalendar, todayFastDate } from './fastCalendar';

export type QuizQuestionType = 'mcq' | 'tf';

export interface QuizQuestion {
  id: string;
  prompt: string;
  type: QuizQuestionType;
  options: string[];
  correctIndex: number;
}

export interface WeeklySyllabus {
  weekNumber: number; // 1..5
  titleAr: string;
  shortLabelAr: string;
  dateRangeAr: string;
  bibleTitleAr: string;
  bibleDetailAr: string;
  patristicTitleAr: string;
  patristicDetailAr: string;
  quizTitleAr: string;
  quizDateAr: string;
  quizPoints: number;
  startDate: FastDate;
  endDate: FastDate;
  quizDate: FastDate;
}

export const WEEKLY_SYLLABUS: WeeklySyllabus[] = [
  {
    weekNumber: 1,
    titleAr: 'الأسبوع الأول',
    shortLabelAr: 'الأسبوع 1',
    dateRangeAr: '30 نوفمبر – 6 ديسمبر',
    bibleTitleAr: 'إنجيل لوقا (1 – 7)',
    bibleDetailAr: 'إصحاح يومياً من إنجيل معلمنا لوقا البشير',
    patristicTitleAr: 'المقدمة + التمهيد + الفصل الأول',
    patristicDetailAr: 'ص 7 – ص 20 • فقرات 1 – 7',
    quizTitleAr: 'مسابقة الأسبوع الأول',
    quizDateAr: 'الأحد 6 ديسمبر',
    quizPoints: 200,
    startDate: { year: 2026, month: 11, day: 30 },
    endDate: { year: 2026, month: 12, day: 6 },
    quizDate: { year: 2026, month: 12, day: 6 },
  },
  {
    weekNumber: 2,
    titleAr: 'الأسبوع الثاني',
    shortLabelAr: 'الأسبوع 2',
    dateRangeAr: '7 ديسمبر – 13 ديسمبر',
    bibleTitleAr: 'إنجيل لوقا (8 – 14)',
    bibleDetailAr: 'إصحاح يومياً من إنجيل معلمنا لوقا البشير',
    patristicTitleAr: 'الفصل الثاني + الفصل الثالث',
    patristicDetailAr: 'ص 21 – ص 32 • فقرات 8 – 16',
    quizTitleAr: 'مسابقة الأسبوع الثاني',
    quizDateAr: 'الأحد 13 ديسمبر',
    quizPoints: 200,
    startDate: { year: 2026, month: 12, day: 7 },
    endDate: { year: 2026, month: 12, day: 13 },
    quizDate: { year: 2026, month: 12, day: 13 },
  },
  {
    weekNumber: 3,
    titleAr: 'الأسبوع الثالث',
    shortLabelAr: 'الأسبوع 3',
    dateRangeAr: '14 ديسمبر – 20 ديسمبر',
    bibleTitleAr: 'إنجيل لوقا (15 – 21)',
    bibleDetailAr: 'إصحاح يومياً من إنجيل معلمنا لوقا البشير',
    patristicTitleAr: 'الفصل الرابع كاملاً',
    patristicDetailAr: 'ص 33 – ص 53 • فقرات 17 – 32',
    quizTitleAr: 'مسابقة الأسبوع الثالث',
    quizDateAr: 'الأحد 20 ديسمبر',
    quizPoints: 200,
    startDate: { year: 2026, month: 12, day: 14 },
    endDate: { year: 2026, month: 12, day: 20 },
    quizDate: { year: 2026, month: 12, day: 20 },
  },
  {
    weekNumber: 4,
    titleAr: 'الأسبوع الرابع',
    shortLabelAr: 'الأسبوع 4',
    dateRangeAr: '21 ديسمبر – 27 ديسمبر',
    bibleTitleAr: 'لوقا (22 – 24) + أفسس (1 – 4)',
    bibleDetailAr: 'إصحاح يومياً: ختام لوقا وبداية رسالة أفسس',
    patristicTitleAr: 'الفصل الخامس كاملاً',
    patristicDetailAr: 'ص 55 – ص 66 • فقرات 33 – 40',
    quizTitleAr: 'مسابقة الأسبوع الرابع',
    quizDateAr: 'الأحد 27 ديسمبر',
    quizPoints: 200,
    startDate: { year: 2026, month: 12, day: 21 },
    endDate: { year: 2026, month: 12, day: 27 },
    quizDate: { year: 2026, month: 12, day: 27 },
  },
  {
    weekNumber: 5,
    titleAr: 'الأسبوع الخامس والختامي',
    shortLabelAr: 'الأسبوع 5 (الختامي)',
    dateRangeAr: '28 ديسمبر – 5 يناير',
    bibleTitleAr: 'أفسس (5 – 6) + فيلبي (1 – 4) + كولوسي (1 – 4)',
    bibleDetailAr: 'ختام رسائل الأسر وفرح الميلاد المجيد',
    patristicTitleAr: 'الفصل السادس كاملاً',
    patristicDetailAr: 'ص 67 – ص 87 • فقرات 41 – 57',
    quizTitleAr: 'المسابقة الختامية الكبرى',
    quizDateAr: 'الثلاثاء 5 يناير',
    quizPoints: 200,
    startDate: { year: 2026, month: 12, day: 28 },
    endDate: { year: 2027, month: 1, day: 5 },
    quizDate: { year: 2027, month: 1, day: 5 },
  },
];

// 37-day official daily schedule (Nov 30, 2026 -> Jan 5, 2027)
// Covers Luke 1–24 (24), Ephesians 1–6 (6), Philippians 1–4 (4), Colossians 1–2 (2), Colossians 3–4 on final day 37
export interface OfficialDailyChapter {
  dayNumber: number; // 1..37
  weekNumber: number; // 1..5
  bookNameAr: string;
  chapterLabelAr: string;
  shortLabelAr: string; // e.g., "لوقا 1"
  fullLabelAr: string;  // e.g., "إنجيل لوقا — الإصحاح 1"
  isBeforeChallenge: boolean;
  isAfterChallenge: boolean;
}

const DAILY_CHAPTER_SCHEDULE: Array<{ bookNameAr: string; chapterLabelAr: string; shortBook: string }> = [
  // Week 1: Luke 1..7 (Nov 30 – Dec 6)
  ...Array.from({ length: 7 }, (_, i) => ({
    bookNameAr: 'إنجيل لوقا',
    chapterLabelAr: `الإصحاح ${i + 1}`,
    shortBook: `لوقا ${i + 1}`,
  })),
  // Week 2: Luke 8..14 (Dec 7 – Dec 13)
  ...Array.from({ length: 7 }, (_, i) => ({
    bookNameAr: 'إنجيل لوقا',
    chapterLabelAr: `الإصحاح ${i + 8}`,
    shortBook: `لوقا ${i + 8}`,
  })),
  // Week 3: Luke 15..21 (Dec 14 – Dec 20)
  ...Array.from({ length: 7 }, (_, i) => ({
    bookNameAr: 'إنجيل لوقا',
    chapterLabelAr: `الإصحاح ${i + 15}`,
    shortBook: `لوقا ${i + 15}`,
  })),
  // Week 4: Luke 22..24 + Ephesians 1..4 (Dec 21 – Dec 27)
  ...Array.from({ length: 3 }, (_, i) => ({
    bookNameAr: 'إنجيل لوقا',
    chapterLabelAr: `الإصحاح ${i + 22}`,
    shortBook: `لوقا ${i + 22}`,
  })),
  ...Array.from({ length: 4 }, (_, i) => ({
    bookNameAr: 'رسالة أفسس',
    chapterLabelAr: `الإصحاح ${i + 1}`,
    shortBook: `أفسس ${i + 1}`,
  })),
  // Week 5 (9 days, Dec 28 – Jan 5): Eph 5..6 (2) + Phil 1..4 (4) + Col 1..2 (2) + Col 3–4 (1)
  { bookNameAr: 'رسالة أفسس', chapterLabelAr: 'الإصحاح 5', shortBook: 'أفسس 5' },
  { bookNameAr: 'رسالة أفسس', chapterLabelAr: 'الإصحاح 6', shortBook: 'أفسس 6' },
  { bookNameAr: 'رسالة فيلبي', chapterLabelAr: 'الإصحاح 1', shortBook: 'فيلبي 1' },
  { bookNameAr: 'رسالة فيلبي', chapterLabelAr: 'الإصحاح 2', shortBook: 'فيلبي 2' },
  { bookNameAr: 'رسالة فيلبي', chapterLabelAr: 'الإصحاح 3', shortBook: 'فيلبي 3' },
  { bookNameAr: 'رسالة فيلبي', chapterLabelAr: 'الإصحاح 4', shortBook: 'فيلبي 4' },
  { bookNameAr: 'رسالة كولوسي', chapterLabelAr: 'الإصحاح 1', shortBook: 'كولوسي 1' },
  { bookNameAr: 'رسالة كولوسي', chapterLabelAr: 'الإصحاح 2', shortBook: 'كولوسي 2' },
  { bookNameAr: 'رسالة كولوسي', chapterLabelAr: 'الإصحاحان 3 و 4', shortBook: 'كولوسي 3 – 4' },
];

export function getOfficialDailyChapter(today: FastDate = todayFastDate()): OfficialDailyChapter {
  const isBefore = compareFastDate(today, FastCalendar.START_DATE) < 0;
  const isAfter = compareFastDate(today, FastCalendar.END_DATE) > 0;

  let dayNumber = 1;
  if (isBefore) {
    dayNumber = 1;
  } else if (isAfter) {
    dayNumber = DAILY_CHAPTER_SCHEDULE.length;
  } else {
    dayNumber = Math.min(
      Math.max(daysBetween(FastCalendar.START_DATE, today) + 1, 1),
      DAILY_CHAPTER_SCHEDULE.length
    );
  }

  const entry = DAILY_CHAPTER_SCHEDULE[dayNumber - 1] ?? DAILY_CHAPTER_SCHEDULE[0];
  const weekNumber = Math.min(Math.max(Math.floor((dayNumber - 1) / 7) + 1, 1), 5);

  return {
    dayNumber,
    weekNumber,
    bookNameAr: entry.bookNameAr,
    chapterLabelAr: entry.chapterLabelAr,
    shortLabelAr: entry.shortBook,
    fullLabelAr: `${entry.bookNameAr} — ${entry.chapterLabelAr}`,
    isBeforeChallenge: isBefore,
    isAfterChallenge: isAfter,
  };
}

export function getSyllabusForWeek(weekNumber: number): WeeklySyllabus {
  const safe = Math.min(Math.max(Math.round(weekNumber || 1), 1), 5);
  return WEEKLY_SYLLABUS[safe - 1] ?? WEEKLY_SYLLABUS[0];
}

export function isQuizHighlightDay(today: FastDate = todayFastDate()): boolean {
  // Highlight every Sunday (day === 0 in JS) and the Grand Finale on Tuesday Jan 5, 2027
  const jsDate = new Date(today.year, today.month - 1, today.day);
  const isSunday = jsDate.getDay() === 0;
  const isFinale = today.year === 2027 && today.month === 1 && today.day === 5;
  return isSunday || isFinale;
}

export function sanitizeQuizQuestions(raw: unknown): QuizQuestion[] {
  if (!Array.isArray(raw)) return [];
  const result: QuizQuestion[] = [];
  for (let i = 0; i < raw.length; i++) {
    const item = raw[i] as Record<string, unknown> | null;
    if (!item || typeof item !== 'object') continue;
    const prompt = String(item.prompt ?? '').trim();
    if (!prompt) continue;
    const type: QuizQuestionType = item.type === 'tf' ? 'tf' : 'mcq';
    const optionsRaw = Array.isArray(item.options)
      ? item.options.map((o) => String(o ?? '').trim()).filter(Boolean)
      : [];
    const options =
      type === 'tf'
        ? ['صح', 'خطأ']
        : optionsRaw.length >= 2
          ? optionsRaw.slice(0, 4)
          : ['الإجابة الأولى', 'الإجابة الثانية'];
    const rawCorrect = Number(item.correctIndex ?? 0);
    const correctIndex =
      Number.isFinite(rawCorrect) && rawCorrect >= 0 && rawCorrect < options.length
        ? Math.floor(rawCorrect)
        : 0;
    result.push({
      id: String(item.id ?? `q_${i + 1}`),
      prompt,
      type,
      options,
      correctIndex,
    });
  }
  return result;
}

export function parseQuizQuestionsJson(jsonStr: string | null | undefined): QuizQuestion[] {
  if (!jsonStr || !jsonStr.trim()) return [];
  try {
    return sanitizeQuizQuestions(JSON.parse(jsonStr));
  } catch {
    return [];
  }
}

// Zero-migration fallback codec: allows activeWeek + quiz questions to sync cleanly
// via existing Supabase `weekly_quiz_question` text column if SQL ALTER TABLE hasn't been run yet.
const META_START = '\n<!--TARIQ_META:';
const META_END = '-->';

export function stripQuizMetaFromQuestion(rawQuestion: string): string {
  const idx = rawQuestion.indexOf(META_START);
  return idx >= 0 ? rawQuestion.slice(0, idx).trim() : rawQuestion;
}

export function encodeQuestionWithMeta(
  cleanQuestion: string,
  activeWeek: number,
  questions: QuizQuestion[],
  enabledQuizWeeks?: number[]
): string {
  const base = stripQuizMetaFromQuestion(cleanQuestion).trim();
  const payload: Record<string, unknown> = {
    w: Math.min(Math.max(Math.round(activeWeek || 1), 1), 5),
    q: questions,
  };
  if (Array.isArray(enabledQuizWeeks)) {
    const cleanWeeks = Array.from(
      new Set(
        enabledQuizWeeks
          .map((n) => Math.round(Number(n)))
          .filter((n) => Number.isFinite(n) && n >= 1 && n <= 5)
      )
    ).sort((a, b) => a - b);
    payload.ew = cleanWeeks;
  }
  return `${base}${META_START}${JSON.stringify(payload)}${META_END}`;
}

export function extractQuizMetaFromQuestion(rawQuestion: string): {
  cleanQuestion: string;
  activeWeek: number | null;
  questions: QuizQuestion[] | null;
  enabledQuizWeeks: number[] | null;
} {
  const startIdx = rawQuestion.indexOf(META_START);
  if (startIdx < 0) {
    return { cleanQuestion: rawQuestion, activeWeek: null, questions: null, enabledQuizWeeks: null };
  }
  const endIdx = rawQuestion.indexOf(META_END, startIdx + META_START.length);
  const cleanQuestion = rawQuestion.slice(0, startIdx).trim();
  if (endIdx < 0) {
    return { cleanQuestion, activeWeek: null, questions: null, enabledQuizWeeks: null };
  }
  const jsonPart = rawQuestion.slice(startIdx + META_START.length, endIdx);
  try {
    const parsed = JSON.parse(jsonPart) as { w?: number; q?: unknown; ew?: unknown };
    const week =
      typeof parsed.w === 'number' && parsed.w >= 1 && parsed.w <= 5
        ? Math.round(parsed.w)
        : null;
    const questions = Array.isArray(parsed.q) ? sanitizeQuizQuestions(parsed.q) : null;
    const enabledQuizWeeks = Array.isArray(parsed.ew)
      ? Array.from(
          new Set(
            parsed.ew
              .map((n) => Math.round(Number(n)))
              .filter((n) => Number.isFinite(n) && n >= 1 && n <= 5)
          )
        ).sort((a, b) => a - b)
      : null;
    return { cleanQuestion, activeWeek: week, questions, enabledQuizWeeks };
  } catch {
    return { cleanQuestion, activeWeek: null, questions: null, enabledQuizWeeks: null };
  }
}
