// TypeScript types mirroring backend DTOs

// Enum types as string union types
export type Source = 'USER_ENTERED' | 'SYSTEM_SEED';

export type PartOfSpeech = 
  | 'NOUN'
  | 'VERB'
  | 'ADJECTIVE'
  | 'ADVERB'
  | 'PRONOUN'
  | 'PREPOSITION'
  | 'CONJUNCTION'
  | 'NUMERAL'
  | 'INTERJECTION'
  | 'PARTICLE'
  | 'INTERROGATIVE';

export type DifficultyLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

export type InflectionDifficulty = 'BASIC' | 'INTERMEDIATE' | 'ADVANCED';

export type ReviewStatus = 'PENDING' | 'REVIEWED' | 'NEEDS_CORRECTION';

// DTOs
export interface InflectionDTO {
  id: number;
  form: string;
  grammaticalInfo: string | null;
  difficultyLevel: InflectionDifficulty | null;
}

export interface LemmaResponseDTO {
  id: number;
  text: string;
  translation: string;
  partOfSpeech: PartOfSpeech | null;
  category: string | null;
  difficultyLevel: DifficultyLevel | null;
  source: Source;
  reviewStatus: ReviewStatus;
  inflectionCount: number;
  createdAt: string;
}

export interface LemmaDetailDTO {
  id: number;
  text: string;
  translation: string;
  notes: string | null;
  partOfSpeech: PartOfSpeech | null;
  category: string | null;
  difficultyLevel: DifficultyLevel | null;
  source: Source;
  reviewStatus: ReviewStatus;
  inflections: InflectionDTO[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateLemmaRequest {
  wordForm: string;
  translation: string;
  notes?: string;
}

export interface InflectionUpdate {
  id?: number;
  form: string;
  grammaticalInfo?: string;
}

export interface UpdateLemmaRequest {
  text: string;
  translation: string;
  notes?: string;
  inflections?: InflectionUpdate[];
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
