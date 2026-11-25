import React, { useMemo, useState } from 'react';
import { BellRing, BookOpen, CheckCircle2, Clock, ListChecks, X } from 'lucide-react';
import type { ReminderSettings } from '../types';
import { formatTimeArabic } from '../lib/fastCalendar';
import { BIBLE_BOOKS, getTodayBibleReading } from '../lib/bibleReadingPlan';
import {
  getNotificationPermission,
  isNotificationSupported,
  requestBrowserNotificationPermission,
} from '../lib/notifications';

interface RemindersModalProps {
  settings: ReminderSettings;
  onUpdateSettings: (settings: ReminderSettings) => void;
  onSendTestBible: () => void;
  onSendTest: () => void;
  onDismiss: () => void;
}

function toTimeInputValue(hour: number, minute: number): string {
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

function parseTimeInputValue(val: string): { hour: number; minute: number } | null {
  const parts = val.split(':');
  if (parts.length !== 2) return null;
  const hour = Number.parseInt(parts[0], 10);
  const minute = Number.parseInt(parts[1], 10);
  if (Number.isNaN(hour) || Number.isNaN(minute)) return null;
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
  return { hour, minute };
}

export const RemindersModal: React.FC<RemindersModalProps> = ({
  settings,
  onUpdateSettings,
  onSendTestBible,
  onSendTest,
  onDismiss,
}) => {
  const supportsNotif = isNotificationSupported();
  const [permission, setPermission] = useState<NotificationPermission>(
    getNotificationPermission()
  );

  const ntBooks = useMemo(() => BIBLE_BOOKS.filter((b) => b.testament === 'nt'), []);
  const otBooks = useMemo(() => BIBLE_BOOKS.filter((b) => b.testament === 'ot'), []);

  const todayReading = useMemo(
    () => getTodayBibleReading(settings.bibleBookId, settings.bibleStartDateIso),
    [settings.bibleBookId, settings.bibleStartDateIso]
  );

  const requestPermission = async () => {
    if (!supportsNotif) return;
    const result = await requestBrowserNotificationPermission();
    setPermission(result);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-[2px]"
      onClick={onDismiss}
    >
      <div
        className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-3xl bg-white p-5 shadow-2xl animate-slide-up"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <BellRing className="h-5 w-5 text-gold-warm" />
            <h3 className="text-lg font-bold text-ink-deep">الإشعارات وورد الكتاب المقدس</h3>
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
              يرجى السماح بصلاحية الإشعارات لاستلام تنبيه الإنجازات اليومية، وإصحاح الكتاب المقدس اليومي، واعتماد نقاطك الجديدة.
            </p>
            <button
              type="button"
              onClick={requestPermission}
              className="mt-2.5 rounded-xl bg-midnight px-4 py-2 text-xs font-bold text-white"
            >
              تفعيل الإشعارات الآن
            </button>
          </div>
        )}

        {supportsNotif && permission === 'granted' && (
          <div className="mb-4 flex items-center gap-2 rounded-2xl border border-status-approved/30 bg-status-approvedBg px-3.5 py-2.5 text-xs font-semibold text-status-approved">
            <CheckCircle2 className="h-4 w-4 shrink-0" />
            <span>الإشعارات مفعّلة حسب المواعيد التي تحددها أدناه</span>
          </div>
        )}

        <div className="space-y-3.5">
          {/* 1. Daily Bible Book & Chapter Plan */}
          <div className="rounded-2xl border border-gold-warm/50 bg-parchment/55 p-3.5">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <BookOpen className="h-4 w-4 text-midnight" />
                <div>
                  <p className="text-sm font-bold text-ink-deep">ورد الكتاب المقدس اليومي 📖</p>
                  <p className="text-[11px] text-ink-muted">
                    تنبيه يومي بالإصحاح الذي عليه الدور من السفر الذي تختاره
                  </p>
                </div>
              </div>
              <ToggleSwitch
                checked={settings.bibleReadingReminderEnabled}
                onChange={(v) => onUpdateSettings({ ...settings, bibleReadingReminderEnabled: v })}
              />
            </div>

            <div className="mt-3 space-y-2.5">
              <div>
                <label className="mb-1 block text-xs font-bold text-ink-deep">
                  اختر السفر للبداية من أول التحدي:
                </label>
                <select
                  value={settings.bibleBookId}
                  onChange={(e) =>
                    onUpdateSettings({ ...settings, bibleBookId: e.target.value })
                  }
                  className="w-full rounded-xl border border-parchment-outline bg-white px-3 py-2.5 text-xs font-semibold text-ink-deep outline-none focus:border-midnight"
                >
                  <optgroup label="العهد الجديد">
                    {ntBooks.map((book) => (
                      <option key={book.id} value={book.id}>
                        {book.nameAr} ({book.chapters} إصحاح)
                      </option>
                    ))}
                  </optgroup>
                  <optgroup label="العهد القديم">
                    {otBooks.map((book) => (
                      <option key={book.id} value={book.id}>
                        {book.nameAr} ({book.chapters} إصحاح)
                      </option>
                    ))}
                  </optgroup>
                </select>
              </div>

              <div className="flex items-center justify-between gap-2 rounded-xl bg-white px-3 py-2 border border-parchment-border">
                <span className="flex items-center gap-1.5 text-xs font-semibold text-ink-deep">
                  <Clock className="h-3.5 w-3.5 text-gold-warm" />
                  وقت تنبيه الإصحاح اليومي
                </span>
                <div className="flex items-center gap-2">
                  <span className="text-[11px] font-bold text-midnight">
                    {formatTimeArabic(settings.bibleReadingHour, settings.bibleReadingMinute)}
                  </span>
                  <input
                    type="time"
                    value={toTimeInputValue(settings.bibleReadingHour, settings.bibleReadingMinute)}
                    onChange={(e) => {
                      const parsed = parseTimeInputValue(e.target.value);
                      if (parsed) {
                        onUpdateSettings({
                          ...settings,
                          bibleReadingHour: parsed.hour,
                          bibleReadingMinute: parsed.minute,
                        });
                      }
                    }}
                    className="rounded-lg border border-parchment-outline bg-parchment/40 px-2 py-1 text-xs font-bold text-ink-deep outline-none focus:border-midnight"
                  />
                </div>
              </div>

              <div className="rounded-xl bg-midnight/5 px-3 py-2 text-[11px] leading-relaxed text-midnight-dark">
                <p className="font-bold">
                  إصحاح اليوم: {todayReading.bookNameAr} — الإصحاح {todayReading.chapterNumber}
                </p>
                <p className="mt-0.5 text-ink-muted">
                  يبدأ تلقائياً من الإصحاح 1 مع بداية التحدي (30 نوفمبر)، وعند انتهاء السفر ينتقل تلقائياً للسفر التالي.
                </p>
              </div>
            </div>
          </div>

          {/* 2. Daily Achievements Reminder at User-Chosen Time */}
          <div className="rounded-2xl border border-midnight/25 bg-midnight/[0.03] p-3.5">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <ListChecks className="h-4 w-4 text-midnight" />
                <div>
                  <p className="text-sm font-bold text-ink-deep">تنبيه الإنجازات اليومية ✅</p>
                  <p className="text-[11px] text-ink-muted">
                    يذكّرك بالمهام اليومية المتبقية في الوقت الذي تحدده بنفسك
                  </p>
                </div>
              </div>
              <ToggleSwitch
                checked={settings.dailyTasksReminderEnabled}
                onChange={(v) => onUpdateSettings({ ...settings, dailyTasksReminderEnabled: v })}
              />
            </div>

            <div className="mt-2.5 flex items-center justify-between gap-2 rounded-xl border border-parchment-border bg-white px-3 py-2">
              <span className="flex items-center gap-1.5 text-xs font-semibold text-ink-deep">
                <Clock className="h-3.5 w-3.5 text-gold-warm" />
                وقت التنبيه اليومي
              </span>
              <div className="flex items-center gap-2">
                <span className="text-[11px] font-bold text-midnight">
                  {formatTimeArabic(
                    settings.dailyTasksReminderHour,
                    settings.dailyTasksReminderMinute
                  )}
                </span>
                <input
                  type="time"
                  value={toTimeInputValue(
                    settings.dailyTasksReminderHour,
                    settings.dailyTasksReminderMinute
                  )}
                  onChange={(e) => {
                    const parsed = parseTimeInputValue(e.target.value);
                    if (parsed) {
                      onUpdateSettings({
                        ...settings,
                        dailyTasksReminderHour: parsed.hour,
                        dailyTasksReminderMinute: parsed.minute,
                      });
                    }
                  }}
                  className="rounded-lg border border-parchment-outline bg-parchment/40 px-2 py-1 text-xs font-bold text-ink-deep outline-none focus:border-midnight"
                />
              </div>
            </div>
          </div>

          {/* 3. Instant Points Approval Alerts */}
          <ReminderToggleRow
            title="إشعارات اعتماد النقاط الجديدة 🎉"
            timeLabel="تنبيه فوري عند اعتماد خدام التحدي لمهامك أو إضافة نقاط بونص"
            checked={settings.pointsApprovalEnabled}
            onChange={(v) => onUpdateSettings({ ...settings, pointsApprovalEnabled: v })}
          />

          {/* 4. Customizable Spiritual Reminders */}
          <p className="pt-1 text-xs font-bold text-ink-muted">تذكيرات روحية إضافية</p>

          <ReminderToggleRow
            title="صلاة باكر"
            timeLabel={formatTimeArabic(settings.morningPrayerHour, settings.morningPrayerMinute)}
            checked={settings.morningPrayerEnabled}
            onChange={(v) => onUpdateSettings({ ...settings, morningPrayerEnabled: v })}
            timeValue={toTimeInputValue(settings.morningPrayerHour, settings.morningPrayerMinute)}
            onTimeChange={(h, m) =>
              onUpdateSettings({ ...settings, morningPrayerHour: h, morningPrayerMinute: m })
            }
          />
          <ReminderToggleRow
            title="صلاة النوم والأجبية"
            timeLabel={formatTimeArabic(settings.nightPrayerHour, settings.nightPrayerMinute)}
            checked={settings.nightPrayerEnabled}
            onChange={(v) => onUpdateSettings({ ...settings, nightPrayerEnabled: v })}
            timeValue={toTimeInputValue(settings.nightPrayerHour, settings.nightPrayerMinute)}
            onTimeChange={(h, m) =>
              onUpdateSettings({ ...settings, nightPrayerHour: h, nightPrayerMinute: m })
            }
          />
          <ReminderToggleRow
            title="القداس والتناول الأسبوعي"
            timeLabel={`الجمعة • ${formatTimeArabic(settings.liturgyHour, settings.liturgyMinute)}`}
            checked={settings.liturgyEnabled}
            onChange={(v) => onUpdateSettings({ ...settings, liturgyEnabled: v })}
            timeValue={toTimeInputValue(settings.liturgyHour, settings.liturgyMinute)}
            onTimeChange={(h, m) =>
              onUpdateSettings({ ...settings, liturgyHour: h, liturgyMinute: m })
            }
          />
          <ReminderToggleRow
            title="اجتماع الخريجين والتسبحة"
            timeLabel={`الخميس • ${formatTimeArabic(settings.meetingHour, settings.meetingMinute)}`}
            checked={settings.meetingEnabled}
            onChange={(v) => onUpdateSettings({ ...settings, meetingEnabled: v })}
            timeValue={toTimeInputValue(settings.meetingHour, settings.meetingMinute)}
            onTimeChange={(h, m) =>
              onUpdateSettings({ ...settings, meetingHour: h, meetingMinute: m })
            }
          />
          <ReminderToggleRow
            title="تشجيع وآية اليوم"
            timeLabel={formatTimeArabic(
              settings.dailyEncouragementHour,
              settings.dailyEncouragementMinute
            )}
            checked={settings.dailyEncouragementEnabled}
            onChange={(v) => onUpdateSettings({ ...settings, dailyEncouragementEnabled: v })}
            timeValue={toTimeInputValue(
              settings.dailyEncouragementHour,
              settings.dailyEncouragementMinute
            )}
            onTimeChange={(h, m) =>
              onUpdateSettings({
                ...settings,
                dailyEncouragementHour: h,
                dailyEncouragementMinute: m,
              })
            }
          />
        </div>

        <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2">
          <button
            type="button"
            onClick={onSendTestBible}
            className="w-full rounded-xl bg-midnight py-2.5 px-3 text-xs font-bold text-white transition hover:bg-midnight-dark"
          >
            تجربة إشعار إصحاح اليوم 📖
          </button>
          <button
            type="button"
            onClick={onSendTest}
            className="w-full rounded-xl border border-midnight/30 py-2.5 px-3 text-xs font-bold text-midnight transition hover:bg-parchment-variant"
          >
            تجربة إشعار تشجيعي ✨
          </button>
        </div>

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

const ToggleSwitch: React.FC<{ checked: boolean; onChange: (checked: boolean) => void }> = ({
  checked,
  onChange,
}) => (
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
);

interface ReminderToggleRowProps {
  title: string;
  timeLabel: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  timeValue?: string;
  onTimeChange?: (hour: number, minute: number) => void;
}

const ReminderToggleRow: React.FC<ReminderToggleRowProps> = ({
  title,
  timeLabel,
  checked,
  onChange,
  timeValue,
  onTimeChange,
}) => (
  <div className="flex items-center justify-between gap-2 rounded-xl border border-parchment-border px-3 py-2.5">
    <div className="min-w-0 flex-1">
      <p className="text-sm font-semibold text-ink-deep">{title}</p>
      <p className="text-xs text-ink-muted">{timeLabel}</p>
    </div>
    {timeValue && onTimeChange && (
      <input
        type="time"
        value={timeValue}
        onChange={(e) => {
          const parsed = parseTimeInputValue(e.target.value);
          if (parsed) onTimeChange(parsed.hour, parsed.minute);
        }}
        className="rounded-lg border border-parchment-outline bg-parchment/40 px-2 py-1 text-xs font-bold text-ink-deep outline-none focus:border-midnight"
      />
    )}
    <ToggleSwitch checked={checked} onChange={onChange} />
  </div>
);
