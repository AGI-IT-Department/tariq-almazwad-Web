import React, { useState } from 'react';
import { Loader2, Lock, Phone, ShieldCheck, Sparkles, User } from 'lucide-react';
import { APP_ASSETS } from '../lib/assets';

interface LoginScreenProps {
  isSyncing: boolean;
  isSupabaseLive: boolean;
  onLoginOrRegister: (fullName: string, pin: string, phone: string) => void;
  onOpenAdmin: () => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({
  isSyncing,
  isSupabaseLive,
  onLoginOrRegister,
  onOpenAdmin,
}) => {
  const [fullName, setFullName] = useState('');
  const [pin, setPin] = useState('');
  const [phone, setPhone] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onLoginOrRegister(fullName, pin, phone);
  };

  return (
    <div className="min-h-[100dvh] bg-parchment safe-pt pb-28">
      <div className="mx-auto max-w-[560px] space-y-4 px-5 py-3">
        {/* Top Bar with discreet Admin entry */}
        <div className="flex items-center justify-between gap-2">
          <div className="inline-flex items-center gap-1.5 rounded-full bg-midnight/10 px-3 py-1.5 text-xs font-semibold text-midnight">
            <Sparkles className="h-3.5 w-3.5 text-gold-warm" />
            <span>{isSupabaseLive ? 'مزامنة حية Supabase' : 'الشارقة • 37 يوماً'}</span>
          </div>

          <button
            type="button"
            onClick={onOpenAdmin}
            className="inline-flex min-h-[44px] items-center gap-1.5 rounded-xl px-2.5 text-sm font-bold text-burgundy transition hover:bg-burgundy/10"
          >
            <ShieldCheck className="h-4 w-4" />
            <span>دخول خدام التحدي</span>
          </button>
        </div>

        {/* Nativity Hero Banner Card with Official Logos */}
        <div className="overflow-hidden rounded-[24px] border border-gold-warm/60 bg-gradient-to-b from-midnight-dark to-midnight p-4 text-white shadow-md">
          <div className="flex items-center gap-3">
            <img
              src={APP_ASSETS.challengeLogo}
              onError={(e) => {
                e.currentTarget.onerror = null;
                e.currentTarget.src = APP_ASSETS.challengeLogoFallback;
              }}
              alt="شعار تحدي طريق المزود"
              className="h-[72px] w-[72px] shrink-0 rounded-full object-contain"
            />
            <div className="min-w-0 flex-1 space-y-1">
              <h1 className="text-xl font-bold text-gold-bright">تحدي طريق المزود</h1>
              <p className="text-xs text-white">صوم الميلاد المجيد • اجتماع أبو سيفين بالشارقة</p>
              <p className="text-[11px] font-medium text-gold-bright/90">
                30 نوفمبر 2026 — 5 يناير 2027 • جوائز لأول 10 مراكز 🎁
              </p>
            </div>
            <div className="shrink-0 rounded-2xl border border-gold-warm/70 bg-white p-1.5">
              <img
                src={APP_ASSETS.churchLogo}
                onError={(e) => {
                  e.currentTarget.onerror = null;
                  e.currentTarget.src = APP_ASSETS.churchLogoFallback;
                }}
                alt="شعار أسرة أبو سيفين للشباب الخريجين"
                className="h-11 w-11 bg-white object-contain"
              />
            </div>
          </div>
        </div>

        {/* Login / Registration Card */}
        <form
          onSubmit={handleSubmit}
          className="space-y-3.5 rounded-[22px] border border-parchment-border bg-white p-5 shadow-sm"
        >
          <div>
            <h2 className="text-lg font-bold text-midnight">تسجيل الدخول أو حساب جديد</h2>
            <p className="mt-1 text-xs leading-relaxed text-ink-muted">
              اكتب اسمك بالكامل ورقم سري من 4 أرقام لتبدأ فوراً بدون إيميل أو تعقيد.
            </p>
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold text-ink-deep">
              الاسم بالكامل *
            </label>
            <div className="relative">
              <User className="pointer-events-none absolute right-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="مثال: بيشوي مجدي"
                required
                className="h-12 w-full rounded-xl border border-parchment-outline bg-parchment/30 pr-10 pl-3.5 text-sm text-ink-deep outline-none transition focus:border-midnight focus:bg-white focus:ring-2 focus:ring-midnight/15"
              />
            </div>
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold text-ink-deep">
              الرقم السري (4 أرقام PIN) *
            </label>
            <div className="relative">
              <Lock className="pointer-events-none absolute right-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
              <input
                type="password"
                inputMode="numeric"
                maxLength={4}
                value={pin}
                onChange={(e) => setPin(e.target.value.replace(/\D/g, '').slice(0, 4))}
                placeholder="مثال: 1234"
                required
                className="h-12 w-full rounded-xl border border-parchment-outline bg-parchment/30 pr-10 pl-3.5 text-sm text-ink-deep outline-none transition focus:border-midnight focus:bg-white focus:ring-2 focus:ring-midnight/15"
              />
            </div>
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold text-ink-deep">
              رقم الموبايل / واتساب (اختياري)
            </label>
            <div className="relative">
              <Phone className="pointer-events-none absolute right-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
              <input
                type="tel"
                dir="ltr"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="0501234567"
                className="h-12 w-full rounded-xl border border-parchment-outline bg-parchment/30 pr-10 pl-3.5 text-right text-sm text-ink-deep outline-none transition focus:border-midnight focus:bg-white focus:ring-2 focus:ring-midnight/15"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isSyncing}
            className="mt-2 flex h-[54px] w-full items-center justify-center rounded-2xl bg-midnight text-base font-bold text-white shadow-md transition hover:bg-midnight-dark active:scale-[0.99] disabled:opacity-65"
          >
            {isSyncing ? (
              <Loader2 className="h-5 w-5 animate-spin text-gold-bright" />
            ) : (
              <span>دخول إلى تحدي طريق المزود</span>
            )}
          </button>
        </form>

        <p className="px-2 pb-4 text-center text-xs leading-relaxed text-ink-muted">
          إذا نسيت رقمك السري، تواصل مع خدام التحدي لإعادة تعيينه من لوحة التحكم.
        </p>
      </div>
    </div>
  );
};
