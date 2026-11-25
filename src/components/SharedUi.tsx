import React, { useState } from 'react';
import {
  AlertCircle,
  BookOpen,
  CheckCircle2,
  Church,
  Flame,
  Heart,
  HelpCircle,
  Hourglass,
  Music,
  Share2,
  ShieldCheck,
  Sparkles,
  Star,
  Users,
  X,
} from 'lucide-react';
import type { ChallengeTask, SubmissionStatus } from '../types';
import { FastCalendar, todayFastDate } from '../lib/fastCalendar';
import type { ToastMessage } from '../hooks/useChallengeApp';

export const TaskIcon: React.FC<{ iconKey: string; className?: string }> = ({
  iconKey,
  className = 'h-6 w-6',
}) => {
  switch (iconKey) {
    case 'book':
      return <BookOpen className={className} />;
    case 'quiz':
      return <HelpCircle className={className} />;
    case 'confession':
      return <ShieldCheck className={className} />;
    case 'church':
      return <Church className={className} />;
    case 'music':
      return <Music className={className} />;
    case 'groups':
      return <Users className={className} />;
    case 'prayer':
      return <Sparkles className={className} />;
    case 'heart':
      return <Heart className={className} />;
    case 'flame':
      return <Flame className={className} />;
    default:
      return <Star className={className} />;
  }
};

export const StatusBadge: React.FC<{ status: SubmissionStatus }> = ({ status }) => {
  if (status === 'approved') {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-status-approvedBg px-2.5 py-1 text-xs font-semibold text-status-approved">
        <CheckCircle2 className="h-3.5 w-3.5" />
        <span>تم الاعتماد ✅</span>
      </span>
    );
  }
  if (status === 'rejected') {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-status-rejectedBg px-2.5 py-1 text-xs font-semibold text-status-rejected">
        <AlertCircle className="h-3.5 w-3.5" />
        <span>مرفوض ❌</span>
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-status-pendingBg px-2.5 py-1 text-xs font-semibold text-status-pending">
      <Hourglass className="h-3.5 w-3.5" />
      <span>قيد المراجعة ⏳</span>
    </span>
  );
};

interface DailyVerseBannerProps {
  verseText: string;
  reference: string;
  encouragement: string;
  onShare: (text: string) => void;
}

export const DailyVerseBanner: React.FC<DailyVerseBannerProps> = ({
  verseText,
  reference,
  encouragement,
  onShare,
}) => {
  if (!verseText.trim()) return null;
  return (
    <section className="rounded-[20px] border border-gold-warm/50 bg-white p-4 shadow-sm">
      <div className="mb-1.5 flex items-center justify-between">
        <span className="text-sm font-bold text-gold-warm">آية اليوم في طريق المزود</span>
        <button
          type="button"
          onClick={() =>
            onShare(
              `${verseText}\n(${reference})\n${encouragement}\n— تحدي طريق المزود • اجتماع أبو سيفين للشباب الخريجين`
            )
          }
          aria-label="مشاركة الآية"
          className="flex h-9 w-9 items-center justify-center rounded-full text-midnight transition hover:bg-parchment-variant active:scale-95"
        >
          <Share2 className="h-4 w-4" />
        </button>
      </div>
      <p className="font-amiri text-center text-xl font-bold leading-relaxed text-ink-deep">
        {verseText}
      </p>
      <p className="mt-1 text-center text-xs font-semibold text-burgundy">— {reference}</p>
      {encouragement.trim() && (
        <p className="mt-1.5 text-center text-xs leading-relaxed text-ink-muted">
          {encouragement}
        </p>
      )}
    </section>
  );
};

interface TaskSubmissionSheetProps {
  task: ChallengeTask;
  initialNote?: string;
  onDismiss: () => void;
  onConfirmSubmit: (note: string) => void;
}

