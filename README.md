# Energy Management System (Distributed Systems)

Microservices behind a Traefik API gateway, with JWT authentication, asynchronous communication through
Apache Kafka and an Angular frontend.

- **Assignment 1:** users, devices, authentication, reverse proxy, Docker deployment.
- **Assignment 2:** device data simulator → Kafka → monitoring microservice (hourly energy consumption),
  event-based synchronization of users and devices, consumption chart for clients.

Detailed notes in Romanian (every file, the message flows, Kafka concepts, answers to the evaluation questions):
[`docs/A2_NOTES.md`](docs/A2_NOTES.md).

## Architecture

```
Browser ──► Traefik :80 ──┬─ /auth/login, /auth/register ───────────────► auth-service:8083       ──► auth-db       (credential)
                          ├─ /users/**      ──[ForwardAuth]──[strip]────► user-service:8080       ──► user-db       (person)
                          ├─ /devices/**    ──[ForwardAuth]──[strip]────► device-service:8081     ──► device-db     (device, app_user)
                          ├─ /monitoring/** ──[ForwardAuth]──[strip]────► monitoring-service:8084 ──► monitoring-db (device, hourly_consumption)
                          ├─ /{users,devices,monitoring,auth}/swagger-ui, /v3/api-docs ──[strip]──► Swagger of that service (public)
                          └─ everything else ───────────────────────────► frontend:80 (nginx + Angular)

Kafka (one broker, KRaft):
  simulator (host) ──► localhost:9092 ── topic device-measurements ──► monitoring-service (group monitoring-service)
  auth-service   ──┐ USER_CREATED                            ┌───────► auth-service       (group auth-service: USER_DELETED)
  user-service   ──┼─► kafka:29092 ──── topic sync-events ───┼───────► user-service       (group user-service: USER_CREATED)
  device-service ──┘ USER_DELETED, DEVICE_*                  ├───────► device-service     (group device-service: USER_*)
                                                             └───────► monitoring-service (group monitoring-service: DEVICE_*, USER_DELETED)
```

auth-service and user-service never call each other over HTTP: they are connected only through `sync-events`.

UML deployment diagram: [`docs/deployment.png`](docs/deployment.png) (source: [`docs/deployment.puml`](docs/deployment.puml)).

![Deployment diagram](docs/deployment.png)

| Service | Module | Responsibility |
|---|---|---|
| `auth-service` | `services/auth_management` | `/login`, `/register`, issues JWTs (`role`, `userId`), `/validate` for Traefik ForwardAuth. Publishes `USER_CREATED` (with the profile), consumes `USER_DELETED` |
| `user-service` | `services/user_management` | Read/update/delete persons (`/person`): id, username, name, address, age. Consumes `USER_CREATED`, publishes `USER_DELETED` |
| `device-service` | `services/device_management` | CRUD on devices (`/device`): id, name, description, address, max consumption, owner. Keeps the user ids in a local `app_user` table (from `USER_*` events). Publishes `DEVICE_CREATED` / `DEVICE_UPDATED` / `DEVICE_DELETED` |
| `monitoring-service` | `services/monitoring_management` | Consumes the measurements and adds them to hourly totals. Keeps a local `device` table (id + owner, from `DEVICE_*` events). `GET /consumption/user/{userId}?deviceId=…&date=…` |
| `kafka` | image `apache/kafka:3.9.1` | Message broker, KRaft mode (no ZooKeeper), one broker. Topics `device-measurements` and `sync-events` |
| `simulator` | `simulator` | Java console app that runs on the host (outside Docker): sends one measurement per 10 minutes to Kafka |
| `frontend` | `frontend` | Angular SPA: login/register, admin pages, client page with the consumption chart (Chart.js) |
| `common` | `common` | Shared exceptions, error handler, `EndpointPaths`, the sync event model (`SyncEvent`, `KafkaTopics`) |

### How a request is authorized
1. The user logs in at `POST /auth/login` and gets a JWT signed with HS256. The token holds the `role` (`ADMIN` / `CLIENT`) and the `userId`.
2. The Angular interceptor (`frontend/src/app/auth/auth.interceptor.ts`) adds `Authorization: Bearer <token>` to every request.
3. For `/users/**`, `/devices/**` and `/monitoring/**`, Traefik first calls `auth-service /validate` (ForwardAuth middleware `jwt-auth`).
   - No token, a bad token or an expired token gets **401**.
   - A valid token without permission gets **403** (rules in `AccessPolicy.java`).
   - Otherwise the request is forwarded, with the `X-User-Id` and `X-User-Role` headers added.
