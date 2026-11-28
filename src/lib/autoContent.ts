import dailyDevotionsRaw from '../data/daily_devotions.json';
import quizWeek1Raw from '../data/quiz_week_1.json';
import quizWeek2Raw from '../data/quiz_week_2.json';
import quizWeek3Raw from '../data/quiz_week_3.json';
import quizWeek4Raw from '../data/quiz_week_4.json';
import quizWeek5Raw from '../data/quiz_week_5.json';
import type { AppConfig, FastDate, TaskSubmission } from '../types';
import { compareFastDate, daysBetween, FastCalendar, todayFastDate } from './fastCalendar';

export interface DailyDevotion {
  dayNumber: number;
  chapterLabel: string;
  verseText: string;
  verseRef: string;
  fatherQuote: string;
  fatherName: string;
}

export interface AutoMcqQuestion {
  id: string;
  q: string;
  o: string[];
  a: number;
}

export interface AutoTfQuestion {
  id: string;
  q: string;
  a: boolean;
}

export interface AutoMatchPair {
  l: string;
  r: string;
}

export interface AutoMatchGroup {
  id: string;
  title: string;
  pairs: AutoMatchPair[];
}

export interface AutoWeeklyQuiz {
  weekNumber: number;
  titleAr: string;
  subtitleAr: string;
  pointsPerItem: number;
  totalItems: number;
  maxPoints: number;
  mcq: AutoMcqQuestion[];
  tf: AutoTfQuestion[];
  match: AutoMatchGroup[];
}

export interface AutoQuizDraft {
  mcqAnswers: Record<string, number>;
  tfAnswers: Record<string, boolean>;
  /** Keyed as `${groupId}:${leftIdx}` -> matched rightIdx (index into original group.pairs) */
  matchAnswers: Record<string, number>;
  updatedAt: number;
}

export interface AutoQuizGradeResult {
  mcqCorrect: number;
  mcqTotal: number;
  tfCorrect: number;
  tfTotal: number;
  matchCorrect: number;
  matchTotal: number;
  correctCount: number;
  totalItems: number;
  answeredCount: number;
  earnedPoints: number;
  maxPoints: number;
  percent: number;
}

export const DEFAULT_ENABLED_QUIZ_WEEKS: number[] = [1];

const DAILY_DEVOTIONS: DailyDevotion[] = dailyDevotionsRaw as DailyDevotion[];

const AUTO_WEEKLY_QUIZZES: AutoWeeklyQuiz[] = [
  quizWeek1Raw as AutoWeeklyQuiz,
  quizWeek2Raw as AutoWeeklyQuiz,
  quizWeek3Raw as AutoWeeklyQuiz,
  quizWeek4Raw as AutoWeeklyQuiz,
  quizWeek5Raw as AutoWeeklyQuiz,
];

function utcEpochDay(date: FastDate): number {
  return Math.floor(Date.UTC(date.year, date.month - 1, date.day) / 86_400_000);
}

/**
 * Returns today's automatic Bible verse (from the official daily syllabus) +
 * Coptic Orthodox Church Father quote aligned with Nativity Fast & the Incarnation.
 * Works immediately every day: exact syllabus day 1..37 during Nov 30 – Jan 5,
 * and deterministic daily rotation before/after so today's verse refreshes daily right now.
 */
export function getTodayAutoDevotion(today: FastDate = todayFastDate()): DailyDevotion {
  const count = DAILY_DEVOTIONS.length;
  if (count === 0) {
    return {
      dayNumber: 1,
      chapterLabel: 'إنجيل لوقا — الإصحاح 2',
      verseText: '«الْمَجْدُ للهِ فِي الأَعَالِي، وَعَلَى الأَرْضِ السَّلاَمُ، وَبِالنَّاسِ الْمَسَرَّةُ»',
      verseRef: 'إنجيل لوقا 2: 14',
      fatherQuote: 'أخلى الكلمة ذاته وُلد في مزود حقير ليعلمنا أن التواضع هو الطريق الملكي لمعاينة مجد الله.',
      fatherName: 'القديس كيرلس عمود الدين',
    };
  }

  const isBefore = compareFastDate(today, FastCalendar.START_DATE) < 0;
  const isAfter = compareFastDate(today, FastCalendar.END_DATE) > 0;

  if (!isBefore && !isAfter) {
    const dayIdx = Math.min(Math.max(daysBetween(FastCalendar.START_DATE, today), 0), count - 1);
    return DAILY_DEVOTIONS[dayIdx];
  }

  const epoch = utcEpochDay(today);
  const rotatedIdx = ((epoch % count) + count) % count;
  return DAILY_DEVOTIONS[rotatedIdx];
}

