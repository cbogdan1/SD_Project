-- Local copy of the devices from device-service: only the id and the owner (user_id, NULL = unassigned).
-- It is filled by the DEVICE_CREATED / DEVICE_UPDATED / DEVICE_DELETED sync events.
CREATE TABLE device (
    id      BYTEA NOT NULL PRIMARY KEY,
    user_id BYTEA
);

-- One row per device per hour: the sum of the 10-minute measurements of that hour (kWh).
-- When a device is deleted, the database also deletes its consumption rows (ON DELETE CASCADE).
CREATE TABLE hourly_consumption (
    id         BIGSERIAL        PRIMARY KEY,
    device_id  BYTEA            NOT NULL REFERENCES device (id) ON DELETE CASCADE,
    hour_start TIMESTAMP        NOT NULL,
    energy_kwh DOUBLE PRECISION NOT NULL,
    CONSTRAINT uq_device_hour UNIQUE (device_id, hour_start)
);
