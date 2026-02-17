import { useStudyStore } from '@/features/study/stores/useStudyStore';
import { useQueryClient } from '@tanstack/react-query';

export function SessionSummaryView() {
  const { summary, reset } = useStudyStore();
  const queryClient = useQueryClient();

  if (!summary) return null;

  const handleDone = () => {
    queryClient.invalidateQueries({ queryKey: ['study'] });
    queryClient.invalidateQueries({ queryKey: ['vocabulary'] });
    reset();
  };

  const retentionColor =
    summary.retentionRate >= 80 ? 'text-green-600' :
    summary.retentionRate >= 50 ? 'text-yellow-600' : 'text-red-500';

  return (
    <div className="max-w-md mx-auto py-8 px-4 text-center">
      <h2 className="text-2xl font-bold text-gray-800 mb-2">
        {summary.status === 'COMPLETED' ? 'Session Complete!' : 'Session Ended'}
      </h2>
      <p className="text-gray-500 text-sm mb-6">
        {summary.status === 'COMPLETED' ? 'Great work!' : 'Progress saved.'}
      </p>

      <div className="bg-white rounded-2xl shadow-sm p-6 mb-6">
        <div className="grid grid-cols-3 gap-4 text-center">
          <div>
            <p className="text-2xl font-bold text-gray-800">{summary.cardsReviewed}</p>
            <p className="text-xs text-gray-400 mt-1">Reviewed</p>
          </div>
          <div>
            <p className="text-2xl font-bold text-green-600">{summary.correctCount}</p>
            <p className="text-xs text-gray-400 mt-1">Correct</p>
          </div>
          <div>
            <p className={`text-2xl font-bold ${retentionColor}`}>{summary.retentionRate}%</p>
            <p className="text-xs text-gray-400 mt-1">Retention</p>
          </div>
        </div>
      </div>

      <button
        onClick={handleDone}
        className="px-8 py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold
                   rounded-xl shadow-sm transition-colors"
      >
        Done
      </button>
    </div>
  );
}
