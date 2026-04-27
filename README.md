# expense-tracker-app-microservices

Expense Tracker APP using Microservices - Spring Boot, Kafka, Redis, MySQL, Kong API Gateway, Docker, Python Flask, React Native



\# Expense Tracker — Microservices Backend



A production-style distributed expense tracking system built from scratch using Java Spring Boot microservices, Python Flask, Apache Kafka, Redis, MySQL, and Kong API Gateway — fully containerized with Docker.



The core idea: a user sends a raw bank SMS to the app. The DS Service parses it using Mistral AI, extracts structured expense data, and publishes it to Kafka. The Expense Service consumes the event and persists it to MySQL — all asynchronously, with no direct service-to-service HTTP calls for the core flow.



\---



\## Architecture Overview

React Native App

│

▼

Kong API Gateway  ←── Custom Lua Auth Plugin

│                    │

│              Auth Service (/ping - JWT validation)

│

├──► Auth Service    (signup, login, token management)

├──► User Service    (user profile, Redis idempotency)

├──► Expense Service (expense CRUD, Kafka consumer)

└──► DS Service      (SMS parsing via Mistral AI → Kafka producer)



\### Event Flow

Mobile App → Kong → DS Service → Kafka → Expense Service → MySQL



\---



\## Services



\### Auth Service · Java Spring Boot · Port 9898



Handles all authentication. On signup, publishes user info to Kafka so the User Service can asynchronously create the user profile — no direct HTTP call between services.



\- JWT access tokens (24hr expiry, HS256 signed)

\- Refresh token support for seamless re-authentication

\- userId generated as UUID, embedded in JWT claims

\- Kafka producer: publishes `UserInfo` on registration

\- Exposes `/auth/v1/ping` — used by Kong plugin to validate every inbound JWT



\### User Service · Java Spring Boot · Port 9810



Consumes user registration events from Kafka and maintains user profiles.



\- Kafka consumer: listens on `testing\_json` topic

\- Redis idempotency: prevents duplicate user creation if Kafka delivers the same event twice

\- Custom deserializer for `UserInfo` Kafka messages



\### Expense Service · Java Spring Boot · Port 9820



Core expense management service. Handles both direct API calls and async Kafka events.



\- Full CRUD: create, read, update, delete expenses

\- Date range queries for expense history

\- Kafka consumer: listens on `expense\_service` topic

\- Custom deserializer for `ExpenseDto` Kafka messages

\- Persists to MySQL via Spring Data JPA



\### DS Service · Python Flask · Port 8000



The AI-powered data ingestion layer. Accepts raw bank SMS messages and converts them into structured expense events.



\- Receives raw SMS text via REST API

\- Parses using Mistral AI (via LangChain)

\- Extracts: `amount`, `merchant`, `currency`

\- Publishes structured expense event to Kafka

\- Expense Service consumes and persists automatically



\### Kong API Gateway · Port 8005 (proxy) · Port 7990 (admin)



All client traffic enters through Kong. Runs in DB-less mode with declarative config.



\- Declarative routing via `config/kong.yml`

\- Custom Lua plugin (`custom-auth`) validates JWT on every protected route

\- Auth plugin calls `Auth Service /auth/v1/ping` to validate token and extract userId

\- Public routes: `/auth/v1/signup`, `/auth/v1/login`, `/auth/v1/refreshToken`

\- All other routes: protected, require valid JWT



\---



\## Authentication Flow

Client Request (Authorization: Bearer <token>)

│

▼

Kong API Gateway

│

▼

Custom Lua Plugin → POST auth-service:9898/auth/v1/ping

│

┌─────────┴─────────┐

Valid               Invalid

│                   │

Forward to service      Return 401

with userId



\---



\## Tech Stack



| Layer | Technology |

|---|---|

| Backend | Java 21, Spring Boot, Gradle |

| AI / ML Service | Python 3.11, Flask, Mistral AI, LangChain |

