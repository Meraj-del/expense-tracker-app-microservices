# Expense Tracker — Microservices Backend

A production-grade distributed expense tracking system built with Java Spring Boot microservices, Python Flask, Apache Kafka, Redis, MySQL, and Kong API Gateway — fully containerized with Docker.

**Core flow:** User sends a raw bank SMS → DS Service parses it using Mistral AI → extracts structured expense data → publishes to Kafka → Expense Service consumes and persists to MySQL. All asynchronously. No direct service-to-service HTTP calls in the core flow.

---

## Architecture
React Native App
│
▼
Kong API Gateway (Port 8005)
│
├── Custom Lua Auth Plugin
│         └── validates JWT via Auth Service /ping
│
├──► Auth Service      (Port 9898) — signup, login, token management
├──► User Service      (Port 9810) — user profiles, Redis idempotency
├──► Expense Service   (Port 9820) — expense CRUD, Kafka consumer
└──► DS Service        (Port 8000) — SMS parsing via Mistral AI

### Async Event Flow
Mobile App → Kong → DS Service → Kafka (expense_service topic)
→ Expense Service → MySQL
Mobile App → Kong → Auth Service → Kafka (testing_json topic)
→ User Service → MySQL

---

## Services

### Auth Service · Java Spring Boot · Port 9898

Handles all authentication and token management.

- JWT access tokens — 24hr expiry, HS256 signed
- Refresh token rotation with 7-day expiry
- UUID-based userId embedded in JWT claims
- On signup: publishes UserInfo event to Kafka (`testing_json` topic)
- Exposes `/auth/v1/ping` — Kong plugin calls this to validate every inbound JWT
- Spring Security filter chain with custom JWT filter
- RBAC via `@PreAuthorize` annotations
- BCrypt password hashing

### User Service · Java Spring Boot · Port 9810

Consumes user registration events from Kafka and maintains user profiles.

- Kafka consumer on `testing_json` topic
- Redis distributed lock — prevents duplicate processing if Kafka redelivers
- Manual offset commit — offset only committed after successful DB write
- Custom Kafka deserializer for UserInfo events
- Create or update user profile in single idempotent operation

### Expense Service · Java Spring Boot · Port 9820

Core expense management. Handles both direct API calls and async Kafka events.

- Full CRUD: create, read, update, delete
- Date range queries for expense history
- Kafka consumer on `expense_service` topic
- Composite index on `(user_id, created_at)` — optimized for date range queries
- Custom Kafka deserializer for ExpenseDto events
- Spring Data JPA with MySQL

### DS Service · Python Flask · Port 8000

AI-powered data ingestion layer. Converts raw bank SMS into structured expense events.

- Accepts raw SMS text via REST
- Keyword detection to filter non-bank messages
- Mistral AI via LangChain — extracts amount, merchant, currency
- Lazy LLM initialization — service starts even if Mistral is unreachable
- Publishes structured event to Kafka (`expense_service` topic)
- Kafka producer with retry and timeout config

### Kong API Gateway · Port 8005 (proxy) · Port 7990 (admin)

All client traffic enters through Kong. Runs in DB-less declarative mode.

- Declarative routing via `config/kong.yml`
- Custom Lua plugin (`custom-auth`) validates JWT on every protected route
- Auth plugin calls `auth-service:9898/auth/v1/ping` to validate token
- Public routes: `/auth/v1/signup`, `/auth/v1/login`, `/auth/v1/refreshToken`
- All other routes: protected, require valid Bearer token

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend Services | Java 21, Spring Boot 3.x, Gradle |
| Security | Spring Security, JWT (JJWT), BCrypt |
| AI / ML | Python 3.11, Flask, Mistral AI, LangChain |
| Message Broker | Apache Kafka (KRaft mode — no Zookeeper) |
| Cache / Lock | Redis 7 (idempotency + distributed locking) |
| Database | MySQL 8.3, Spring Data JPA, Hibernate |
| API Gateway | Kong (DB-less, custom Lua plugin) |
| Containerization | Docker, Docker Compose |
| Mobile | React Native (CLI) |

---

## Infrastructure

All services run in Docker containers on a shared bridge network (`backend-network`).

| Container | Image | Port |
|---|---|---|
| auth-service | custom build | 9898 |
| user-service | custom build | 9810 |
| expense-service | custom build | 9820 |
| ds-service | custom build | 8000 |
| mysql-container | mysql:8.3 | 3306 |
| redis | redis:7 | 6379 |
| kafka-kraft | apache/kafka | 9092 |
| kong | kong | 8005 / 7990 |

