COMMENT ON COLUMN sign_sample.frames IS 'Clip JSON (heavy). Prefer sign_sample_summary for listing; fetch frames by id only when needed.';

CREATE OR REPLACE VIEW sign_sample_summary AS
SELECT
    id,
    sign_id,
    created_at,
    duration_ms,
    frame_count,
    two_hand_frame_ratio
FROM sign_sample;
