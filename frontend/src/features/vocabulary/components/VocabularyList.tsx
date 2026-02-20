import { useState } from 'react';
import { useDebounce } from '@/hooks/useDebounce';
import { useDueCount } from '@/features/study/api/useDueCount';
import { useVocabulary } from '../api/useVocabulary';
import { useSearchVocabulary } from '../api/useSearchVocabulary';
import { useGenerateAllSentences } from '../api/useGenerateAllSentences';
import { useVocabularyUIStore } from '../stores/useVocabularyUIStore';
import { VocabularyFilters } from './VocabularyFilters';
import { VocabularyCard } from './VocabularyCard';
import { Pagination } from './Pagination';

interface VocabularyListProps {
  onViewDetail: (id: number) => void;
  onNavigateStudy?: () => void;
}

/**
 * Main vocabulary list page component.
 * Displays vocabulary entries with search, filtering, pagination, and audio playback.
 */
export function VocabularyList({ onViewDetail, onNavigateStudy }: VocabularyListProps) {
  // Get UI state from Zustand store
  const searchQuery = useVocabularyUIStore(state => state.searchQuery);
  const selectedSource = useVocabularyUIStore(state => state.selectedSource);
  const selectedPartOfSpeech = useVocabularyUIStore(state => state.selectedPartOfSpeech);
  const selectedDifficultyLevel = useVocabularyUIStore(state => state.selectedDifficultyLevel);
  const currentPage = useVocabularyUIStore(state => state.currentPage);
  const pageSize = useVocabularyUIStore(state => state.pageSize);
  const setCurrentPage = useVocabularyUIStore(state => state.setCurrentPage);
  const openCreateModal = useVocabularyUIStore(state => state.openCreateModal);
  const openEditModal = useVocabularyUIStore(state => state.openEditModal);
  const openDeleteConfirm = useVocabularyUIStore(state => state.openDeleteConfirm);

  const { data: dueCount } = useDueCount();
  const totalDue = (dueCount?.dueToday ?? 0) + (dueCount?.newCards ?? 0);
  const generateAll = useGenerateAllSentences();
  const [generateAllMessage, setGenerateAllMessage] = useState<string | null>(null);

  const handleGenerateAll = () => {
    generateAll.mutate(undefined, {
      onSuccess: (data) => {
        setGenerateAllMessage(
          data.queued > 0
            ? `${data.queued} word${data.queued === 1 ? '' : 's'} queued for sentence generation.`
            : 'All words already have sentences.'
        );
        setTimeout(() => setGenerateAllMessage(null), 5000);
      },
    });
  };

  // Debounce search query
  const debouncedSearchQuery = useDebounce(searchQuery, 300);

  // Determine which query to use
  const isSearchMode = debouncedSearchQuery.length >= 2;

  // Fetch data based on mode
  const browseQuery = useVocabulary({
    page: currentPage,
    size: pageSize,
    source: selectedSource,
    partOfSpeech: selectedPartOfSpeech,
    difficultyLevel: selectedDifficultyLevel,
  });

  const searchQuery2 = useSearchVocabulary(debouncedSearchQuery);

  // Select the appropriate query based on mode
  const { data, isLoading, error, refetch } = isSearchMode ? searchQuery2 : browseQuery;

  // Extract data from result
  const entries = isSearchMode
    ? (data as any) || []
    : (data as any)?.content || [];
  const totalPages = isSearchMode ? 1 : (data as any)?.totalPages || 0;
  const totalElements = isSearchMode ? entries.length : (data as any)?.totalElements || 0;

  // Note: onViewDetail prop passed through from App.tsx

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Page header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Vocabulary</h1>
        <div className="flex items-center gap-2">
          {totalDue > 0 && onNavigateStudy && (
            <button
              onClick={onNavigateStudy}
              className="flex items-center gap-1.5 px-3 py-2 bg-orange-50 hover:bg-orange-100
                         border border-orange-200 rounded-lg text-sm font-medium text-orange-700
                         transition-colors"
            >
              <span className="inline-flex items-center justify-center w-5 h-5 bg-orange-500
                               text-white text-xs rounded-full font-bold">
                {totalDue > 99 ? '99+' : totalDue}
              </span>
              Study
            </button>
          )}
          <button
            onClick={handleGenerateAll}
            disabled={generateAll.isPending}
            title="Generate example sentences for all words that don't have them yet"
            className="px-3 py-2 text-sm font-medium text-gray-600 border border-gray-300 rounded-md
                       hover:bg-gray-50 transition-colors disabled:opacity-50"
          >
            Generate All Sentences
          </button>
          <button
            onClick={openCreateModal}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors"
          >
          <svg className="inline-block w-5 h-5 mr-2" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          Add Vocabulary
          </button>
        </div>
      </div>

      {/* Generate All feedback */}
      {generateAllMessage && (
        <div className="mb-4 px-4 py-2 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-700">
          {generateAllMessage}
        </div>
      )}

      {/* Filters */}
      <VocabularyFilters />

      {/* Loading state */}
      {isLoading && (
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
            <p className="text-gray-600">Loading vocabulary...</p>
          </div>
        </div>
      )}

      {/* Error state */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <svg className="inline-block w-12 h-12 text-red-500 mb-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <p className="text-red-800 font-medium mb-2">Failed to load vocabulary</p>
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
      )}

      {/* Empty state */}
      {!isLoading && !error && entries.length === 0 && (
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-12 text-center">
          <svg className="inline-block w-16 h-16 text-gray-400 mb-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 2v20M2 12h20" />
          </svg>
          <p className="text-gray-800 font-medium text-lg mb-2">No vocabulary entries found</p>
          <p className="text-gray-600 mb-4">
            {isSearchMode
              ? 'Try a different search term or adjust your filters'
              : 'Get started by adding your first vocabulary entry'}
          </p>
          {!isSearchMode && (
            <button
              onClick={openCreateModal}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors"
            >
              Add Your First Entry
            </button>
          )}
        </div>
      )}

      {/* Vocabulary grid */}
      {!isLoading && !error && entries.length > 0 && (
        <>
          {/* Total count display */}
          <div className="mb-4 text-sm text-gray-600">
            Showing {entries.length} {isSearchMode ? 'results' : `of ${totalElements} entries`}
          </div>

          {/* Card grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {entries.map((lemma: any) => (
              <VocabularyCard
                key={lemma.id}
                lemma={lemma}
                onEdit={openEditModal}
                onDelete={openDeleteConfirm}
                onViewDetail={onViewDetail}
              />
            ))}
          </div>

          {/* Pagination (only in browse mode) */}
          {!isSearchMode && (
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
            />
          )}
        </>
      )}
    </div>
  );
}
