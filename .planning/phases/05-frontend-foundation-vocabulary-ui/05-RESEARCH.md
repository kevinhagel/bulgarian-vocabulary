# Phase 5: Frontend Foundation & Vocabulary UI - Research

**Researched:** 2026-02-16
**Domain:** React 19 frontend with TypeScript, Vite 6, TanStack Query, Zustand, Tailwind CSS
**Confidence:** HIGH

## Summary

Phase 5 establishes the React 19 frontend foundation for the Bulgarian Vocabulary Tutor, implementing a modern, performant, and maintainable web application with proper separation of server state (TanStack Query) and UI state (Zustand). The recommended stack leverages React 19's new Actions and form handling features, Vite 6's fast build system, TanStack Query v5 for server state with automatic caching and background updates, and Zustand for lightweight UI state management. Critical considerations include: Bulgarian Cyrillic font rendering (requires lang="bg" attribute and appropriate fonts), audio playback via native HTML5 audio elements, mobile-responsive design using Tailwind CSS utility classes, and TypeScript for type safety throughout.

The phase delivers a complete vocabulary management interface (list view, detail view, create/edit forms, delete confirmations, search/filter) with audio playback for lemmas and inflections. React 19's new form features (useActionState, useFormStatus, useOptimistic) combined with React Hook Form + Zod provide an excellent developer experience with runtime validation and compile-time type safety. Vite 6 provides sub-second hot module replacement during development and optimized production builds.

**Primary recommendation:** Use React 19 + Vite 6 + TypeScript with TanStack Query v5 for server state, Zustand for UI state, React Hook Form + Zod for forms, and Tailwind CSS 4 for styling. Structure the project by features (vocabulary/, audio/, search/) with shared components/ and hooks/ directories. Deploy frontend as static assets with Vite production build, connecting to Spring Boot backend REST API.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19.x | UI framework | Latest stable with Actions, form enhancements, ref as prop; no forwardRef needed |
| Vite | 6.x | Build tool & dev server | Fast HMR (sub-second), optimized builds, Node.js 18/20/22 support, experimental Environment API |
| TypeScript | 5.x | Type safety | Compile-time safety, excellent IDE support, reduces runtime errors |
| TanStack Query | 5.x | Server state management | Industry standard for data fetching/caching; handles 80% of app state in modern React apps |
| Zustand | 4.x | UI state management | Lightweight (minimal boilerplate), hook-based API, no Context Provider needed |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| React Hook Form | 7.x | Form state management | All forms with validation (recommended for all but simplest forms) |
| Zod | 3.x | Schema validation | Type-safe runtime validation with React Hook Form integration |
| @hookform/resolvers | 3.x | Validation bridge | Connects React Hook Form to Zod schemas |
| Tailwind CSS | 4.x | Utility-first CSS | Rapid UI development, mobile-responsive design, customizable design tokens |
| @tanstack/react-query-devtools | 5.x | Query debugging | Development-only; inspect cache, queries, mutations |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| TanStack Query | Redux Toolkit Query | RTK Query is tied to Redux; TanStack Query is more flexible, framework-agnostic |
| Zustand | Redux Toolkit | Redux has more boilerplate; Zustand is simpler for UI state (modals, form inputs) |
| React Hook Form | Formik | Formik has more re-renders; React Hook Form uses uncontrolled components (better performance) |
| Tailwind CSS | CSS Modules / Styled Components | Tailwind is faster for prototyping; CSS-in-JS has runtime cost |
| Vite | Webpack + CRA | Vite is faster (Rust-based bundler); Webpack has more plugins but slower dev experience |

**Installation:**

```bash
# From project root
mkdir frontend
cd frontend

# Create Vite + React + TypeScript project
npm create vite@latest . -- --template react-ts

# Install dependencies
npm install @tanstack/react-query@5 zustand@4
npm install react-hook-form@7 zod@3 @hookform/resolvers@3
npm install -D @tanstack/react-query-devtools@5

# Tailwind CSS 4 (CSS-first configuration)
npm install -D tailwindcss@next @tailwindcss/vite@next
```

## Architecture Patterns

### Recommended Project Structure

