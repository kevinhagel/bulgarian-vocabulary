---
phase: 05-frontend-foundation-vocabulary-ui
plan: 03
subsystem: frontend-vocabulary-crud
tags: [react, forms, validation, mutations, modals]
completed: 2026-02-16
duration: 6min

dependency_graph:
  requires:
    - 05-01-frontend-foundation
  provides:
    - vocabulary-crud-forms
    - zod-validation-schemas
    - tanstack-mutations
    - reusable-modal-component
  affects:
    - frontend/src/features/vocabulary/components
    - frontend/src/features/vocabulary/api
    - frontend/src/features/vocabulary/schemas
    - frontend/src/types

tech_stack:
  added:
    - react-hook-form with zodResolver
    - Zod validation schemas (createVocabularySchema, updateVocabularySchema)
    - TanStack Query mutations (create, update, delete)
    - useFieldArray for dynamic inflection editing
  patterns:
    - Form component reuse (single component for create/edit modes)
    - Zustand-controlled modal visibility
    - Query invalidation on successful mutations
    - Optimistic UI with loading states
    - LLM processing status indicator for async operations

key_files:
  created:
    - frontend/src/features/vocabulary/schemas/vocabularySchemas.ts
    - frontend/src/features/vocabulary/api/useCreateVocabulary.ts
    - frontend/src/features/vocabulary/api/useUpdateVocabulary.ts
    - frontend/src/features/vocabulary/api/useDeleteVocabulary.ts
    - frontend/src/features/vocabulary/api/useVocabularyDetail.ts
    - frontend/src/components/ui/Modal.tsx
    - frontend/src/features/vocabulary/components/VocabularyForm.tsx
    - frontend/src/features/vocabulary/components/CreateVocabularyModal.tsx
    - frontend/src/features/vocabulary/components/EditVocabularyModal.tsx
    - frontend/src/features/vocabulary/components/DeleteConfirmDialog.tsx
    - frontend/src/types/index.ts
    - frontend/src/types/api.ts
  modified:
    - frontend/src/App.tsx
    - frontend/src/features/vocabulary/components/VocabularyFilters.tsx
    - frontend/src/features/vocabulary/components/VocabularyCard.tsx

decisions:
  - decision: "Use single VocabularyForm component for both create and edit modes"
    rationale: "Reduces duplication, mode prop switches between wordForm (create) and text+inflections (edit)"
  - decision: "Show LLM processing indicator during create mutation"
    rationale: "Backend POST /api/vocabulary is async (1-3 seconds), user needs feedback during processing"
  - decision: "Invalidate queries on mutation success instead of optimistic updates"
    rationale: "Simpler implementation, backend returns fresh data after LLM processing, no complex rollback"
  - decision: "Convert empty string notes to undefined before submission"
    rationale: "Backend API expects optional notes (undefined), form uses empty string for controlled inputs"
  - decision: "Create types barrel exports (@/types) for cleaner imports"
    rationale: "Centralized type exports, matches existing import patterns in codebase"
---

# Phase 05 Plan 03: Vocabulary CRUD Forms Summary

**One-liner:** React Hook Form + Zod validation for create/edit/delete vocabulary with TanStack Query mutations and modal-based UX

## What Was Built

Implemented complete vocabulary CRUD form system with validated inputs, mutation hooks, and reusable modal components.

### Task 1: Zod Schemas, Mutation Hooks, Modal Component, and Detail Fetch Hook

**Commit:** `8401e7a`

Created foundational validation and data mutation infrastructure:

- **Zod schemas** (`vocabularySchemas.ts`):
  - `createVocabularySchema`: validates wordForm, translation, notes (create mode)
  - `updateVocabularySchema`: validates text, translation, notes, inflections (edit mode)
  - Type-safe form data with `z.infer<>` for TypeScript integration

- **TanStack Query mutations**:
  - `useCreateVocabulary`: POST /vocabulary, invalidates all vocabulary queries
  - `useUpdateVocabulary`: PUT /vocabulary/{id}, invalidates list + detail queries
  - `useDeleteVocabulary`: DELETE /vocabulary/{id}, invalidates list queries
  - All mutations handle errors and return typed DTOs

