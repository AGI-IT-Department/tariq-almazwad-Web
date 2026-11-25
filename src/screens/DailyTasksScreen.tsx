import React, { useMemo, useState } from 'react';
import {
  BellRing,
  BookOpen,
  CheckCircle2,
  Clock,
  ExternalLink,
  HelpCircle,
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
import { getTodayBibleReading } from '../lib/bibleReadingPlan';
import {
  UrlValidator,
  openSafeExternalUrl,
  shareOrCopyText,
} from '../lib/securityAndExport';
import {
  DailyVerseBanner,
  StatusBadge,
  TaskIcon,
  TaskSubmissionSheet,
} from '../components/SharedUi';
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
  { id: 'once', label: 'مرة في الصوم' },
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
  const [notifPermission, setNotifPermission] = useState<NotificationPermission>(() =>
    getNotificationPermission()
  );
  const [notifPromptDismissed, setNotifPromptDismissed] = useState<boolean>(() =>
    localStore.isNotifPromptDismissed()
  );

  const todayBibleReading = useMemo(
    () =>
      getTodayBibleReading(
        reminderSettings.bibleBookId,
        reminderSettings.bibleStartDateIso,
        today
      ),
    [reminderSettings.bibleBookId, reminderSettings.bibleStartDateIso, today]
  );

  const bibleTask = useMemo(
    () => allTasks.find((t) => t.id === 'task_bible_reading' && t.isActive) ?? null,
    [allTasks]
  );
  const bibleTaskSubToday = useMemo(() => {
    if (!bibleTask) return null;
    const pk = FastCalendar.periodKeyForTask(bibleTask.frequency, today);
    return userSubmissions.find((s) => s.taskId === bibleTask.id && s.periodKey === pk) ?? null;
  }, [bibleTask, userSubmissions, today]);
  const isBibleTaskLocked =
    isAfterEnd ||
    bibleTaskSubToday?.status === 'pending' ||
    bibleTaskSubToday?.status === 'approved';

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

  const activeTasks = useMemo(
    () => allTasks.filter((t) => t.isActive).sort((a, b) => a.sortOrder - b.sortOrder),
    [allTasks]
  );

  const completedNowCount = useMemo(() => {
    return activeTasks.filter((task) => {
      const periodKey = FastCalendar.periodKeyForTask(task.frequency, today);
      const sub = userSubmissions.find((s) => s.taskId === task.id && s.periodKey === periodKey);
      return sub && (sub.status === 'approved' || sub.status === 'pending');
    }).length;
  }, [activeTasks, userSubmissions, today]);

  const completionRatio = activeTasks.length === 0 ? 0 : completedNowCount / activeTasks.length;

  const filteredTasks = useMemo(() => {
    if (selectedFilter === 'all') return activeTasks;
    return activeTasks.filter((t) => t.frequency === selectedFilter);
  }, [activeTasks, selectedFilter]);

  // SVG Ring geometry
  const ringRadius = 32;
  const ringCircumference = 2 * Math.PI * ringRadius;
  const ringOffset = ringCircumference * (1 - completionRatio);

  const daySubtitle = isBeforeStart
    ? `وضع تجريبي 🧪 • التحدي الرسمي يبدأ خلال ${daysToStart} يوم (30/11)`
    : isAfterEnd
      ? 'انتهى التحدي يوم 5 يناير 2027 🎉'
      : `اليوم ${currentDay} من ${FastCalendar.FAST_DAYS_COUNT} • باقي ${daysToEnd} يوم على النهاية`;

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

      {/* Challenge Window / Trial Mode Banner */}
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
              ? 'جرّب تسجيل المهام الآن بحرية لتتعود على التطبيق! هذه النقاط تجريبية فقط، ومن يوم 30 نوفمبر 2026 تبدأ النتائج الرسمية من الصفر وتُحسب حتى 5 يناير 2027.'
              : 'انتهت فترة تسجيل المهام يوم 5 يناير 2027. يمكنك متابعة مركزك وسجلك بانتظار إعلان الفائزين الـ 10!'}
          </p>
        </section>
      )}

      {/* Weekly Quiz Banner */}
      {appConfig.weeklyQuizQuestion.trim() && (
        <section className="rounded-[22px] border border-burgundy/35 bg-burgundy/10 p-4">
          <div className="flex items-center gap-2 text-burgundy">
            <HelpCircle className="h-5 w-5 shrink-0" />
            <h3 className="text-base font-bold">
              {appConfig.weeklyQuizTitle.trim() || 'مسابقة نهاية الأسبوع'}
            </h3>
          </div>
          <p className="mt-2 text-sm leading-relaxed text-ink-deep">
            {appConfig.weeklyQuizQuestion}
          </p>
          {UrlValidator.isSafeHttpUrl(appConfig.weeklyQuizUrl) && (
            <button
              type="button"
              onClick={() => openSafeExternalUrl(appConfig.weeklyQuizUrl)}
              className="mt-3 inline-flex items-center gap-1.5 rounded-xl border border-burgundy px-3.5 py-2 text-xs font-bold text-burgundy transition hover:bg-burgundy hover:text-white"
            >
              <ExternalLink className="h-4 w-4" />
              <span>فتح رابط المسابقة الأسبوعية</span>
            </button>
          )}
        </section>
      )}

      {/* Daily Verse Banner */}
      <DailyVerseBanner
        verseText={appConfig.dailyVerseText}
        reference={appConfig.dailyVerseRef}
        encouragement={appConfig.dailyEncouragement}
        onShare={async (text) => {
          const msg = await shareOrCopyText(text, 'آية اليوم في طريق المزود');
          onFeedback(msg);
        }}
      />

      {/* Daily Bible Chapter & Personal Reminder Card */}
      <section className="rounded-[22px] border border-midnight/20 bg-white p-4 shadow-sm">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-midnight text-gold-bright shadow-sm">
              <BookOpen className="h-5 w-5" />
            </div>
            <div>
              <p className="text-xs font-bold text-gold-warm">ورد الكتاب المقدس اليوم 📖</p>
              <h3 className="mt-0.5 text-base font-extrabold text-ink-deep">
                {todayBibleReading.bookNameAr} — الإصحاح {todayBibleReading.chapterNumber}
              </h3>
              <div className="mt-1 flex flex-wrap items-center gap-2 text-[11px] text-ink-muted">
                <span className="inline-flex items-center gap-1">
                  <Clock className="h-3.5 w-3.5 text-midnight" />
                  القراءة: {formatTimeArabic(reminderSettings.bibleReadingHour, reminderSettings.bibleReadingMinute)}
                </span>
                <span>•</span>
                <span>
                  الإنجازات: {formatTimeArabic(reminderSettings.dailyTasksReminderHour, reminderSettings.dailyTasksReminderMinute)}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-3 flex flex-wrap items-center gap-2">
          {bibleTask && !isBibleTaskLocked && (
            <button
              type="button"
              onClick={() => {
                setSheetInitialNote(
                  `تمت قراءة ${todayBibleReading.bookNameAr} — الإصحاح ${todayBibleReading.chapterNumber}`
                );
                setSelectedTaskForSheet(bibleTask);
              }}
              className="flex-1 rounded-xl bg-midnight px-3 py-2.5 text-xs font-bold text-white transition hover:bg-midnight-dark active:scale-[0.99]"
            >
              سجّل قراءة الإصحاح (+{bibleTask.points} نقطة)
            </button>
          )}
          <button
            type="button"
            onClick={onOpenReminders}
            className="rounded-xl border border-midnight/25 px-3 py-2.5 text-xs font-bold text-midnight transition hover:bg-parchment-variant"
          >
            تغيير السفر والوقت ⏰
          </button>
        </div>
      </section>

      {/* Category Filter Chips */}
      <div className="space-y-2 pt-1">
        <h3 className="text-lg font-bold text-ink-deep">
          مهام طريق المزود ({filteredTasks.length})
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

      {/* Interactive Task Cards */}
      <div className="space-y-3">
        {filteredTasks.map((task) => {
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

          return (
            <div
              key={task.id}
              onClick={() => !isLocked && setSelectedTaskForSheet(task)}
              className={`rounded-[22px] border bg-white p-4 transition ${borderClass} ${
                isLocked ? 'shadow-xs' : 'cursor-pointer shadow-sm hover:shadow-md active:scale-[0.995]'
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
                <p className="mt-2.5 text-xs leading-relaxed text-ink-muted">{task.description}</p>
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
                    if (!isLocked) setSelectedTaskForSheet(task);
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
