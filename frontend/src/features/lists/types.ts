import type { LemmaResponseDTO } from '@/features/vocabulary/types';

export interface WordListSummaryDTO {
  id: number;
  name: string;
  lemmaCount: number;
  createdAt: string;
}

export interface WordListDetailDTO {
  id: number;
  name: string;
  lemmas: LemmaResponseDTO[];
  createdAt: string;
}
