import {
  type AppConfig,
  type ChallengeTask,
  type TaskSubmission,
  type YouthUser,
  normalizeFrequency,
  normalizeStatus,
} from '../types';

function sanitizeSupabaseUrl(raw: string): string {
  const cleaned = (raw || '')
    .trim()
    .replace(/^NEXT_PUBLIC_SUPABASE_URL=/i, '')
    .replace(/^VITE_SUPABASE_URL=/i, '')
    .replace(/^SUPABASE_URL=/i, '')
    .replace(/^['"\s]+|['"\s]+$/g, '')
    .replace(/[\r\n]/g, '');

  const matches = [...cleaned.matchAll(/https?:\/\/\s*([a-zA-Z0-9-]+\.supabase\.co)/gi)];
  if (matches.length > 0) {
    return `https://${matches[matches.length - 1][1]}`;
  }

  return cleaned
    .replace(/^https?:\/\/\s*https?:\/\//i, 'https://')
    .replace(/\s+/g, '')
    .replace(/\/+$/, '')
    .replace(/\/rest\/v1$/i, '');
}

function sanitizeAnonKey(raw: string): string {
  return (raw || '')
    .trim()
    .replace(/^NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY=/i, '')
    .replace(/^NEXT_PUBLIC_SUPABASE_ANON_KEY=/i, '')
    .replace(/^VITE_SUPABASE_ANON_KEY=/i, '')
    .replace(/^SUPABASE_ANON_KEY=/i, '')
    .replace(/^['"\s]+|['"\s]+$/g, '')
    .replace(/[\r\n]/g, '')
    .trim();
}

const rawUrl = import.meta.env.VITE_SUPABASE_URL || import.meta.env.SUPABASE_URL || '';
const rawKey = import.meta.env.VITE_SUPABASE_ANON_KEY || import.meta.env.SUPABASE_ANON_KEY || '';

class SupabaseRestClient {
  private readonly baseUrl: string = sanitizeSupabaseUrl(rawUrl);
  private readonly anonKey: string = sanitizeAnonKey(rawKey);

  get isConfigured(): boolean {
    return (
      this.baseUrl.startsWith('https://') &&
      !this.baseUrl.includes('YOUR_SUPABASE_URL') &&
      this.anonKey.length > 0 &&
      !this.anonKey.includes('YOUR_SUPABASE_ANON_KEY')
    );
  }

  private buildUrl(table: string, params: Record<string, string> = {}): string {
    const url = new URL(`${this.baseUrl}/rest/v1/${table}`);
    Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
    return url.toString();
  }

  private buildHeaders(prefer?: string): HeadersInit {
    const headers: Record<string, string> = {
      apikey: this.anonKey,
      'Content-Type': 'application/json',
    };
    if (this.anonKey.startsWith('eyJ')) {
      headers.Authorization = `Bearer ${this.anonKey}`;
    }
    if (prefer) {
      headers.Prefer = prefer;
    }
    return headers;
  }

  private formatHttpError(action: string, status: number, body: string): string {
    if (status === 404) {
      return `HTTP 404 (${action}): تأكد من تشغيل كود SQL لإنشاء الجداول في Supabase SQL Editor ثم أعد المحاولة`;
    }
    const detail = (body || '').slice(0, 120).trim();
    return detail ? `HTTP ${status} (${action}): ${detail}` : `HTTP ${status} (${action})`;
  }

  private async request(
    action: string,
    url: string,
    init: RequestInit
  ): Promise<Response> {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 15000);
    try {
      const resp = await fetch(url, { ...init, signal: controller.signal });
      if (!resp.ok) {
        const text = await resp.text().catch(() => '');
        throw new Error(this.formatHttpError(action, resp.status, text));
      }
      return resp;
    } finally {
      clearTimeout(timeout);
    }
  }

  async fetchUsers(): Promise<YouthUser[]> {
    const resp = await this.request(
      'users',
      this.buildUrl('users', { select: '*', order: 'created_at.asc' }),
      { method: 'GET', headers: this.buildHeaders() }
    );
    const arr = (await resp.json()) as Array<Record<string, unknown>>;
    return arr.map((o) => ({
      id: String(o.id),
      fullName: String(o.full_name ?? ''),
      phone: String(o.phone ?? ''),
      pinHash: String(o.pin_hash ?? ''),
      bonusPoints: Number(o.bonus_points ?? 0),
      createdAt: Number(o.created_at ?? Date.now()),
    }));
  }

  async upsertUser(user: YouthUser): Promise<void> {
    await this.request('upsert user', this.buildUrl('users'), {
      method: 'POST',
      headers: this.buildHeaders('resolution=merge-duplicates,return=minimal'),
      body: JSON.stringify({
        id: user.id,
        full_name: user.fullName,
        phone: user.phone,
        pin_hash: user.pinHash,
        bonus_points: user.bonusPoints,
        created_at: user.createdAt,
      }),
    });
  }

  async deleteUser(userId: string): Promise<void> {
    await this.request(
      'delete user',
      this.buildUrl('users', { id: `eq.${userId}` }),
      { method: 'DELETE', headers: this.buildHeaders('return=minimal') }
    );
  }

  async updateSubmissionUserName(userId: string, newName: string): Promise<void> {
    await this.request(
      'update submission name',
      this.buildUrl('submissions', { user_id: `eq.${userId}` }),
      {
        method: 'PATCH',
        headers: this.buildHeaders('return=minimal'),
        body: JSON.stringify({ user_name: newName }),
      }
    );
  }

  async fetchTasks(): Promise<ChallengeTask[]> {
    const resp = await this.request(
      'tasks',
      this.buildUrl('tasks', { select: '*', order: 'sort_order.asc' }),
      { method: 'GET', headers: this.buildHeaders() }
    );
    const arr = (await resp.json()) as Array<Record<string, unknown>>;
    return arr.map((o, i) => ({
      id: String(o.id),
      title: String(o.title ?? ''),
      description: String(o.description ?? ''),
      points: Number(o.points ?? 100),
      frequency: normalizeFrequency(String(o.frequency ?? 'daily')),
      iconKey: String(o.icon_key ?? 'star'),
      isActive: o.is_active !== false,
      sortOrder: Number(o.sort_order ?? i + 1),
    }));
  }

  async upsertTask(task: ChallengeTask): Promise<void> {
    await this.request('upsert task', this.buildUrl('tasks'), {
      method: 'POST',
      headers: this.buildHeaders('resolution=merge-duplicates,return=minimal'),
      body: JSON.stringify({
        id: task.id,
        title: task.title,
        description: task.description,
        points: task.points,
        frequency: task.frequency,
        icon_key: task.iconKey,
        is_active: task.isActive,
        sort_order: task.sortOrder,
      }),
    });
  }

  async fetchSubmissions(): Promise<TaskSubmission[]> {
    const resp = await this.request(
      'submissions',
      this.buildUrl('submissions', { select: '*', order: 'submitted_at.desc' }),
      { method: 'GET', headers: this.buildHeaders() }
    );
    const arr = (await resp.json()) as Array<Record<string, unknown>>;
    return arr.map((o) => ({
      id: String(o.id),
      userId: String(o.user_id ?? ''),
      userName: String(o.user_name ?? ''),
      taskId: String(o.task_id ?? ''),
      taskTitle: String(o.task_title ?? ''),
      points: Number(o.points ?? 0),
      periodKey: String(o.period_key ?? ''),
      submissionDate: String(o.submission_date ?? ''),
      note: String(o.note ?? ''),
      status: normalizeStatus(String(o.status ?? 'pending')),
      adminNote: String(o.admin_note ?? ''),
      submittedAt: Number(o.submitted_at ?? Date.now()),
      reviewedAt: o.reviewed_at == null ? null : Number(o.reviewed_at),
    }));
  }

  async upsertSubmission(submission: TaskSubmission): Promise<void> {
    await this.request('upsert submission', this.buildUrl('submissions'), {
      method: 'POST',
      headers: this.buildHeaders('resolution=merge-duplicates,return=minimal'),
      body: JSON.stringify({
        id: submission.id,
        user_id: submission.userId,
        user_name: submission.userName,
        task_id: submission.taskId,
        task_title: submission.taskTitle,
        points: submission.points,
        period_key: submission.periodKey,
        submission_date: submission.submissionDate,
        note: submission.note,
        status: submission.status,
        admin_note: submission.adminNote,
        submitted_at: submission.submittedAt,
        reviewed_at: submission.reviewedAt,
      }),
    });
  }

  async approveAllPendingRemote(reviewedAt: number): Promise<void> {
    await this.request(
      'approve all pending',
      this.buildUrl('submissions', { status: 'eq.pending' }),
      {
        method: 'PATCH',
        headers: this.buildHeaders('return=minimal'),
        body: JSON.stringify({
          status: 'approved',
          admin_note: 'تمت الموافقة الجماعية من الخادم',
          reviewed_at: reviewedAt,
        }),
      }
    );
  }

  async fetchAppConfig(): Promise<AppConfig | null> {
    const resp = await this.request(
      'app_config',
      this.buildUrl('app_config', { select: '*', id: 'eq.1' }),
      { method: 'GET', headers: this.buildHeaders() }
    );
    const arr = (await resp.json()) as Array<Record<string, unknown>>;
    if (!arr.length) return null;
    const o = arr[0];
    return {
      id: 1,
      weeklyQuizTitle: String(o.weekly_quiz_title ?? ''),
      weeklyQuizQuestion: String(o.weekly_quiz_question ?? ''),
      weeklyQuizUrl: String(o.weekly_quiz_url ?? ''),
      dailyVerseText: String(o.daily_verse_text ?? ''),
      dailyVerseRef: String(o.daily_verse_ref ?? ''),
      dailyEncouragement: String(o.daily_encouragement ?? ''),
    };
  }

  async upsertAppConfig(config: AppConfig): Promise<void> {
    await this.request('upsert app_config', this.buildUrl('app_config'), {
      method: 'POST',
      headers: this.buildHeaders('resolution=merge-duplicates,return=minimal'),
      body: JSON.stringify({
        id: 1,
        weekly_quiz_title: config.weeklyQuizTitle,
        weekly_quiz_question: config.weeklyQuizQuestion,
        weekly_quiz_url: config.weeklyQuizUrl,
        daily_verse_text: config.dailyVerseText,
        daily_verse_ref: config.dailyVerseRef,
        daily_encouragement: config.dailyEncouragement,
      }),
    });
  }
}

export const supabaseClient = new SupabaseRestClient();
