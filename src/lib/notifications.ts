import type {
  ChallengeTask,
  ReminderSettings,
  TaskSubmission,
  YouthUser,
} from '../types';
import { FastCalendar, todayFastDate, toIsoString } from './fastCalendar';
import {
  formatBibleReminderNotification,
  getTodayBibleReading,
} from './bibleReadingPlan';
import { getTodayAutoDevotion } from './autoContent';
import { localStore } from './storage';

export interface PointsApprovalNotice {
  title: string;
  body: string;
  toastText: string;
  addedPoints: number;
  tag: string;
}

export function isNotificationSupported(): boolean {
  return typeof window !== 'undefined' && 'Notification' in window;
}

export function getNotificationPermission(): NotificationPermission {
  if (!isNotificationSupported()) return 'denied';
  return Notification.permission;
}

export async function requestBrowserNotificationPermission(): Promise<NotificationPermission> {
  if (!isNotificationSupported()) return 'denied';
  try {
    return await Notification.requestPermission();
  } catch {
    return 'denied';
  }
}

export async function showLocalOrSwNotification(
  title: string,
  body: string,
  tag = 'tariq-almazwad-notif'
): Promise<boolean> {
  if (!isNotificationSupported() || Notification.permission !== 'granted') {
    return false;
  }

  // Prefer ServiceWorkerRegistration.showNotification (required on mobile Chrome & iOS installed PWAs)
  if (typeof navigator !== 'undefined' && 'serviceWorker' in navigator) {
    try {
      const reg = await navigator.serviceWorker.ready;
      if (reg && typeof reg.showNotification === 'function') {
        await reg.showNotification(title, {
          body,
          icon: '/icon-192.png',
          badge: '/icon-192.png',
          dir: 'rtl',
          lang: 'ar',
          tag,
        });
        return true;
      }
    } catch {
      // Fall through to Window Notification constructor
    }
  }

  try {
    new Notification(title, {
      body,
      icon: '/icon-192.png',
      dir: 'rtl',
      lang: 'ar',
      tag,
    });
    return true;
  } catch {
    return false;
  }
}

export function detectNewApprovedPoints(
  activeUserId: string | null,
  users: YouthUser[],
  submissions: TaskSubmission[]
): PointsApprovalNotice | null {
  if (!activeUserId) return null;
  const user = users.find((u) => u.id === activeUserId);
  if (!user) return null;

  const userApproved = submissions.filter(
    (s) => s.userId === activeUserId && s.status === 'approved'
  );
  const currentApprovedIds = userApproved.map((s) => s.id);
  const prev = localStore.getApprovalSnapshot(activeUserId);

  // First time tracking this user on this device: record baseline without firing historical alerts
  if (!prev) {
    localStore.saveApprovalSnapshot(activeUserId, {
      seenApprovedIds: currentApprovedIds,
      lastBonusPoints: user.bonusPoints,
    });
    return null;
  }

  const seenSet = new Set(prev.seenApprovedIds);
  const newlyApprovedTasks = userApproved.filter(
    (s) => !seenSet.has(s.id) && s.points > 0
  );
  const taskPointsAdded = newlyApprovedTasks.reduce((sum, s) => sum + s.points, 0);
  const bonusDelta = Math.max(user.bonusPoints - prev.lastBonusPoints, 0);
  const totalAddedPoints = taskPointsAdded + bonusDelta;

  // Always persist latest snapshot so approvals never notify twice
  localStore.saveApprovalSnapshot(activeUserId, {
    seenApprovedIds: currentApprovedIds,
    lastBonusPoints: user.bonusPoints,
  });

  if (totalAddedPoints <= 0) {
    return null;
  }

  const today = todayFastDate();
  const isTrialNow = FastCalendar.isTrialMode(today);
  const countedApproved = isTrialNow
    ? userApproved
    : userApproved.filter((s) => FastCalendar.isOfficialSubmission(s));
  const approvedTaskPoints = countedApproved.reduce((sum, s) => sum + s.points, 0);
  const totalPointsNow = Math.max(approvedTaskPoints + user.bonusPoints, 0);

  let title: string;
  let body: string;

  if (newlyApprovedTasks.length === 1 && bonusDelta === 0) {
    const task = newlyApprovedTasks[0];
    title = 'مبروك! تم اعتماد نقاطك ✨';
    body = `تم اعتماد «${task.taskTitle}» وإضافة +${task.points} نقطة لرصيدك (الإجمالي: ${totalPointsNow} نقطة)`;
  } else if (newlyApprovedTasks.length > 1 && bonusDelta === 0) {
    title = `مبروك! تم اعتماد ${newlyApprovedTasks.length} مهام جديدة 🎉`;
    body = `تمت إضافة +${taskPointsAdded} نقطة جديدة إلى رصيدك (الإجمالي: ${totalPointsNow} نقطة)`;
  } else if (newlyApprovedTasks.length === 0 && bonusDelta > 0) {
    title = 'مكافأة نقاط جديدة من خدام التحدي 🎁';
    body = `تمت إضافة +${bonusDelta} نقطة بونص إلى رصيدك! (الإجمالي: ${totalPointsNow} نقطة)`;
  } else {
    title = 'مبروك! نقاط جديدة معتمدة 🎉';
    body = `تمت إضافة +${totalAddedPoints} نقطة جديدة إلى رصيدك في تحدي طريق المزود (الإجمالي: ${totalPointsNow} نقطة)`;
  }

  return {
    title,
    body,
    toastText: `🎉 ${body}`,
    addedPoints: totalAddedPoints,
    tag: `points-approved-${Date.now()}`,
  };
}

