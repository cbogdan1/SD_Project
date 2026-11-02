-- Every device must have a name. The existing (seed) devices take their description as name.
ALTER TABLE device ADD COLUMN name VARCHAR(255);
UPDATE device SET name = description;
ALTER TABLE device ALTER COLUMN name SET NOT NULL;
