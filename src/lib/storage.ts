import {
  type AppConfig,
  type ChallengeTask,
  type ReminderSettings,
  type TaskSubmission,
  type YouthUser,
  ALL_DEFAULT_CHALLENGE_TASKS,
  DEFAULT_APP_CONFIG,
  DEFAULT_REMINDER_SETTINGS,
  mergeMissingAddonTasks,
} from '../types';
import { PinHasher } from './securityAndExport';

const KEYS = {
  USERS: 'tariq_users_v1',
  TASKS: 'tariq_tasks_v1',
  SUBMISSIONS: 'tariq_submissions_v1',
  CONFIG: 'tariq_config_v1',
  ACTIVE_USER_ID: 'tariq_active_user_id_v1',
  HAS_SEEN_WELCOME: 'tariq_has_seen_welcome_v1',
  ADMIN_PIN_HASH: 'tariq_admin_pin_hash_v1',
  REMINDERS: 'tariq_reminders_v1',
  INSTALL_BANNER_DISMISSED: 'tariq_install_banner_dismissed_v1',
  APPROVAL_SNAPSHOTS: 'tariq_approval_snapshots_v1',
  NOTIF_PROMPT_DISMISSED: 'tariq_notif_prompt_dismissed_v1',
  FIRED_REMINDERS: 'tariq_fired_reminders_v1',
} as const;

export interface ApprovalSnapshot {
  seenApprovedIds: string[];
  lastBonusPoints: number;
}

export const DEFAULT_ADMIN_PIN = '2026';

function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

function writeJson<T>(key: string, value: T): void {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // ignore quota errors
  }
}

