import { create } from 'zustand';

interface ListsUIState {
  selectedListId: number | null;
  isCreateModalOpen: boolean;
  isRenameModalOpen: boolean;
  isDeleteConfirmOpen: boolean;

  selectList: (id: number) => void;
  clearSelection: () => void;
  openCreateModal: () => void;
  closeCreateModal: () => void;
  openRenameModal: () => void;
  closeRenameModal: () => void;
  openDeleteConfirm: () => void;
  closeDeleteConfirm: () => void;
}

export const useListsUIStore = create<ListsUIState>((set) => ({
  selectedListId: null,
  isCreateModalOpen: false,
  isRenameModalOpen: false,
  isDeleteConfirmOpen: false,

  selectList: (id) => set({ selectedListId: id }),
  clearSelection: () => set({ selectedListId: null }),
  openCreateModal: () => set({ isCreateModalOpen: true }),
  closeCreateModal: () => set({ isCreateModalOpen: false }),
  openRenameModal: () => set({ isRenameModalOpen: true }),
  closeRenameModal: () => set({ isRenameModalOpen: false }),
  openDeleteConfirm: () => set({ isDeleteConfirmOpen: true }),
  closeDeleteConfirm: () => set({ isDeleteConfirmOpen: false }),
}));