export function getAllAutoWeeklyQuizzes(): AutoWeeklyQuiz[] {
  return AUTO_WEEKLY_QUIZZES;
}

export function getAutoWeeklyQuiz(weekNumber: number): AutoWeeklyQuiz {
  const safe = Math.min(Math.max(Math.round(weekNumber || 1), 1), AUTO_WEEKLY_QUIZZES.length);
  return AUTO_WEEKLY_QUIZZES[safe - 1] ?? AUTO_WEEKLY_QUIZZES[0];
}

export function sanitizeEnabledQuizWeeks(raw: unknown): number[] {
  if (!Array.isArray(raw)) return [...DEFAULT_ENABLED_QUIZ_WEEKS];
  const unique = new Set<number>();
  for (const item of raw) {
    const num = Number(item);
    if (Number.isFinite(num) && num >= 1 && num <= 5) {
      unique.add(Math.round(num));
    }
  }
  return Array.from(unique).sort((a, b) => a - b);
}

export function getEnabledQuizWeeks(config: AppConfig | null | undefined): number[] {
  if (!config || !Array.isArray(config.enabledQuizWeeks)) {
    return [...DEFAULT_ENABLED_QUIZ_WEEKS];
  }
  return sanitizeEnabledQuizWeeks(config.enabledQuizWeeks);
}

export function withToggledQuizWeek(config: AppConfig, weekNumber: number, enabled: boolean): AppConfig {
  const current = new Set(getEnabledQuizWeeks(config));
  const safeWeek = Math.min(Math.max(Math.round(weekNumber), 1), 5);
  if (enabled) current.add(safeWeek);
  else current.delete(safeWeek);
  const nextWeeks = Array.from(current).sort((a, b) => a - b);
  return {
    ...config,
    activeWeek: enabled ? safeWeek : (nextWeeks[nextWeeks.length - 1] ?? config.activeWeek),
    enabledQuizWeeks: nextWeeks,
  };
}

export function autoQuizPeriodKey(weekNumber: number, today: FastDate = todayFastDate()): string {
  const safe = Math.min(Math.max(Math.round(weekNumber), 1), 5);
  return FastCalendar.isTrialMode(today) ? `TRIAL_QUIZ_WEEK_${safe}` : `QUIZ_WEEK_${safe}`;
}

export function findUserAutoQuizSubmission(
  submissions: TaskSubmission[],
  userId: string,
  weekNumber: number,
  today: FastDate = todayFastDate()
): TaskSubmission | undefined {
  const key = autoQuizPeriodKey(weekNumber, today);
  return submissions.find(
    (s) =>
      s.userId === userId &&
      s.periodKey === key &&
      s.status === 'approved'
  );
}

/**
 * Deterministic shuffle of indices 0..length-1 seeded by group id,
 * so the right column of matching questions is scrambled consistently across renders.
 */
export function getShuffledRightIndices(groupId: string, length: number): number[] {
  const indices = Array.from({ length }, (_, i) => i);
  let seed = 2166136261;
  for (let i = 0; i < groupId.length; i++) {
    seed ^= groupId.charCodeAt(i);
    seed = Math.imul(seed, 16777619);
  }
  const nextRand = () => {
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    return seed / 4294967296;
  };
  for (let i = indices.length - 1; i > 0; i--) {
    const j = Math.floor(nextRand() * (i + 1));
    [indices[i], indices[j]] = [indices[j], indices[i]];
  }
  // Ensure it's not identical to identity order when length >= 2
  if (length >= 2 && indices.every((v, idx) => v === idx)) {
    [indices[0], indices[1]] = [indices[1], indices[0]];
  }
  return indices;
}

