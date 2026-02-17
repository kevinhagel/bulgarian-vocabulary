import { useStudyStore } from '@/features/study/stores/useStudyStore';
import { useRateCard } from '@/features/study/api/useRateCard';
import { useEndSession } from '@/features/study/api/useEndSession';
import { FlashcardFront } from './FlashcardFront';
import { FlashcardBack } from './FlashcardBack';
import { SessionProgress } from './SessionProgress';

export function FlashcardView() {
  const { sessionId, cardCount, currentCard, isRevealed, setRevealed, setCurrentCard, showSummary } = useStudyStore();
  const rateCard = useRateCard();
  const endSession = useEndSession();

  if (!currentCard || !sessionId) return null;

  const reviewed = cardCount - Number(currentCard.cardsRemaining);

  const handleRate = async (rating: 'CORRECT' | 'INCORRECT') => {
    const next = await rateCard.mutateAsync({
      sessionId,
      request: { lemmaId: currentCard.lemmaId, rating },
    });
    if (next) {
      setCurrentCard(next);
    } else {
      const summary = await endSession.mutateAsync({ sessionId });
      showSummary(summary);
    }
  };

  const handleEndEarly = async () => {
    const summary = await endSession.mutateAsync({ sessionId });
    showSummary(summary);
  };

  return (
    <div className="max-w-md mx-auto py-6 px-4">
      <SessionProgress reviewed={reviewed} total={cardCount} />

      {!isRevealed ? (
        <FlashcardFront
          lemmaText={currentCard.lemmaText}
          onReveal={() => setRevealed(true)}
        />
      ) : (
        <FlashcardBack
          lemmaText={currentCard.lemmaText}
          translation={currentCard.translation ?? '—'}
          onCorrect={() => handleRate('CORRECT')}
          onIncorrect={() => handleRate('INCORRECT')}
          isLoading={rateCard.isPending || endSession.isPending}
        />
      )}

      <div className="mt-4 text-center">
        <button
          onClick={handleEndEarly}
          disabled={endSession.isPending}
          className="text-xs text-gray-400 hover:text-gray-600 underline transition-colors"
        >
          End session early
        </button>
      </div>
    </div>
  );
}
