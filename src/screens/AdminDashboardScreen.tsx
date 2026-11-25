import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  Award,
  CheckCheck,
  CheckCircle2,
  Download,
  Edit3,
  Gift,
  Lock,
  MessageCircle,
  PlusCircle,
  Search,
  ShieldCheck,
  Trash2,
  UserPlus,
  X,
  XCircle,
} from 'lucide-react';
import {
  type AppConfig,
  type ChallengeTask,
  type LeaderboardEntry,
  type TaskSubmission,
  FREQUENCY_LABELS,
} from '../types';
import { FastCalendar, formatTimestampArabic } from '../lib/fastCalendar';
import { openWhatsAppChat } from '../lib/securityAndExport';

interface AdminDashboardScreenProps {
  isUnlocked: boolean;
  pendingSubmissions: TaskSubmission[];
  leaderboard: LeaderboardEntry[];
  allUsersCount: number;
  approvedSubmissionsCount: number;
  allTasks: ChallengeTask[];
  appConfig: AppConfig;
  onUnlock: (pin: string) => Promise<{ ok: boolean; error?: string }>;
  onClose: () => void;
  onLock: () => void;
  onReviewSubmission: (id: string, approve: boolean, note?: string) => void;
  onApproveAllPending: () => void;
  onAdjustBonus: (userId: string, delta: number, reason: string) => void;
  onAdminAddUser: (fullName: string, phone: string, pin: string, initialBonus: number) => void;
  onAdminUpdateUser: (userId: string, fullName: string, phone: string, newPinOptional: string) => void;
  onAdminDeleteUser: (userId: string) => void;
  onUpdateTask: (task: ChallengeTask) => void;
  onUpdateConfig: (config: AppConfig) => void;
  onExportCsv: () => void;
  onChangeAdminPin: (currentPin: string, newPin: string) => Promise<boolean>;
  onFeedback: (msg: string) => void;
}

