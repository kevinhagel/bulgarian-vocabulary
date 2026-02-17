-- SRS state: one row per studied USER_ENTERED lemma
CREATE TABLE srs_state (
    id                BIGSERIAL PRIMARY KEY,
    lemma_id          BIGINT NOT NULL UNIQUE REFERENCES lemmas(id) ON DELETE CASCADE,
    ease_factor       DECIMAL(4,2) NOT NULL DEFAULT 2.50,
    interval_days     INT NOT NULL DEFAULT 0,
    repetition_count  INT NOT NULL DEFAULT 0,
    next_review_date  DATE NOT NULL DEFAULT CURRENT_DATE,
    last_reviewed_at  TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_srs_state_lemma_id ON srs_state(lemma_id);
CREATE INDEX idx_srs_state_next_review ON srs_state(next_review_date);

-- Study sessions
CREATE TABLE study_sessions (
    id              BIGSERIAL PRIMARY KEY,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    card_count      INT NOT NULL DEFAULT 0,
    cards_reviewed  INT NOT NULL DEFAULT 0,
    correct_count   INT NOT NULL DEFAULT 0,
    started_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ended_at        TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_session_status CHECK (status IN ('ACTIVE','COMPLETED','ABANDONED'))
);

-- Session cards: ordered list of lemma IDs for this session
CREATE TABLE session_cards (
    session_id  BIGINT NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
    lemma_id    BIGINT NOT NULL REFERENCES lemmas(id) ON DELETE CASCADE,
    position    INT NOT NULL,
    reviewed    BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (session_id, lemma_id)
);

CREATE INDEX idx_session_cards_session_position ON session_cards(session_id, position);

-- Study reviews: one row per card rating event
CREATE TABLE study_reviews (
    id           BIGSERIAL PRIMARY KEY,
    session_id   BIGINT NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
    lemma_id     BIGINT NOT NULL REFERENCES lemmas(id) ON DELETE CASCADE,
    rating       VARCHAR(20) NOT NULL,
    reviewed_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_review_rating CHECK (rating IN ('CORRECT','INCORRECT'))
);

CREATE INDEX idx_study_reviews_lemma_id ON study_reviews(lemma_id);
CREATE INDEX idx_study_reviews_session_id ON study_reviews(session_id);
