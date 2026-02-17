interface SessionProgressProps {
  reviewed: number;
  total: number;
}

export function SessionProgress({ reviewed, total }: SessionProgressProps) {
  const percent = total > 0 ? Math.round((reviewed / total) * 100) : 0;
  return (
    <div className="mb-4">
      <div className="flex justify-between text-xs text-gray-500 mb-1">
        <span>{reviewed} of {total} reviewed</span>
        <span>{percent}%</span>
      </div>
      <div className="w-full bg-gray-200 rounded-full h-1.5">
        <div
          className="bg-blue-500 h-1.5 rounded-full transition-all duration-300"
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
}