4. Permissions:
   - **ADMIN** has full access to users, devices and consumption.
   - **CLIENT** can only `GET /users/person/{ownId}`, `GET /devices/device/user/{ownId}` and
     `GET /monitoring/consumption/user/{ownId}?deviceId=…&date=…`. For the last one, monitoring-service also checks
     that the device belongs to that user (**403** otherwise).

Only `/auth/login` and `/auth/register` are routed publicly. `/validate` is reachable only on the internal Docker network. The backends have no host ports, so the gateway is the only way in.

### Asynchronous communication (Kafka)

| Topic | Producer(s) | Consumer group(s) | Message value (JSON text) |
|---|---|---|---|
| `device-measurements` | simulator | `monitoring-service` | `{"timestamp":"2026-11-22T18:40:00","device_id":"d1eebc99-…","measurement_value":0.1234}` |
| `sync-events` | auth-service, user-service, device-service | `auth-service`, `user-service`, `device-service`, `monitoring-service` | `{"type":"DEVICE_CREATED","id":"<device id>","userId":"<owner id>"}`, `{"type":"USER_CREATED","id":"<user id>","username":"ion","name":"Ion Popescu","address":"…","age":22}` |

- Both topics have 1 partition. Kafka creates them on first use.
- Every service reads with its **own consumer group**, so every service gets every message (publish-subscribe).
  Each one only acts on the event types it needs.
- A new consumer group starts from the oldest stored message (`auto-offset-reset=earliest`). A service that starts
  later, or was down, still gets the events it missed.
- The message key is the device/user id. Messages with the same key go to the same partition, so they stay in order.
- `measurement_value` is the energy (kWh) used in the 10 minutes that start at `timestamp` (local time, no time zone).
  monitoring-service adds it to the row of that device and hour (`18:40` → row `18:00`).

### Cross-service consistency (synchronization with events)
The device DB keeps a local table `app_user(id)` with only the ids of the users. `device.user_id` is a foreign key
to it (`ON DELETE SET NULL`): one user has many devices. The monitoring DB keeps a local `device(id, user_id)` table.

- **Create a user** (register or admin "New user"):
  1. auth-service validates the request (name and address required, age ≥ 18), generates the user id (UUID), checks
     that the username is free and saves the credential (hashed password).
  2. It publishes `USER_CREATED` with the id and the profile (username, name, address, age).
  3. user-service consumes it and inserts the person with that exact id. device-service inserts the id into `app_user`.

  The person appears a few milliseconds after `/auth/register` returns (eventual consistency). The admin page waits
  500 ms before it reloads the user list. There is no `POST /person` anymore.
- **Seeded admin:** on every start auth-service creates the `admin` credential if it is missing, then publishes
  `USER_CREATED` for it (name `Administrator`). The consumers skip a user they already have, so the admin has a
  person and an `app_user` row like any other user, also on databases created before this change.
- **Assign a device:** device-service checks that the owner id exists in its own `app_user` table (no call to
  user-service). Unknown id → **404**.
- **Create / update / delete a device:** device-service publishes `DEVICE_CREATED` / `DEVICE_UPDATED` /
  `DEVICE_DELETED` with the device id and its owner. monitoring-service inserts, updates or deletes the device in its
  own `device` table. On delete, the database also deletes the device's hourly rows (`ON DELETE CASCADE`).
- **Delete a user:**
  1. user-service deletes the person and publishes `USER_DELETED`.
  2. auth-service deletes the credential, so the user cannot log in anymore. An already issued JWT stays valid
     until it expires (at most 60 minutes).
  3. device-service deletes the id from `app_user` with one direct `DELETE`, and the foreign key unassigns that user's
     devices. monitoring-service also sets the owner to `NULL` on those devices.
- **Kafka down:** the services wait for Kafka to confirm every event inside `@Transactional`. If Kafka cannot be
  reached within 10 s, the database change is rolled back and the API returns **503**, so the services never
  disagree.
- **Measurements of unknown devices** are ignored by monitoring-service and logged. These are devices for which no
  `DEVICE_CREATED` was received.
