import React, { useMemo, useState } from 'react';
import {
  BellRing,
  BookMarked,
  BookOpen,
  Calendar,
  CheckCircle2,
  ClipboardCheck,
  Clock,
  ExternalLink,
  Hourglass,
  LogOut,
  Trophy,
  X,
} from 'lucide-react';
import {
  type AppConfig,
  type ChallengeTask,
  type LeaderboardEntry,
  type ReminderSettings,
  type TaskFrequency,
  type TaskSubmission,
  type YouthUser,
  FREQUENCY_LABELS,
} from '../types';
import { FastCalendar, formatTimeArabic, todayFastDate } from '../lib/fastCalendar';
import {
  decodeQuizConfig,
  getActiveSyllabusWeek,
  getTodayOfficialBiblePlan,
  type WeeklyQuizQuestion,
  type WeeklySyllabusItem,
} from '../lib/weeklySyllabus';
import { stripQuizMetaFromQuestion } from '../lib/readingPlan';
import { UrlValidator, openSafeExternalUrl, shareOrCopyText } from '../lib/securityAndExport';
import {
  DailyVerseBanner,
  StatusBadge,
  TaskIcon,
  TaskSubmissionSheet,
} from '../components/SharedUi';
import { InAppWeeklyQuizModal } from '../components/WeeklySyllabusAndQuiz';
import {
  InAppBibleChapterReaderModal,
  InAppBookPdfReaderModal,
} from '../components/InAppReaders';
import { APP_ASSETS } from '../lib/assets';
import {
  getNotificationPermission,
  isNotificationSupported,
  requestBrowserNotificationPermission,
} from '../lib/notifications';
import { localStore } from '../lib/storage';

interface DailyTasksScreenProps {
  user: YouthUser;
  rankEntry: LeaderboardEntry | null;
  allTasks: ChallengeTask[];
  userSubmissions: TaskSubmission[];
  appConfig: AppConfig;
  reminderSettings: ReminderSettings;
  onSubmitTask: (task: ChallengeTask, note: string) => void;
  onOpenReadingPlan?: () => void;
  onOpenMyRank: () => void;
  onOpenReminders: () => void;
  onLogout: () => void;
  onFeedback: (msg: string) => void;
}

type TaskFilter = 'all' | TaskFrequency;

const FILTERS: Array<{ id: TaskFilter; label: string }> = [
  { id: 'all', label: 'كل المهام' },
  { id: 'daily', label: 'يومي' },
  { id: 'weekly', label: 'أسبوعي' },
  { id: 'once', label: 'مرة في التحدي' },
];

const FREQUENCY_SECTIONS: Array<{
  frequency: TaskFrequency;
  titleAr: string;
  subtitleAr: string;
}> = [
  { frequency: 'daily', titleAr: 'المهام اليومية', subtitleAr: 'تُجدَّد كل يوم' },
  { frequency: 'weekly', titleAr: 'المهام الأسبوعية', subtitleAr: 'حسب منهج كل أسبوع' },
  { frequency: 'once', titleAr: 'مهام مرة في التحدي', subtitleAr: 'مرة واحدة خلال الصوم' },
];

