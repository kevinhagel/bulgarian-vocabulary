package com.vocab.bulgarian.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Example sentence demonstrating natural Bulgarian usage of a lemma.
 * Maps to the 'example_sentences' table with ManyToOne relationship back to Lemma.
 */
@Entity
@Table(name = "example_sentences")
public class ExampleSentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "bulgarian_text", nullable = false, columnDefinition = "TEXT")
    private String bulgarianText;

    @NotNull
    @Column(name = "english_translation", nullable = false, columnDefinition = "TEXT")
    private String englishTranslation;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExampleSentence that = (ExampleSentence) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBulgarianText() { return bulgarianText; }
    public void setBulgarianText(String bulgarianText) { this.bulgarianText = bulgarianText; }

    public String getEnglishTranslation() { return englishTranslation; }
    public void setEnglishTranslation(String englishTranslation) { this.englishTranslation = englishTranslation; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public Lemma getLemma() { return lemma; }
    public void setLemma(Lemma lemma) { this.lemma = lemma; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