export const TaskSubmissionSheet: React.FC<TaskSubmissionSheetProps> = ({
  task,
  initialNote = '',
  onDismiss,
  onConfirmSubmit,
}) => {
  const [note, setNote] = useState(initialNote);
  const periodLabel = FastCalendar.periodLabelForTask(task.frequency, todayFastDate());

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 backdrop-blur-[2px]"
      onClick={onDismiss}
    >
      <div
        className="w-full max-w-[640px] rounded-t-3xl bg-white px-5 pt-4 pb-8 shadow-2xl animate-slide-up safe-pb"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mx-auto mb-3 h-1.5 w-12 rounded-full bg-parchment-outline" />
        <div className="mb-3 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-midnight/10 text-midnight">
              <TaskIcon iconKey={task.iconKey} className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-ink-deep">{task.title}</h3>
              <p className="text-xs font-semibold text-gold-warm">
                {periodLabel} • +{task.points} نقطة بعد الاعتماد
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onDismiss}
            aria-label="إغلاق"
            className="flex h-9 w-9 items-center justify-center rounded-full text-ink-muted hover:bg-parchment-variant"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {task.description.trim() && (
          <p className="mb-3 text-xs leading-relaxed text-ink-muted">{task.description}</p>
        )}

        <label className="block text-xs font-semibold text-ink-deep mb-1.5">
          ملاحظة قصيرة للخادم (اختياري)
        </label>
        <textarea
          rows={3}
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="مثال: قرأت إنجيل لوقا الأصحاح الأول"
          className="w-full rounded-xl border border-parchment-outline bg-parchment/40 p-3 text-sm text-ink-deep outline-none transition focus:border-midnight focus:ring-2 focus:ring-midnight/15"
        />

        <div className="mt-3 rounded-xl bg-status-pendingBg px-3 py-2.5 text-xs leading-relaxed text-midnight-dark">
          سيتم إرسال الإنجاز بحالة «قيد المراجعة ⏳» وستُضاف {task.points} نقطة لرصيدك فور اعتماد الخادم.
        </div>

        <button
          type="button"
          onClick={() => onConfirmSubmit(note)}
          className="mt-4 flex h-[52px] w-full items-center justify-center rounded-2xl bg-midnight text-base font-bold text-white shadow-md transition hover:bg-midnight-dark active:scale-[0.99]"
        >
          تأكيد وإرسال الإنجاز (+{task.points} نقطة)
        </button>
      </div>
    </div>
  );
};

export const ToastHost: React.FC<{ toast: ToastMessage | null }> = ({ toast }) => {
  if (!toast) return null;
  return (
    <div className="fixed top-4 inset-x-0 z-50 flex justify-center px-4 pointer-events-none safe-pt">
      <div
        key={toast.id}
        className="pointer-events-auto max-w-[520px] rounded-2xl border border-gold-warm/60 bg-midnight-dark px-4 py-3 text-center text-sm font-semibold text-white shadow-xl animate-slide-up"
      >
        {toast.text}
      </div>
    </div>
  );
};

interface ConfirmLogoutModalProps {
  userName: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ConfirmLogoutModal: React.FC<ConfirmLogoutModalProps> = ({
  userName,
  onConfirm,
  onCancel,
}) => {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-[2px]"
      onClick={onCancel}
    >
      <div
        className="w-full max-w-md rounded-3xl bg-white p-5 shadow-2xl animate-slide-up"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-lg font-bold text-ink-deep">تسجيل الخروج</h3>
        <p className="mt-2 text-sm leading-relaxed text-ink-muted">
          هل تريد تسجيل الخروج من حساب «{userName}»؟ يمكنك تسجيل الدخول مرة أخرى في أي وقت بنفس الاسم والرقم السري.
        </p>
        <div className="mt-5 flex items-center justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-xl px-4 py-2.5 text-sm font-semibold text-ink-muted hover:bg-parchment-variant"
          >
            إلغاء
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="rounded-xl bg-burgundy px-4 py-2.5 text-sm font-bold text-white shadow-sm hover:opacity-95"
          >
            تسجيل الخروج
          </button>
        </div>
      </div>
    </div>
  );
};
