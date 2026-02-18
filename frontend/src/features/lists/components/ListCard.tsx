import type { WordListSummaryDTO } from '@/features/lists/types';

interface ListCardProps {
  list: WordListSummaryDTO;
  onClick: () => void;
}

export function ListCard({ list, onClick }: ListCardProps) {
  return (
    <button
      onClick={onClick}
      className="w-full text-left bg-white rounded-xl shadow-sm border border-gray-100
                 p-4 hover:shadow-md hover:border-blue-200 transition-all min-h-[80px]
                 flex flex-col justify-between active:bg-gray-50"
    >
      <p className="font-semibold text-gray-900 text-base leading-snug">{list.name}</p>
      <p className="text-sm text-gray-400 mt-2">
        {list.lemmaCount === 0
          ? 'Empty — tap to add vocabulary'
          : `${list.lemmaCount} ${list.lemmaCount === 1 ? 'word' : 'words'}`}
      </p>
    </button>
  );
}