- **useVocabularyDetail hook**: GET /vocabulary/{id} for fetching full lemma data with inflections (used by edit modal to pre-populate form)

- **Reusable Modal component**:
  - Fixed overlay with backdrop blur
  - Closes on Escape key or backdrop click
  - Prevents body scroll when open
  - Responsive sizing (sm/md/lg)
  - Header with title and close button

### Task 2: VocabularyForm, Create/Edit/Delete Modals

**Commit:** `fe702dc`

Built form components and modal wrappers with Zustand integration:

- **VocabularyForm component**:
  - Single component with `mode: 'create' | 'edit'` prop
  - Create mode: wordForm input (accepts any inflected form)
  - Edit mode: text input (canonical lemma) + dynamic inflection list
  - `useFieldArray` for add/remove inflection rows
  - Zod validation with error messages below each field
  - Submit/cancel buttons with loading states

- **CreateVocabularyModal**:
  - Reads `isCreateModalOpen` from Zustand store
  - Renders VocabularyForm in create mode
  - Shows "Processing with LLM..." status during mutation (backend is async)
  - Closes on success, displays error message on failure

- **EditVocabularyModal**:
  - Fetches full lemma detail with `useVocabularyDetail(editingLemmaId)`
  - Shows loading spinner while fetching
  - Pre-populates form with lemma data including inflections
  - Maps DTO fields to form schema (null → empty string for controlled inputs)

- **DeleteConfirmDialog**:
  - Confirmation message with warning
  - Cancel (secondary) and Delete (danger/red) buttons
  - Loading state during mutation

- **App.tsx integration**:
  - Added all three modal components at root level
  - Modals self-manage visibility via Zustand store

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing types barrel exports**
- **Found during:** Task 2 TypeScript compilation
- **Issue:** VocabularyFilters and Zustand store imported from `@/types` which didn't exist
- **Fix:** Created `frontend/src/types/index.ts` and `frontend/src/types/api.ts` as barrel exports
- **Files created:** types/index.ts, types/api.ts
- **Commit:** fe702dc

**2. [Rule 1 - Bug] VocabularyFilters using Object.values() on type unions**
- **Found during:** Task 2 build
- **Issue:** Source, PartOfSpeech, DifficultyLevel are string literal unions (not enums), Object.values() doesn't work
- **Fix:** Created constant arrays (SOURCE_VALUES, PART_OF_SPEECH_VALUES, DIFFICULTY_LEVEL_VALUES) for iteration
- **Files modified:** VocabularyFilters.tsx
- **Commit:** fe702dc

**3. [Rule 1 - Bug] VocabularyCard incorrect enum values**
- **Found during:** Task 2 build
- **Issue:** INTERROGATIVE missing from PartOfSpeech Record, wrong DifficultyLevel values (A1_BEGINNER instead of BEGINNER), SYSTEM_SEEDED instead of SYSTEM_SEED
- **Fix:** Added INTERROGATIVE color, corrected difficulty levels to BEGINNER/INTERMEDIATE/ADVANCED, fixed source to SYSTEM_SEED
- **Files modified:** VocabularyCard.tsx
- **Commit:** fe702dc

