# Pitfalls Research

**Domain:** Bulgarian Vocabulary Learning Application
**Researched:** 2026-02-15
**Confidence:** MEDIUM-HIGH

## Critical Pitfalls

### Pitfall 1: Morphological Data Stored as Flat Strings

**What goes wrong:**
Storing Bulgarian word forms as simple strings without proper relational modeling leads to data duplication, inconsistent inflections, and inability to query morphological patterns. When you need to show all forms of a verb or find all words with a particular aspect, you're stuck doing string matching instead of structured queries.

**Why it happens:**
Bulgarian's rich morphology (gender, number, definiteness, aspect, tense) can seem overwhelming. Developers take the "easy" path: store each word form separately without capturing the relationships between lemma and inflections. LLMs can generate inflection data, but without a proper schema to store it, you lose the structure.

**How to avoid:**
Design your database schema to separate:
- **Lemmas table**: Base forms (e.g., "чета" - to read)
- **Word forms table**: All inflected forms with morphological tags
- **Morphological features table**: Gender, number, definiteness, aspect, tense, person
- **Relationships**: Foreign keys linking forms to lemmas

Use PostgreSQL's JSONB for flexible morphological metadata while maintaining relational integrity for core data.

**Warning signs:**
- Finding yourself duplicating example sentences across word forms
- Unable to answer "show me all perfective verbs the user knows"
- LLM generates good inflection data but you can't query it effectively
- Realizing you need to update the same information in 20+ places

**Phase to address:**
Foundation/Data Model phase - this must be right from day one. Changing your core schema later requires migrating all vocabulary data and rebuilding indexes.

---

### Pitfall 2: LLM Hallucinations Treated as Ground Truth

**What goes wrong:**
LLMs (including Ollama models) generate plausible but incorrect linguistic analysis. Bulgarian learners internalize wrong verb aspects, incorrect word genders, or non-existent inflection patterns. The app sounds authoritative, users trust it, and they learn incorrect Bulgarian.

**Why it happens:**
LLMs are trained on broad datasets, not specialized Bulgarian linguistic corpora. They excel at generating plausible-sounding content but can fabricate morphological rules, especially for:
- Irregular verb conjugations
- Aspect pairs (imperfective/perfective)
- Definiteness marking on adjectives
- Verbal noun formations

Bulgarian linguistic data is less abundant in training sets than English, amplifying this problem.

**How to avoid:**
1. **Never trust LLM output without validation**:
   - Maintain a curated dictionary of verified lemmas and inflections
   - Flag LLM-generated content as "unverified" until human review
   - Build a feedback loop where users can report errors

2. **Use LLMs for what they do well**:
   - Detecting lemmas from text (high accuracy)
   - Generating example sentences (verify for naturalness, not grammar rules)
   - Creating metadata (translations, usage notes)

3. **Use verified sources for what LLMs do poorly**:
   - Morphological analysis: Use rule-based systems or verified dictionaries
   - Inflection tables: Pre-populate from authoritative sources
   - Aspect pairs: Manual curation essential

**Warning signs:**
- Users reporting "this conjugation doesn't match my textbook"
- Native speakers flagging incorrect forms in user feedback
- LLM generating different results for the same word on repeated calls
- Inflection patterns that violate Bulgarian phonological rules

**Phase to address:**
- **Foundation**: Implement verification layer for LLM outputs
- **MVP**: Add user feedback mechanism for linguistic errors
- **Polish**: Build human review queue for unverified content

---

### Pitfall 3: Spaced Repetition System Breaks with Inconsistency

**What goes wrong:**
The mathematical precision of SRS algorithms (SM-2, FSRS) falls apart when users miss days of study. Cards pile up, review sessions become overwhelming (500+ cards due), users feel defeated and abandon the app. The "snowball of shame" effect: the longer you're away, the harder it is to come back.

**Why it happens:**
Life happens - users get sick, travel, or lose motivation. Most SRS implementations are unforgiving: they schedule reviews assuming perfect adherence. Bulgarian's morphological complexity means users already face harder cards than English learners, amplifying the overwhelm.

**How to avoid:**
1. **Implement forgiving catch-up logic**:
   - Cap daily review count (e.g., max 100 cards even if 500 due)
   - Gradually reintroduce overdue cards over several days
   - Reset intervals for cards overdue by >30 days