export const AdminDashboardScreen: React.FC<AdminDashboardScreenProps> = ({
  isUnlocked,
  pendingSubmissions,
  leaderboard,
  allUsersCount,
  approvedSubmissionsCount,
  allTasks,
  appConfig,
  onUnlock,
  onClose,
  onLock,
  onReviewSubmission,
  onApproveAllPending,
  onAdjustBonus,
  onAdminAddUser,
  onAdminUpdateUser,
  onAdminDeleteUser,
  onUpdateTask,
  onUpdateConfig,
  onExportCsv,
  onChangeAdminPin,
  onFeedback,
}) => {
  const [selectedTab, setSelectedTab] = useState(0);

  // Dialog states
  const [showAddUser, setShowAddUser] = useState(false);
  const [editingUser, setEditingUser] = useState<LeaderboardEntry | null>(null);
  const [deletingUser, setDeletingUser] = useState<LeaderboardEntry | null>(null);
  const [rejectingSub, setRejectingSub] = useState<TaskSubmission | null>(null);
  const [bonusTarget, setBonusTarget] = useState<LeaderboardEntry | null>(null);
  const [editingTask, setEditingTask] = useState<ChallengeTask | null>(null);

  if (!isUnlocked) {
    return <AdminPinGate onUnlock={onUnlock} onBack={onClose} />;
  }

  const tabs = [
    `طلبات الاعتماد (${pendingSubmissions.length})`,
    `لوحة الأبطال والشباب (${allUsersCount})`,
    'المهام والمسابقة',
    'التصدير والإعدادات',
  ];

  return (
    <div className="min-h-[100dvh] bg-parchment pb-28">
      {/* Admin Header */}
      <header className="sticky top-0 z-30 bg-midnight text-white shadow-md safe-pt">
        <div className="mx-auto flex max-w-[640px] items-center justify-between gap-2 px-3 pb-3">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              aria-label="رجوع"
              className="flex h-10 w-10 items-center justify-center rounded-full text-white transition hover:bg-white/10"
            >
              <ArrowRight className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-base font-bold text-gold-bright">لوحة تحكم خدام التحدي</h1>
              <p className="text-[11px] text-white/85">
                لوحة الأبطال الحصرية وإدارة الشباب والموافقات
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onLock}
            className="inline-flex h-10 items-center gap-1.5 rounded-xl px-3 text-xs font-bold text-white transition hover:bg-white/10"
          >
            <Lock className="h-4 w-4 text-gold-bright" />
            <span>قفل</span>
          </button>
        </div>

        {/* 4 Scrollable Admin Tabs */}
        <div className="bg-white text-midnight border-b border-parchment-border">
          <div className="no-scrollbar mx-auto flex max-w-[640px] gap-1 overflow-x-auto px-3">
            {tabs.map((title, idx) => {
              const active = selectedTab === idx;
              return (
                <button
                  key={title}
                  type="button"
                  onClick={() => setSelectedTab(idx)}
                  className={`relative shrink-0 px-3 py-3 text-xs font-bold transition ${
                    active ? 'text-midnight' : 'text-ink-muted hover:text-midnight'
                  }`}
                >
                  {title}
                  {active && (
                    <span className="absolute inset-x-2 bottom-0 h-[3px] rounded-t-full bg-midnight" />
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-[640px] p-4">
        {selectedTab === 0 && (
          <PendingQueueTab
            pending={pendingSubmissions}
            onApprove={(sub) => onReviewSubmission(sub.id, true, '')}
            onReject={(sub) => setRejectingSub(sub)}
            onApproveAll={onApproveAllPending}
          />
        )}
        {selectedTab === 1 && (
          <LeaderboardUsersTab
            entries={leaderboard}
            onAddUserClick={() => setShowAddUser(true)}
            onBonusClick={(e) => setBonusTarget(e)}
            onEditUserClick={(e) => setEditingUser(e)}
            onDeleteUserClick={(e) => setDeletingUser(e)}
            onNoPhoneFeedback={() =>
              onFeedback('لا يوجد رقم واتساب مسجل لهذا المشارك — اضغط «تعديل» لإضافته')
            }
          />
        )}
        {selectedTab === 2 && (
          <TasksAndQuizTab
            tasks={allTasks}
            config={appConfig}
            onToggleTask={(task, active) => onUpdateTask({ ...task, isActive: active })}
            onEditTask={(task) => setEditingTask(task)}
            onSaveConfig={onUpdateConfig}
          />
        )}
        {selectedTab === 3 && (
          <ExportAndSetupTab
            usersCount={allUsersCount}
            approvedCount={approvedSubmissionsCount}
            onExportCsv={onExportCsv}
            onChangeAdminPin={onChangeAdminPin}
          />
        )}
      </main>

      {/* Modals */}
      {showAddUser && (
        <AddUserModal
          onClose={() => setShowAddUser(false)}
          onConfirm={(name, phone, pin, bonus) => {
            onAdminAddUser(name, phone, pin, bonus);
            setShowAddUser(false);
          }}
        />
      )}
      {editingUser && (
        <EditUserModal
          entry={editingUser}
          onClose={() => setEditingUser(null)}
          onConfirm={(name, phone, newPin) => {
            onAdminUpdateUser(editingUser.user.id, name, phone, newPin);
            setEditingUser(null);
          }}
        />
      )}
      {deletingUser && (
        <ModalFrame title="حذف المشارك نهائياً" onClose={() => setDeletingUser(null)}>
          <p className="text-sm leading-relaxed text-ink-muted">
            هل أنت متأكد من حذف «{deletingUser.user.fullName}» وجميع مشاركاته ونقاطه (
            {deletingUser.totalApprovedPoints} نقطة)؟ لا يمكن التراجع عن هذا الإجراء.
          </p>
          <div className="mt-5 flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setDeletingUser(null)}
              className="rounded-xl px-4 py-2 text-sm font-semibold text-ink-muted hover:bg-parchment-variant"
            >
              إلغاء
            </button>
            <button
              type="button"
              onClick={() => {
                onAdminDeleteUser(deletingUser.user.id);
                setDeletingUser(null);
              }}
              className="rounded-xl bg-status-rejected px-4 py-2 text-sm font-bold text-white"
            >
              نعم، احذف
            </button>
          </div>
        </ModalFrame>
      )}
      {rejectingSub && (
        <RejectSubmissionModal
          submission={rejectingSub}
          onClose={() => setRejectingSub(null)}
          onConfirm={(reason) => {
            onReviewSubmission(rejectingSub.id, false, reason);
            setRejectingSub(null);
          }}
        />
      )}
      {bonusTarget && (
        <BonusModal
          entry={bonusTarget}
          onClose={() => setBonusTarget(null)}
          onConfirm={(delta, reason) => {
            onAdjustBonus(bonusTarget.user.id, delta, reason);
            setBonusTarget(null);
          }}
        />
      )}
      {editingTask && (
        <EditTaskModal
          task={editingTask}
          onClose={() => setEditingTask(null)}
          onConfirm={(updated) => {
            onUpdateTask(updated);
            setEditingTask(null);
          }}
        />
      )}
    </div>
  );
};

/* ---------------- Admin PIN Gate ---------------- */
const AdminPinGate: React.FC<{
  onUnlock: (pin: string) => Promise<{ ok: boolean; error?: string }>;
  onBack: () => void;
}> = ({ onUnlock, onBack }) => {
  const [pin, setPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (checking) return;
    setChecking(true);
    setError(null);
    const res = await onUnlock(pin);
    setChecking(false);
    if (!res.ok) setError(res.error || 'كلمة مرور الأدمن غير صحيحة');
  };

  return (
    <div className="flex min-h-[100dvh] items-center justify-center bg-parchment p-5 safe-pt pb-28">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-[440px] space-y-4 rounded-[24px] border border-gold-warm/60 bg-white p-6 text-center shadow-md"
      >
        <ShieldCheck className="mx-auto h-12 w-12 text-burgundy" />
        <h1 className="text-xl font-bold text-midnight">دخول خدام التحدي</h1>
        <p className="text-xs text-ink-muted">
          هذه اللوحة خاصة بخدام التحدي فقط — أدخل كلمة مرور الأدمن للدخول
        </p>

        <div className="text-right">
          <input
            type="password"
            autoComplete="current-password"
            value={pin}
            onChange={(e) => {
              setPin(e.target.value);
              setError(null);
            }}
            placeholder="كلمة مرور الأدمن"
            className={`h-12 w-full rounded-xl border px-3.5 text-center text-base tracking-widest outline-none ${
              error
                ? 'border-status-rejected focus:ring-2 focus:ring-status-rejected/20'
                : 'border-parchment-outline focus:border-midnight focus:ring-2 focus:ring-midnight/15'
            }`}
          />
          {error && <p className="mt-1.5 text-xs font-semibold text-status-rejected">{error}</p>}
        </div>

        <button
          type="submit"
          disabled={checking}
          className="h-12 w-full rounded-2xl bg-midnight text-sm font-bold text-white shadow hover:bg-midnight-dark disabled:opacity-60"
        >
          {checking ? 'جاري التحقق...' : 'دخول لوحة التحكم'}
        </button>
        <button
          type="button"
          onClick={onBack}
          className="w-full py-2 text-xs font-bold text-ink-muted hover:text-midnight"
        >
          العودة للتطبيق
        </button>
      </form>
    </div>
  );
};

/* ---------------- Tab 0: Pending Approvals ---------------- */
const PendingQueueTab: React.FC<{
  pending: TaskSubmission[];
  onApprove: (sub: TaskSubmission) => void;
  onReject: (sub: TaskSubmission) => void;
  onApproveAll: () => void;
}> = ({ pending, onApprove, onReject, onApproveAll }) => (
  <div className="space-y-3">
    {pending.length > 0 && (
      <button
        type="button"
        onClick={onApproveAll}
        className="flex h-12 w-full items-center justify-center gap-2 rounded-2xl bg-status-approved text-sm font-bold text-white shadow transition hover:opacity-95 active:scale-[0.99]"
      >
        <CheckCheck className="h-5 w-5" />
        <span>موافقة على الكل ({pending.length} طلب)</span>
      </button>
    )}

    {pending.length === 0 ? (
      <div className="flex flex-col items-center rounded-[20px] border border-parchment-border bg-white px-6 py-10 text-center">
        <CheckCircle2 className="h-11 w-11 text-status-approved" />
        <h3 className="mt-2 text-base font-bold text-ink-deep">لا توجد طلبات معلقة حالياً</h3>
        <p className="mt-1 text-xs text-ink-muted">كل مشاركات الشباب تمت مراجعتها بنجاح!</p>
      </div>
    ) : (
      pending.map((sub) => (
        <div
          key={sub.id}
          className="space-y-2.5 rounded-[20px] border border-gold-warm/50 bg-white p-4 shadow-xs"
        >
          <div className="flex items-center justify-between gap-2">
            <h4 className="text-base font-bold text-midnight">{sub.userName}</h4>
            <span className="rounded-full bg-gold-warm/20 px-3 py-1 text-xs font-bold text-gold-warm">
              +{sub.points} نقطة
            </span>
          </div>
          <p className="text-sm font-semibold text-ink-deep">{sub.taskTitle}</p>
          <p className="text-xs text-ink-muted">التاريخ: {formatTimestampArabic(sub.submittedAt)}</p>
          {sub.note.trim() && (
            <div className="rounded-xl bg-parchment-variant/65 px-3 py-2 text-xs text-ink-deep">
              ملاحظة الشاب: {sub.note}
            </div>
          )}

          <div className="grid grid-cols-2 gap-2.5 pt-1">
            <button
              type="button"
              onClick={() => onApprove(sub)}
              className="flex h-12 items-center justify-center gap-1.5 rounded-xl bg-status-approved text-sm font-bold text-white transition hover:opacity-95 active:scale-95"
            >
              <CheckCircle2 className="h-4 w-4" />
              <span>موافقة ✅</span>
            </button>
            <button
              type="button"
              onClick={() => onReject(sub)}
              className="flex h-12 items-center justify-center gap-1.5 rounded-xl border border-status-rejected text-sm font-bold text-status-rejected transition hover:bg-status-rejectedBg active:scale-95"
            >
              <XCircle className="h-4 w-4" />
              <span>رفض ❌</span>
            </button>
          </div>
        </div>
      ))
    )}
  </div>
);

/* ---------------- Tab 1: Leaderboard & Youth Management ---------------- */
const LeaderboardUsersTab: React.FC<{
  entries: LeaderboardEntry[];
  onAddUserClick: () => void;
  onBonusClick: (entry: LeaderboardEntry) => void;
  onEditUserClick: (entry: LeaderboardEntry) => void;
  onDeleteUserClick: (entry: LeaderboardEntry) => void;
  onNoPhoneFeedback: () => void;
}> = ({
  entries,
  onAddUserClick,
  onBonusClick,
  onEditUserClick,
  onDeleteUserClick,
  onNoPhoneFeedback,
}) => {
  const [query, setQuery] = useState('');
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return entries;
    return entries.filter(
      (e) => e.user.fullName.toLowerCase().includes(q) || e.user.phone.includes(q)
    );
  }, [entries, query]);

  return (
    <div className="space-y-3">
      <div className="space-y-2.5 rounded-[20px] bg-midnight p-4 text-white shadow-sm">
        <h2 className="text-base font-bold text-gold-bright">
          لوحة الأبطال الحصرية وإدارة الشباب 🏆
        </h2>
        <p className="text-xs leading-relaxed text-white/90">
          {FastCalendar.isTrialMode()
            ? 'الوضع التجريبي مفعل حالياً 🧪 • تبدأ النتائج الرسمية تلقائياً من الصفر يوم 30/11/2026 حتى 5/1/2027.'
            : 'الترتيب الرسمي الحصري للأدمن فقط (30/11/2026 — 5/1/2027) • يمكنك إدارة أي مشارك أو مراسلته واتساب مباشرة.'}
        </p>
        <button
          type="button"
          onClick={onAddUserClick}
          className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-gold-warm text-sm font-bold text-midnight-dark transition hover:bg-gold-bright active:scale-[0.99]"
        >
          <UserPlus className="h-4 w-4" />
          <span>إضافة مشارك جديد +</span>
        </button>
      </div>

      <div className="relative">
        <Search className="pointer-events-none absolute right-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="بحث بالاسم أو رقم الموبايل..."
          className="h-12 w-full rounded-xl border border-parchment-outline bg-white pr-10 pl-3.5 text-sm outline-none focus:border-midnight"
        />
      </div>

      {filtered.length === 0 ? (
        <div className="rounded-2xl border border-parchment-border bg-white p-6 text-center text-sm text-ink-muted">
          لا يوجد مشاركون مطابقون للبحث.
        </div>
      ) : (
        filtered.map((entry) => {
          const isTop10 = entry.rank >= 1 && entry.rank <= FastCalendar.TOP_PRIZE_RANKS;
          const medalBg =
            entry.rank === 1
              ? 'bg-medal-gold'
              : entry.rank === 2
                ? 'bg-medal-silver'
                : entry.rank === 3
                  ? 'bg-medal-bronze'
                  : isTop10
                    ? 'bg-midnight'
                    : 'bg-parchment-outline';
          const borderClass =
            entry.rank === 1
              ? 'border-medal-gold'
              : entry.rank === 2
                ? 'border-medal-silver'
                : entry.rank === 3
                  ? 'border-medal-bronze'
                  : 'border-parchment-border';

          return (
            <div
              key={entry.user.id}
              className={`space-y-3 rounded-[20px] border bg-white p-3.5 shadow-xs ${borderClass}`}
            >
              <div className="flex items-center gap-3">
                <div
                  className={`flex h-11 w-11 shrink-0 flex-col items-center justify-center rounded-full text-white ${medalBg}`}
                >
                  {entry.rank <= 3 && <Award className="h-3.5 w-3.5" />}
                  <span className="text-xs font-extrabold leading-none">#{entry.rank}</span>
                </div>

                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-bold text-ink-deep">{entry.user.fullName}</p>
                  <p className="text-xs text-ink-muted">
                    {entry.user.phone.trim() ? `📱 ${entry.user.phone}` : 'بدون رقم واتساب'}
                  </p>
                  {isTop10 && entry.totalApprovedPoints > 0 && (
                    <span className="mt-0.5 inline-flex items-center gap-1 text-[11px] font-bold text-burgundy">
                      <Gift className="h-3 w-3" />
                      <span>مؤهل للجائزة 🎁</span>
                    </span>
                  )}
                </div>

                <div className="text-left">
                  <span className="inline-block rounded-lg bg-gold-warm/20 px-2.5 py-1 text-xs font-bold text-midnight">
                    {entry.totalApprovedPoints} نقطة
                  </span>
                  <p className="mt-0.5 text-[10px] text-ink-muted">
                    بونص: {entry.user.bonusPoints} | معلق: {entry.pendingPoints}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => {
                    if (!entry.user.phone.trim()) {
                      onNoPhoneFeedback();
                    } else {
                      openWhatsAppChat(
                        entry.user.phone,
                        `سلام ونعمة يا ${entry.user.fullName} ✨\nربنا يبارك جهادك في تحدي طريق المزود (مركزك الحالي #${entry.rank} برصيد ${entry.totalApprovedPoints} نقطة) — اجتماع أبو سيفين للشباب الخريجين`
                      );
                    }
                  }}
                  className="flex h-11 items-center justify-center gap-1.5 rounded-xl bg-whatsapp text-xs font-bold text-white transition hover:opacity-95"
                >
                  <MessageCircle className="h-4 w-4" />
                  <span>واتساب مباشر</span>
                </button>
                <button
                  type="button"
                  onClick={() => onBonusClick(entry)}
                  className="flex h-11 items-center justify-center gap-1.5 rounded-xl bg-midnight text-xs font-bold text-white transition hover:bg-midnight-dark"
                >
                  <PlusCircle className="h-4 w-4" />
                  <span>إضافة / خصم</span>
                </button>
              </div>

              <div className="grid grid-cols-[1fr_auto] gap-2">
                <button
                  type="button"
                  onClick={() => onEditUserClick(entry)}
                  className="flex h-11 items-center justify-center gap-1.5 rounded-xl border border-parchment-outline text-xs font-bold text-midnight hover:bg-parchment-variant"
                >
                  <Edit3 className="h-4 w-4" />
                  <span>تعديل البيانات / PIN</span>
                </button>
                <button
                  type="button"
                  onClick={() => onDeleteUserClick(entry)}
                  className="flex h-11 items-center justify-center gap-1.5 rounded-xl border border-status-rejected/70 px-4 text-xs font-bold text-status-rejected hover:bg-status-rejectedBg"
                >
                  <Trash2 className="h-4 w-4" />
                  <span>حذف</span>
                </button>
              </div>
            </div>
          );
        })
      )}
    </div>
  );
};

/* ---------------- Tab 2: Tasks & Weekly Quiz ---------------- */
const TasksAndQuizTab: React.FC<{
  tasks: ChallengeTask[];
  config: AppConfig;
  onToggleTask: (task: ChallengeTask, active: boolean) => void;
  onEditTask: (task: ChallengeTask) => void;
  onSaveConfig: (config: AppConfig) => void;
}> = ({ tasks, config, onToggleTask, onEditTask, onSaveConfig }) => {
  const [quizTitle, setQuizTitle] = useState(config.weeklyQuizTitle);
  const [quizQuestion, setQuizQuestion] = useState(config.weeklyQuizQuestion);
  const [quizUrl, setQuizUrl] = useState(config.weeklyQuizUrl);
  const [verseText, setVerseText] = useState(config.dailyVerseText);
  const [verseRef, setVerseRef] = useState(config.dailyVerseRef);
  const [encouragement, setEncouragement] = useState(config.dailyEncouragement);

  useEffect(() => {
    setQuizTitle(config.weeklyQuizTitle);
    setQuizQuestion(config.weeklyQuizQuestion);
    setQuizUrl(config.weeklyQuizUrl);
    setVerseText(config.dailyVerseText);
    setVerseRef(config.dailyVerseRef);
    setEncouragement(config.dailyEncouragement);
  }, [config]);

  return (
    <div className="space-y-4">
      <div className="space-y-3 rounded-[20px] border border-gold-warm/50 bg-white p-4 shadow-xs">
        <h3 className="text-base font-bold text-midnight">المسابقة الأسبوعية وآية اليوم</h3>
        <Field label="عنوان المسابقة الأسبوعية" value={quizTitle} onChange={setQuizTitle} />
        <TextAreaField
          label="سؤال أو تفاصيل المسابقة الأسبوعية"
          value={quizQuestion}
          onChange={setQuizQuestion}
        />
        <Field
          label="رابط المسابقة (Google Form - يبدأ بـ https://)"
          value={quizUrl}
          onChange={setQuizUrl}
          dir="ltr"
        />
        <TextAreaField label="آية اليوم" value={verseText} onChange={setVerseText} rows={2} />
        <Field label="الشاهد الكتابي" value={verseRef} onChange={setVerseRef} />
        <TextAreaField
          label="رسالة تشجيعية يومية"
          value={encouragement}
          onChange={setEncouragement}
          rows={2}
        />
        <button
          type="button"
          onClick={() =>
            onSaveConfig({
              id: 1,
              weeklyQuizTitle: quizTitle.trim(),
              weeklyQuizQuestion: quizQuestion.trim(),
              weeklyQuizUrl: quizUrl.trim(),
              dailyVerseText: verseText.trim(),
              dailyVerseRef: verseRef.trim(),
              dailyEncouragement: encouragement.trim(),
            })
          }
          className="h-12 w-full rounded-xl bg-midnight text-sm font-bold text-white shadow hover:bg-midnight-dark"
        >
          نشر المسابقة والآية للشباب
        </button>
      </div>

      <h3 className="text-base font-bold text-midnight">
        إدارة المهام الـ {tasks.length} وقيمة النقاط
      </h3>

      <div className="space-y-2.5">
        {tasks.map((task) => (
          <div
            key={task.id}
            className="flex items-center justify-between gap-2 rounded-2xl border border-parchment-border bg-white p-3.5"
          >
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-bold text-ink-deep">{task.title}</p>
              <p className="text-xs font-semibold text-gold-warm">
                {FREQUENCY_LABELS[task.frequency]} • {task.points} نقطة
              </p>
            </div>
            <button
              type="button"
              onClick={() => onEditTask(task)}
              className="rounded-xl px-3 py-1.5 text-xs font-bold text-midnight hover:bg-parchment-variant"
            >
              تعديل
            </button>
            <button
              type="button"
              role="switch"
              aria-checked={task.isActive}
              onClick={() => onToggleTask(task, !task.isActive)}
              className={`relative inline-flex h-7 w-12 shrink-0 items-center rounded-full transition-colors ${
                task.isActive ? 'bg-midnight' : 'bg-parchment-outline'
              }`}
            >
              <span
                className={`inline-block h-5 w-5 transform rounded-full bg-white shadow transition-transform ${
                  task.isActive ? '-translate-x-6' : '-translate-x-1'
                }`}
              />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

/* ---------------- Tab 3: Export & Settings ---------------- */
const ExportAndSetupTab: React.FC<{
  usersCount: number;
  approvedCount: number;
  onExportCsv: () => void;
  onChangeAdminPin: (currentPin: string, newPin: string) => Promise<boolean>;
}> = ({
  usersCount,
  approvedCount,
  onExportCsv,
  onChangeAdminPin,
}) => {
  const [currentPin, setCurrentPin] = useState('');
  const [newPin, setNewPin] = useState('');
  const [savingPin, setSavingPin] = useState(false);

  const handleUpdatePin = async () => {
    if (savingPin) return;
    setSavingPin(true);
    const ok = await onChangeAdminPin(currentPin, newPin);
    setSavingPin(false);
    if (ok) {
      setCurrentPin('');
      setNewPin('');
    }
  };

  return (
    <div className="space-y-3.5">
      <div className="space-y-3 rounded-[20px] border border-gold-warm/60 bg-white p-4 shadow-xs">
        <h3 className="text-base font-bold text-midnight">
          تصدير التقرير النهائي لجوائز عيد الميلاد 🎁
        </h3>
        <p className="text-xs leading-relaxed text-ink-muted">
          عدد المشاركين: {usersCount} • المهام المعتمدة: {approvedCount}.
          <br />
          يُصدّر ملف CSV جاهز للفتح في Microsoft Excel باللغة العربية.
        </p>
        <button
          type="button"
          onClick={onExportCsv}
          className="flex h-[52px] w-full items-center justify-center gap-2 rounded-2xl bg-burgundy text-sm font-bold text-white shadow transition hover:opacity-95"
        >
          <Download className="h-5 w-5" />
          <span>تصدير لوحة الأبطال والنقاط (CSV / Excel)</span>
        </button>
      </div>

      <div className="space-y-3 rounded-[20px] border border-parchment-border bg-white p-4 shadow-xs">
        <h3 className="text-base font-bold text-midnight">تغيير كلمة مرور الأدمن</h3>
        <p className="text-xs text-ink-muted">
          يتم تحديث كلمة المرور مباشرة في قاعدة البيانات وتُطبّق فوراً على جميع الأجهزة.
        </p>
        <Field
          label="كلمة المرور الحالية"
          type="password"
          value={currentPin}
          onChange={setCurrentPin}
        />
        <Field
          label="كلمة المرور الجديدة (4 أحرف أو أرقام فأكثر)"
          type="password"
          value={newPin}
          onChange={setNewPin}
        />
        <button
          type="button"
          disabled={savingPin}
          onClick={handleUpdatePin}
          className="h-12 w-full rounded-xl bg-midnight text-sm font-bold text-white shadow hover:bg-midnight-dark disabled:opacity-60"
        >
          {savingPin ? 'جاري التحديث في قاعدة البيانات...' : 'تحديث كلمة مرور الأدمن'}
        </button>
      </div>
    </div>
  );
};

/* ---------------- Reusable Modals & Form Controls ---------------- */
const ModalFrame: React.FC<{
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}> = ({ title, onClose, children }) => (
  <div
    className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-[2px]"
    onClick={onClose}
  >
    <div
      className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-3xl bg-white p-5 shadow-2xl animate-slide-up"
      onClick={(e) => e.stopPropagation()}
    >
      <div className="mb-4 flex items-center justify-between gap-2">
        <h3 className="text-base font-bold text-midnight">{title}</h3>
        <button
          type="button"
          onClick={onClose}
          aria-label="إغلاق"
          className="flex h-8 w-8 items-center justify-center rounded-full text-ink-muted hover:bg-parchment-variant"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
      {children}
    </div>
  </div>
);

const Field: React.FC<{
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  type?: string;
  dir?: 'rtl' | 'ltr';
}> = ({ label, value, onChange, placeholder, type = 'text', dir }) => (
  <div>
    <label className="mb-1 block text-xs font-semibold text-ink-deep">{label}</label>
    <input
      type={type}
      dir={dir}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      className="h-11 w-full rounded-xl border border-parchment-outline bg-parchment/30 px-3 text-sm text-ink-deep outline-none focus:border-midnight focus:bg-white"
    />
  </div>
);

const TextAreaField: React.FC<{
  label: string;
  value: string;
  onChange: (v: string) => void;
  rows?: number;
}> = ({ label, value, onChange, rows = 3 }) => (
  <div>
    <label className="mb-1 block text-xs font-semibold text-ink-deep">{label}</label>
    <textarea
      rows={rows}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="w-full rounded-xl border border-parchment-outline bg-parchment/30 p-3 text-sm text-ink-deep outline-none focus:border-midnight focus:bg-white"
    />
  </div>
);

const AddUserModal: React.FC<{
  onClose: () => void;
  onConfirm: (name: string, phone: string, pin: string, bonus: number) => void;
}> = ({ onClose, onConfirm }) => {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [pin, setPin] = useState('');
  const [bonusStr, setBonusStr] = useState('0');
  return (
    <ModalFrame title="إضافة مشارك جديد" onClose={onClose}>
      <div className="space-y-3">
        <Field label="الاسم بالكامل *" value={name} onChange={setName} />
        <Field
          label="رقم الواتساب / الموبايل"
          value={phone}
          onChange={setPhone}
          placeholder="مثال: 0501234567"
          dir="ltr"
        />
        <Field
          label="الرقم السري (4 أرقام PIN) *"
          type="password"
          value={pin}
          onChange={(v) => setPin(v.replace(/\D/g, '').slice(0, 4))}
        />
        <Field
          label="نقاط بداية / بونص (اختياري)"
          value={bonusStr}
          onChange={(v) => setBonusStr(v.replace(/[^0-9-]/g, ''))}
        />
      </div>
      <div className="mt-5 flex justify-end gap-2">
        <button
          type="button"
          onClick={onClose}
          className="rounded-xl px-4 py-2 text-sm font-semibold text-ink-muted hover:bg-parchment-variant"
        >
          إلغاء
        </button>
        <button
          type="button"
          onClick={() => onConfirm(name, phone, pin, parseInt(bonusStr, 10) || 0)}
          className="rounded-xl bg-midnight px-4 py-2 text-sm font-bold text-white"
        >
          إضافة المشارك
        </button>
      </div>
    </ModalFrame>
  );
};

const EditUserModal: React.FC<{
  entry: LeaderboardEntry;
  onClose: () => void;
  onConfirm: (name: string, phone: string, newPin: string) => void;
}> = ({ entry, onClose, onConfirm }) => {
  const [name, setName] = useState(entry.user.fullName);
  const [phone, setPhone] = useState(entry.user.phone);
  const [newPin, setNewPin] = useState('');
  return (
    <ModalFrame title="تعديل بيانات المشارك" onClose={onClose}>
      <div className="space-y-3">
        <Field label="الاسم بالكامل *" value={name} onChange={setName} />
        <Field label="رقم الواتساب / الموبايل" value={phone} onChange={setPhone} dir="ltr" />
        <Field
          label="رقم سري جديد (اختياري - اتركه فارغاً للإبقاء)"
          type="password"
          value={newPin}
          onChange={(v) => setNewPin(v.replace(/\D/g, '').slice(0, 4))}
        />
      </div>
      <div className="mt-5 flex justify-end gap-2">
        <button
          type="button"
          onClick={onClose}
          className="rounded-xl px-4 py-2 text-sm font-semibold text-ink-muted hover:bg-parchment-variant"
        >
          إلغاء
        </button>
        <button
          type="button"
          onClick={() => onConfirm(name, phone, newPin)}
          className="rounded-xl bg-midnight px-4 py-2 text-sm font-bold text-white"
        >
          حفظ التعديلات
        </button>
      </div>
    </ModalFrame>
  );
};

const RejectSubmissionModal: React.FC<{
  submission: TaskSubmission;
  onClose: () => void;
  onConfirm: (reason: string) => void;
}> = ({ submission, onClose, onConfirm }) => {
  const [reason, setReason] = useState('');
  return (
    <ModalFrame title={`رفض مشاركة ${submission.userName}`} onClose={onClose}>
      <p className="mb-3 text-xs text-ink-muted">
        المهمة: {submission.taskTitle} (+{submission.points} نقطة)
      </p>
      <Field
        label="سبب الرفض (اختياري)"
        value={reason}
        onChange={setReason}
        placeholder="مثال: يرجى التأكد من إتمام المهمة وإعادة التسجيل"
      />
      <div className="mt-5 flex justify-end gap-2">
        <button
          type="button"
          onClick={onClose}
          className="rounded-xl px-4 py-2 text-sm font-semibold text-ink-muted hover:bg-parchment-variant"
        >
          إلغاء
        </button>
        <button
          type="button"
          onClick={() => onConfirm(reason)}
          className="rounded-xl bg-status-rejected px-4 py-2 text-sm font-bold text-white"
        >
          تأكيد الرفض
        </button>
      </div>
    </ModalFrame>
  );
};

const BonusModal: React.FC<{
  entry: LeaderboardEntry;
  onClose: () => void;
  onConfirm: (delta: number, reason: string) => void;
}> = ({ entry, onClose, onConfirm }) => {
  const [deltaStr, setDeltaStr] = useState('100');
  const [reason, setReason] = useState('فائز في المسابقة الأسبوعية');
  return (
    <ModalFrame title={`إضافة / خصم نقاط • ${entry.user.fullName}`} onClose={onClose}>
      <p className="mb-3 text-xs text-ink-muted">
        استخدم رقم موجب للإضافة (مثل 100) أو رقم سالب للخصم (مثل -50).
      </p>
      <div className="space-y-3">
        <Field label="عدد النقاط (+ أو -)" value={deltaStr} onChange={setDeltaStr} />
        <Field label="السبب / الملاحظة" value={reason} onChange={setReason} />
      </div>
      <div className="mt-5 flex justify-end gap-2">
        <button
          type="button"
          onClick={onClose}
          className="rounded-xl px-4 py-2 text-sm font-semibold text-ink-muted hover:bg-parchment-variant"
        >
          إلغاء
        </button>
        <button
          type="button"
          onClick={() => onConfirm(parseInt(deltaStr.trim(), 10) || 0, reason)}
          className="rounded-xl bg-midnight px-4 py-2 text-sm font-bold text-white"
        >
          حفظ التعديل
        </button>
      </div>
    </ModalFrame>
  );
};

const EditTaskModal: React.FC<{
  task: ChallengeTask;
  onClose: () => void;
  onConfirm: (updated: ChallengeTask) => void;
}> = ({ task, onClose, onConfirm }) => {
  const [title, setTitle] = useState(task.title);
  const [pointsStr, setPointsStr] = useState(String(task.points));
  return (
    <ModalFrame title="تعديل المهمة والنقاط" onClose={onClose}>
      <div className="space-y-3">
        <Field label="اسم المهمة" value={title} onChange={setTitle} />
        <Field
          label="قيمة النقاط"
          value={pointsStr}
          onChange={(v) => setPointsStr(v.replace(/\D/g, ''))}
        />
      </div>
      <div className="mt-5 flex justify-end gap-2">
        <button
          type="button"
          onClick={onClose}
          className="rounded-xl px-4 py-2 text-sm font-semibold text-ink-muted hover:bg-parchment-variant"
        >
          إلغاء
        </button>
        <button
          type="button"
          onClick={() => {
            const parsed = parseInt(pointsStr, 10);
            const safePts = Number.isNaN(parsed) ? task.points : Math.min(Math.max(parsed, 10), 2000);
            onConfirm({
              ...task,
              title: title.trim() || task.title,
              points: safePts,
            });
          }}
          className="rounded-xl bg-midnight px-4 py-2 text-sm font-bold text-white"
        >
          حفظ
        </button>
      </div>
    </ModalFrame>
  );
};
