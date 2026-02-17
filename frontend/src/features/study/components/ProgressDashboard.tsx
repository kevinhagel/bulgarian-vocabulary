import { useProgress } from '@/features/study/api/useProgress';

function StatTile({ value, label, colorClass }: { value: number | string; label: string; colorClass: string }) {
  return (
    <div className="bg-white rounded-lg shadow-sm p-4 text-center">
      <div className={`text-2xl font-bold ${colorClass}`}>{value}</div>
      <div className="text-xs text-gray-500 mt-1">{label}</div>
    </div>
  );
}

export function ProgressDashboard() {
  const { data } = useProgress();
  if (!data) return null;

  const studied = data.totalUserVocab > 0
    ? Math.round((data.totalVocabStudied / data.totalUserVocab) * 100)
    : 0;
  const retentionColor = data.retentionRate >= 80 ? 'text-green-600' :
    data.retentionRate >= 50 ? 'text-yellow-600' : 'text-red-500';

  return (
    <div className="max-w-md mx-auto mt-6">
      <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3 text-center">Your Progress</h3>
      <div className="grid grid-cols-2 gap-3 mb-4">
        <StatTile value={data.totalUserVocab} label="Total Vocabulary" colorClass="text-gray-800" />
        <StatTile value={data.totalSessions} label="Study Sessions" colorClass="text-blue-600" />
        <StatTile value={data.totalCardsReviewed} label="Cards Reviewed" colorClass="text-green-600" />
        <StatTile value={`${data.retentionRate}%`} label="Retention Rate" colorClass={retentionColor} />
      </div>
      {data.totalUserVocab > 0 && (
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex justify-between text-xs text-gray-500 mb-1">
            <span>{data.totalVocabStudied} words studied</span>
            <span>{data.totalUserVocab} total</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div className="bg-blue-500 h-2 rounded-full transition-all" style={{ width: `${studied}%` }} />
          </div>
          <p className="text-xs text-gray-400 text-center mt-2">{studied}% studied at least once</p>
        </div>
      )}
    </div>
  );
}
