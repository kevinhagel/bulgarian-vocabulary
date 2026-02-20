package com.vocab.bulgarian.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Inflection entity representing an inflected form of a lemma.
 * Maps to the 'inflections' table with ManyToOne relationship back to Lemma.
 */
@Entity
@Table(name = "inflections")
public class Inflection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String form;

    @Column(name = "grammatical_info", length = 100)
    private String grammaticalInfo;

    @Column(name = "difficulty_level", length = 20)
    private String difficultyLevel;

    @Column(name = "accented_form", length = 120)
    private String accentedForm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Lifecycle callback
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // equals and hashCode based on id (null-safe for new entities)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inflection that = (Inflection) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getGrammaticalInfo() {
        return grammaticalInfo;
    }

    public void setGrammaticalInfo(String grammaticalInfo) {
        this.grammaticalInfo = grammaticalInfo;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getAccentedForm() {
        return accentedForm;
    }

    public void setAccentedForm(String accentedForm) {
        this.accentedForm = accentedForm;
    }

    public Lemma getLemma() {
        return lemma;
    }

    public void setLemma(Lemma lemma) {
        this.lemma = lemma;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
