import { useStudyStore } from '@/features/study/stores/useStudyStore';
import { useStartSession } from '@/features/study/api/useStartSession';
import { useDueCount } from '@/features/study/api/useDueCount';

interface StudyLauncherProps {
  onNavigateToReview?: () => void;
}

export function StudyLauncher({ onNavigateToReview }: StudyLauncherProps) {
  const { startSession } = useStudyStore();
  const startMutation = useStartSession();
  const { data: dueCount } = useDueCount();

  const dueToday = dueCount?.dueToday ?? 0;
  const newCards = dueCount?.newCards ?? 0;
  const pendingReview = dueCount?.pendingReview ?? 0;
  const total = dueToday + newCards;

  const handleStart = async () => {
    const result = await startMutation.mutateAsync({ maxCards: 100 });
    startSession(result.sessionId, result.cardCount, result.firstCard);
  };

  return (
    <div className="text-center py-8">
      <h2 className="text-xl font-semibold text-gray-800 mb-2">Study Session</h2>

      {total === 0 ? (
        <div className="text-gray-500 py-4">
          <p className="font-medium">All caught up!</p>
          <p className="text-sm mt-1 text-gray-400">No cards due for review today.</p>
          {pendingReview > 0 ? (
            <p className="text-sm mt-2 text-yellow-700">
              {pendingReview} word{pendingReview !== 1 ? 's' : ''} waiting for review —{' '}
              {onNavigateToReview ? (
                <button
                  onClick={onNavigateToReview}
                  className="underline hover:text-yellow-800 transition-colors"
                >
                  go to Review tab
                </button>
              ) : (
                'check the Review tab'
              )}{' '}
              to approve them for study.
            </p>
          ) : (
            <p className="text-sm mt-1 text-gray-400">Check back tomorrow or add more vocabulary.</p>
          )}
        </div>
      ) : (
        <div className="mb-6">
          <div className="flex justify-center gap-4 mb-4 text-sm text-gray-600">
            {dueToday > 0 && (
              <span className="bg-orange-100 text-orange-700 px-3 py-1 rounded-full font-medium">
                {dueToday} due
              </span>
            )}
            {newCards > 0 && (
              <span className="bg-blue-100 text-blue-700 px-3 py-1 rounded-full font-medium">
                {newCards} new
              </span>
            )}
          </div>
          <button
            onClick={handleStart}
            disabled={startMutation.isPending}
            className="px-8 py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold
                       rounded-xl shadow-sm transition-colors disabled:opacity-50"
          >
            {startMutation.isPending ? 'Starting…' : `Start Session (${total} cards)`}
          </button>
        </div>
      )}

      {startMutation.isError && (
        <p className="text-red-500 text-sm mt-2">
          Failed to start session. Please try again.
        </p>
      )}
    </div>
  );
}
