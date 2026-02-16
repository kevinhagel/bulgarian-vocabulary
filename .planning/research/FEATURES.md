# Feature Research

**Domain:** Vocabulary/Language Learning Applications
**Researched:** 2026-02-15
**Confidence:** HIGH

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Flashcard study mode | Core vocabulary learning mechanism used by all major apps (Anki, Quizlet, Duolingo) | LOW | Multiple quiz modes expected: recognition, recall, typing |
| Spaced repetition system (SRS) | Scientifically proven technique for long-term retention; industry standard since Anki popularized it | MEDIUM | Algorithm determines optimal review intervals based on performance |
| Progress tracking | Users need to see learning progress to maintain motivation; 43% quit within 6 months without visible progress | LOW-MEDIUM | Study streaks, cards due, accuracy rates, performance analytics |
| Word list management | Basic organizational need; users need to categorize and manage vocabulary collections | LOW | Create, edit, delete lists; basic CRUD operations |
| Audio playback (TTS) | Critical for pronunciation learning; listening practice is one of four essential skills | MEDIUM | Native speaker quality audio; may use TTS or recorded audio |
| Example sentences | Vocabulary retention improves with contextual usage vs rote memorization | LOW-MEDIUM | Real-world context sentences, not robotic filler |
| Multi-device sync | Users expect to study across desktop and mobile; offline capability expected | MEDIUM-HIGH | Cloud sync with offline support like Anki |
| Search and lookup | Quick access to words in large vocabularies; frustration point if missing | LOW | Basic search across word lists |
| Word definitions/translations | Fundamental need for vocabulary learning; approachable definitions with multiple meanings | LOW | English translations with clear definitions |
| Daily practice reminders | Irregular study habits harm retention; apps must encourage 10-15 minute daily sessions | LOW | Push notifications, streak tracking |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not expected, but valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| LLM-powered lemma detection | Automatically identify word root forms and generate inflections for Bulgarian's complex grammar | HIGH | Addresses specific Bulgarian complexity; saves manual entry time |
| Text parsing for vocabulary extraction | Extract vocabulary from dialog printouts and tutoring materials Elena provides | MEDIUM-HIGH | NLP-based extraction; auto-generate study lists from real materials |
| Tutor integration workflow | Purpose-built for students working with tutors; import from tutor materials | MEDIUM | Aligns with user context (Elena tutoring); differs from self-study apps |
| Context-aware categorization | Auto-categorize by lesson, dialog, or theme from source material | MEDIUM | Organizes vocabulary by tutoring context vs generic lists |
| Cyrillic alphabet support | First-class support for Bulgarian Cyrillic with proper rendering and input | MEDIUM | Many apps have issues with non-Roman text; table stakes for Bulgarian specifically |
| Grammar-aware flashcards | Display inflections, aspect pairs, and grammatical context for Bulgarian | MEDIUM-HIGH | Leverages LLM capabilities; addresses Bulgarian's grammatical complexity |
| Smart review scheduling | Adaptive algorithm considering Bulgarian-specific challenges (aspect, case system) | HIGH | More sophisticated than basic SRS; language-aware difficulty adjustment |
| Personalized knowledge map | Visual representation of vocabulary mastery by category/topic | MEDIUM | Used by WordUp; helps learners see progress patterns |
| Offline-first architecture | Study without connectivity; sync when available | MEDIUM | Important for mobile learning; Anki does this well |
| Custom word metadata | Tags, notes, difficulty ratings, tutor comments | LOW-MEDIUM | Power-user feature for organized learners |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Gamification with points/badges | "Makes learning fun" like Duolingo | Extrinsic motivation crowds out intrinsic; can feel childish for adult learners with tutors | Simple streak tracking and progress visualization; focus on mastery not rewards |
| Social features/leaderboards | "Competitive learning" | Adds complexity; privacy concerns; not relevant for 1-on-1 tutoring context | Optional sharing of study stats with tutor only |
| Built-in lessons/curriculum | "Complete learning solution" | Not the core value; user has tutor (Elena); curriculum is her job | Focus on vocabulary management for existing lessons |
| Conversation practice chatbot | "AI conversation partner" | High complexity; doesn't replace tutor; pronunciation feedback unreliable for Bulgarian | Provide example dialogs from tutor materials; actual conversation is with Elena |
| Community-generated content | "Learn from other users" | Quality control issues; Bulgarian content may have errors (noted in research) | Curated content from tutor only; maintain quality |
| Advanced analytics dashboard | "Data-driven learning" | Analysis paralysis; users want simple "am I progressing?" not charts | Simple metrics: streak, accuracy, words mastered; detailed stats optional |
| Multiple language support | "Expand user base" | Dilutes focus; each language has unique challenges (Bulgarian Cyrillic, inflections) | Bulgarian-only in MVP; excellence in one language beats mediocrity in many |
| Real-time collaboration | "Study with friends" | Complex sync requirements; not aligned with tutor-student model | Async sharing of lists; tutor can review progress |
| Video lessons | "Comprehensive learning" | Production costs; not the core value proposition | Focus on vocabulary; actual teaching is Elena's role |
| Native app for every platform | "Reach all users" | Development and maintenance burden | Progressive Web App (PWA) works across platforms; native mobile if validated |

