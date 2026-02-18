import { useDeleteList } from '@/features/lists/api/useDeleteList';
import { useListsUIStore } from '@/features/lists/stores/useListsUIStore';
import { Modal } from '@/components/ui/Modal';

interface Props { listName: string; listId: number; onDeleted: () => void; }

export function DeleteListConfirm({ listName, listId, onDeleted }: Props) {
  const closeDeleteConfirm = useListsUIStore(s => s.closeDeleteConfirm);
  const deleteMutation = useDeleteList();

  const handleDelete = async () => {
    await deleteMutation.mutateAsync({ listId });
    closeDeleteConfirm();
    onDeleted();
  };

  return (
    <Modal isOpen onClose={closeDeleteConfirm} title="Delete List">
      <p className="text-gray-600 mb-6">
        Delete <strong>"{listName}"</strong>? The list will be removed, but the vocabulary
        words themselves will not be deleted.
      </p>
      {deleteMutation.isError && (
        <p className="text-red-500 text-sm mb-4">Failed to delete. Please try again.</p>
      )}
      <div className="flex gap-3">
        <button onClick={closeDeleteConfirm}
          className="flex-1 py-3 border border-gray-300 rounded-xl text-gray-700
                     font-medium hover:bg-gray-50 transition-colors min-h-[44px]">
          Cancel
        </button>
        <button onClick={handleDelete} disabled={deleteMutation.isPending}
          className="flex-1 py-3 bg-red-600 text-white font-semibold rounded-xl
                     hover:bg-red-700 transition-colors disabled:opacity-50 min-h-[44px]">
          {deleteMutation.isPending ? 'Deleting…' : 'Delete List'}
        </button>
      </div>
    </Modal>
  );
}