export interface ScheduledReminderFired {
  title: string;
  body: string;
  tag: string;
}

/**
 * Checks if any user-scheduled daily reminders (Bible chapter, daily achievements, prayers)
 * have reached their chosen local time today and have not yet fired today.
 */
export async function checkAndFireScheduledReminders(
  settings: ReminderSettings,
  activeUserId: string | null,
  tasks: ChallengeTask[],
  submissions: TaskSubmission[],
  onFired?: (notice: ScheduledReminderFired) => void
): Promise<void> {
  if (!activeUserId) return;

  const now = new Date();
  const nowMinutes = now.getHours() * 60 + now.getMinutes();
  const today = todayFastDate();
  const todayIso = toIsoString(today);

  const fireOnce = async (
    key: string,
    hour: number,
    minute: number,
    build: () => { title: string; body: string }
  ) => {
    const targetMinutes = hour * 60 + minute;
    if (nowMinutes < targetMinutes) return;
    if (localStore.hasFiredReminderToday(todayIso, key)) return;

    localStore.markReminderFiredToday(todayIso, key);
    const { title, body } = build();
    const tag = `tariq-${key}-${todayIso}`;
    await showLocalOrSwNotification(title, body, tag);
    onFired?.({ title, body, tag });
  };

  // 1. Daily Bible Reading Chapter Reminder (chosen book + daily chapter progression)
  if (settings.bibleReadingReminderEnabled) {
    await fireOnce(
      'bible_chapter',
      settings.bibleReadingHour,
      settings.bibleReadingMinute,
      () => {
        const reading = getTodayBibleReading(
          settings.bibleBookId,
          settings.bibleStartDateIso,
          today
        );
        return formatBibleReminderNotification(reading);
      }
    );
  }

  // 2. Daily Achievements Reminder at user-chosen time
  if (settings.dailyTasksReminderEnabled) {
    await fireOnce(
      'daily_tasks',
      settings.dailyTasksReminderHour,
      settings.dailyTasksReminderMinute,
      () => {
        const dailyTasks = tasks.filter((t) => t.isActive && t.frequency === 'daily');
        const periodKey = FastCalendar.periodKeyForTask('daily', today);
        const remainingCount = dailyTasks.filter((t) => {
          const sub = submissions.find(
            (s) =>
              s.userId === activeUserId &&
              s.taskId === t.id &&
              s.periodKey === periodKey &&
              (s.status === 'approved' || s.status === 'pending')
          );
          return !sub;
        }).length;

        if (remainingCount > 0) {
          return {
            title: 'تذكير بإنجازاتك اليومية ✨',
            body: `متبقي لك ${remainingCount} من المهام اليومية اليوم في تحدي طريق المزود! خصص دقائق الآن لإنجازها وتسجيل نقاطك.`,
          };
        }
        return {
          title: 'أحسنت! أتممت إنجازاتك اليومية 🎉',
          body: 'رائع! سجلت جميع مهامك اليومية اليوم. تابع مركزك واستعد لورد الغد في تحدي طريق المزود.',
        };
      }
    );
  }

  // 3. Morning prayer reminder
  if (settings.morningPrayerEnabled) {
    await fireOnce(
      'morning_prayer',
      settings.morningPrayerHour,
      settings.morningPrayerMinute,
      () => ({
        title: 'صباح الخير • صلاة باكر ☀️',
        body: 'ابدأ يومك في صوم الميلاد بصلاة باكر وسجّل مشاركتك في تحدي طريق المزود!',
      })
    );
  }

  // 4. Night prayer reminder
  if (settings.nightPrayerEnabled) {
    await fireOnce(
      'night_prayer',
      settings.nightPrayerHour,
      settings.nightPrayerMinute,
      () => ({
        title: 'تذكير صلاة النوم والأجبية 🌙',
        body: 'قبل ما تنام، اختم يومك بصلاة النوم وسجّل إنجازاتك اليومية للمراجعة.',
      })
    );
  }

  // 5. Daily encouragement reminder (automatic verse + Coptic Orthodox Church Father quote)
  if (settings.dailyEncouragementEnabled) {
    await fireOnce(
      'daily_encouragement',
      settings.dailyEncouragementHour,
      settings.dailyEncouragementMinute,
      () => {
        const devotion = getTodayAutoDevotion(today);
        return {
          title: `آية اليوم • ${devotion.verseRef} ✨`,
          body: `${devotion.verseText} — ${devotion.fatherQuote} (${devotion.fatherName})`,
        };
      }
    );
  }
}