## Feature Dependencies

```
Text Parsing
    └──requires──> LLM Integration
                       └──enables──> Lemma Detection
                                         └──enables──> Inflection Generation

Flashcard Study Mode
    └──requires──> Word List Management
    └──enhances──> Spaced Repetition System

Audio Playback
    └──requires──> TTS Integration or Audio Storage

Multi-device Sync
    └──requires──> Cloud Storage/Backend
    └──conflicts-with──> Offline-First (needs reconciliation strategy)

Progress Tracking
    └──requires──> Study Session Recording
    └──enhances──> Spaced Repetition System

Context-aware Categorization
    └──requires──> Text Parsing
    └──enhances──> Word List Management

Grammar-aware Flashcards
    └──requires──> LLM Integration
    └──requires──> Lemma Detection
```

### Dependency Notes

- **Text Parsing requires LLM Integration:** NLP-based vocabulary extraction needs LLM for Bulgarian-specific processing
- **LLM Integration enables Lemma Detection:** Once LLM is integrated, can detect word roots and generate inflections
- **Flashcard Mode requires Word Lists:** Can't study if there's no vocabulary to study
- **Spaced Repetition enhances Flashcards:** SRS algorithm determines which cards to show when
- **Multi-device Sync conflicts with Offline-First:** Need conflict resolution strategy for offline edits
- **Progress Tracking requires Session Recording:** Must log study activity to calculate statistics
- **Context-aware Categorization requires Text Parsing:** Auto-categorization depends on source material analysis

## MVP Definition

### Launch With (v1)

Minimum viable product — what's needed to validate the concept.

- [ ] **Word list management** — Core organizational capability; must create/edit/delete lists
- [ ] **Manual vocabulary entry** — Add words with translations; baseline data entry
- [ ] **Basic flashcard study mode** — Recognition mode (see Bulgarian, recall English)
- [ ] **Audio playback** — Hear correct pronunciation (TTS acceptable for MVP)
- [ ] **Simple progress tracking** — Study streak, words studied today, basic accuracy
- [ ] **Example sentences** — Context for each word (1-2 sentences minimum)
- [ ] **Cyrillic support** — Proper rendering and input for Bulgarian
- [ ] **Mobile-responsive web app** — Study on phone/tablet/desktop (PWA)

**Rationale:** These features enable the core loop: add vocabulary from tutoring sessions → study with flashcards → track progress → maintain daily practice. Addresses primary user need (organizing and studying vocabulary from Elena) without overbuilding.

### Add After Validation (v1.x)

Features to add once core is working.

- [ ] **Spaced repetition algorithm** — When daily usage is established and retention data exists (requires 2+ weeks usage)
- [ ] **Text parsing/vocabulary extraction** — When manual entry friction is validated as real problem
- [ ] **LLM-powered lemma detection** — After text parsing; reduces manual work for inflections
- [ ] **Recall mode flashcards** — Type the translation; after recognition mode is working
- [ ] **Categorization and tagging** — When users have 50+ words and organization becomes problem
- [ ] **Offline support** — After multi-device usage is validated need
- [ ] **Advanced progress analytics** — When users request more detailed insights

**Triggers for each:**
- **SRS:** Users complete 14+ days of study; have retention data
- **Text parsing:** Users report manual entry is tedious; have dialog printouts to test with
- **Lemma detection:** Text parsing working; users manually entering inflections
- **Recall mode:** Recognition mode retention rate > 70%; users want harder challenge
- **Categorization:** Average user has 50+ words; reports difficulty finding words
- **Offline:** Users report studying in no-connectivity situations
- **Analytics:** Users ask "how am I doing?" beyond basic metrics

### Future Consideration (v2+)

Features to defer until product-market fit is established.

- [ ] **Grammar-aware flashcards** — Defer until core vocabulary workflow proven (6+ months)
- [ ] **Multi-device cloud sync** — After local-first version validated; adds backend complexity
- [ ] **Personalized knowledge map** — Visual progress; nice-to-have not essential
- [ ] **Custom word metadata** — Power-user feature; wait for demand signal
- [ ] **Tutor dashboard** — Elena's view of student progress; defer until multiple students
- [ ] **Shared word lists** — Sharing between students; wait for user base
- [ ] **Alternative study modes** — Matching, multiple choice, etc.; after core modes proven
- [ ] **Native mobile apps** — If PWA has limitations; defer to reduce complexity

