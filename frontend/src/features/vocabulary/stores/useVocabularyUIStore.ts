import { create } from 'zustand';
import type { Source, PartOfSpeech, DifficultyLevel } from '@/types';

interface VocabularyUIState {
  // Search and filters
  searchQuery: string;
  selectedSource: Source | null;
  selectedPartOfSpeech: PartOfSpeech | null;
  selectedDifficultyLevel: DifficultyLevel | null;

  // Pagination
  currentPage: number;
  pageSize: number;

  // Modal states
  isCreateModalOpen: boolean;
  isEditModalOpen: boolean;
  editingLemmaId: number | null;
  isDeleteConfirmOpen: boolean;
  deletingLemmaId: number | null;

  // Actions
  setSearchQuery: (query: string) => void;
  setFilter: (filter: Partial<{
    selectedSource: Source | null;
    selectedPartOfSpeech: PartOfSpeech | null;
    selectedDifficultyLevel: DifficultyLevel | null;
  }>) => void;
  setCurrentPage: (page: number) => void;
  resetFilters: () => void;

  // Modal actions
  openCreateModal: () => void;
  closeCreateModal: () => void;
  openEditModal: (id: number) => void;
  closeEditModal: () => void;
  openDeleteConfirm: (id: number) => void;
  closeDeleteConfirm: () => void;
}

/**
 * Zustand store for vocabulary UI state management.
 * Manages search query, filters, pagination, and modal visibility.
 *
 * ALWAYS use selectors in components:
 * const searchQuery = useVocabularyUIStore(state => state.searchQuery);
 */
export const useVocabularyUIStore = create<VocabularyUIState>((set) => ({
  // Initial state
  searchQuery: '',
  selectedSource: null,
  selectedPartOfSpeech: null,
  selectedDifficultyLevel: null,
  currentPage: 0,
  pageSize: 20,
  isCreateModalOpen: false,
  isEditModalOpen: false,
  editingLemmaId: null,
  isDeleteConfirmOpen: false,
  deletingLemmaId: null,

  // Search action - resets page when search changes
  setSearchQuery: (query: string) =>
    set({ searchQuery: query, currentPage: 0 }),

  // Filter action - resets page when filters change
  setFilter: (filter) =>
    set((state) => ({
      ...state,
      ...filter,
      currentPage: 0, // Always reset to first page on filter change
    })),

  // Pagination action
  setCurrentPage: (page: number) =>
    set({ currentPage: page }),

  // Reset all filters and search, go back to first page
  resetFilters: () =>
    set({
      searchQuery: '',
      selectedSource: null,
      selectedPartOfSpeech: null,
      selectedDifficultyLevel: null,
      currentPage: 0,
    }),

  // Modal actions
  openCreateModal: () =>
    set({ isCreateModalOpen: true }),

  closeCreateModal: () =>
    set({ isCreateModalOpen: false }),

  openEditModal: (id: number) =>
    set({ isEditModalOpen: true, editingLemmaId: id }),

  closeEditModal: () =>
    set({ isEditModalOpen: false, editingLemmaId: null }),

  openDeleteConfirm: (id: number) =>
    set({ isDeleteConfirmOpen: true, deletingLemmaId: id }),

  closeDeleteConfirm: () =>
    set({ isDeleteConfirmOpen: false, deletingLemmaId: null }),
}));
