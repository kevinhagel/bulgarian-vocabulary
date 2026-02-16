/**
 * API-related types (re-export from vocabulary types for now).
 * This file provides a cleaner import path for API DTOs.
 */
export type {
  Source,
  PartOfSpeech,
  DifficultyLevel,
  ReviewStatus,
  InflectionDTO,
  LemmaResponseDTO,
  LemmaDetailDTO,
  CreateLemmaRequest,
  InflectionUpdate,
  UpdateLemmaRequest,
  PaginatedResponse,
} from '@/features/vocabulary/types';
