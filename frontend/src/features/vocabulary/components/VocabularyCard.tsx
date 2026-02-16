import { AudioPlayButton } from '@/components/audio/AudioPlayButton';
import type { LemmaResponseDTO, PartOfSpeech, DifficultyLevel, ReviewStatus, Source } from '@/types';

interface VocabularyCardProps {
  lemma: LemmaResponseDTO;
  onEdit: (id: number) => void;
  onDelete: (id: number) => void;
  onViewDetail: (id: number) => void;
}

/**
 * Converts enum values to display-friendly labels.
 */
function formatEnumLabel(value: string): string {
  return value
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
}

/**
 * Returns color classes for part of speech badges.
 */
function getPartOfSpeechColor(pos: PartOfSpeech | null): string {
  if (!pos) return 'bg-gray-100 text-gray-800';

  const colors: Record<PartOfSpeech, string> = {
    VERB: 'bg-blue-100 text-blue-800',
    NOUN: 'bg-green-100 text-green-800',
    ADJECTIVE: 'bg-purple-100 text-purple-800',
    ADVERB: 'bg-yellow-100 text-yellow-800',
    PRONOUN: 'bg-pink-100 text-pink-800',
    PREPOSITION: 'bg-indigo-100 text-indigo-800',
    CONJUNCTION: 'bg-orange-100 text-orange-800',
    INTERJECTION: 'bg-red-100 text-red-800',
    PARTICLE: 'bg-teal-100 text-teal-800',
    NUMERAL: 'bg-cyan-100 text-cyan-800',
    INTERROGATIVE: 'bg-lime-100 text-lime-800',
  };

  return colors[pos] || 'bg-gray-100 text-gray-800';
}

/**
 * Returns color classes for difficulty level badges.
 */
function getDifficultyColor(level: DifficultyLevel | null): string {
  if (!level) return 'bg-gray-100 text-gray-800';

  const colors: Record<DifficultyLevel, string> = {
    BEGINNER: 'bg-green-100 text-green-800',
    INTERMEDIATE: 'bg-yellow-100 text-yellow-800',
    ADVANCED: 'bg-red-100 text-red-800',
  };

  return colors[level] || 'bg-gray-100 text-gray-800';
}

/**
 * Returns color for review status indicator dot.
 */
function getReviewStatusColor(status: ReviewStatus): string {
  const colors: Record<ReviewStatus, string> = {
    PENDING: 'bg-yellow-500',
    REVIEWED: 'bg-green-500',
    NEEDS_CORRECTION: 'bg-red-500',
  };

  return colors[status];
}

/**
 * Returns color for source badge.
 */
function getSourceColor(source: Source): string {
  const colors: Record<Source, string> = {
    USER_ENTERED: 'bg-blue-100 text-blue-800',
    SYSTEM_SEED: 'bg-gray-100 text-gray-800',
  };

  return colors[source];
}

/**
 * Card component for displaying a single vocabulary entry in the list.
 */
export function VocabularyCard({ lemma, onEdit, onDelete, onViewDetail }: VocabularyCardProps) {
  return (
    <div className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
      {/* Header: Lemma text and review status */}
      <div className="flex items-start justify-between mb-3">
        <div className="flex-1">
          <h3 className="text-2xl font-semibold text-gray-900 mb-1" lang="bg">
            {lemma.text}
          </h3>
          <p className="text-gray-600">{lemma.translation}</p>
        </div>
        <div
          className={`w-3 h-3 rounded-full ${getReviewStatusColor(lemma.reviewStatus)} ml-3 mt-2`}
          title={formatEnumLabel(lemma.reviewStatus)}
          aria-label={`Review status: ${formatEnumLabel(lemma.reviewStatus)}`}
        />
      </div>

      {/* Metadata badges */}
      <div className="flex flex-wrap gap-2 mb-4">
        {lemma.partOfSpeech && (
          <span className={`px-2 py-1 text-xs font-medium rounded-full ${getPartOfSpeechColor(lemma.partOfSpeech)}`}>
            {formatEnumLabel(lemma.partOfSpeech)}
          </span>
        )}
        {lemma.difficultyLevel && (
          <span className={`px-2 py-1 text-xs font-medium rounded-full ${getDifficultyColor(lemma.difficultyLevel)}`}>
            {formatEnumLabel(lemma.difficultyLevel)}
          </span>
        )}
        <span className={`px-2 py-1 text-xs font-medium rounded-full ${getSourceColor(lemma.source)}`}>
          {formatEnumLabel(lemma.source)}
        </span>
        {lemma.inflectionCount > 0 && (
          <span className="px-2 py-1 text-xs font-medium rounded-full bg-gray-100 text-gray-800">
            {lemma.inflectionCount} {lemma.inflectionCount === 1 ? 'inflection' : 'inflections'}
          </span>
        )}
      </div>

      {/* Audio play button */}
      <div className="mb-4">
        <AudioPlayButton text={lemma.text} />
      </div>

      {/* Action buttons */}
      <div className="flex gap-2 pt-4 border-t border-gray-200">
        <button
          onClick={() => onViewDetail(lemma.id)}
          className="flex-1 px-3 py-2 text-sm font-medium text-blue-700 bg-blue-50 rounded-md hover:bg-blue-100 transition-colors"
        >
          <svg className="inline-block w-4 h-4 mr-1" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
          View
        </button>
        <button
          onClick={() => onEdit(lemma.id)}
          className="flex-1 px-3 py-2 text-sm font-medium text-gray-700 bg-gray-50 rounded-md hover:bg-gray-100 transition-colors"
        >
          <svg className="inline-block w-4 h-4 mr-1" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
          Edit
        </button>
        <button
          onClick={() => onDelete(lemma.id)}
          className="flex-1 px-3 py-2 text-sm font-medium text-red-700 bg-red-50 rounded-md hover:bg-red-100 transition-colors"
        >
          <svg className="inline-block w-4 h-4 mr-1" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          </svg>
          Delete
        </button>
      </div>
    </div>
  );
}
