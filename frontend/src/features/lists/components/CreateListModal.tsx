import { useState } from 'react';
import { useCreateList } from '@/features/lists/api/useCreateList';
import { useListsUIStore } from '@/features/lists/stores/useListsUIStore';
import { Modal } from '@/components/ui/Modal';

export function CreateListModal() {
  const [name, setName] = useState('');
  const closeCreateModal = useListsUIStore(s => s.closeCreateModal);
  const createMutation = useCreateList();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    await createMutation.mutateAsync({ name: name.trim() });
    closeCreateModal();
  };

  return (
    <Modal isOpen onClose={closeCreateModal} title="New List">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            List name
          </label>
          <input
            type="text"
            value={name}
            onChange={e => setName(e.target.value)}
            placeholder="e.g. Elena Lesson 3, Restaurant Vocab"
            className="w-full border border-gray-300 rounded-xl px-4 py-3 text-base
                       focus:outline-none focus:ring-2 focus:ring-blue-500"
            autoFocus
            maxLength={100}
          />
        </div>
        {createMutation.isError && (
          <p className="text-red-500 text-sm">Failed to create list. Please try again.</p>
        )}
        <div className="flex gap-3 pt-2">
          <button type="button" onClick={closeCreateModal}
            className="flex-1 py-3 border border-gray-300 rounded-xl text-gray-700
                       font-medium hover:bg-gray-50 transition-colors min-h-[44px]">
            Cancel
          </button>
          <button type="submit" disabled={!name.trim() || createMutation.isPending}
            className="flex-1 py-3 bg-blue-600 text-white font-semibold rounded-xl
                       hover:bg-blue-700 transition-colors disabled:opacity-50 min-h-[44px]">
            {createMutation.isPending ? 'Creating…' : 'Create'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
