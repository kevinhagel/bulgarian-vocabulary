import { useEffect } from 'react';
import { AudioPlayButton } from '@/components/audio/AudioPlayButton';
import { useGenerateSentences } from '../api/useGenerateSentences';
import type { LemmaDetailDTO, SentenceStatus } from '../types';

interface Props {
  lemma: LemmaDetailDTO;
  onUpdated: () => void;
}

function statusLabel(status: SentenceStatus): string {
  switch (status) {
    case 'QUEUED':     return 'Queued for generation…';
    case 'GENERATING': return 'Generating sentences with Qwen 2.5 14B… (60–90s)';
    case 'FAILED':     return 'Generation failed — try again';
    default:           return '';
  }
}

/**
 * Displays example sentences for a lemma with generate/regenerate controls.
 * Polls for completion when status is QUEUED or GENERATING.
 */
export function ExampleSentencesSection({ lemma, onUpdated }: Props) {
  const generate = useGenerateSentences();
  const isProcessing = lemma.sentenceStatus === 'QUEUED' || lemma.sentenceStatus === 'GENERATING';

  // Poll every 4 seconds while generation is in progress
  useEffect(() => {
    if (!isProcessing) return;
    const timer = setInterval(onUpdated, 4000);
    return () => clearInterval(timer);
  }, [isProcessing, onUpdated]);

  const handleGenerate = () => {
    generate.mutate(lemma.id, { onSuccess: onUpdated });
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-lg font-semibold text-gray-900">
          Example Sentences
          {lemma.sentenceStatus === 'DONE' && lemma.exampleSentences.length > 0 && (
            <span className="ml-2 text-sm font-normal text-gray-500">
              ({lemma.exampleSentences.length})
            </span>
          )}
        </h2>
        {lemma.sentenceStatus === 'DONE' && lemma.exampleSentences.length > 0 && (
          <button
            onClick={handleGenerate}
            disabled={generate.isPending}
            className="text-xs text-gray-400 hover:text-gray-600 transition-colors disabled:opacity-50"
          >
            Regenerate
          </button>
        )}
      </div>

      {/* Not yet generated */}
      {(lemma.sentenceStatus === 'NONE' || lemma.sentenceStatus === 'FAILED') && (
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 flex items-center justify-between">
          {lemma.sentenceStatus === 'FAILED' && (
            <p className="text-sm text-red-600">Generation failed — please try again.</p>
          )}
          {lemma.sentenceStatus === 'NONE' && (
            <p className="text-sm text-gray-500">
              No example sentences yet. Generate 4 natural sentences with Qwen 2.5 14B.
            </p>
          )}
          <button
            onClick={handleGenerate}
            disabled={generate.isPending}
            className="ml-4 shrink-0 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg
                       hover:bg-blue-700 transition-colors disabled:opacity-50"
          >
            Generate Sentences
          </button>
        </div>
      )}

      {/* In progress */}
      {isProcessing && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-center gap-3">
          <svg className="animate-spin h-5 w-5 text-blue-600 shrink-0" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
            <path className="opacity-75" fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          <p className="text-sm text-blue-700">{statusLabel(lemma.sentenceStatus)}</p>
        </div>
      )}

      {/* Sentences ready */}
      {lemma.sentenceStatus === 'DONE' && lemma.exampleSentences.length > 0 && (
        <ol className="space-y-4">
          {lemma.exampleSentences.map((sentence, index) => (
            <li key={sentence.id} className="bg-gray-50 border border-gray-100 rounded-lg p-4">
              <div className="flex items-start gap-3">
                <span className="shrink-0 w-6 h-6 flex items-center justify-center rounded-full
                                 bg-blue-100 text-blue-700 text-xs font-bold mt-0.5">
                  {index + 1}
                </span>
                <div className="flex-1 min-w-0">
                  <p className="text-lg font-medium text-gray-900" lang="bg"
                     style={{ fontFamily: 'Sofia Sans, sans-serif' }}>
                    {sentence.bulgarianText}
                  </p>
                  <p className="mt-1 text-sm text-gray-500 italic">{sentence.englishTranslation}</p>
                </div>
                <AudioPlayButton text={sentence.bulgarianText} />
              </div>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
