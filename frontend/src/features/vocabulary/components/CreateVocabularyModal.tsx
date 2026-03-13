import { useState, useEffect, useRef } from 'react';
import { Modal } from '@/components/ui/Modal';
import { useVocabularyUIStore } from '../stores/useVocabularyUIStore';
import { useCreateVocabulary } from '../api/useCreateVocabulary';
import { useSearchDictionary } from '../api/useSearchDictionary';
import type { DictionarySearchResultDTO } from '../types';

/**
 * Modal for creating new vocabulary entries.
 * Flow: user types a Bulgarian word → dictionary search runs automatically →
 * user picks a dictionary match (instant create) or skips to LLM fallback.
 */
export function CreateVocabularyModal() {
  const isOpen = useVocabularyUIStore((state) => state.isCreateModalOpen);
  const closeModal = useVocabularyUIStore((state) => state.closeCreateModal);
  const createMutation = useCreateVocabulary();

  const [wordForm, setWordForm] = useState('');
  const [translation, setTranslation] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [selectedResult, setSelectedResult] = useState<DictionarySearchResultDTO | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Debounced search query
  const [debouncedQuery, setDebouncedQuery] = useState('');
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(wordForm.trim()), 300);
    return () => clearTimeout(timer);
  }, [wordForm]);

  const { data: dictResults, isLoading: isDictLoading } = useSearchDictionary(debouncedQuery);

  // Reset state when modal opens/closes
  useEffect(() => {
    if (isOpen) {
      setWordForm('');
      setTranslation('');
      setNotes('');
      setError(null);
      setSelectedResult(null);
      setDebouncedQuery('');
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [isOpen]);

  // When user selects a dictionary result, pre-fill translation
  const handleSelectResult = (result: DictionarySearchResultDTO) => {
    setSelectedResult(result);
    setTranslation(result.primaryTranslation || '');
    setError(null);
  };

  const handleDeselectResult = () => {
    setSelectedResult(null);
    setTranslation('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedWord = wordForm.trim();
    if (!trimmedWord) return;

    try {
      setError(null);
      await createMutation.mutateAsync({
        wordForm: trimmedWord,
        translation: translation.trim() || '',
        notes: notes.trim() || undefined,
        dictionaryWordId: selectedResult?.dictionaryWordId,
      });
      closeModal();
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 409) {
        setError(`"${trimmedWord.toLowerCase()}" is already in your vocabulary.`);
      } else {
        setError(err instanceof Error ? err.message : 'Failed to create vocabulary');
      }
    }
  };

  const handleClose = () => {
    setError(null);
    closeModal();
  };

  const showDictResults = !selectedResult && debouncedQuery.length >= 2;

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Add Vocabulary" size="lg">
      {/* LLM processing status */}
      {createMutation.isPending && (
        <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-md text-blue-700">
          {selectedResult
            ? 'Creating from dictionary...'
            : 'Processing with LLM... This may take a few seconds.'}
        </div>
      )}

      {/* Error message */}
      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Bulgarian word input */}
        <div>
          <label htmlFor="wordForm" className="block text-sm font-medium text-gray-700 mb-1">
            Bulgarian Word
          </label>
          <input
            ref={inputRef}
            id="wordForm"
            type="text"
            lang="bg"
            value={wordForm}
            onChange={(e) => {
              setWordForm(e.target.value);
              if (selectedResult) handleDeselectResult();
            }}
            placeholder="Enter Bulgarian word or phrase"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            disabled={createMutation.isPending}
          />
        </div>

        {/* Dictionary search results */}
        {showDictResults && (
          <DictionaryResults
            results={dictResults ?? []}
            isLoading={isDictLoading}
            onSelect={handleSelectResult}
          />
        )}

        {/* Selected dictionary entry */}
        {selectedResult && (
          <SelectedDictionaryEntry
            result={selectedResult}
            onDeselect={handleDeselectResult}
          />
        )}

        {/* Translation */}
        <div>
          <label htmlFor="translation" className="block text-sm font-medium text-gray-700 mb-1">
            English Translation{' '}
            <span className="text-gray-500 font-normal">
              {selectedResult ? '(from dictionary)' : '(optional - auto-translated if empty)'}
            </span>
          </label>
          <input
            id="translation"
            type="text"
            value={translation}
            onChange={(e) => setTranslation(e.target.value)}
            placeholder={selectedResult ? '' : 'Optional - will auto-translate'}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            disabled={createMutation.isPending}
          />
        </div>

        {/* Notes */}
        <div>
          <label htmlFor="notes" className="block text-sm font-medium text-gray-700 mb-1">
            Notes
          </label>
          <textarea
            id="notes"
            rows={2}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Optional notes..."
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            disabled={createMutation.isPending}
          />
        </div>

        {/* Form actions */}
        <div className="flex justify-end gap-3 pt-4 border-t border-gray-200">
          <button
            type="button"
            onClick={handleClose}
            className="px-4 py-2 text-gray-700 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors"
            disabled={createMutation.isPending}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors disabled:bg-blue-300 disabled:cursor-not-allowed"
            disabled={createMutation.isPending || !wordForm.trim()}
          >
            {createMutation.isPending
              ? 'Saving...'
              : selectedResult
                ? 'Create from Dictionary'
                : 'Create with LLM'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

/**
 * Displays dictionary search results for the user to pick from.
 */
function DictionaryResults({
  results,
  isLoading,
  onSelect,
}: {
  results: DictionarySearchResultDTO[];
  isLoading: boolean;
  onSelect: (result: DictionarySearchResultDTO) => void;
}) {
  if (isLoading) {
    return (
      <div className="p-3 bg-gray-50 border border-gray-200 rounded-md text-sm text-gray-500">
        Searching dictionary...
      </div>
    );
  }

  if (results.length === 0) {
    return (
      <div className="p-3 bg-amber-50 border border-amber-200 rounded-md text-sm text-amber-700">
        No dictionary match found. Will use LLM to generate inflections (slower).
      </div>
    );
  }

  return (
    <div className="border border-gray-200 rounded-md overflow-hidden">
      <div className="px-3 py-2 bg-gray-50 border-b border-gray-200">
        <p className="text-xs font-medium text-gray-600 uppercase tracking-wide">
          Dictionary matches ({results.length})
        </p>
      </div>
      <ul className="divide-y divide-gray-100 max-h-64 overflow-y-auto">
        {results.map((result) => (
          <li key={result.dictionaryWordId}>
            <button
              type="button"
              onClick={() => onSelect(result)}
              className="w-full text-left px-3 py-2.5 hover:bg-blue-50 transition-colors"
            >
              <div className="flex items-baseline gap-2">
                <span className="font-medium text-gray-900" lang="bg">
                  {result.word}
                </span>
                <span className="text-xs font-medium text-gray-500 bg-gray-100 px-1.5 py-0.5 rounded">
                  {result.pos}
                </span>
                <span className="text-sm text-gray-600">
                  {result.primaryTranslation}
                </span>
              </div>
              {result.alternateMeanings.length > 0 && (
                <p className="text-xs text-gray-400 mt-0.5">
                  Also: {result.alternateMeanings.slice(0, 3).join(', ')}
                </p>
              )}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

/**
 * Shows the selected dictionary entry with a way to deselect.
 */
function SelectedDictionaryEntry({
  result,
  onDeselect,
}: {
  result: DictionarySearchResultDTO;
  onDeselect: () => void;
}) {
  return (
    <div className="p-3 bg-green-50 border border-green-200 rounded-md">
      <div className="flex items-center justify-between">
        <div className="flex items-baseline gap-2">
          <span className="text-sm font-medium text-green-800">Dictionary match:</span>
          <span className="font-medium text-green-900" lang="bg">{result.word}</span>
          <span className="text-xs font-medium text-green-700 bg-green-100 px-1.5 py-0.5 rounded">
            {result.pos}
          </span>
          <span className="text-sm text-green-700">{result.primaryTranslation}</span>
        </div>
        <button
          type="button"
          onClick={onDeselect}
          className="text-green-600 hover:text-green-800 text-sm font-medium"
        >
          Change
        </button>
      </div>
      {result.forms.length > 0 && (
        <p className="text-xs text-green-600 mt-1">
          {result.forms.length} inflection{result.forms.length !== 1 ? 's' : ''} will be added from dictionary
        </p>
      )}
    </div>
  );
}
