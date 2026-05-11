CREATE TABLE samples (
    id BIGSERIAL PRIMARY KEY,
    created TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    label VARCHAR(80) NOT NULL,
    handedness VARCHAR(20) NOT NULL,
    landmarks JSONB NOT NULL,
    device_info JSONB,
    submitted_by VARCHAR(180)
);

CREATE INDEX idx_samples_label ON samples (label);
CREATE INDEX idx_samples_created ON samples (created);

CREATE TABLE model (
    id BIGINT PRIMARY KEY,
    training_state VARCHAR(20) NOT NULL,
    artifact_url VARCHAR(500),
    metrics JSONB,
    trained_at TIMESTAMPTZ,
    error_message TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO model (id, training_state) VALUES (1, 'IDLE');
