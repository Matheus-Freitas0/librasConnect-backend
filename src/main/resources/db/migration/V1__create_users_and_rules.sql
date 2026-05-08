CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    created TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE user_rules (
    user_id BIGINT NOT NULL,
    rule VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_rules PRIMARY KEY (user_id, rule),
    CONSTRAINT fk_user_rules_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_rules_user_id ON user_rules (user_id);
