import { useLists } from '@/features/lists/api/useLists';
import { useListsUIStore } from '@/features/lists/stores/useListsUIStore';
import { ListCard } from './ListCard';
import { CreateListModal } from './CreateListModal';

export function ListsView() {
  const { data: lists, isLoading, isError } = useLists();
  const { selectList, isCreateModalOpen, openCreateModal } = useListsUIStore();

  if (isLoading) return (
    <div className="flex justify-center py-12">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
    </div>
  );

  if (isError) return (
    <p className="text-red-500 text-center py-8">Failed to load lists. Please refresh.</p>
  );

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-bold text-gray-900">Word Lists</h2>
        <button
          onClick={openCreateModal}
          className="px-4 py-2 bg-blue-600 text-white font-semibold rounded-xl
                     hover:bg-blue-700 transition-colors text-sm min-h-[44px]"
        >
          + New List
        </button>
      </div>

      {lists && lists.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <p className="text-lg font-medium mb-2">No lists yet</p>
          <p className="text-sm">
            Create a list to organize vocabulary from a lesson or topic.
          </p>
          <button
            onClick={openCreateModal}
            className="mt-6 px-6 py-3 bg-blue-600 text-white font-semibold rounded-xl
                       hover:bg-blue-700 transition-colors"
          >
            Create Your First List
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {lists?.map(list => (
            <ListCard key={list.id} list={list} onClick={() => selectList(list.id)} />
          ))}
        </div>
      )}

      {isCreateModalOpen && <CreateListModal />}
    </div>
  );
}
