CREATE TABLE device (
    id                     BYTEA            NOT NULL PRIMARY KEY,
    description            VARCHAR(255)     NOT NULL,
    address                VARCHAR(255)     NOT NULL,
    max_energy_consumption DOUBLE PRECISION NOT NULL
);
