import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

// --- Types ---

interface LemmaStats {
  total: number;
  completed: number;
  failed: number;
  processing: number;
  queued: number;
  reviewed: number;
  pending: number;
  needsCorrection: number;
}

interface SentenceStats {
  done: number;
  none: number;
  queued: number;
  generating: number;
  failed: number;
}

interface IssueLemma {
  id: number;
  text: string;
  notes: string | null;
  processingStatus: string;
  errorMessage: string | null;
  updatedAt: string | null;
}

interface DuplicateEntry {
  id: number;
  notes: string | null;
  processingStatus: string;
  createdAt: string | null;
}

interface DuplicateGroup {
  text: string;
  source: string;
  entries: DuplicateEntry[];
}

interface FailedSentence {
  id: number;
  text: string;
}

interface AdminStats {
  lemmas: LemmaStats;
  sentences: SentenceStats;
  totalInflections: number;
  failedLemmas: IssueLemma[];
  stuckLemmas: IssueLemma[];
  duplicates: DuplicateGroup[];
  failedSentences: FailedSentence[];
  avgSentenceSeconds: number;
}

// --- Components ---

function StatCard({ label, value, color = 'gray' }: { label: string; value: number; color?: string }) {
  const colorMap: Record<string, string> = {
    green:  'text-green-700 bg-green-50 border-green-200',
    red:    'text-red-700 bg-red-50 border-red-200',
    yellow: 'text-yellow-700 bg-yellow-50 border-yellow-200',
    blue:   'text-blue-700 bg-blue-50 border-blue-200',
    gray:   'text-gray-700 bg-gray-50 border-gray-200',
  };
  return (
    <div className={`rounded-lg border p-3 text-center ${colorMap[color] ?? colorMap.gray}`}>
      <div className="text-2xl font-bold">{value.toLocaleString()}</div>
      <div className="text-xs font-medium mt-0.5">{label}</div>
    </div>
  );
}