**Why defer:**
- **Grammar-aware flashcards:** High complexity; core vocabulary learning must work first
- **Cloud sync:** Backend infrastructure overhead; local-first is simpler for MVP
- **Knowledge map:** Visualization is polishing; basic stats sufficient initially
- **Custom metadata:** Optimization for power users; most won't use initially
- **Tutor dashboard:** Build for 1 student first; scale when there are multiple
- **Shared lists:** Network effects need user base; not relevant for single student
- **Alternative modes:** Flashcard basics must work before variations
- **Native apps:** PWA validates concept before platform-specific investment

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Flashcard study mode | HIGH | LOW | P1 |
| Word list management | HIGH | LOW | P1 |
| Audio playback (TTS) | HIGH | MEDIUM | P1 |
| Cyrillic support | HIGH | MEDIUM | P1 |
| Example sentences | HIGH | LOW | P1 |
| Basic progress tracking | MEDIUM | LOW | P1 |
| Manual vocabulary entry | HIGH | LOW | P1 |
| Mobile-responsive UI | HIGH | MEDIUM | P1 |
| Spaced repetition system | HIGH | MEDIUM | P2 |
| Text parsing extraction | HIGH | HIGH | P2 |
| LLM lemma detection | MEDIUM-HIGH | HIGH | P2 |
| Categorization/tagging | MEDIUM | MEDIUM | P2 |
| Recall mode flashcards | MEDIUM | LOW | P2 |
| Offline support | MEDIUM | MEDIUM-HIGH | P2 |
| Advanced analytics | LOW-MEDIUM | MEDIUM | P2 |
| Grammar-aware flashcards | MEDIUM | HIGH | P3 |
| Multi-device sync | MEDIUM | HIGH | P3 |
| Knowledge map visualization | LOW | MEDIUM | P3 |
| Custom metadata | LOW | MEDIUM | P3 |
| Tutor dashboard | LOW | MEDIUM-HIGH | P3 |
| Shared word lists | LOW | MEDIUM | P3 |

**Priority key:**
- P1: Must have for launch (core vocabulary learning loop)
- P2: Should have, add when validated (optimization and automation)
- P3: Nice to have, future consideration (polish and scaling)

## Competitor Feature Analysis

| Feature | Anki | Duolingo | Quizlet | Our Approach (Bulgarian Tutor App) |
|---------|------|----------|---------|-------------------------------------|
| Spaced repetition | Advanced SRS algorithm (gold standard) | Simplified SRS | AI-powered SRS | Start simple (v1.x), implement proven algorithm |
| Content creation | User-created decks; steep learning curve | Pre-built lessons only | Mix of user and community | User creates from tutor materials; import from text |
| Vocabulary entry | Manual card creation; flexible but tedious | No custom vocabulary | Manual or AI-generated | Manual initially; auto-extract from tutor materials (v1.x) |
| Study modes | Flashcards only (recognition/recall) | Gamified exercises, multiple types | 7+ modes (Learn, Match, Test, etc.) | Focus on flashcards (recognition → recall); keep simple |
| Mobile experience | Functional but dated UI | Polished, modern, gamified | Modern, social features | Modern PWA; no gamification gimmicks |
| Progress tracking | Utilitarian stats; detailed | Streaks, XP, leaderboards | Performance analytics, study streaks | Simple stats; focus on mastery not points |
| Language support | User-dependent; any language | 40+ languages with courses | Any language (user content) | Bulgarian-specific; Cyrillic first-class |
| Audio | User must add audio | Native speaker audio for all lessons | TTS or user-added | TTS initially; may add native recordings |
| Context/sentences | User must add | Integrated in lessons | Optional; user-added | Built-in for each word; real context |
| Grammar support | User-created notes | Integrated grammar tips | User-dependent | LLM-generated inflections/aspect notes (v1.x+) |
| Offline support | Full offline with sync | Limited offline | Limited offline | PWA offline-capable (v1.x) |
| Price model | Free (desktop), $25 iOS one-time | Freemium with ads/subscription | Freemium (Plus subscription) | TBD; likely free for tutoring students |

### Key Differentiators from Competitors