export function gradeAutoQuiz(quiz: AutoWeeklyQuiz, draft: AutoQuizDraft): AutoQuizGradeResult {
  let mcqCorrect = 0;
  let answeredCount = 0;
  for (const item of quiz.mcq) {
    const picked = draft.mcqAnswers[item.id];
    if (typeof picked === 'number') {
      answeredCount++;
      if (picked === item.a) mcqCorrect++;
    }
  }

  let tfCorrect = 0;
  for (const item of quiz.tf) {
    const picked = draft.tfAnswers[item.id];
    if (typeof picked === 'boolean') {
      answeredCount++;
      if (picked === item.a) tfCorrect++;
    }
  }

  let matchCorrect = 0;
  let matchTotal = 0;
  for (const group of quiz.match) {
    group.pairs.forEach((_, leftIdx) => {
      matchTotal++;
      const key = `${group.id}:${leftIdx}`;
      const pickedRightIdx = draft.matchAnswers[key];
      if (typeof pickedRightIdx === 'number') {
        answeredCount++;
        if (pickedRightIdx === leftIdx) matchCorrect++;
      }
    });
  }

  const totalItems = quiz.mcq.length + quiz.tf.length + matchTotal;
  const correctCount = mcqCorrect + tfCorrect + matchCorrect;
  const earnedPoints = correctCount * quiz.pointsPerItem;
  const maxPoints = totalItems * quiz.pointsPerItem;
  const percent = totalItems > 0 ? Math.round((correctCount / totalItems) * 100) : 0;

  return {
    mcqCorrect,
    mcqTotal: quiz.mcq.length,
    tfCorrect,
    tfTotal: quiz.tf.length,
    matchCorrect,
    matchTotal,
    correctCount,
    totalItems,
    answeredCount,
    earnedPoints,
    maxPoints,
    percent,
  };
}

export function emptyAutoQuizDraft(): AutoQuizDraft {
  return {
    mcqAnswers: {},
    tfAnswers: {},
    matchAnswers: {},
    updatedAt: Date.now(),
  };
}

function draftStorageKey(userId: string, weekNumber: number): string {
  return `tariq_auto_quiz_draft_v1_${userId}_w${weekNumber}`;
}

export function loadAutoQuizDraft(userId: string, weekNumber: number): AutoQuizDraft {
  try {
    const raw = localStorage.getItem(draftStorageKey(userId, weekNumber));
    if (!raw) return emptyAutoQuizDraft();
    const parsed = JSON.parse(raw) as Partial<AutoQuizDraft>;
    return {
      mcqAnswers: parsed.mcqAnswers && typeof parsed.mcqAnswers === 'object' ? parsed.mcqAnswers : {},
      tfAnswers: parsed.tfAnswers && typeof parsed.tfAnswers === 'object' ? parsed.tfAnswers : {},
      matchAnswers:
        parsed.matchAnswers && typeof parsed.matchAnswers === 'object' ? parsed.matchAnswers : {},
      updatedAt: typeof parsed.updatedAt === 'number' ? parsed.updatedAt : Date.now(),
    };
  } catch {
    return emptyAutoQuizDraft();
  }
}

export function saveAutoQuizDraft(userId: string, weekNumber: number, draft: AutoQuizDraft): void {
  try {
    localStorage.setItem(
      draftStorageKey(userId, weekNumber),
      JSON.stringify({ ...draft, updatedAt: Date.now() })
    );
  } catch {
    // ignore storage quota errors
  }
}

export function clearAutoQuizDraft(userId: string, weekNumber: number): void {
  try {
    localStorage.removeItem(draftStorageKey(userId, weekNumber));
  } catch {
    // ignore
  }
}
