import type { LeaderboardEntry } from '../types';
import { FastCalendar } from './fastCalendar';

const APP_SALT = 'tariq_almazwad_abu_sefein_sharjah_2026';

// Pure JS SHA-256 fallback for insecure HTTP contexts where crypto.subtle may be unavailable
function sha256Fallback(ascii: string): string {
  const rightRotate = (value: number, amount: number) =>
    (value >>> amount) | (value << (32 - amount));

  const mathPow = Math.pow;
  const maxWord = mathPow(2, 32);
  let result = '';

  const words: number[] = [];
  const utf8 = unescape(encodeURIComponent(ascii));
  const asciiBitLength = utf8.length * 8;

  const hash: number[] = [];
  const k: number[] = [];
  let primeCounter = 0;

  const isComposite: Record<number, boolean> = {};
  for (let candidate = 2; primeCounter < 64; candidate++) {
    if (!isComposite[candidate]) {
      for (let i = 0; i < 313; i += candidate) {
        isComposite[i] = true;
      }
      hash[primeCounter] = (mathPow(candidate, 0.5) * maxWord) | 0;
      k[primeCounter++] = (mathPow(candidate, 1 / 3) * maxWord) | 0;
    }
  }

  let msg = utf8 + '\x80';
  while ((msg.length % 64) - 56) msg += '\x00';
  for (let i = 0; i < msg.length; i++) {
    const j = msg.charCodeAt(i);
    words[i >> 2] |= j << (((3 - i) % 4) * 8);
  }
  words[words.length] = (asciiBitLength / maxWord) | 0;
  words[words.length] = asciiBitLength;

  for (let j = 0; j < words.length; ) {
    const w = words.slice(j, (j += 16));
    const oldHash = hash.slice(0);

    for (let i = 0; i < 64; i++) {
      const w15 = w[i - 15];
      const w2 = w[i - 2];

      const a = hash[0];
      const e = hash[4];
      const temp1 =
        hash[7] +
        (rightRotate(e, 6) ^ rightRotate(e, 11) ^ rightRotate(e, 25)) +
        ((e & hash[5]) ^ (~e & hash[6])) +
        k[i] +
        (w[i] =
          i < 16
            ? w[i]
            : (w[i - 16] +
                (rightRotate(w15, 7) ^ rightRotate(w15, 18) ^ (w15 >>> 3)) +
                w[i - 7] +
                (rightRotate(w2, 17) ^ rightRotate(w2, 19) ^ (w2 >>> 10))) |
              0);
      const temp2 =
        (rightRotate(a, 2) ^ rightRotate(a, 13) ^ rightRotate(a, 22)) +
        ((a & hash[1]) ^ (a & hash[2]) ^ (hash[1] & hash[2]));

      hash.unshift((temp1 + temp2) | 0);
      hash[4] = (hash[4] + temp1) | 0;
      hash.pop();
    }

    for (let i = 0; i < 8; i++) {
      hash[i] = (hash[i] + oldHash[i]) | 0;
    }
  }

  for (let i = 0; i < 8; i++) {
    for (let j = 3; j + 1; j--) {
      const b = (hash[i] >> (j * 8)) & 255;
      result += (b < 16 ? '0' : '') + b.toString(16);
    }
  }
  return result;
}

export const PinHasher = {
  async hashPin(pin: string): Promise<string> {
    const input = `${APP_SALT}:${pin.trim()}`;
    if (typeof crypto !== 'undefined' && crypto.subtle) {
      try {
        const data = new TextEncoder().encode(input);
        const buffer = await crypto.subtle.digest('SHA-256', data);
        return Array.from(new Uint8Array(buffer))
          .map((b) => b.toString(16).padStart(2, '0'))
          .join('');
      } catch {
        // fall through to JS fallback
      }
    }
    return sha256Fallback(input);
  },

  async matches(pin: string, storedHash: string): Promise<boolean> {
    if (!storedHash || !storedHash.trim()) return false;
    const hashed = await this.hashPin(pin);
    return hashed === storedHash || pin.trim() === storedHash;
  },
};

