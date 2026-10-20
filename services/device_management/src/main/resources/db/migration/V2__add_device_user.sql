ALTER TABLE device ADD COLUMN user_id BYTEA;
CREATE INDEX idx_device_user_id ON device (user_id);
