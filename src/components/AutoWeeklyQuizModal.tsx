import React, { useEffect, useMemo, useState } from 'react';
import {
  Award,
  Check,
  CheckCircle2,
  Eye,
  Filter,
  HelpCircle,
  Link2,
  Sparkles,
  X,
  XCircle,
} from 'lucide-react';
import {
  type AutoQuizDraft,
  type AutoQuizGradeResult,
  type AutoWeeklyQuiz,
  emptyAutoQuizDraft,
  getShuffledRightIndices,
  gradeAutoQuiz,
  loadAutoQuizDraft,
  saveAutoQuizDraft,
} from '../lib/autoContent';

type QuizSectionTab = 'mcq' | 'tf' | 'match';

interface AutoWeeklyQuizModalProps {
  quiz: AutoWeeklyQuiz;
  userId: string;
  adminPreview?: boolean;
  completedPoints?: number | null;
  onSubmitResult?: (
    weekNumber: number,
    correctCount: number,
    totalItems: number,
    earnedPoints: number
  ) => Promise<boolean>;
  onClose: () => void;
}

const ARABIC_OPTION_LETTERS = ['أ', 'ب', 'ج', 'د'];

export const AutoWeeklyQuizModal: React.FC<AutoWeeklyQuizModalProps> = ({
  quiz,
  userId,
  adminPreview = false,
  completedPoints = null,
  onSubmitResult,
  onClose,
}) => {
  const isReadOnly = adminPreview || completedPoints !== null;
  const [section, setSection] = useState<QuizSectionTab>('mcq');
  const [onlyUnanswered, setOnlyUnanswered] = useState(false);
  const [draft, setDraft] = useState<AutoQuizDraft>(() =>
    isReadOnly ? emptyAutoQuizDraft() : loadAutoQuizDraft(userId, quiz.weekNumber)
  );
  const [activeMatchLeft, setActiveMatchLeft] = useState<Record<string, number | null>>({});
  const [submittedResult, setSubmittedResult] = useState<AutoQuizGradeResult | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [confirmIncomplete, setConfirmIncomplete] = useState(false);

  const showAnswers = isReadOnly || submittedResult !== null;

  // Persist draft automatically as user answers
  useEffect(() => {
    if (!isReadOnly && submittedResult === null && userId) {
      saveAutoQuizDraft(userId, quiz.weekNumber, draft);
    }
  }, [draft, isReadOnly, submittedResult, userId, quiz.weekNumber]);

  const progress = useMemo(() => {
    const mcqAnswered = quiz.mcq.filter((q) => typeof draft.mcqAnswers[q.id] === 'number').length;
    const tfAnswered = quiz.tf.filter((q) => typeof draft.tfAnswers[q.id] === 'boolean').length;
    let matchAnswered = 0;
    let matchTotal = 0;
    for (const g of quiz.match) {
      g.pairs.forEach((_, idx) => {
        matchTotal++;
        if (typeof draft.matchAnswers[`${g.id}:${idx}`] === 'number') {
          matchAnswered++;
        }
      });
    }
    const total = quiz.mcq.length + quiz.tf.length + matchTotal;
    const answered = mcqAnswered + tfAnswered + matchAnswered;
    return {
      mcqAnswered,
      mcqTotal: quiz.mcq.length,
      tfAnswered,
      tfTotal: quiz.tf.length,
      matchAnswered,
      matchTotal,
      answered,
      total,
      percent: total > 0 ? Math.round((answered / total) * 100) : 0,
    };
  }, [quiz, draft]);

  const shuffledByGroup = useMemo(() => {
    const map: Record<string, number[]> = {};
    for (const g of quiz.match) {
      map[g.id] = getShuffledRightIndices(g.id, g.pairs.length);
    }
    return map;
  }, [quiz]);

  const handleSelectMcq = (questionId: string, optionIdx: number) => {
    if (showAnswers) return;
    setDraft((prev) => ({
      ...prev,
      mcqAnswers: { ...prev.mcqAnswers, [questionId]: optionIdx },
      updatedAt: Date.now(),
    }));
  };

  const handleSelectTf = (questionId: string, value: boolean) => {
    if (showAnswers) return;
    setDraft((prev) => ({
      ...prev,
      tfAnswers: { ...prev.tfAnswers, [questionId]: value },
      updatedAt: Date.now(),
    }));
  };

  const handleTapLeftMatch = (groupId: string, leftIdx: number) => {
    if (showAnswers) return;
    setActiveMatchLeft((prev) => ({
      ...prev,
      [groupId]: prev[groupId] === leftIdx ? null : leftIdx,
    }));
  };

  const handleTapRightMatch = (groupId: string, originalRightIdx: number) => {
    if (showAnswers) return;
    const activeLeft = activeMatchLeft[groupId];
    if (activeLeft === null || activeLeft === undefined) {
      // Auto-select the first unmatched left item in this group
      const group = quiz.match.find((g) => g.id === groupId);
      if (!group) return;
      const firstUnmatched = group.pairs.findIndex(
        (_, idx) => typeof draft.matchAnswers[`${groupId}:${idx}`] !== 'number'
      );
      if (firstUnmatched < 0) return;
      assignMatchPair(groupId, firstUnmatched, originalRightIdx);
      return;
    }
    assignMatchPair(groupId, activeLeft, originalRightIdx);
  };

  const assignMatchPair = (groupId: string, leftIdx: number, originalRightIdx: number) => {
    setDraft((prev) => {
      const nextMatch = { ...prev.matchAnswers };
      // Ensure 1-to-1 matching within the same group
      for (const [k, v] of Object.entries(nextMatch)) {
        if (k.startsWith(`${groupId}:`) && v === originalRightIdx) {
          delete nextMatch[k];
        }
      }
      nextMatch[`${groupId}:${leftIdx}`] = originalRightIdx;
      return { ...prev, matchAnswers: nextMatch, updatedAt: Date.now() };
    });

    // Auto-advance active left item to next unmatched item in this group
    const group = quiz.match.find((g) => g.id === groupId);
    if (group) {
      const nextUnmatched = group.pairs.findIndex(
        (_, idx) => idx !== leftIdx && typeof draft.matchAnswers[`${groupId}:${idx}`] !== 'number'
      );
      setActiveMatchLeft((prev) => ({
        ...prev,
        [groupId]: nextUnmatched >= 0 ? nextUnmatched : null,
      }));
    }
  };

  const clearMatchLeft = (groupId: string, leftIdx: number) => {
    if (showAnswers) return;
    setDraft((prev) => {
      const nextMatch = { ...prev.matchAnswers };
      delete nextMatch[`${groupId}:${leftIdx}`];
      return { ...prev, matchAnswers: nextMatch, updatedAt: Date.now() };
    });
  };

  const finalizeSubmit = async () => {
    if (isSubmitting || !onSubmitResult) return;
    setIsSubmitting(true);
    setConfirmIncomplete(false);
    const result = gradeAutoQuiz(quiz, draft);
    const ok = await onSubmitResult(
      quiz.weekNumber,
      result.correctCount,
      result.totalItems,
      result.earnedPoints
    );
    setIsSubmitting(false);
    if (ok) {
      setSubmittedResult(result);
    }
  };

  const handleRequestSubmit = () => {
    if (progress.answered < progress.total) {
      setConfirmIncomplete(true);
    } else {
      void finalizeSubmit();
    }
  };

  const filteredMcq = useMemo(
    () =>
      onlyUnanswered && !showAnswers
        ? quiz.mcq.filter((q) => typeof draft.mcqAnswers[q.id] !== 'number')
        : quiz.mcq,
    [quiz.mcq, onlyUnanswered, showAnswers, draft.mcqAnswers]
  );

  const filteredTf = useMemo(
    () =>
      onlyUnanswered && !showAnswers
        ? quiz.tf.filter((q) => typeof draft.tfAnswers[q.id] !== 'boolean')
        : quiz.tf,
    [quiz.tf, onlyUnanswered, showAnswers, draft.tfAnswers]
  );

  return (
    <div
      className="fixed inset-0 z-50 flex flex-col bg-[#F9F6F0]"
      dir="rtl"
      role="dialog"
      aria-modal="true"
    >
      {/* Top sticky header */}
      <div className="bg-gradient-to-l from-[#10323F] to-[#1A4B5C] text-white px-4 pt-4 pb-3 shadow-md shrink-0">
        <div className="max-w-3xl mx-auto">
          <div className="flex items-start justify-between gap-3">
            <div className="flex-1 min-w-0">
              <div className="flex flex-wrap items-center gap-2 mb-1">
                <span className="inline-flex items-center gap-1 rounded-full bg-[#C99B3B]/25 border border-[#C99B3B]/45 px-2.5 py-0.5 text-[11px] font-bold text-[#F3DA9E]">
                  <Sparkles className="w-3 h-3" />
                  {quiz.totalItems} سؤال • {quiz.maxPoints} نقطة ({quiz.pointsPerItem} نقطة/سؤال)
                </span>
                {adminPreview && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-white/15 px-2.5 py-0.5 text-[11px] font-bold text-white">
                    <Eye className="w-3 h-3" />
                    معاينة الخدام (الإجابات الصحيحة ظاهرة)
                  </span>
                )}
                {completedPoints !== null && !adminPreview && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/25 border border-emerald-400/50 px-2.5 py-0.5 text-[11px] font-bold text-emerald-100">
                    <CheckCircle2 className="w-3 h-3" />
                    نتيجتك المسجلة: {completedPoints} / {quiz.maxPoints} نقطة
                  </span>
                )}
              </div>
              <h2 className="text-lg font-extrabold text-white leading-snug">{quiz.titleAr}</h2>
              <p className="text-xs text-[#D3E5EC] mt-0.5 leading-relaxed">{quiz.subtitleAr}</p>
            </div>
            <button
              type="button"
              onClick={onClose}
              aria-label="إغلاق المسابقة"
              className="w-10 h-10 rounded-2xl bg-white/15 hover:bg-white/25 flex items-center justify-center text-white shrink-0"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Progress bar when active */}
          {!isReadOnly && submittedResult === null && (
            <div className="mt-3">
              <div className="flex items-center justify-between text-xs text-[#E4F0F4] mb-1">
                <span>
                  تمت الإجابة على{' '}
                  <strong className="text-[#F3DA9E]">
                    {progress.answered} / {progress.total}
                  </strong>{' '}
                  سؤال (يُحفظ تقدمك تلقائياً)
                </span>
                <span className="font-bold text-[#F3DA9E]">{progress.percent}%</span>
              </div>
              <div className="h-2 w-full rounded-full bg-white/15 overflow-hidden">
                <div
                  className="h-full rounded-full bg-gradient-to-l from-[#E8BF63] to-[#C99B3B] transition-all duration-300"
                  style={{ width: `${progress.percent}%` }}
                />
              </div>
            </div>
          )}

          {/* Section tabs */}
          <div className="mt-3 grid grid-cols-3 gap-2">
            <button
              type="button"
              onClick={() => setSection('mcq')}
              className={`rounded-xl py-2 px-2 text-xs font-extrabold transition ${
                section === 'mcq'
                  ? 'bg-[#C99B3B] text-[#10323F] shadow-sm'
                  : 'bg-white/10 text-white hover:bg-white/20'
              }`}
            >
              اختيار من متعدد ({isReadOnly ? quiz.mcq.length : `${progress.mcqAnswered}/${progress.mcqTotal}`})
            </button>
            <button
              type="button"
              onClick={() => setSection('tf')}
              className={`rounded-xl py-2 px-2 text-xs font-extrabold transition ${
                section === 'tf'
                  ? 'bg-[#C99B3B] text-[#10323F] shadow-sm'
                  : 'bg-white/10 text-white hover:bg-white/20'
              }`}
            >
              صح وخطأ ({isReadOnly ? quiz.tf.length : `${progress.tfAnswered}/${progress.tfTotal}`})
            </button>
            <button
              type="button"
              onClick={() => setSection('match')}
              className={`rounded-xl py-2 px-2 text-xs font-extrabold transition ${
                section === 'match'
                  ? 'bg-[#C99B3B] text-[#10323F] shadow-sm'
                  : 'bg-white/10 text-white hover:bg-white/20'
              }`}
            >
              توصيل ({isReadOnly ? progress.matchTotal : `${progress.matchAnswered}/${progress.matchTotal}`})
            </button>
          </div>
        </div>
      </div>

      {/* Scrollable Body */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        <div className="max-w-3xl mx-auto space-y-4 pb-24">
          {/* Score Result Card right after auto-grading */}
          {submittedResult && (
            <div className="rounded-3xl bg-gradient-to-l from-[#10323F] to-[#1A4B5C] p-5 text-white shadow-lg border border-[#C99B3B]/40">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-12 h-12 rounded-2xl bg-[#C99B3B]/25 border border-[#C99B3B] flex items-center justify-center text-[#F3DA9E] shrink-0">
                  <Award className="w-7 h-7" />
                </div>
                <div>
                  <div className="text-xs font-bold text-[#F3DA9E]">
                    تم التصحيح التلقائي وإضافة النقاط مباشرة لرصيدك ✅
                  </div>
                  <div className="text-2xl font-black text-white">
                    {submittedResult.earnedPoints} / {submittedResult.maxPoints} نقطة
                  </div>
                  <div className="text-xs text-[#D3E5EC]">
                    {submittedResult.correctCount} إجابة صحيحة من {submittedResult.totalItems} سؤال (
                    {submittedResult.percent}%)
                  </div>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-2 text-center">
                <div className="rounded-xl bg-white/10 p-2">
                  <div className="text-[11px] text-[#D3E5EC]">اختيار من متعدد</div>
                  <div className="text-sm font-extrabold text-[#F3DA9E]">
                    {submittedResult.mcqCorrect} / {submittedResult.mcqTotal}
                  </div>
                </div>
                <div className="rounded-xl bg-white/10 p-2">
                  <div className="text-[11px] text-[#D3E5EC]">صح وخطأ</div>
                  <div className="text-sm font-extrabold text-[#F3DA9E]">
                    {submittedResult.tfCorrect} / {submittedResult.tfTotal}
                  </div>
                </div>
                <div className="rounded-xl bg-white/10 p-2">
                  <div className="text-[11px] text-[#D3E5EC]">التوصيل</div>
                  <div className="text-sm font-extrabold text-[#F3DA9E]">
                    {submittedResult.matchCorrect} / {submittedResult.matchTotal}
                  </div>
                </div>
              </div>
              <p className="text-xs text-[#E4F0F4] mt-3">
                يمكنك الآن مراجعة جميع إجاباتك والإجابات الصحيحة أدناه عبر التبويبات.
              </p>
            </div>
          )}

          {/* Unanswered filter toggle */}
          {!showAnswers && section !== 'match' && (
            <div className="flex items-center justify-between rounded-2xl bg-white border border-[#E5DEC9] px-3.5 py-2.5">
              <div className="flex items-center gap-2 text-xs font-bold text-[#10323F]">
                <Filter className="w-4 h-4 text-[#C99B3B]" />
                <span>تصفية الأسئلة غير المجاب عليها فقط</span>
              </div>
              <button
                type="button"
                onClick={() => setOnlyUnanswered((v) => !v)}
                className={`px-3 py-1 rounded-xl text-xs font-bold transition ${
                  onlyUnanswered
                    ? 'bg-[#10323F] text-white'
                    : 'bg-[#F4EFE4] text-[#5C6B73]'
                }`}
              >
                {onlyUnanswered ? 'مفعّل ✓' : 'الكل'}
              </button>
            </div>
          )}

          {/* SECTION 1: MCQ (40) */}
          {section === 'mcq' && (
            <div className="space-y-3">
              {filteredMcq.length === 0 ? (
                <div className="rounded-2xl bg-emerald-50 border border-emerald-200 p-6 text-center text-sm font-bold text-emerald-800">
                  أحسنت! أجبت على جميع أسئلة الاختيار من متعدد ({quiz.mcq.length}/{quiz.mcq.length}) ✅
                </div>
              ) : (
                filteredMcq.map((item) => {
                  const originalNum = quiz.mcq.findIndex((q) => q.id === item.id) + 1;
                  const picked = draft.mcqAnswers[item.id];
                  return (
                    <div
                      key={item.id}
                      className="rounded-2xl bg-white border border-[#E5DEC9] p-4 shadow-sm"
                    >
                      <div className="flex items-start gap-2.5 mb-3">
                        <span className="w-7 h-7 rounded-xl bg-[#10323F] text-[#F3DA9E] text-xs font-black flex items-center justify-center shrink-0">
                          {originalNum}
                        </span>
                        <div className="flex-1">
                          <p className="text-sm font-extrabold text-[#10323F] leading-relaxed">
                            {item.q}
                          </p>
                          <span className="text-[11px] font-bold text-[#A57A20]">
                            ({quiz.pointsPerItem} نقطتان)
                          </span>
                        </div>
                      </div>
                      <div className="space-y-2">
                        {item.o.map((opt, optIdx) => {
                          const isPicked = picked === optIdx;
                          const isCorrect = item.a === optIdx;
                          let style =
                            'bg-[#FAF8F4] border-[#E6DFCE] text-[#1D2D35] hover:border-[#C99B3B]';
                          if (showAnswers) {
                            if (isCorrect) {
                              style =
                                'bg-emerald-50 border-emerald-500 text-emerald-900 font-bold';
                            } else if (isPicked && !isCorrect) {
                              style = 'bg-rose-50 border-rose-400 text-rose-900 font-bold';
                            }
                          } else if (isPicked) {
                            style = 'bg-[#10323F] border-[#10323F] text-white font-bold shadow-sm';
                          }
                          return (
                            <button
                              key={optIdx}
                              type="button"
                              disabled={showAnswers}
                              onClick={() => handleSelectMcq(item.id, optIdx)}
                              className={`w-full text-right rounded-xl border px-3.5 py-2.5 text-sm transition flex items-center justify-between gap-2 min-h-[46px] ${style}`}
                            >
                              <div className="flex items-center gap-2.5">
                                <span
                                  className={`w-6 h-6 rounded-lg text-xs font-extrabold flex items-center justify-center shrink-0 ${
                                    isPicked && !showAnswers
                                      ? 'bg-[#C99B3B] text-[#10323F]'
                                      : showAnswers && isCorrect
                                        ? 'bg-emerald-600 text-white'
                                        : 'bg-[#ECE6D6] text-[#10323F]'
                                  }`}
                                >
                                  {ARABIC_OPTION_LETTERS[optIdx] ?? optIdx + 1}
                                </span>
                                <span className="leading-snug">{opt}</span>
                              </div>
                              {showAnswers && isCorrect && (
                                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                              )}
                              {showAnswers && isPicked && !isCorrect && (
                                <XCircle className="w-4 h-4 text-rose-600 shrink-0" />
                              )}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          )}

          {/* SECTION 2: TRUE / FALSE (40) */}
          {section === 'tf' && (
            <div className="space-y-3">
              {filteredTf.length === 0 ? (
                <div className="rounded-2xl bg-emerald-50 border border-emerald-200 p-6 text-center text-sm font-bold text-emerald-800">
                  أحسنت! أجبت على جميع أسئلة الصح والخطأ ({quiz.tf.length}/{quiz.tf.length}) ✅
                </div>
              ) : (
                filteredTf.map((item) => {
                  const originalNum = quiz.mcq.length + quiz.tf.findIndex((q) => q.id === item.id) + 1;
                  const picked = draft.tfAnswers[item.id];
                  return (
                    <div
                      key={item.id}
                      className="rounded-2xl bg-white border border-[#E5DEC9] p-4 shadow-sm"
                    >
                      <div className="flex items-start gap-2.5 mb-3">
                        <span className="w-7 h-7 rounded-xl bg-[#7A1E2C] text-white text-xs font-black flex items-center justify-center shrink-0">
                          {originalNum}
                        </span>
                        <div className="flex-1">
                          <p className="text-sm font-extrabold text-[#10323F] leading-relaxed">
                            {item.q}
                          </p>
                          <span className="text-[11px] font-bold text-[#A57A20]">
                            ({quiz.pointsPerItem} نقطتان)
                          </span>
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-2.5">
                        {[
                          { label: 'صح ✓', value: true },
                          { label: 'خطأ ✗', value: false },
                        ].map((choice) => {
                          const isPicked = picked === choice.value;
                          const isCorrect = item.a === choice.value;
                          let style =
                            'bg-[#FAF8F4] border-[#E6DFCE] text-[#1D2D35] hover:border-[#C99B3B]';
                          if (showAnswers) {
                            if (isCorrect) {
                              style = 'bg-emerald-50 border-emerald-500 text-emerald-900 font-extrabold';
                            } else if (isPicked && !isCorrect) {
                              style = 'bg-rose-50 border-rose-400 text-rose-900 font-extrabold';
                            }
                          } else if (isPicked) {
                            style = 'bg-[#10323F] border-[#10323F] text-white font-extrabold shadow-sm';
                          }
                          return (
                            <button
                              key={String(choice.value)}
                              type="button"
                              disabled={showAnswers}
                              onClick={() => handleSelectTf(item.id, choice.value)}
                              className={`rounded-xl border py-3 px-3 text-sm transition flex items-center justify-center gap-1.5 min-h-[48px] ${style}`}
                            >
                              <span>{choice.label}</span>
                              {showAnswers && isCorrect && (
                                <Check className="w-4 h-4 text-emerald-600" />
                              )}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          )}

          {/* SECTION 3: MATCHING (5 groups x 4 pairs = 20 items) */}
          {section === 'match' && (
            <div className="space-y-4">
              {!showAnswers && (
                <div className="rounded-2xl bg-[#FFF9EC] border border-[#E5CF97] px-4 py-3 text-xs text-[#6B4E14] font-semibold flex items-center gap-2">
                  <Link2 className="w-4 h-4 text-[#B78722] shrink-0" />
                  <span>
                    طريقة الحل: اضغط على العبارة من العمود (أ) ثم اضغط على الإجابة المناسبة لها من
                    القائمة (ب) ليتم التوصيل تلقائياً (كل زوج صحيح = نقطتان).
                  </span>
                </div>
              )}

              {quiz.match.map((group, groupIdx) => {
                const shuffledRight = shuffledByGroup[group.id] ?? group.pairs.map((_, i) => i);
                const activeLeft = activeMatchLeft[group.id] ?? null;
                const usedRightToLeft = new Map<number, number>();
                group.pairs.forEach((_, leftIdx) => {
                  const chosenRight = draft.matchAnswers[`${group.id}:${leftIdx}`];
                  if (typeof chosenRight === 'number') {
                    usedRightToLeft.set(chosenRight, leftIdx);
                  }
                });

                return (
                  <div
                    key={group.id}
                    className="rounded-3xl bg-white border border-[#E5DEC9] p-4 shadow-sm space-y-3"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <span className="px-2.5 py-1 rounded-xl bg-[#10323F] text-[#F3DA9E] text-xs font-extrabold">
                          مجموعة {groupIdx + 1}
                        </span>
                        <h3 className="text-sm font-extrabold text-[#10323F]">{group.title}</h3>
                      </div>
                      <span className="text-[11px] font-bold text-[#A57A20] shrink-0">
                        {group.pairs.length * quiz.pointsPerItem} نقاط
                      </span>
                    </div>

                    {/* Left items (column A) */}
                    <div className="space-y-2">
                      <div className="text-xs font-bold text-[#5C6B73]">العمود (أ) — اختر العنصر:</div>
                      {group.pairs.map((pair, leftIdx) => {
                        const chosenRightIdx = draft.matchAnswers[`${group.id}:${leftIdx}`];
                        const isMatched = typeof chosenRightIdx === 'number';
                        const isSelected = activeLeft === leftIdx;
                        const matchedShuffledPos = isMatched
                          ? shuffledRight.indexOf(chosenRightIdx)
                          : -1;
                        const isRightAnswer = isMatched && chosenRightIdx === leftIdx;

                        let cardCls =
                          'bg-[#FAF8F4] border-[#E6DFCE] text-[#10323F] hover:border-[#C99B3B]';
                        if (showAnswers) {
                          if (adminPreview || completedPoints !== null) {
                            cardCls = 'bg-emerald-50/70 border-emerald-300 text-[#10323F]';
                          } else {
                            cardCls = isRightAnswer
                              ? 'bg-emerald-50 border-emerald-500 text-emerald-950'
                              : 'bg-rose-50 border-rose-400 text-rose-950';
                          }
                        } else if (isSelected) {
                          cardCls = 'bg-[#10323F] border-[#10323F] text-white shadow-md';
                        } else if (isMatched) {
                          cardCls = 'bg-[#FDF6E3] border-[#C99B3B] text-[#10323F]';
                        }

                        return (
                          <div
                            key={leftIdx}
                            onClick={() => handleTapLeftMatch(group.id, leftIdx)}
                            className={`rounded-xl border p-3 transition cursor-pointer ${cardCls}`}
                          >
                            <div className="flex items-start justify-between gap-2">
                              <div className="flex items-start gap-2 flex-1">
                                <span
                                  className={`w-6 h-6 rounded-lg text-xs font-black flex items-center justify-center shrink-0 ${
                                    isSelected && !showAnswers
                                      ? 'bg-[#C99B3B] text-[#10323F]'
                                      : 'bg-[#10323F]/10 text-[#10323F]'
                                  }`}
                                >
                                  {leftIdx + 1}
                                </span>
                                <span className="text-sm font-bold leading-snug">{pair.l}</span>
                              </div>
                              {!showAnswers && isMatched && (
                                <button
                                  type="button"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    clearMatchLeft(group.id, leftIdx);
                                  }}
                                  className="inline-flex items-center gap-1 rounded-lg bg-[#C99B3B] text-[#10323F] px-2 py-0.5 text-[11px] font-extrabold shrink-0"
                                >
                                  <span>← {ARABIC_OPTION_LETTERS[matchedShuffledPos] ?? ''}</span>
                                  <X className="w-3 h-3" />
                                </button>
                              )}
                            </div>

                            {!showAnswers && isMatched && (
                              <div
                                className={`mt-1.5 text-xs pr-8 ${
                                  isSelected ? 'text-[#F3DA9E]' : 'text-[#7A5B18] font-semibold'
                                }`}
                              >
                                تم توصيله بـ: {group.pairs[chosenRightIdx]?.r}
                              </div>
                            )}

                            {showAnswers && (
                              <div className="mt-2 pr-8 space-y-1 text-xs">
                                {submittedResult && isMatched && !isRightAnswer && (
                                  <div className="text-rose-700 font-bold">
                                    ✗ إجابتك: {group.pairs[chosenRightIdx]?.r}
                                  </div>
                                )}
                                <div className="text-emerald-800 font-extrabold">
                                  ✓ الإجابة الصحيحة: {pair.r}
                                </div>
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>

                    {/* Right options (column B) - interactive when solving */}
                    {!showAnswers && (
                      <div className="space-y-2 pt-2 border-t border-[#EFE8D6]">
                        <div className="text-xs font-bold text-[#5C6B73]">
                          العمود (ب) — اضغط على الإجابة المناسبة
                          {activeLeft !== null
                            ? ` للعنصر رقم (${activeLeft + 1}):`
                            : ' (أو اختر عنصراً من الأعلى أولاً):'}
                        </div>
                        <div className="grid grid-cols-1 gap-2">
                          {shuffledRight.map((origIdx, displayIdx) => {
                            const pairedWithLeft = usedRightToLeft.get(origIdx);
                            const isUsed = pairedWithLeft !== undefined;
                            return (
                              <button
                                key={origIdx}
                                type="button"
                                onClick={() => handleTapRightMatch(group.id, origIdx)}
                                className={`w-full text-right rounded-xl border px-3 py-2.5 text-sm transition flex items-center justify-between gap-2 min-h-[46px] ${
                                  isUsed
                                    ? 'bg-[#F4EFE2] border-[#C99B3B]/60 text-[#5C533D]'
                                    : activeLeft !== null
                                      ? 'bg-[#FFFDF8] border-[#C99B3B] text-[#10323F] font-semibold hover:bg-[#FDF6E3]'
                                      : 'bg-[#FAF8F4] border-[#E6DFCE] text-[#1D2D35] hover:border-[#C99B3B]'
                                }`}
                              >
                                <div className="flex items-start gap-2">
                                  <span className="w-6 h-6 rounded-lg bg-[#ECE6D6] text-[#10323F] text-xs font-extrabold flex items-center justify-center shrink-0">
                                    {ARABIC_OPTION_LETTERS[displayIdx] ?? displayIdx + 1}
                                  </span>
                                  <span className="leading-snug">{group.pairs[origIdx]?.r}</span>
                                </div>
                                {isUsed && (
                                  <span className="rounded-lg bg-[#10323F] text-[#F3DA9E] px-2 py-0.5 text-[11px] font-extrabold shrink-0">
                                    مربوط بـ {pairedWithLeft + 1}
                                  </span>
                                )}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Bottom Sticky Action Bar */}
      <div className="bg-white border-t border-[#E5DEC9] px-4 py-3 shadow-lg shrink-0">
        <div className="max-w-3xl mx-auto flex items-center justify-between gap-3">
          {!isReadOnly && submittedResult === null ? (
            <>
              <div className="text-xs text-[#5C6B73]">
                <div className="font-extrabold text-[#10323F]">
                  أجبت على {progress.answered} / {progress.total}
                </div>
                <div>التصحيح والإضافة للرصيد فوريين</div>
              </div>
              <button
                type="button"
                disabled={isSubmitting || progress.answered === 0}
                onClick={handleRequestSubmit}
                className="rounded-2xl bg-gradient-to-l from-[#10323F] to-[#1A4B5C] hover:opacity-95 disabled:opacity-50 text-white px-5 py-3 text-sm font-extrabold shadow-md flex items-center gap-2 min-h-[48px]"
              >
                <CheckCircle2 className="w-4 h-4 text-[#F3DA9E]" />
                <span>{isSubmitting ? 'جارٍ التصحيح والحفظ...' : 'إنهاء وتصحيح المسابقة'}</span>
              </button>
            </>
          ) : (
            <button
              type="button"
              onClick={onClose}
              className="w-full rounded-2xl bg-[#10323F] text-white py-3 text-sm font-extrabold min-h-[48px]"
            >
              العودة إلى التطبيق
            </button>
          )}
        </div>
      </div>

      {/* Confirm Incomplete Modal */}
      {confirmIncomplete && (
        <div className="fixed inset-0 z-[60] bg-black/55 flex items-center justify-center p-4">
          <div className="w-full max-w-md rounded-3xl bg-white p-5 shadow-2xl space-y-4">
            <div className="flex items-center gap-2.5 text-[#10323F]">
              <HelpCircle className="w-6 h-6 text-[#C99B3B] shrink-0" />
              <h3 className="text-base font-extrabold">تأكيد تسليم المسابقة</h3>
            </div>
            <p className="text-sm text-[#3F4E56] leading-relaxed">
              لقد أجبت على <strong>{progress.answered}</strong> سؤالاً من أصل{' '}
              <strong>{progress.total}</strong>، ومتبقي{' '}
              <strong className="text-rose-700">{progress.total - progress.answered}</strong> سؤال
              بدون إجابة. بعد التسليم يتم التصحيح التلقائي وإضافة النقاط مباشرة لرصيدك ولا يمكن
              إعادة المسابقة لنفس الأسبوع.
            </p>
            <div className="flex items-center gap-2.5">
              <button
                type="button"
                onClick={() => setConfirmIncomplete(false)}
                className="flex-1 rounded-2xl bg-[#F4EFE4] text-[#10323F] py-3 text-sm font-extrabold"
              >
                إكمال الأسئلة أولاً
              </button>
              <button
                type="button"
                onClick={() => void finalizeSubmit()}
                className="flex-1 rounded-2xl bg-[#10323F] text-white py-3 text-sm font-extrabold"
              >
                تسليم وتصحيح الآن
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
