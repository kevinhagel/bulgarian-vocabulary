import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { useReprocessVocabulary } from '../api/useReprocessVocabulary';

interface ReprocessModalProps {
  lemmaId: number;
  lemmaText: string;
  currentNotes: string | null;
  onClose: () => void;
  onSuccess: () => void;
}

/**
 * Modal for reprocessing a vocabulary entry through the LLM pipeline.
 * Optionally accepts a disambiguation hint to guide the LLM.
 */
export function ReprocessModal({ lemmaId, lemmaText, currentNotes, onClose, onSuccess }: ReprocessModalProps) {
  const [hint, setHint] = useState('');
  const reprocess = useReprocessVocabulary();

  const handleSubmit = async () => {
    await reprocess.mutateAsync({ id: lemmaId, hint: hint.trim() || undefined });
    onSuccess();
    onClose();
  };

  return (
    <Modal isOpen title="Reprocess Word" onClose={onClose} size="sm">
      <div className="space-y-4">
        <p className="text-sm text-gray-500">
          Re-run the LLM pipeline for{' '}
          <span className="font-medium text-gray-800" lang="bg">{lemmaText}</span>.
          Existing inflections will be cleared and regenerated.
        </p>

        {currentNotes && (
          <div className="bg-gray-50 border border-gray-200 rounded p-3 text-sm text-gray-600">
            <span className="font-medium text-gray-700">Current notes: </span>
            {currentNotes}
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Disambiguation hint <span className="text-gray-400 font-normal">(optional)</span>
          </label>
          <input
            type="text"
            value={hint}
            onChange={e => setHint(e.target.value)}
            placeholder='e.g. "verb – to wash" or "noun – feather"'
            className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <p className="text-xs text-gray-400 mt-1">
            The hint will be appended to the word's notes for LLM disambiguation.
          </p>
        </div>

        {reprocess.isError && (
          <p className="text-red-500 text-sm">Reprocessing failed. Please try again.</p>
        )}

        <div className="flex gap-3 pt-2">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={reprocess.isPending}
            className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors disabled:opacity-50"
          >
            {reprocess.isPending ? 'Queuing…' : 'Reprocess'}
          </button>
        </div>
      </div>
    </Modal>
  );
}
