import { useState } from 'react';
import { useListDetail } from '@/features/lists/api/useListDetail';
import { useRemoveFromList } from '@/features/lists/api/useRemoveFromList';
import { useListsUIStore } from '@/features/lists/stores/useListsUIStore';
import { useStartListSession } from '@/features/lists/api/useStartListSession';
import { useStudyStore } from '@/features/study/stores/useStudyStore';
import { RenameListModal } from './RenameListModal';
import { DeleteListConfirm } from './DeleteListConfirm';

interface Props {
  listId: number;
  onBack: () => void;
  onStudyStarted: () => void;
  onAddVocabulary: () => void;
}

export function ListDetail({ listId, onBack, onStudyStarted, onAddVocabulary }: Props) {
  const { data: list, isLoading, isError } = useListDetail(listId);
  const removeMutation = useRemoveFromList();
  const startSessionMutation = useStartListSession();
  const { startSession } = useStudyStore();
  const { isRenameModalOpen, isDeleteConfirmOpen, openRenameModal, openDeleteConfirm } = useListsUIStore();
  const [studyError, setStudyError] = useState<string | null>(null);

  const handleStudy = async (mode: 'DUE' | 'ALL') => {
    setStudyError(null);
    try {
      const result = await startSessionMutation.mutateAsync({ listId, mode });
      startSession(result.sessionId, result.cardCount, result.firstCard);
      onStudyStarted();
    } catch {
      setStudyError(
        mode === 'DUE'
          ? 'No cards due right now. Try "Practice All" to review everything.'
          : 'No cards available to study. Add vocabulary to this list first.'
      );
    }
  };

  if (isLoading) return (
    <div className="flex justify-center py-12">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
    </div>
  );

  if (isError || !list) return (
    <p className="text-red-500 text-center py-8">Failed to load list. Please try again.</p>
  );

  return (
    <div>
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <button
          onClick={onBack}
          className="p-2 rounded-lg hover:bg-gray-100 transition-colors text-gray-500 text-lg leading-none"
          aria-label="Back to lists"
        >
          ←
        </button>
        <h2 className="text-xl font-bold text-gray-900 flex-1 min-w-0 truncate">{list.name}</h2>
      </div>

      {/* Action buttons */}
      <div className="grid grid-cols-2 gap-3 mb-4">
        <button
          onClick={onAddVocabulary}
          className="col-span-2 py-3 px-4 bg-blue-600 text-white font-semibold rounded-xl
                     hover:bg-blue-700 transition-colors text-sm min-h-[44px]"
        >
          + Add Vocabulary
        </button>
        <button
          onClick={() => handleStudy('DUE')}
          disabled={startSessionMutation.isPending || list.lemmas.length === 0}
          className="py-3 px-4 bg-orange-500 text-white font-semibold rounded-xl
                     hover:bg-orange-600 transition-colors disabled:opacity-50 text-sm min-h-[44px]"
        >
          {startSessionMutation.isPending ? 'Starting…' : 'Study Due'}
        </button>
        <button
          onClick={() => handleStudy('ALL')}
          disabled={startSessionMutation.isPending || list.lemmas.length === 0}
          className="py-3 px-4 bg-green-600 text-white font-semibold rounded-xl
                     hover:bg-green-700 transition-colors disabled:opacity-50 text-sm min-h-[44px]"
        >
          Practice All
        </button>
        <button
          onClick={openRenameModal}
          className="py-3 px-3 border border-gray-300 text-gray-700 font-medium rounded-xl
                     hover:bg-gray-50 transition-colors text-sm min-h-[44px]"
        >
          Rename
        </button>
        <button
          onClick={openDeleteConfirm}
          className="py-3 px-3 border border-red-200 text-red-600 font-medium rounded-xl
                     hover:bg-red-50 transition-colors text-sm min-h-[44px]"
        >
          Delete List
        </button>
      </div>

      {studyError && (
        <p className="text-orange-700 text-sm bg-orange-50 border border-orange-200 rounded-lg px-4 py-3 mb-4">
          {studyError}
        </p>
      )}

      {/* Member list */}
      {list.lemmas.length === 0 ? (
        <div className="text-center py-10 text-gray-400">
          <p className="font-medium">No vocabulary yet</p>
          <p className="text-sm mt-1">Tap "Add Vocabulary" to add words to this list.</p>
        </div>
      ) : (
        <div>
          <p className="text-sm text-gray-400 mb-3">
            {list.lemmas.length} {list.lemmas.length === 1 ? 'word' : 'words'}
          </p>
          <div className="space-y-1">
            {list.lemmas.map(lemma => (
              <div
                key={lemma.id}
                className="flex items-center gap-3 py-3 px-4 bg-white rounded-xl border border-gray-100
                           hover:border-gray-200 transition-colors"
              >
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900" style={{ fontFamily: 'Sofia Sans, sans-serif' }}>
                    {lemma.text}
                  </p>
                  {lemma.translation && (
                    <p className="text-sm text-gray-500 truncate">{lemma.translation}</p>
                  )}
                </div>
                <button
                  onClick={() => removeMutation.mutate({ listId, lemmaId: lemma.id })}
                  disabled={removeMutation.isPending}
                  className="p-1.5 text-gray-300 hover:text-red-400 transition-colors rounded shrink-0"
                  aria-label={`Remove ${lemma.text} from list`}
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Modals */}
      {isRenameModalOpen && <RenameListModal currentName={list.name} listId={listId} />}
      {isDeleteConfirmOpen && (
        <DeleteListConfirm listName={list.name} listId={listId} onDeleted={onBack} />
      )}
    </div>
  );
}