**vs Anki:**
- Modern UI/UX (Anki's interface is dated and intimidating)
- Bulgarian-specific features (lemma detection, inflections, Cyrillic)
- Easier vocabulary entry (text parsing vs manual card creation)
- Tutor-integrated workflow (built for students with teachers)

**vs Duolingo:**
- Custom vocabulary from tutor (not pre-built lessons)
- No gamification overhead (serious learning for adult students)
- Grammar-aware for Bulgarian (not generic language learning)
- Focus on vocabulary depth (not broad curriculum)

**vs Quizlet:**
- Spaced repetition from start (not addon feature)
- Language-specific intelligence (lemma detection, inflections)
- Tutor material import (purpose-built workflow)
- Quality over community content (no random user decks with errors)

## Sources

### Vocabulary Learning App Features
- [Best Vocabulary Learning Apps 2026: 15 Picks to Explore](https://brighterly.com/blog/best-vocabulary-learning-apps/)
- [5 Best Vocabulary Builder Apps in 2026](https://emergent.sh/learn/best-vocabulary-builder-apps)
- [Best Vocabulary Apps 2026 - Top 14](https://www.speedreadinglounge.com/vocabulary-apps)
- [Grow your vocabulary fast: find the best vocabulary app in 2026](https://www.heylama.com/blog/guide-to-vocabulary-flashcard-apps)

### Language Learning Best Practices
- [10 Best Apps for Learning Languages in 2026](https://www.italki.com/en/blog/best-apps-for-learning-languages)
- [How to Build a Language Learning App: Essential Features and Best Practices](https://easternpeak.com/blog/how-to-build-a-language-learning-app/)
- [Best Language Learning Apps for 2026](https://lingua-learn.ca/blogs/language-learning-apps-2026/)

### Competitor Analysis
- [Anki vs. Duolingo: Which Language Learning App Really Works?](https://speakada.com/anki-vs-duolingo-which-language-learning-app-really-works/)
- [Anki Vs Duolingo: Which App Boosts Your Language Skills Best?](https://duolingoguides.com/anki-vs-duolingo/)
- [Vocabeo vs Duolingo, Quizlet, Anki & Clozemaster – Feature Comparison](https://vocabeo.com/comparison)

### Spaced Repetition Systems
- [Spaced Repetition App Guide: Remember What You Read [2025–2026]](https://makeheadway.com/blog/spaced-repetition-app/)
- [Best Anki Alternatives in 2026: 7 Flashcard Apps](https://goodoff.co/blog/best-anki-alternatives-2026-flashcard-apps)
- [Mochi - Spaced repetition flashcards](https://mochi.cards/)

### AI Features in Language Learning
- [Best AI Language Learning Apps in 2026](https://copycatcafe.com/blog/ai-language-learning-apps)
- [What's the best AI language learning app in 2026?](https://languatalk.com/blog/whats-the-best-ai-for-language-learning/)
- [Meet Langua, the world's most advanced AI language coach](https://languatalk.com/try-langua)
- [Inside Praktika's conversational approach to language learning](https://openai.com/index/praktika/)

### Context and Example Usage
- [Best Vocabulary Learning Apps 2026: 15 Picks to Explore](https://brighterly.com/blog/best-vocabulary-learning-apps/)
- [The Vocabulary App](https://thevocabulary.app/)
- [Use Context in Vocabulary Learning](https://www.shanahanonliteracy.com/blog/how-i-teach-students-to-use-context-in-vocabulary-learning)

### Common Mistakes and Anti-Patterns
- [Avoid These 7 Language Learning App Mistakes in the US](https://finmodelslab.com/blogs/avoid-mistakes/ai-enhanced-language-learning-app-avoid-mistakes)
- [Mistakes to Avoid When Using Language Learning Apps](https://talkpal.ai/mistakes-to-avoid-when-using-language-learning-apps/)
- [Common Language Learning Mistakes to Avoid Today](https://learnship.com/common-language-learning-mistakes)

### Bulgarian-Specific Challenges
- [Best apps to learn Bulgarian: Top picks and expert guide 2025](https://preply.com/en/blog/best-apps-learn-bulgarian/)
- [10 Best Apps To Learn Bulgarian Online](https://ling-app.com/blog/apps-to-learn-bulgarian/)
- [The 6 Best Apps to Learn Bulgarian Rapidly](https://www.langoly.com/best-bulgarian-apps/)

### Word List Management and Categorization
- [Wokabulary — Flash card vocabulary learning app](https://wokabulary.com/)
- [WordUp Vocabulary App](https://www.wordupapp.co/)
- [Vocabulary Organizer Apps: vocabs](https://www.trendhunter.com/trends/vocabs)
- [Vocabulary Miner - Brilliantly simple Flashcards](https://vocabulary-miner.com/)

### Progress Tracking and Analytics
- [Brainscape: The Best Flashcards App](https://www.brainscape.com)
- [The 7 Best Flashcard Apps for iPhone and Android](https://mobilemarketingreads.com/best-flashcard-apps-for-iphone-and-android/)
- [Frontend Mentor | Build a flashcard app with study modes and progress tracking](https://www.frontendmentor.io/articles/build-a-flashcard-app-with-study-modes-and-progress-tracking-aOCRXXFul8)

---
*Feature research for: Bulgarian Vocabulary Tutoring Application*
*Researched: 2026-02-15*
