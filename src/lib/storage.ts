import {
  type AppConfig,
  type ChallengeTask,
  type ReminderSettings,
  type TaskSubmission,
  type YouthUser,
  DEFAULT_APP_CONFIG,
  DEFAULT_CHALLENGE_TASKS,
  DEFAULT_REMINDER_SETTINGS,
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
} as const;

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
    const stored = readJson<ChallengeTask[]>(KEYS.TASKS, []);
    return stored.length > 0 ? stored : DEFAULT_CHALLENGE_TASKS;
  },
  saveTasks(tasks: ChallengeTask[]): void {
    writeJson(KEYS.TASKS, tasks);
  },

  getSubmissions(): TaskSubmission[] {
    return readJson<TaskSubmission[]>(KEYS.SUBMISSIONS, []);
  },
  saveSubmissions(subs: TaskSubmission[]): void {
    writeJson(KEYS.SUBMISSIONS, subs);
  },

  getAppConfig(): AppConfig {
    return readJson<AppConfig>(KEYS.CONFIG, DEFAULT_APP_CONFIG);
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
};
