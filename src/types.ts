import type { QuizQuestion } from './lib/readingPlan';

export type TaskFrequency = 'daily' | 'weekly' | 'once';

export type SubmissionStatus = 'pending' | 'approved' | 'rejected';

export type YouthTab = 'daily' | 'plan' | 'my_log' | 'my_rank';

export interface FastDate {
  year: number;
  month: number; // 1..12
  day: number;   // 1..31
}

export interface YouthUser {
  id: string;
  fullName: string;
  phone: string;
  pinHash: string;
  bonusPoints: number;
  createdAt: number;
}

export interface ChallengeTask {
  id: string;
  title: string;
  description: string;
  points: number;
  frequency: TaskFrequency;
  iconKey: string;
  isActive: boolean;
  sortOrder: number;
}

export interface TaskSubmission {
  id: string;
  userId: string;
  userName: string;
  taskId: string;
  taskTitle: string;
  points: number;
  periodKey: string;      // e.g. "2026-11-30" | "WEEK_1" | "FAST_2026" | "TRIAL_..."
  submissionDate: string; // "yyyy-MM-dd"
  note: string;
  status: SubmissionStatus;
  adminNote: string;
  submittedAt: number;
  reviewedAt: number | null;
}

export interface AppConfig {
  id: number;
  activeWeek: number;
  weeklyQuizTitle: string;
  weeklyQuizQuestion: string;
  weeklyQuizUrl: string;
  weeklyQuizQuestions: QuizQuestion[];
  dailyVerseText: string;
  dailyVerseRef: string;
  dailyEncouragement: string;
}

export interface ReminderSettings {
  pointsApprovalEnabled: boolean;
  dailyTasksReminderEnabled: boolean;
  dailyTasksReminderHour: number;
  dailyTasksReminderMinute: number;
  bibleReadingReminderEnabled: boolean;
  bibleBookId: string;
  bibleReadingHour: number;
  bibleReadingMinute: number;
  bibleStartDateIso: string;
  morningPrayerEnabled: boolean;
  morningPrayerHour: number;
  morningPrayerMinute: number;
  nightPrayerEnabled: boolean;
  nightPrayerHour: number;
  nightPrayerMinute: number;
  liturgyEnabled: boolean;
  liturgyDayOfWeek: number;
  liturgyHour: number;
  liturgyMinute: number;
  meetingEnabled: boolean;
  meetingDayOfWeek: number;
  meetingHour: number;
  meetingMinute: number;
  dailyEncouragementEnabled: boolean;
  dailyEncouragementHour: number;
  dailyEncouragementMinute: number;
}

export interface LeaderboardEntry {
  rank: number;
  user: YouthUser;
  approvedTaskPoints: number;
  totalApprovedPoints: number; // approvedTaskPoints + user.bonusPoints
  pendingPoints: number;
  approvedTasksCount: number;
  pendingTasksCount: number;
}

export const FREQUENCY_LABELS: Record<TaskFrequency, string> = {
  daily: 'يومي',
  weekly: 'أسبوعي',
  once: 'مرة خلال الصوم',
};

export const STATUS_LABELS: Record<SubmissionStatus, string> = {
  pending: 'قيد المراجعة',
  approved: 'مقبول',
  rejected: 'مرفوض',
};

export const DEFAULT_APP_CONFIG: AppConfig = {
  id: 1,
  activeWeek: 1,
  weeklyQuizTitle: 'مسابقة الأسبوع الأول • بشارة الميلاد',
  weeklyQuizQuestion:
    'اقرأ أصحاح 1 و 2 من إنجيل معلمنا لوقا وشارك في المسابقة الأسبوعية للحصول على 200 نقطة!',
  weeklyQuizUrl: '',
  weeklyQuizQuestions: [],
  dailyVerseText: '«المجد لله في الأعالي، وعلى الأرض السلام، وبالناس المسرة»',
  dailyVerseRef: 'إنجيل لوقا 2: 14',
  dailyEncouragement:
    'كل خطوة في طريق المزود تقربنا لملك السلام. سجّل إنجازاتك اليومية ليعتمدها الخادم!',
};

export const DEFAULT_REMINDER_SETTINGS: ReminderSettings = {
  pointsApprovalEnabled: true,
  dailyTasksReminderEnabled: true,
  dailyTasksReminderHour: 20,
  dailyTasksReminderMinute: 0,
  bibleReadingReminderEnabled: true,
  bibleBookId: 'luke',
  bibleReadingHour: 8,
  bibleReadingMinute: 0,
  bibleStartDateIso: '',
  morningPrayerEnabled: true,
  morningPrayerHour: 7,
  morningPrayerMinute: 30,
  nightPrayerEnabled: true,
  nightPrayerHour: 22,
  nightPrayerMinute: 0,
  liturgyEnabled: true,
  liturgyDayOfWeek: 6, // Friday
  liturgyHour: 7,
  liturgyMinute: 0,
  meetingEnabled: true,
  meetingDayOfWeek: 5, // Thursday
  meetingHour: 18,
  meetingMinute: 0,
  dailyEncouragementEnabled: true,
  dailyEncouragementHour: 14,
  dailyEncouragementMinute: 0,
};