```
frontend/
├── public/                          # Static assets
├── src/
│   ├── assets/                      # Images, fonts (if not using CDN)
│   ├── components/                  # Reusable UI components
│   │   ├── ui/                      # Generic UI (Button, Modal, Input)
│   │   │   ├── Button.tsx
│   │   │   ├── Modal.tsx
│   │   │   └── Input.tsx
│   │   ├── layout/                  # Layout components (Header, Footer, Sidebar)
│   │   └── audio/                   # Audio playback components
│   │       └── AudioPlayer.tsx
│   ├── features/                    # Feature-based modules (domain-driven)
│   │   ├── vocabulary/
│   │   │   ├── api/                 # TanStack Query hooks
│   │   │   │   ├── useVocabulary.ts
│   │   │   │   ├── useCreateVocabulary.ts
│   │   │   │   └── useDeleteVocabulary.ts
│   │   │   ├── components/          # Feature-specific components
│   │   │   │   ├── VocabularyList.tsx
│   │   │   │   ├── VocabularyDetail.tsx
│   │   │   │   ├── VocabularyForm.tsx
│   │   │   │   └── VocabularyFilters.tsx
│   │   │   ├── stores/              # Zustand stores (UI state)
│   │   │   │   └── useVocabularyUIStore.ts
│   │   │   ├── schemas/             # Zod validation schemas
│   │   │   │   └── vocabularySchemas.ts
│   │   │   └── types.ts             # TypeScript types/interfaces
│   │   └── search/
│   │       ├── api/
│   │       └── components/
│   ├── hooks/                       # Shared custom hooks
│   │   └── useDebounce.ts
│   ├── lib/                         # Third-party library configurations
│   │   ├── queryClient.ts           # TanStack Query setup
│   │   └── axios.ts                 # Axios instance with base URL
│   ├── utils/                       # Utility functions
│   │   └── formatters.ts
│   ├── App.tsx                      # Root component
│   ├── main.tsx                     # Entry point
│   ├── index.css                    # Tailwind directives + global styles
│   └── vite-env.d.ts                # Vite type definitions
├── index.html                       # HTML entry (Vite serves this)
├── vite.config.ts                   # Vite configuration
├── tsconfig.json                    # TypeScript configuration
├── tsconfig.node.json               # TypeScript config for Vite config files
├── tailwind.config.js               # Tailwind configuration (if needed)
├── package.json
└── .env                             # Environment variables (not committed)
```

**Key principles:**
- **Feature-based organization**: Group by domain (vocabulary/, search/) not by type (components/, hooks/)
- **Colocation**: Keep feature-specific code together (API hooks, components, stores, schemas in same feature/)
- **Shared vs. Feature**: Shared components in `components/`, feature-specific in `features/{feature}/components/`
- **Path aliases**: Configure `@/` alias for `src/` in `vite.config.ts` and `tsconfig.json`

### Pattern 1: TanStack Query for Server State

**What:** Manage all server data (vocabulary CRUD, search results) with TanStack Query hooks
**When to use:** All backend API interactions

**Example:**

```typescript
// features/vocabulary/api/useVocabulary.ts
import { useQuery } from '@tanstack/react-query';
import axios from '@/lib/axios';
import type { LemmaResponseDTO } from '@/features/vocabulary/types';

export function useVocabulary(page = 0, size = 20) {
  return useQuery({
    queryKey: ['vocabulary', { page, size }],
    queryFn: async () => {
      const { data } = await axios.get<{content: LemmaResponseDTO[]}>('/api/vocabulary', {
        params: { page, size }
      });
      return data;
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

// features/vocabulary/api/useCreateVocabulary.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from '@/lib/axios';
import type { CreateLemmaRequestDTO, LemmaDetailDTO } from '@/features/vocabulary/types';

export function useCreateVocabulary() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: CreateLemmaRequestDTO) => {
      const { data } = await axios.post<LemmaDetailDTO>('/api/vocabulary', request);
      return data;
    },
    onSuccess: () => {
      // Invalidate vocabulary list to refetch
      queryClient.invalidateQueries({ queryKey: ['vocabulary'] });
    },
  });
}

// Usage in component
function VocabularyList() {
  const { data, isLoading, error } = useVocabulary(0, 20);

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;

  return (
    <ul>
      {data.content.map(lemma => (
        <li key={lemma.id}>{lemma.text} - {lemma.translation}</li>
      ))}
    </ul>
  );
}
```

**Key points:**
- Query keys as arrays: `['vocabulary', { page, size }]` for automatic cache invalidation
- `staleTime` prevents excessive refetches (default 0 = always stale)
- `invalidateQueries` after mutations to refetch affected data
- TypeScript types from backend DTOs

### Pattern 2: Zustand for UI State

