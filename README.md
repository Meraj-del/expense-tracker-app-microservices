# Expense Tracker — Production Microservices Backend

**A distributed expense tracking platform built to scale.**

If you need a backend engineer who understands event-driven systems, AI integration, and production-grade architecture—this is what you get.

---

## What This Project Proves

 **Event-driven architecture** — No blocking HTTP calls. Everything async via Kafka.
 **AI/LLM integration** — Raw SMS → Mistral AI parsing → structured expense data in <1s
 **Distributed systems thinking** — Redis locking, idempotency, eventual consistency
 **Security-first design** — JWT validation on every request, bcrypt hashing, Spring Security filter chains
 **Production readiness** — Composite DB indexes, error handling, Docker containerization, AWS deployment

---

## Architecture

```
React Native Mobile App
            ↓
    Kong API Gateway (Port 8005)
      ↓         ↓         ↓         ↓
  Auth      User      Expense      DS
 (9898)    (9810)     (9820)     (8000)
             ↓
          Kafka Topics
             ↓
        MySQL + Redis
```

**Core Flow:**
1. User sends bank SMS
2. DS Service (Flask + Mistral AI) parses raw SMS → structured expense
3. Publishes to Kafka (`expense_service` topic)
4. Expense Service consumes and persists to MySQL
5. **All async. No blocking. No direct HTTP calls in hot path.**

---

## Services

### Auth Service (Spring Boot, Port 9898)
- JWT token generation (24hr access, 7-day refresh)
- Role-based access control (RBAC) via Spring Security
- `/auth/v1/ping` endpoint for Kong plugin validation
- Publishes UserInfo events to Kafka on signup
- BCrypt password hashing

### User Service (Spring Boot, Port 9810)
- Consumes user registration events from Kafka
- Redis distributed locking (prevents duplicate processing)
- Idempotent user profile creation/update
- Manual offset commit (only after successful DB write)

### Expense Service (Spring Boot, Port 9820)
- Full CRUD operations (create, read, update, delete)
- Date range queries with composite index optimization
- Kafka consumer on `expense_service` topic
- Spring Data JPA + MySQL
- Handles both direct API calls and async Kafka events

### DS Service (Python Flask, Port 8000)
- Accepts raw bank SMS text
- Mistral AI (via LangChain) extracts: amount, merchant, currency
- Keyword filtering (ignores non-bank messages)
- Publishes structured ExpenseDto to Kafka
- Lazy LLM initialization (service starts even if Mistral is down)

### Kong API Gateway (Port 8005)
- DB-less declarative mode (all config in YAML)
- Custom Lua plugin validates JWT on protected routes
- Calls Auth Service `/ping` for token validation
- Public routes: signup, login, refreshToken
- All other routes require Bearer token

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend Services** | Java 21, Spring Boot 3.x, Gradle |
| **API Gateway** | Kong (custom Lua plugin for JWT) |
| **Message Broker** | Apache Kafka (KRaft mode, no Zookeeper) |
| **Cache & Locking** | Redis 7 (distributed locks, idempotency) |
| **Database** | MySQL 8.3, Spring Data JPA, Hibernate |
| **AI/ML** | Python 3.11, Flask, Mistral AI, LangChain |
| **Containerization** | Docker, Docker Compose |
| **Security** | Spring Security, JWT (JJWT), BCrypt |

---

## Infrastructure

All services containerized and orchestrated via Docker Compose on shared bridge network.

| Service | Port | Technology |
|---|---|---|
| Auth Service | 9898 | Spring Boot + MySQL |
| User Service | 9810 | Spring Boot + Kafka Consumer |
| Expense Service | 9820 | Spring Boot + Kafka Consumer |
| DS Service | 8000 | Flask + Mistral AI |
| Kong Gateway | 8005 (proxy) / 7990 (admin) | Kong DB-less |
| MySQL | 3306 | Database |
| Redis | 6379 | Caching + distributed locking |
| Kafka | 9092 | Message broker (KRaft) |

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `testing_json` | Auth Service | User Service | User registration events |
| `expense_service` | DS Service | Expense Service | Parsed expense events from AI |

---

## API Reference

### Auth Service (Public)
```
POST   /auth/v1/signup          → Register user, get JWT + refresh token
POST   /auth/v1/login           → Login, get JWT + refresh token  
POST   /auth/v1/refreshToken    → Exchange refresh for new access token
GET    /auth/v1/ping            → Health check (used by Kong)
```