2. **Prevent pile-up before it happens**:
   - Alert users before reviews cross threshold (>50 due tomorrow)
   - Offer "review vacation" mode that pauses new cards
   - Show streak but don't punish breaks

3. **Design for real human behavior**:
   - "I have 5 minutes" quick session mode
   - Priority-based reviews (hardest cards first, or newest)
   - Option to defer specific cards without penalty

**Warning signs:**
- High drop-off after users miss 2-3 days
- Users creating new accounts to "start fresh" instead of catching up
- Support requests: "how do I reset my reviews?"
- Average session length declining over time

**Phase to address:**
- **MVP**: Implement SRS with forgiveness logic from day one
- **Growth**: Add analytics to detect users at risk of abandonment
- **Polish**: Sophisticated catch-up modes and motivation recovery

---

### Pitfall 4: Text-to-Speech Quality Gaps Ruin Pronunciation Learning

**What goes wrong:**
Poor TTS quality for Bulgarian (robotic voices, wrong stress patterns, incorrect pronunciation of consonant clusters) teaches users bad pronunciation. Unlike English with abundant high-quality TTS, Bulgarian TTS can have:
- Incorrect stress placement (critical in Bulgarian)
- Poor handling of consonant reduction
- Robotic prosody that obscures natural speech patterns
- Wrong pronunciation of ъ (schwa) and other Bulgarian-specific phonemes

**Why it happens:**
Bulgarian is lower-resourced than major European languages. Many TTS engines have limited Bulgarian training data. Edge TTS (from archived project) worked well, but relying on a single provider creates risk. Performance issues arise when:
- Generating TTS on-the-fly for every review (latency)
- Caching all possible word forms (storage explosion)
- Edge TTS rate limits or API changes

**How to avoid:**
1. **Quality validation**:
   - Test TTS with native speakers before launch
   - Prioritize stress accuracy and natural prosody
   - Have fallback pronunciation guides for words TTS mangles

2. **Performance strategy**:
   - Pre-generate and cache TTS for common vocabulary (top 5000 words)
   - Generate on-demand for user-added words, then cache
   - Use background jobs to pre-warm cache for user's study queue
   - Monitor cache hit rates - should be >95% for active users

3. **Provider resilience**:
   - Abstract TTS behind an interface
   - Have fallback provider if primary fails (Edge TTS → Google TTS)
   - Store audio files locally, not just URLs

**Warning signs:**
- Users asking "how is this word really pronounced?"
- Native speakers reporting TTS sounds "wrong"
- Latency spikes during review sessions (TTS generation lag)
- Storage costs exploding from TTS caching strategy
- Edge TTS rate limiting errors in logs

**Phase to address:**
- **Foundation**: Choose and validate TTS provider
- **MVP**: Implement smart caching strategy
- **Growth**: Add fallback provider and monitoring

---

### Pitfall 5: Learning Words in Isolation (No Context)

**What goes wrong:**
Users memorize Bulgarian words as decontextualized translations but can't use them in real conversation. They know "чета" means "read" but don't know:
- Which preposition it takes (different from English)
- Whether it's transitive/intransitive
- Common collocations ("чета книга" vs *"чета на книга")
- Register (formal/informal)

Bulgarian learners particularly struggle because:
- Word order differs from English
- Preposition usage is unpredictable from English
- Verbs have aspect pairs that affect usage patterns

**Why it happens:**
Context takes more effort to create than simple word-translation pairs. It's tempting to build a flashcard app where cards just show "Bulgarian word → English word." LLMs can generate sentences, but without curation, they produce:
- Artificially simple "textbook" sentences
- Examples that don't show the word's most common usage
- Grammatically correct but unnatural collocations

**How to avoid:**
1. **Make context mandatory**:
   - Every vocabulary entry requires example sentence(s)
   - Show word in 2-3 different contexts during learning
   - LLM-generated sentences flagged for human review

2. **Leverage your archived project learnings**:
   - Fill-in-the-blank didn't work → focus on recognition first
   - Show sentences with target word highlighted
   - Progressive reveal: sentence → translation → explanation

3. **Smart sentence selection**:
   - Prioritize sentences using vocabulary user already knows
   - Mark sentence difficulty based on grammar/vocabulary complexity
   - Include audio for every example sentence (TTS or native recordings)