**What:** Manage UI-only state (modal visibility, form inputs, filter selections) with Zustand stores
**When to use:** State that doesn't need to persist to backend (UI ephemeral state)

**Example:**

```typescript
// features/vocabulary/stores/useVocabularyUIStore.ts
import { create } from 'zustand';

interface VocabularyUIState {
  // Modal state
  isCreateModalOpen: boolean;
  isEditModalOpen: boolean;
  editingLemmaId: number | null;

  // Filter state
  selectedSource: string | null;
  selectedPartOfSpeech: string | null;
  searchQuery: string;

  // Actions
  openCreateModal: () => void;
  closeCreateModal: () => void;
  openEditModal: (id: number) => void;
  closeEditModal: () => void;
  setFilter: (filter: Partial<Pick<VocabularyUIState, 'selectedSource' | 'selectedPartOfSpeech'>>) => void;
  setSearchQuery: (query: string) => void;
  resetFilters: () => void;
}

export const useVocabularyUIStore = create<VocabularyUIState>((set) => ({
  // Initial state
  isCreateModalOpen: false,
  isEditModalOpen: false,
  editingLemmaId: null,
  selectedSource: null,
  selectedPartOfSpeech: null,
  searchQuery: '',

  // Actions
  openCreateModal: () => set({ isCreateModalOpen: true }),
  closeCreateModal: () => set({ isCreateModalOpen: false }),
  openEditModal: (id) => set({ isEditModalOpen: true, editingLemmaId: id }),
  closeEditModal: () => set({ isEditModalOpen: false, editingLemmaId: null }),
  setFilter: (filter) => set(filter),
  setSearchQuery: (query) => set({ searchQuery: query }),
  resetFilters: () => set({
    selectedSource: null,
    selectedPartOfSpeech: null,
    searchQuery: ''
  }),
}));

// Usage in component with selectors (prevents unnecessary re-renders)
function VocabularyFilters() {
  const searchQuery = useVocabularyUIStore(state => state.searchQuery);
  const setSearchQuery = useVocabularyUIStore(state => state.setSearchQuery);

  return (
    <input
      value={searchQuery}
      onChange={(e) => setSearchQuery(e.target.value)}
    />
  );
}
```

**Key points:**
- ALWAYS use selectors: `useStore(state => state.field)` prevents rerenders
- Multiple stores encouraged: one per feature (not global store like Redux)
- Immutable updates via `set` function
- No Provider needed (unlike Context API)

### Pattern 3: React Hook Form + Zod for Forms

**What:** Type-safe form validation with runtime checks and compile-time types
**When to use:** All forms with validation (create/edit vocabulary)

**Example:**

```typescript
// features/vocabulary/schemas/vocabularySchemas.ts
import { z } from 'zod';

export const createVocabularySchema = z.object({
  wordForm: z.string()
    .min(1, 'Word form is required')
    .max(100, 'Word form too long'),
  translation: z.string()
    .min(1, 'Translation is required')
    .max(200, 'Translation too long'),
  notes: z.string().max(5000, 'Notes too long').optional(),
});

export type CreateVocabularyFormData = z.infer<typeof createVocabularySchema>;

// features/vocabulary/components/VocabularyForm.tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { createVocabularySchema, type CreateVocabularyFormData } from '../schemas/vocabularySchemas';
import { useCreateVocabulary } from '../api/useCreateVocabulary';

export function VocabularyForm({ onSuccess }: { onSuccess: () => void }) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<CreateVocabularyFormData>({
    resolver: zodResolver(createVocabularySchema),
  });

  const createMutation = useCreateVocabulary();

  const onSubmit = async (data: CreateVocabularyFormData) => {
    await createMutation.mutateAsync(data);
    onSuccess();
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <div>
        <label htmlFor="wordForm">Bulgarian Word</label>
        <input
          id="wordForm"
          {...register('wordForm')}
          className={errors.wordForm ? 'border-red-500' : ''}
        />
        {errors.wordForm && <p className="text-red-500">{errors.wordForm.message}</p>}
      </div>

      <div>
        <label htmlFor="translation">English Translation</label>
        <input
          id="translation"
          {...register('translation')}
          className={errors.translation ? 'border-red-500' : ''}
        />
        {errors.translation && <p className="text-red-500">{errors.translation.message}</p>}
      </div>

      <div>
        <label htmlFor="notes">Notes (optional)</label>
        <textarea
          id="notes"
          {...register('notes')}
        />
        {errors.notes && <p className="text-red-500">{errors.notes.message}</p>}
      </div>

      <button type="submit" disabled={isSubmitting || createMutation.isPending}>
        {isSubmitting || createMutation.isPending ? 'Creating...' : 'Create Vocabulary'}
      </button>
    </form>
  );
}
```

