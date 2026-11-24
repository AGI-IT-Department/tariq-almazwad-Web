import React, { useEffect, useState } from 'react';
import { Download, Share, Smartphone, X } from 'lucide-react';
import { localStore } from '../lib/storage';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

interface InstallPwaBannerProps {
  /** Extra bottom spacing when BottomNav is visible */
  aboveBottomNav?: boolean;
}

function isRunningStandalone(): boolean {
  if (typeof window === 'undefined') return false;
  const mq = window.matchMedia?.('(display-mode: standalone)')?.matches;
  const iosStandalone = (window.navigator as Navigator & { standalone?: boolean }).standalone === true;
  return Boolean(mq || iosStandalone);
}

export const InstallPwaBanner: React.FC<InstallPwaBannerProps> = ({ aboveBottomNav = false }) => {
  const [visible, setVisible] = useState(false);
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);

  useEffect(() => {
    if (isRunningStandalone() || localStore.isInstallBannerDismissed()) {
      setVisible(false);
      return;
    }
    setVisible(true);

    const handleBeforeInstall = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      setVisible(true);
    };

    const handleAppInstalled = () => {
      setDeferredPrompt(null);
      setVisible(false);
      localStore.dismissInstallBanner();
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstall);
    window.addEventListener('appinstalled', handleAppInstalled);
    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstall);
      window.removeEventListener('appinstalled', handleAppInstalled);
    };
  }, []);

  const handleDismiss = () => {
    localStore.dismissInstallBanner();
    setVisible(false);
  };

  const handleNativeInstall = async () => {
    if (!deferredPrompt) return;
    await deferredPrompt.prompt();
    const choice = await deferredPrompt.userChoice;
    if (choice.outcome === 'accepted') {
      localStore.dismissInstallBanner();
      setVisible(false);
    }
    setDeferredPrompt(null);
  };

  if (!visible) return null;

  return (
    <div
      role="region"
      aria-label="تثبيت التطبيق"
      className={`fixed inset-x-0 z-40 px-3 pointer-events-none animate-slide-up ${
        aboveBottomNav ? 'bottom-[82px]' : 'bottom-4 safe-pb'
      }`}
    >
      <div className="pointer-events-auto mx-auto max-w-[600px] rounded-2xl border border-gold-warm/60 bg-midnight-dark/95 px-3.5 py-3 text-white shadow-xl backdrop-blur-md">
        <div className="flex items-start gap-3">
          <div className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-gold-warm/20 text-gold-bright">
            <Smartphone className="h-5 w-5" />
          </div>

          <div className="min-w-0 flex-1">
            <p className="text-xs font-bold leading-relaxed text-white">
              لتثبيت التطبيق على موبايلك: اضغط على مشاركة{' '}
              <Share className="inline-block h-3.5 w-3.5 align-text-bottom text-gold-bright" />{' '}
              ثم إضافة إلى الشاشة الرئيسية
            </p>
            {deferredPrompt && (
              <button
                type="button"
                onClick={handleNativeInstall}
                className="mt-2 inline-flex items-center gap-1.5 rounded-xl bg-gold-warm px-3 py-1.5 text-xs font-bold text-midnight-dark transition hover:bg-gold-bright active:scale-95"
              >
                <Download className="h-3.5 w-3.5" />
                <span>تثبيت التطبيق مباشرة</span>
              </button>
            )}
          </div>

          <button
            type="button"
            onClick={handleDismiss}
            aria-label="إغلاق إشعار التثبيت"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-white/75 transition hover:bg-white/10 hover:text-white"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
};