function IssueTable({ title, items, emptyMsg }: {
  title: string;
  items: IssueLemma[];
  emptyMsg: string;
}) {
  return (
    <section>
      <h3 className="text-sm font-semibold text-gray-700 mb-2">{title}</h3>
      {items.length === 0 ? (
        <p className="text-sm text-gray-400 italic">{emptyMsg}</p>
      ) : (
        <div className="overflow-x-auto rounded border border-gray-200">
          <table className="min-w-full text-xs">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-3 py-2 text-left font-medium text-gray-500">ID</th>
                <th className="px-3 py-2 text-left font-medium text-gray-500">Word</th>
                <th className="px-3 py-2 text-left font-medium text-gray-500">Notes</th>
                <th className="px-3 py-2 text-left font-medium text-gray-500">Status</th>
                <th className="px-3 py-2 text-left font-medium text-gray-500">Error</th>
                <th className="px-3 py-2 text-left font-medium text-gray-500">Updated</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {items.map(item => (
                <tr key={item.id} className="hover:bg-gray-50">
                  <td className="px-3 py-2 text-gray-500">{item.id}</td>
                  <td className="px-3 py-2 font-medium text-gray-900">{item.text}</td>
                  <td className="px-3 py-2 text-gray-500 max-w-[160px] truncate">{item.notes ?? '—'}</td>
                  <td className="px-3 py-2">
                    <span className="px-1.5 py-0.5 rounded text-xs bg-red-100 text-red-700">
                      {item.processingStatus}
                    </span>
                  </td>
                  <td className="px-3 py-2 text-gray-500 max-w-[240px] truncate" title={item.errorMessage ?? ''}>
                    {item.errorMessage ?? '—'}
                  </td>
                  <td className="px-3 py-2 text-gray-400">
                    {item.updatedAt ? item.updatedAt.replace('T', ' ').substring(0, 16) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

// --- Main dashboard ---

export function AdminDashboard() {
  const queryClient = useQueryClient();
  const [clearMsg, setClearMsg] = useState<string | null>(null);

  const { data: stats, isLoading, isError, refetch } = useQuery<AdminStats>({
    queryKey: ['admin', 'stats'],
    queryFn: () => api.get<AdminStats>('/admin/stats').then(r => r.data),
    staleTime: 30_000,
    refetchInterval: (query) => {
      const d = query.state.data;
      const active = (d?.sentences?.generating ?? 0) + (d?.sentences?.queued ?? 0);
      return active > 0 ? 8_000 : false;
    },
  });

  const clearCache = useMutation({
    mutationFn: () => api.post('/admin/cache/clear'),
    onSuccess: () => {
      queryClient.invalidateQueries();
      setClearMsg('Cache cleared.');
      setTimeout(() => setClearMsg(null), 3000);
    },
  });

  if (isLoading) return (
    <div className="flex items-center justify-center py-20">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
    </div>
  );

  if (isError || !stats) return (
    <div className="text-center py-12 text-red-600">Failed to load admin stats.</div>
  );

  const { lemmas, sentences, totalInflections, failedLemmas, stuckLemmas, duplicates, failedSentences, avgSentenceSeconds } = stats;

  return (
    <div className="space-y-6 max-w-5xl">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-gray-900">Admin Dashboard</h2>
        <div className="flex items-center gap-3">
          {clearMsg && <span className="text-sm text-green-600">{clearMsg}</span>}
          <button
            onClick={() => clearCache.mutate()}
            disabled={clearCache.isPending}
            className="px-3 py-1.5 text-sm rounded border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:opacity-50"
          >
            Clear cache
          </button>
          <button
            onClick={() => refetch()}
            className="px-3 py-1.5 text-sm rounded border border-gray-300 text-gray-600 hover:bg-gray-50"
          >
            Refresh
          </button>
        </div>
      </div>

      {/* Lemma stats */}
      <section>
        <h3 className="text-sm font-semibold text-gray-700 mb-2">Lemmas</h3>
        <div className="grid grid-cols-4 gap-3 sm:grid-cols-8">
          <StatCard label="Total" value={lemmas.total} color="blue" />
          <StatCard label="Completed" value={lemmas.completed} color="green" />
          <StatCard label="Failed" value={lemmas.failed} color="red" />
          <StatCard label="Processing" value={lemmas.processing} color="yellow" />
          <StatCard label="Queued" value={lemmas.queued} />
          <StatCard label="Reviewed" value={lemmas.reviewed} color="green" />
          <StatCard label="Pending review" value={lemmas.pending} color="yellow" />
          <StatCard label="Needs correction" value={lemmas.needsCorrection} color="red" />
        </div>
      </section>

      {/* Inflections */}
      <section>
        <h3 className="text-sm font-semibold text-gray-700 mb-2">Inflections</h3>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <StatCard label="Total inflections" value={totalInflections} color="blue" />
        </div>
      </section>

      {/* Sentence generation */}
      <section>
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-sm font-semibold text-gray-700">Example sentences</h3>
          {(sentences.generating > 0 || sentences.queued > 0) && (
            <span className="flex items-center gap-1.5 text-xs text-blue-600 font-medium">
              <span className="inline-block w-2 h-2 rounded-full bg-blue-500 animate-pulse" />
              Generating — auto-refreshing
            </span>
          )}
        </div>

        {/* Progress bar */}
        {(() => {
          const total = sentences.done + sentences.none + sentences.queued + sentences.generating + sentences.failed;
          const pct = total > 0 ? Math.round((sentences.done / total) * 100) : 0;
          const remaining = sentences.none + sentences.queued + sentences.generating;
          const estMinutes = remaining > 0 && avgSentenceSeconds > 0
            ? Math.ceil((remaining * avgSentenceSeconds) / 60)
            : 0;
          return (
            <div className="mb-4">
              <div className="flex justify-between text-xs text-gray-500 mb-1">
                <span>{sentences.done} / {total} words have sentences ({pct}%)</span>
                {remaining > 0 && estMinutes > 0 && (
                  <span>~{estMinutes < 60 ? `${estMinutes} min` : `${Math.round(estMinutes/60)}h ${estMinutes%60}m`} remaining at {Math.round(avgSentenceSeconds)}s/word</span>
                )}
                {remaining === 0 && total > 0 && (
                  <span className="text-green-600 font-medium">All done</span>
                )}
              </div>
              <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-500"
                  style={{
                    width: `${pct}%`,
                    backgroundColor: pct === 100 ? '#16a34a' : sentences.generating > 0 ? '#3b82f6' : '#6366f1',
                  }}
                />
              </div>
            </div>
          );
        })()}

        {/* Status cards */}
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-5">
          <StatCard label="Done" value={sentences.done} color="green" />
          <StatCard label="None" value={sentences.none} />
          <StatCard label="Queued" value={sentences.queued} color="yellow" />
          <StatCard label="Generating" value={sentences.generating} color="blue" />
          <StatCard label="Failed" value={sentences.failed} color="red" />
        </div>

        {/* Failed sentence list */}
        {failedSentences.length > 0 && (
          <div className="mt-3 rounded border border-red-200 bg-red-50 p-3">
            <p className="text-xs font-semibold text-red-700 mb-1.5">
              Words where sentence generation failed ({failedSentences.length})
            </p>
            <div className="flex flex-wrap gap-1.5">
              {failedSentences.map(f => (
                <span key={f.id} className="px-2 py-0.5 text-xs rounded bg-red-100 text-red-800 font-medium">
                  {f.text}
                </span>
              ))}
            </div>
          </div>
        )}
      </section>

      {/* Failed lemmas */}
      <IssueTable
        title={`Failed lemmas (${failedLemmas.length})`}
        items={failedLemmas}
        emptyMsg="No failed lemmas."
      />

      {/* Stuck lemmas */}
      <IssueTable
        title={`Stuck lemmas — PROCESSING > 15 min (${stuckLemmas.length})`}
        items={stuckLemmas}
        emptyMsg="No stuck lemmas."
      />

      {/* Duplicates */}
      <section>
        <h3 className="text-sm font-semibold text-gray-700 mb-2">
          Duplicate groups ({duplicates.length})
        </h3>
        {duplicates.length === 0 ? (
          <p className="text-sm text-gray-400 italic">No duplicates found.</p>
        ) : (
          <div className="space-y-3">
            {duplicates.map(group => (
              <div key={`${group.text}|${group.source}`}
                   className="rounded border border-yellow-200 bg-yellow-50 p-3">
                <div className="text-sm font-semibold text-yellow-800 mb-2">
                  "{group.text}" — source: {group.source}
                </div>
                <div className="overflow-x-auto">
                  <table className="min-w-full text-xs">
                    <thead>
                      <tr className="text-yellow-700">
                        <th className="pr-4 text-left font-medium">ID</th>
                        <th className="pr-4 text-left font-medium">Notes</th>
                        <th className="pr-4 text-left font-medium">Status</th>
                        <th className="text-left font-medium">Created</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-yellow-100">
                      {group.entries.map(e => (
                        <tr key={e.id}>
                          <td className="pr-4 py-1 text-gray-600">{e.id}</td>
                          <td className="pr-4 py-1 text-gray-700">{e.notes ?? '—'}</td>
                          <td className="pr-4 py-1 text-gray-600">{e.processingStatus}</td>
                          <td className="py-1 text-gray-400">
                            {e.createdAt ? e.createdAt.replace('T', ' ').substring(0, 16) : '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
