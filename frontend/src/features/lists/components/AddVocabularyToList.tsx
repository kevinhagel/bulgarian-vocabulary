import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import { Modal } from '@/components/ui/Modal';
import type { LemmaDetailDTO } from '@/features/vocabulary/types';

interface WordStatus {
  word: string;
  status: 'pending' | 'adding' | 'added' | 'duplicate_found' | 'failed';
  lemmaText?: string;
  error?: string;
}

interface Props {
  listId: number;
  onClose: () => void;
}

export function AddVocabularyToList({ listId, onClose }: Props) {
  const [input, setInput] = useState('');
  const [statuses, setStatuses] = useState<WordStatus[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const queryClient = useQueryClient();

  const parseWords = (text: string): string[] =>
    text.split('\n').map(w => w.trim()).filter(w => w.length > 0);

  const updateStatus = (index: number, update: Partial<WordStatus>) =>
    setStatuses(prev => prev.map((s, i) => i === index ? { ...s, ...update } : s));

  const processWords = async () => {
    const words = parseWords(input);
    if (words.length === 0) return;

    setIsProcessing(true);
    setStatuses(words.map(w => ({ word: w, status: 'pending' })));

    for (let i = 0; i < words.length; i++) {
      const word = words[i];
      updateStatus(i, { status: 'adding' });

      try {
        let lemmaId: number;
        let lemmaText: string;

        try {
          const createRes = await api.post<LemmaDetailDTO>('/vocabulary', {
            wordForm: word,
            translation: '',
          });
          lemmaId = createRes.data.id;
          lemmaText = createRes.data.text;
        } catch (createErr: unknown) {
          const status = (createErr as { response?: { status?: number } })?.response?.status;
          if (status === 409) {
            // Word already exists — find it by search
            const searchRes = await api.get<LemmaDetailDTO[]>(
              `/vocabulary/search?q=${encodeURIComponent(word)}`
            );
            if (searchRes.data.length === 0) {
              updateStatus(i, { status: 'failed', error: 'Word not found after duplicate check' });
              continue;
            }
            lemmaId = searchRes.data[0].id;
            lemmaText = searchRes.data[0].text;
            updateStatus(i, { status: 'duplicate_found', lemmaText });
          } else {
            throw createErr;
          }
        }

        // Add to list (idempotent — ON CONFLICT DO NOTHING)
        await api.post(`/lists/${listId}/members`, { lemmaId });
        if (statuses[i]?.status !== 'duplicate_found') {
          updateStatus(i, { status: 'added', lemmaText });
        }

      } catch {
        updateStatus(i, { status: 'failed', error: 'Failed to add — please try again' });
      }
    }

    setIsProcessing(false);
    queryClient.invalidateQueries({ queryKey: ['lists', listId] });
    queryClient.invalidateQueries({ queryKey: ['lists'] });
  };

  const statusIcon = (status: WordStatus['status']) => {
    switch (status) {
      case 'pending': return '○';
      case 'adding': return '…';
      case 'added': return '✓';
      case 'duplicate_found': return '✓';
      case 'failed': return '✗';
    }
  };

  const statusColor = (status: WordStatus['status']) => {
    switch (status) {
      case 'added': return 'text-green-600';
      case 'duplicate_found': return 'text-blue-500';
      case 'failed': return 'text-red-500';
      case 'adding': return 'text-orange-500';
      default: return 'text-gray-400';
    }
  };

  const statusLabel = (s: WordStatus) => {
    switch (s.status) {
      case 'adding': return 'Adding…';
      case 'added': return s.lemmaText && s.lemmaText !== s.word ? `Added as "${s.lemmaText}"` : 'Added';
      case 'duplicate_found': return s.lemmaText ? `Already in vocabulary as "${s.lemmaText}" — added to list` : 'Added (existing)';
      case 'failed': return s.error ?? 'Failed';
      default: return '';
    }
  };

  const wordCount = parseWords(input).length;
  const hasResults = statuses.length > 0;
  const allDone = hasResults && !isProcessing;

  return (
    <Modal isOpen onClose={onClose} title="Add Vocabulary to List" size="md">
      <div className="space-y-4">
        {!hasResults && (
          <>
            <p className="text-sm text-gray-500">
              Enter Bulgarian word forms, one per line. You can enter inflected forms
              (e.g. чете) or lemmas (чета) — the app will detect the canonical form.
            </p>
            <textarea
              value={input}
              onChange={e => setInput(e.target.value)}
              placeholder={'чете\nказвам\nказвам се\nобичам'}
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
                  <p className="font-medium text-gray-800"
                     style={{ fontFamily: 'Sofia Sans, sans-serif' }}>
                    {s.word}
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
