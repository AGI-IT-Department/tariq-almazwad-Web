import React, { useMemo, useState } from 'react';
import {
  BookMarked,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Eye,
  HelpCircle,
  Lock,
  PlayCircle,
  Sparkles,
  Trophy,
} from 'lucide-react';
import type { AppConfig, ChallengeTask, TaskSubmission } from '../types';
import { FastCalendar, todayFastDate } from '../lib/fastCalendar';
import {
  WEEKLY_SYLLABUS,
  getOfficialDailyChapter,
  getSyllabusForWeek,
  isQuizHighlightDay,
} from '../lib/readingPlan';
import {
  findUserAutoQuizSubmission,
  getAutoWeeklyQuiz,
  getEnabledQuizWeeks,
} from '../lib/autoContent';
import { StatusBadge, TaskSubmissionSheet } from '../components/SharedUi';
import { AutoWeeklyQuizModal } from '../components/AutoWeeklyQuizModal';

interface ReadingPlanScreenProps {
  userId: string;
  allTasks: ChallengeTask[];
  userSubmissions: TaskSubmission[];
  appConfig: AppConfig;
  onSubmitTask: (task: ChallengeTask, note: string) => void;
  onSubmitAutoQuiz: (
    weekNumber: number,
    correctCount: number,
    totalItems: number,
    earnedPoints: number,
    preferredTaskId?: string
  ) => Promise<boolean>;
}

