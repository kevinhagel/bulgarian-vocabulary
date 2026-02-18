package com.vocab.bulgarian.api.dto;

/**
 * Request body for the reprocess endpoint.
 * The hint (optional) is appended to the lemma's notes so the LLM
 * has better context for disambiguating homographs on re-processing.
 */
public record ReprocessRequestDTO(String hint) {}
