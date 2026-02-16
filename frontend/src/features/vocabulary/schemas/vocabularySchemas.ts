import { z } from 'zod';

/**
 * Validation schema for creating new vocabulary entries.
 * User provides an inflected word form (not necessarily the canonical lemma).
 */
export const createVocabularySchema = z.object({
  wordForm: z.string().min(1, 'Word form is required').max(100, 'Word form too long'),
  translation: z.string().min(1, 'Translation is required').max(200, 'Translation too long'),
  notes: z.string().max(5000, 'Notes too long').optional().or(z.literal('')),
});

export type CreateVocabularyFormData = z.infer<typeof createVocabularySchema>;

/**
 * Validation schema for inflection updates.
 */
const inflectionUpdateSchema = z.object({
  id: z.number().optional(),
  form: z.string().min(1, 'Form is required').max(100),
  grammaticalInfo: z.string().max(100).optional().or(z.literal('')),
});

/**
 * Validation schema for updating existing vocabulary entries.
 * Works with canonical lemma text and allows editing inflections.
 */
export const updateVocabularySchema = z.object({
  text: z.string().min(1, 'Lemma text is required').max(100, 'Text too long'),
  translation: z.string().min(1, 'Translation is required').max(200, 'Translation too long'),
  notes: z.string().max(5000, 'Notes too long').optional().or(z.literal('')),
  inflections: z.array(inflectionUpdateSchema).optional(),
});

export type UpdateVocabularyFormData = z.infer<typeof updateVocabularySchema>;
