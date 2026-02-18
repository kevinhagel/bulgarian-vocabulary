import { useState } from 'react';
import { useReviewQueue } from '../api/useReviewQueue';
import { useUpdateReviewStatus } from '../api/useUpdateReviewStatus';
import { ReprocessModal } from './ReprocessModal';
import type { LemmaResponseDTO, ReviewStatus } from '../types';

interface ReviewQueueViewProps {
  onViewDetail: (id: number) => void;
}

function statusBadge(status: ReviewStatus) {
  if (status === 'NEEDS_CORRECTION') {
    return (
      <span className="inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-full bg-red-100 text-red-700">
        Needs correction
      </span>
    );
  }
  return (
    <span className="inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-full bg-yellow-100 text-yellow-700">
      Pending review
    </span>
  );
}

interface RowActionsProps {
  lemma: LemmaResponseDTO;
  onViewDetail: (id: number) => void;
}

function RowActions({ lemma, onViewDetail }: RowActionsProps) {
  const [showReprocess, setShowReprocess] = useState(false);
  const markReviewed = useUpdateReviewStatus();

  return (
    <div className="flex items-center gap-2">
      <button
        onClick={() => onViewDetail(lemma.id)}
        className="text-xs px-2.5 py-1 text-blue-600 border border-blue-200 rounded hover:bg-blue-50 transition-colors"
      >
        View
      </button>
      <button
        onClick={() => markReviewed.mutate({ id: lemma.id, status: 'REVIEWED' })}
        disabled={markReviewed.isPending}
        className="text-xs px-2.5 py-1 text-green-700 border border-green-200 rounded hover:bg-green-50 transition-colors disabled:opacity-50"
      >
        {markReviewed.isPending ? '…' : 'Mark reviewed'}
      </button>
      <button
        onClick={() => setShowReprocess(true)}
        className="text-xs px-2.5 py-1 text-gray-600 border border-gray-200 rounded hover:bg-gray-50 transition-colors"
      >
        Reprocess
      </button>

      {showReprocess && (
        <ReprocessModal
          lemmaId={lemma.id}
          lemmaText={lemma.text}
          currentNotes={null}
          onClose={() => setShowReprocess(false)}
          onSuccess={() => {}}
        />
      )}
    </div>
  );
}

/**
 * Review queue tab: lists all user-entered words that are PENDING or NEEDS_CORRECTION.
 * Allows marking words as reviewed (enabling SRS study) or reprocessing them.
 */
export function ReviewQueueView({ onViewDetail }: ReviewQueueViewProps) {
  const [page, setPage] = useState(0);
  const { data, isLoading, error } = useReviewQueue(page);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-yellow-500" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700 text-sm">
        Failed to load review queue.
      </div>
    );
  }

  const items = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  if (items.length === 0) {
    return (
      <div className="text-center py-12">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-green-100 mb-4">
          <svg className="w-8 h-8 text-green-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h3 className="text-lg font-semibold text-gray-800 mb-1">All reviewed!</h3>
        <p className="text-sm text-gray-500">No words are waiting for review.</p>
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-gray-800">
          Review Queue
          <span className="ml-2 text-sm font-normal text-gray-500">
            {totalElements} word{totalElements !== 1 ? 's' : ''} pending
          </span>
        </h2>
      </div>

      <p className="text-sm text-gray-500 mb-4">
        Words marked <span className="font-medium text-yellow-700">Pending review</span> or{' '}
        <span className="font-medium text-red-700">Needs correction</span> are excluded from study sessions
        until you mark them as reviewed.
      </p>

      <div className="space-y-2">
        {items.map(lemma => (
          <div
            key={lemma.id}
            className="flex items-center gap-3 p-3 bg-white border border-gray-200 rounded-lg hover:border-gray-300 transition-colors"
          >
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-medium text-gray-900" lang="bg">{lemma.text}</span>
                {statusBadge(lemma.reviewStatus)}
              </div>
              {lemma.translation && (
                <p className="text-sm text-gray-500 mt-0.5 truncate">{lemma.translation}</p>
              )}
            </div>
            <RowActions lemma={lemma} onViewDetail={onViewDetail} />
          </div>
        ))}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-6">
          <button
            onClick={() => setPage(p => p - 1)}
            disabled={page === 0}
            className="px-3 py-1.5 text-sm border border-gray-300 rounded-md disabled:opacity-40 hover:bg-gray-50 transition-colors"
          >
            Previous
          </button>
          <span className="text-sm text-gray-600">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(p => p + 1)}
            disabled={page >= totalPages - 1}
            className="px-3 py-1.5 text-sm border border-gray-300 rounded-md disabled:opacity-40 hover:bg-gray-50 transition-colors"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
