# Energy Management System (Distributed Systems)

Microservices behind a Traefik API gateway, with JWT authentication and an Angular frontend.

## Architecture

```
Browser ──► Traefik :80 ──┬─ /auth/login, /auth/register ─────────────► auth-service:8083   ──► auth-db   (credentials)
                          ├─ /users/**   ──[ForwardAuth]──[strip]─────► user-service:8080   ──► user-db   (persons)
                          ├─ /devices/** ──[ForwardAuth]──[strip]─────► device-service:8081 ──► device-db (devices)
                          └─ everything else ─────────────────────────► frontend:80 (nginx + Angular)
```

| Service | Module | Responsibility |
|---|---|---|
| `auth-service` | `services/auth_management` | `/login`, `/register`, issues JWTs (`role`, `userId`), `/validate` for Traefik ForwardAuth |
| `user-service` | `services/user_management` | CRUD on persons (`/person`) |
| `device-service` | `services/device_management` | CRUD on devices (`/device`), device ↔ user mapping (`/device/user/{userId}`) |
| `frontend` | `frontend` | Angular SPA: login/register, admin pages, client page |
| `common` | `common` | Shared exceptions, error handler, `EndpointPaths` |

### How a request is authorized
1. The user logs in at `POST /auth/login` and gets a JWT signed with HS256. The token holds the `role` (`ADMIN` / `CLIENT`) and the `userId`.
2. The Angular interceptor (`frontend/src/app/auth/auth.interceptor.ts`) adds `Authorization: Bearer <token>` to every request.
3. For `/users/**` and `/devices/**`, Traefik first calls `auth-service /validate` (ForwardAuth middleware `jwt-auth`).
   - No token, a bad token or an expired token gets **401**.
   - A valid token without permission gets **403** (rules in `AccessPolicy.java`).
   - Otherwise the request is forwarded, with the `X-User-Id` and `X-User-Role` headers added.
4. Permissions:
   - **ADMIN** has full access to users and devices.
   - **CLIENT** can only `GET /users/person/{ownId}` and `GET /devices/device/user/{ownId}`.

Only `/auth/login` and `/auth/register` are routed publicly. `/validate` and `/credentials` are reachable only on the internal Docker network. The backends have no host ports, so the gateway is the only way in.

### Cross-service consistency
- **Register:** auth-service creates the person in user-service (`POST /person`), then stores the credential linked to that `userId`.
- **Delete a person:** user-service first deletes the credential in auth-service and unassigns the devices in device-service, then deletes the person. If either call fails, the person is kept and the API returns 503.

## Build & run

Requirements: Docker (with Compose). Maven and Node are only needed if you build outside Docker.

```bash
# 1. Build the Spring Boot jars (the service Dockerfiles copy target/*.jar)
docker run --rm -v "$PWD":/src -v m2cache:/root/.m2 -w /src maven:3.9-eclipse-temurin-17 mvn -B package -DskipTests
#    or, with Maven installed:  mvn package -DskipTests

# 2. Build images and start everything
docker compose up -d --build

# optional: load-balance a service across replicas
docker compose up -d --scale user-service=2
```

- App: http://localhost
- Traefik dashboard: http://localhost:8082
- Default admin: `admin` / `admin`, created on the first start of auth-service.

For anything beyond local development, create a `.env` file next to `docker-compose.yml`:

```
JWT_SECRET=<at least 32 random characters>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<strong password>
```

## Test

### Unit tests
```bash
mvn test        # or the docker run ... mvn -B test variant above
```
`auth_management` tests cover the JWT round trip and the rejection cases (`JwtServiceTest`), plus the gateway permission rules (`AccessPolicyTest`).

> Known pre-existing failure: `PersonServiceIntegrationTests` in `user_management` fails because `test-sql/delete.sql` uses MySQL syntax (`SET FOREIGN_KEY_CHECKS`) on H2.

### End-to-end through the gateway (curl)
```bash
# login as admin
TOKEN=$(curl -s -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin"}' \
  http://localhost/auth/login | sed 's/.*"token":"\([^"]*\)".*/\1/')

curl -i http://localhost/users/person                                  # 401, no token
curl -i -H "Authorization: Bearer $TOKEN" http://localhost/users/person # 200

# register a client (creates person + credential), returns the userId
curl -s -H 'Content-Type: application/json' \
  -d '{"username":"ana","password":"pass1","name":"Ana","address":"Str 1","age":30}' \
  http://localhost/auth/register
```
Then log in as `ana`:
- `GET /devices/device/user/<anaId>` returns **200**
- `GET /devices/device` returns **403**

### In the browser
1. Open http://localhost and log in as `admin`. You can manage users and devices, and assign a device to a user using the **Owner** dropdown.
2. Log out and use **Register** to create a client. The client only sees **My devices**.
3. Open `/admin/users` as a client. The route guard redirects you to `/my-devices`.

## Ports

| Port (host) | What |
|---|---|
| 80 | Traefik (app entry point) |
| 8082 | Traefik dashboard |
| 5432 / 5433 / 5434 | user-db / device-db / auth-db (for local inspection) |