- **Seed devices:** the seed devices of device-service are also in the monitoring DB, with the same ids
  (`V2__insert_sample_devices.sql`), because they were inserted by a migration and never produced an event.

## Build & run

Requirements: Docker (with Compose). Maven and Node run inside the Docker builds (multi-stage Dockerfiles).
The simulator needs Java 17+ on the host.

```bash
# build all images and start everything (proxy, frontend, 4 services, 4 databases, Kafka)
docker compose up --build        # add -d to run in the background

# start from empty databases again (deletes the DB volumes)
docker compose down -v

# optional: load-balance a service across replicas
docker compose up -d --scale user-service=2
```

The first build downloads the Maven and npm dependencies and takes a few minutes. After the containers start,
wait ~20 seconds until the Spring services are ready. Kafka has no volume: `docker compose down` deletes its
messages. The hourly totals are kept in `monitoring-db`.

### Device data simulator
The simulator runs on the host and connects to Kafka at `localhost:9092`, which docker-compose publishes. Build it once:

```bash
mvn -pl simulator package
# without Maven installed:
docker run --rm -v "$PWD":/src -v m2cache:/root/.m2 -w /src maven:3.9-eclipse-temurin-17 mvn -B -pl simulator package
```

Set the device in `simulator/config.properties`. Copy the id from the **Id** column of the admin **Devices** page:

```properties
device.id=d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01
kafka.bootstrap.servers=localhost:9092
kafka.topic=device-measurements
send.interval.seconds=600
start.time=
```

Then run it:

```bash
java -jar simulator/target/simulator.jar simulator/config.properties
```

- `send.interval.seconds=600` sends one message every 10 minutes (real time). If `start.time` is empty, the first
  timestamp is now, rounded down to 10 minutes.
- **Demo mode:** set `send.interval.seconds=1` and `start.time=2026-11-22T00:00`. The simulator then sends one message
  per second while the timestamp still moves 10 minutes per message, so one full day (144 messages) takes about
  2.5 minutes. Then log in as the owner of the device, pick the device and `2026-11-22`, and click **Show**.
- **Several devices:** run one simulator per device, each with its own config file
  (`java -jar simulator/target/simulator.jar fridge.properties`).
- The value of each message is the base load (random, chosen at start) × a factor for the hour (low at night,
  highest from 17:00 to 22:00) × a ±10 % fluctuation.

The simulator prints every message it sends:
```
Sent: {"timestamp":"2026-11-21T00:00:00","device_id":"d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01","measurement_value":0.0466}
```
monitoring-service prints every message it receives (`docker compose logs -f monitoring-service`):
```
Received measurement: {"timestamp":"2026-11-21T00:00:00","device_id":"d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01","measurement_value":0.0466}
Device d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01, hour 2026-11-21T00:00: total is now 0.047 kWh
```

### URLs

| What | URL |
|---|---|
| App (frontend) | http://localhost |
| Traefik dashboard | http://localhost:8082 |
| Swagger UI – user-service | http://localhost/users/swagger-ui |
| Swagger UI – device-service | http://localhost/devices/swagger-ui |
| Swagger UI – monitoring-service | http://localhost/monitoring/swagger-ui |
| Swagger UI – auth-service | http://localhost/auth/swagger-ui |
| OpenAPI JSON | http://localhost/users/v3/api-docs, http://localhost/devices/v3/api-docs, http://localhost/monitoring/v3/api-docs, http://localhost/auth/v3/api-docs |
| Kafka (for the simulator) | `localhost:9092` |
| Kafka UI (topics, messages, consumer groups) | http://localhost:8085 |

Swagger is public (no token). To try the protected endpoints: call `POST /login` in the auth-service Swagger
(or `curl`, see below), copy the `token`, click **Authorize** in the user/device/monitoring Swagger and paste only
the token (Swagger adds `Bearer ` itself). Requests go through Traefik, so the role rules still apply
(CLIENT gets 403 on admin endpoints). `/login` and `/register` are public and need no token.

### Test accounts

| Username | Password | Role | Notes |
|---|---|---|---|
| `admin` | `admin` | ADMIN | created on the first start of auth-service |
| `ion` | `ion123` | CLIENT | owns *Fridge* (`d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01`) and *Washing machine* |
| `maria` | `maria123` | CLIENT | owns *Air conditioner* |

