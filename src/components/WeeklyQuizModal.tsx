import React, { useState } from 'react';
import { Award, CheckCircle2, HelpCircle, Send, X, XCircle } from 'lucide-react';
import type { QuizQuestion } from '../lib/readingPlan';

interface WeeklyQuizModalProps {
  title: string;
  weekLabel: string;
  questions: QuizQuestion[];
  points: number;
  alreadySubmittedStatus: 'pending' | 'approved' | 'rejected' | null;
  onSubmitAnswers: (noteSummary: string) => void;
  onClose: () => void;
}

export const WeeklyQuizModal: React.FC<WeeklyQuizModalProps> = ({
  title,
  weekLabel,
  questions,
  points,
  alreadySubmittedStatus,
  onSubmitAnswers,
  onClose,
}) => {
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, number>>({});
  const [submittedResult, setSubmittedResult] = useState<{
    correctCount: number;
    total: number;
    percent: number;
    noteSummary: string;
  } | null>(null);

  const answeredCount = questions.filter((q) => selectedAnswers[q.id] !== undefined).length;
  const allAnswered = questions.length > 0 && answeredCount === questions.length;
  const isLocked = alreadySubmittedStatus === 'pending' || alreadySubmittedStatus === 'approved';

  const handleEvaluate = () => {
    let correctCount = 0;
    const parts: string[] = [];
    questions.forEach((q, idx) => {
      const pickedIdx = selectedAnswers[q.id];
      const isRight = pickedIdx === q.correctIndex;
      if (isRight) correctCount += 1;
      const pickedLabel = q.options[pickedIdx] ?? '-';
      parts.push(`س${idx + 1}: ${pickedLabel} (${isRight ? 'صحيح' : 'خطأ'})`);
    });
    const total = questions.length;
    const percent = total === 0 ? 0 : Math.round((correctCount / total) * 100);
    const noteSummary = `نتيجة مسابقة ${weekLabel} داخل التطبيق: ${correctCount}/${total} (${percent}%) • ${parts.join(' | ')}`;
    setSubmittedResult({ correctCount, total, percent, noteSummary });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/55 backdrop-blur-[2px] sm:items-center sm:p-4"
      onClick={onClose}
    >
      <div
        className="flex max-h-[90vh] w-full max-w-lg flex-col overflow-hidden rounded-t-3xl bg-white shadow-2xl animate-slide-up sm:rounded-3xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between gap-3 bg-midnight px-5 py-4 text-white">
          <div className="min-w-0 flex-1">
            <span className="inline-flex items-center gap-1 rounded-full bg-gold-warm/25 px-2.5 py-0.5 text-[11px] font-bold text-gold-bright">
              <HelpCircle className="h-3.5 w-3.5" />
              <span>{weekLabel} • +{points} نقطة</span>
            </span>
            <h3 className="mt-1 text-base font-bold leading-snug text-white">{title}</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="إغلاق"
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-white/80 hover:bg-white/10"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 space-y-4 overflow-y-auto p-5">
          {submittedResult && (
            <div className="rounded-2xl border border-gold-warm bg-status-pendingBg p-4 text-center">
              <Award className="mx-auto h-10 w-10 text-gold-warm" />
              <p className="mt-1 text-lg font-extrabold text-midnight-dark">
                نتيجتك: {submittedResult.correctCount} من {submittedResult.total} ({submittedResult.percent}%)
              </p>
              <p className="mt-1 text-xs leading-relaxed text-ink-deep">
                {submittedResult.percent >= 70
                  ? 'أحسنت! نتيجة رائعة في الإنجيل وكتاب «في تجسد الكلمة» ✨'
                  : 'شكراً لمشاركتك! راجع الإجابات الصحيحة بالأسفل وأرسل نتيجتك للخادم.'}
              </p>
            </div>
          )}

          {questions.map((q, qIdx) => {
            const picked = selectedAnswers[q.id];
            return (
              <div
                key={q.id}
                className="space-y-2.5 rounded-2xl border border-parchment-border bg-parchment/30 p-3.5"
              >
                <div className="flex items-start gap-2">
                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-midnight text-xs font-bold text-gold-bright">
                    {qIdx + 1}
                  </span>
                  <div className="flex-1">
                    <p className="text-sm font-bold leading-relaxed text-ink-deep">{q.prompt}</p>
                    <span className="text-[11px] font-semibold text-ink-muted">
                      {q.type === 'tf' ? 'صح أم خطأ' : 'اختيار من متعدد'}
                    </span>
                  </div>
                </div>

                <div className="space-y-2 pt-1">
                  {q.options.map((opt, oIdx) => {
                    const isSelected = picked === oIdx;
                    const isCorrect = oIdx === q.correctIndex;
                    let optionStyle =
                      'border-parchment-outline bg-white text-ink-deep hover:border-midnight/40';
                    if (submittedResult) {
                      if (isCorrect) {
                        optionStyle =
                          'border-status-approved bg-status-approved/10 text-status-approved font-bold';
                      } else if (isSelected && !isCorrect) {
                        optionStyle =
                          'border-status-rejected bg-status-rejectedBg text-status-rejected font-bold';
                      }
                    } else if (isSelected) {
                      optionStyle = 'border-midnight bg-midnight/10 text-midnight font-bold';
                    }

                    return (
                      <button
                        key={oIdx}
                        type="button"
                        disabled={Boolean(submittedResult)}
                        onClick={() =>
                          setSelectedAnswers((prev) => ({
                            ...prev,
                            [q.id]: oIdx,
                          }))
                        }
                        className={`flex min-h-[44px] w-full items-center justify-between gap-2 rounded-xl border px-3.5 py-2 text-right text-sm transition ${optionStyle}`}
                      >
                        <span className="flex-1">{opt}</span>
                        {submittedResult ? (
                          isCorrect ? (
                            <CheckCircle2 className="h-4 w-4 shrink-0 text-status-approved" />
                          ) : isSelected ? (
                            <XCircle className="h-4 w-4 shrink-0 text-status-rejected" />
                          ) : null
                        ) : (
                          <span
                            className={`h-4 w-4 shrink-0 rounded-full border-2 ${
                              isSelected
                                ? 'border-midnight bg-midnight'
                                : 'border-parchment-outline bg-white'
                            }`}
                          />
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>

        {/* Footer Actions */}
        <div className="border-t border-parchment-border bg-white p-4">
          {!submittedResult ? (
            <button
              type="button"
              disabled={!allAnswered}
              onClick={handleEvaluate}
              className="flex h-12 w-full items-center justify-center gap-2 rounded-2xl bg-midnight text-sm font-bold text-white shadow transition hover:bg-midnight-dark disabled:opacity-45"
            >
              <CheckCircle2 className="h-5 w-5" />
              <span>
                {allAnswered
                  ? 'تصحيح الإجابات وعرض النتيجة'
                  : `أجب على جميع الأسئلة (${answeredCount}/${questions.length})`}
              </span>
            </button>
          ) : isLocked ? (
            <button
              type="button"
              onClick={onClose}
              className="h-12 w-full rounded-2xl bg-parchment-variant text-sm font-bold text-ink-deep"
            >
              تم تسجيل المسابقة بالفعل لهذا الأسبوع — إغلاق
            </button>
          ) : (
            <button
              type="button"
              onClick={() => {
                onSubmitAnswers(submittedResult.noteSummary);
                onClose();
              }}
              className="flex h-12 w-full items-center justify-center gap-2 rounded-2xl bg-burgundy text-sm font-bold text-white shadow transition hover:opacity-95"
            >
              <Send className="h-4 w-4" />
              <span>إرسال الإجابات والنتيجة للخادم (+{points} نقطة)</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
