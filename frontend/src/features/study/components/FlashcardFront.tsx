import { AudioPlayButton } from '@/components/audio/AudioPlayButton';

interface FlashcardFrontProps {
  lemmaText: string;
  onReveal: () => void;
}

export function FlashcardFront({ lemmaText, onReveal }: FlashcardFrontProps) {
  return (
    <div className="bg-white rounded-2xl shadow-md p-8 min-h-48 flex flex-col items-center justify-center gap-4">
      <p className="text-4xl font-bold text-gray-900 text-center" style={{ fontFamily: 'Sofia Sans, sans-serif' }}>
        {lemmaText}
      </p>
      <AudioPlayButton text={lemmaText} />
      <button
        onClick={onReveal}
        className="mt-4 px-6 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700
                   font-medium rounded-xl transition-colors text-sm"
      >
        Reveal Translation
      </button>
    </div>
  );
}
