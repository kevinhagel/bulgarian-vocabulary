export type StudyPhase = 'idle' | 'active' | 'summary';

export interface StudyCardDTO {
  lemmaId: number;
  lemmaText: string;
  translation: string;
  cardsRemaining: number;
}

export interface StartSessionResponseDTO {
  sessionId: number;
  cardCount: number;
  firstCard: StudyCardDTO;
}

export interface RateCardRequestDTO {
  lemmaId: number;
  rating: 'CORRECT' | 'INCORRECT';
}

export interface SessionSummaryDTO {
  sessionId: number;
  status: string;
  cardCount: number;
  cardsReviewed: number;
  correctCount: number;
  retentionRate: number;
}

export interface DueCountDTO {
  dueToday: number;
  newCards: number;
  pendingReview: number;
}

export interface ProgressDashboardDTO {
  totalUserVocab: number;
  totalVocabStudied: number;
  totalSessions: number;
  totalCardsReviewed: number;
  totalCorrect: number;
  retentionRate: number;
  cardsDueToday: number;
  newCards: number;
}

export interface LemmaStatsDTO {
  lemmaId: number;
  reviewCount: number;
  correctCount: number;
  correctRate: number;
  lastReviewedAt: string | null;
  nextReviewDate: string | null;
  intervalDays: number;
  easeFactor: number;
}
