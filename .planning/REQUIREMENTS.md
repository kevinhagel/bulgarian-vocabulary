# Requirements: Bulgarian Vocabulary Tutor

**Defined:** 2026-02-15
**Core Value:** Each vocabulary entry must accurately represent the lemma (dictionary headword) with all its inflections, English translation, and metadata, enabling effective study through audio playback and interactive learning tools.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Vocabulary Management (VOCAB)

- [ ] **VOCAB-01**: User can manually enter any Bulgarian word form (inflection) and system detects canonical lemma
- [ ] **VOCAB-02**: User can manually enter multi-word lemma (e.g., "казвам се", "искам да")
- [ ] **VOCAB-03**: LLM auto-generates all inflections for entered lemma (person, number, tense, aspect, mood, gender)
- [ ] **VOCAB-04**: LLM auto-generates part of speech (verb, noun, adjective, pronoun, etc.)
- [ ] **VOCAB-05**: LLM auto-generates topic/category tags
- [ ] **VOCAB-06**: LLM auto-generates difficulty level (beginner, intermediate, advanced)
- [ ] **VOCAB-07**: User can enter English translation (required field)
- [ ] **VOCAB-08**: User can enter notes (optional, user-editable field)
- [ ] **VOCAB-09**: User can edit vocabulary entry (lemma text, translation, notes, inflections)
- [ ] **VOCAB-10**: User can delete vocabulary entry
- [ ] **VOCAB-11**: User can browse all vocabulary entries
- [ ] **VOCAB-12**: User can search vocabulary by lemma text (Bulgarian full-text search with Cyrillic support)
- [ ] **VOCAB-13**: User can filter vocabulary by part of speech, category, difficulty, source
- [ ] **VOCAB-14**: System distinguishes user-entered lemmas vs system-seeded reference vocabulary

### Reference Vocabulary (REF)

- [ ] **REF-01**: System pre-populates interrogatives via Flyway migration (кой, какво, кога, къде, защо, как, докога, etc.)
- [ ] **REF-02**: System pre-populates pronouns via Flyway migration (аз, ти, той, тя, то, ние, вие, те)
- [ ] **REF-03**: System pre-populates common prepositions via Flyway migration (в, на, от, до, за, с, при, без)
- [ ] **REF-04**: System pre-populates common conjunctions via Flyway migration (и, но, или, че, защото, когато)
- [ ] **REF-05**: System pre-populates numerals via Flyway migration (един/една/едно, два/две, три, etc.)

### Audio & TTS (AUDIO)

- [ ] **AUDIO-01**: System generates audio for lemma using Edge TTS
- [ ] **AUDIO-02**: System generates audio for all inflections using Edge TTS
- [ ] **AUDIO-03**: Audio files stored on disk (not database BLOBs)
- [ ] **AUDIO-04**: User can play audio for lemma in vocabulary list
- [ ] **AUDIO-05**: User can play audio for inflections when viewing lemma details
- [ ] **AUDIO-06**: Audio generation happens asynchronously in background
- [ ] **AUDIO-07**: System caches generated audio files to avoid regeneration

### LLM Integration (LLM)

- [ ] **LLM-01**: System connects to Ollama instance on Mac Studio
- [ ] **LLM-02**: System uses Spring AI 2.0 for LLM integration
- [ ] **LLM-03**: LLM calls execute asynchronously to avoid blocking UI
- [ ] **LLM-04**: System implements circuit breaker pattern for LLM failures
- [ ] **LLM-05**: System caches LLM responses to reduce redundant API calls
- [ ] **LLM-06**: User can review LLM-generated metadata before saving
- [ ] **LLM-07**: System validates LLM outputs for obvious errors (empty inflections, malformed data)

### Study - Flashcards (FLASH)

- [ ] **FLASH-01**: User can start flashcard study session
- [ ] **FLASH-02**: Flashcard shows lemma text and plays audio on click
- [ ] **FLASH-03**: User can reveal English translation on flashcard
- [ ] **FLASH-04**: User can mark answer as "correct" or "incorrect"
- [ ] **FLASH-05**: User can view all inflections on flashcard
- [ ] **FLASH-06**: User can play audio for any inflection on flashcard
- [ ] ] **FLASH-07**: Flashcard session shows progress (X of Y cards reviewed)
- [ ] **FLASH-08**: User can end session early and save progress

### Study - Spaced Repetition (SRS)

- [ ] **SRS-01**: System schedules flashcard reviews based on spaced repetition algorithm
- [ ] **SRS-02**: System increases review interval when user answers correctly
- [ ] **SRS-03**: System decreases review interval when user answers incorrectly
- [ ] **SRS-04**: User sees due date for next review on vocabulary entry
- [ ] **SRS-05**: System caps daily reviews to prevent overwhelming backlog
- [ ] **SRS-06**: System implements forgiveness logic for missed reviews (no snowball of shame)

### Study - Word Lists (LISTS)

- [ ] **LISTS-01**: User can create named word list (e.g., "Week 3 Verbs", "Food Vocabulary")
- [ ] **LISTS-02**: User can add lemmas to word list
- [ ] **LISTS-03**: User can remove lemmas from word list
- [ ] **LISTS-04**: User can delete word list
- [ ] **LISTS-05**: User can view all word lists
- [ ] **LISTS-06**: User can study word list as flashcard session (list-specific study mode)

### Study - Progress Tracking (PROGRESS)

- [ ] **PROGRESS-01**: System tracks total vocabulary count (user-entered only, exclude reference)
- [ ] **PROGRESS-02**: System tracks total study sessions completed
- [ ] **PROGRESS-03**: System tracks cards reviewed (total count)
- [ ] **PROGRESS-04**: System calculates retention rate (correct / total reviews)
- [ ] **PROGRESS-05**: User can view overall progress dashboard
- [ ] **PROGRESS-06**: User can view per-lemma study stats (last reviewed, review count, correctness rate)

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Text Analysis (TEXT)

