CREATE TABLE credential (
    id            BYTEA        NOT NULL PRIMARY KEY,
    username      VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    user_id       BYTEA        NOT NULL UNIQUE
);
