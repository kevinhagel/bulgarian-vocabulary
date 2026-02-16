package com.vocab.bulgarian.llm.validation;

/**
 * Exception thrown when LLM output validation fails.
 * Used to signal schema violations, missing Cyrillic content, duplicates, or Bulgarian morphology rule violations.
 */
public class LlmValidationException extends RuntimeException {

    public LlmValidationException(String message) {
        super(message);
    }

    public LlmValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
