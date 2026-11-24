import React, { useMemo, useState } from 'react';
import { History } from 'lucide-react';
import type { SubmissionStatus, TaskSubmission } from '../types';
import { FastCalendar, formatTimestampArabic } from '../lib/fastCalendar';
import { StatusBadge } from '../components/SharedUi';

interface MyLogScreenProps {
  submissions: TaskSubmission[];
}

type LogFilter = 'all' | SubmissionStatus;

const FILTERS: Array<{ id: LogFilter; label: string }> = [
  { id: 'all', label: 'الكل' },
  { id: 'approved', label: 'مقبول' },
  { id: 'pending', label: 'قيد المراجعة' },
  { id: 'rejected', label: 'مرفوض' },
];

export const MyLogScreen: React.FC<MyLogScreenProps> = ({ submissions }) => {
  const [filter, setFilter] = useState<LogFilter>('all');

  const approvedCount = useMemo(
    () => submissions.filter((s) => s.status === 'approved').length,
    [submissions]
  );
  const pendingCount = useMemo(
    () => submissions.filter((s) => s.status === 'pending').length,
    [submissions]
  );
  const rejectedCount = useMemo(
    () => submissions.filter((s) => s.status === 'rejected').length,
    [submissions]
  );

  const filtered = useMemo(() => {
    if (filter === 'all') return submissions;
    return submissions.filter((s) => s.status === filter);
  }, [submissions, filter]);

  return (
    <div className="mx-auto max-w-[640px] space-y-3.5 px-4 pt-3.5 pb-40">
      <div>
        <h2 className="text-xl font-bold text-ink-deep">سجلي الروحي</h2>
        <p className="mt-0.5 text-xs text-ink-muted">
          متابعة حالة كل المهام التي سجلتها وملاحظات الخادم
        </p>
      </div>

      <div className="grid grid-cols-3 gap-2">
        <StatChip label="مقبول" count={approvedCount} className="bg-status-approvedBg text-status-approved" />
        <StatChip label="قيد المراجعة" count={pendingCount} className="bg-status-pendingBg text-status-pending" />
        <StatChip label="مرفوض" count={rejectedCount} className="bg-status-rejectedBg text-status-rejected" />
      </div>

      <div className="no-scrollbar flex gap-2 overflow-x-auto pb-1">
        {FILTERS.map((f) => {
          const active = filter === f.id;
          return (
            <button
              key={f.id}
              type="button"
              onClick={() => setFilter(f.id)}
              className={`shrink-0 rounded-full border px-4 py-1.5 text-xs font-bold transition ${
                active
                  ? 'border-midnight bg-midnight text-white'
                  : 'border-parchment-outline bg-white text-ink-deep hover:bg-parchment-variant'
              }`}
            >
              {f.label}
            </button>
          );
        })}
      </div>

      {filtered.length === 0 ? (
        <div className="flex flex-col items-center rounded-[22px] border border-parchment-border bg-white px-6 py-10 text-center">
          <History className="h-11 w-11 text-gold-warm" />
          <h3 className="mt-2.5 text-base font-bold text-ink-deep">
            لا توجد مشاركات في هذا التصنيف
          </h3>
          <p className="mt-1 text-xs leading-relaxed text-ink-muted">
            سجّل مهامك اليومية من تبويب «اليوميات» وستظهر هنا مباشرة مع حالة الاعتماد.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((sub) => {
            const isTrial = FastCalendar.isTrialSubmission(sub);
            return (
              <div
                key={sub.id}
                className="space-y-2 rounded-[18px] border border-parchment-border bg-white p-4 shadow-xs"
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex min-w-0 flex-1 flex-wrap items-center gap-1.5">
                    <h4 className="text-sm font-bold text-ink-deep">{sub.taskTitle}</h4>
                    {isTrial && (
                      <span className="rounded-full bg-status-pendingBg px-2 py-0.5 text-[10px] font-bold text-status-pending">
                        تجريبي 🧪
                      </span>
                    )}
                  </div>
                  <StatusBadge status={sub.status} />
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-ink-muted">{formatTimestampArabic(sub.submittedAt)}</span>
                  {sub.points > 0 && (
                    <span className="font-bold text-gold-warm">+{sub.points} نقطة</span>
                  )}
                </div>

                {sub.note.trim() && (
                  <p className="text-xs text-ink-deep">ملاحظتك: {sub.note}</p>
                )}

                {sub.adminNote.trim() && (
                  <div
                    className={`rounded-xl px-3 py-2 text-xs font-medium ${
                      sub.status === 'rejected'
                        ? 'bg-status-rejectedBg text-status-rejected'
                        : 'bg-parchment-variant/70 text-midnight'
                    }`}
                  >
                    ملاحظة الخادم: {sub.adminNote}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

const StatChip: React.FC<{ label: string; count: number; className: string }> = ({
  label,
  count,
  className,
}) => (
  <div className={`flex flex-col items-center rounded-2xl px-2 py-2.5 ${className}`}>
    <span className="text-xl font-extrabold leading-tight">{count}</span>
    <span className="text-[11px] font-semibold">{label}</span>
  </div>
);