**Warning signs:**
- Users can translate words but fail conversation practice
- High card retention but low confidence self-ratings
- Users requesting "how do I use this in a sentence?"
- Quiz mode shows recognition but not production

**Phase to address:**
- **Foundation**: Require sentence context in data model
- **MVP**: LLM sentence generation with review queue
- **Growth**: Sentence difficulty rating and adaptive selection

---

### Pitfall 6: Premature Gamification Obscures Learning Effectiveness

**What goes wrong:**
Users chase streaks, XP, and achievements instead of actual learning. App metrics look great (high DAU, long sessions) but users aren't retaining vocabulary. The app becomes a game that happens to have Bulgarian words in it, not a learning tool that's fun.

**Why it happens:**
Duolingo's success makes gamification seem mandatory. Developers add:
- Daily streaks (creates guilt, not motivation)
- Leaderboards (competitive anxiety for language learning)
- XP systems (optimized for quantity over quality)
- Achievements (unlock after X reviews, regardless of retention)

These can help, but only if the underlying learning loop is effective. Add them too early and you:
- Optimize for the wrong metrics
- Can't tell if features work because gamification confounds the data
- Create perverse incentives (users rushing through reviews)

**How to avoid:**
1. **Validate learning loop first**:
   - Launch MVP without gamification
   - Measure actual retention (7-day, 30-day recall tests)
   - Understand what drives engagement vs. what drives learning
   - Add gamification only after learning effectiveness is proven

2. **Use gamification to reinforce good behavior**:
   - Reward consistent study sessions, not just volume
   - Celebrate mastery (card graduation) over reviews completed
   - Make achievements about learning milestones ("10 verbs mastered")

3. **Avoid dark patterns**:
   - Don't guilt users about breaking streaks
   - Let users pause/resume without penalty
   - Make the learning intrinsically satisfying

**Warning signs:**
- High engagement but low retention scores
- Users completing 100 cards/day with <50% accuracy
- Complaints about "grinding" or "having to"
- Metrics improve but user language ability doesn't
- Can't isolate whether features work because gamification confounds A/B tests

**Phase to address:**
- **MVP**: Ship without gamification, focus on learning loop
- **Growth**: Add light gamification after validating retention
- **Polish**: Sophisticated motivation systems

---

### Pitfall 7: Ignoring Bulgarian's Lack of Infinitive

**What goes wrong:**
English learners expect "to read," "to write" dictionary forms. Bulgarian has no infinitive - verbs are cited in 1st person singular present tense ("чета" not "to read"). This creates confusion in:
- Dictionary lookups (users search for non-existent infinitives)
- Conjugation tables (which form is the "base"?)
- UI labels ("Verb: чета" vs "Verb: to read" - different paradigms)

Apps that ignore this ship English-centric designs that confuse Bulgarian learners.

**Why it happens:**
Most language learning app templates assume infinitive-having languages. Bulgarian's first-person citation form seems odd, so developers "normalize" it by inventing infinitives or using English translations as lemmas.

**How to avoid:**
1. **Embrace Bulgarian's structure**:
   - Use 1st person singular as lemma (чета, пиша, говоря)
   - UI: "Verb: чета (I read)" not "Verb: to read"
   - Dictionary search accepts both forms but returns 1st person

2. **Conjugation tables start from 1st person**:
   ```
   чета (I read)
   ├─ четеш (you read)
   ├─ чете (he/she reads)
   └─ ... etc
   ```

3. **Help English speakers transition**:
   - Glossary explaining citation forms
   - Search matches "to read" → "чета"
   - Tutorial on first use explaining why no infinitives