**Key points:**
- Zod schema defines validation rules AND TypeScript types (via `z.infer`)
- `zodResolver` connects React Hook Form to Zod
- `register()` connects inputs to form state (uncontrolled components)
- `formState.errors` provides field-level error messages
- `handleSubmit` validates before calling `onSubmit`

### Pattern 4: HTML5 Audio with React Refs

**What:** Native HTML5 `<audio>` element controlled via React refs for playback
**When to use:** Audio playback for lemmas and inflections

**Example:**

```typescript
// components/audio/AudioPlayer.tsx
import { useRef, useState } from 'react';

interface AudioPlayerProps {
  audioUrl: string; // e.g., /api/audio/{hash}.mp3
  label?: string;   // e.g., "пиша" or "Play audio"
}

export function AudioPlayer({ audioUrl, label }: AudioPlayerProps) {
  const audioRef = useRef<HTMLAudioElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);

  const handlePlayPause = () => {
    if (!audioRef.current) return;

    if (isPlaying) {
      audioRef.current.pause();
    } else {
      audioRef.current.play();
    }
  };

  const handleEnded = () => {
    setIsPlaying(false);
  };

  const handlePlay = () => {
    setIsPlaying(true);
  };

  const handlePause = () => {
    setIsPlaying(false);
  };

  return (
    <div className="inline-flex items-center gap-2">
      <button
        onClick={handlePlayPause}
        className="p-2 rounded hover:bg-gray-100"
        aria-label={label ? `Play ${label}` : 'Play audio'}
      >
        {isPlaying ? '⏸️' : '▶️'}
      </button>
      {label && <span className="text-lg" lang="bg">{label}</span>}
      <audio
        ref={audioRef}
        src={audioUrl}
        onEnded={handleEnded}
        onPlay={handlePlay}
        onPause={handlePause}
        preload="metadata"
      />
    </div>
  );
}

// Usage
<AudioPlayer
  audioUrl="/api/audio/a1b2c3...xyz.mp3"
  label="пиша"
/>
```

**Key points:**
- `useRef<HTMLAudioElement>` for direct DOM access
- Event listeners: `onEnded`, `onPlay`, `onPause` for state sync
- `preload="metadata"` loads duration without downloading full audio
- `lang="bg"` on text elements for proper Bulgarian Cyrillic rendering
- Backend provides immutable content-hash URLs (aggressive browser caching)

### Pattern 5: Bulgarian Cyrillic Font Rendering

**What:** Ensure proper Bulgarian Cyrillic typography (differs from Russian)
**When to use:** All text displaying Bulgarian lemmas and inflections

**Example:**

```typescript
// App.tsx or index.html
// Set lang attribute on root element
<html lang="bg">
  <body>
    <div id="root"></div>
  </body>
</html>

// index.css (Tailwind CSS 4 with @theme directive)
@import "tailwindcss";

/* Bulgarian Cyrillic font stack */
@theme {
  --font-family-sans: 'Sofia Sans', 'Segoe UI', system-ui, sans-serif;
}

/* Load Sofia Sans (Bulgarian Cyrillic default forms) via Google Fonts or local */
@import url('https://fonts.googleapis.com/css2?family=Sofia+Sans:wght@400;600;700&display=swap');

/* Component usage with lang attribute */
function VocabularyCard({ lemma }: { lemma: LemmaResponseDTO }) {
  return (
    <div className="p-4 border rounded">
      <h2 className="text-2xl font-semibold" lang="bg">
        {lemma.text}
      </h2>
      <p className="text-gray-600">
        {lemma.translation}
      </p>
    </div>
  );
}
```

**Key points:**
- **HTML lang attribute**: Set `lang="bg"` on root `<html>` or individual elements
- **Font selection**: Use Sofia Sans (Bulgarian Cyrillic as default) or fonts with OpenType `locl` feature
- **Font size**: Bulgarian learners need large, clear fonts (text-2xl, text-3xl for lemmas)
- **Avoid**: Fonts without proper Bulgarian Cyrillic support (displays Russian forms)

### Pattern 6: Mobile-Responsive Layout with Tailwind CSS

