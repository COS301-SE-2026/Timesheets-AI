ALTER TABLE users
    ADD COLUMN email_verified  BOOLEAN  NOT NULL DEFAULT false,
    ADD COLUMN login_attempts  INTEGER  NOT NULL DEFAULT 0;

UPDATE users SET email_verified = true;

CREATE TABLE email_verification_tokens (
    token       VARCHAR(255) PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id),
    expires_at  TIMESTAMP    NOT NULL,
    verified    BOOLEAN      NOT NULL DEFAULT false
);

CREATE TABLE password_reset_tokens (
    token       VARCHAR(255) PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id),
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT false
);