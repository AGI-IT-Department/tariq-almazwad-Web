import React from 'react';
import { CheckCircle2, History, Trophy } from 'lucide-react';
import type { YouthTab } from '../types';

interface BottomNavProps {
  selectedTab: YouthTab;
  onSelectTab: (tab: YouthTab) => void;
}

const TABS: Array<{ id: YouthTab; label: string; Icon: React.ComponentType<{ className?: string }> }> = [
  { id: 'daily', label: 'اليوميات', Icon: CheckCircle2 },
  { id: 'my_log', label: 'سجلي', Icon: History },
  { id: 'my_rank', label: 'مركزي', Icon: Trophy },
];

export const BottomNav: React.FC<BottomNavProps> = ({ selectedTab, onSelectTab }) => {
  return (
    <nav
      aria-label="التنقل الرئيسي"
      className="fixed bottom-0 inset-x-0 z-30 border-t border-parchment-border bg-white/95 backdrop-blur-md shadow-[0_-4px_18px_rgba(19,49,61,0.06)] safe-pb"
    >
      <div className="mx-auto grid max-w-[640px] grid-cols-3 px-2 pt-1.5">
        {TABS.map(({ id, label, Icon }) => {
          const active = selectedTab === id;
          return (
            <button
              key={id}
              type="button"
              onClick={() => onSelectTab(id)}
              className="flex min-h-[52px] flex-col items-center justify-center gap-1 rounded-2xl transition active:scale-95"
            >
              <span
                className={`flex h-8 w-14 items-center justify-center rounded-full transition-colors ${
                  active ? 'bg-gold-container text-midnight' : 'text-ink-muted'
                }`}
              >
                <Icon className="h-5 w-5" />
              </span>
              <span
                className={`text-xs font-semibold ${
                  active ? 'text-midnight' : 'text-ink-muted'
                }`}
              >
                {label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
