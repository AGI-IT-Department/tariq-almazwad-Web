import React, { useMemo, useState } from 'react';
import {
  Award,
  BookMarked,
  BookOpen,
  Calendar,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  ClipboardCheck,
  ExternalLink,
  HelpCircle,
  Sparkles,
  X,
  XCircle,
} from 'lucide-react';
import type { AppConfig, ChallengeTask, FastDate, TaskSubmission } from '../types';
import {
  WEEKLY_SYLLABUS,
  decodeQuizConfig,
  getActiveSyllabusWeek,
  type WeeklyQuizQuestion,
  type WeeklySyllabusItem,
} from '../lib/weeklySyllabus';
import { UrlValidator, openSafeExternalUrl } from '../lib/securityAndExport';
import { StatusBadge } from './SharedUi';

/* ---------------- 1. Weekly Exam Banner Card ---------------- */

interface WeeklyExamBannerCardProps {
  today: FastDate;
  appConfig: AppConfig;
  examTask: ChallengeTask | null;
  examSubmission: TaskSubmission | null;
  isAfterEnd: boolean;
  onStartInAppQuiz: (questions: WeeklyQuizQuestion[], week: WeeklySyllabusItem) => void;
  onLogExamParticipation: (task: ChallengeTask, prefilledNote: string) => void;
}

