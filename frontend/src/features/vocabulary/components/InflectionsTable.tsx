import { AudioPlayButton } from '@/components/audio/AudioPlayButton';
import type { InflectionDTO } from '@/types';

interface InflectionsTableProps {
  inflections: InflectionDTO[];
}

/**
 * Responsive table component displaying inflections with audio playback.
 * Desktop: table layout with header row.
 * Mobile: card-like stacked layout.
 */
export function InflectionsTable({ inflections }: InflectionsTableProps) {
  // Empty state
  if (inflections.length === 0) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-6 text-center">
        <svg className="inline-block w-12 h-12 text-gray-400 mb-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <p className="text-gray-700 font-medium">No inflections available</p>
        <p className="text-gray-600 text-sm mt-1">This lemma has no stored inflections.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      {/* Desktop table layout (hidden on mobile) */}
      <table className="hidden md:table min-w-full border-collapse bg-white rounded-lg overflow-hidden">
        <thead>
          <tr className="bg-gray-100 border-b border-gray-200">
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
              Inflected Form
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
              Grammatical Info
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
              Audio
            </th>
          </tr>
        </thead>
        <tbody>
          {inflections.map((inflection, index) => (
            <tr
              key={inflection.id}
              className={`border-b border-gray-200 ${index % 2 === 0 ? 'bg-white' : 'bg-gray-50'}`}
            >
              <td className="px-6 py-4">
                <span className="text-2xl font-semibold text-gray-900" lang="bg">
                  {inflection.form}
                </span>
              </td>
              <td className="px-6 py-4 text-sm text-gray-600">
                {inflection.grammaticalInfo || '—'}
              </td>
              <td className="px-6 py-4">
                <AudioPlayButton text={inflection.form} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Mobile card layout (visible on mobile only) */}
      <div className="md:hidden space-y-3">
        {inflections.map((inflection) => (
          <div key={inflection.id} className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
            <div className="mb-3">
              <span className="text-2xl font-semibold text-gray-900" lang="bg">
                {inflection.form}
              </span>
            </div>
            <div className="mb-3 text-sm text-gray-600">
              <span className="font-medium text-gray-700">Grammatical Info:</span>{' '}
              {inflection.grammaticalInfo || '—'}
            </div>
            <div>
              <AudioPlayButton text={inflection.form} />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