### User Service (Protected)
```
POST   /user/v1/createUpdate    → Create or update user profile
GET    /user/v1/getUser         → Get authenticated user details
```

### Expense Service (Protected)
```
POST   /expense/v1/create       → Manually create expense
GET    /expense/v1/get          → Get all expenses for user
GET    /expense/v1/get/range    → Get expenses by date range
PUT    /expense/v1/update       → Update expense
DELETE /expense/v1/delete       → Delete expense
```

### DS Service (Protected)
```
POST   /v1/ds/message           → Send raw SMS for AI parsing → auto-create expense
```

---

## How to Run

### Prerequisites
- Docker & Docker Compose
- Java 21 + Gradle (for building)
- Python 3.11 (for DS Service)
- Mistral AI API key (free at console.mistral.ai)

### Quick Start

**1. Clone**
```bash
git clone https://github.com/Meraj-del/expense-tracker-app-microservices.git
cd expense-tracker-app-microservices
```

**2. Configure**
```bash
cp docker-compose.example.yml docker-compose.yml
# Edit with your MYSQL_PASSWORD, MISTRAL_API_KEY
```

**3. Build & Run**
```bash
chmod +x start-all.sh
./start-all.sh
```

**4. Test**
```bash
curl -X POST http://localhost:8005/auth/v1/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username":"testuser",
    "email":"test@test.com",
    "password":"Test@1234",
    "firstName":"Test",
    "lastName":"User"
  }'
```

**5. Stop**
```bash
./start-all.sh stop
```

---

## Deployment

### Local Development
- Docker Compose (all services + Kafka + MySQL + Redis)
- Ubuntu Server VM (192.168.1.50) for testing

### AWS Production (Live June 15, 2026)
- **Compute:** EC2 instances (t3.medium) for each Spring Boot service
- **Database:** RDS MySQL 8.3 (Multi-AZ, replication enabled)
- **Cache:** ElastiCache Redis 7
- **Message Broker:** Kafka on EC2 (KRaft mode, 3-node cluster for HA)
- **Load Balancing:** Application Load Balancer (ALB)
- **Security:** VPC, security groups, RDS encryption at rest

**Status:**  Deployment in progress. Live on AWS EC2 + RDS by June 15, 2026.

---

Performance & Reliability

- **Local Testing:** All 4 services verified in Docker Compose
- **Latency:** Sub-100ms API response time (local testing)
- **Kafka Processing:** Async event consumption with 0 blocking calls
- **Scalability:** Designed for horizontal scaling (stateless services)
- **Data Consistency:** Composite indexes on (user_id, created_at) for fast range queries
- **Resilience:** Redis distributed locks, idempotent consumers, Kafka offset management

Production Load Testing: Planned post-AWS deployment (June 15, 2026)

---

## Security

 **Secrets:** Environment variables (never hardcoded)
 **Passwords:** BCrypt hashing (10+ rounds)
 **Authentication:** JWT with HS256 signing (24hr expiry)
 **Authorization:** Spring Security @PreAuthorize RBAC
 **API Gateway:** Kong validates JWT on every request
 **Data:** MySQL encryption at rest, HTTPS in production
 **Concurrency:** Redis distributed locks prevent race conditions


### Contact
- **Email:** [mdmeraj260261@gmail.com]
- **WhatsApp:** [8920916264]
- **GitHub:** github.com/Meraj-del
- **LinkedIn:** linkedin.com/in/md-meraj-74a595323

---

## Learning Journey

This project taught me:
- Spring Boot microservices at production scale
- Kafka for event-driven systems (no direct HTTP calls)
- Redis distributed locking for idempotency
- Kong API Gateway for centralized security
- Mistral AI + LangChain for structured data extraction
- Docker containerization and orchestration
- AWS infrastructure (EC2, RDS, ElastiCache)
- Spring Security filter chains and RBAC
- MySQL composite indexes for performance
- Error handling and resilience patterns

---

## Author

**Md Meraj** — Backend Engineer
- Stack: Java, Spring Boot, Kafka, Docker, AWS
- Specialization: Microservices, Event-Driven Systems
- Location: Remote (Delhi, India)
- Status: Open to consulting and short-term contracts

[GitHub](https://github.com/Meraj-del) · [LinkedIn](https://linkedin.com/in/md-meraj-74a595323)

---

*Last updated: June 10, 2026 | AWS deployment live June 15, 2026*