**Warning signs:**
- Users asking "where's the infinitive?"
- Bug reports: "verb conjugations wrong" (they're looking for infinitive row)
- Confusion in forums/feedback about verb forms
- Parallel issues with verbal nouns (четене)

**Phase to address:**
- **Foundation**: Data model uses 1st person lemmas
- **MVP**: UI clearly indicates citation form convention
- **Polish**: Smart search handles infinitive queries from English speakers

---

## Technical Debt Patterns

Shortcuts that seem reasonable but create long-term problems.

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Store inflections as comma-separated strings | Fast to implement, easy to display | Can't query morphological patterns, impossible to validate, breaks when you need relational data | Never - use JSONB or relational tables from day one |
| Skip TTS caching, generate on-the-fly | No storage costs, simpler architecture | Terrible latency during reviews, API rate limits, costs scale with usage | Only in prototype/demo - MVP must cache |
| Use LLM for everything including inflection tables | Rapid content generation, feels AI-native | Unreliable accuracy, inconsistent results, can't validate automatically | Use for generation, never for canonical data |
| Single-table vocabulary design (no lemma/form separation) | Simplest possible schema | Can't handle Bulgarian morphology, forces duplication, major migration pain later | Never for morphologically rich languages |
| Hard-code SRS algorithm instead of making it configurable | Faster initial development | Can't experiment with intervals, stuck when you need forgiveness logic | Only acceptable if using proven algorithm (SM-2) with known good parameters |
| Skip example sentences for MVP | Ship faster, less content to create | Users can't use words in context, learning effectiveness tanks | Only if you have plan to add pre-launch - never ship without |
| Store TTS audio as URLs only (no local cache) | Lower storage costs, simpler deployment | Vendor lock-in, no offline mode, broken audio if provider changes | Never - at minimum cache in CDN/object storage |

## Integration Gotchas

Common mistakes when connecting to external services.

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Ollama/LLM | Treating outputs as verified facts, no retry logic for failed calls | Wrap in validation layer, cache results, implement exponential backoff, version API calls |
| Edge TTS | No fallback when rate limited, assuming API will never change | Abstract behind interface, implement circuit breaker, cache aggressively, have backup provider |
| PostgreSQL | Using TEXT fields for structured morphological data instead of JSONB | Use JSONB for flexible metadata, enable GIN indexes, validate JSON structure with constraints |
| Spring Boot 4.2.x | Not leveraging virtual threads for I/O-bound operations (LLM calls, TTS generation) | Use virtual threads for all external API calls to prevent thread exhaustion |
| React State | Storing large vocabulary lists in component state causing re-render cascades | Use proper state management (Redux/Zustand), paginate/virtualize large lists, memoize selectively |

## Performance Traps

Patterns that work at small scale but fail as usage grows.

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Loading all user vocabulary on app start | Fast for first 50 words | App initialization takes >5s, memory bloat | >500 words (~2 months active use) |
| N+1 queries loading word forms with lemmas | Slow page loads in development | Use JPA fetch joins or entity graphs, batch queries | >100 words in active study |
| Synchronous LLM calls during review flow | Review sessions lag during API calls | Background processing, pre-generate for study queue, show cached data | First API hiccup or rate limit |
| Generating TTS every time word is reviewed | Latency spikes, API costs | Pre-generate for user's study queue, cache indefinitely, background warm-up | Day 1 - unacceptable from start |
| Full table scan for "words user needs to review today" | Works fine in testing | >1s query time, database CPU spikes | >5000 vocabulary entries |
| Frontend re-rendering entire word list on filter change | Smooth with small datasets | UI freezes, typing lag in search | >1000 words visible |

## Security Mistakes

Domain-specific security issues beyond general web security.

| Mistake | Risk | Prevention |
|---------|------|------------|
| Storing user vocabulary data without encryption | User study data leaked in breach exposes what they're learning (privacy) | Encrypt sensitive fields (study history, personal notes), use database-level encryption |
| No rate limiting on LLM-powered features | Abuse runs up Ollama compute costs or external API bills | Rate limit per user/IP, cap LLM requests per session, monitor for abuse patterns |
| Trusting client-side SRS calculations | Users hack their way to "completed" status without learning | Server-side SRS calculation, validate review results, detect impossible retention patterns |
| Allowing arbitrary text in example sentences | XSS via user-added vocabulary, inappropriate content | Sanitize all user input, content moderation for community features, escape HTML properly |
| No audit trail on vocabulary edits | Can't detect/revert LLM hallucinations that slip through | Log all changes with timestamps, version vocabulary entries, allow rollback |

## UX Pitfalls

Common user experience mistakes in this domain.

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Showing conjugation tables with all 9+ verb forms at once | Cognitive overload, especially for beginners | Progressive disclosure: show 1st person by default, expand to see full conjugation, highlight patterns |
| No feedback on why a card is due for review | Users don't understand SRS schedule | Show "Last reviewed: 3 days ago, Next: today" with visual timeline of card history |
| Masculine/feminine/neuter forms all shown equally | Users can't tell which is most important | Indicate frequency (показател - masculine is common, feminine rare), show usage examples |
| Review sessions with no progress indicator | Anxiety about session length, no sense of accomplishment | "15 of 40 cards" progress, option to stop after N cards, celebrate milestones |
| Bulgarian text too small or unclear font | Difficult to distinguish cyrillic characters (т vs m, н vs h) | Large, clear font optimized for Cyrillic, high contrast, option to increase size |
| English UI assumes user knows linguistic terms (perfective, definiteness) | Confusion about morphological features | Use plain language ("completed action" vs "perfective"), tooltips with examples |
| No way to mark "I know this word from elsewhere" | Must review words they already know to unlock new content | Bulk import, "mark as known" feature, placement test to skip basics |

## "Looks Done But Isn't" Checklist

Things that appear complete but are missing critical pieces.

- [ ] **Vocabulary entries:** Often missing morphological metadata (gender, aspect) - verify every entry has complete grammatical info, not just translation
- [ ] **Example sentences:** Often missing audio or using low-quality TTS - verify native speaker validation or premium TTS, check stress accuracy
- [ ] **SRS implementation:** Often using default SM-2 parameters tuned for English - verify algorithm tested with Bulgarian morphological complexity, forgiveness logic exists
- [ ] **LLM integration:** Often no validation pipeline - verify every LLM output goes through verification queue before shown to users
- [ ] **Aspect pairs:** Often incomplete (only perfective or only imperfective) - verify both aspects present with usage guidance
- [ ] **Search functionality:** Often only matches exact forms - verify stemming/lemmatization so "четеш" finds "чета"
- [ ] **Offline mode:** Often claims offline but TTS breaks - verify cached audio works without network, degraded but functional experience
- [ ] **Performance testing:** Often tested with <100 words - verify realistic data (5000+ words, 100+ daily reviews, concurrent users)

## Recovery Strategies

When pitfalls occur despite prevention, how to recover.

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Flat string inflections discovered in production | HIGH | Create migration script to parse strings → relational tables, rebuild indexes, add constraints, validate all data, may require manual cleanup |
| LLM hallucinations in user decks | MEDIUM | Build review queue showing LLM-generated content by confidence score, crowdsource validation, flag entries for expert review, notify affected users |
| Users overwhelmed by review backlog | LOW | Implement catch-up mode retroactively, send "we've improved review scheduling" email, offer one-time "reset intervals" option, gentle re-onboarding |
| TTS provider shutdown/changed | MEDIUM | Activate backup provider, re-generate cached audio in background, notify users of temporary quality change, prioritize common words first |
| Morphological data missing gender/aspect | MEDIUM | Run batch analysis with LLM to fill gaps, flag for human verification, update schema to require fields, prevent future gaps |
| Performance degradation from poor queries | LOW | Add database indexes, implement caching layer, optimize N+1 queries, may need zero-downtime migration for index creation |
| Users learning from wrong translations | HIGH | Identify scope of problem, contact affected users, offer re-study option, add verification process, may damage user trust |

## Pitfall-to-Phase Mapping

How roadmap phases should address these pitfalls.

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Morphological data stored as flat strings | Foundation/Data Model | Schema review: separate lemma and forms tables exist with proper FKs and JSONB metadata |
| LLM hallucinations as ground truth | Foundation + MVP | Every LLM output goes to verification queue; spot-check 20 entries for accuracy |
| SRS breaks with inconsistency | MVP | Test user scenarios: miss 3 days, 7 days, 30 days - verify catch-up works and doesn't overwhelm |
| TTS quality gaps | Foundation | Native speaker validation of 100 common words; latency <200ms for cached; >95% cache hit rate |
| Learning words in isolation | MVP | Every vocabulary entry has 2+ example sentences; user testing shows improved context understanding |
| Premature gamification | Growth (post-MVP) | MVP ships without streaks/XP; retention metrics validated before adding gamification |
| Ignoring lack of infinitive | Foundation | Data model uses 1st person lemmas; UI shows "чета (I read)" not "to read"; search handles both |
| Poor query performance | Growth | Load testing with 5000+ words shows <200ms query times; monitoring shows no N+1 queries |
| No user feedback on errors | MVP | Feedback mechanism exists; review queue processes reports; response time <48hrs |
| Offline mode incomplete | Polish | Airplane mode testing shows reviews work; graceful degradation when TTS cache incomplete |

## Sources

### Language Learning Apps - General Pitfalls
- [2025's language learning lessons](https://www.lingoda.com/blog/en/2025-language-learning-lessons/) - Tool overload and AI over-reliance
- [7 Major Disadvantages of Language Apps](https://www.studyfrenchspanish.com/disadvantages-of-language-apps/) - Lack of consistency and conversation practice
- [10 Common Language Learning Mistakes](https://ilang.io/blog/10-common-language-learning-mistakes) - Avoiding speaking practice

### Vocabulary Learning Mistakes
- [13 commonly made mistakes in vocabulary instruction](https://gianfrancoconti.com/2016/02/06/10-commonly-made-mistakes-in-vocabulary-instruction/) - Shallow vs. deep processing, oral vs. written presentation
- [Vocabulary Learning: A Critical Analysis](https://teslcanadajournal.ca/index.php/tesl/article/view/566) - Technical analysis of vocabulary techniques

### Morphologically Rich Languages
- [Why do language models perform worse for morphologically complex languages?](https://arxiv.org/html/2411.14198v1) - Data sparsity and vocabulary issues
- [Processing Morphologically Rich Languages](https://direct.mit.edu/coli/article/39/1/15/1428/Parsing-Morphologically-Rich-Languages) - Parsing and segmentation challenges
- [Developing a Hybrid Morphological Analyzer](https://www.mdpi.com/2076-3417/15/10/5682) - Neural models and data requirements

### LLM Integration in Education
- [Large language models in education: systematic review](https://www.sciencedirect.com/science/article/pii/S2666920X25001699) - Hallucination and over-reliance issues
- [AI and Large Language Models: shortcomings](https://www.strategian.com/2025/12/08/ai-and-large-language-models-shortcomings-and-mistakes/) - Technical accuracy concerns
- [LLM Agents for Education](https://aclanthology.org/2025.findings-emnlp.743.pdf) - Ethical issues and integration challenges

### Spaced Repetition Systems
- [Spaced Repetition Guide: How to Use It and Avoid Common Mistakes](https://trustwrites.com/en/education-srs-en/) - Inconsistent study habits and poor card design
- [Spaced repetition memory system](https://notes.andymatuschak.org/Spaced_repetition_memory_system) - Meaningless goals and system limitations
- [Master Language Learning with Spaced Repetition](https://migaku.com/blog/language-fun/spaced-repetition-language-learning) - Application limits

### Anki-Specific Issues
- [Effectively Using Anki Flashcards](https://louisrli.github.io/blog/2023/06/13/effectively-using-anki-flashcards-for-language-learning/) - Recognition vs. production, learning in isolation
- [Best Practices for Vocabulary with Anki](https://languagecrush.com/forum/t/3436) - Pre-made deck issues, card organization
- [How to Use Anki for Language Learning](https://jacoblaguerre.com/language-learning/how-to-use-anki-for-language-learning/) - Overloading study sessions

### Text-to-Speech
- [The Best Text-to-Speech Apps for Language Learning](https://preply.com/en/blog/the-best-text-to-speech-apps/) - Quality and natural-sounding voices
- [Best Text-to-Speech APIs in 2025](https://www.edenai.co/post/best-text-to-speech-apis) - Provider comparison and features

### UX in Language Learning Apps
- [Language Learning App Design: UX](https://www.psd-dude.com/tutorials/resources/user-interface-design.aspx) - Interface design principles
- [UX Case Study: Duolingo](https://usabilitygeek.com/ux-case-study-duolingo/) - Gamification and motivation
- [LingQ: Language Learning App Usability](https://uxplanet.org/lingq-language-learning-app-usability-3e6f08f3fc50) - Poor usability patterns
- [LINGO: Solving Language Learners Problems](https://medium.com/@tazimimran/lingo-a-language-learning-app-376c76c34e5c) - Comprehensive learning coverage issues

### Linguistic Data Modeling
- [LemmInflect GitHub](https://github.com/bjascob/LemmInflect) - Lemmatization and inflection modeling
- [The LiLa Lemma Bank](https://openhumanitiesdata.metajnl.com/articles/10.5334/johd.145) - Knowledge base structure for canonical forms

---
*Pitfalls research for: Bulgarian Vocabulary Learning Application*
*Researched: 2026-02-15*
