import { create } from 'zustand';
import type { StudyPhase, StudyCardDTO, SessionSummaryDTO } from '@/features/study/types';

interface StudyState {
  phase: StudyPhase;
  sessionId: number | null;
  cardCount: number;
  currentCard: StudyCardDTO | null;
  isRevealed: boolean;
  summary: SessionSummaryDTO | null;

  startSession: (sessionId: number, cardCount: number, firstCard: StudyCardDTO) => void;
  setCurrentCard: (card: StudyCardDTO | null) => void;
  setRevealed: (revealed: boolean) => void;
  showSummary: (summary: SessionSummaryDTO) => void;
  reset: () => void;
}

export const useStudyStore = create<StudyState>((set) => ({
  phase: 'idle',
  sessionId: null,
  cardCount: 0,
  currentCard: null,
  isRevealed: false,
  summary: null,

  startSession: (sessionId, cardCount, firstCard) =>
    set({ phase: 'active', sessionId, cardCount, currentCard: firstCard, isRevealed: false, summary: null }),

  setCurrentCard: (card) =>
    set({ currentCard: card, isRevealed: false }),

  setRevealed: (revealed) =>
    set({ isRevealed: revealed }),

  showSummary: (summary) =>
    set({ phase: 'summary', summary }),

  reset: () =>
    set({ phase: 'idle', sessionId: null, cardCount: 0, currentCard: null, isRevealed: false, summary: null }),
}));
