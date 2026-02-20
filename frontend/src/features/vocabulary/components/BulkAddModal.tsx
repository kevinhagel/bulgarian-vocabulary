import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import { Modal } from '@/components/ui/Modal';
import type { LemmaDetailDTO } from '@/features/vocabulary/types';

interface ParsedEntry {
  word: string;
  notes?: string;
}

interface WordStatus {
  word: string;
  notes?: string;
  status: 'pending' | 'adding' | 'added' | 'exists' | 'failed';
  lemmaText?: string;
  error?: string;
}

interface Props {
  onClose: () => void;
}

export function BulkAddModal({ onClose }: Props) {
  const [input, setInput] = useState('');
  const [statuses, setStatuses] = useState<WordStatus[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const queryClient = useQueryClient();

  const parseEntry = (line: string): ParsedEntry => {
    const match = line.match(/^(.+?)\s*\(notes:\s*(.+?)\)\s*$/);
    if (match) return { word: match[1].trim(), notes: match[2].trim() };
    return { word: line.trim() };
  };

  const parseEntries = (text: string): ParsedEntry[] =>
    text.split('\n').map(l => l.trim()).filter(l => l.length > 0).map(parseEntry);

  const updateStatus = (index: number, update: Partial<WordStatus>) =>
    setStatuses(prev => prev.map((s, i) => i === index ? { ...s, ...update } : s));

  const processWords = async () => {
    const entries = parseEntries(input);
    if (entries.length === 0) return;

    setIsProcessing(true);
    setStatuses(entries.map(e => ({ word: e.word, notes: e.notes, status: 'pending' })));

    for (let i = 0; i < entries.length; i++) {
      const { word, notes } = entries[i];
      updateStatus(i, { status: 'adding' });

      try {
        const res = await api.post<LemmaDetailDTO>('/vocabulary', {
          wordForm: word,
          translation: '',
          ...(notes && { notes }),
        });
        updateStatus(i, { status: 'added', lemmaText: res.data.text });
      } catch (err: unknown) {
        const status = (err as { response?: { status?: number } })?.response?.status;
        if (status === 409) {
          updateStatus(i, { status: 'exists' });
        } else {
          updateStatus(i, { status: 'failed', error: 'Failed — please try again' });
        }
      }
    }

    setIsProcessing(false);
    queryClient.invalidateQueries({ queryKey: ['vocabulary'] });
  };

  const statusIcon = (status: WordStatus['status']) => {
    switch (status) {
      case 'pending': return '○';
      case 'adding': return '…';
      case 'added': return '✓';
      case 'exists': return '=';
      case 'failed': return '✗';
    }
  };

  const statusColor = (status: WordStatus['status']) => {
    switch (status) {
      case 'added': return 'text-green-600';
      case 'exists': return 'text-blue-500';
      case 'failed': return 'text-red-500';
      case 'adding': return 'text-orange-500';
      default: return 'text-gray-400';
    }
  };

  const statusLabel = (s: WordStatus) => {
    switch (s.status) {
      case 'adding': return 'Adding…';
      case 'added': return s.lemmaText && s.lemmaText !== s.word ? `Added as "${s.lemmaText}"` : 'Added';
      case 'exists': return 'Already in vocabulary';
      case 'failed': return s.error ?? 'Failed';
      default: return '';
    }
  };

  const wordCount = parseEntries(input).length;
  const hasResults = statuses.length > 0;
  const allDone = hasResults && !isProcessing;

  return (
    <Modal isOpen onClose={onClose} title="Bulk Add Vocabulary" size="md">
      <div className="space-y-4">
        {!hasResults && (
          <>
            <p className="text-sm text-gray-500">
              Enter Bulgarian word forms, one per line. Inflected forms (e.g.{' '}
              <span lang="bg">чете</span>) are detected automatically. Add notes inline
              to disambiguate homographs:
            </p>
            <pre className="bg-gray-50 border border-gray-200 rounded-lg px-3 py-2 text-xs font-mono text-gray-600">
{`път (notes: noun meaning road)
път (notes: noun meaning time/occasion, NOT road)
чета
обичам`}
            </pre>
            <textarea
              value={input}
              onChange={e => setInput(e.target.value)}
              placeholder={'чета\nпера (notes: verb - to wash)\nобичам'}
              rows={8}
              className="w-full border border-gray-300 rounded-xl px-4 py-3 text-lg
                         focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              style={{ fontFamily: 'Sofia Sans, sans-serif' }}
              disabled={isProcessing}
              autoFocus
            />
            <p className="text-xs text-gray-400">
              {wordCount} {wordCount === 1 ? 'word' : 'words'} to add
            </p>
          </>
        )}

        {hasResults && (
          <div className="space-y-1 max-h-[50vh] overflow-y-auto">
            {statuses.map((s, i) => (
              <div key={i} className="flex items-start gap-3 py-2 border-b border-gray-50 last:border-0">
                <span className={`font-bold mt-0.5 w-4 shrink-0 ${statusColor(s.status)}`}>
                  {statusIcon(s.status)}
                </span>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-800" style={{ fontFamily: 'Sofia Sans, sans-serif' }}>
                    {s.word}{s.notes && <span className="text-xs text-gray-400 ml-2 font-normal">({s.notes})</span>}
                  </p>
                  {statusLabel(s) && (
                    <p className={`text-sm ${statusColor(s.status)}`}>{statusLabel(s)}</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="flex gap-3 pt-2">
          <button
            onClick={onClose}
            className="flex-1 py-3 border border-gray-300 rounded-xl text-gray-700
                       font-medium hover:bg-gray-50 transition-colors min-h-[44px]">
            {allDone ? 'Done' : 'Cancel'}
          </button>
          {!hasResults && (
            <button
              onClick={processWords}
              disabled={wordCount === 0 || isProcessing}
              className="flex-1 py-3 bg-blue-600 text-white font-semibold rounded-xl
                         hover:bg-blue-700 transition-colors disabled:opacity-50 min-h-[44px]">
              Add All
            </button>
          )}
          {allDone && (
            <button
              onClick={() => { setStatuses([]); setInput(''); }}
              className="flex-1 py-3 bg-blue-600 text-white font-semibold rounded-xl
                         hover:bg-blue-700 transition-colors min-h-[44px]">
              Add More
            </button>
          )}
        </div>
      </div>
    </Modal>
  );
}
