import { useState } from 'react';
import { AudioPlayButton } from '@/components/audio/AudioPlayButton';
import { InflectionsTable } from './InflectionsTable';
import { LemmaSrsInfo } from './LemmaSrsInfo';
import { ReprocessModal } from './ReprocessModal';
import { useVocabularyDetail } from '../api/useVocabularyDetail';
import { useVocabularyUIStore } from '../stores/useVocabularyUIStore';
import { useFlagVocabulary } from '../api/useFlagVocabulary';
import { useUpdateReviewStatus } from '../api/useUpdateReviewStatus';
import type { PartOfSpeech, DifficultyLevel, ReviewStatus, Source } from '@/types';

interface VocabularyDetailProps {
  lemmaId: number;
  onBack: () => void;
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
 * Formats ISO date string to readable format.
 */
function formatDate(isoString: string): string {
  const date = new Date(isoString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
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
 * Returns color for review status badge.
 */
function getReviewStatusColor(status: ReviewStatus): string {
  const colors: Record<ReviewStatus, string> = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    REVIEWED: 'bg-green-100 text-green-800',
    NEEDS_CORRECTION: 'bg-red-100 text-red-800',
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
 * Vocabulary detail page component showing full lemma information with inflections.
 */
export function VocabularyDetail({ lemmaId, onBack }: VocabularyDetailProps) {
  const { data: lemma, isLoading, error, refetch } = useVocabularyDetail(lemmaId);
  const openEditModal = useVocabularyUIStore(state => state.openEditModal);
  const openDeleteConfirm = useVocabularyUIStore(state => state.openDeleteConfirm);
  const [showReprocess, setShowReprocess] = useState(false);
  const flagVocabulary = useFlagVocabulary();
  const updateReviewStatus = useUpdateReviewStatus();

  // Loading state
  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="flex items-center justify-center py-12">
          <div className="flex flex-col items-center gap-3">
            <svg className="animate-spin h-8 w-8 text-blue-600" viewBox="0 0 24 24">
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
                fill="none"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              />
            </svg>
            <p className="text-gray-600">Loading vocabulary details...</p>
          </div>
        </div>
      </div>
    );
  }

  // Error state
  if (error || !lemma) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <button
          onClick={onBack}
          className="mb-6 inline-flex items-center gap-2 text-blue-600 hover:text-blue-800 font-medium"
        >
          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          Back to Vocabulary
        </button>

        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <svg className="inline-block w-12 h-12 text-red-500 mb-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <p className="text-red-800 font-medium mb-2">Failed to load vocabulary details</p>
          <p className="text-red-600 text-sm mb-4">
            {error instanceof Error ? error.message : 'An error occurred'}
          </p>
          <button
            onClick={() => refetch()}
            className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  // Success state
  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      {/* Back button */}
      <button
        onClick={onBack}
        className="mb-6 inline-flex items-center gap-2 text-blue-600 hover:text-blue-800 font-medium transition-colors"
      >
        <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </svg>
        Back to Vocabulary
      </button>

      {/* Main content card */}
      <div className="bg-white rounded-lg shadow-lg p-8 space-y-8">
        {/* Header section */}
        <div className="border-b border-gray-200 pb-6">
          <div className="flex items-start gap-4 mb-4">
            <h1 className="flex-1 text-4xl font-bold text-gray-900" lang="bg">
              {lemma.text}
            </h1>
            <AudioPlayButton text={lemma.text} />
          </div>
          <p className="text-xl text-gray-600">{lemma.translation}</p>
        </div>

        {/* Metadata section */}
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-3">Metadata</h2>
          <div className="flex flex-wrap gap-2">
            {lemma.partOfSpeech && (
              <span className={`px-3 py-1.5 text-sm font-medium rounded-full ${getPartOfSpeechColor(lemma.partOfSpeech)}`}>
                {formatEnumLabel(lemma.partOfSpeech)}
              </span>
            )}
            {lemma.category && (
              <span className="px-3 py-1.5 text-sm font-medium rounded-full bg-gray-100 text-gray-800">
                {lemma.category}
              </span>
            )}
            {lemma.difficultyLevel && (
              <span className={`px-3 py-1.5 text-sm font-medium rounded-full ${getDifficultyColor(lemma.difficultyLevel)}`}>
                {formatEnumLabel(lemma.difficultyLevel)}
              </span>
            )}
            <span className={`px-3 py-1.5 text-sm font-medium rounded-full ${getSourceColor(lemma.source)}`}>
              {formatEnumLabel(lemma.source)}
            </span>
            <span className={`px-3 py-1.5 text-sm font-medium rounded-full ${getReviewStatusColor(lemma.reviewStatus)}`}>
              {formatEnumLabel(lemma.reviewStatus)}
            </span>
          </div>
          <div className="mt-4 text-sm text-gray-600 space-y-1">
            <p>
              <span className="font-medium text-gray-700">Created:</span> {formatDate(lemma.createdAt)}
            </p>
            <p>
              <span className="font-medium text-gray-700">Updated:</span> {formatDate(lemma.updatedAt)}
            </p>
          </div>
        </div>

        {/* Notes section */}
        {lemma.notes && (
          <div>
            <h2 className="text-lg font-semibold text-gray-900 mb-3">Notes</h2>
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
              <p className="text-gray-700 whitespace-pre-wrap">{lemma.notes}</p>
            </div>
          </div>
        )}

        {/* Inflections section */}
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-3">
            Inflections ({lemma.inflections.length})
          </h2>
          <InflectionsTable inflections={lemma.inflections} />
        </div>

        {/* SRS study statistics for user-entered lemmas */}
        {lemma.source === 'USER_ENTERED' && <LemmaSrsInfo lemmaId={lemma.id} />}

        {/* Action buttons */}
        <div className="pt-6 border-t border-gray-200 space-y-3">
          {/* Primary actions */}
          <div className="flex gap-3">
            <button
              onClick={() => openEditModal(lemma.id)}
              className="flex-1 px-4 py-2.5 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors"
            >
              <svg className="inline-block w-4 h-4 mr-2" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
              </svg>
              Edit
            </button>
            <button
              onClick={() => openDeleteConfirm(lemma.id)}
              className="flex-1 px-4 py-2.5 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 transition-colors"
            >
              <svg className="inline-block w-4 h-4 mr-2" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
              Delete
            </button>
          </div>

          {/* Review / reprocess actions (user-entered words only) */}
          {lemma.source === 'USER_ENTERED' && (
            <div className="flex gap-3">
              {(lemma.reviewStatus === 'PENDING' || lemma.reviewStatus === 'NEEDS_CORRECTION') && (
                <button
                  onClick={() => updateReviewStatus.mutate({ id: lemma.id, status: 'REVIEWED' })}
                  disabled={updateReviewStatus.isPending}
                  className="flex-1 px-4 py-2 text-sm font-medium text-green-700 border border-green-300 bg-green-50 rounded-md hover:bg-green-100 transition-colors disabled:opacity-50"
                >
                  Mark Reviewed
                </button>
              )}
              {lemma.reviewStatus === 'REVIEWED' && (
                <button
                  onClick={() => flagVocabulary.mutate(lemma.id)}
                  disabled={flagVocabulary.isPending}
                  className="flex-1 px-4 py-2 text-sm font-medium text-orange-700 border border-orange-300 bg-orange-50 rounded-md hover:bg-orange-100 transition-colors disabled:opacity-50"
                >
                  Flag for Correction
                </button>
              )}
              <button
                onClick={() => setShowReprocess(true)}
                className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 bg-white rounded-md hover:bg-gray-50 transition-colors"
              >
                Reprocess
              </button>
            </div>
          )}
        </div>

        {showReprocess && (
          <ReprocessModal
            lemmaId={lemma.id}
            lemmaText={lemma.text}
            currentNotes={lemma.notes}
            onClose={() => setShowReprocess(false)}
            onSuccess={() => refetch()}
          />
        )}
      </div>
    </div>
  );
}
