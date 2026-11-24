import React from 'react';
import {
  Award,
  CheckCircle2,
  Gift,
  Hourglass,
  Sparkles,
  Star,
  Trophy,
} from 'lucide-react';
import type { LeaderboardEntry, YouthUser } from '../types';
import { FastCalendar } from '../lib/fastCalendar';

interface MyRankScreenProps {
  user: YouthUser;
  rankEntry: LeaderboardEntry | null;
  totalParticipants: number;
}

export const MyRankScreen: React.FC<MyRankScreenProps> = ({
  user,
  rankEntry,
  totalParticipants,
}) => {
  const totalCount = Math.max(totalParticipants, 1);
  const myRank = rankEntry?.rank ?? totalCount;
  const approvedPoints = rankEntry?.totalApprovedPoints ?? 0;
  const pendingPoints = rankEntry?.pendingPoints ?? 0;
  const bonusPoints = user.bonusPoints;
  const approvedTasksCount = rankEntry?.approvedTasksCount ?? 0;

  const isTop10 = myRank >= 1 && myRank <= FastCalendar.TOP_PRIZE_RANKS && approvedPoints > 0;
  const ranksToTop10 = Math.max(myRank - FastCalendar.TOP_PRIZE_RANKS, 0);

  const badgeHex =
    myRank === 1
      ? '#D4AF37'
      : myRank === 2
        ? '#8C9BA5'
        : myRank === 3
          ? '#B87333'
          : '#C89D4C';

  const fastDay = FastCalendar.currentFastDayIndex();
  const fastProgress = Math.min(Math.max(fastDay / FastCalendar.FAST_DAYS_COUNT, 0.05), 1);

  const ringRadius = 66;
  const ringCircumference = 2 * Math.PI * ringRadius;
  const ringOffset = ringCircumference * (1 - fastProgress);

  return (
    <div className="mx-auto max-w-[640px] space-y-4 px-4 pt-3.5 pb-40">
      {/* Private Personal Rank Crest Card */}
      <section className="rounded-[28px] bg-gradient-to-b from-midnight to-midnight-dark p-6 text-center text-white shadow-lg">
        <div className="mx-auto inline-flex items-center gap-1.5 rounded-full border border-gold-bright/45 bg-gold-warm/20 px-3.5 py-1.5 text-xs font-semibold text-gold-bright">
          <Sparkles className="h-4 w-4" />
          <span>
            {FastCalendar.isTrialMode()
              ? 'الوضع التجريبي 🧪 • الترتيب الرسمي يبدأ 30/11'
              : 'مركزك الشخصي في تحدي طريق المزود'}
          </span>
        </div>

        {/* Animated Rank Ring */}
        <div className="relative mx-auto my-4 flex h-[152px] w-[152px] items-center justify-center">
          <svg className="h-full w-full -rotate-90" viewBox="0 0 152 152">
            <circle
              cx="76"
              cy="76"
              r={ringRadius}
              fill="none"
              stroke="rgba(255,255,255,0.14)"
              strokeWidth="10"
            />
            <circle
              cx="76"
              cy="76"
              r={ringRadius}
              fill="none"
              stroke={badgeHex}
              strokeWidth="10"
              strokeLinecap="round"
              strokeDasharray={ringCircumference}
              strokeDashoffset={ringOffset}
              className="transition-all duration-700 ease-out"
            />
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            {myRank <= 3 ? (
              <Award className="h-7 w-7" style={{ color: badgeHex }} />
            ) : (
              <Trophy className="h-7 w-7" style={{ color: badgeHex }} />
            )}
            <span className="text-3xl font-extrabold leading-tight text-white">#{myRank}</span>
            <span className="text-[11px] text-white/75">من {totalCount} مشارك</span>
          </div>
        </div>

        <h2 className="text-xl font-bold text-white">{user.fullName}</h2>

        <div className="mt-3">
          {isTop10 ? (
            <div className="inline-flex items-center gap-1.5 rounded-full border border-gold-bright/60 bg-burgundy px-4 py-2 text-xs font-bold text-white shadow">
              <Gift className="h-4 w-4 text-gold-bright" />
              <span>أنت ضمن أفضل 10 مراكز • مؤهل للجائزة 🎁</span>
            </div>
          ) : (
            <div className="rounded-2xl bg-white/10 px-4 py-2.5 text-xs leading-relaxed text-gold-bright">
              {approvedPoints === 0
                ? 'سجّل أول مهمة اليوم لتبدأ رحلتك نحو المراكز الـ 10 الأولى! ✨'
                : `أنت قريب جداً! باقي ${ranksToTop10} مركز لدخول قائمة الجوائز (أفضل 10) 💪`}
            </div>
          )}
        </div>
      </section>

      {/* Personal Stats 2x2 Grid */}
      <div className="grid grid-cols-2 gap-3">
        <PersonalStatCard
          icon={<CheckCircle2 className="h-5 w-5 text-status-approved" />}
          iconBg="bg-status-approved/15"
          value={String(approvedPoints)}
          label="نقاطك المعتمدة"
        />
        <PersonalStatCard
          icon={<Hourglass className="h-5 w-5 text-status-pending" />}
          iconBg="bg-status-pending/15"
          value={String(pendingPoints)}
          label="قيد المراجعة ⏳"
        />
        <PersonalStatCard
          icon={<Star className="h-5 w-5 text-midnight" />}
          iconBg="bg-midnight/15"
          value={String(approvedTasksCount)}
          label="مهام معتمدة"
        />
        <PersonalStatCard
          icon={<Gift className="h-5 w-5 text-burgundy" />}
          iconBg="bg-burgundy/15"
          value={bonusPoints >= 0 ? `+${bonusPoints}` : String(bonusPoints)}
          label="نقاط بونص الخادم"
        />
      </div>

      {/* Spiritual Encouragement Note about Privacy of Leaderboard */}
      <section className="rounded-[20px] border border-gold-warm/45 bg-white p-4 shadow-xs">
        <h3 className="text-base font-bold text-midnight">المنافسة الحقيقية هي مع نفسك ✨</h3>
        <p className="mt-1.5 text-xs leading-relaxed text-ink-muted">
          يظهر لك هنا مركزك الشخصي وتقدمك فقط للتركيز على نموك الروحي خلال صوم الميلاد، بينما يتابع
          خادم الاجتماع لوحة الأبطال الكاملة لإعلان الفائزين الـ 10 في احتفال عيد الميلاد المجيد.
        </p>
      </section>
    </div>
  );
};

const PersonalStatCard: React.FC<{
  icon: React.ReactNode;
  iconBg: string;
  value: string;
  label: string;
}> = ({ icon, iconBg, value, label }) => (
  <div className="flex flex-col items-center gap-2 rounded-[20px] border border-parchment-border bg-white p-4 text-center shadow-xs">
    <div className={`flex h-10 w-10 items-center justify-center rounded-full ${iconBg}`}>{icon}</div>
    <span className="text-2xl font-extrabold leading-tight text-ink-deep">{value}</span>
    <span className="text-xs font-semibold text-ink-muted">{label}</span>
  </div>
);
