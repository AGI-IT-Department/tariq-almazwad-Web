import React from 'react';
import {
  BellRing,
  Cloud,
  CloudOff,
  LogOut,
  RefreshCw,
  ShieldCheck,
} from 'lucide-react';
import { APP_ASSETS } from '../lib/assets';

interface AppHeaderProps {
  isSupabaseLive: boolean;
  isSyncing: boolean;
  pendingAdminCount: number;
  onOpenAdmin: () => void;
  onOpenReminders: () => void;
  onRefresh: () => void;
  onLogout: () => void;
}

export const AppHeader: React.FC<AppHeaderProps> = ({
  isSupabaseLive,
  isSyncing,
  pendingAdminCount,
  onOpenAdmin,
  onOpenReminders,
  onRefresh,
  onLogout,
}) => {
  return (
    <header className="sticky top-0 z-30 bg-midnight text-white shadow-md safe-pt">
      <div className="mx-auto max-w-[640px] px-4 pb-3">
        <div className="flex items-center justify-between gap-2">
          <div className="flex min-w-0 flex-1 items-center gap-2.5">
            <img
              src={APP_ASSETS.challengeLogo}
              onError={(e) => {
                e.currentTarget.onerror = null;
                e.currentTarget.src = APP_ASSETS.challengeLogoFallback;
              }}
              alt="شعار تحدي طريق المزود"
              className="h-11 w-11 shrink-0 rounded-full object-contain ring-1 ring-gold-warm/40"
            />
            <div className="min-w-0">
              <h1 className="truncate text-lg font-bold leading-tight text-white">
                تحدي طريق المزود
              </h1>
              <p className="truncate text-[11px] font-medium text-gold-bright">
                صوم الميلاد المجيد • اجتماع أبو سيفين للشباب الخريجين
              </p>
            </div>
          </div>

          <div className="flex shrink-0 items-center gap-0.5">
            {isSupabaseLive && (
              <button
                type="button"
                onClick={onRefresh}
                aria-label="تحديث البيانات"
                className="flex h-10 w-10 items-center justify-center rounded-full text-white/90 transition hover:bg-white/10 active:scale-95"
              >
                <RefreshCw className={`h-5 w-5 ${isSyncing ? 'animate-spin' : ''}`} />
              </button>
            )}
            <button
              type="button"
              onClick={onOpenReminders}
              aria-label="التنبيهات اليومية"
              className="flex h-10 w-10 items-center justify-center rounded-full text-gold-bright transition hover:bg-white/10 active:scale-95"
            >
              <BellRing className="h-5 w-5" />
            </button>
            <button
              type="button"
              onClick={onOpenAdmin}
              aria-label="دخول الخادم / الأدمن"
              className="relative flex h-10 w-10 items-center justify-center rounded-full text-white transition hover:bg-white/10 active:scale-95"
            >
              <ShieldCheck className="h-5 w-5" />
              {pendingAdminCount > 0 && (
                <span className="absolute -top-0.5 -right-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-burgundy px-1 text-[10px] font-bold text-white shadow">
                  {pendingAdminCount}
                </span>
              )}
            </button>
            <button
              type="button"
              onClick={onLogout}
              aria-label="تسجيل الخروج"
              className="flex h-10 w-10 items-center justify-center rounded-full text-white/90 transition hover:bg-white/10 active:scale-95"
            >
              <LogOut className="h-5 w-5" />
            </button>
          </div>
        </div>

        <div className="mt-1.5 flex items-center gap-1.5 text-[11px] text-white/80">
          {isSupabaseLive ? (
            <Cloud className="h-3.5 w-3.5 text-status-approvedBg" />
          ) : (
            <CloudOff className="h-3.5 w-3.5 text-gold-bright/85" />
          )}
          <span className="truncate">
            {isSupabaseLive
              ? 'متصل بقاعدة بيانات Supabase الحية'
              : 'وضع محلي فوري • أضف مفاتيح Supabase للمزامنة بين الموبايلات'}
          </span>
        </div>
      </div>
    </header>
  );
};
