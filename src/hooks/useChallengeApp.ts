import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  type AppConfig,
  type ChallengeTask,
  type LeaderboardEntry,
  type ReminderSettings,
  type TaskSubmission,
  type YouthTab,
  type YouthUser,
  STATUS_LABELS,
} from '../types';
import { FastCalendar, todayFastDate, toIsoString } from '../lib/fastCalendar';
import {
  CsvExporter,
  PinHasher,
  UrlValidator,
  generateUuid,
} from '../lib/securityAndExport';
import { localStore } from '../lib/storage';
import { supabaseClient } from '../lib/supabaseClient';

export interface ToastMessage {
  id: number;
  text: string;
}

export function computeLeaderboard(
  users: YouthUser[],
  submissions: TaskSubmission[]
): LeaderboardEntry[] {
  const today = todayFastDate();
  const isTrialNow = FastCalendar.isTrialMode(today);
  const counted = isTrialNow
    ? submissions
    : submissions.filter((s) => FastCalendar.isOfficialSubmission(s));

  const grouped = new Map<string, TaskSubmission[]>();
  for (const sub of counted) {
    const arr = grouped.get(sub.userId);
    if (arr) arr.push(sub);
    else grouped.set(sub.userId, [sub]);
  }

  const unsorted: LeaderboardEntry[] = users.map((user) => {
    const userSubs = grouped.get(user.id) ?? [];
    const approvedSubs = userSubs.filter((s) => s.status === 'approved');
    const pendingSubs = userSubs.filter((s) => s.status === 'pending');
    const approvedTaskPoints = approvedSubs.reduce((sum, s) => sum + s.points, 0);
    const totalApprovedPoints = Math.max(approvedTaskPoints + user.bonusPoints, 0);
    return {
      rank: 0,
      user,
      approvedTaskPoints,
      totalApprovedPoints,
      pendingPoints: pendingSubs.reduce((sum, s) => sum + s.points, 0),
      approvedTasksCount: approvedSubs.filter((s) => s.points > 0).length,
      pendingTasksCount: pendingSubs.length,
    };
  });

  unsorted.sort((a, b) => {
    if (b.totalApprovedPoints !== a.totalApprovedPoints) {
      return b.totalApprovedPoints - a.totalApprovedPoints;
    }
    if (b.approvedTasksCount !== a.approvedTasksCount) {
      return b.approvedTasksCount - a.approvedTasksCount;
    }
    return a.user.fullName.localeCompare(b.user.fullName, 'ar');
  });

  return unsorted.map((entry, idx) => ({ ...entry, rank: idx + 1 }));
}

