CREATE TABLE gesture_training_samples (
    id BIGSERIAL PRIMARY KEY,
    created TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    label VARCHAR(96) NOT NULL,
    landmarks JSONB NOT NULL,
    CONSTRAINT chk_gesture_landmarks
        CHECK (jsonb_typeof(landmarks) = 'array' AND jsonb_array_length(landmarks) = 63)
);

CREATE INDEX idx_gesture_training_user ON gesture_training_samples (user_id);
CREATE INDEX idx_gesture_training_user_label ON gesture_training_samples (user_id, label);

CREATE TABLE translator_samples (
    id BIGSERIAL PRIMARY KEY,
    created TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    inferred_label VARCHAR(96) NOT NULL
);

CREATE INDEX idx_translator_samples_user ON translator_samples (user_id);