### Database dumps
`db/user_sd.sql`, `db/device_sd.sql`, `db/auth_sd.sql` and `db/monitoring_sd.sql` are `pg_dump`s of the
4 databases. The first three hold the seed data. `monitoring_sd.sql` holds the seed devices plus one simulated day
(2026-11-21) for ion's *Fridge*. They were made with:
```bash
docker exec user-postgres       pg_dump -U postgres user_sd       > db/user_sd.sql
docker exec device-postgres     pg_dump -U postgres device_sd     > db/device_sd.sql
docker exec auth-postgres       pg_dump -U postgres auth_sd       > db/auth_sd.sql
docker exec monitoring-postgres pg_dump -U postgres monitoring_sd > db/monitoring_sd.sql
```
You do not need them to run the app: Flyway creates the tables and the seed data on the first start.

For anything beyond local development, create a `.env` file next to `docker-compose.yml`:

```
JWT_SECRET=<at least 32 random characters>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<strong password>
```

## Test

### Unit tests
```bash
mvn test
# without Maven installed:
docker run --rm -v "$PWD":/src -v m2cache:/root/.m2 -w /src maven:3.9-eclipse-temurin-17 mvn -B test
```
- `auth_management`: `JwtServiceTest` covers the JWT round trip and the rejection cases. `AccessPolicyTest` covers
  the gateway permission rules, including the consumption rule.
- `monitoring_management`: `ConsumptionServiceTest` covers adding a measurement to its hour, ignoring unknown
  devices, always returning 24 hourly values, and 403 for another user's device.

> Known failure: `PersonServiceIntegrationTests.testInsertCorrectWithGetById` fails on the H2 test database: H2 2.x
> stores the UUID in a fixed-length `BINARY(255)` column (padded with zeros), so the lookup by id finds nothing.
> The real app uses PostgreSQL (`BYTEA`), where this works. All other tests pass.

### End-to-end through the gateway (curl)
```bash
# login as admin
TOKEN=$(curl -s -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin"}' \
  http://localhost/auth/login | sed 's/.*"token":"\([^"]*\)".*/\1/')

curl -i http://localhost/users/person                                  # 401, no token
curl -i -H "Authorization: Bearer $TOKEN" http://localhost/users/person # 200

# register a client (credential now, person a few ms later via USER_CREATED), returns the userId
curl -s -H 'Content-Type: application/json' \
  -d '{"username":"ana","password":"pass1","name":"Ana","address":"Str 1","age":18}' \
  http://localhost/auth/register
```
Then log in as `ana`:
- `GET /devices/device/user/<anaId>` returns **200**
- `GET /devices/device` returns **403**

Consumption of ion's Fridge on a day (run the simulator for it first):
```bash
ION=$(curl -s -H 'Content-Type: application/json' -d '{"username":"ion","password":"ion123"}' \
  http://localhost/auth/login | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl -s -H "Authorization: Bearer $ION" \
  "http://localhost/monitoring/consumption/user/a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11?deviceId=d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01&date=2026-11-21"
# 200: [{"hour":0,"energyKwh":0.254},{"hour":1,"energyKwh":0.258}, ... 24 values]
```
With maria's token, the same request returns **403**.

To watch the events and messages live:
```bash
docker compose logs -f user-service device-service monitoring-service | grep -E "sync event|measurement"
```

### In the browser
1. Open http://localhost and log in as `admin`. You can manage users and devices, and assign a device to a user using the **Owner** dropdown.
2. Log out and use **Register** to create a client. The client only sees **My devices**.
3. Open `/admin/users` as a client. The route guard redirects you to `/my-devices`.
4. As a client, under **Consumption** on **My devices**, choose a device and a day and click **Show**.
   The bar chart shows the energy for each hour (OX = hours 0–23, OY = kWh).

## Ports

| Port (host) | What |
|---|---|
| 80 | Traefik (app entry point) |
| 8082 | Traefik dashboard |
| 9092 | Kafka, for the simulator on the host (containers use `kafka:29092`) |
| 8085 | Kafka UI (`kafbat/kafka-ui`, connects to `kafka:29092`) |
| 5432 / 5433 / 5434 / 5435 | user-db / device-db / auth-db / monitoring-db (for local inspection) |