| Message Broker | Apache Kafka (KRaft mode — no Zookeeper) |

| Cache | Redis 7 |

| Database | MySQL 8.3 |

| API Gateway | Kong (DB-less, custom Lua plugin) |

| Containerization | Docker, Docker Compose |

| Mobile | React Native (CLI) |



\---



\## Infrastructure



All services run in isolated Docker containers on a shared bridge network (`backend-network`).



| Container | Image | Port |

|---|---|---|

| auth-service | custom build | 9898 |

| user-service | custom build | 9810 |

| expense-service | custom build | 9820 |

| ds-service | custom build | 8000 |

| mysql-container | mysql:8.3 | 3306 |

| redis | redis:7 | 6379 |

| kafka-kraft | apache/kafka | 9092 |

| kong | kong | 8005 |



Kong runs in a separate compose file (`kong.yml`) and joins the backend network as an external network — this keeps infrastructure concerns separated.



\---



\## API Reference



\### Auth (Public)

| Method | Route | Description |

|---|---|---|

| POST | `/auth/v1/signup` | Register new user |

| POST | `/auth/v1/login` | Login, returns JWT + refresh token |

| POST | `/auth/v1/refreshToken` | Get new access token |



\### User (Protected)

| Method | Route | Description |

|---|---|---|

| POST | `/user/v1/createUpdate` | Create or update user profile |

| GET | `/user/v1/getUser` | Get user details |



\### Expense (Protected)

| Method | Route | Description |

|---|---|---|

| POST | `/expense/v1/create` | Create expense manually |

| GET | `/expense/v1/get` | Get all expenses |

| GET | `/expense/v1/get/range` | Get expenses by date range |

| PUT | `/expense/v1/update` | Update expense |

| DELETE | `/expense/v1/delete` | Delete expense |

| POST | `/expense/v1/addExpense` | Add expense via Kafka event |



\### DS Service (Protected)

| Method | Route | Description |

|---|---|---|

| POST | `/v1/ds/message` | Send raw SMS for AI parsing |



\---



\## How to Run



\### Prerequisites

\- Docker and Docker Compose

\- Mistral AI API key (get one at console.mistral.ai)



\### Setup



\*\*1. Clone the repository\*\*

```bash

git clone https://github.com/Meraj-del/expense-tracker-app-microservices.git

cd expense-tracker-app-microservices

```



\*\*2. Configure environment\*\*

```bash

cp docker-compose.example.yml docker-compose.yml

\# Fill in: MYSQL\_ROOT\_PASSWORD, MYSQL\_PASSWORD, MISTRAL\_API\_KEY

```



\*\*3. Add application.properties to each Spring Boot service\*\*

```bash

\# Use the .example files as reference

cp AuthService1/app/src/main/resources/application.properties.example \\

&#x20;  AuthService1/app/src/main/resources/application.properties



cp ExpenseService/src/main/resources/application.properties.example \\

&#x20;  ExpenseService/src/main/resources/application.properties



cp userservice/src/main/resources/application.properties.example \\

&#x20;  userservice/src/main/resources/application.properties

```



\*\*4. Build jars and place in correct directories\*\*

auth-jar/app.jar

expense-jar/ExpenseService-0.0.1-SNAPSHOT.jar

user-jar/userservice-0.0.1-SNAPSHOT.jar

jar/ds\_service-1.0.tar.gz



\*\*5. Start all containers\*\*

```bash

chmod +x start-all.sh

./start-all.sh

```



\*\*6. Stop all containers\*\*

```bash

./start-all.sh stop

```



\---



\## Security Notes

\- All secrets managed via environment variables — never hardcoded

\- `application.properties` excluded from version control

\- `docker-compose.yml` excluded from version control

\- Use `\*.example` files as templates

\- JWT signed with HS256, validated on every request via Kong



\---



\## Author

Md Meraj · Java Backend Developer

\[GitHub](https://github.com/Meraj-del)

