import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { useVocabularyUIStore } from '../stores/useVocabularyUIStore';
import { useDeleteVocabulary } from '../api/useDeleteVocabulary';

/**
 * Confirmation dialog for deleting vocabulary entries.
 * Shows warning message and uses useDeleteVocabulary mutation.
 */
export function DeleteConfirmDialog() {
  const isOpen = useVocabularyUIStore((state) => state.isDeleteConfirmOpen);
  const deletingLemmaId = useVocabularyUIStore((state) => state.deletingLemmaId);
  const closeDialog = useVocabularyUIStore((state) => state.closeDeleteConfirm);

  const deleteMutation = useDeleteVocabulary();
  const [error, setError] = useState<string | null>(null);

  const handleDelete = async () => {
    if (!deletingLemmaId) return;

    try {
      setError(null);
      await deleteMutation.mutateAsync(deletingLemmaId);
      closeDialog();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete vocabulary');
    }
  };

  const handleClose = () => {
    setError(null);
    closeDialog();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Delete Vocabulary" size="sm">
      {/* Error message */}
      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700">
          {error}
        </div>
      )}

      {/* Confirmation message */}
      <p className="text-gray-700 mb-6">
        Are you sure you want to delete this vocabulary entry? This action cannot be undone.
      </p>

      {/* Action buttons */}
      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={handleClose}
          className="px-4 py-2 text-gray-700 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors"
          disabled={deleteMutation.isPending}
        >
          Cancel
        </button>
        <button
          type="button"
          onClick={handleDelete}
          className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors disabled:bg-red-300 disabled:cursor-not-allowed"
          disabled={deleteMutation.isPending}
        >
          {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
        </button>
      </div>
    </Modal>
  );
}