**What:** Mobile-first responsive design using Tailwind's breakpoint prefixes
**When to use:** All components and layouts

**Example:**

```typescript
// features/vocabulary/components/VocabularyList.tsx
function VocabularyList() {
  const { data } = useVocabulary();

  return (
    <div className="container mx-auto px-4">
      {/* Mobile: stack vertically, Desktop: grid layout */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {data?.content.map(lemma => (
          <VocabularyCard key={lemma.id} lemma={lemma} />
        ))}
      </div>
    </div>
  );
}

// components/layout/Header.tsx
function Header() {
  return (
    <header className="bg-white shadow">
      {/* Mobile: vertical stack, Desktop: horizontal flex */}
      <div className="container mx-auto px-4 py-4 flex flex-col md:flex-row md:items-center md:justify-between">
        <h1 className="text-2xl font-bold mb-2 md:mb-0">Bulgarian Vocabulary</h1>
        <nav className="flex flex-col md:flex-row gap-2 md:gap-4">
          <a href="/vocabulary" className="text-blue-600 hover:underline">Vocabulary</a>
          <a href="/flashcards" className="text-blue-600 hover:underline">Flashcards</a>
        </nav>
      </div>
    </header>
  );
}
```

**Key points:**
- **Mobile-first**: Base styles for mobile, `md:` and `lg:` for larger screens
- **Tailwind breakpoints**: `sm:` (640px), `md:` (768px), `lg:` (1024px), `xl:` (1280px)
- **Flexbox/Grid**: Use `flex flex-col md:flex-row` for responsive layouts
- **Container**: Use `container mx-auto` for centered, max-width layouts

### Anti-Patterns to Avoid

- **Storing API data in Zustand**: Use TanStack Query for server state, Zustand for UI state only
- **Not using selectors in Zustand**: Always use `useStore(state => state.field)` to prevent rerenders
- **Mutating state directly**: React state is immutable; use `setState` or `set` functions
- **useEffect for data fetching**: Use TanStack Query hooks, not useEffect + fetch
- **Prop drilling for server data**: Use TanStack Query hooks in child components directly
- **Not invalidating queries after mutations**: Always `invalidateQueries` in mutation `onSuccess`
- **Missing lang attribute for Cyrillic**: Bulgarian text needs `lang="bg"` for proper font rendering
- **Blocking the main thread**: Use React 19's `useTransition` for non-urgent updates

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Data fetching & caching | Custom fetch wrapper with useState + useEffect | TanStack Query | Handles caching, background refetch, stale-while-revalidate, retries, request deduplication |
| Form validation | Custom validation functions | React Hook Form + Zod | Handles field registration, validation timing, error state, TypeScript integration |
| Audio playback | Custom audio state management | HTML5 `<audio>` with refs | Browser-native playback controls, events, preloading |
| Modal state management | Custom modal context | Zustand store | Simpler than Context, no Provider, TypeScript-friendly |
| Debouncing search input | Manual setTimeout logic | `useDebounce` hook or `lodash.debounce` | Edge cases (cleanup, rapid changes, component unmount) |
| Date/time formatting | Custom formatters | `date-fns` or `Intl.DateTimeFormat` | Handles timezones, locales, edge cases |

**Key insight:** Modern React ecosystem provides excellent libraries for common problems. Custom solutions introduce bugs (race conditions, memory leaks, edge cases) and maintenance burden. Use established solutions unless there's a compelling reason not to.

## Common Pitfalls

### Pitfall 1: TanStack Query Request Waterfalls

**What goes wrong:** Parent component fetches data, passes ID to child, child fetches detail data → sequential requests instead of parallel

**Why it happens:** Nested queries where child depends on parent's data

**How to avoid:**
- Fetch all data at highest common ancestor
- Pass `enabled: !!dependentValue` to delay child query until parent data ready
- Use `prefetchQuery` in parent to start child query early

**Warning signs:** Network tab shows sequential requests; slow page loads

**Example:**

```typescript
// BAD: Request waterfall
function VocabularyPage() {
  const { data: list } = useVocabulary(); // Request 1
  return <VocabularyDetail id={list?.content[0].id} />; // Request 2 waits for 1
}

function VocabularyDetail({ id }: { id?: number }) {
  const { data } = useVocabularyDetail(id); // Waits for parent
  return <div>{data?.text}</div>;
}

// GOOD: Enabled query
function VocabularyDetail({ id }: { id?: number }) {
  const { data } = useQuery({
    queryKey: ['vocabulary', id],
    queryFn: () => fetchVocabulary(id!),
    enabled: !!id, // Only runs when id is available
  });
  return data ? <div>{data.text}</div> : null;
}
```