export const DEFAULT_CHALLENGE_TASKS: ChallengeTask[] = [
  {
    id: 'task_bible_reading',
    title: 'قراءة الإنجيل اليومية',
    description: 'قراءة أصحاح يومي من الكتاب المقدس بتأمل',
    points: 100,
    frequency: 'daily',
    iconKey: 'book',
    isActive: true,
    sortOrder: 1,
  },
  {
    id: 'task_weekly_quiz',
    title: 'مسابقة نهاية الأسبوع',
    description: 'المشاركة والإجابة في المسابقة الأسبوعية للاجتماع',
    points: 200,
    frequency: 'weekly',
    iconKey: 'quiz',
    isActive: true,
    sortOrder: 2,
  },
  {
    id: 'task_confession',
    title: 'سر الاعتراف',
    description: 'ممارسة سر التوبة والاعتراف خلال صوم الميلاد',
    points: 200,
    frequency: 'once',
    iconKey: 'confession',
    isActive: true,
    sortOrder: 3,
  },
  {
    id: 'task_liturgy',
    title: 'حضور القداس والتناول',
    description: 'حضور القداس الإلهي والتناول من الأسرار المقدسة',
    points: 100,
    frequency: 'weekly',
    iconKey: 'church',
    isActive: true,
    sortOrder: 4,
  },
  {
    id: 'task_kiahk_tasbeha',
    title: 'حضور تسبحة كيهك',
    description: 'المشاركة في سهرة التسبحة الكيهكية الأسبوعية',
    points: 150,
    frequency: 'weekly',
    iconKey: 'music',
    isActive: true,
    sortOrder: 5,
  },
  {
    id: 'task_meeting',
    title: 'حضور الاجتماع أو التلمذة',
    description: 'حضور اجتماع أبو سيفين للشباب الخريجين أو لقاء التلمذة',
    points: 100,
    frequency: 'weekly',
    iconKey: 'groups',
    isActive: true,
    sortOrder: 6,
  },
  {
    id: 'task_agpeya',
    title: 'صلاة الأجبية (ساعة على الأقل)',
    description: 'صلاة ساعة على الأقل يومياً من كتاب الأجبية المقدسة',
    points: 100,
    frequency: 'daily',
    iconKey: 'prayer',
    isActive: true,
    sortOrder: 7,
  },
  {
    id: 'task_jesus_prayer',
    title: 'صلاة يسوع',
    description: 'ترديد صلاة يسوع «يا ربي يسوع المسيح ارحمني أنا الخاطئ» بتركيز',
    points: 100,
    frequency: 'daily',
    iconKey: 'heart',
    isActive: true,
    sortOrder: 8,
  },
  {
    id: 'task_fasting_metanoias',
    title: 'الصوم الانقطاعي والميطانيات',
    description: 'الالتزام بالصوم الانقطاعي حسب إرشاد أب الاعتراف مع الميطانيات',
    points: 100,
    frequency: 'daily',
    iconKey: 'flame',
    isActive: true,
    sortOrder: 9,
  },
];

export const ADDON_WEEKLY_TASKS: ChallengeTask[] = [
  {
    id: 'task_incarnation_book',
    title: 'قراءة كتاب "في تجسد الكلمة" الأسبوعية',
    description:
      'قراءة الجزء المقرر أسبوعياً من كتاب "في تجسد الكلمة" للقديس أثناسيوس الرسولي حسب منهج الأسبوع (200 نقطة أسبوعياً)',
    points: 200,
    frequency: 'weekly',
    iconKey: 'book',
    isActive: true,
    sortOrder: 10,
  },
  {
    id: 'task_weekly_exam_incarnation',
    title: 'المسابقة الأسبوعية (إنجيل + كتاب تجسد الكلمة)',
    description:
      'المسابقة الأسبوعية كل أحد وتشمل قراءات الكتاب المقدس وفصول كتاب "في تجسد الكلمة" المقررة للأسبوع (200 نقطة أسبوعياً)',
    points: 200,
    frequency: 'weekly',
    iconKey: 'quiz',
    isActive: true,
    sortOrder: 11,
  },
];

export const ALL_DEFAULT_CHALLENGE_TASKS: ChallengeTask[] = [
  ...DEFAULT_CHALLENGE_TASKS,
  ...ADDON_WEEKLY_TASKS,
];

export function mergeMissingAddonTasks(existing: ChallengeTask[]): ChallengeTask[] {
  const existingIds = new Set(existing.map((t) => t.id));
  const missing = ADDON_WEEKLY_TASKS.filter((t) => !existingIds.has(t.id));
  if (missing.length === 0) return existing;
  return [...existing, ...missing].sort((a, b) => a.sortOrder - b.sortOrder);
}

export function normalizeFrequency(key: string): TaskFrequency {
  const lower = (key || '').toLowerCase();
  if (lower === 'weekly') return 'weekly';
  if (lower === 'once') return 'once';
  return 'daily';
}

export function normalizeStatus(key: string): SubmissionStatus {
  const lower = (key || '').toLowerCase();
  if (lower === 'approved') return 'approved';
  if (lower === 'rejected') return 'rejected';
  return 'pending';
}
