-- Test logins for the sample persons from user-service V2__insert_sample_persons.sql (same user ids).
-- Passwords (BCrypt-hashed): ion / ion123, maria / maria123. Test data only.
INSERT INTO credential (id, username, password_hash, role, user_id)
VALUES
    (decode('c1aabc999c0b4ef8bb6d6bb9bd380a11', 'hex'), 'ion',
     '$2a$10$AxXCDaanas9zlpwGcLsfJOQhg5bhEU0LDBfzJ15FP5mJy93coMmhu', 'CLIENT',
     decode('a0eebc999c0b4ef8bb6d6bb9bd380a11', 'hex')),
    (decode('c2aabc999c0b4ef8bb6d6bb9bd380a22', 'hex'), 'maria',
     '$2a$10$CgDda4p/llVkiZPyOtY3/u.J.2ihSx5ioAnQrceOtmCYXu/qicE1a', 'CLIENT',
     decode('b1eebc999c0b4ef8bb6d6bb9bd380a22', 'hex'));
