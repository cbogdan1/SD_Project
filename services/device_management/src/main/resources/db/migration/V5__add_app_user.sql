-- Local copy of the user ids from user-service (only the id, nothing else).
-- user-service adds a row when a user is created and removes it when the user is deleted.
CREATE TABLE app_user (
    id BYTEA NOT NULL PRIMARY KEY
);

-- The users that already own seed devices must exist here before the foreign key is added.
INSERT INTO app_user (id)
SELECT DISTINCT user_id FROM device WHERE user_id IS NOT NULL;

-- One user has many devices. When a user is deleted from app_user,
-- the database itself sets user_id = NULL on that user's devices (they become unassigned).
ALTER TABLE device
    ADD CONSTRAINT fk_device_app_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE SET NULL;
