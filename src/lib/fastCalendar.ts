import type { FastDate, TaskFrequency, TaskSubmission } from '../types';

const MONTHS_AR: Record<number, string> = {
  1: 'يناير',
  2: 'فبراير',
  3: 'مارس',
  4: 'أبريل',
  5: 'مايو',
  6: 'يونيو',
  7: 'يوليو',
  8: 'أغسطس',
  9: 'سبتمبر',
  10: 'أكتوبر',
  11: 'نوفمبر',
  12: 'ديسمبر',
};

export function todayFastDate(): FastDate {
  const now = new Date();
  return {
    year: now.getFullYear(),
    month: now.getMonth() + 1,
    day: now.getDate(),
  };
}

export function toIsoString(d: FastDate): string {
  const y = String(d.year).padStart(4, '0');
  const m = String(d.month).padStart(2, '0');
  const day = String(d.day).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function toShortArabicLabel(d: FastDate): string {
  return `${d.day} ${MONTHS_AR[d.month] ?? d.month}`;
}

export function compareFastDate(a: FastDate, b: FastDate): number {
  if (a.year !== b.year) return a.year - b.year;
  if (a.month !== b.month) return a.month - b.month;
  return a.day - b.day;
}

function toEpochDayMillis(d: FastDate): number {
  return Date.UTC(d.year, d.month - 1, d.day, 12, 0, 0, 0);
}

export function daysBetween(start: FastDate, end: FastDate): number {
  const diff = toEpochDayMillis(end) - toEpochDayMillis(start);
  return Math.round(diff / (1000 * 60 * 60 * 24));
}

export const FastCalendar = {
  START_DATE: { year: 2026, month: 11, day: 30 } as FastDate,
  END_DATE: { year: 2027, month: 1, day: 5 } as FastDate,
  FAST_DAYS_COUNT: 37,
  TOTAL_WEEKS: 5,
  TOP_PRIZE_RANKS: 10,

  currentFastDayIndex(today: FastDate = todayFastDate()): number {
    if (compareFastDate(today, this.START_DATE) < 0) return 0;
    if (compareFastDate(today, this.END_DATE) > 0) return this.FAST_DAYS_COUNT;
    const raw = daysBetween(this.START_DATE, today) + 1;
    return Math.min(Math.max(raw, 1), this.FAST_DAYS_COUNT);
  },

  isBeforeStart(today: FastDate = todayFastDate()): boolean {
    return compareFastDate(today, this.START_DATE) < 0;
  },

  isTrialMode(today: FastDate = todayFastDate()): boolean {
    return compareFastDate(today, this.START_DATE) < 0;
  },

  isAfterEnd(today: FastDate = todayFastDate()): boolean {
    return compareFastDate(today, this.END_DATE) > 0;
  },

  isWithinChallengeWindow(today: FastDate = todayFastDate()): boolean {
    return (
      compareFastDate(today, this.START_DATE) >= 0 &&
      compareFastDate(today, this.END_DATE) <= 0
    );
  },

  isTrialSubmission(submission: TaskSubmission): boolean {
    if (submission.periodKey.startsWith('TRIAL_')) return true;
    const iso = (submission.submissionDate || '').trim();
    if (iso.length === 10 && iso < toIsoString(this.START_DATE)) return true;
    return false;
  },

  isOfficialSubmission(submission: TaskSubmission): boolean {
    if (this.isTrialSubmission(submission)) return false;
    const iso = (submission.submissionDate || '').trim();
    if (iso.length === 10) {
      return iso >= toIsoString(this.START_DATE) && iso <= toIsoString(this.END_DATE);
    }
    return true;
  },

  daysUntilStart(today: FastDate = todayFastDate()): number {
    return Math.max(daysBetween(today, this.START_DATE), 0);
  },

  daysRemainingToFeast(today: FastDate = todayFastDate()): number {
    return Math.max(daysBetween(today, this.END_DATE), 0);
  },

  weekIndexForDate(date: FastDate): number {
    let dayIndex = 1;
    if (compareFastDate(date, this.START_DATE) < 0) {
      dayIndex = 1;
    } else if (compareFastDate(date, this.END_DATE) > 0) {
      dayIndex = this.FAST_DAYS_COUNT;
    } else {
      dayIndex = Math.min(Math.max(daysBetween(this.START_DATE, date) + 1, 1), this.FAST_DAYS_COUNT);
    }
    return Math.min(Math.max(Math.floor((dayIndex - 1) / 7) + 1, 1), this.TOTAL_WEEKS);
  },

  periodKeyForTask(frequency: TaskFrequency, date: FastDate = todayFastDate()): string {
    if (this.isTrialMode(date)) {
      if (frequency === 'daily') return `TRIAL_${toIsoString(date)}`;
      if (frequency === 'weekly') return `TRIAL_WEEK_${toIsoString(date)}`;
      return 'TRIAL_ONCE';
    }
    if (frequency === 'daily') return toIsoString(date);
    if (frequency === 'weekly') return `WEEK_${this.weekIndexForDate(date)}`;
    return 'FAST_2026';
  },

  periodLabelForTask(frequency: TaskFrequency, date: FastDate = todayFastDate()): string {
    let base = '';
    if (frequency === 'daily') {
      base = `يوم ${toShortArabicLabel(date)}`;
    } else if (frequency === 'weekly') {
      base = this.isTrialMode(date)
        ? 'أسبوع تجريبي'
        : `الأسبوع ${this.weekIndexForDate(date)} من الصوم`;
    } else {
      base = 'مرة خلال صوم الميلاد';
    }
    return this.isTrialMode(date) ? `${base} (تجريبي)` : base;
  },
};

export function formatTimestampArabic(epochMillis: number): string {
  const date = new Date(epochMillis);
  const dd = String(date.getDate()).padStart(2, '0');
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const yyyy = date.getFullYear();
  const hours = date.getHours();
  const mins = String(date.getMinutes()).padStart(2, '0');
  const period = hours < 12 ? 'ص' : 'م';
  const h12 = String(hours === 0 ? 12 : hours > 12 ? hours - 12 : hours).padStart(2, '0');
  return `${dd}/${mm}/${yyyy} • ${h12}:${mins} ${period}`;
}

export function formatTimeArabic(hour: number, minute: number): string {
  const period = hour < 12 ? 'ص' : 'م';
  const h12 = hour === 0 ? 12 : hour > 12 ? hour - 12 : hour;
  return `${String(h12).padStart(2, '0')}:${String(minute).padStart(2, '0')} ${period}`;
}
