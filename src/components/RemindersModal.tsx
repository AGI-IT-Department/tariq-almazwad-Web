import React, { useState } from 'react';
import { BellRing, X } from 'lucide-react';
import type { ReminderSettings } from '../types';
import { formatTimeArabic } from '../lib/fastCalendar';

interface RemindersModalProps {
  settings: ReminderSettings;
  onUpdateSettings: (settings: ReminderSettings) => void;
  onSendTest: () => void;
  onDismiss: () => void;
}

export const RemindersModal: React.FC<RemindersModalProps> = ({
  settings,
  onUpdateSettings,
  onSendTest,
  onDismiss,
}) => {
  const supportsNotif = typeof window !== 'undefined' && 'Notification' in window;
  const [permission, setPermission] = useState<NotificationPermission>(
    supportsNotif ? Notification.permission : 'denied'
  );

  const requestPermission = async () => {
    if (!supportsNotif) return;
    const result = await Notification.requestPermission().catch(() => 'denied' as NotificationPermission);
    setPermission(result);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-[2px]"
      onClick={onDismiss}
    >
      <div
        className="max-h-[88vh] w-full max-w-md overflow-y-auto rounded-3xl bg-white p-5 shadow-2xl animate-slide-up"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <BellRing className="h-5 w-5 text-gold-warm" />
            <h3 className="text-lg font-bold text-ink-deep">التنبيهات اليومية للتشجيع</h3>
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

        {supportsNotif && permission !== 'granted' && (
          <div className="mb-4 rounded-2xl bg-burgundy-container p-3.5">
            <p className="text-xs leading-relaxed text-burgundy">
              يرجى السماح بصلاحية الإشعارات في المتصفح لاستلام التذكيرات التشجيعية اليومية.
            </p>
            <button
              type="button"
              onClick={requestPermission}
              className="mt-2.5 rounded-xl bg-midnight px-4 py-2 text-xs font-bold text-white"
            >
              السماح بالإشعارات
            </button>
          </div>
        )}

        <div className="space-y-3">
          <ReminderToggleRow
            title="صلاة باكر والإنجيل"
            timeLabel={formatTimeArabic(settings.morningPrayerHour, settings.morningPrayerMinute)}
            checked={settings.morningPrayerEnabled}
            onChange={(v) => onUpdateSettings({ ...settings, morningPrayerEnabled: v })}
          />
          <ReminderToggleRow
            title="صلاة النوم والأجبية"
            timeLabel={formatTimeArabic(settings.nightPrayerHour, settings.nightPrayerMinute)}
            checked={settings.nightPrayerEnabled}
            onChange={(v) => onUpdateSettings({ ...settings, nightPrayerEnabled: v })}
          />
          <ReminderToggleRow
            title="القداس والتناول الأسبوعي"
            timeLabel={`الجمعة • ${formatTimeArabic(settings.liturgyHour, settings.liturgyMinute)}`}
            checked={settings.liturgyEnabled}
            onChange={(v) => onUpdateSettings({ ...settings, liturgyEnabled: v })}
          />
          <ReminderToggleRow
            title="اجتماع الخريجين والتسبحة"
            timeLabel={`الخميس • ${formatTimeArabic(settings.meetingHour, settings.meetingMinute)}`}
            checked={settings.meetingEnabled}
            onChange={(v) => onUpdateSettings({ ...settings, meetingEnabled: v })}
          />
          <ReminderToggleRow
            title="تشجيع وآية اليوم"
            timeLabel={formatTimeArabic(
              settings.dailyEncouragementHour,
              settings.dailyEncouragementMinute
            )}
            checked={settings.dailyEncouragementEnabled}
            onChange={(v) => onUpdateSettings({ ...settings, dailyEncouragementEnabled: v })}
          />
        </div>

        <button
          type="button"
          onClick={onSendTest}
          className="mt-4 w-full rounded-xl border border-midnight/30 py-2.5 text-sm font-bold text-midnight transition hover:bg-parchment-variant"
        >
          تجربة إشعار تشجيعي الآن
        </button>

        <div className="mt-3 flex justify-end">
          <button
            type="button"
            onClick={onDismiss}
            className="rounded-xl px-4 py-2 text-sm font-bold text-midnight hover:bg-parchment-variant"
          >
            تم
          </button>
        </div>
      </div>
    </div>
  );
};

interface ReminderToggleRowProps {
  title: string;
  timeLabel: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}

const ReminderToggleRow: React.FC<ReminderToggleRowProps> = ({
  title,
  timeLabel,
  checked,
  onChange,
}) => (
  <div className="flex items-center justify-between gap-3 rounded-xl border border-parchment-border px-3 py-2.5">
    <div>
      <p className="text-sm font-semibold text-ink-deep">{title}</p>
      <p className="text-xs text-ink-muted">{timeLabel}</p>
    </div>
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className={`relative inline-flex h-7 w-12 shrink-0 items-center rounded-full transition-colors ${
        checked ? 'bg-midnight' : 'bg-parchment-outline'
      }`}
    >
      <span
        className={`inline-block h-5 w-5 transform rounded-full bg-white shadow transition-transform ${
          checked ? '-translate-x-6' : '-translate-x-1'
        }`}
      />
    </button>
  </div>
);