- **TEXT-01**: User can paste Bulgarian text (paragraph, dialog, sentence)
- **TEXT-02**: System parses text and extracts single words (tokenization)
- **TEXT-03**: System highlights words in text (green = already in vocabulary, yellow = new)
- **TEXT-04**: User can click word to add to vocabulary
- **TEXT-05**: User can bulk-add all new words detected in text
- **TEXT-06**: System deduplicates detected words against existing vocabulary
- **TEXT-07**: System batch-processes words via LLM for lemma detection

### Advanced Study Features

- **STUDY-01**: Grammar-aware flashcards (verb conjugation drills, noun declension practice)
- **STUDY-02**: Customizable flashcard templates (show lemma → translate, or translate → show lemma)
- **STUDY-03**: Study statistics and analytics (learning curve, problem areas, retention over time)
- **STUDY-04**: Offline PWA support with sync when online

### Tutor Features

- **TUTOR-01**: Tutor dashboard showing student vocabulary progress
- **TUTOR-02**: Tutor can assign word lists to students
- **TUTOR-03**: Tutor can see which words students struggle with

### Infrastructure

- **INFRA-01**: Cloud sync across devices (same vocabulary on laptop, phone, tablet)
- **INFRA-02**: Export vocabulary as CSV/JSON
- **INFRA-03**: Import vocabulary from CSV/JSON
- **INFRA-04**: Multi-user support with authentication

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Speech-to-Text (STT) | Learned from bulgarian-tutor-web that no STT technologies work reliably for Bulgarian |
| Fill-in-the-blank exercises | Tried in bulgarian-tutor-web, didn't work well — defer to future phase after validating other study modes |
| Multi-word phrase detection from text | Too ambiguous; user manually enters multi-word lemmas to maintain control |
| Mobile native app | Web-first with PWA; defer native mobile to v2+ |
| Real-time collaboration | Single-user focus for v1 |
| Gamification (streaks, XP, badges) | Risk optimizing for engagement over actual learning; defer until retention validated |
| Community-generated content | Quality issues for Bulgarian; focus on personal vocabulary from tutor |
| Conversation chatbot | Doesn't replace tutor; adds complexity without core value |
| Grammar lessons/curriculum | That's Elena's job; focus on vocabulary management |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| VOCAB-01 | Phase 4 | Pending |
| VOCAB-02 | Phase 4 | Pending |
| VOCAB-03 | Phase 4 | Pending |
| VOCAB-04 | Phase 4 | Pending |
| VOCAB-05 | Phase 4 | Pending |
| VOCAB-06 | Phase 4 | Pending |
| VOCAB-07 | Phase 4 | Pending |
| VOCAB-08 | Phase 4 | Pending |
| VOCAB-09 | Phase 4 | Pending |
| VOCAB-10 | Phase 4 | Pending |
| VOCAB-11 | Phase 4 | Pending |
| VOCAB-12 | Phase 4 | Pending |
| VOCAB-13 | Phase 4 | Pending |
| VOCAB-14 | Phase 4 | Pending |
| REF-01 | Phase 4 | Pending |
| REF-02 | Phase 4 | Pending |
| REF-03 | Phase 4 | Pending |
| REF-04 | Phase 4 | Pending |
| REF-05 | Phase 4 | Pending |
| AUDIO-01 | Phase 3 | Pending |
| AUDIO-02 | Phase 3 | Pending |
| AUDIO-03 | Phase 3 | Pending |
| AUDIO-04 | Phase 5 | Pending |
| AUDIO-05 | Phase 5 | Pending |
| AUDIO-06 | Phase 3 | Pending |
| AUDIO-07 | Phase 3 | Pending |
| LLM-01 | Phase 1 | Pending |
| LLM-02 | Phase 1 | Pending |
| LLM-03 | Phase 2 | Pending |
| LLM-04 | Phase 2 | Pending |
| LLM-05 | Phase 2 | Pending |
| LLM-06 | Phase 2 | Pending |
| LLM-07 | Phase 2 | Pending |
| FLASH-01 | Phase 6 | Pending |
| FLASH-02 | Phase 6 | Pending |
| FLASH-03 | Phase 6 | Pending |
| FLASH-04 | Phase 6 | Pending |
| FLASH-05 | Phase 6 | Pending |
| FLASH-06 | Phase 6 | Pending |
| FLASH-07 | Phase 6 | Pending |
| FLASH-08 | Phase 6 | Pending |
| SRS-01 | Phase 6 | Pending |
| SRS-02 | Phase 6 | Pending |
| SRS-03 | Phase 6 | Pending |
| SRS-04 | Phase 6 | Pending |
| SRS-05 | Phase 8 | Pending |
| SRS-06 | Phase 8 | Pending |
| LISTS-01 | Phase 7 | Pending |
| LISTS-02 | Phase 7 | Pending |
| LISTS-03 | Phase 7 | Pending |
| LISTS-04 | Phase 7 | Pending |
| LISTS-05 | Phase 7 | Pending |
| LISTS-06 | Phase 7 | Pending |
| PROGRESS-01 | Phase 6 | Pending |
| PROGRESS-02 | Phase 6 | Pending |
| PROGRESS-03 | Phase 6 | Pending |
| PROGRESS-04 | Phase 6 | Pending |
| PROGRESS-05 | Phase 6 | Pending |
| PROGRESS-06 | Phase 6 | Pending |

**Coverage:**
- v1 requirements: 59 total
- Mapped to phases: 59/59 (100%)
- Unmapped: 0

---
*Requirements defined: 2026-02-15*
*Last updated: 2026-02-15 after roadmap creation*