export const WeeklyExamBannerCard: React.FC<WeeklyExamBannerCardProps> = ({
  today,
  appConfig,
  examTask,
  examSubmission,
  isAfterEnd,
  onStartInAppQuiz,
  onLogExamParticipation,
}) => {
  const { activeWeek, phase } = useMemo(() => getActiveSyllabusWeek(today), [today]);
  const decodedQuiz = useMemo(() => {
    const decoded = decodeQuizConfig(appConfig.weeklyQuizQuestion);
    const mergedQuestions =
      appConfig.weeklyQuizQuestions && appConfig.weeklyQuizQuestions.length > 0
        ? appConfig.weeklyQuizQuestions
        : decoded.questions;
    return {
      introText: decoded.introText,
      questions: mergedQuestions,
    };
  }, [appConfig.weeklyQuizQuestion, appConfig.weeklyQuizQuestions]);

  const hasInAppQuestions = decodedQuiz.questions.length > 0;
  const hasExternalLink = UrlValidator.isSafeHttpUrl(appConfig.weeklyQuizUrl);
  const status = examSubmission?.status ?? null;
  const isLocked = isAfterEnd || status === 'pending' || status === 'approved';
  const rewardPoints = examTask?.points ?? 200;

  const phaseLabel =
    phase === 'upcoming'
      ? 'القادم • يبدأ 30 نوفمبر'
      : phase === 'completed'
        ? 'الأسبوع الختامي'
        : 'الأسبوع الحالي';

  const defaultExamNote = `${activeWeek.titleAr} (${activeWeek.examDateAr}) — إنجيل + كتاب في تجسد الكلمة`;

  return (
    <section className="overflow-hidden rounded-[24px] border border-burgundy/40 bg-gradient-to-br from-burgundy/10 via-white to-status-pendingBg/50 p-4 shadow-sm">
      {/* Header Row */}
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="flex items-center gap-2.5">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-burgundy text-gold-bright shadow-sm">
            <HelpCircle className="h-5 w-5" />
          </div>
          <div>
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="rounded-full bg-burgundy px-2.5 py-0.5 text-[10px] font-bold text-white">
                {phaseLabel}
              </span>
              <span className="rounded-full bg-status-pendingBg px-2.5 py-0.5 text-[11px] font-extrabold text-gold-warm">
                +{rewardPoints} نقطة أسبوعياً
              </span>
            </div>
            <h3 className="mt-1 text-base font-extrabold text-burgundy">
              {appConfig.weeklyQuizTitle.trim() || 'المسابقة الأسبوعية (إنجيل + كتاب تجسد الكلمة)'}
            </h3>
          </div>
        </div>
        {status && <StatusBadge status={status} />}
      </div>

      {/* Current Week Exam Topic Box */}
      <div className="mt-3 space-y-1.5 rounded-2xl border border-burgundy/20 bg-white/90 p-3 text-xs">
        <div className="flex flex-wrap items-center justify-between gap-1">
          <span className="font-extrabold text-midnight">
            {activeWeek.titleAr} ({activeWeek.dateRangeAr})
          </span>
          <span className="inline-flex items-center gap-1 rounded-full bg-burgundy/10 px-2.5 py-0.5 text-[11px] font-bold text-burgundy">
            <Calendar className="h-3 w-3" />
            {activeWeek.examLabelAr}: {activeWeek.examDateAr}
          </span>
        </div>
        <p className="leading-relaxed text-ink-deep">
          <span className="font-bold text-midnight">الكتاب المقدس: </span>
          {activeWeek.bibleRangeAr}
        </p>
        <p className="leading-relaxed text-ink-deep">
          <span className="font-bold text-burgundy">كتاب «في تجسد الكلمة»: </span>
          {activeWeek.incarnationBookAr}
        </p>
      </div>

      {/* Admin Custom Intro / Announcement */}
      {decodedQuiz.introText && (
        <p className="mt-2.5 text-xs leading-relaxed text-ink-deep">{decodedQuiz.introText}</p>
      )}

      {/* Action Buttons */}
      <div className="mt-3 flex flex-wrap items-center gap-2">
        {hasInAppQuestions && (
          <button
            type="button"
            onClick={() => onStartInAppQuiz(decodedQuiz.questions, activeWeek)}
            className="inline-flex min-h-[44px] flex-1 items-center justify-center gap-1.5 rounded-xl bg-burgundy px-3.5 py-2 text-xs font-bold text-white shadow-sm transition hover:opacity-95 active:scale-[0.99]"
          >
            <ClipboardCheck className="h-4 w-4 shrink-0" />
            <span>
              {isLocked
                ? `مراجعة أسئلة المسابقة (${decodedQuiz.questions.length} أسئلة)`
                : `ابدأ المسابقة داخل التطبيق (${decodedQuiz.questions.length} أسئلة)`}
            </span>
          </button>
        )}

        {hasExternalLink && (
          <button
            type="button"
            onClick={() => openSafeExternalUrl(appConfig.weeklyQuizUrl)}
            className="inline-flex min-h-[44px] items-center justify-center gap-1.5 rounded-xl border border-burgundy bg-white px-3.5 py-2 text-xs font-bold text-burgundy transition hover:bg-burgundy hover:text-white"
          >
            <ExternalLink className="h-4 w-4 shrink-0" />
            <span>فتح رابط المسابقة</span>
          </button>
        )}

        {examTask && !isLocked && (
          <button
            type="button"
            onClick={() => onLogExamParticipation(examTask, defaultExamNote)}
            className={`inline-flex min-h-[44px] items-center justify-center gap-1.5 rounded-xl px-3.5 py-2 text-xs font-bold transition active:scale-[0.99] ${
              hasInAppQuestions
                ? 'border border-midnight/30 bg-white text-midnight hover:bg-parchment-variant'
                : 'flex-1 bg-midnight text-white shadow-sm hover:bg-midnight-dark'
            }`}
          >
            <Award className="h-4 w-4 shrink-0" />
            <span>تسجيل إتمام المسابقة (+{examTask.points} نقطة)</span>
          </button>
        )}
      </div>
    </section>
  );
};

/* ---------------- 2. Collapsible 5-Week Reading Plan & Syllabus Card ---------------- */

interface WeeklySyllabusCardProps {
  today: FastDate;
  incarnationTask: ChallengeTask | null;
  incarnationSubmission: TaskSubmission | null;
  isAfterEnd: boolean;
  onLogIncarnationReading: (task: ChallengeTask, prefilledNote: string) => void;
}