export const DailyTasksScreen: React.FC<DailyTasksScreenProps> = ({
  user,
  rankEntry,
  allTasks,
  userSubmissions,
  appConfig,
  reminderSettings,
  onSubmitTask,
  onOpenMyRank,
  onOpenReminders,
  onLogout,
  onFeedback,
}) => {
  const today = useMemo(() => todayFastDate(), []);
  const currentDay = FastCalendar.currentFastDayIndex(today);
  const isBeforeStart = FastCalendar.isBeforeStart(today);
  const isAfterEnd = FastCalendar.isAfterEnd(today);
  const isWithinWindow = !isBeforeStart && !isAfterEnd;
  const daysToStart = FastCalendar.daysUntilStart(today);
  const daysToEnd = FastCalendar.daysRemainingToFeast(today);
  const fastProgress = Math.min(Math.max(currentDay / FastCalendar.FAST_DAYS_COUNT, 0), 1);

  const [selectedFilter, setSelectedFilter] = useState<TaskFilter>('all');
  const [selectedTaskForSheet, setSelectedTaskForSheet] = useState<ChallengeTask | null>(null);
  const [sheetInitialNote, setSheetInitialNote] = useState('');
  const [activeQuizModal, setActiveQuizModal] = useState<{
    task: ChallengeTask;
    questions: WeeklyQuizQuestion[];
    week: WeeklySyllabusItem;
    alreadySubmitted: boolean;
  } | null>(null);
  const [showBookReader, setShowBookReader] = useState(false);
  const [showBibleReader, setShowBibleReader] = useState(false);

  const [notifPermission, setNotifPermission] = useState<NotificationPermission>(() =>
    getNotificationPermission()
  );
  const [notifPromptDismissed, setNotifPromptDismissed] = useState<boolean>(() =>
    localStore.isNotifPromptDismissed()
  );

  const { activeWeek } = useMemo(() => getActiveSyllabusWeek(today), [today]);
  const todayOfficialBible = useMemo(() => getTodayOfficialBiblePlan(today), [today]);

  const decodedQuiz = useMemo(() => {
    const decoded = decodeQuizConfig(appConfig.weeklyQuizQuestion);
    const cleanIntro = stripQuizMetaFromQuestion(decoded.introText);
    const mergedQuestions =
      appConfig.weeklyQuizQuestions && appConfig.weeklyQuizQuestions.length > 0
        ? appConfig.weeklyQuizQuestions
        : decoded.questions;
    return {
      introText: cleanIntro,
      questions: mergedQuestions,
    };
  }, [appConfig.weeklyQuizQuestion, appConfig.weeklyQuizQuestions]);

  const hasInAppQuestions = decodedQuiz.questions.length > 0;
  const hasExternalQuizLink = UrlValidator.isSafeHttpUrl(appConfig.weeklyQuizUrl);

  const activeTasks = useMemo(
    () => allTasks.filter((t) => t.isActive).sort((a, b) => a.sortOrder - b.sortOrder),
    [allTasks]
  );

  const bibleTask = useMemo(
    () => activeTasks.find((t) => t.id === 'task_bible_reading') ?? null,
    [activeTasks]
  );
  const incarnationTask = useMemo(
    () => activeTasks.find((t) => t.id === 'task_incarnation_book') ?? null,
    [activeTasks]
  );

  const getSubmissionForTask = (task: ChallengeTask | null): TaskSubmission | null => {
    if (!task) return null;
    const pk = FastCalendar.periodKeyForTask(task.frequency, today);
    return userSubmissions.find((s) => s.taskId === task.id && s.periodKey === pk) ?? null;
  };

  const bibleSubToday = getSubmissionForTask(bibleTask);
  const isBibleLocked =
    isAfterEnd || bibleSubToday?.status === 'pending' || bibleSubToday?.status === 'approved';

  const incarnationSubThisWeek = getSubmissionForTask(incarnationTask);
  const isIncarnationLocked =
    isAfterEnd ||
    incarnationSubThisWeek?.status === 'pending' ||
    incarnationSubThisWeek?.status === 'approved';

  const showNotifPrompt =
    isNotificationSupported() && notifPermission === 'default' && !notifPromptDismissed;

  const handleEnableNotifications = async () => {
    const result = await requestBrowserNotificationPermission();
    setNotifPermission(result);
    if (result === 'granted') {
      onFeedback('تم تفعيل الإشعارات بنجاح! سيصلك تنبيه فوري عند اعتماد نقاطك 🎉');
    } else {
      onFeedback('يمكنك تفعيل الإشعارات لاحقاً من أيقونة الجرس بالأعلى');
    }
  };

  const completedNowCount = useMemo(() => {
    return activeTasks.filter((task) => {
      const periodKey = FastCalendar.periodKeyForTask(task.frequency, today);
      const sub = userSubmissions.find((s) => s.taskId === task.id && s.periodKey === periodKey);
      return sub && (sub.status === 'approved' || sub.status === 'pending');
    }).length;
  }, [activeTasks, userSubmissions, today]);

  const completionRatio = activeTasks.length === 0 ? 0 : completedNowCount / activeTasks.length;

  // Strictly ordered task sections: Daily -> Weekly -> Once
  const visibleSections = useMemo(() => {
    const targetSections =
      selectedFilter === 'all'
        ? FREQUENCY_SECTIONS
        : FREQUENCY_SECTIONS.filter((s) => s.frequency === selectedFilter);
    return targetSections
      .map((section) => ({
        ...section,
        tasks: activeTasks.filter((t) => t.frequency === section.frequency),
      }))
      .filter((section) => section.tasks.length > 0);
  }, [activeTasks, selectedFilter]);

  const visibleTaskCount = useMemo(
    () => visibleSections.reduce((acc, s) => acc + s.tasks.length, 0),
    [visibleSections]
  );

  // SVG Ring geometry
  const ringRadius = 32;
  const ringCircumference = 2 * Math.PI * ringRadius;
  const ringOffset = ringCircumference * (1 - completionRatio);

  const daySubtitle = isBeforeStart
    ? `وضع تجريبي 🧪 • التحدي الرسمي يبدأ خلال ${daysToStart} يوم (30/11)`
    : isAfterEnd
      ? 'انتهى التحدي يوم 5 يناير 2027 🎉'
      : `اليوم ${currentDay} من ${FastCalendar.FAST_DAYS_COUNT} • باقي ${daysToEnd} يوم على النهاية`;

  const getDefaultNoteForTask = (task: ChallengeTask): string => {
    if (task.id === 'task_bible_reading') {
      return `تمت قراءة ${todayOfficialBible.fullLabelAr}`;
    }
    if (task.id === 'task_incarnation_book') {
      return `تمت قراءة ورد ${activeWeek.titleAr} من كتاب «في تجسد الكلمة»`;
    }
    if (task.id === 'task_weekly_exam_incarnation' || task.id === 'task_weekly_quiz') {
      return `${activeWeek.titleAr} (${activeWeek.examDateAr}) — إنجيل + كتاب في تجسد الكلمة`;
    }
    return '';
  };

  const openTaskSubmission = (task: ChallengeTask, customNote?: string) => {
    setSheetInitialNote(customNote !== undefined ? customNote : getDefaultNoteForTask(task));
    setSelectedTaskForSheet(task);
  };

  return (
    <div className="mx-auto max-w-[640px] space-y-3.5 px-4 pt-3.5 pb-40">
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

      {activeQuizModal && (
        <InAppWeeklyQuizModal
          title={appConfig.weeklyQuizTitle}
          week={activeQuizModal.week}
          questions={activeQuizModal.questions}
          alreadySubmitted={activeQuizModal.alreadySubmitted}
          onDismiss={() => setActiveQuizModal(null)}
          onSubmitQuizResult={(summary) => {
            onSubmitTask(activeQuizModal.task, summary);
            setActiveQuizModal(null);
          }}
        />
      )}

      {showBookReader && (
        <InAppBookPdfReaderModal
          initialWeekNumber={activeWeek.weekNumber}
          activeWeekNumber={activeWeek.weekNumber}
          points={incarnationTask?.points ?? 200}
          isLocked={isIncarnationLocked}
          statusLabel={
            incarnationSubThisWeek?.status === 'approved'
              ? 'تم اعتماد قراءة هذا الأسبوع ✅'
              : incarnationSubThisWeek?.status === 'pending'
                ? 'تم إرسال قراءة الأسبوع للمراجعة ⏳'
                : undefined
          }
          onDismiss={() => setShowBookReader(false)}
          onLogWeekReading={
            incarnationTask
              ? (week) => {
                  setShowBookReader(false);
                  openTaskSubmission(
                    incarnationTask,
                    `تمت قراءة ورد ${week.titleAr} من كتاب «في تجسد الكلمة»`
                  );
                }
              : undefined
          }
        />
      )}

      {showBibleReader && (
        <InAppBibleChapterReaderModal
          initialDayNumber={todayOfficialBible.dayNumber}
          todayDayNumber={todayOfficialBible.dayNumber}
          points={bibleTask?.points ?? 100}
          isLocked={isBibleLocked}
          statusLabel={
            bibleSubToday?.status === 'approved'
              ? 'تم اعتماد قراءة اليوم ✅'
              : bibleSubToday?.status === 'pending'
                ? 'تم إرسال قراءة اليوم للمراجعة ⏳'
                : undefined
          }
          onDismiss={() => setShowBibleReader(false)}
          onLogBibleReading={
            bibleTask
              ? (dayPlan) => {
                  setShowBibleReader(false);
                  openTaskSubmission(bibleTask, `تمت قراءة ${dayPlan.fullLabelAr}`);
                }
              : undefined
          }
        />
      )}

      {/* Interactive Nativity Hero Card */}
      <section className="relative overflow-hidden rounded-[26px] shadow-lg">
        <img
          src={APP_ASSETS.nativityHero}
          alt="نجمة بيت لحم لصوم الميلاد المجيد"
          className="absolute inset-0 h-full w-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-b from-midnight-dark/60 to-midnight-dark/95" />

        <div className="relative flex min-h-[228px] flex-col justify-between gap-4 p-[18px] text-white">
          <div className="flex items-center justify-between gap-3">
            <div className="min-w-0 flex-1 space-y-1">
              <h2 className="truncate text-xl font-bold text-white">أهلاً يا {user.fullName} ✨</h2>
              <p className="text-xs font-medium text-gold-bright">{daySubtitle}</p>

              {rankEntry && (
                <button
                  type="button"
                  onClick={onOpenMyRank}
                  className="mt-1 inline-flex items-center gap-1.5 rounded-full border border-gold-bright/45 bg-burgundy px-3 py-1 text-xs font-bold text-white shadow transition hover:opacity-95 active:scale-95"
                >
                  <Trophy className="h-3.5 w-3.5 text-gold-bright" />
                  <span>مركزك الآن: #{rankEntry.rank}</span>
                </button>
              )}
            </div>

            {/* Animated Circular Progress Gauge */}
            <div className="relative flex h-[76px] w-[76px] shrink-0 items-center justify-center">
              <svg className="h-full w-full -rotate-90" viewBox="0 0 76 76">
                <circle
                  cx="38"
                  cy="38"
                  r={ringRadius}
                  fill="none"
                  stroke="rgba(255,255,255,0.18)"
                  strokeWidth="7"
                />
                <circle
                  cx="38"
                  cy="38"
                  r={ringRadius}
                  fill="none"
                  stroke="#E5BF70"
                  strokeWidth="7"
                  strokeLinecap="round"
                  strokeDasharray={ringCircumference}
                  strokeDashoffset={ringOffset}
                  className="transition-all duration-500 ease-out"
                />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center">
                <span className="text-sm font-bold leading-none text-white">
                  {completedNowCount}/{activeTasks.length}
                </span>
                <span className="mt-0.5 text-[10px] font-semibold text-gold-bright">مُنجَز</span>
              </div>
            </div>
          </div>

          {/* Approved vs Pending Score Row */}
          <div className="grid grid-cols-2 gap-2.5">
            <div className="rounded-2xl bg-gold-warm px-3 py-2 text-center text-midnight-dark">
              <p className="text-2xl font-extrabold leading-tight">
                {rankEntry?.totalApprovedPoints ?? 0}
              </p>
              <p className="text-[11px] font-bold">
                {isBeforeStart ? 'نقاط تجريبية 🧪' : 'نقاطك المعتمدة ✅'}
              </p>
            </div>
            <div className="rounded-2xl border border-gold-bright/45 bg-white/15 px-3 py-2 text-center backdrop-blur-sm">
              <p className="text-2xl font-extrabold leading-tight text-white">
                {rankEntry?.pendingPoints ?? 0}
              </p>
              <p className="text-[11px] font-bold text-gold-bright">قيد المراجعة ⏳</p>
            </div>
          </div>

          {/* 37-day fast progress bar */}
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/25">
            <div
              className="h-full rounded-full bg-gold-bright transition-all duration-500"
              style={{ width: `${Math.round(fastProgress * 100)}%` }}
            />
          </div>
        </div>
      </section>

      {/* One-tap Points Approval Notification Prompt */}
      {showNotifPrompt && (
        <section className="flex items-center gap-3 rounded-[20px] border border-gold-warm/45 bg-white p-3.5 shadow-sm">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-status-pendingBg text-gold-warm">
            <BellRing className="h-5 w-5" />
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-xs font-bold text-ink-deep">فعّل إشعارات اعتماد النقاط</p>
            <p className="mt-0.5 text-[11px] leading-snug text-ink-muted">
              يصلك تنبيه فوري بمجرد اعتماد خدام التحدي لمهامك وإضافة نقاطك الجديدة
            </p>
          </div>
          <button
            type="button"
            onClick={handleEnableNotifications}
            className="shrink-0 rounded-xl bg-midnight px-3 py-2 text-xs font-bold text-white transition hover:bg-midnight-dark active:scale-95"
          >
            تفعيل
          </button>
          <button
            type="button"
            aria-label="إخفاء"
            onClick={() => {
              localStore.dismissNotifPrompt();
              setNotifPromptDismissed(true);
            }}
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-ink-muted hover:bg-parchment-variant"
          >
            <X className="h-4 w-4" />
          </button>
        </section>
      )}

      {/* Trial Mode Banner — automatically hidden starting Nov 30, 2026 */}
      {!isWithinWindow && (
        <section
          className={`rounded-[20px] border p-4 ${
            isBeforeStart
              ? 'border-gold-warm bg-status-pendingBg'
              : 'border-burgundy/45 bg-burgundy/10'
          }`}
        >
          <h3
            className={`text-base font-bold ${
              isBeforeStart ? 'text-midnight-dark' : 'text-burgundy'
            }`}
          >
            {isBeforeStart
              ? `الوضع التجريبي مفعل حتى 30 نوفمبر 🧪 (متبقي ${daysToStart} يوم)`
              : 'انتهت فترة تحدي طريق المزود 🎉'}
          </h3>
          <p className="mt-1 text-xs leading-relaxed text-ink-deep">
            {isBeforeStart
              ? 'جرّب تسجيل المهام الآن بحرية لتتعود على التطبيق! هذه النقاط تجريبية فقط، ومن يوم 30 نوفمبر 2026 يختفي هذا التنويه وتبدأ النتائج الرسمية من الصفر حتى 5 يناير 2027.'
              : 'انتهت فترة تسجيل المهام يوم 5 يناير 2027. يمكنك متابعة مركزك وسجلك بانتظار إعلان الفائزين الـ 10!'}
          </p>
        </section>
      )}

      {/* Compact Daily Verse Banner */}
      <DailyVerseBanner
        verseText={appConfig.dailyVerseText}
        reference={appConfig.dailyVerseRef}
        encouragement={appConfig.dailyEncouragement}
        onShare={async (text) => {
          const msg = await shareOrCopyText(text, 'آية اليوم في طريق المزود');
          onFeedback(msg);
        }}
      />

      {/* Category Filter Chips */}
      <div className="space-y-2 pt-1">
        <h3 className="text-lg font-bold text-ink-deep">
          مهام طريق المزود ({visibleTaskCount})
        </h3>
        <div className="no-scrollbar flex gap-2 overflow-x-auto pb-1">
          {FILTERS.map((f) => {
            const active = selectedFilter === f.id;
            return (
              <button
                key={f.id}
                type="button"
                onClick={() => setSelectedFilter(f.id)}
                className={`shrink-0 rounded-full border px-4 py-1.5 text-xs font-bold transition ${
                  active
                    ? 'border-midnight bg-midnight text-white shadow-sm'
                    : 'border-parchment-outline bg-white text-ink-deep hover:bg-parchment-variant'
                }`}
              >
                {f.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Strictly Ordered Grouped Task Sections: Daily -> Weekly -> Once */}
      <div className="space-y-5">
        {visibleSections.map((section) => (
          <div key={section.frequency} className="space-y-3">
            {selectedFilter === 'all' && (
              <div className="flex items-center justify-between px-1">
                <div className="flex items-center gap-2">
                  <span className="h-2.5 w-2.5 rounded-full bg-gold-warm" />
                  <h4 className="text-sm font-extrabold text-midnight">{section.titleAr}</h4>
                  <span className="rounded-full bg-midnight/10 px-2 py-0.5 text-[11px] font-bold text-midnight">
                    {section.tasks.length}
                  </span>
                </div>
                <span className="text-[11px] font-medium text-ink-muted">
                  {section.subtitleAr}
                </span>
              </div>
            )}

            <div className="space-y-3">
              {section.tasks.map((task) => {
                const periodKey = FastCalendar.periodKeyForTask(task.frequency, today);
                const existingSub = userSubmissions.find(
                  (s) => s.taskId === task.id && s.periodKey === periodKey
                );
                const status = existingSub?.status ?? null;
                const isLocked = isAfterEnd || status === 'pending' || status === 'approved';

                const borderClass =
                  status === 'approved'
                    ? 'border-status-approved/70'
                    : status === 'pending'
                      ? 'border-gold-warm/75'
                      : status === 'rejected'
                        ? 'border-burgundy/65'
                        : 'border-parchment-border';

                const iconCircleClass =
                  status === 'approved'
                    ? 'bg-status-approved/15 text-status-approved'
                    : status === 'pending'
                      ? 'bg-gold-warm/15 text-gold-warm'
                      : 'bg-midnight/10 text-midnight';

                const isBibleTask = task.id === 'task_bible_reading';
                const isIncarnationTask = task.id === 'task_incarnation_book';
                const isWeeklyExamTask =
                  task.id === 'task_weekly_exam_incarnation' || task.id === 'task_weekly_quiz';

                return (
                  <div
                    key={task.id}
                    onClick={() => !isLocked && openTaskSubmission(task)}
                    className={`rounded-[22px] border bg-white p-4 transition ${borderClass} ${
                      isLocked
                        ? 'shadow-xs'
                        : 'cursor-pointer shadow-sm hover:shadow-md active:scale-[0.995]'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full ${iconCircleClass}`}
                      >
                        {status === 'approved' ? (
                          <CheckCircle2 className="h-6 w-6" />
                        ) : status === 'pending' ? (
                          <Hourglass className="h-6 w-6" />
                        ) : (
                          <TaskIcon iconKey={task.iconKey} className="h-6 w-6" />
                        )}
                      </div>

                      <div className="min-w-0 flex-1">
                        <h4 className="text-base font-bold text-ink-deep">{task.title}</h4>
                        <div className="mt-1 flex flex-wrap items-center gap-1.5">
                          <span className="rounded-full bg-parchment-variant px-2.5 py-0.5 text-[11px] font-semibold text-ink-muted">
                            {FREQUENCY_LABELS[task.frequency]}
                          </span>
                          <span className="rounded-full bg-status-pendingBg px-2.5 py-0.5 text-xs font-bold text-gold-warm">
                            +{task.points} نقطة
                          </span>
                        </div>
                      </div>
                    </div>

                    {task.description.trim() && (
                      <p className="mt-2.5 text-xs leading-relaxed text-ink-muted">
                        {task.description}
                      </p>
                    )}

                    {/* INLINE: Daily Bible Plan + In-App Chapter Reader */}
                    {isBibleTask && (
                      <div
                        onClick={(e) => e.stopPropagation()}
                        className="mt-3 space-y-2.5 rounded-2xl border border-midnight/20 bg-parchment/70 p-3"
                      >
                        <div className="flex flex-wrap items-center justify-between gap-1.5">
                          <span className="text-[11px] font-extrabold text-midnight">
                            📖 ورد اليوم ({activeWeek.titleAr} • {todayOfficialBible.dateLabelAr})
                          </span>
                          <span className="rounded-full bg-midnight/10 px-2 py-0.5 text-[10px] font-bold text-midnight">
                            {todayOfficialBible.shortLabelAr}
                          </span>
                        </div>
                        <p className="text-xs font-bold text-ink-deep">
                          {todayOfficialBible.fullLabelAr}
                        </p>
                        <p className="text-[11px] leading-relaxed text-ink-muted">
                          منهج الأسبوع: {activeWeek.bibleRangeAr}
                        </p>
                        <div className="flex flex-wrap items-center gap-2 pt-0.5">
                          <button
                            type="button"
                            onClick={() => setShowBibleReader(true)}
                            className="inline-flex h-10 flex-1 items-center justify-center gap-1.5 rounded-xl bg-midnight px-3 text-xs font-bold text-white shadow-xs transition hover:bg-midnight-dark active:scale-[0.98]"
                          >
                            <BookOpen className="h-4 w-4 text-gold-bright" />
                            <span>اقرأ إصحاح اليوم داخل التطبيق</span>
                          </button>
                          <button
                            type="button"
                            onClick={onOpenReminders}
                            className="inline-flex h-10 items-center gap-1 rounded-xl border border-midnight/25 bg-white px-3 text-[11px] font-bold text-midnight transition hover:bg-parchment-variant"
                          >
                            <Clock className="h-3.5 w-3.5" />
                            <span>
                              {formatTimeArabic(
                                reminderSettings.bibleReadingHour,
                                reminderSettings.bibleReadingMinute
                              )}
                            </span>
                          </button>
                        </div>
                      </div>
                    )}

                    {/* INLINE: Weekly "On the Incarnation" Reading + In-App PDF Reader */}
                    {isIncarnationTask && (
                      <div
                        onClick={(e) => e.stopPropagation()}
                        className="mt-3 space-y-2 rounded-2xl border border-burgundy/25 bg-status-pendingBg/50 p-3"
                      >
                        <div className="flex flex-wrap items-center justify-between gap-1.5">
                          <span className="text-[11px] font-extrabold text-burgundy">
                            📚 ورد {activeWeek.titleAr} ({activeWeek.dateRangeAr})
                          </span>
                          <span className="rounded-full bg-burgundy/12 px-2 py-0.5 text-[10px] font-bold text-burgundy">
                            ص {activeWeek.pdfStartPage} – {activeWeek.pdfEndPage} بالملف
                          </span>
                        </div>
                        <p className="text-xs font-semibold leading-relaxed text-ink-deep">
                          {activeWeek.incarnationBookAr}
                        </p>
                        <button
                          type="button"
                          onClick={() => setShowBookReader(true)}
                          className="inline-flex h-10 w-full items-center justify-center gap-1.5 rounded-xl bg-burgundy px-3 text-xs font-bold text-white shadow-xs transition hover:opacity-95 active:scale-[0.98]"
                        >
                          <BookMarked className="h-4 w-4 text-gold-bright" />
                          <span>اقرأ ورد هذا الأسبوع من الكتاب داخل التطبيق</span>
                        </button>
                      </div>
                    )}

                    {/* INLINE: Weekly Exam Syllabus + In-App Quiz / External Link */}
                    {isWeeklyExamTask && (
                      <div
                        onClick={(e) => e.stopPropagation()}
                        className="mt-3 space-y-2 rounded-2xl border border-burgundy/25 bg-burgundy/5 p-3 text-xs"
                      >
                        <div className="flex flex-wrap items-center justify-between gap-1.5">
                          <span className="font-extrabold text-midnight">
                            {activeWeek.titleAr} ({activeWeek.dateRangeAr})
                          </span>
                          <span className="inline-flex items-center gap-1 rounded-full bg-burgundy/12 px-2 py-0.5 text-[10px] font-bold text-burgundy">
                            <Calendar className="h-3 w-3" />
                            {activeWeek.examLabelAr}: {activeWeek.examDateAr}
                          </span>
                        </div>
                        <p className="text-[11px] leading-relaxed text-ink-deep">
                          <span className="font-bold text-midnight">الإنجيل: </span>
                          {activeWeek.bibleRangeAr}
                        </p>
                        <p className="text-[11px] leading-relaxed text-ink-deep">
                          <span className="font-bold text-burgundy">كتاب تجسد الكلمة: </span>
                          {activeWeek.incarnationBookAr}
                        </p>
                        {decodedQuiz.introText && (
                          <p className="rounded-xl bg-white/85 px-2.5 py-1.5 text-[11px] leading-relaxed text-ink-muted">
                            {decodedQuiz.introText}
                          </p>
                        )}
                        {(hasInAppQuestions || hasExternalQuizLink) && (
                          <div className="flex flex-wrap items-center gap-2 pt-0.5">
                            {hasInAppQuestions && (
                              <button
                                type="button"
                                onClick={() =>
                                  setActiveQuizModal({
                                    task,
                                    questions: decodedQuiz.questions,
                                    week: activeWeek,
                                    alreadySubmitted: isLocked,
                                  })
                                }
                                className="inline-flex h-10 flex-1 items-center justify-center gap-1.5 rounded-xl bg-burgundy px-3 text-xs font-bold text-white shadow-xs transition hover:opacity-95 active:scale-[0.98]"
                              >
                                <ClipboardCheck className="h-4 w-4 text-gold-bright" />
                                <span>
                                  ابدأ المسابقة داخل التطبيق ({decodedQuiz.questions.length} أسئلة)
                                </span>
                              </button>
                            )}
                            {hasExternalQuizLink && (
                              <button
                                type="button"
                                onClick={() => openSafeExternalUrl(appConfig.weeklyQuizUrl)}
                                className="inline-flex h-10 items-center justify-center gap-1.5 rounded-xl border border-burgundy/35 bg-white px-3 text-xs font-bold text-burgundy transition hover:bg-burgundy/5"
                              >
                                <ExternalLink className="h-3.5 w-3.5" />
                                <span>فتح رابط المسابقة</span>
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                    )}

                    {status === 'rejected' && existingSub?.adminNote?.trim() && (
                      <p className="mt-2 rounded-xl bg-status-rejectedBg px-3 py-1.5 text-xs font-medium text-burgundy">
                        ملاحظة الخادم: {existingSub.adminNote}
                      </p>
                    )}

                    <div className="mt-3 flex items-center justify-between gap-2">
                      {status ? (
                        <StatusBadge status={status} />
                      ) : (
                        <span className="text-[11px] text-ink-muted">
                          {isAfterEnd
                            ? 'انتهت فترة التحدي (5/1/2027)'
                            : isBeforeStart
                              ? 'اضغط للتجربة في الوضع التجريبي 🧪'
                              : 'اضغط لتسجيل إنجازك الآن'}
                        </span>
                      )}

                      <button
                        type="button"
                        disabled={isLocked}
                        onClick={(e) => {
                          e.stopPropagation();
                          if (!isLocked) openTaskSubmission(task);
                        }}
                        className={`h-11 shrink-0 rounded-2xl px-4 text-xs font-bold transition ${
                          isLocked
                            ? 'bg-parchment-variant text-ink-muted'
                            : 'bg-midnight text-white shadow-sm hover:bg-midnight-dark active:scale-95'
                        }`}
                      >
                        {status === 'approved'
                          ? 'تم الاعتماد'
                          : status === 'pending'
                            ? 'تم الإرسال'
                            : isAfterEnd
                              ? 'انتهى التحدي'
                              : status === 'rejected'
                                ? 'إعادة التسجيل'
                                : isBeforeStart
                                  ? 'تجربة التسجيل 🧪'
                                  : 'تسجيل الإنجاز'}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {/* Logout Footer */}
      <div className="flex justify-center pt-2">
        <button
          type="button"
          onClick={onLogout}
          className="inline-flex min-h-[44px] items-center gap-1.5 rounded-xl px-4 text-xs font-semibold text-ink-muted transition hover:bg-parchment-variant hover:text-ink-deep"
        >
          <LogOut className="h-4 w-4" />
          <span>تسجيل الخروج / التبديل لحساب آخر</span>
        </button>
      </div>
    </div>
  );
};
