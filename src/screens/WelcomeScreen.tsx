import React from 'react';
import { ArrowLeft, Gift, Sparkles, Star } from 'lucide-react';
import { APP_ASSETS } from '../lib/assets';

interface WelcomeScreenProps {
  onStartJourney: () => void;
}

const STARS = [
  { left: '12%', top: '10%', size: 6, alt: false },
  { left: '86%', top: '12%', size: 5, alt: true },
  { left: '22%', top: '24%', size: 4, alt: false },
  { left: '78%', top: '22%', size: 6, alt: true },
  { left: '8%', top: '44%', size: 4, alt: false },
  { left: '92%', top: '48%', size: 5, alt: true },
  { left: '16%', top: '72%', size: 6, alt: false },
  { left: '84%', top: '76%', size: 4, alt: true },
  { left: '50%', top: '7%', size: 6, alt: false },
];

export const WelcomeScreen: React.FC<WelcomeScreenProps> = ({ onStartJourney }) => {
  return (
    <div className="relative min-h-[100dvh] w-full overflow-x-hidden bg-gradient-to-b from-midnight-dark via-midnight to-midnight-dark text-white">
      {/* Subtle animated Bethlehem starry backdrop */}
      <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
        {STARS.map((s, idx) => (
          <span
            key={idx}
            className={`absolute rounded-full bg-gold-bright ${
              s.alt ? 'animate-twinkle-alt' : 'animate-twinkle'
            }`}
            style={{
              left: s.left,
              top: s.top,
              width: s.size,
              height: s.size,
            }}
          />
        ))}
      </div>

      <div className="relative mx-auto flex min-h-[100dvh] max-w-[520px] flex-col items-center justify-between px-6 pt-5 pb-28 safe-pt">
        {/* Official Church & Meeting Logo Card */}
        <div className="w-full rounded-[22px] border border-gold-warm/85 bg-white px-3.5 py-2.5 shadow-lg">
          <div className="flex items-center gap-3">
            <img
              src={APP_ASSETS.churchLogo}
              onError={(e) => {
                e.currentTarget.onerror = null;
                e.currentTarget.src = APP_ASSETS.churchLogoFallback;
              }}
              alt="شعار أسرة أبو سيفين للشباب الخريجين"
              className="h-14 w-14 shrink-0 rounded-2xl bg-white object-contain"
            />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-bold text-midnight-dark">
                أسرة أبو سيفين للشباب الخريجين
              </p>
              <p className="text-xs font-semibold text-gold-warm">
                كنيسة الشهيد أبي سيفين بالشارقة
              </p>
            </div>
          </div>
        </div>

        {/* Centerpiece Challenge Emblem with Golden Glow */}
        <div className="my-5 flex w-full flex-col items-center text-center">
          <div className="relative flex h-60 w-60 items-center justify-center">
            <div
              className="animate-halo absolute inset-0 rounded-full"
              style={{
                background:
                  'radial-gradient(circle, rgba(229,191,112,0.44) 0%, rgba(200,157,76,0.16) 52%, transparent 72%)',
              }}
            />
            <img
              src={APP_ASSETS.challengeLogo}
              onError={(e) => {
                e.currentTarget.onerror = null;
                e.currentTarget.src = APP_ASSETS.challengeLogoFallback;
              }}
              alt="شعار تحدي طريق المزود صوم الميلاد 2026"
              className="relative h-52 w-52 rounded-full object-contain drop-shadow-xl"
            />
          </div>

          <h1 className="font-amiri mt-3 text-4xl font-bold leading-tight text-gold-bright">
            تحدي طريق المزود
          </h1>
          <p className="mt-2.5 text-sm leading-relaxed text-white/90">
            رحلتك الروحية اليومية في صوم الميلاد المجيد
            <br />
            من 30 نوفمبر 2026 حتى 5 يناير 2027
          </p>

          {/* 3 Compact Navy & Gold Highlights */}
          <div className="mt-5 grid w-full grid-cols-3 gap-2">
            <HighlightChip icon={<Sparkles className="h-5 w-5 text-gold-bright" />} title="37 يوماً" subtitle="5 أسابيع صوم" />
            <HighlightChip icon={<Star className="h-5 w-5 text-gold-bright" />} title="10 مهام" subtitle="يومية وأسبوعية" />
            <HighlightChip icon={<Gift className="h-5 w-5 text-gold-bright" />} title="أول 10" subtitle="جوائز مميزة" />
          </div>
        </div>

        {/* Primary CTA Button */}
        <div className="w-full space-y-3 text-center">
          <button
            type="button"
            onClick={onStartJourney}
            className="flex h-[58px] w-full items-center justify-center gap-2 rounded-[18px] bg-gold-warm text-base font-bold text-midnight-dark shadow-lg transition hover:bg-gold-bright active:scale-[0.99]"
          >
            <span>ابدأ رحلتك</span>
            <ArrowLeft className="h-5 w-5" />
          </button>
          <p className="font-amiri text-[15px] text-gold-bright/85">
            «المجد لله في الأعالي، وعلى الأرض السلام، وبالناس المسرة»
          </p>
        </div>
      </div>
    </div>
  );
};

const HighlightChip: React.FC<{
  icon: React.ReactNode;
  title: string;
  subtitle: string;
}> = ({ icon, title, subtitle }) => (
  <div className="flex flex-col items-center gap-1 rounded-2xl border border-gold-warm/45 bg-white/10 px-2 py-2.5 backdrop-blur-sm">
    {icon}
    <span className="text-sm font-bold text-white">{title}</span>
    <span className="text-[11px] font-medium text-gold-bright/90">{subtitle}</span>
  </div>
);