export const UrlValidator = {
  isSafeHttpUrl(rawUrl: string): boolean {
    const trimmed = (rawUrl || '').trim();
    if (!trimmed) return false;
    try {
      const parsed = new URL(trimmed);
      return (parsed.protocol === 'https:' || parsed.protocol === 'http:') && Boolean(parsed.hostname);
    } catch {
      return false;
    }
  },
};

function sanitizeCsvCell(raw: string): string {
  const cleaned = raw.replace(/\r/g, ' ').replace(/\n/g, ' ').trim();
  const safe =
    cleaned.length > 0 && ['=', '+', '-', '@'].includes(cleaned[0]) ? `'${cleaned}` : cleaned;
  const escaped = safe.replace(/"/g, '""');
  return `"${escaped}"`;
}

export const CsvExporter = {
  buildLeaderboardCsv(entries: LeaderboardEntry[]): string {
    const lines: string[] = [
      'الترتيب,الاسم بالكامل,رقم الهاتف,النقاط المعتمدة,نقاط البونص اليدوية,نقاط قيد المراجعة,عدد المهام المعتمدة,الأهلية للجائزة',
    ];
    for (const item of entries) {
      const prizeStatus =
        item.rank >= 1 &&
        item.rank <= FastCalendar.TOP_PRIZE_RANKS &&
        item.totalApprovedPoints > 0
          ? 'مؤهل للجائزة'
          : 'مشارك';
      lines.push(
        [
          String(item.rank),
          sanitizeCsvCell(item.user.fullName),
          sanitizeCsvCell(item.user.phone.trim() || '-'),
          String(item.totalApprovedPoints),
          String(item.user.bonusPoints),
          String(item.pendingPoints),
          String(item.approvedTasksCount),
          sanitizeCsvCell(prizeStatus),
        ].join(',')
      );
    }
    // UTF-8 BOM so Microsoft Excel renders Arabic cleanly
    return '\uFEFF' + lines.join('\r\n');
  },

  downloadLeaderboardCsv(entries: LeaderboardEntry[]): string {
    const csvContent = this.buildLeaderboardCsv(entries);
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'tariq_almazwad_report.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    return 'تم تنزيل ملف التقرير (tariq_almazwad_report.csv) بنجاح';
  },
};

export function openWhatsAppChat(rawPhone: string, message = ''): void {
  const digits = (rawPhone || '').replace(/\D/g, '');
  let normalized = digits;
  if (digits.startsWith('00')) {
    normalized = digits.slice(2);
  } else if (digits.startsWith('05') && digits.length === 10) {
    normalized = `971${digits.slice(1)}`;
  } else if (digits.startsWith('01') && digits.length === 11) {
    normalized = `20${digits.slice(1)}`;
  }
  if (!normalized) return;

  const url = message.trim()
    ? `https://wa.me/${normalized}?text=${encodeURIComponent(message)}`
    : `https://wa.me/${normalized}`;
  window.open(url, '_blank', 'noopener,noreferrer');
}

export function openSafeExternalUrl(url: string): void {
  if (!UrlValidator.isSafeHttpUrl(url)) return;
  window.open(url.trim(), '_blank', 'noopener,noreferrer');
}

export async function shareOrCopyText(text: string, title = 'تحدي طريق المزود'): Promise<string> {
  if (typeof navigator !== 'undefined' && navigator.share) {
    try {
      await navigator.share({ title, text });
      return 'تمت المشاركة بنجاح';
    } catch {
      // User dismissed share sheet or share failed; fall back to clipboard
    }
  }
  if (typeof navigator !== 'undefined' && navigator.clipboard) {
    try {
      await navigator.clipboard.writeText(text);
      return 'تم نسخ النص للمشاركة!';
    } catch {
      // ignore
    }
  }
  return 'تعذر النسخ التلقائي';
}

export function generateUuid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
