package com.vocab.bulgarian.llm.validation;

import com.vocab.bulgarian.domain.enums.DifficultyLevel;
import com.vocab.bulgarian.domain.enums.PartOfSpeech;
import com.vocab.bulgarian.llm.dto.InflectionSet;
import com.vocab.bulgarian.llm.dto.LemmaDetectionResponse;
import com.vocab.bulgarian.llm.dto.LemmaMetadata;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates LLM output DTOs for schema compliance, Cyrillic content, and Bulgarian morphology rules.
 */
@Component
public class LlmOutputValidator {

    private final Validator validator;

    public LlmOutputValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates lemma detection response.
     * Checks schema, Cyrillic content, and field constraints.
     *
     * @param response the lemma detection response from LLM
     * @throws LlmValidationException if validation fails
     */
    public void validateLemmaDetection(LemmaDetectionResponse response) {
        if (response == null) {
            throw new LlmValidationException("LemmaDetectionResponse is null");
        }

        // If detection failed, this is expected for fallbacks -- don't validate further
        if (response.detectionFailed()) {
            return;
        }

        // Run Jakarta Bean Validation
        Set<ConstraintViolation<LemmaDetectionResponse>> violations = validator.validate(response);
        if (!violations.isEmpty()) {
            String violationMessages = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
            throw new LlmValidationException("Bean validation failed: " + violationMessages);
        }

        // Validate lemma field
        String lemma = response.lemma();
        if (lemma == null || lemma.isBlank()) {
            throw new LlmValidationException("Lemma is blank");
        }

        if (!containsCyrillic(lemma)) {
            throw new LlmValidationException("Lemma does not contain Cyrillic characters: " + lemma);
        }

        if (lemma.length() > 100) {
            throw new LlmValidationException("Lemma exceeds maximum length of 100 characters: " + lemma.length());
        }
    }

    /**
     * Validates inflection set response.
     * Checks schema, Cyrillic content, duplicates, and minimum inflection counts by part of speech.
     *
     * @param set the inflection set from LLM
     * @throws LlmValidationException if validation fails
     */
    public void validateInflectionSet(InflectionSet set) {
        if (set == null) {
            throw new LlmValidationException("InflectionSet is null");
        }

        // Run Jakarta Bean Validation
        Set<ConstraintViolation<InflectionSet>> violations = validator.validate(set);
        if (!violations.isEmpty()) {
            String violationMessages = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
            throw new LlmValidationException("Bean validation failed: " + violationMessages);
        }

        // Check inflections list is not empty
        if (set.inflections() == null || set.inflections().isEmpty()) {
            throw new LlmValidationException("Inflections list is empty");
        }

        // Validate each inflection entry
        Set<String> seenTexts = new HashSet<>();
        for (InflectionSet.InflectionEntry entry : set.inflections()) {
            if (entry.text() == null || entry.text().isBlank()) {
                throw new LlmValidationException("Inflection text is blank");
            }

            if (!containsCyrillic(entry.text())) {
                throw new LlmValidationException("Inflection does not contain Cyrillic characters: " + entry.text());
            }

            // Check for duplicates
            if (!seenTexts.add(entry.text())) {
                throw new LlmValidationException("Duplicate inflection text found: " + entry.text());
            }
        }

        // Validate minimum inflection count by part of speech
        int inflectionCount = set.inflections().size();
        String partOfSpeech = set.partOfSpeech();
        int minimumRequired = getMinimumInflectionCount(partOfSpeech);

        if (inflectionCount < minimumRequired) {
            throw new LlmValidationException(
                String.format("Insufficient inflections for %s: found %d, minimum %d required",
                    partOfSpeech, inflectionCount, minimumRequired)
            );
        }
    }

    /**
     * Validates lemma metadata response.
     * Checks schema, enum values for part of speech and difficulty level.
     *
     * @param metadata the metadata from LLM
     * @throws LlmValidationException if validation fails
     */
    public void validateLemmaMetadata(LemmaMetadata metadata) {
        if (metadata == null) {
            throw new LlmValidationException("LemmaMetadata is null");
        }

        // Run Jakarta Bean Validation
        Set<ConstraintViolation<LemmaMetadata>> violations = validator.validate(metadata);
        if (!violations.isEmpty()) {
            String violationMessages = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
            throw new LlmValidationException("Bean validation failed: " + violationMessages);
        }

        // Validate part of speech matches enum
        if (!isValidPartOfSpeech(metadata.partOfSpeech())) {
            throw new LlmValidationException(
                "Invalid part of speech: " + metadata.partOfSpeech() +
                " (must match PartOfSpeech enum)"
            );
        }

        // Validate difficulty level matches enum
        if (!isValidDifficultyLevel(metadata.difficultyLevel())) {
            throw new LlmValidationException(
                "Invalid difficulty level: " + metadata.difficultyLevel() +
                " (must match DifficultyLevel enum)"
            );
        }

        // Category is optional (can be null/blank)
    }

    /**
     * Checks if text contains at least one Cyrillic character.
     */
    private boolean containsCyrillic(String text) {
        if (text == null) {
            return false;
        }
        return text.matches(".*[а-яА-Я].*");
    }

    /**
     * Determines minimum inflection count by part of speech based on Bulgarian morphology.
     */
    private int getMinimumInflectionCount(String partOfSpeech) {
        if (partOfSpeech == null) {
            return 1;
        }

        return switch (partOfSpeech.toUpperCase()) {
            case "VERB" -> 6;        // At least 6 person/number forms for present tense
            case "NOUN" -> 2;        // At least singular, plural
            case "ADJECTIVE" -> 3;   // At least masculine, feminine, neuter
            default -> 1;            // Other parts of speech
        };
    }

    /**
     * Validates that the part of speech string matches a PartOfSpeech enum value (case-insensitive).
     */
    private boolean isValidPartOfSpeech(String pos) {
        if (pos == null || pos.isBlank()) {
            return false;
        }

        try {
            PartOfSpeech.valueOf(pos.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates that the difficulty level string matches a DifficultyLevel enum value (case-insensitive).
     */
    private boolean isValidDifficultyLevel(String level) {
        if (level == null || level.isBlank()) {
            return false;
        }

        try {
            DifficultyLevel.valueOf(level.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