### Pitfall 2: Async setState Race Conditions

**What goes wrong:** Component updates state based on async operation result, but component unmounts before async completes → setState on unmounted component warning

**Why it happens:** Async operations (API calls) finish after component unmounts

**How to avoid:**
- Use TanStack Query mutations (handles cleanup automatically)
- If using useEffect, return cleanup function to cancel
- Use AbortController for fetch requests

**Warning signs:** Console warnings "Can't perform a React state update on an unmounted component"

**Example:**

```typescript
// BAD: Race condition
function VocabularyForm() {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (data) => {
    setIsSubmitting(true);
    await createVocabulary(data);
    setIsSubmitting(false); // Component might be unmounted!
  };
}

// GOOD: TanStack Query handles cleanup
function VocabularyForm() {
  const mutation = useCreateVocabulary();

  const handleSubmit = (data) => {
    mutation.mutate(data); // Cleanup handled automatically
  };

  return (
    <form onSubmit={handleSubmit}>
      <button disabled={mutation.isPending}>
        {mutation.isPending ? 'Creating...' : 'Create'}
      </button>
    </form>
  );
}
```

### Pitfall 3: Missing Query Invalidation After Mutations

**What goes wrong:** User creates/updates/deletes data, but UI shows stale cached data because queries weren't invalidated

**Why it happens:** Forgetting to invalidate queries in mutation `onSuccess` handler

**How to avoid:** Always invalidate related queries after mutations

**Warning signs:** User performs action, success message shows, but list doesn't update until page refresh

**Example:**

```typescript
// BAD: Stale cache
function useDeleteVocabulary() {
  return useMutation({
    mutationFn: (id: number) => axios.delete(`/api/vocabulary/${id}`),
    // ❌ Missing invalidation
  });
}

// GOOD: Invalidate queries
function useDeleteVocabulary() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => axios.delete(`/api/vocabulary/${id}`),
    onSuccess: () => {
      // ✅ Invalidate all vocabulary queries
      queryClient.invalidateQueries({ queryKey: ['vocabulary'] });
    },
  });
}
```

### Pitfall 4: Zustand Store Without Selectors

**What goes wrong:** Component subscribes to entire store, rerenders on every state change even if component doesn't use changed state

**Why it happens:** Using `useStore()` without selector subscribes to all state

**How to avoid:** ALWAYS use selectors: `useStore(state => state.field)`

**Warning signs:** Performance profiler shows unnecessary rerenders

**Example:**

```typescript
// BAD: Subscribes to all state
function SearchInput() {
  const store = useVocabularyUIStore(); // ❌ Rerenders on ANY state change
  return <input value={store.searchQuery} onChange={(e) => store.setSearchQuery(e.target.value)} />;
}

// GOOD: Selector for specific state
function SearchInput() {
  const searchQuery = useVocabularyUIStore(state => state.searchQuery); // ✅ Only rerenders when searchQuery changes
  const setSearchQuery = useVocabularyUIStore(state => state.setSearchQuery);
  return <input value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />;
}
```

### Pitfall 5: Bulgarian Cyrillic Rendering Without lang Attribute

**What goes wrong:** Bulgarian text renders with Russian Cyrillic glyphs (different letterforms) instead of Bulgarian forms

**Why it happens:** Browser doesn't know text is Bulgarian without `lang="bg"` attribute; defaults to Russian Cyrillic

**How to avoid:** Set `lang="bg"` on `<html>` element or individual text elements with Bulgarian content

**Warning signs:** Bulgarian text looks "wrong" to native speakers (Russian-style letters)

**Example:**

```typescript
// BAD: Missing lang attribute
<div className="text-2xl">{lemma.text}</div> // Renders Russian Cyrillic forms

// GOOD: lang="bg" for Bulgarian
<div className="text-2xl" lang="bg">{lemma.text}</div> // Renders Bulgarian Cyrillic forms
```

### Pitfall 6: useEffect Dependency Array Mistakes

**What goes wrong:** Infinite loop, stale closure, or missing effect execution

**Why it happens:** Missing dependencies or including objects/functions that change every render

**How to avoid:**
- Include all variables from component scope used in effect
- Use `useCallback` for functions passed as dependencies
- Use `useMemo` for objects passed as dependencies
- ESLint rule: `react-hooks/exhaustive-deps`

