import { useVocabularyUIStore } from '../stores/useVocabularyUIStore';
import type { Source, PartOfSpeech, DifficultyLevel } from '@/types';

// Constant arrays for enum-like types
const SOURCE_VALUES: Source[] = ['USER_ENTERED', 'SYSTEM_SEED'];
const PART_OF_SPEECH_VALUES: PartOfSpeech[] = [
  'NOUN', 'VERB', 'ADJECTIVE', 'ADVERB', 'PRONOUN',
  'PREPOSITION', 'CONJUNCTION', 'NUMERAL', 'INTERJECTION',
  'PARTICLE', 'INTERROGATIVE'
];
const DIFFICULTY_LEVEL_VALUES: DifficultyLevel[] = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

/**
 * Converts enum values to display-friendly labels.
 * E.g., "USER_ENTERED" -> "User Entered", "NOUN" -> "Noun"
 */
function formatEnumLabel(value: string): string {
  return value
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
}

/**
 * Filter controls for vocabulary list: search input and dropdowns for
 * source, part of speech, and difficulty level.
 */
export function VocabularyFilters() {
  const searchQuery = useVocabularyUIStore(state => state.searchQuery);
  const selectedSource = useVocabularyUIStore(state => state.selectedSource);
  const selectedPartOfSpeech = useVocabularyUIStore(state => state.selectedPartOfSpeech);
  const selectedDifficultyLevel = useVocabularyUIStore(state => state.selectedDifficultyLevel);
  const setSearchQuery = useVocabularyUIStore(state => state.setSearchQuery);
  const setFilter = useVocabularyUIStore(state => state.setFilter);
  const resetFilters = useVocabularyUIStore(state => state.resetFilters);

  // Check if any filter is active
  const hasActiveFilters = searchQuery || selectedSource || selectedPartOfSpeech || selectedDifficultyLevel;

  return (
    <div className="flex flex-col md:flex-row gap-3 mb-6">
      {/* Search input */}
      <div className="flex-1">
        <input
          type="text"
          placeholder="Search Bulgarian text..."
          lang="bg"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>

      {/* Source filter */}
      <div className="w-full md:w-48">
        <select
          value={selectedSource || ''}
          onChange={(e) => setFilter({ selectedSource: e.target.value ? e.target.value as Source : null })}
          className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          <option value="">All Sources</option>
          {SOURCE_VALUES.map(source => (
            <option key={source} value={source}>
              {formatEnumLabel(source)}
            </option>
          ))}
        </select>
      </div>

      {/* Part of Speech filter */}
      <div className="w-full md:w-48">
        <select
          value={selectedPartOfSpeech || ''}
          onChange={(e) => setFilter({ selectedPartOfSpeech: e.target.value ? e.target.value as PartOfSpeech : null })}
          className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          <option value="">All Parts of Speech</option>
          {PART_OF_SPEECH_VALUES.map(pos => (
            <option key={pos} value={pos}>
              {formatEnumLabel(pos)}
            </option>
          ))}
        </select>
      </div>

      {/* Difficulty Level filter */}
      <div className="w-full md:w-48">
        <select
          value={selectedDifficultyLevel || ''}
          onChange={(e) => setFilter({ selectedDifficultyLevel: e.target.value ? e.target.value as DifficultyLevel : null })}
          className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          <option value="">All Difficulty Levels</option>
          {DIFFICULTY_LEVEL_VALUES.map(level => (
            <option key={level} value={level}>
              {formatEnumLabel(level)}
            </option>
          ))}
        </select>
      </div>

      {/* Reset filters button */}
      {hasActiveFilters && (
        <button
          onClick={resetFilters}
          className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 transition-colors whitespace-nowrap"
        >
          Reset Filters
        </button>
      )}
    </div>
  );
}
