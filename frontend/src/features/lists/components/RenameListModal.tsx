import { useState } from 'react';
import { useRenameList } from '@/features/lists/api/useRenameList';
import { useListsUIStore } from '@/features/lists/stores/useListsUIStore';
import { Modal } from '@/components/ui/Modal';

interface Props { currentName: string; listId: number; }

export function RenameListModal({ currentName, listId }: Props) {
  const [name, setName] = useState(currentName);
  const closeRenameModal = useListsUIStore(s => s.closeRenameModal);
  const renameMutation = useRenameList();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    if (name.trim() === currentName) { closeRenameModal(); return; }
    await renameMutation.mutateAsync({ listId, name: name.trim() });
    closeRenameModal();
  };

  return (
    <Modal isOpen onClose={closeRenameModal} title="Rename List">
      <form onSubmit={handleSubmit} className="space-y-4">
        <input
          type="text"
          value={name}
          onChange={e => setName(e.target.value)}
          className="w-full border border-gray-300 rounded-xl px-4 py-3 text-base
                     focus:outline-none focus:ring-2 focus:ring-blue-500"
          autoFocus
          maxLength={100}
        />
        {renameMutation.isError && (
          <p className="text-red-500 text-sm">Failed to rename. Please try again.</p>
        )}
        <div className="flex gap-3">
          <button type="button" onClick={closeRenameModal}
            className="flex-1 py-3 border border-gray-300 rounded-xl text-gray-700
                       font-medium hover:bg-gray-50 transition-colors min-h-[44px]">
            Cancel
          </button>
          <button type="submit" disabled={!name.trim() || renameMutation.isPending}
            className="flex-1 py-3 bg-blue-600 text-white font-semibold rounded-xl
                       hover:bg-blue-700 transition-colors disabled:opacity-50 min-h-[44px]">
            {renameMutation.isPending ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