export const WeeklySyllabusCard: React.FC<WeeklySyllabusCardProps> = ({
  today,
  incarnationTask,
  incarnationSubmission,
  isAfterEnd,
  onLogIncarnationReading,
}) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const { activeWeek, phase } = useMemo(() => getActiveSyllabusWeek(today), [today]);

  const status = incarnationSubmission?.status ?? null;
  const isLocked = isAfterEnd || status === 'pending' || status === 'approved';

  const activeBadgeText =
    phase === 'upcoming'
      ? 'يبدأ 30 نوفمبر'
      : phase === 'completed'
        ? 'الأسبوع الختامي'
        : 'الأسبوع النشط الآن';

  return (
    <section className="overflow-hidden rounded-[22px] border border-gold-warm/50 bg-white shadow-sm">
      {/* Collapsible Header */}
      <button
        type="button"
        onClick={() => setIsExpanded((prev) => !prev)}
        className="flex w-full items-center justify-between gap-3 bg-gradient-to-l from-midnight/5 via-white to-status-pendingBg/40 p-4 text-right transition hover:bg-parchment/40"
      >
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-midnight text-gold-bright shadow-sm">
            <BookMarked className="h-5 w-5" />
          </div>
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-1.5">
              <h3 className="text-base font-extrabold text-midnight">منهج القراءة والمسابقة</h3>
              <span className="rounded-full bg-gold-warm/20 px-2 py-0.5 text-[10px] font-bold text-midnight">
                5 أسابيع
              </span>
            </div>
            <p className="mt-0.5 truncate text-xs text-ink-muted">
              {activeWeek.titleAr} ({activeWeek.dateRangeAr}) • 30 نوفمبر – 5 يناير
            </p>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-1 rounded-xl border border-parchment-outline bg-white px-2.5 py-1.5 text-xs font-bold text-midnight">
          <span>{isExpanded ? 'إخفاء' : 'عرض الجدول'}</span>
          {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
        </div>
      </button>

      {/* Active Week Quick Preview (always visible when collapsed) */}
      {!isExpanded && (
        <div className="border-t border-parchment-border px-4 pb-4 pt-3">
          <div className="space-y-2 rounded-2xl border border-gold-warm/45 bg-parchment/45 p-3">
            <div className="flex flex-wrap items-center justify-between gap-1.5">
              <span className="inline-flex items-center gap-1 rounded-full bg-midnight px-2.5 py-0.5 text-[11px] font-bold text-gold-bright">
                <Sparkles className="h-3 w-3" />
                {activeBadgeText} • {activeWeek.titleAr}
              </span>
              <span className="text-[11px] font-bold text-burgundy">
                {activeWeek.examLabelAr}: {activeWeek.examDateAr}
              </span>
            </div>
            <p className="text-xs leading-relaxed text-ink-deep">
              <span className="font-bold text-midnight">الكتاب المقدس: </span>
              {activeWeek.bibleRangeAr}
            </p>
            <p className="text-xs leading-relaxed text-ink-deep">
              <span className="font-bold text-burgundy">في تجسد الكلمة: </span>
              {activeWeek.incarnationBookAr}
            </p>
          </div>

          {incarnationTask && (
            <div className="mt-2.5 flex flex-wrap items-center justify-between gap-2">
              {status ? (
                <StatusBadge status={status} />
              ) : (
                <span className="text-[11px] font-semibold text-ink-muted">
                  قراءة كتاب «في تجسد الكلمة» الأسبوعية (+{incarnationTask.points} نقطة)
                </span>
              )}
              {!isLocked && (
                <button
                  type="button"
                  onClick={() =>
                    onLogIncarnationReading(
                      incarnationTask,
                      `${activeWeek.titleAr}: ${activeWeek.incarnationBookAr}`
                    )
                  }
                  className="min-h-[40px] rounded-xl bg-midnight px-3.5 py-1.5 text-xs font-bold text-white shadow-xs transition hover:bg-midnight-dark active:scale-95"
                >
                  تسجيل قراءة الكتاب (+{incarnationTask.points} نقطة)
                </button>
              )}
            </div>
          )}
        </div>
      )}

      {/* Expanded Complete 5-Week Schedule */}
      {isExpanded && (
        <div className="space-y-2.5 border-t border-parchment-border p-4">
          {WEEKLY_SYLLABUS.map((week) => {
            const isCurrent = week.weekNumber === activeWeek.weekNumber;
            return (
              <div
                key={week.weekNumber}
                className={`rounded-2xl border p-3.5 transition ${
                  isCurrent
                    ? 'border-gold-warm bg-gradient-to-l from-status-pendingBg/50 via-white to-midnight/5 shadow-xs'
                    : 'border-parchment-border bg-parchment/30'
                }`}
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex flex-wrap items-center gap-1.5">
                    <span
                      className={`rounded-full px-2.5 py-0.5 text-xs font-extrabold ${
                        isCurrent
                          ? 'bg-midnight text-gold-bright'
                          : week.isFinalWeek
                            ? 'bg-burgundy text-white'
                            : 'bg-parchment-variant text-midnight'
                      }`}
                    >
                      {week.titleAr}
                    </span>
                    <span className="text-xs font-bold text-ink-muted">({week.dateRangeAr})</span>
                  </div>

                  {isCurrent && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-gold-warm px-2.5 py-0.5 text-[10px] font-extrabold text-midnight-dark">
                      <Sparkles className="h-3 w-3" />
                      {activeBadgeText}
                    </span>
                  )}
                </div>

                <div className="mt-2.5 space-y-1.5 text-xs">
                  <div className="flex items-start gap-2">
                    <BookOpen className="mt-0.5 h-3.5 w-3.5 shrink-0 text-midnight" />
                    <p className="leading-relaxed text-ink-deep">
                      <span className="font-bold text-midnight">الكتاب المقدس: </span>
                      {week.bibleRangeAr}
                    </p>
                  </div>

                  <div className="flex items-start gap-2">
                    <BookMarked className="mt-0.5 h-3.5 w-3.5 shrink-0 text-burgundy" />
                    <p className="leading-relaxed text-ink-deep">
                      <span className="font-bold text-burgundy">كتاب في تجسد الكلمة: </span>
                      {week.incarnationBookAr}
                    </p>
                  </div>

                  <div className="flex items-start gap-2">
                    <Calendar className="mt-0.5 h-3.5 w-3.5 shrink-0 text-gold-warm" />
                    <p className="font-bold text-ink-deep">
                      <span className="text-gold-warm">{week.examLabelAr}: </span>
                      {week.examDateAr}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}

          {incarnationTask && !isLocked && (
            <button
              type="button"
              onClick={() =>
                onLogIncarnationReading(
                  incarnationTask,
                  `${activeWeek.titleAr}: ${activeWeek.incarnationBookAr}`
                )
              }
              className="mt-1 flex h-12 w-full items-center justify-center gap-2 rounded-2xl bg-midnight text-sm font-bold text-white shadow transition hover:bg-midnight-dark"
            >
              <BookMarked className="h-4 w-4 text-gold-bright" />
              <span>
                تسجيل قراءة كتاب «في تجسد الكلمة» للأسبوع الحالي (+{incarnationTask.points} نقطة)
              </span>
            </button>
          )}
        </div>
      )}
    </section>
  );
};

/* ---------------- 3. Interactive In-App Weekly Quiz Modal ---------------- */

interface InAppWeeklyQuizModalProps {
  title: string;
  week: WeeklySyllabusItem;
  questions: WeeklyQuizQuestion[];
  alreadySubmitted: boolean;
  onDismiss: () => void;
  onSubmitQuizResult: (noteSummary: string) => void;
}

export const InAppWeeklyQuizModal: React.FC<InAppWeeklyQuizModalProps> = ({
  title,
  week,
  questions,
  alreadySubmitted,
  onDismiss,
  onSubmitQuizResult,
}) => {
  const [answers, setAnswers] = useState<Record<string, number>>({});
  const [isGraded, setIsGraded] = useState(false);

  const answeredCount = Object.keys(answers).length;
  const allAnswered = answeredCount === questions.length && questions.length > 0;

  const correctCount = useMemo(() => {
    return questions.reduce((sum, q) => sum + (answers[q.id] === q.correctIndex ? 1 : 0), 0);
  }, [questions, answers]);

  const handleConfirmSubmission = () => {
    const percentage = questions.length > 0 ? Math.round((correctCount / questions.length) * 100) : 0;
    const summary = `تم حل ${title || 'المسابقة الأسبوعية'} (${week.titleAr}) داخل التطبيق — النتيجة: ${correctCount} من ${questions.length} (${percentage}%)`;
    onSubmitQuizResult(summary);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/55 backdrop-blur-[2px] sm:items-center sm:p-4"
      onClick={onDismiss}
    >
      <div
        className="flex max-h-[90vh] w-full max-w-lg flex-col overflow-hidden rounded-t-[28px] bg-white shadow-2xl animate-slide-up sm:rounded-[28px]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between gap-2 border-b border-parchment-border bg-midnight px-5 py-4 text-white">
          <div className="min-w-0">
            <p className="text-[11px] font-bold text-gold-bright">
              {week.titleAr} • {week.examDateAr}
            </p>
            <h3 className="truncate text-base font-extrabold">
              {title || 'المسابقة الأسبوعية (إنجيل + كتاب تجسد الكلمة)'}
            </h3>
          </div>
          <button
            type="button"
            onClick={onDismiss}
            aria-label="إغلاق"
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/15 text-white hover:bg-white/25"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Questions Body */}
        <div className="flex-1 space-y-4 overflow-y-auto p-5">
          {isGraded && (
            <div className="rounded-2xl border border-gold-warm bg-status-pendingBg p-4 text-center">
              <p className="text-xs font-bold text-midnight">نتيجتك في المسابقة الأسبوعية 🎉</p>
              <p className="mt-1 text-2xl font-extrabold text-burgundy">
                {correctCount} / {questions.length}
              </p>
              <p className="mt-1 text-xs text-ink-deep">
                {alreadySubmitted
                  ? 'تم تسجيل مشاركتك في المسابقة لهذا الأسبوع بالفعل.'
                  : 'اضغط على الزر بالأسفل لإرسال نتيجتك لخدام التحدي واعتماد +200 نقطة.'}
              </p>
            </div>
          )}

          {questions.map((q, qIdx) => {
            const picked = answers[q.id];
            return (
              <div
                key={q.id}
                className="space-y-2.5 rounded-2xl border border-parchment-border bg-parchment/30 p-4"
              >
                <div className="flex items-start gap-2">
                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-midnight text-xs font-bold text-gold-bright">
                    {qIdx + 1}
                  </span>
                  <p className="text-sm font-bold leading-relaxed text-ink-deep">{q.prompt}</p>
                </div>

                <div className="space-y-2">
                  {q.options.map((opt, optIdx) => {
                    const isPicked = picked === optIdx;
                    const isCorrectOpt = optIdx === q.correctIndex;

                    let btnStyle =
                      'border-parchment-outline bg-white text-ink-deep hover:border-midnight/50';
                    if (isGraded) {
                      if (isCorrectOpt) {
                        btnStyle =
                          'border-status-approved bg-status-approved/10 text-status-approved font-bold';
                      } else if (isPicked && !isCorrectOpt) {
                        btnStyle =
                          'border-burgundy bg-status-rejectedBg text-burgundy font-bold';
                      }
                    } else if (isPicked) {
                      btnStyle = 'border-midnight bg-midnight text-white font-bold shadow-xs';
                    }

                    return (
                      <button
                        key={optIdx}
                        type="button"
                        disabled={isGraded}
                        onClick={() => setAnswers((prev) => ({ ...prev, [q.id]: optIdx }))}
                        className={`flex min-h-[44px] w-full items-center justify-between gap-2 rounded-xl border px-3.5 py-2 text-right text-xs transition ${btnStyle}`}
                      >
                        <span>{opt}</span>
                        {isGraded && isCorrectOpt && (
                          <CheckCircle2 className="h-4 w-4 shrink-0 text-status-approved" />
                        )}
                        {isGraded && isPicked && !isCorrectOpt && (
                          <XCircle className="h-4 w-4 shrink-0 text-burgundy" />
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>

        {/* Footer Actions */}
        <div className="border-t border-parchment-border bg-white p-4">
          {!isGraded ? (
            <button
              type="button"
              disabled={!allAnswered}
              onClick={() => setIsGraded(true)}
              className="h-12 w-full rounded-2xl bg-burgundy text-sm font-bold text-white shadow transition hover:opacity-95 disabled:opacity-45"
            >
              {allAnswered
                ? 'تصحيح الإجابات وعرض النتيجة'
                : `أجب على جميع الأسئلة (${answeredCount}/${questions.length})`}
            </button>
          ) : alreadySubmitted ? (
            <button
              type="button"
              onClick={onDismiss}
              className="h-12 w-full rounded-2xl bg-midnight text-sm font-bold text-white"
            >
              إغلاق
            </button>
          ) : (
            <button
              type="button"
              onClick={handleConfirmSubmission}
              className="h-12 w-full rounded-2xl bg-midnight text-sm font-bold text-white shadow hover:bg-midnight-dark"
            >
              إرسال النتيجة للمراجعة (+200 نقطة) ✅
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
