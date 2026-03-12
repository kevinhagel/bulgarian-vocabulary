package com.vocab.bulgarian.dictionary.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Objects;

@Entity
@Table(name = "dictionary_forms")
public class DictionaryForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private DictionaryWord dictionaryWord;

    @Column(nullable = false)
    private String form;

    @Column(name = "plain_form", nullable = false)
    private String plainForm;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]", nullable = false)
    private String[] tags;

    @Column(name = "accented_form")
    private String accentedForm;

    private String romanization;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DictionaryForm that = (DictionaryForm) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DictionaryWord getDictionaryWord() { return dictionaryWord; }
    public void setDictionaryWord(DictionaryWord dictionaryWord) { this.dictionaryWord = dictionaryWord; }

    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }

    public String getPlainForm() { return plainForm; }
    public void setPlainForm(String plainForm) { this.plainForm = plainForm; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }

    public String getAccentedForm() { return accentedForm; }
    public void setAccentedForm(String accentedForm) { this.accentedForm = accentedForm; }

    public String getRomanization() { return romanization; }
    public void setRomanization(String romanization) { this.romanization = romanization; }
}