export const ReadingPlanScreen: React.FC<ReadingPlanScreenProps> = ({
  userId,
  allTasks,
  userSubmissions,
  appConfig,
  onSubmitTask,
  onSubmitAutoQuiz,
}) => {
  const today = useMemo(() => todayFastDate(), []);
  const isAfterEnd = FastCalendar.isAfterEnd(today);
  const isQuizToday = useMemo(() => isQuizHighlightDay(today), [today]);
  const officialChapter = useMemo(() => getOfficialDailyChapter(today), [today]);
  const activeWeekNum = Math.min(Math.max(appConfig.activeWeek || 1, 1), 5);
  const activeSyllabus = useMemo(() => getSyllabusForWeek(activeWeekNum), [activeWeekNum]);
  const enabledQuizWeeks = useMemo(() => getEnabledQuizWeeks(appConfig), [appConfig]);

  const [expandedWeek, setExpandedWeek] = useState<number>(activeWeekNum);
  const [selectedTaskForSheet, setSelectedTaskForSheet] = useState<ChallengeTask | null>(null);
  const [sheetInitialNote, setSheetInitialNote] = useState('');
  const [activeAutoQuiz, setActiveAutoQuiz] = useState<{
    weekNumber: number;
    completedPoints: number | null;
  } | null>(null);

  const incarnationTask = useMemo(
    () => allTasks.find((t) => t.id === 'task_incarnation_book' && t.isActive) ?? null,
    [allTasks]
  );

  const weeklyPeriodKey = FastCalendar.periodKeyForTask('weekly', today);
  const incarnationSub = useMemo(
    () =>
      incarnationTask
        ? userSubmissions.find(
            (s) => s.taskId === incarnationTask.id && s.periodKey === weeklyPeriodKey
          ) ?? null
        : null,
    [incarnationTask, userSubmissions, weeklyPeriodKey]
  );

  const isIncarnationLocked =
    isAfterEnd || incarnationSub?.status === 'pending' || incarnationSub?.status === 'approved';

  const activeAutoQuizData = useMemo(() => getAutoWeeklyQuiz(activeWeekNum), [activeWeekNum]);
  const isActiveWeekQuizEnabled = enabledQuizWeeks.includes(activeWeekNum);
  const activeWeekQuizSub = useMemo(
    () => findUserAutoQuizSubmission(userSubmissions, userId, activeWeekNum, today),
    [userSubmissions, userId, activeWeekNum, today]
  );

  return (
    <div className="mx-auto max-w-[640px] space-y-4 px-4 pt-3.5 pb-40">
      {selectedTaskForSheet && (
        <TaskSubmissionSheet
          task={selectedTaskForSheet}
          initialNote={sheetInitialNote}
          onDismiss={() => {
            setSelectedTaskForSheet(null);
            setSheetInitialNote('');
          }}
          onConfirmSubmit={(note) => {
            onSubmitTask(selectedTaskForSheet, note);
            setSelectedTaskForSheet(null);
            setSheetInitialNote('');
          }}
        />
      )}

      {activeAutoQuiz && (
        <AutoWeeklyQuizModal
          quiz={getAutoWeeklyQuiz(activeAutoQuiz.weekNumber)}
          userId={userId}
          completedPoints={activeAutoQuiz.completedPoints}
          onSubmitResult={(weekNumber, correctCount, totalItems, earnedPoints) =>
            onSubmitAutoQuiz(
              weekNumber,
              correctCount,
              totalItems,
              earnedPoints,
              'task_weekly_exam_incarnation'
            )
          }
          onClose={() => setActiveAutoQuiz(null)}
        />
      )}

      {/* Header Timeline Card */}
      <section className="rounded-[24px] bg-gradient-to-br from-midnight-dark via-midnight to-midnight-dark p-5 text-white shadow-lg">
        <div className="flex items-center gap-2 text-gold-bright">
          <BookMarked className="h-5 w-5 shrink-0" />
          <span className="text-xs font-bold">خطة القراءة والمسابقة الأسبوعية</span>
        </div>
        <h2 className="font-amiri mt-1.5 text-2xl font-bold text-white">
          الكتاب المقدس وكتاب «في تجسد الكلمة»
        </h2>
        <p className="mt-1 text-xs leading-relaxed text-white/85">
          للقديس أثناسيوس الرسولي • الإثنين 30 نوفمبر 2026 حتى الثلاثاء 5 يناير 2027 (37 يوماً / 5 أسابيع)
        </p>

        {/* Today's Official Daily Chapter Chip */}
        <div className="mt-3.5 flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-full border border-gold-bright/45 bg-white/10 px-3 py-1 text-xs font-bold text-gold-bright">
            <BookOpen className="h-3.5 w-3.5" />
            <span>
              {officialChapter.isBeforeChallenge
                ? `بداية الخطة (اليوم 1): ${officialChapter.shortLabelAr}`
                : `ورد اليوم: ${officialChapter.fullLabelAr}`}
            </span>
          </span>
          <span className="inline-flex items-center gap-1 rounded-full bg-gold-warm px-3 py-1 text-xs font-extrabold text-midnight-dark">
            <Sparkles className="h-3.5 w-3.5" />
            <span>الأسبوع النشط: {activeSyllabus.shortLabelAr}</span>
          </span>
        </div>
      </section>

      {/* Active Week Highlight Card with Quick Submission Actions */}
      <section className="space-y-3 rounded-[24px] border-2 border-gold-warm bg-white p-4 shadow-md">
        <div className="flex items-center justify-between gap-2">
          <div>
            <span className="inline-block rounded-full bg-gold-warm/20 px-2.5 py-0.5 text-[11px] font-extrabold text-midnight">
              المقرر الحالي النشط ✨
            </span>
            <h3 className="mt-1 text-lg font-extrabold text-midnight">
              {activeSyllabus.titleAr}
            </h3>
            <p className="text-xs font-semibold text-ink-muted">
              <CalendarDays className="ml-1 inline h-3.5 w-3.5 text-gold-warm" />
              {activeSyllabus.dateRangeAr}
            </p>
          </div>
          {isQuizToday && (
            <span className="rounded-xl bg-burgundy px-2.5 py-1.5 text-center text-[11px] font-bold text-white shadow-sm">
              موعد المسابقة اليوم! 🎯
            </span>
          )}
        </div>

        {/* Syllabus Breakdown */}
        <div className="space-y-2.5">
          <div className="rounded-2xl border border-midnight/15 bg-parchment/55 p-3">
            <p className="text-[11px] font-bold text-midnight">📖 الكتاب المقدس</p>
            <p className="mt-0.5 text-sm font-extrabold text-ink-deep">
              {activeSyllabus.bibleTitleAr}
            </p>
            <p className="text-xs text-ink-muted">{activeSyllabus.bibleDetailAr}</p>
          </div>

          <div className="rounded-2xl border border-gold-warm/40 bg-status-pendingBg/65 p-3">
            <p className="text-[11px] font-bold text-gold-warm">
              📚 كتاب «في تجسد الكلمة» — القديس أثناسيوس الرسولي
            </p>
            <p className="mt-0.5 text-sm font-extrabold text-ink-deep">
              {activeSyllabus.patristicTitleAr}
            </p>
            <p className="text-xs font-semibold text-ink-muted">{activeSyllabus.patristicDetailAr}</p>

            {incarnationTask && (
              <div className="mt-2.5 flex items-center justify-between gap-2 border-t border-gold-warm/25 pt-2.5">
                {incarnationSub ? (
                  <StatusBadge status={incarnationSub.status} />
                ) : (
                  <span className="text-xs font-bold text-gold-warm">
                    +{incarnationTask.points} نقطة أسبوعياً
                  </span>
                )}
                <button
                  type="button"
                  disabled={isIncarnationLocked}
                  onClick={() => {
                    setSheetInitialNote(
                      `أتممت قراءة ورد ${activeSyllabus.titleAr} من كتاب في تجسد الكلمة (${activeSyllabus.patristicTitleAr} • ${activeSyllabus.patristicDetailAr})`
                    );
                    setSelectedTaskForSheet(incarnationTask);
                  }}
                  className={`h-10 rounded-xl px-3.5 text-xs font-bold transition ${
                    isIncarnationLocked
                      ? 'bg-parchment-variant text-ink-muted'
                      : 'bg-midnight text-white shadow-sm hover:bg-midnight-dark'
                  }`}
                >
                  {incarnationSub?.status === 'approved'
                    ? 'تم الاعتماد ✓'
                    : incarnationSub?.status === 'pending'
                      ? 'قيد المراجعة ⏳'
                      : 'تسجيل قراءة الكتاب'}
                </button>
              </div>
            )}
          </div>

          {/* Automatic Weekly Quiz Card */}
          <div className="rounded-2xl border border-burgundy/35 bg-burgundy/8 p-3.5">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-1.5 text-burgundy">
                <Trophy className="h-4 w-4 shrink-0" />
                <p className="text-xs font-extrabold">
                  {activeAutoQuizData.titleAr} • {activeSyllabus.quizDateAr}
                </p>
              </div>
              <span className="rounded-full bg-burgundy px-2.5 py-0.5 text-[11px] font-bold text-white">
                {activeAutoQuizData.totalItems} سؤال • {activeAutoQuizData.maxPoints} نقطة
              </span>
            </div>

            <p className="mt-1.5 text-xs leading-relaxed text-ink-muted">
              {activeAutoQuizData.subtitleAr} — تصحيح تلقائي فوري وإضافة النقاط مباشرة لرصيدك (2 نقطة لكل سؤال).
            </p>

            <div className="mt-3">
              {activeWeekQuizSub ? (
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="inline-flex items-center gap-1 rounded-full bg-status-approvedBg px-3 py-1 text-xs font-extrabold text-status-approved">
                    <CheckCircle2 className="h-4 w-4" />
                    <span>
                      تم الحل والاعتماد: {activeWeekQuizSub.points} / {activeAutoQuizData.maxPoints} نقطة
                    </span>
                  </span>
                  <button
                    type="button"
                    onClick={() =>
                      setActiveAutoQuiz({
                        weekNumber: activeWeekNum,
                        completedPoints: activeWeekQuizSub.points,
                      })
                    }
                    className="inline-flex h-10 items-center gap-1.5 rounded-xl border border-midnight/25 bg-white px-3.5 text-xs font-bold text-midnight hover:bg-midnight hover:text-white"
                  >
                    <Eye className="h-4 w-4" />
                    <span>مراجعة الإجابات</span>
                  </button>
                </div>
              ) : isActiveWeekQuizEnabled ? (
                <button
                  type="button"
                  disabled={isAfterEnd}
                  onClick={() =>
                    setActiveAutoQuiz({
                      weekNumber: activeWeekNum,
                      completedPoints: null,
                    })
                  }
                  className="inline-flex h-11 w-full items-center justify-center gap-1.5 rounded-xl bg-burgundy px-3 text-xs font-bold text-white shadow-sm transition hover:opacity-95 disabled:opacity-50"
                >
                  <PlayCircle className="h-4 w-4 text-gold-bright" />
                  <span>
                    {isAfterEnd
                      ? 'انتهت فترة التحدي'
                      : `ابدأ ${activeAutoQuizData.titleAr} (${activeAutoQuizData.totalItems} سؤال • تصحيح فوري)`}
                  </span>
                </button>
              ) : (
                <div className="flex items-center gap-2 rounded-xl border border-parchment-outline bg-white/85 px-3 py-2 text-xs font-semibold text-ink-muted">
                  <Lock className="h-4 w-4 shrink-0 text-gold-warm" />
                  <span>بانتظار تفعيل هذه المسابقة من خدام التحدي</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Full 5-Week Syllabus Explorer */}
      <div className="space-y-2.5">
        <h3 className="text-base font-extrabold text-midnight">
          جدول الأسابيع الخمسة بالكامل (30 نوفمبر – 5 يناير)
        </h3>

        {WEEKLY_SYLLABUS.map((week) => {
          const isActive = week.weekNumber === activeWeekNum;
          const isOpen = expandedWeek === week.weekNumber;
          const weekQuiz = getAutoWeeklyQuiz(week.weekNumber);
          const isWeekQuizEnabled = enabledQuizWeeks.includes(week.weekNumber);
          const weekQuizSub = findUserAutoQuizSubmission(
            userSubmissions,
            userId,
            week.weekNumber,
            today
          );

          return (
            <div
              key={week.weekNumber}
              className={`overflow-hidden rounded-[20px] border bg-white transition ${
                isActive ? 'border-gold-warm shadow-sm' : 'border-parchment-border'
              }`}
            >
              <button
                type="button"
                onClick={() => setExpandedWeek(isOpen ? 0 : week.weekNumber)}
                className="flex w-full items-center justify-between gap-3 p-4 text-right"
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl text-sm font-extrabold ${
                      isActive
                        ? 'bg-gold-warm text-midnight-dark'
                        : 'bg-midnight/10 text-midnight'
                    }`}
                  >
                    {week.weekNumber}
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-1.5">
                      <span className="text-sm font-extrabold text-ink-deep">
                        {week.titleAr}
                      </span>
                      {isActive && (
                        <span className="rounded-full bg-midnight px-2 py-0.5 text-[10px] font-bold text-gold-bright">
                          الأسبوع الحالي
                        </span>
                      )}
                      {weekQuizSub ? (
                        <span className="rounded-full bg-status-approvedBg px-2 py-0.5 text-[10px] font-bold text-status-approved">
                          تم حل المسابقة ({weekQuizSub.points}/200) ✓
                        </span>
                      ) : isWeekQuizEnabled ? (
                        <span className="rounded-full bg-burgundy/12 px-2 py-0.5 text-[10px] font-bold text-burgundy">
                          المسابقة مفعّلة ✨
                        </span>
                      ) : null}
                    </div>
                    <p className="text-xs text-ink-muted">{week.dateRangeAr}</p>
                  </div>
                </div>
                <span className="text-xs font-bold text-midnight">
                  {isOpen ? 'إخفاء ▲' : 'التفاصيل ▼'}
                </span>
              </button>

              {isOpen && (
                <div className="space-y-2.5 border-t border-parchment-border bg-parchment/35 px-4 py-3 text-xs">
                  <div className="flex items-start gap-2">
                    <BookOpen className="mt-0.5 h-4 w-4 shrink-0 text-midnight" />
                    <div>
                      <span className="font-bold text-midnight">الكتاب المقدس: </span>
                      <span className="font-semibold text-ink-deep">{week.bibleTitleAr}</span>
                      <span className="text-ink-muted"> — {week.bibleDetailAr}</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-2">
                    <BookMarked className="mt-0.5 h-4 w-4 shrink-0 text-gold-warm" />
                    <div>
                      <span className="font-bold text-gold-warm">كتاب «في تجسد الكلمة»: </span>
                      <span className="font-semibold text-ink-deep">{week.patristicTitleAr}</span>
                      <span className="text-ink-muted"> ({week.patristicDetailAr})</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-2">
                    <HelpCircle className="mt-0.5 h-4 w-4 shrink-0 text-burgundy" />
                    <div>
                      <span className="font-bold text-burgundy">{week.quizTitleAr}: </span>
                      <span className="font-semibold text-ink-deep">
                        {week.quizDateAr} ({weekQuiz.totalItems} سؤال • {weekQuiz.maxPoints} نقطة)
                      </span>
                    </div>
                  </div>

                  {weekQuizSub ? (
                    <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
                      <span className="inline-flex items-center gap-1 rounded-full bg-status-approvedBg px-2.5 py-1 text-[11px] font-bold text-status-approved">
                        <CheckCircle2 className="h-3.5 w-3.5" />
                        <span>
                          نتيجتك المعتمدة: {weekQuizSub.points} / {weekQuiz.maxPoints} نقطة
                        </span>
                      </span>
                      <button
                        type="button"
                        onClick={() =>
                          setActiveAutoQuiz({
                            weekNumber: week.weekNumber,
                            completedPoints: weekQuizSub.points,
                          })
                        }
                        className="inline-flex h-9 items-center gap-1 rounded-xl border border-midnight/25 bg-white px-3 text-[11px] font-bold text-midnight hover:bg-midnight hover:text-white"
                      >
                        <Eye className="h-3.5 w-3.5" />
                        <span>مراجعة الإجابات</span>
                      </button>
                    </div>
                  ) : isWeekQuizEnabled ? (
                    <button
                      type="button"
                      disabled={isAfterEnd}
                      onClick={() =>
                        setActiveAutoQuiz({
                          weekNumber: week.weekNumber,
                          completedPoints: null,
                        })
                      }
                      className="inline-flex h-10 w-full items-center justify-center gap-1.5 rounded-xl bg-burgundy px-3 text-xs font-bold text-white shadow-xs transition hover:opacity-95 disabled:opacity-50"
                    >
                      <PlayCircle className="h-4 w-4 text-gold-bright" />
                      <span>ابدأ {weekQuiz.titleAr} الآن ({weekQuiz.totalItems} سؤال)</span>
                    </button>
                  ) : (
                    <div className="flex items-center gap-1.5 text-[11px] font-semibold text-ink-muted">
                      <Lock className="h-3.5 w-3.5 text-gold-warm" />
                      <span>المسابقة مغلقة حالياً بانتظار التفعيل من خدام التحدي</span>
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
