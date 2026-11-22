# Tema A2: Comunicare asincronă (note de pregătire)

Sistemul din A1 (Traefik + auth/user/device + Angular) a fost extins cu:

- un **simulator** de contor inteligent (producător de mesaje), care rulează pe calculatorul gazdă;
- **Apache Kafka** ca message broker (în loc de RabbitMQ);
- un **microserviciu de monitorizare** (consumator), care calculează consumul pe oră;
- **sincronizare prin evenimente** între servicii (în loc de apelurile REST din A1);
- un **grafic** al consumului zilnic pentru client.

Cuprins:
1. [Ce este nou: servicii, containere, topicuri, tabele, endpoint-uri, pagini](#1-ce-este-nou)
2. [Fluxul mesajelor: simulator → Kafka → monitoring → DB → REST → grafic](#2-fluxul-mesajelor-de-măsurare)
3. [Fluxul de sincronizare (evenimente user/device)](#3-fluxul-de-sincronizare)
4. [Conceptele Kafka folosite și comparația cu RabbitMQ](#4-concepte-kafka)
5. [Răspunsuri la întrebările de evaluare (MOM, Queue vs Topic, P2P vs Pub-Sub)](#5-întrebările-de-evaluare)
6. [Fișiere noi și modificate (ce face fiecare și de ce)](#6-fișiere-noi-și-modificate)
7. [Decizii luate și cazuri limită](#7-decizii-și-cazuri-limită)
8. [Demo la evaluare (pași) și alte întrebări probabile](#8-demo-la-evaluare)

---

## 1. Ce este nou

### Servicii și containere

| Container | Imagine / modul | Port | Rol |
|---|---|---|---|
| `kafka` | `apache/kafka:3.9.1` | `29092` pentru containere, `9092` pe gazdă | Brokerul de mesaje. Mod **KRaft** (fără ZooKeeper): un singur nod e și broker, și controller. |
| `monitoring-service` | `services/monitoring_management` | 8084 (doar în rețeaua Docker) | Citește măsurătorile și evenimentele de sincronizare, salvează totalurile pe oră, le oferă prin REST. |
| `monitoring-db` (`monitoring-postgres`) | `postgres:15-alpine` | 5435 pe gazdă | Baza de date proprie a serviciului de monitorizare (`monitoring_sd`). |
| simulatorul (nu e container) | `simulator/` → `simulator.jar` | se conectează la `localhost:9092` | Aplicație Java de consolă care trimite o măsurătoare la fiecare 10 minute. |

Containerele din A1 au rămas. Doar `user-service` și `device-service` au primit adresa Kafka (`KAFKA_BOOTSTRAP_SERVERS=kafka:29092`)
și pornesc după ce Kafka e „healthy” (`depends_on`).

### Topicuri Kafka

| Topic | Cine scrie (producer) | Cine citește (consumer group) | Ce conține |
|---|---|---|---|
| `device-measurements` | simulatorul | `monitoring-service` | `{"timestamp":"2026-11-22T18:40:00","device_id":"…","measurement_value":0.1234}` |
| `sync-events` | `user-service`, `device-service` | `device-service`, `monitoring-service` | `{"type":"DEVICE_CREATED","id":"…","userId":"…"}` |

Ambele topicuri au o singură partiție. Kafka le creează automat la prima folosire.
`sync-events` este „Synchronization Queue” din PDF, iar `device-measurements` este coada de colectare a datelor.

### Tabele noi (baza `monitoring_sd`)

| Tabel | Coloane | Rol |
|---|---|---|
| `device` | `id` (BYTEA, PK), `user_id` (BYTEA, poate fi NULL) | Copia locală a dispozitivelor: doar id-ul și proprietarul. Se umple din evenimentele `DEVICE_*`. |
| `hourly_consumption` | `id` (BIGSERIAL), `device_id` (FK → `device`, `ON DELETE CASCADE`), `hour_start` (TIMESTAMP), `energy_kwh` | Un rând pentru fiecare dispozitiv și fiecare oră. `UNIQUE (device_id, hour_start)`. |

Migrații Flyway: `V1__init_monitoring.sql` (tabelele) și `V2__insert_sample_devices.sql` (cele 4 dispozitive seed din device-service,
cu aceleași id-uri). Migrațiile din A1 **nu** au fost modificate.

### Endpoint-uri

| Endpoint (prin Traefik) | Cine are voie | Răspuns |
|---|---|---|
| `GET /monitoring/consumption/user/{userId}?deviceId={id}&date=2026-11-22` | ADMIN: orice. CLIENT: doar cu propriul `userId`, și doar pentru dispozitivele lui | 24 de obiecte `{"hour":0..23,"energyKwh":…}` (0 pentru orele fără date) |
| `GET /monitoring/` | ADMIN | „Monitoring Service is running...” |
| `/monitoring/swagger-ui`, `/monitoring/v3/api-docs` | public | documentația Swagger |

Endpoint-uri **șterse**: `POST /device/user/{userId}` și `DELETE /device/user/{userId}` din device-service.
Erau sincronizarea prin REST din A1 și acum sunt înlocuite de evenimente.

### Pagini frontend

- **My devices** (client): o secțiune nouă **Consumption**, cu un `<select>` cu dispozitivele clientului, un `<input type="date">`
  (calendarul), butonul **Show** și un grafic cu bare (Chart.js). OX = orele 0–23, OY = kWh consumați în acea oră. HTML simplu, fără CSS nou.
- **Devices** (admin): o coloană nouă **Id**, ca să poți copia id-ul dispozitivului în `config.properties` al simulatorului.

---

## 2. Fluxul mesajelor de măsurare

```
 GAZDA (Windows)                    DOCKER                                             
 ┌────────────────────────┐        ┌────────────────────────────────┐                  
 │ simulator.jar          │  JSON  │ kafka                          │                  
 │  EnergyGenerator       │ ─────► │  topic device-measurements     │                  
 │  KafkaProducer         │ :9092  │  partiția 0: offset 0, 1, 2 …  │                  
 └────────────────────────┘        └───────────────┬────────────────┘                  
                                                   │ poll (grupul monitoring-service)  
                                                   ▼                                   
                                   ┌────────────────────────────────┐                  
                                   │ monitoring-service             │                  
                                   │  MeasurementListener           │ afișează mesajul 
                                   │  → ConsumptionService          │                  
                                   │     .addMeasurement()          │                  
                                   └───────────────┬────────────────┘                  
                                                   │ JPA: SELECT + INSERT/UPDATE       
                                                   ▼                                   
                                   monitoring-db: hourly_consumption                   
                                                   ▲                                   
 Browser (Angular + Chart.js)                      │ SELECT … BETWEEN 00:00 AND 23:00  
   GET /monitoring/consumption/user/{id}?deviceId=…&date=…                             
      → Traefik → ForwardAuth /validate (AccessPolicy) → ConsumptionController         
      → ConsumptionService.getDailyConsumption() → 24 valori → grafic                  
```

Pas cu pas:

1. **Simulatorul** citește `config.properties`: `device.id`, adresa Kafka, topicul, intervalul și ora de start.
2. La fiecare pas, `EnergyGenerator.nextValue(ora)` calculează energia consumată în 10 minute:
   **sarcina de bază** (aleatoare, aleasă o dată la pornire, 0,05–0,15 kWh) × **factorul orei**
   (0,4 noaptea, 1,0 dimineața, 0,7 ziua, **1,6 seara între 17 și 22**, 0,8 târziu) × o **fluctuație** de ±10 %.
3. Măsurătoarea e transformată în JSON (Jackson) și trimisă cu `KafkaProducer` în topicul `device-measurements`.
   Cheia mesajului este `device_id`. Simulatorul așteaptă confirmarea (`get()`) și apoi afișează `Sent: {...}`.
4. Ora simulată crește mereu cu 10 minute. În **modul demo** (`send.interval.seconds=1`) se trimite un mesaj pe secundă,
   deci o zi întreagă (144 de mesaje) durează ~2,5 minute.
5. **monitoring-service** citește topicul cu `@KafkaListener` și afișează fiecare mesaj în consolă
   (`Received measurement: …`, cerința „Display messages … in Console”). Apoi transformă JSON-ul în `MeasurementDTO`.
6. `ConsumptionService.addMeasurement()`:
   - dacă dispozitivul nu există în tabela locală `device`, ignoră mesajul și scrie un warning în log;
   - altfel rotunjește ora în jos (`18:40 → 18:00`), caută rândul (device, 18:00), îl creează cu 0 dacă nu există,
     adună valoarea și salvează.
7. Clientul alege dispozitivul și ziua și apasă **Show**. Angular trimite cererea GET (interceptorul pune token-ul).
   Traefik întreabă auth-service (`/validate`), iar `AccessPolicy` lasă un CLIENT doar cu propriul `userId`.
8. `getDailyConsumption()` verifică dacă dispozitivul e al acelui utilizator (403 dacă nu e), citește rândurile zilei
   și întoarce mereu 24 de valori, cu 0 unde nu sunt date. Valorile sunt rotunjite la 3 zecimale.
9. `my-devices.component.ts` desenează graficul cu bare (Chart.js): etichetele sunt orele 0–23, valorile sunt kWh.

Un exemplu real din test: simulatorul a trimis 144 de mesaje pentru „Elena Heater” (ziua 2026-11-22).
monitoring-service a afișat 144 de mesaje primite, iar în DB au apărut 24 de rânduri. Fiecare rând este exact suma
celor 6 valori din ora lui. Noaptea sunt ~0,14 kWh/oră, iar seara ~0,59 kWh/oră.

**Ora** este ora locală a contorului, fără fus orar (`"2026-11-22T18:40:00"`). Nu există conversii nicăieri,
deci ora 18 din simulator este bara 18 din grafic. `timestamp` este **începutul** intervalului de 10 minute.

---

## 3. Fluxul de sincronizare

Fiecare serviciu are baza lui de date. Datele de care au nevoie și alte servicii sunt copiate prin evenimente:
user-service ține profilul (`person`), device-service ține id-urile utilizatorilor (`app_user`), iar monitoring-service
ține dispozitivele și proprietarii (`device`). Ca în diagrama conceptuală din PDF, **auth-service și user-service sunt
legate doar de broker**: nu se apelează niciodată direct prin HTTP.

```
 auth-service ─── USER_CREATED ─────────────────────────────┐
 user-service ─── USER_DELETED ─────────────────────────────┤
                                                            ▼
                                   ┌──────────────────────────────────────────┐
 device-service ─ DEVICE_CREATED / │ topic sync-events (o partiție)           │
                  DEVICE_UPDATED / │ offset: 0 USER_CREATED, 1 DEVICE_CREATED │
                  DEVICE_DELETED ─►│         2 USER_DELETED, …                │
                                   └───┬──────────┬──────────┬──────────┬─────┘
              (fiecare grup primește   │          │          │          │
               TOATE evenimentele)     ▼          ▼          ▼          ▼
   group auth-service    group user-service   group device-service     group monitoring-service
   USER_DELETED →        USER_CREATED →       USER_CREATED → INSERT    DEVICE_CREATED/UPDATED → save(device, owner)
     DELETE credential     INSERT person        app_user               DEVICE_DELETED → DELETE device (+ consum)
   restul → ignoră       restul → ignoră      USER_DELETED → DELETE    USER_DELETED → user_id = NULL la device-uri
                                                app_user               USER_CREATED → ignoră
                                              DEVICE_* → ignoră
```

Mesajul este un singur DTO, `SyncEvent`, în modulul `common`, deci același pentru toți: `type` + `id` + `userId`
(proprietarul, doar la device-uri) + `username`, `name`, `address`, `age` (profilul, doar la `USER_CREATED`).
Câmpurile `null` nu apar în JSON (`@JsonInclude(NON_NULL)`).

### Pe scenarii

**Utilizator nou** (Register pe pagina publică sau „New user” la admin) — pornește din auth-service:

```
 Browser ── POST /auth/register ──► auth-service
                                     1. validează: name, address obligatorii, age >= 18 (altfel 400)
                                     2. username liber? (altfel 409)
                                     3. userId = UUID.randomUUID()
                                     4. INSERT credential (parola hash-uită)
                                     5. publică USER_CREATED {id, username, name, address, age}
 Browser ◄── 201 + userId ────────── (4 și 5 în aceeași @Transactional)
                                                 │ sync-events
                         ┌───────────────────────┼─────────────────────────┐
                         ▼                       ▼                         ▼
                   user-service            device-service           monitoring-service
                   INSERT person           INSERT app_user          ignoră
                   (cu ACELAȘI id)         (același id)
```

- user-service **nu** publică încă un `USER_CREATED`: evenimentul care a creat persoana e deja în topic.
- Dacă Kafka e oprit, `publish()` aruncă eroare, `@Transactional` anulează login-ul și răspunsul e 503.
- user-service nu poate „refuza” un eveniment, de aceea validarea profilului s-a mutat în `RegisterRequest` (auth-service).

**Adminul inițial (seed):** la fiecare pornire, auth-service creează login-ul `admin` dacă lipsește și apoi publică
`USER_CREATED` pentru el (nume `Administrator`). Consumatorii sar peste un utilizator pe care îl au deja
(„already exists, skipped”), deci repetarea nu strică nimic. Așa adminul are persoană și rând în `app_user` ca oricine,
chiar și pe un volum vechi, creat înainte de această schimbare. Dacă publicarea eșuează, se reîncearcă la pornirea următoare.

**Dispozitiv nou / modificat / șters** (pagina Devices la admin):
1. device-service verifică proprietarul în `app_user` (404 dacă nu există), salvează și publică `DEVICE_CREATED`
   (sau `DEVICE_UPDATED` / `DEVICE_DELETED`) cu id-ul dispozitivului și proprietarul.
2. monitoring-service: la CREATED/UPDATED face `save(new Device(id, userId))`, adică insert sau suprascrie proprietarul.
   La DELETED face `DELETE`, iar baza de date șterge singură și consumul (`ON DELETE CASCADE`).

**Ștergere utilizator** (adminul apasă Delete) — pornește din user-service:

```
 Browser ── DELETE /users/person/{id} ──► user-service
                                           1. DELETE person
                                           2. publică USER_DELETED {id}   (1 și 2 în aceeași @Transactional)
 Browser ◄── 204 ─────────────────────────
                                                 │ sync-events
             ┌───────────────────┬───────────────┴──────────┬──────────────────────────┐
             ▼                   ▼                          ▼                          ▼
       auth-service        device-service             monitoring-service         user-service
       DELETE credential   DELETE app_user            user_id = NULL la          ignoră (e al lui)
       (nu se mai poate    (FK: device-urile lui      device-urile lui
        loga)               devin neasignate)
```

1. user-service șterge persoana și publică `USER_DELETED`.
2. auth-service șterge login-ul. Un token JWT deja emis rămâne valid până expiră (maxim 60 de minute).
3. device-service șterge id-ul din `app_user`. Cheia străină (`ON DELETE SET NULL`) face dispozitivele lui „neasignate”.
4. monitoring-service pune `user_id = NULL` la dispozitivele acelui utilizator. Fără asta ar rămâne un proprietar
   vechi, pentru că FK-ul din device-service schimbă rândurile direct în DB, fără să trimită evenimente `DEVICE_UPDATED`.

**Dacă Kafka e oprit:** `publish()` aruncă o eroare după cel mult ~10 s, `@Transactional` anulează (rollback)
modificarea din DB și API-ul răspunde **503**. Testat: cu Kafka oprit, Register → 503, nu există persoană, nu există login,
iar după repornirea Kafka nu apare niciun eveniment „fantomă”.

**Dacă un consumator e oprit** (de ex. device-service): evenimentul rămâne în Kafka. Când serviciul pornește,
continuă de la ultimul offset confirmat și recuperează tot. Testat: monitoring-service a pornit după ce evenimentele
fuseseră deja trimise și le-a citit pe toate de la început (grup nou + `auto-offset-reset=earliest`).

---

## 4. Concepte Kafka

| Concept | Ce înseamnă | Unde apare în proiect |
|---|---|---|
| **Broker** | Serverul Kafka care primește, păstrează și dă mai departe mesajele. | Containerul `kafka` (un singur broker). |
| **KRaft** | Kafka își ține singur metadatele (algoritmul Raft), fără ZooKeeper. Un nod poate fi broker + controller. | `KAFKA_PROCESS_ROLES: broker,controller`. |
| **Topic** | Un „canal” cu nume în care se scriu mesaje. Mesajele **rămân** pe disc (implicit 7 zile), nu se șterg când sunt citite. | `device-measurements`, `sync-events`. |
| **Partiție** | Un topic e împărțit în partiții. Fiecare partiție e un log ordonat (doar se adaugă la final). Ordinea e garantată în interiorul unei partiții. | O partiție pe topic, deci ordine totală. |
| **Offset** | Poziția (numărul) unui mesaj în partiție: 0, 1, 2, … Fiecare grup de consumatori ține minte până unde a citit. | Spring confirmă (commit) offsetul după ce metoda `@KafkaListener` s-a terminat fără eroare. Offseturile stau în topicul intern `__consumer_offsets`. |
| **Producer** | Cine scrie mesaje într-un topic. | Simulatorul (`KafkaProducer`), user-service și device-service (`KafkaTemplate` în `SyncEventPublisher`). |
| **Consumer** | Cine citește mesaje. În Kafka consumatorul **cere** mesajele (pull / poll). | `@KafkaListener` în `MeasurementListener` și în cele două `SyncEventListener`. |
| **Consumer group** | Un grup de consumatori care împart munca. Fiecare partiție e citită de un singur membru al grupului. **Grupuri diferite primesc fiecare toate mesajele.** | `device-service` și `monitoring-service` (`spring.kafka.consumer.group-id`). |
| **Cheie (key)** | Mesajele cu aceeași cheie ajung în aceeași partiție, deci rămân în ordine. | `device_id` / id-ul utilizatorului. |
| **auto-offset-reset** | De unde începe un grup **nou**: `earliest` de la primul mesaj păstrat, `latest` doar mesajele noi. | `earliest`, ca să nu se piardă evenimente trimise înainte de prima pornire. |
| **Listeners / advertised listeners** | Pe ce adresă ascultă brokerul și ce adresă le comunică clienților. | `INTERNAL://kafka:29092` (containere), `EXTERNAL://localhost:9092` (gazda, simulatorul). |
| **acks** | Producătorul așteaptă confirmarea brokerului. | Implicit `acks=all`. Noi așteptăm explicit cu `.get()`. |

Serializarea: cheia și valoarea sunt **text** (`StringSerializer`/`StringDeserializer`, implicit în Spring).
JSON-ul e făcut și citit de mână cu `ObjectMapper`. E simplu de explicat și un mesaj stricat nu blochează consumatorul:
se scrie o eroare în log și se trece mai departe.

### Kafka vs RabbitMQ

| | Kafka | RabbitMQ |
|---|---|---|
| Model | **Log distribuit**: mesajele rămân pe disc și se pot reciti | **Cozi**: mesajul se șterge după ce consumatorul confirmă (ACK) |
| Cine trimite mesajul la consumator | Consumatorul cere (**pull**) și își ține minte offsetul | Brokerul împinge (**push**) mesajele la consumator |
| Publish-subscribe | Un topic + **câte un consumer group** pentru fiecare serviciu | Un **exchange** (fanout/topic) + **câte o coadă** pentru fiecare serviciu |
| Recitire / un serviciu nou primește istoricul | Da (resetare offset, grup nou cu `earliest`) | Nu, după ACK mesajul nu mai există |
| Ordine | Garantată într-o partiție | Garantată într-o coadă (cu un singur consumator) |
| Rutare | Simplă: topic + cheie | Bogată: tipuri de exchange, routing keys, headere |
| Protocol | Protocol propriu Kafka peste TCP | AMQP 0-9-1 (și MQTT, STOMP prin pluginuri) |
| Potrivit pentru | Volume mari de date / evenimente, streaming, istoric | Cozi de sarcini, rutare complexă, latență mică |

Cu RabbitMQ, același proiect ar avea o coadă `device-measurements` și un exchange `fanout` `sync`, cu o coadă pentru
device-service și una pentru monitoring-service. În Kafka același efect se obține cu un topic și două consumer groups.

---

## 5. Întrebările de evaluare

### 5.1 Tipuri de Message-Oriented Middleware (MOM)

MOM este un software (de obicei un **broker**) prin care aplicațiile comunică **asincron**, prin mesaje:
producătorul trimite și **nu așteaptă** consumatorul, iar brokerul păstrează mesajul până e preluat.
Avantajul este decuplarea în **timp** (consumatorul poate fi oprit), în **spațiu** (nu trebuie să se cunoască între ei)
și în **încărcare** (mesajele se adună în coadă când consumatorul e lent).

Clasificări uzuale:
- **După modelul de comunicare:**
  - **Point-to-Point** (cozi): un mesaj e procesat de un singur consumator.
  - **Publish-Subscribe** (topicuri): un mesaj ajunge la toți abonații.
  - (Request-Reply se construiește peste acestea, cu o coadă de răspuns.)
- **După arhitectură:**
  - **cu broker central** (hub-and-spoke): RabbitMQ, ActiveMQ, IBM MQ, Kafka;
  - **fără broker** (brokerless / peer-to-peer): ZeroMQ.
- **După felul în care păstrează mesajele:**
  - **cozi clasice**, unde mesajul dispare după ACK: RabbitMQ, ActiveMQ, JMS;
  - **log / event streaming**, unde mesajele rămân și se pot reciti: Kafka, Pulsar.
- **După livrare:** push (brokerul trimite) sau pull (consumatorul cere); persistent (durable) sau doar în memorie.
- **Standarde / protocoale:** JMS (API Java), AMQP, MQTT (IoT), STOMP. Variante în cloud: Amazon SQS/SNS, Azure Service Bus, Google Pub/Sub.

În proiect: **Kafka**, un MOM **cu broker**, de tip **log**, cu **pull** și mesaje **persistente**.

### 5.2 Queue vs Topic

| | Queue (coadă) | Topic |
|---|---|---|
| Cine primește mesajul | **Un singur** consumator | **Toți** abonații (fiecare o copie) |
| Mai mulți consumatori | Concurează între ei (*competing consumers*), munca se împarte | Fiecare primește tot |
| După citire | Mesajul dispare | Mesajul e livrat tuturor abonaților |
| Folosire tipică | Distribuirea sarcinilor | Notificări / evenimente pentru mai multe servicii |

**În Kafka** există doar topicuri, dar ambele comportamente se obțin cu consumer groups:
- **în interiorul unui grup**, Kafka se comportă ca o **coadă**: fiecare mesaj e procesat de un singur membru al grupului;
- **între grupuri diferite**, Kafka se comportă ca un **topic**: fiecare grup primește toate mesajele.

**În proiect:**
- `device-measurements` e folosit ca **coadă**: un singur grup (`monitoring-service`) și fiecare măsurătoare
  e adunată o singură dată. Dacă am porni 2 replici de monitoring, ele ar fi în același grup și nu ar număra de două ori.
- `sync-events` e folosit ca **topic**: două grupuri (`device-service`, `monitoring-service`) și fiecare primește
  fiecare eveniment.

### 5.3 Point-to-Point vs Publish-Subscribe

- **Point-to-Point:** producător → coadă → **exact un** consumator. Producătorul și consumatorul nu trebuie să fie
  pornite în același timp. Mesajul e consumat o singură dată.
  *Exemplu în proiect:* simulator → `device-measurements` → monitoring-service.
  O valoare trebuie adunată o singură dată la totalul orei.
- **Publish-Subscribe:** publicatorul trimite un eveniment fără să știe cine îl ascultă. **Toți** abonații primesc o copie,
  iar abonații noi se adaugă fără să schimbi publicatorul.
  *Exemplu în proiect:* user-service publică `USER_DELETED`. Îl primesc auth-service (șterge login-ul), device-service (șterge din `app_user`)
  și monitoring-service (pune `user_id = NULL`). user-service nu știe de niciunul.
- **Abonament durabil:** la Kafka abonatul care pornește mai târziu primește și mesajele vechi, pentru că ele rămân
  în topic. Așa a recuperat monitoring-service toate evenimentele trimise înainte să existe.

---

## 6. Fișiere noi și modificate

### `common` (cod comun pentru toate serviciile)
- `events/KafkaTopics.java` (**nou**): numele celor două topicuri ca constante, ca să nu fie scrise de mână în fiecare serviciu.
- `events/SyncEventType.java` (**nou**): enum cu tipurile de evenimente (`USER_CREATED`, `USER_DELETED`, `DEVICE_CREATED`, `DEVICE_UPDATED`, `DEVICE_DELETED`).
- `events/SyncEvent.java` (**nou**): DTO-ul unic al evenimentelor de sincronizare: `type`, `id`, `userId` (proprietarul, doar la device-uri) și profilul `username`, `name`, `address`, `age` (doar la `USER_CREATED`). Are constructor gol pentru Jackson.
- `EndpointPaths.java` (**modificat**): a dispărut `CREDENTIALS`.
- `EndpointPaths.java` (**modificat**): `CONSUMPTION = "/consumption"` și `GATEWAY_MONITORING = "/monitoring"`, folosite de controller și de `AccessPolicy`.

### `auth_management`
- `pom.xml` (**modificat**): `spring-kafka`.
- `kafka/SyncEventPublisher.java` (**nou**): la fel ca cel din user-service (cheia = id-ul utilizatorului).
- `kafka/SyncEventListener.java` (**nou**): `@KafkaListener` pe `sync-events` (grupul `auth-service`). La `USER_DELETED` apelează `deleteByUserId` (șterge login-ul). Restul evenimentelor le ignoră.
- `services/AuthService.java` (**modificat**):
  - `register` generează id-ul (UUID), salvează login-ul și publică `USER_CREATED` cu profilul, în `@Transactional`. Apelul REST la user-service și `RestTemplate` au dispărut;
  - `run` (seed-ul adminului) publică `USER_CREATED` pentru admin la fiecare pornire.
- `dtos/RegisterRequest.java` (**modificat**): validarea profilului e aici acum: `name`/`address` `@NotBlank`, `age` `@Min(18)`.
- `controllers/AuthController.java` (**modificat**): a dispărut endpoint-ul intern `DELETE /credentials/user/{id}`.
- `application.properties` (**modificat**): a dispărut `services.user.url`. Kafka: adresa, timeout-urile producătorului, `group-id = auth-service`, `earliest`.
- `services/AccessPolicy.java` (**modificat**): o regulă nouă. CLIENT poate face `GET` pe exact `/monitoring/consumption/user/{idul lui}`, cu `path.equals(...)`, la fel ca regulile din A1.
- `test/.../AccessPolicyTest.java` (**modificat**): testul `clientCanReadOnlyOwnConsumption`. Propriul id e permis. Id-ul altuia, alte căi, `DELETE` și lipsa id-ului sunt refuzate. ADMIN poate tot.

### `user_management`
- `pom.xml` (**modificat**): dependența `spring-kafka`.
- `kafka/SyncEventPublisher.java` (**nou**): transformă evenimentul în JSON și îl trimite în `sync-events` cu `KafkaTemplate`, apoi **așteaptă confirmarea** (`.get()`). Dacă Kafka nu răspunde, aruncă `CustomException` cu 503.
- `kafka/SyncEventListener.java` (**nou**): `@KafkaListener` pe `sync-events` (grupul `user-service`). La `USER_CREATED` cu profil apelează `insert`. Restul evenimentelor le ignoră.
- `services/PersonService.java` (**modificat**):
  - `insert(SyncEvent)` salvează persoana cu id-ul din eveniment; dacă există deja, sare peste ea. Nu publică nimic;
  - `delete` șterge persoana și publică `USER_DELETED` în `@Transactional`, ca eroarea Kafka să anuleze ștergerea;
  - nu mai există niciun apel REST (nici la device-service, nici la auth-service).
- `entities/Person.java` (**modificat**): id-ul nu mai e generat, vine din eveniment (același ca în auth-service). `@Column(length = 16)` pentru baza H2 din teste.
- `controllers/PersonController.java` (**modificat**): a dispărut `POST /person` (utilizatorii se creează doar prin `/auth/register`). Au rămas GET, PUT, DELETE.
- `UsersApplication.java` (**modificat**): a dispărut bean-ul `RestTemplate`.
- `application.properties` (**modificat**):
  - adresa Kafka;
  - timeout-urile producătorului de 10 s în loc de 2 minute. Așa un mesaj anulat nu mai poate fi livrat mai târziu de un retry;
  - `group-id = user-service` și `earliest`;
  - au dispărut `services.device.url` și `services.auth.url`.
- `test/.../PersonServiceIntegrationTests.java` (**modificat**): `@MockBean SyncEventPublisher`, pentru că Kafka nu rulează în teste. Testează `insert` din eveniment (id-ul se păstrează, același eveniment de două ori = o singură persoană).
- `test/.../PersonControllerUnitTest.java` (**modificat**): validarea la `PUT` (vârstă < 18, adresă lipsă → 400) și `POST /person` → 405.
- `test/resources/application-test.properties` (**modificat**): `spring.kafka.listener.auto-startup=false` (listenerul nu pornește în teste).

### `device_management`
- `pom.xml` (**modificat**): `spring-kafka`.
- `kafka/SyncEventPublisher.java` (**nou**): la fel ca cel din user-service, dar cheia mesajului e id-ul dispozitivului.
- `kafka/SyncEventListener.java` (**nou**): `@KafkaListener` pe `sync-events` (grupul `device-service`). La `USER_CREATED` apelează `addUser`, la `USER_DELETED` apelează `deleteUser`. Evenimentele `DEVICE_*` sunt ale lui și le ignoră.
- `services/DeviceService.java` (**modificat**): `insert`, `update` și `delete` publică `DEVICE_CREATED/UPDATED/DELETED`, în `@Transactional`. `addUser`/`deleteUser` sunt chemate acum de listener și scriu în log la nivel INFO.
- `controllers/DeviceController.java` (**modificat**): au fost șterse `POST/DELETE /device/user/{userId}` (sincronizarea REST din A1). Textele Swagger au acum și codul 503.
- `application.properties` (**modificat**):
  - adresa Kafka;
  - `group-id = device-service`;
  - `auto-offset-reset = earliest`;
  - timeout-urile producătorului.

### `monitoring_management` (serviciu **nou**, aceeași structură ca celelalte)
- `pom.xml`: web, JPA, validation, PostgreSQL, Flyway, `spring-kafka`, springdoc (Swagger), plus `spring-boot-starter-test` pentru teste.
- `Dockerfile`: multi-stage ca la celelalte. Maven construiește jar-ul, apoi jar-ul rulează pe `eclipse-temurin:17-jre-alpine` (port 8084).
- `MonitoringApplication.java`: clasa `main`. Importă handler-ul de erori comun, definește butonul Authorize din Swagger și setează fusul orar UTC (ca la celelalte).
- `controllers/ConsumptionController.java`: `GET /consumption/user/{userId}?deviceId=&date=`. `userId` e în path ca gateway-ul să poată verifica proprietarul token-ului. `date` e citit ca `LocalDate` (format ISO).
- `controllers/IndexController.java`: `GET /` → „Monitoring Service is running...”, ca la celelalte servicii.
- `dtos/MeasurementDTO.java`: mesajul simulatorului. `@JsonProperty("device_id")` și `@JsonProperty("measurement_value")` leagă numele din JSON de câmpurile Java.
- `dtos/HourlyConsumptionDTO.java`: o bară din grafic: `hour` (0–23) și `energyKwh`.
- `entities/Device.java`: copia locală a unui dispozitiv (id + proprietar). Id-ul nu e generat, e același ca în device-service.
- `entities/HourlyConsumption.java`: totalul unei ore pentru un dispozitiv. `id`-ul e generat de PostgreSQL (`IDENTITY`/`BIGSERIAL`).
- `repositories/DeviceRepository.java`: are două metode proprii:
  - `deleteDirect`: un singur `DELETE`, iar consumul se șterge prin CASCADE;
  - `unassignUser`: `UPDATE … SET user_id = NULL`.
- `repositories/HourlyConsumptionRepository.java`: `findByDeviceIdAndHourStart` (rândul unei ore) și `findByDeviceIdAndHourStartBetween` (orele unei zile). Spring generează SQL-ul din numele metodelor.
- `services/ConsumptionService.java`: logica principală:
  - `addMeasurement` rotunjește ora în jos, găsește sau creează rândul și adună valoarea;
  - `getDailyConsumption` verifică proprietarul (403/404) și întoarce 24 de valori.
- `services/DeviceService.java`: ține la zi tabela locală `device` (`saveDevice`, `deleteDevice`, `unassignUser`).
- `kafka/MeasurementListener.java`: `@KafkaListener` pe `device-measurements`:
  - afișează fiecare mesaj;
  - transformă JSON-ul în `MeasurementDTO`;
  - sare peste mesajele stricate sau incomplete;
  - apelează `addMeasurement`.
- `kafka/SyncEventListener.java`: `@KafkaListener` pe `sync-events` (grupul `monitoring-service`):
  - `DEVICE_CREATED`/`UPDATED` → `saveDevice`;
  - `DEVICE_DELETED` → `deleteDevice`;
  - `USER_DELETED` → `unassignUser`.
- `resources/application.properties`:
  - DB-ul `monitoring_sd` (port 5435 pe gazdă);
  - Kafka cu `group-id = monitoring-service` și `earliest`;
  - `show_sql=false`, ca mesajele Kafka să se vadă în consolă;
  - Swagger la `/swagger-ui`.
- `db/migration/V1__init_monitoring.sql`: tabelele `device` și `hourly_consumption` (FK cu `ON DELETE CASCADE`, `UNIQUE (device_id, hour_start)`).
- `db/migration/V2__insert_sample_devices.sql`: cele 4 dispozitive seed din device-service, cu aceleași id-uri și proprietari. Ele au fost create printr-o migrație, deci nu au trimis niciodată evenimente.
- `test/.../ConsumptionServiceTest.java`: 4 teste unitare cu repository-uri mock (Mockito):
  - valoarea se adună la ora ei;
  - dispozitivul necunoscut e ignorat;
  - întoarce mereu 24 de valori;
  - dispozitivul altcuiva dă 403.

### `simulator` (modul **nou**, aplicație de consolă fără Spring)
- `pom.xml`:
  - dependențele `kafka-clients` (producătorul), `jackson-databind` (JSON) și `slf4j-simple` (logurile clientului Kafka);
  - pluginul `spring-boot-maven-plugin` pune totul într-un singur jar rulabil, `target/simulator.jar`.
- `Simulator.java`: `main`:
  - citește `config.properties` (sau fișierul dat ca argument) și creează `KafkaProducer`;
  - în buclă: creează măsurătoarea (`LinkedHashMap` → JSON), o trimite cu cheia `device_id`, așteaptă confirmarea, afișează `Sent: …`, avansează ora cu 10 minute și doarme `send.interval.seconds`.
- `EnergyGenerator.java`: formula valorilor: sarcină de bază aleatoare × factorul orei (noaptea mic, seara mare) × fluctuație de ±10 %, rotunjit la 4 zecimale.
- `config.properties`: `device.id`, `kafka.bootstrap.servers=localhost:9092`, `kafka.topic`, `send.interval.seconds` (600 = timp real, 1 = demo), `start.time` (gol = acum).
- `src/main/resources/simplelogger.properties`: clientul Kafka afișează doar warning-uri și erori, ca în consolă să se vadă mesajele trimise.

### `frontend`
- `package.json` / `package-lock.json` (**modificat**): `chart.js` 4.6.1, singura bibliotecă nouă.
- `src/app/monitoring.service.ts` (**nou**): `getDailyConsumption(userId, deviceId, date)` face `GET /monitoring/consumption/user/{userId}` cu parametrii `deviceId` și `date`. URL relativ: trece prin Traefik și interceptorul pune token-ul.
- `src/app/pages/my-devices.component.ts` (**modificat**):
  - secțiunea Consumption (`<select>`, `<input type="date">`, butonul Show, `<canvas>`);
  - `drawChart()` distruge graficul vechi și desenează unul nou cu bare;
  - funcția `today()` dă data de azi în formatul `YYYY-MM-DD`.
- `src/app/pages/admin-devices.component.ts` (**modificat**): coloana **Id**, de unde copiezi id-ul pentru simulator.
- `src/app/app.routes.ts` (**modificat**): doar comentariul. Rutele Angular nu au voie să înceapă cu `/monitoring`.
- `nginx.conf` (**modificat**): `/monitoring` e și el prefix de API. Dacă serviciul e oprit, nginx răspunde 503 în loc de `index.html`.

### Rădăcina proiectului, deployment, documentație
- `pom.xml` (**modificat**): modulele noi `services/monitoring_management` și `simulator`.
- `docker-compose.yml` (**modificat**):
  - serviciile `kafka` (listeners INTERNAL/EXTERNAL, healthcheck), `monitoring-db` și `monitoring-service` (labels Traefik cu `jwt-auth` și strip `/monitoring`, plus router public pentru Swagger);
  - volumul `monitoring-pgdata`;
  - `KAFKA_BOOTSTRAP_SERVERS` și `depends_on: kafka` la auth-service, user-service și device-service;
  - au dispărut `USER_SERVICE_URL` (auth-service) și `AUTH_SERVICE_URL` (user-service).
- `frontend/src/app/pages/admin-users.component.ts` (**modificat**): după „New user” așteaptă 500 ms înainte să reîncarce lista (persoana apare prin Kafka).
- `README.md` (**modificat**): arhitectura cu Kafka, topicurile, sincronizarea, rularea simulatorului, URL-uri, porturi, exemple curl.
- `docs/deployment.puml` + `docs/deployment.png` (**modificat**): diagrama UML de deployment cu `kafka` (cele două topicuri), `monitoring-service`, `monitoring-db` și simulatorul pe gazdă.
- `db/monitoring_sd.sql` (**nou**): `pg_dump` al bazei de monitorizare: dispozitivele seed și o zi simulată (2026-11-21) pentru *Fridge*-ul lui ion.
- `docs/A2_NOTES.md` (**nou**): acest document.

---

## 7. Decizii și cazuri limită

- **De ce Kafka și nu RabbitMQ?**
  - Kafka păstrează mesajele, deci un serviciu pornit mai târziu sau căzut recuperează tot;
  - un singur topic servește mai multe servicii (câte un consumer group pentru fiecare);
  - e făcut pentru fluxuri mari de date de la senzori.
  - Dezavantaj: rutarea e mai simplă decât la RabbitMQ.
- **De ce nu există HTTP direct între auth-service și user-service?**
  - în diagrama conceptuală din PDF, Authorization Service și User Management sunt legate **doar** de
    Synchronization Broker;
  - fără apeluri directe, serviciile nu depind unul de altul: dacă user-service e oprit, Register merge în continuare,
    iar persoana se creează când user-service pornește (mesajul așteaptă în Kafka);
  - auth-service nu mai are nevoie de id-ul înapoi de la user-service, pentru că **îl generează el** (UUID) și îl trimite
    în eveniment. user-service îl folosește ca cheie (`Person.id` nu mai e generat).
- **Consistență eventuală la Register:** `/auth/register` răspunde imediat ce login-ul e salvat și evenimentul e în
  Kafka. Persoana apare în user-service câteva milisecunde mai târziu. De aceea pagina de admin așteaptă 500 ms înainte
  să reîncarce lista de utilizatori. Login-ul funcționează imediat (e în auth-service).
- **Consistență eventuală la ștergere:** login-ul dispare câteva milisecunde după ce `DELETE` răspunde 204.
  Un token deja emis rămâne valid până expiră (60 min), la fel ca înainte.
- **Evenimente vechi:** grupurile noi `auth-service` și `user-service` citesc topicul de la început (`earliest`).
  `USER_CREATED`-urile de dinainte (publicate de user-service, doar cu id) nu au profil, deci user-service le sare
  („USER_CREATED without a profile, skipped”).
- **De ce `userId` în URL-ul de consum?** Ca `AccessPolicy` să compare exact calea cu id-ul din token (`path.equals`),
  ca în A1. Gateway-ul nu știe ce dispozitive are cineva, așa că monitoring-service verifică în tabela lui locală
  dacă dispozitivul e al acelui utilizator.
- **De ce publicăm în `@Transactional` și așteptăm `.get()`?** Ca o modificare să fie ori în DB **și** în Kafka,
  ori în niciunul. Fără `.get()` am afla prea târziu că mesajul nu a plecat.
- **De ce timeout-uri de 10 s la producător?** Implicit producătorul reîncearcă 2 minute. Dacă renunțăm să așteptăm după
  10 s dar el mai reîncearcă, mesajul unei modificări deja anulate ar putea ajunge totuși. Am testat: cu timeout-urile
  noi nu apare niciun eveniment fantomă.
- **Livrare „at-least-once”:** offsetul se confirmă după procesare. Dacă monitoring-service cade exact între salvarea
  în DB și confirmarea offsetului, mesajul vine din nou și ar fi adunat de două ori. Evenimentele de sincronizare sunt
  **idempotente** (a doua oară nu schimbă nimic). Măsurătorile nu sunt, dar cazul e rar. Soluția completă ar fi
  să ținem minte ultimul `timestamp` procesat pentru fiecare dispozitiv.
- **Consistență eventuală:** între publicarea `USER_CREATED` și inserarea în `app_user` trec câteva milisecunde.
  Dacă adminul asignează un dispozitiv exact în acel moment, primește 404. Ce am măsurat în test: ~170 ms.
- **Măsurători pentru dispozitive necunoscute** sunt ignorate și scrise în log. Un dispozitiv creat înainte de A2
  (dacă există în volumul vechi) devine cunoscut dacă îl salvezi o dată la admin (Edit → Save trimite `DEVICE_UPDATED`).
- **Un broker, o partiție, fără volum pentru Kafka:** suficient pentru temă. La `docker compose down` mesajele din
  Kafka se pierd, dar totalurile sunt în `monitoring-db`. Pentru producție ar fi nevoie de 3 brokeri și replicare.

---

## 8. Demo la evaluare

> Dacă Docker rulează direct în WSL (fără Docker Desktop), **ține o fereastră WSL deschisă**. Când nu e deschisă
> nicio fereastră WSL, distribuția se oprește și oprește și containerele.

1. `docker compose up --build -d`, apoi `docker compose ps` (toate „Up”, Kafka și DB-urile „healthy”).
2. Ține deschise logurile: `docker compose logs -f auth-service user-service device-service monitoring-service`.
3. Creează un client (Register) și arată `Published sync event: USER_CREATED` (auth-service), `Person … was inserted`
   (user-service) și `Received sync event` + `was added to app_user` (device-service). În DB:
   `docker exec device-postgres psql -U postgres -d device_sd -c "SELECT encode(id,'hex') FROM app_user;"`
4. Ca admin, creează un dispozitiv pentru el și arată `DEVICE_CREATED` + `saved with owner` (monitoring-service).
   Copiază id-ul din coloana **Id**.
5. Pune id-ul în `simulator/config.properties`, cu `send.interval.seconds=1` și `start.time=<azi>T00:00`, și pornește
   `java -jar simulator/target/simulator.jar simulator/config.properties`. Arată `Sent: …` în simulator și
   `Received measurement: …` în monitoring-service.
6. În DB: `docker exec monitoring-postgres psql -U postgres -d monitoring_sd -c "SELECT hour_start, energy_kwh FROM hourly_consumption ORDER BY 1;"`
7. Loghează-te cu clientul, la **My devices → Consumption** alege dispozitivul și ziua, apoi **Show**: apare graficul.
8. Arată 403: un alt client nu poate citi consumul acestui dispozitiv (exemplele curl din README).

### Alte întrebări probabile (răspunsuri scurte)

- *Ce se întâmplă dacă monitoring-service e oprit cât rulează simulatorul?* Mesajele rămân în Kafka. La pornire
  serviciul continuă de la ultimul offset confirmat, deci nu se pierde nimic.
- *Ce se întâmplă dacă Kafka e oprit?*
  - simulatorul se oprește cu eroare după timeout-ul implicit al clientului Kafka (1–2 minute);
  - crearea sau ștergerea de utilizatori/dispozitive răspunde 503 și nu se salvează nimic.
- *De ce două listeners în Kafka?* Containerele găsesc brokerul la `kafka:29092`, iar gazda (simulatorul) la `localhost:9092`.
  Brokerul trebuie să le spună fiecăruia adresa pe care o poate folosi (*advertised listeners*).
- *De ce cheia mesajului = id-ul dispozitivului?* Mesajele cu aceeași cheie merg în aceeași partiție, deci sunt procesate
  în ordine și de un singur consumator. Altfel două replici ar putea actualiza în același timp rândul aceleiași ore.
- *Ce este un consumer group și cum ai obținut publish-subscribe?* Un grup e „un serviciu” (oricâte replici ar avea).
  Fiecare grup primește toate mesajele, iar în interiorul grupului fiecare mesaj e procesat o singură dată.
  device-service și monitoring-service au grupuri diferite, deci amândouă primesc toate evenimentele de sincronizare.
- *Unde se ține offsetul?* În topicul intern `__consumer_offsets` din Kafka, pentru fiecare grup și partiție.
- *Ce este KRaft?* Modul în care Kafka își ține singur metadatele (Raft), fără ZooKeeper.
  La noi un singur nod e și broker, și controller.
- *Cum arată mesajul?* `{"timestamp":"2026-11-22T18:40:00","device_id":"…","measurement_value":0.1234}`. JSON trimis ca text,
  cu cheia = `device_id`.
- *De ce 24 de valori și nu doar orele cu date?* Ca graficul să aibă mereu axa OX completă (0–23). Orele fără date au 0.

## 9. Kafka UI (vizualizarea Kafka în browser)

**Ce este:** o aplicație web (`kafbat/kafka-ui`, containerul `kafka-ui`) care se conectează la broker prin listenerul
intern `kafka:29092` și arată tot ce e în Kafka: topicuri, mesaje, partiții, offseturi și consumer groups.
Este doar un instrument de dezvoltare/demo: nu trece prin Traefik și aplicația nu depinde de el.

**Cum îl deschid:** `docker compose up -d` îl pornește odată cu restul (după ce Kafka e healthy), apoi
http://localhost:8085. Clusterul se numește `local`.

**Ce arăt la evaluare:**
1. **Topics** – apar `device-measurements` (date de la simulator) și `sync-events` (sincronizare utilizatori/dispozitive).
   Topicurile interne (`__consumer_offsets`) se văd dacă bifez *Show Internal Topics*.
2. **Mesaje** – intru pe `device-measurements` → tab-ul **Messages**: văd fiecare mesaj JSON, cheia (= `device_id`),
   partiția, offsetul și ora. Pornesc simulatorul și dau refresh: apar mesaje noi. La fel pe `sync-events`
   după ce creez un utilizator sau un dispozitiv (`USER_CREATED`, `DEVICE_CREATED`).
3. **Partiții și offseturi** – în tab-ul **Overview** al topicului: numărul de partiții, iar pentru fiecare partiție
   primul și ultimul offset (câte mesaje are).
4. **Consumer groups** – meniul **Consumers**: grupurile `auth-service`, `user-service`, `device-service` și `monitoring-service`, starea `STABLE`,
   membrii activi și, pe fiecare partiție, offsetul confirmat și **lag**-ul (câte mesaje nu au fost încă citite).
   Demo util: opresc `monitoring-service`, trimit mesaje cu simulatorul → lag-ul crește; îl repornesc → lag-ul
   revine la 0, adică nu s-a pierdut nimic.
