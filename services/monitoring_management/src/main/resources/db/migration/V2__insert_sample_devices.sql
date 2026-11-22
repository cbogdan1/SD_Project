-- The seed devices of device-service (its V3__insert_sample_devices.sql), with the same ids and owners.
-- They were inserted by a migration there, so no sync event exists for them.
-- Owners: Ion (Fridge, Washing machine), Maria (Air conditioner), unassigned (Electric heater).
INSERT INTO device (id, user_id)
VALUES
    (decode('d1eebc999c0b4ef8bb6d6bb9bd380a01', 'hex'), decode('a0eebc999c0b4ef8bb6d6bb9bd380a11', 'hex')),
    (decode('d2eebc999c0b4ef8bb6d6bb9bd380a02', 'hex'), decode('a0eebc999c0b4ef8bb6d6bb9bd380a11', 'hex')),
    (decode('d3eebc999c0b4ef8bb6d6bb9bd380a03', 'hex'), decode('b1eebc999c0b4ef8bb6d6bb9bd380a22', 'hex')),
    (decode('d4eebc999c0b4ef8bb6d6bb9bd380a04', 'hex'), NULL);
