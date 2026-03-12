package com.vocab.bulgarian.dictionary.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "dictionary_words")
public class DictionaryWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private String pos;

    @Column(name = "primary_translation")
    private String primaryTranslation;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "alternate_meanings", columnDefinition = "TEXT[]")
    private String[] alternateMeanings;

    private String ipa;

    @Column(name = "raw_data", nullable = false, columnDefinition = "JSONB")
    private String rawData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "dictionaryWord", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DictionaryForm> forms = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DictionaryWord that = (DictionaryWord) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getPos() { return pos; }
    public void setPos(String pos) { this.pos = pos; }

    public String getPrimaryTranslation() { return primaryTranslation; }
    public void setPrimaryTranslation(String primaryTranslation) { this.primaryTranslation = primaryTranslation; }

    public String[] getAlternateMeanings() { return alternateMeanings; }
    public void setAlternateMeanings(String[] alternateMeanings) { this.alternateMeanings = alternateMeanings; }

    public String getIpa() { return ipa; }
    public void setIpa(String ipa) { this.ipa = ipa; }

    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public List<DictionaryForm> getForms() { return forms; }
    public void setForms(List<DictionaryForm> forms) { this.forms = forms; }

    public void addForm(DictionaryForm form) {
        forms.add(form);
        form.setDictionaryWord(this);
    }
}
