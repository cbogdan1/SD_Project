-- Sample devices; user_id values are the sample persons from user-service (Ion, Maria), NULL = unassigned.
INSERT INTO device (id, description, address, max_energy_consumption, user_id)
VALUES
    (decode('d1eebc999c0b4ef8bb6d6bb9bd380a01', 'hex'), 'Fridge', 'Str. Observatorului 34, Cluj-Napoca', 1.5,
     decode('a0eebc999c0b4ef8bb6d6bb9bd380a11', 'hex')),
    (decode('d2eebc999c0b4ef8bb6d6bb9bd380a02', 'hex'), 'Washing machine', 'Str. Observatorului 34, Cluj-Napoca', 2.2,
     decode('a0eebc999c0b4ef8bb6d6bb9bd380a11', 'hex')),
    (decode('d3eebc999c0b4ef8bb6d6bb9bd380a03', 'hex'), 'Air conditioner', 'Str. Memorandumului 10, Cluj-Napoca', 3.0,
     decode('b1eebc999c0b4ef8bb6d6bb9bd380a22', 'hex')),
    (decode('d4eebc999c0b4ef8bb6d6bb9bd380a04', 'hex'), 'Electric heater', 'Str. Memorandumului 10, Cluj-Napoca', 2.0,
     NULL);