export function useChallengeApp() {
  const isSupabaseLive = supabaseClient.isConfigured;

  const [isLoading, setIsLoading] = useState(true);
  const [isSyncing, setIsSyncing] = useState(false);
  const [users, setUsers] = useState<YouthUser[]>(() => localStore.getUsers());
  const [tasks, setTasks] = useState<ChallengeTask[]>(() => localStore.getTasks());
  const [submissions, setSubmissions] = useState<TaskSubmission[]>(() => localStore.getSubmissions());
  const [appConfig, setAppConfig] = useState<AppConfig>(() => localStore.getAppConfig());
  const [activeUserId, setActiveUserId] = useState<string | null>(() => localStore.getActiveUserId());
  // Always show WelcomeScreen on every app launch before Login / Dashboard
  const [hasSeenWelcome, setHasSeenWelcome] = useState<boolean>(false);
  const [reminderSettings, setReminderSettings] = useState<ReminderSettings>(() =>
    localStore.getReminderSettings()
  );

  const [selectedTab, setSelectedTab] = useState<YouthTab>('daily');
  const [isAdminOpen, setIsAdminOpen] = useState(false);
  const [isAdminUnlocked, setIsAdminUnlocked] = useState(false);
  const [lastSyncError, setLastSyncError] = useState<string | null>(null);
  const [toast, setToast] = useState<ToastMessage | null>(null);
  const toastTimerRef = useRef<number | null>(null);

  const showToast = useCallback((text: string) => {
    if (toastTimerRef.current) window.clearTimeout(toastTimerRef.current);
    setToast({ id: Date.now(), text });
    toastTimerRef.current = window.setTimeout(() => {
      setToast(null);
    }, 3600);
  }, []);

  const syncFromSupabase = useCallback(async (silent = true): Promise<boolean> => {
    if (!supabaseClient.isConfigured) return true;
    try {
      const [remoteTasks, remoteUsers, remoteSubs, remoteConfig] = await Promise.all([
        supabaseClient.fetchTasks(),
        supabaseClient.fetchUsers(),
        supabaseClient.fetchSubmissions(),
        supabaseClient.fetchAppConfig().catch(() => null),
      ]);

      if (remoteTasks.length > 0) {
        setTasks(remoteTasks);
        localStore.saveTasks(remoteTasks);
      }
      setUsers(remoteUsers);
      localStore.saveUsers(remoteUsers);

      setSubmissions(remoteSubs);
      localStore.saveSubmissions(remoteSubs);

      if (remoteConfig) {
        setAppConfig(remoteConfig);
        localStore.saveAppConfig(remoteConfig);
      }
      setLastSyncError(null);
      return true;
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'تعذر الاتصال بالخادم';
      setLastSyncError(msg);
      if (!silent) {
        showToast(`تعذر التحديث: ${msg}`);
      }
      return false;
    }
  }, [showToast]);

  // Initial load + 20s polling when Supabase is configured
  useEffect(() => {
    let mounted = true;
    (async () => {
      setIsSyncing(true);
      await syncFromSupabase(true);
      if (mounted) {
        setIsSyncing(false);
        setIsLoading(false);
      }
    })();

    if (!supabaseClient.isConfigured) return () => { mounted = false; };
    const interval = window.setInterval(() => {
      void syncFromSupabase(true);
    }, 20000);
    return () => {
      mounted = false;
      window.clearInterval(interval);
    };
  }, [syncFromSupabase]);

  const refreshNow = useCallback(async () => {
    setIsSyncing(true);
    const ok = await syncFromSupabase(false);
    setIsSyncing(false);
    if (ok && supabaseClient.isConfigured) {
      showToast('تم تحديث البيانات من Supabase');
    }
  }, [syncFromSupabase, showToast]);

  const activeUser = useMemo(
    () => users.find((u) => u.id === activeUserId) ?? null,
    [users, activeUserId]
  );

  const leaderboard = useMemo(
    () => computeLeaderboard(users, submissions),
    [users, submissions]
  );

  const activeUserRankEntry = useMemo(
    () => (activeUser ? leaderboard.find((e) => e.user.id === activeUser.id) ?? null : null),
    [activeUser, leaderboard]
  );

  const userSubmissions = useMemo(
    () =>
      activeUser
        ? submissions
            .filter((s) => s.userId === activeUser.id)
            .sort((a, b) => b.submittedAt - a.submittedAt)
        : [],
    [activeUser, submissions]
  );

  const pendingSubmissions = useMemo(() => {
    const isTrialNow = FastCalendar.isTrialMode();
    return submissions
      .filter(
        (s) =>
          s.status === 'pending' &&
          (isTrialNow || FastCalendar.isOfficialSubmission(s))
      )
      .sort((a, b) => a.submittedAt - b.submittedAt);
  }, [submissions]);

  const completeWelcome = useCallback(() => {
    setHasSeenWelcome(true);
  }, []);

  const loginOrRegister = useCallback(
    async (fullName: string, pin: string, phone: string) => {
      const cleanName = fullName.trim();
      const cleanPin = pin.trim();
      const cleanPhone = phone.trim();

      if (cleanName.length < 3) {
        showToast('يرجى كتابة الاسم بالكامل (3 أحرف على الأقل)');
        return;
      }
      if (!/^\d{4}$/.test(cleanPin)) {
        showToast('الرقم السري (PIN) يجب أن يتكون من 4 أرقام فقط');
        return;
      }

      setIsSyncing(true);
      let currentUsers = users;
      if (supabaseClient.isConfigured) {
        try {
          const remoteUsers = await supabaseClient.fetchUsers();
          currentUsers = remoteUsers;
          setUsers(remoteUsers);
          localStore.saveUsers(remoteUsers);
        } catch {
          // continue with cached users
        }
      }

      const existing = currentUsers.find(
        (u) => u.fullName.trim().toLowerCase() === cleanName.toLowerCase()
      );

      if (existing) {
        const matched = await PinHasher.matches(cleanPin, existing.pinHash);
        setIsSyncing(false);
        if (matched) {
          localStore.setActiveUserId(existing.id);
          setActiveUserId(existing.id);
          showToast(`مرحباً بعودتك يا ${existing.fullName}!`);
          if (supabaseClient.isConfigured) void syncFromSupabase(true);
        } else {
          showToast('الرقم السري غير صحيح لهذا الاسم. تواصل مع الخادم لإعادة تعيينه.');
        }
      } else {
        const pinHash = await PinHasher.hashPin(cleanPin);
        const newUser: YouthUser = {
          id: generateUuid(),
          fullName: cleanName,
          phone: cleanPhone,
          pinHash,
          bonusPoints: 0,
          createdAt: Date.now(),
        };
        if (supabaseClient.isConfigured) {
          try {
            await supabaseClient.upsertUser(newUser);
          } catch (err) {
            setIsSyncing(false);
            showToast(`تعذر الاتصال بقاعدة البيانات: ${err instanceof Error ? err.message : ''}`);
            return;
          }
        }
        const nextUsers = [...currentUsers, newUser];
        setUsers(nextUsers);
        localStore.saveUsers(nextUsers);
        localStore.setActiveUserId(newUser.id);
        setActiveUserId(newUser.id);
        setIsSyncing(false);
        showToast(`تم تسجيلك بنجاح يا ${cleanName}! أهلاً بك في تحدي طريق المزود`);
      }
    },
    [users, showToast, syncFromSupabase]
  );

  const logoutYouth = useCallback(() => {
    localStore.setActiveUserId(null);
    setActiveUserId(null);
    setSelectedTab('daily');
    showToast('تم تسجيل الخروج بنجاح');
  }, [showToast]);

  const submitTask = useCallback(
    async (task: ChallengeTask, note: string) => {
      if (!activeUser) return;
      const today = todayFastDate();
      if (FastCalendar.isAfterEnd(today)) {
        showToast('انتهت فترة التحدي يوم 5 يناير 2027 — تم إغلاق تسجيل المهام');
        return;
      }

      const isTrial = FastCalendar.isTrialMode(today);
      const periodKey = FastCalendar.periodKeyForTask(task.frequency, today);
      const existing = submissions.find(
        (s) => s.userId === activeUser.id && s.taskId === task.id && s.periodKey === periodKey
      );

      if (existing && existing.status !== 'rejected') {
        showToast(`تم تسجيل «${task.title}» بالفعل لهذه الفترة (${STATUS_LABELS[existing.status]})`);
        return;
      }

      const submission: TaskSubmission = {
        id: existing?.status === 'rejected' ? existing.id : generateUuid(),
        userId: activeUser.id,
        userName: activeUser.fullName,
        taskId: task.id,
        taskTitle: task.title,
        points: task.points,
        periodKey,
        submissionDate: toIsoString(today),
        note: note.trim(),
        status: 'pending',
        adminNote: '',
        submittedAt: Date.now(),
        reviewedAt: null,
      };

      if (supabaseClient.isConfigured) {
        try {
          await supabaseClient.upsertSubmission(submission);
        } catch (err) {
          showToast(`تعذر إرسال المشاركة لـ Supabase: ${err instanceof Error ? err.message : ''}`);
          return;
        }
      }

      const nextSubs = [
        submission,
        ...submissions.filter((s) => s.id !== submission.id),
      ];
      setSubmissions(nextSubs);
      localStore.saveSubmissions(nextSubs);

      showToast(
        isTrial
          ? `تم إرسال «${task.title}» (وضع تجريبي 🧪 — النتيجة الرسمية تبدأ 30/11/2026)`
          : `تم إرسال «${task.title}» للمراجعة ⏳ (+${task.points} نقطة بعد موافقة الخادم)`
      );
    },
    [activeUser, submissions, showToast]
  );

  // Admin actions
  const openAdmin = useCallback(() => setIsAdminOpen(true), []);
  const closeAdmin = useCallback(() => setIsAdminOpen(false), []);
  const lockAdmin = useCallback(() => {
    setIsAdminUnlocked(false);
    setIsAdminOpen(false);
  }, []);

  const unlockAdmin = useCallback(
    async (pin: string): Promise<boolean> => {
      const ok = await localStore.verifyAdminPin(pin);
      if (ok) {
        setIsAdminUnlocked(true);
        showToast('أهلاً بك في لوحة تحكم الخادم - مينا');
        if (supabaseClient.isConfigured) void refreshNow();
        return true;
      }
      return false;
    },
    [showToast, refreshNow]
  );

  const reviewSubmission = useCallback(
    async (submissionId: string, approve: boolean, adminNote = '') => {
      const existing = submissions.find((s) => s.id === submissionId);
      if (!existing) {
        showToast('المشاركة غير موجودة');
        return;
      }
      const updated: TaskSubmission = {
        ...existing,
        status: approve ? 'approved' : 'rejected',
        adminNote:
          adminNote.trim() ||
          (approve ? 'أحسنت! ربنا يبارك تعبك' : 'يرجى مراجعة المهمة وإعادة الإرسال'),
        reviewedAt: Date.now(),
      };

      if (supabaseClient.isConfigured) {
        try {
          await supabaseClient.upsertSubmission(updated);
        } catch {
          showToast('تعذر تحديث حالة المشاركة في Supabase');
          return;
        }
      }

      const nextSubs = submissions.map((s) => (s.id === submissionId ? updated : s));
      setSubmissions(nextSubs);
      localStore.saveSubmissions(nextSubs);
      showToast(approve ? `تم الاعتماد وإضافة ${updated.points} نقطة!` : 'تم رفض المشاركة');
    },
    [submissions, showToast]
  );

  const approveAllPending = useCallback(async () => {
    const now = Date.now();
    if (supabaseClient.isConfigured) {
      try {
        await supabaseClient.approveAllPendingRemote(now);
      } catch {
        showToast('تعذر تنفيذ الموافقة الجماعية على Supabase');
        return;
      }
    }
    const nextSubs = submissions.map((s) =>
      s.status === 'pending'
        ? {
            ...s,
            status: 'approved' as const,
            adminNote: 'تمت الموافقة الجماعية من الخادم',
            reviewedAt: now,
          }
        : s
    );
    setSubmissions(nextSubs);
    localStore.saveSubmissions(nextSubs);
    showToast('تمت الموافقة على جميع الطلبات المعلقة وإضافة النقاط!');
  }, [submissions, showToast]);

  const adjustUserBonus = useCallback(
    async (userId: string, delta: number, reason: string) => {
      if (delta === 0) {
        showToast('يرجى إدخال عدد نقاط أكبر أو أقل من صفر');
        return;
      }
      const user = users.find((u) => u.id === userId);
      if (!user) {
        showToast('المستخدم غير موجود');
        return;
      }
      const updatedUser: YouthUser = {
        ...user,
        bonusPoints: Math.max(user.bonusPoints + delta, -5000),
      };
      if (supabaseClient.isConfigured) {
        try {
          await supabaseClient.upsertUser(updatedUser);
        } catch {
          showToast('تعذر تعديل النقاط على Supabase');
          return;
        }
      }
      const nextUsers = users.map((u) => (u.id === userId ? updatedUser : u));
      setUsers(nextUsers);
      localStore.saveUsers(nextUsers);

      const now = Date.now();
      const noteText =
        reason.trim() ||
        (delta >= 0 ? 'نقاط مكافأة إضافية من الخادم' : 'خصم نقاط من الخادم');
      const adjustmentRecord: TaskSubmission = {
        id: generateUuid(),
        userId: user.id,
        userName: user.fullName,
        taskId: 'task_weekly_quiz',
        taskTitle:
          delta >= 0 ? `مكافأة من الخادم (+${delta})` : `تعديل نقاط من الخادم (${delta})`,
        points: 0,
        periodKey: `BONUS_${now}`,
        submissionDate: toIsoString(todayFastDate()),
        note: noteText,
        status: 'approved',
        adminNote: `تعديل يدوي بواسطة الخادم (${delta} نقطة)`,
        submittedAt: now,
        reviewedAt: now,
      };
      if (supabaseClient.isConfigured) {
        supabaseClient.upsertSubmission(adjustmentRecord).catch(() => {});
      }
      const nextSubs = [adjustmentRecord, ...submissions];
      setSubmissions(nextSubs);
      localStore.saveSubmissions(nextSubs);
      showToast(`تم تحديث نقاط ${user.fullName} بمقدار ${delta >= 0 ? `+${delta}` : delta} نقطة`);
    },
    [users, submissions, showToast]
  );

  const adminAddUser = useCallback(
    async (fullName: string, phone: string, pin: string, initialBonus: number) => {
      const cleanName = fullName.trim();
      const cleanPin = pin.trim();
      const cleanPhone = phone.trim();
      if (cleanName.length < 3) {
        showToast('يرجى إدخال الاسم بالكامل (3 أحرف على الأقل)');
        return;
      }
      if (!/^\d{4}$/.test(cleanPin)) {
        showToast('الرقم السري (PIN) يجب أن يتكون من 4 أرقام');
        return;
      }
      if (users.some((u) => u.fullName.trim().toLowerCase() === cleanName.toLowerCase())) {
        showToast('يوجد مشارك مسجل بنفس الاسم بالفعل');
        return;
      }
      const pinHash = await PinHasher.hashPin(cleanPin);
      const newUser: YouthUser = {
        id: generateUuid(),
        fullName: cleanName,
        phone: cleanPhone,
        pinHash,
        bonusPoints: initialBonus,
        createdAt: Date.now(),
      };
      if (supabaseClient.isConfigured) {
        try {
          await supabaseClient.upsertUser(newUser);
        } catch (err) {
          showToast(`تعذر إضافة المشارك على Supabase: ${err instanceof Error ? err.message : ''}`);
          return;
        }
      }
      const nextUsers = [...users, newUser];
      setUsers(nextUsers);
      localStore.saveUsers(nextUsers);
      showToast(`تمت إضافة «${cleanName}» إلى تحدي طريق المزود بنجاح`);
    },
    [users, showToast]
  );

  const adminUpdateUser = useCallback(
    async (userId: string, fullName: string, phone: string, newPinOptional: string) => {
      const cleanName = fullName.trim();
      const cleanPhone = phone.trim();
      const cleanPin = newPinOptional.trim();
      if (cleanName.length < 3) {
        showToast('الاسم بالكامل يجب أن يكون 3 أحرف على الأقل');
        return;
      }
      if (cleanPin && !/^\d{4}$/.test(cleanPin)) {
        showToast('الرقم السري الجديد يجب أن يكون 4 أرقام');
        return;
      }
      const existing = users.find((u) => u.id === userId);
      if (!existing) {
        showToast('المستخدم غير موجود');
        return;
      }
      const updated: YouthUser = {
        ...existing,
        fullName: cleanName,
        phone: cleanPhone,
        pinHash: cleanPin ? await PinHasher.hashPin(cleanPin) : existing.pinHash,
      };
      if (supabaseClient.isConfigured) {
        try {
          await supabaseClient.upsertUser(updated);
          if (existing.fullName !== cleanName) {
            await supabaseClient.updateSubmissionUserName(userId, cleanName);
          }
        } catch (err) {
          showToast(`تعذر تحديث البيانات على Supabase: ${err instanceof Error ? err.message : ''}`);
          return;
        }
      }
      const nextUsers = users.map((u) => (u.id === userId ? updated : u));
      setUsers(nextUsers);
      localStore.saveUsers(nextUsers);
      if (existing.fullName !== cleanName) {
        const nextSubs = submissions.map((s) =>
          s.userId === userId ? { ...s, userName: cleanName } : s
        );
        setSubmissions(nextSubs);
        localStore.saveSubmissions(nextSubs);
      }
      showToast(`تم تحديث بيانات «${cleanName}» بنجاح`);
    },
    [users, submissions, showToast]
  );

  const adminDeleteUser = useCallback(
    async (userId: string) => {
      const existing = users.find((u) => u.id === userId);
      if (!existing) {
        showToast('المستخدم غير موجود');
        return;
      }
      if (supabaseClient.isConfigured) {
        try {
          await supabaseClient.deleteUser(userId);
        } catch (err) {
          showToast(`تعذر حذف المشارك من Supabase: ${err instanceof Error ? err.message : ''}`);
          return;
        }
      }
      const nextUsers = users.filter((u) => u.id !== userId);
      const nextSubs = submissions.filter((s) => s.userId !== userId);
      setUsers(nextUsers);
      setSubmissions(nextSubs);
      localStore.saveUsers(nextUsers);
      localStore.saveSubmissions(nextSubs);
      if (activeUserId === userId) {
        localStore.setActiveUserId(null);
        setActiveUserId(null);
      }
      showToast(`تم حذف المشارك «${existing.fullName}» وجميع مشاركاته`);
    },
    [users, submissions, activeUserId, showToast]
  );

  const updateTask = useCallback(
    async (task: ChallengeTask) => {
      if (supabaseClient.isConfigured) {
        try {
          await supabaseClient.upsertTask(task);
        } catch {
          showToast('تعذر حفظ المهمة على Supabase');
          return;
        }
      }
      const nextTasks = tasks.map((t) => (t.id === task.id ? task : t));
      setTasks(nextTasks);
      localStore.saveTasks(nextTasks);
      showToast(`تم حفظ تحديثات المهمة «${task.title}»`);
    },
    [tasks, showToast]
  );

  const updateAppConfig = useCallback(
    async (config: AppConfig) => {
      if (config.weeklyQuizUrl.trim() && !UrlValidator.isSafeHttpUrl(config.weeklyQuizUrl)) {
        showToast('رابط المسابقة يجب أن يبدأ بـ https:// أو http://');
        return;
      }
      if (supabaseClient.isConfigured) {
        try {
          await supabaseClient.upsertAppConfig(config);
        } catch {
          showToast('تعذر حفظ الإعدادات على Supabase');
          return;
        }
      }
      setAppConfig(config);
      localStore.saveAppConfig(config);
      showToast('تم تحديث المسابقة الأسبوعية وآية اليوم بنجاح');
    },
    [showToast]
  );

  const changeAdminPin = useCallback(
    async (currentPin: string, newPin: string) => {
      if (newPin.trim().length < 4) {
        showToast('الرمز السري يجب أن يكون 4 أرقام على الأقل');
        return;
      }
      const ok = await localStore.verifyAdminPin(currentPin);
      if (!ok) {
        showToast('الرمز السري الحالي غير صحيح');
        return;
      }
      await localStore.updateAdminPin(newPin.trim());
      showToast('تم تغيير رمز دخول الأدمن بنجاح');
    },
    [showToast]
  );

  const exportLeaderboardCsv = useCallback(() => {
    if (leaderboard.length === 0) {
      showToast('لا يوجد مشاركون لتصدير التقرير بعد');
      return;
    }
    try {
      const msg = CsvExporter.downloadLeaderboardCsv(leaderboard);
      showToast(msg);
    } catch {
      showToast('تعذر تصدير ملف التقرير');
    }
  }, [leaderboard, showToast]);

  const updateReminders = useCallback(
    (settings: ReminderSettings) => {
      setReminderSettings(settings);
      localStore.saveReminderSettings(settings);
      showToast('تم حفظ مواعيد التنبيهات اليومية');
    },
    [showToast]
  );

  const sendTestEncouragement = useCallback(async () => {
    const title = 'تحدي طريق المزود ✨';
    const body = `${appConfig.dailyVerseText} (${appConfig.dailyVerseRef}) — ${appConfig.dailyEncouragement}`;

    if (typeof window !== 'undefined' && 'Notification' in window) {
      if (Notification.permission === 'default') {
        await Notification.requestPermission().catch(() => 'denied');
      }
      if (Notification.permission === 'granted') {
        try {
          new Notification(title, {
            body,
            icon: '/icon-192.png',
            dir: 'rtl',
            lang: 'ar',
          });
          showToast('تم إرسال إشعار تشجيعي الآن!');
          return;
        } catch {
          // Some mobile browsers require SW notification; fallback to toast below
        }
      }
    }
    showToast(`${title} ${appConfig.dailyVerseText}`);
  }, [appConfig, showToast]);

  const testSupabaseConnection = useCallback(async () => {
    const res = await supabaseClient.testConnection();
    if (res.ok) {
      setLastSyncError(null);
      await syncFromSupabase(true);
    } else {
      setLastSyncError(res.message);
    }
    showToast(res.message);
    return res;
  }, [showToast, syncFromSupabase]);

  return {
    isLoading,
    isSyncing,
    isSupabaseLive,
    supabaseHost: supabaseClient.projectHost,
    lastSyncError,
    activeUser,
    allUsers: users,
    allTasks: tasks,
    allSubmissions: submissions,
    userSubmissions,
    pendingSubmissions,
    leaderboard,
    activeUserRankEntry,
    appConfig,
    reminderSettings,
    selectedTab,
    isAdminOpen,
    isAdminUnlocked,
    hasSeenWelcome,
    toast,
    showToast,
    setSelectedTab,
    completeWelcome,
    refreshNow,
    loginOrRegister,
    logoutYouth,
    submitTask,
    openAdmin,
    closeAdmin,
    unlockAdmin,
    lockAdmin,
    reviewSubmission,
    approveAllPending,
    adjustUserBonus,
    adminAddUser,
    adminUpdateUser,
    adminDeleteUser,
    updateTask,
    updateAppConfig,
    changeAdminPin,
    exportLeaderboardCsv,
    updateReminders,
    sendTestEncouragement,
    testSupabaseConnection,
  };
}
