-- Every user has a username (the same one used to log in, which lives in auth-service).
ALTER TABLE person ADD COLUMN username VARCHAR(255);

-- The seed persons get the usernames of their seed logins (auth-service V2__insert_sample_credentials.sql).
UPDATE person SET username = 'ion' WHERE id = decode('a0eebc999c0b4ef8bb6d6bb9bd380a11', 'hex');
UPDATE person SET username = 'maria' WHERE id = decode('b1eebc999c0b4ef8bb6d6bb9bd380a22', 'hex');
-- Any other older row gets its name, so the column can become NOT NULL.
UPDATE person SET username = name WHERE username IS NULL;

ALTER TABLE person ALTER COLUMN username SET NOT NULL;
