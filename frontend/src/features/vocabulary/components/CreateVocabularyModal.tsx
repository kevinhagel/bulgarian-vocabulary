import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { VocabularyForm } from './VocabularyForm';
import { useVocabularyUIStore } from '../stores/useVocabularyUIStore';
import { useCreateVocabulary } from '../api/useCreateVocabulary';
import type { CreateVocabularyFormData } from '../schemas/vocabularySchemas';

/**
 * Modal for creating new vocabulary entries.
 * Reads modal state from Zustand store and uses useCreateVocabulary mutation.
 * Shows "Processing with LLM..." status during async backend processing.
 */
export function CreateVocabularyModal() {
  const isOpen = useVocabularyUIStore((state) => state.isCreateModalOpen);
  const closeModal = useVocabularyUIStore((state) => state.closeCreateModal);
  const createMutation = useCreateVocabulary();
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (data: CreateVocabularyFormData) => {
    try {
      setError(null);
      await createMutation.mutateAsync({
        wordForm: data.wordForm,
        translation: data.translation,
        notes: data.notes || undefined,
      });
      closeModal();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create vocabulary');
    }
  };

  const handleClose = () => {
    setError(null);
    closeModal();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Add Vocabulary">
      {/* LLM processing status */}
      {createMutation.isPending && (
        <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-md text-blue-700">
          Processing with LLM... This may take a few seconds.
        </div>
      )}

      {/* Error message */}
      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700">
          {error}
        </div>
      )}

      <VocabularyForm
        mode="create"
        onSubmit={handleSubmit}
        isSubmitting={createMutation.isPending}
        onCancel={handleClose}
      />
    </Modal>
  );
}
