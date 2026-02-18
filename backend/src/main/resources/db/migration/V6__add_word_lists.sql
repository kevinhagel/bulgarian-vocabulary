-- Named word lists for organizing vocabulary into study sets
CREATE TABLE word_lists (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Many-to-many: a lemma can be in multiple lists, a list can have many lemmas
CREATE TABLE word_list_members (
    list_id   BIGINT NOT NULL REFERENCES word_lists(id) ON DELETE CASCADE,
    lemma_id  BIGINT NOT NULL REFERENCES lemmas(id) ON DELETE CASCADE,
    added_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (list_id, lemma_id)
);

CREATE INDEX idx_word_list_members_lemma ON word_list_members(lemma_id);
CREATE INDEX idx_word_list_members_list  ON word_list_members(list_id);