**Warning signs:** Infinite renders, effect doesn't run when expected, console errors from ESLint

**Example:**

```typescript
// BAD: Missing dependency
function VocabularyList({ source }) {
  const [data, setData] = useState([]);

  useEffect(() => {
    fetchVocabulary(source).then(setData);
  }, []); // ❌ Missing 'source' dependency
}

// GOOD: All dependencies
function VocabularyList({ source }) {
  const [data, setData] = useState([]);

  useEffect(() => {
    fetchVocabulary(source).then(setData);
  }, [source]); // ✅ Includes 'source'
}

// BETTER: Use TanStack Query
function VocabularyList({ source }) {
  const { data } = useVocabulary({ source }); // ✅ No useEffect needed
}
```

## Code Examples

Verified patterns from official sources:

### TanStack Query Setup

```typescript
// lib/queryClient.ts
// Source: https://tanstack.com/query/v5/docs/framework/react/overview
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      retry: 1,
      refetchOnWindowFocus: false, // Disable for better UX
    },
  },
});

// main.tsx
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { queryClient } from './lib/queryClient';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </React.StrictMode>
);
```

### Vite Configuration with Path Aliases

```typescript
// vite.config.ts
// Source: https://vitejs.dev/config/
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // Spring Boot backend
        changeOrigin: true,
      },
    },
  },
});

// tsconfig.json (add paths)
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

### Tailwind CSS 4 Setup (CSS-First Configuration)

```css
/* src/index.css */
/* Source: https://tailwindcss.com/docs/installation/framework-guides */
@import "tailwindcss";

/* Custom design tokens via @theme directive */
@theme {
  --color-primary: #3b82f6;
  --color-secondary: #8b5cf6;
  --font-family-sans: 'Sofia Sans', 'Segoe UI', system-ui, sans-serif;
}

/* Global styles */
body {
  @apply bg-gray-50 text-gray-900;
}
```

```typescript
// vite.config.ts (add Tailwind plugin)
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
});
```

### React 19 Form with useActionState

```typescript
// features/vocabulary/components/VocabularyForm.tsx
// Source: https://react.dev/blog/2024/12/05/react-19
import { useActionState } from 'react';
import { createVocabulary } from '../api/createVocabulary';

