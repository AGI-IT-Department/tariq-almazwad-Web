import React, { useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  BookMarked,
  BookOpen,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Loader2,
  Minus,
  Plus,
  RefreshCw,
  X,
} from 'lucide-react';
import {
  INCARNATION_BOOK_PDF_PATH,
  OFFICIAL_DAILY_BIBLE_PLAN,
  WEEKLY_SYLLABUS,
  fetchArabicBibleChapters,
  type BibleVerseItem,
  type OfficialBibleDayPlan,
  type WeeklySyllabusItem,
} from '../lib/weeklySyllabus';

/* ---------------- 1. In-App Book PDF Reader ("في تجسد الكلمة") ---------------- */

interface InAppBookPdfReaderModalProps {
  initialWeekNumber: number;
  activeWeekNumber: number;
  points: number;
  isLocked: boolean;
  statusLabel?: string;
  onDismiss: () => void;
  onLogWeekReading?: (week: WeeklySyllabusItem) => void;
}

export const InAppBookPdfReaderModal: React.FC<InAppBookPdfReaderModalProps> = ({
  initialWeekNumber,
  activeWeekNumber,
  points,
  isLocked,
  statusLabel,
  onDismiss,
  onLogWeekReading,
}) => {
  const [selectedWeekNum, setSelectedWeekNum] = useState<number>(
    Math.min(Math.max(initialWeekNumber || 1, 1), 5)
  );
  const [iframeReloadKey, setIframeReloadKey] = useState(0);

  const selectedWeek = useMemo(
    () => WEEKLY_SYLLABUS.find((w) => w.weekNumber === selectedWeekNum) ?? WEEKLY_SYLLABUS[0],
    [selectedWeekNum]
  );

  const pdfUrlWithPage = `${INCARNATION_BOOK_PDF_PATH}#page=${selectedWeek.pdfStartPage}&view=FitH`;

  const handleSelectWeek = (weekNum: number) => {
    setSelectedWeekNum(weekNum);
    setIframeReloadKey((k) => k + 1);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/65 p-0 sm:items-center sm:p-4"
      onClick={onDismiss}
    >
      <div
        className="flex h-[94vh] w-full max-w-[820px] flex-col overflow-hidden rounded-t-[28px] bg-parchment shadow-2xl sm:h-[90vh] sm:rounded-[28px]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between gap-2 border-b border-parchment-border bg-midnight px-4 py-3 text-white">
          <div className="flex min-w-0 items-center gap-2.5">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white/15 text-gold-bright">
              <BookMarked className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <h3 className="truncate text-sm font-extrabold sm:text-base">
                كتاب «في تجسد الكلمة» — للقديس أثناسيوس
              </h3>
              <p className="truncate text-[11px] text-gold-bright">
                مقسّم حسب خطة الأسابيع الخمسة لتحدي طريق المزود
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onDismiss}
            aria-label="إغلاق"
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/15 text-white transition hover:bg-white/25"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* 5-Week Selector Pills */}
        <div className="border-b border-parchment-border bg-white px-3 py-2.5">
          <div className="no-scrollbar flex gap-1.5 overflow-x-auto pb-0.5">
            {WEEKLY_SYLLABUS.map((week) => {
              const isSelected = week.weekNumber === selectedWeek.weekNumber;
              const isCurrent = week.weekNumber === activeWeekNumber;
              return (
                <button
                  key={week.weekNumber}
                  type="button"
                  onClick={() => handleSelectWeek(week.weekNumber)}
                  className={`shrink-0 rounded-full border px-3 py-1.5 text-xs font-bold transition ${
                    isSelected
                      ? 'border-burgundy bg-burgundy text-white shadow-sm'
                      : 'border-parchment-outline bg-parchment/60 text-ink-deep hover:bg-parchment-variant'
                  }`}
                >
                  <span>الأسبوع {week.weekNumber}</span>
                  {isCurrent && (
                    <span
                      className={`mr-1 rounded-full px-1.5 py-0.5 text-[9px] ${
                        isSelected ? 'bg-white/20 text-white' : 'bg-gold-warm/20 text-gold-warm'
                      }`}
                    >
                      الحالي
                    </span>
                  )}
                </button>
              );
            })}
          </div>

          {/* Assigned Reading Summary Strip */}
          <div className="mt-2 rounded-2xl border border-burgundy/25 bg-status-pendingBg/55 px-3 py-2 text-xs">
            <div className="flex flex-wrap items-center justify-between gap-1.5">
              <span className="font-extrabold text-burgundy">
                {selectedWeek.titleAr} ({selectedWeek.dateRangeAr})
              </span>
              <span className="rounded-full bg-burgundy px-2.5 py-0.5 text-[11px] font-bold text-white">
                من صفحة {selectedWeek.pdfStartPage} إلى صفحة {selectedWeek.pdfEndPage} في الملف
              </span>
            </div>
            <p className="mt-1 leading-relaxed text-ink-deep">{selectedWeek.incarnationBookAr}</p>
          </div>
        </div>

        {/* Embedded PDF Viewer */}
        <div className="relative flex-1 bg-neutral-100">
          <iframe
            key={`${selectedWeek.weekNumber}-${iframeReloadKey}`}
            src={pdfUrlWithPage}
            title={`كتاب في تجسد الكلمة - ${selectedWeek.titleAr}`}
            className="h-full w-full border-0"
          />
        </div>

        {/* Footer Actions */}
        <div className="flex flex-wrap items-center justify-between gap-2 border-t border-parchment-border bg-white px-4 py-3">
          <a
            href={pdfUrlWithPage}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex h-11 items-center gap-1.5 rounded-xl border border-midnight/25 bg-parchment/50 px-3.5 text-xs font-bold text-midnight transition hover:bg-parchment-variant"
          >
            <ExternalLink className="h-4 w-4" />
            <span>فتح / تحميل PDF كاملاً</span>
          </a>

          {onLogWeekReading && (
            <button
              type="button"
              disabled={isLocked}
              onClick={() => onLogWeekReading(selectedWeek)}
              className={`inline-flex h-11 flex-1 items-center justify-center gap-1.5 rounded-xl px-4 text-xs font-bold transition sm:flex-none ${
                isLocked
                  ? 'bg-parchment-variant text-ink-muted'
                  : 'bg-midnight text-white shadow-sm hover:bg-midnight-dark active:scale-[0.98]'
              }`}
            >
              <CheckCircle2 className="h-4 w-4" />
              <span>
                {isLocked
                  ? statusLabel || 'تم تسجيل قراءة هذا الأسبوع'
                  : `سجّل قراءة ورد الأسبوع (+${points} نقطة)`}
              </span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

/* ---------------- 2. In-App Daily Bible Chapter Reader ---------------- */

interface InAppBibleChapterReaderModalProps {
  initialDayNumber: number;
  todayDayNumber: number;
  points: number;
  isLocked: boolean;
  statusLabel?: string;
  onDismiss: () => void;
  onLogBibleReading?: (dayPlan: OfficialBibleDayPlan) => void;
}

export const InAppBibleChapterReaderModal: React.FC<InAppBibleChapterReaderModalProps> = ({
  initialDayNumber,
  todayDayNumber,
  points,
  isLocked,
  statusLabel,
  onDismiss,
  onLogBibleReading,
}) => {
  const [selectedDayNum, setSelectedDayNum] = useState<number>(
    Math.min(Math.max(initialDayNumber || 1, 1), OFFICIAL_DAILY_BIBLE_PLAN.length)
  );
  const [fontSizePx, setFontSizePx] = useState<number>(19);
  const [verses, setVerses] = useState<BibleVerseItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState<number>(0);

  const selectedDay = useMemo(
    () =>
      OFFICIAL_DAILY_BIBLE_PLAN.find((d) => d.dayNumber === selectedDayNum) ??
      OFFICIAL_DAILY_BIBLE_PLAN[0],
    [selectedDayNum]
  );

  const selectedWeekDays = useMemo(
    () => OFFICIAL_DAILY_BIBLE_PLAN.filter((d) => d.weekNumber === selectedDay.weekNumber),
    [selectedDay.weekNumber]
  );

  const selectedWeekSyllabus = useMemo(
    () =>
      WEEKLY_SYLLABUS.find((w) => w.weekNumber === selectedDay.weekNumber) ?? WEEKLY_SYLLABUS[0],
    [selectedDay.weekNumber]
  );

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setErrorMsg(null);
    fetchArabicBibleChapters(selectedDay.bookId, selectedDay.chapters)
      .then((loaded) => {
        if (!cancelled) {
          setVerses(loaded);
          setIsLoading(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setErrorMsg('تعذر تحميل نص الإصحاح حالياً. تأكد من اتصالك بالإنترنت ثم اضغط إعادة المحاولة.');
          setIsLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [selectedDay.bookId, selectedDay.chapters, reloadTick]);

  const handleSelectWeek = (weekNum: number) => {
    const firstInWeek = OFFICIAL_DAILY_BIBLE_PLAN.find((d) => d.weekNumber === weekNum);
    if (firstInWeek) setSelectedDayNum(firstInWeek.dayNumber);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/65 p-0 sm:items-center sm:p-4"
      onClick={onDismiss}
    >
      <div
        className="flex h-[94vh] w-full max-w-[760px] flex-col overflow-hidden rounded-t-[28px] bg-parchment shadow-2xl sm:h-[90vh] sm:rounded-[28px]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between gap-2 border-b border-parchment-border bg-midnight px-4 py-3 text-white">
          <div className="flex min-w-0 items-center gap-2.5">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white/15 text-gold-bright">
              <BookOpen className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <h3 className="truncate text-sm font-extrabold sm:text-base">
                {selectedDay.fullLabelAr}
              </h3>
              <p className="truncate text-[11px] text-gold-bright">
                اليوم {selectedDay.dayNumber} ({selectedDay.dateLabelAr}) • {selectedWeekSyllabus.titleAr}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={() => setFontSizePx((s) => Math.max(15, s - 2))}
              aria-label="تصغير الخط"
              className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/15 text-white hover:bg-white/25"
            >
              <Minus className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={() => setFontSizePx((s) => Math.min(27, s + 2))}
              aria-label="تكبير الخط"
              className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/15 text-white hover:bg-white/25"
            >
              <Plus className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={onDismiss}
              aria-label="إغلاق"
              className="mr-1 flex h-9 w-9 items-center justify-center rounded-full bg-white/15 text-white transition hover:bg-white/25"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Week + Day Plan Navigation */}
        <div className="space-y-2 border-b border-parchment-border bg-white px-3 py-2.5">
          {/* 5 Week Tabs */}
          <div className="no-scrollbar flex gap-1.5 overflow-x-auto">
            {WEEKLY_SYLLABUS.map((w) => {
              const active = w.weekNumber === selectedDay.weekNumber;
              return (
                <button
                  key={w.weekNumber}
                  type="button"
                  onClick={() => handleSelectWeek(w.weekNumber)}
                  className={`shrink-0 rounded-full border px-3 py-1 text-[11px] font-bold transition ${
                    active
                      ? 'border-midnight bg-midnight text-white'
                      : 'border-parchment-outline bg-parchment/50 text-ink-deep hover:bg-parchment-variant'
                  }`}
                >
                  الأسبوع {w.weekNumber}
                </button>
              );
            })}
          </div>

          {/* Days inside selected week */}
          <div className="no-scrollbar flex gap-1.5 overflow-x-auto pb-0.5">
            {selectedWeekDays.map((day) => {
              const isSelected = day.dayNumber === selectedDay.dayNumber;
              const isToday = day.dayNumber === todayDayNumber;
              return (
                <button
                  key={day.dayNumber}
                  type="button"
                  onClick={() => setSelectedDayNum(day.dayNumber)}
                  className={`shrink-0 rounded-xl border px-2.5 py-1.5 text-xs font-bold transition ${
                    isSelected
                      ? 'border-gold-warm bg-status-pendingBg text-midnight-dark shadow-xs'
                      : 'border-parchment-border bg-white text-ink-muted hover:bg-parchment/60'
                  }`}
                >
                  <span>{day.shortLabelAr}</span>
                  <span className="mr-1 text-[10px] font-normal opacity-80">
                    ({isToday ? 'اليوم' : day.dateLabelAr})
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Bible Text Scroll Area */}
        <div className="flex-1 overflow-y-auto px-4 py-4 sm:px-6">
          {isLoading ? (
            <div className="flex h-full min-h-[240px] flex-col items-center justify-center gap-3 text-ink-muted">
              <Loader2 className="h-8 w-8 animate-spin text-midnight" />
              <p className="text-sm font-semibold">جارٍ تحميل {selectedDay.fullLabelAr}...</p>
            </div>
          ) : errorMsg ? (
            <div className="flex h-full min-h-[240px] flex-col items-center justify-center gap-3 text-center">
              <AlertCircle className="h-10 w-10 text-burgundy" />
              <p className="max-w-sm text-sm font-medium text-ink-deep">{errorMsg}</p>
              <button
                type="button"
                onClick={() => setReloadTick((t) => t + 1)}
                className="inline-flex items-center gap-1.5 rounded-xl bg-midnight px-4 py-2.5 text-xs font-bold text-white"
              >
                <RefreshCw className="h-4 w-4" />
                <span>إعادة المحاولة</span>
              </button>
            </div>
          ) : (
            <div className="mx-auto max-w-[640px] space-y-3 rounded-[24px] border border-parchment-border bg-white p-5 shadow-xs">
              <div className="border-b border-parchment-border pb-3 text-center">
                <p className="text-xs font-bold text-gold-warm">الكتاب المقدس — ترجمة فانديك</p>
                <h4 className="mt-0.5 text-lg font-extrabold text-midnight">
                  {selectedDay.fullLabelAr}
                </h4>
              </div>

              <div
                className="space-y-3 leading-[2.05] text-ink-deep"
                style={{ fontSize: `${fontSizePx}px` }}
              >
                {verses.map((v, idx) => {
                  const showChapterHeading =
                    selectedDay.chapters.length > 1 &&
                    (idx === 0 || verses[idx - 1].chapter !== v.chapter);
                  return (
                    <React.Fragment key={`${v.chapter}-${v.verse}`}>
                      {showChapterHeading && (
                        <div className="pt-2 pb-1 text-center">
                          <span className="inline-block rounded-full bg-midnight/10 px-4 py-1 text-xs font-extrabold text-midnight">
                            {selectedDay.bookNameAr} — الإصحاح {v.chapter}
                          </span>
                        </div>
                      )}
                      <p className="text-right">
                        <span className="ml-2 inline-flex h-6 min-w-[24px] items-center justify-center rounded-full bg-status-pendingBg px-1.5 align-middle text-xs font-extrabold text-gold-warm">
                          {v.verse}
                        </span>
                        <span>{v.text}</span>
                      </p>
                    </React.Fragment>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* Sticky Footer Controls */}
        <div className="flex flex-wrap items-center justify-between gap-2 border-t border-parchment-border bg-white px-4 py-3">
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              disabled={selectedDayNum <= 1}
              onClick={() => setSelectedDayNum((n) => Math.max(1, n - 1))}
              className="inline-flex h-10 items-center gap-1 rounded-xl border border-parchment-outline px-3 text-xs font-bold text-ink-deep transition hover:bg-parchment-variant disabled:opacity-40"
            >
              <ChevronRight className="h-4 w-4" />
              <span>السابق</span>
            </button>
            <button
              type="button"
              disabled={selectedDayNum >= OFFICIAL_DAILY_BIBLE_PLAN.length}
              onClick={() =>
                setSelectedDayNum((n) => Math.min(OFFICIAL_DAILY_BIBLE_PLAN.length, n + 1))
              }
              className="inline-flex h-10 items-center gap-1 rounded-xl border border-parchment-outline px-3 text-xs font-bold text-ink-deep transition hover:bg-parchment-variant disabled:opacity-40"
            >
              <span>التالي</span>
              <ChevronLeft className="h-4 w-4" />
            </button>
          </div>

          {onLogBibleReading && (
            <button
              type="button"
              disabled={isLocked}
              onClick={() => onLogBibleReading(selectedDay)}
              className={`inline-flex h-11 flex-1 items-center justify-center gap-1.5 rounded-xl px-4 text-xs font-bold transition sm:flex-none ${
                isLocked
                  ? 'bg-parchment-variant text-ink-muted'
                  : 'bg-midnight text-white shadow-sm hover:bg-midnight-dark active:scale-[0.98]'
              }`}
            >
              <CheckCircle2 className="h-4 w-4" />
              <span>
                {isLocked
                  ? statusLabel || 'تم تسجيل قراءة اليوم'
                  : `سجّل قراءة الإصحاح (+${points} نقطة)`}
              </span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
