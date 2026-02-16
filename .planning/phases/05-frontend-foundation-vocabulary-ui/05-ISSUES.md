# Phase 5 Known Issues

## Critical: LLM Processing Speed (Deferred to Phase 6)

**Issue:** Vocabulary creation takes 60-90 seconds due to synchronous LLM processing.

**Impact:**
- User must wait for lemma detection, inflection generation, and metadata
- Cannot add multiple words quickly
- Poor UX for vocabulary entry

**Root Cause:**
- BgGPT (9B model) is accurate for Bulgarian but slow (60-90s)
- qwen2.5:7b is fast (33s) but inaccurate (failed lemma detection test)
- Synchronous processing blocks user interaction
- Design requires manual English translation entry

**Attempted Solutions:**
1. ✓ GPU optimization (num-gpu, num-ctx tuning) - helped but not enough
2. ✗ Switch to smaller model (qwen2.5:7b) - faster but quality issues
3. ✗ Further GPU tuning - M4 Max already fully utilized

**Proper Solution (Phase 6):**

Implement background processing pipeline:

```
User enters Bulgarian word → Save immediately (< 1s)
                           ↓
                    Background Pipeline:
                    1. Lemma detection (rule-based or fast LLM)
                    2. Google Translate (< 1s)
                    3. BgGPT inflections (30-60s, doesn't block UI)
                    4. Metadata generation
                           ↓
                    Update entry when complete
```

**Benefits:**
- User waits 0 seconds
- No manual translation needed
- Can add 10 words in 30 seconds
- Accurate Bulgarian processing (BgGPT)
- Fast translation (Google Translate API)

**Effort:** 1-2 hours
- Add ProcessingStatus enum and field
- Make translation nullable
- Database migration
- Refactor VocabularyService
- Add Google Translate integration
- Frontend polling/WebSocket for updates

**Test Results:**
- BgGPT: 60-90s, accurate (плете → плета ✓)
- qwen2.5:7b: 33s, inaccurate (плете → плете ✗)

---

## Resolved Issues

### ✓ Cyrillic Font Rendering
**Issue:** Bulgarian text displayed with wrong glyphs (д→g, б→6, з→3)

**Root Cause:** System fonts rendering Cyrillic in cursive style, not print style

**Solution:** Switched to Inter font (Google Fonts) for print-style Cyrillic

**Fixed:** 2026-02-16

### ✓ Duplicate Inflection Validation
**Issue:** LLM-generated inflections rejected due to duplicate forms

**Root Cause:** Validator didn't allow same inflection text with different grammar (valid in Bulgarian)

**Solution:** Removed duplicate check in LlmOutputValidator

**Fixed:** 2026-02-16

---

## Hardware Context

**Mac Studio M4 Max:**
- 32 GPU cores (Metal 4)
- 14 CPU cores
- Ollama using 28GB VRAM for BgGPT model
- GPU fully utilized, speed issue is model size not hardware

**Current LLM:**
- Model: todorov/bggpt:9B-IT-v1.0.Q4_K_M
- Size: 36GB (28GB in VRAM)
- Quantization: Q4_K_M (4-bit)
- Bulgarian-specific, accurate morphology

**Alternative tested:**
- Model: qwen2.5:7b
- Faster but less accurate for Bulgarian
- Not suitable for production use

---

*Created: 2026-02-16*
*Status: Deferred to Phase 6*
