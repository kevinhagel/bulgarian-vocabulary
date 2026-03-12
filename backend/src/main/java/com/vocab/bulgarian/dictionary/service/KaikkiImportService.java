package com.vocab.bulgarian.dictionary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class KaikkiImportService {

    private static final Logger log = LoggerFactory.getLogger(KaikkiImportService.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public KaikkiImportService(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportResult importFromJsonl(Path jsonlPath) throws IOException {
        log.info("Starting Kaikki import from {}", jsonlPath);

        int wordCount = 0;
        int formCount = 0;
        int skipped = 0;
        int errors = 0;

        try (BufferedReader reader = Files.newBufferedReader(jsonlPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    JsonNode entry = objectMapper.readTree(line);

                    if (isFormOfEntry(entry)) {
                        skipped++;
                        continue;
                    }

                    String word = entry.has("word") ? entry.get("word").asText() : null;
                    String pos = entry.has("pos") ? entry.get("pos").asText() : null;
                    if (word == null || pos == null) {
                        skipped++;
                        continue;
                    }

                    String plainWord = DictionaryService.stripAccents(word);

                    // Extract translations
                    String primaryTranslation = null;
                    String[] alternateMeanings = null;
                    JsonNode senses = entry.get("senses");
                    if (senses != null && senses.isArray() && !senses.isEmpty()) {
                        List<String> allGlosses = new ArrayList<>();
                        for (JsonNode sense : senses) {
                            JsonNode glosses = sense.get("glosses");
                            if (glosses != null && glosses.isArray()) {
                                for (JsonNode g : glosses) {
                                    allGlosses.add(g.asText());
                                }
                            }
                        }
                        if (!allGlosses.isEmpty()) {
                            primaryTranslation = allGlosses.getFirst();
                            if (allGlosses.size() > 1) {
                                alternateMeanings = allGlosses.subList(1, allGlosses.size()).toArray(String[]::new);
                            }
                        }
                    }

                    // Extract IPA
                    String ipa = null;
                    JsonNode sounds = entry.get("sounds");
                    if (sounds != null && sounds.isArray()) {
                        for (JsonNode sound : sounds) {
                            if (sound.has("ipa")) {
                                ipa = sound.get("ipa").asText();
                                break;
                            }
                        }
                    }

                    // Insert word using native SQL with RETURNING
                    var keyHolder = new GeneratedKeyHolder();
                    var wordParams = new MapSqlParameterSource()
                        .addValue("word", plainWord)
                        .addValue("pos", pos)
                        .addValue("translation", primaryTranslation)
                        .addValue("ipa", ipa)
                        .addValue("rawData", line);

                    // Handle TEXT[] for alternate_meanings via JDBC
                    String altMeaningsLiteral = alternateMeanings != null
                        ? "{" + String.join(",", java.util.Arrays.stream(alternateMeanings)
                            .map(s -> "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                            .toArray(String[]::new)) + "}"
                        : null;
                    wordParams.addValue("altMeanings", altMeaningsLiteral);

                    jdbcTemplate.update(
                        "INSERT INTO dictionary_words (word, pos, primary_translation, alternate_meanings, ipa, raw_data, created_at) " +
                        "VALUES (:word, :pos, :translation, :altMeanings::TEXT[], :ipa, :rawData::jsonb, NOW())",
                        wordParams, keyHolder, new String[]{"id"}
                    );

                    Long wordId = keyHolder.getKey().longValue();

                    // Parse and insert forms
                    JsonNode formsNode = entry.get("forms");
                    if (formsNode != null && formsNode.isArray()) {
                        for (JsonNode formNode : formsNode) {
                            String form = formNode.has("form") ? formNode.get("form").asText() : null;
                            if (form == null || "-".equals(form) || "none".equals(form)) continue;

                            JsonNode tagsNode = formNode.get("tags");
                            if (tagsNode == null || !tagsNode.isArray()) continue;

                            List<String> tagList = new ArrayList<>();
                            for (JsonNode t : tagsNode) {
                                tagList.add(t.asText());
                            }

                            if (tagList.contains("romanization") || tagList.contains("table-tags")
                                    || tagList.contains("inflection-template")) {
                                continue;
                            }

                            String plainForm = DictionaryService.stripAccents(form);
                            String romanization = formNode.has("roman") ? formNode.get("roman").asText() : null;

                            String tagsLiteral = "{" + String.join(",",
                                tagList.stream()
                                    .map(s -> "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                                    .toArray(String[]::new)) + "}";

                            var formParams = new MapSqlParameterSource()
                                .addValue("wordId", wordId)
                                .addValue("form", form)
                                .addValue("plainForm", plainForm)
                                .addValue("tags", tagsLiteral)
                                .addValue("accentedForm", form)
                                .addValue("romanization", romanization);

                            jdbcTemplate.update(
                                "INSERT INTO dictionary_forms (word_id, form, plain_form, tags, accented_form, romanization) " +
                                "VALUES (:wordId, :form, :plainForm, :tags::TEXT[], :accentedForm, :romanization)",
                                formParams
                            );
                            formCount++;
                        }
                    }

                    wordCount++;
                    if (wordCount % 5000 == 0) {
                        log.info("Imported {} words, {} forms so far...", wordCount, formCount);
                    }
                } catch (Exception e) {
                    errors++;
                    if (errors <= 10) {
                        log.warn("Error parsing line: {}", e.getMessage());
                    }
                }
            }
        }

        log.info("Kaikki import complete: {} words, {} forms, {} skipped, {} errors",
                wordCount, formCount, skipped, errors);

        return new ImportResult(wordCount, formCount, skipped, errors);
    }

    private boolean isFormOfEntry(JsonNode entry) {
        JsonNode senses = entry.get("senses");
        if (senses == null || !senses.isArray() || senses.isEmpty()) return false;

        for (JsonNode sense : senses) {
            JsonNode tags = sense.get("tags");
            boolean hasFormOf = false;
            if (tags != null && tags.isArray()) {
                for (JsonNode tag : tags) {
                    if ("form-of".equals(tag.asText())) {
                        hasFormOf = true;
                        break;
                    }
                }
            }
            if (!hasFormOf) return false;
        }
        return true;
    }

    public record ImportResult(int wordCount, int formCount, int skipped, int errors) {}
}