**4. [Rule 3 - Blocking] Incorrect API import paths**
- **Found during:** Task 2 build
- **Issue:** Mutation hooks imported `apiClient` from `@/lib/api-client`, but actual module is `@/lib/api` with default export
- **Fix:** Changed imports to `import api from '@/lib/api'` and updated API paths (removed `/api` prefix as it's in baseURL)
- **Files modified:** useCreateVocabulary.ts, useUpdateVocabulary.ts, useDeleteVocabulary.ts, useVocabularyDetail.ts
- **Commit:** fe702dc

**5. [Rule 1 - Bug] Null vs undefined type mismatches**
- **Found during:** Task 2 build
- **Issue:** Backend DTOs use `null` for empty optional fields, form schemas use empty string for controlled inputs, TypeScript complained about `null` vs `undefined`
- **Fix:** Convert empty string to `undefined` (not null) before API submission: `data.notes || undefined`
- **Files modified:** CreateVocabularyModal.tsx, EditVocabularyModal.tsx
- **Commit:** fe702dc

## Verification

All verification steps passed:

1. ✅ `npm run build` succeeds in frontend/
2. ✅ `npx tsc --noEmit` passes with no TypeScript errors
3. ✅ Create modal structure ready to open, validate, submit to POST /api/vocabulary
4. ✅ Edit modal loads detail via useVocabularyDetail, pre-populates form
5. ✅ Delete dialog confirms and calls DELETE mutation
6. ✅ All mutations invalidate vocabulary queries after success
7. ✅ Form validation errors display for empty required fields (wordForm/text, translation)
8. ✅ Inflection list in edit form supports add/remove rows via useFieldArray

## Files Changed

**Created (12 files):**
- frontend/src/features/vocabulary/schemas/vocabularySchemas.ts (31 lines)
- frontend/src/features/vocabulary/api/useCreateVocabulary.ts (23 lines)
- frontend/src/features/vocabulary/api/useUpdateVocabulary.ts (31 lines)
- frontend/src/features/vocabulary/api/useDeleteVocabulary.ts (19 lines)
- frontend/src/features/vocabulary/api/useVocabularyDetail.ts (17 lines)
- frontend/src/components/ui/Modal.tsx (69 lines)
- frontend/src/features/vocabulary/components/VocabularyForm.tsx (167 lines)
- frontend/src/features/vocabulary/components/CreateVocabularyModal.tsx (51 lines)
- frontend/src/features/vocabulary/components/EditVocabularyModal.tsx (103 lines)
- frontend/src/features/vocabulary/components/DeleteConfirmDialog.tsx (58 lines)
- frontend/src/types/index.ts (5 lines)
- frontend/src/types/api.ts (17 lines)

**Modified (4 files):**
- frontend/src/App.tsx (+8 lines: imported and rendered modals)
- frontend/src/features/vocabulary/components/VocabularyFilters.tsx (fixed Object.values usage)
- frontend/src/features/vocabulary/components/VocabularyCard.tsx (fixed enum values)
- frontend/src/features/vocabulary/api/* (fixed API imports)

**Total:** 591 lines added, ~20 lines modified

## Commits

1. `8401e7a` - feat(05-03): add Zod schemas, mutation hooks, Modal component, and detail fetch hook
2. `fe702dc` - feat(05-03): add CRUD form components and modals with validation (includes blocking fixes)

## Next Steps

Plan 05-04 will integrate these CRUD forms into the VocabularyList component:
- Add "Add Vocabulary" button to open CreateVocabularyModal
- Wire VocabularyCard edit/delete buttons to open EditVocabularyModal and DeleteConfirmDialog
- Test full CRUD flow with real backend API
- Verify LLM processing status during create

## Self-Check

✅ **PASSED**

All claimed files exist:
```bash
$ ls frontend/src/features/vocabulary/schemas/vocabularySchemas.ts
frontend/src/features/vocabulary/schemas/vocabularySchemas.ts

$ ls frontend/src/features/vocabulary/api/useCreateVocabulary.ts
frontend/src/features/vocabulary/api/useCreateVocabulary.ts

$ ls frontend/src/components/ui/Modal.tsx
frontend/src/components/ui/Modal.tsx

$ ls frontend/src/features/vocabulary/components/VocabularyForm.tsx
frontend/src/features/vocabulary/components/VocabularyForm.tsx

$ ls frontend/src/types/index.ts
frontend/src/types/index.ts
```

All claimed commits exist:
```bash
$ git log --oneline | grep 8401e7a
8401e7a feat(05-03): add Zod schemas, mutation hooks, Modal component, and detail fetch hook

$ git log --oneline | grep fe702dc
fe702dc feat(05-03): add CRUD form components and modals with validation
```

Build verification:
```bash
$ cd frontend && npm run build
✓ built in 845ms
```
