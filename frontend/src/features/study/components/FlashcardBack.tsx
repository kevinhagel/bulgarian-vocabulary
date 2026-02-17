import { AudioPlayButton } from '@/components/audio/AudioPlayButton';

interface FlashcardBackProps {
  lemmaText: string;
  translation: string;
  onCorrect: () => void;
  onIncorrect: () => void;
  isLoading: boolean;
}

export function FlashcardBack({ lemmaText, translation, onCorrect, onIncorrect, isLoading }: FlashcardBackProps) {
  return (
    <div className="bg-white rounded-2xl shadow-md p-8 flex flex-col gap-4">
      <div className="text-center">
        <p className="text-3xl font-bold text-gray-900" style={{ fontFamily: 'Sofia Sans, sans-serif' }}>
          {lemmaText}
        </p>
        <div className="mt-2 flex justify-center">
          <AudioPlayButton text={lemmaText} />
        </div>
      </div>

      <div className="border-t pt-4 text-center">
        <p className="text-lg text-blue-700 font-medium">{translation}</p>
      </div>

      <div className="flex gap-3 mt-2">
        <button
          onClick={onIncorrect}
          disabled={isLoading}
          className="flex-1 py-3 bg-red-50 hover:bg-red-100 text-red-700 font-semibold
                     rounded-xl border border-red-200 transition-colors disabled:opacity-50"
        >
          Incorrect
        </button>
        <button
          onClick={onCorrect}
          disabled={isLoading}
          className="flex-1 py-3 bg-green-50 hover:bg-green-100 text-green-700 font-semibold
                     rounded-xl border border-green-200 transition-colors disabled:opacity-50"
        >
          Correct
        </button>
      </div>
    </div>
  );
}
