import { useLemmaStats } from '@/features/study/api/useLemmaStats';

export function LemmaSrsInfo({ lemmaId }: { lemmaId: number }) {
  const { data: stats, isLoading } = useLemmaStats(lemmaId);
  if (isLoading || !stats) return null;

  if (stats.reviewCount === 0) {
    return (
      <div className="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-100">
        <p className="text-sm text-blue-700 text-center">
          Not studied yet. Start a study session to track progress for this word.
        </p>
      </div>
    );
  }

  const formatNext = (d: string | null) => {
    if (!d) return 'Not scheduled';
    const date = new Date(d);
    const today = new Date(); today.setHours(0, 0, 0, 0); date.setHours(0, 0, 0, 0);
    const diff = Math.round((date.getTime() - today.getTime()) / 86_400_000);
    if (diff < 0) return 'Overdue';
    if (diff === 0) return 'Today';
    if (diff === 1) return 'Tomorrow';
    return `In ${diff} days`;
  };

  const formatLast = (d: string | null) =>
    d ? new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : 'Never';

  const rateColor = stats.correctRate >= 80 ? 'text-green-600' :
    stats.correctRate >= 50 ? 'text-yellow-600' : 'text-red-500';

  return (
    <div className="mt-6">
      <h3 className="text-sm font-semibold text-gray-600 mb-3">Study Statistics</h3>
      <div className="bg-gray-50 rounded-lg border border-gray-100 p-4">
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-xs text-gray-400 block mb-0.5">Total Reviews</span>
            <span className="font-semibold text-gray-800">{stats.reviewCount}</span>
          </div>
          <div>
            <span className="text-xs text-gray-400 block mb-0.5">Correct Rate</span>
            <span className={`font-semibold ${rateColor}`}>{stats.correctRate}%</span>
          </div>
          <div>
            <span className="text-xs text-gray-400 block mb-0.5">Last Reviewed</span>
            <span className="font-semibold text-gray-800">{formatLast(stats.lastReviewedAt)}</span>
          </div>
          <div>
            <span className="text-xs text-gray-400 block mb-0.5">Next Review</span>
            <span className={`font-semibold ${stats.nextReviewDate && new Date(stats.nextReviewDate) < new Date() ? 'text-orange-500' : 'text-gray-800'}`}>
              {formatNext(stats.nextReviewDate)}
            </span>
          </div>
        </div>
        {stats.intervalDays > 0 && (
          <p className="text-xs text-gray-400 mt-3 pt-3 border-t border-gray-200">
            Interval: {stats.intervalDays}d | Ease: {stats.easeFactor.toFixed(2)}
          </p>
        )}
      </div>
    </div>
  );
}