Kong runs in a separate compose file (`kong.yml`) and joins `backend-network` as an external network — keeping infrastructure concerns separated.

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `testing_json` | Auth Service | User Service | User registration events |
| `expense_service` | DS Service | Expense Service | Parsed expense events |

---

## API Reference

### Auth (Public)

| Method | Route | Description |
|---|---|---|
| POST | `/auth/v1/signup` | Register new user, returns JWT + refresh token |
| POST | `/auth/v1/login` | Login with credentials, returns JWT + refresh token |
| POST | `/auth/v1/refreshToken` | Exchange refresh token for new access token |

### User (Protected — requires Bearer token)

| Method | Route | Description |
|---|---|---|
| POST | `/user/v1/createUpdate` | Create or update user profile |
| GET | `/user/v1/getUser` | Get user details |

### Expense (Protected — requires Bearer token)

| Method | Route | Description |
|---|---|---|
| POST | `/expense/v1/create` | Create expense manually |
| GET | `/expense/v1/get?user_id=` | Get all expenses for user |
| GET | `/expense/v1/get/range` | Get expenses by date range |
| PUT | `/expense/v1/update` | Update expense |
| DELETE | `/expense/v1/delete` | Delete expense |
| POST | `/expense/v1/addExpense` | Add expense via gateway header |

### DS Service (Protected — requires Bearer token)

| Method | Route | Description |
|---|---|---|
| POST | `/v1/ds/message` | Send raw SMS for AI parsing and expense extraction |

---

## How to Run

### Prerequisites

- Docker and Docker Compose
- Java 21 + Gradle (for building JARs)
- Python 3.11 (for building DS service)
- Mistral AI API key — get one at [console.mistral.ai](https://console.mistral.ai)

### Setup

**1. Clone the repository**
```bash
git clone https://github.com/Meraj-del/expense-tracker-app-microservices.git
cd expense-tracker-app-microservices
```

**2. Configure Docker environment**
```bash
cp docker-compose.example.yml docker-compose.yml
# Edit docker-compose.yml — fill in:
# MYSQL_ROOT_PASSWORD, MYSQL_PASSWORD, MISTRAL_API_KEY
```

**3. Configure each Spring Boot service**
```bash
cp AuthService1/app/src/main/resources/application.properties.example \
   AuthService1/app/src/main/resources/application.properties

cp ExpenseService/src/main/resources/application.properties.example \
   ExpenseService/src/main/resources/application.properties

cp userservice/src/main/resources/application.properties.example \
   userservice/src/main/resources/application.properties
```

**4. Configure DS Service**
```bash
cp dsService/src/.env.example dsService/src/.env
# Edit .env — add your MISTRAL_API_KEY
```

**5. Build JARs and place in correct directories**
```bash
# Auth Service
cd AuthService1 && gradlew clean build -x test
cp app/build/libs/app.jar ../auth-jar/app.jar

# User Service  
cd userservice && gradlew clean build -x test
cp build/libs/userservice-0.0.1-SNAPSHOT.jar ../user-jar/

# Expense Service
cd ExpenseService && gradlew clean build -x test
cp build/libs/ExpenseService-0.0.1-SNAPSHOT.jar ../expense-jar/

# DS Service
cd dsService && python setup.py sdist
cp dist/ds_service-1.0.tar.gz ../jar/
```

**6. Start all containers**
```bash
chmod +x start-all.sh
./start-all.sh
```

**7. Stop all containers**
```bash
./start-all.sh stop
```

### Verify Setup
```bash
# Should return JWT tokens
curl -X POST http://localhost:8005/auth/v1/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@test.com","password":"Test@1234","firstName":"Test","lastName":"User","phoneNumber":9999999999}'
```

---

## Security

- All secrets managed via environment variables — never hardcoded
- `application.properties` excluded from version control via `.gitignore`
- `docker-compose.yml` excluded — use `docker-compose.example.yml` as template
- JWT signed with HS256, validated on every request via Kong custom plugin
- Passwords hashed with BCrypt
- Redis distributed locking prevents duplicate event processing

---

## Author

**Md Meraj** · Java Backend Developer

[GitHub](https://github.com/Meraj-del) · Open to Backend Engineering roles in Dubai / India

---

*AWS deployment coming soon — EC2 + RDS + S3*