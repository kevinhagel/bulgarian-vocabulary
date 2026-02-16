import { useState, useEffect } from 'react';
import { Modal } from '@/components/ui/Modal';
import { VocabularyForm } from './VocabularyForm';
import { useVocabularyUIStore } from '../stores/useVocabularyUIStore';
import { useVocabularyDetail } from '../api/useVocabularyDetail';
import { useUpdateVocabulary } from '../api/useUpdateVocabulary';
import type { UpdateVocabularyFormData } from '../schemas/vocabularySchemas';

/**
 * Modal for editing existing vocabulary entries.
 * Fetches full lemma detail (with inflections) and pre-populates form.
 * Uses useUpdateVocabulary mutation to save changes.
 */
export function EditVocabularyModal() {
  const isOpen = useVocabularyUIStore((state) => state.isEditModalOpen);
  const editingLemmaId = useVocabularyUIStore((state) => state.editingLemmaId);
  const closeModal = useVocabularyUIStore((state) => state.closeEditModal);

  const detailQuery = useVocabularyDetail(editingLemmaId);
  const updateMutation = useUpdateVocabulary();
  const [error, setError] = useState<string | null>(null);

  // Reset error when modal opens
  useEffect(() => {
    if (isOpen) {
      setError(null);
    }
  }, [isOpen]);

  const handleSubmit = async (data: UpdateVocabularyFormData) => {
    if (!editingLemmaId) return;

    try {
      setError(null);
      await updateMutation.mutateAsync({
        id: editingLemmaId,
        data: {
          text: data.text,
          translation: data.translation,
          notes: data.notes || undefined,
          inflections: data.inflections?.map((inf) => ({
            id: inf.id,
            form: inf.form,
            grammaticalInfo: inf.grammaticalInfo || undefined,
          })),
        },
      });
      closeModal();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update vocabulary');
    }
  };

  const handleClose = () => {
    setError(null);
    closeModal();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Edit Vocabulary">
      {/* Loading state */}
      {detailQuery.isLoading && (
        <div className="py-8 text-center text-gray-600">
          Loading vocabulary details...
        </div>
      )}

      {/* Error loading detail */}
      {detailQuery.isError && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-md text-red-700 mb-4">
          Failed to load vocabulary details. Please try again.
        </div>
      )}

      {/* Update error */}
      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700">
          {error}
        </div>
      )}

      {/* Form (only show when data is loaded) */}
      {detailQuery.data && (
        <VocabularyForm
          mode="edit"
          defaultValues={{
            text: detailQuery.data.text,
            translation: detailQuery.data.translation,
            notes: detailQuery.data.notes || '',
            inflections: detailQuery.data.inflections.map((inf) => ({
              id: inf.id,
              form: inf.form,
              grammaticalInfo: inf.grammaticalInfo || '',
            })),
          }}
          onSubmit={handleSubmit}
          isSubmitting={updateMutation.isPending}
          onCancel={handleClose}
        />
      )}
    </Modal>
  );
}