export function VocabularyForm() {
  const [error, submitAction, isPending] = useActionState(
    async (previousState, formData) => {
      const wordForm = formData.get('wordForm') as string;
      const translation = formData.get('translation') as string;
      const notes = formData.get('notes') as string;

      try {
        await createVocabulary({ wordForm, translation, notes });
        return null; // Success
      } catch (err) {
        return err instanceof Error ? err.message : 'Failed to create vocabulary';
      }
    },
    null
  );

  return (
    <form action={submitAction}>
      <input type="text" name="wordForm" required />
      <input type="text" name="translation" required />
      <textarea name="notes" />
      <button type="submit" disabled={isPending}>
        {isPending ? 'Creating...' : 'Create Vocabulary'}
      </button>
      {error && <p className="text-red-500">{error}</p>}
    </form>
  );
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Create React App (CRA) | Vite | 2023-2024 | 10-100x faster dev builds, sub-second HMR |
| Redux for all state | TanStack Query (server) + Zustand (UI) | 2022-2023 | Less boilerplate, better developer experience, automatic caching |
| Class components + lifecycle methods | Function components + hooks | 2019 (React 16.8) | Simpler code, better composition, easier testing |
| forwardRef for ref forwarding | ref as prop | React 19 (Dec 2024) | Less boilerplate, more intuitive API |
| Formik for forms | React Hook Form + Zod | 2021-2022 | Better performance (uncontrolled), type safety |
| CSS-in-JS (styled-components) | Tailwind CSS | 2020-2022 | Faster builds (no runtime), smaller bundles, rapid prototyping |
| Axios everywhere | Native fetch + TanStack Query | 2023-2024 | Less dependencies, modern APIs, automatic retries |

**Deprecated/outdated:**
- **Create React App**: No longer maintained; use Vite or Next.js
- **forwardRef**: React 19 deprecates this; use ref as prop
- **Context.Provider**: React 19 allows `<Context value={...}>` directly
- **PropTypes**: Use TypeScript instead for type checking
- **React.FC**: Explicit return types preferred (`function Comp(): JSX.Element`)

## Open Questions

1. **Bulgarian Font CDN vs. Self-Hosted**
   - What we know: Sofia Sans available via Google Fonts CDN; self-hosting possible
   - What's unclear: CDN performance vs. self-hosted for Bulgarian users; GDPR implications of Google Fonts
   - Recommendation: Start with Google Fonts CDN for Phase 5; measure loading times; switch to self-hosted if latency issues

2. **Audio Preloading Strategy**
   - What we know: HTML5 `preload="metadata"` loads duration only; `preload="auto"` downloads full audio
   - What's unclear: Best strategy for vocabulary list with 20+ audio files; memory/bandwidth tradeoff
   - Recommendation: Use `preload="metadata"` for list view; `preload="auto"` for detail view; lazy load audio on first play

3. **Production Deployment: Static vs. SSR**
   - What we know: Vite builds static assets (SPA); Spring Boot can serve static files
   - What's unclear: SEO needs for vocabulary app (probably low); SSR complexity vs. benefits
   - Recommendation: Start with static SPA (simpler); Spring Boot serves frontend from `src/main/resources/static/`; revisit SSR in future phase if SEO needed

4. **Offline Support**
   - What we know: Service Workers enable offline caching; audio files can be cached
   - What's unclear: Priority for offline support; complexity of service worker setup
   - Recommendation: Defer to Phase 8 (production readiness); Phase 5 focuses on online experience

## Sources

### Primary (HIGH confidence)

- [React 19 Official Blog](https://react.dev/blog/2024/12/05/react-19) - Actions, useActionState, ref as prop, form enhancements
- [Vite 6 Announcement](https://vite.dev/blog/announcing-vite6) - Environment API, Node.js requirements, breaking changes
- [TanStack Query v5 Docs](https://tanstack.com/query/v5/docs/framework/react/overview) - Query patterns, mutations, invalidation
- [Zustand GitHub README](https://github.com/pmndrs/zustand) - Store creation, selectors, TypeScript integration

### Secondary (MEDIUM confidence)

- [React v19 – React](https://react.dev/blog/2024/12/05/react-19) - Official React 19 announcement
- [Vite 6.0 is out! | Vite](https://vite.dev/blog/announcing-vite6) - Official Vite 6 release notes
- [How to Use React Query (TanStack Query) for Server State Management](https://oneuptime.com/blog/post/2026-01-15-react-query-tanstack-server-state/view) - TanStack Query patterns 2026
- [State Management in 2026: Redux, Context API, and Modern Patterns](https://www.nucamp.co/blog/state-management-in-2026-redux-context-api-and-modern-patterns) - State management trends
- [Complete Guide to Setting Up React with TypeScript and Vite (2026)](https://medium.com/@robinviktorsson/complete-guide-to-setting-up-react-with-typescript-and-vite-2025-468f6556aaf2) - Project setup patterns
- [Bulgarian Cyrillic on the Web: Techniques for Authentic Font Rendering](https://medium.com/@mevbg/bulgarian-cyrillic-on-the-web-techniques-for-authentic-font-rendering-bec82c24e39f) - Bulgarian typography
- [Learn Zod validation with React Hook Form | Contentful](https://www.contentful.com/blog/react-hook-form-validation-zod/) - Form validation patterns
- [Tailwind CSS v4: The Complete Guide for 2026 | DevToolbox Blog](https://devtoolbox.dedyn.io/blog/tailwind-css-v4-complete-guide) - Tailwind CSS 4 setup

### Tertiary (LOW confidence - unverified WebSearch)

- Various Medium articles on React patterns (Jan-Feb 2026) - Common mistakes, optimization techniques
- LogRocket blogs on React performance - Build optimization strategies
- Community blog posts on TanStack Query pitfalls - Anti-patterns and best practices

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Official docs and releases verified; React 19, Vite 6, TanStack Query v5 all stable releases
- Architecture: HIGH - Patterns verified from official docs and community best practices (TkDodo blog, official guides)
- Pitfalls: MEDIUM - Based on verified common issues (WebSearch + official docs); some from community sources

**Research date:** 2026-02-16
**Valid until:** 2026-03-16 (30 days - stable ecosystem)

**Notes:**
- React 19 released December 2024; stable for production use
- Vite 6 released November 2024; stable for production use
- TanStack Query v5 mature and widely adopted
- Tailwind CSS 4 in release candidate stage (CSS-first configuration)
- Bulgarian Cyrillic font rendering requires specific fonts (Sofia Sans recommended)
- No Context7 available during research; relied on official docs and verified WebSearch sources