export const localStore = {
  getUsers(): YouthUser[] {
    return readJson<YouthUser[]>(KEYS.USERS, []);
  },
  saveUsers(users: YouthUser[]): void {
    writeJson(KEYS.USERS, users);
  },

  getTasks(): ChallengeTask[] {
    const stored = readJson<ChallengeTask[]>(KEYS.TASKS, []).filter(
      (t) => t.id !== 'task_incarnation_reading'
    );
    return stored.length > 0 ? mergeMissingAddonTasks(stored) : ALL_DEFAULT_CHALLENGE_TASKS;
  },
  saveTasks(tasks: ChallengeTask[]): void {
    writeJson(
      KEYS.TASKS,
      tasks.filter((t) => t.id !== 'task_incarnation_reading')
    );
  },

  getSubmissions(): TaskSubmission[] {
    return readJson<TaskSubmission[]>(KEYS.SUBMISSIONS, []);
  },
  saveSubmissions(subs: TaskSubmission[]): void {
    writeJson(KEYS.SUBMISSIONS, subs);
  },

  getAppConfig(): AppConfig {
    const stored = readJson<Partial<AppConfig>>(KEYS.CONFIG, {});
    return {
      ...DEFAULT_APP_CONFIG,
      ...stored,
      activeWeek:
        typeof stored.activeWeek === 'number' && stored.activeWeek >= 1 && stored.activeWeek <= 5
          ? stored.activeWeek
          : DEFAULT_APP_CONFIG.activeWeek,
      enabledQuizWeeks: Array.isArray(stored.enabledQuizWeeks)
        ? stored.enabledQuizWeeks
        : DEFAULT_APP_CONFIG.enabledQuizWeeks,
      weeklyQuizQuestions: Array.isArray(stored.weeklyQuizQuestions)
        ? stored.weeklyQuizQuestions
        : [],
    };
  },
  saveAppConfig(config: AppConfig): void {
    writeJson(KEYS.CONFIG, config);
  },

  getActiveUserId(): string | null {
    try {
      const val = localStorage.getItem(KEYS.ACTIVE_USER_ID);
      return val && val.trim() ? val : null;
    } catch {
      return null;
    }
  },
  setActiveUserId(userId: string | null): void {
    try {
      if (!userId) localStorage.removeItem(KEYS.ACTIVE_USER_ID);
      else localStorage.setItem(KEYS.ACTIVE_USER_ID, userId);
    } catch {
      // ignore
    }
  },

  getHasSeenWelcome(): boolean {
    try {
      return localStorage.getItem(KEYS.HAS_SEEN_WELCOME) === 'true';
    } catch {
      return false;
    }
  },
  markWelcomeSeen(): void {
    try {
      localStorage.setItem(KEYS.HAS_SEEN_WELCOME, 'true');
    } catch {
      // ignore
    }
  },

  async verifyAdminPin(pin: string): Promise<boolean> {
    let storedHash: string | null = null;
    try {
      storedHash = localStorage.getItem(KEYS.ADMIN_PIN_HASH);
    } catch {
      // ignore
    }
    const expectedHash = storedHash || (await PinHasher.hashPin(DEFAULT_ADMIN_PIN));
    return PinHasher.matches(pin, expectedHash);
  },

  async updateAdminPin(newPin: string): Promise<void> {
    const hash = await PinHasher.hashPin(newPin);
    try {
      localStorage.setItem(KEYS.ADMIN_PIN_HASH, hash);
    } catch {
      // ignore
    }
  },

  getReminderSettings(): ReminderSettings {
    return {
      ...DEFAULT_REMINDER_SETTINGS,
      ...readJson<Partial<ReminderSettings>>(KEYS.REMINDERS, {}),
    };
  },
  saveReminderSettings(settings: ReminderSettings): void {
    writeJson(KEYS.REMINDERS, settings);
  },

  isInstallBannerDismissed(): boolean {
    try {
      return localStorage.getItem(KEYS.INSTALL_BANNER_DISMISSED) === 'true';
    } catch {
      return false;
    }
  },
  dismissInstallBanner(): void {
    try {
      localStorage.setItem(KEYS.INSTALL_BANNER_DISMISSED, 'true');
    } catch {
      // ignore
    }
  },

  getApprovalSnapshot(userId: string): ApprovalSnapshot | null {
    const map = readJson<Record<string, ApprovalSnapshot>>(KEYS.APPROVAL_SNAPSHOTS, {});
    return map[userId] ?? null;
  },
  saveApprovalSnapshot(userId: string, snapshot: ApprovalSnapshot): void {
    const map = readJson<Record<string, ApprovalSnapshot>>(KEYS.APPROVAL_SNAPSHOTS, {});
    map[userId] = snapshot;
    writeJson(KEYS.APPROVAL_SNAPSHOTS, map);
  },

  isNotifPromptDismissed(): boolean {
    try {
      return localStorage.getItem(KEYS.NOTIF_PROMPT_DISMISSED) === 'true';
    } catch {
      return false;
    }
  },
  dismissNotifPrompt(): void {
    try {
      localStorage.setItem(KEYS.NOTIF_PROMPT_DISMISSED, 'true');
    } catch {
      // ignore
    }
  },

  hasFiredReminderToday(dateIso: string, reminderKey: string): boolean {
    const map = readJson<Record<string, boolean>>(KEYS.FIRED_REMINDERS, {});
    return Boolean(map[`${dateIso}:${reminderKey}`]);
  },

  markReminderFiredToday(dateIso: string, reminderKey: string): void {
    const map = readJson<Record<string, boolean>>(KEYS.FIRED_REMINDERS, {});
    // Keep only today's entries to prevent unbounded growth
    const pruned: Record<string, boolean> = {};
    for (const [k, v] of Object.entries(map)) {
      if (k.startsWith(`${dateIso}:`) && v) {
        pruned[k] = true;
      }
    }
    pruned[`${dateIso}:${reminderKey}`] = true;
    writeJson(KEYS.FIRED_REMINDERS, pruned);
  },

  clearFiredReminderToday(dateIso: string, reminderKey: string): void {
    const map = readJson<Record<string, boolean>>(KEYS.FIRED_REMINDERS, {});
    const fullKey = `${dateIso}:${reminderKey}`;
    if (fullKey in map) {
      delete map[fullKey];
      writeJson(KEYS.FIRED_REMINDERS, map);
    }
  },
};
