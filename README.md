# Card360 — Card Lifecycle Management

Card360 is a small, runnable card-management system for demonstrating a legacy-to-modern application journey. It supports customer and card search, card activation, blocking, unblocking, replacement, transaction viewing, and business-rule validation.

The application is self-contained: its database runs inside the application, so a separate MySQL installation is not required.

## Features

- Search customers by ID or name
- View debit and credit cards owned by a customer
- View card details and recent transactions
- Activate pending cards
- Block cards with an auditable reason; unblock blocked cards
- Issue a replacement card for a blocked card
- Change card limits through the REST API, with validation and limit history
- View seeded workflow-usage metrics

## Technology stack

| Layer | Technology |
| --- | --- |
| Backend | Java 8, Spring Boot 2.7, Spring MVC |
| REST/data access | Spring JDBC (`JdbcTemplate`) |
| Database | H2 embedded database, in MySQL compatibility mode |
| Frontend | HTML, CSS, vanilla JavaScript |
| Web server | Embedded Tomcat |
| Build tool | Maven |

## Pages

| Page | URL | Purpose |
| --- | --- | --- |
| Customer search | `/` | Find customers by ID or name |
| Customer overview | `/customer.html?id=1001` | Review a customer's cards |
| Card details | `/card.html?id=2001` | View transactions and complete card actions |

## Run locally

Prerequisite: Java 8 and Maven 3.6+.

```bash
git clone <your-repository-url>
cd card360
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Search for the seeded customer ID `1001`.

Maven dependencies are stored in `work/.m2` within the project; no write permission to your home Maven directory is needed.

## Database

The H2 database is stored locally under `data/` after the first run. It is excluded from Git. The schema is initialized from `src/main/resources/schema.sql`, with sample data in `src/main/resources/data.sql`.

The H2 console is available at [http://localhost:8080/h2-console](http://localhost:8080/h2-console):

```text
JDBC URL: jdbc:h2:file:./data/card360;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
User:     sa
Password: <blank>
```

## Business rules demonstrated

1. Only a pending, unexpired card can be activated.
2. Only an active card can be blocked.
3. A block reason is mandatory.
4. Only a blocked card can be unblocked.
5. A replacement card receives a new card ID and is issued pending activation.
6. A credit limit cannot be lower than the outstanding balance.

## REST API

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/v1/customers?query=1001` | Search customers |
| `GET` | `/api/v1/customers/{customerId}` | Customer and cards |
| `GET` | `/api/v1/cards/{cardId}` | Card and transactions |
| `PATCH` | `/api/v1/cards/{cardId}/activate` | Activate a card |
| `PATCH` | `/api/v1/cards/{cardId}/block` | Block a card, with `reason` |
| `PATCH` | `/api/v1/cards/{cardId}/unblock` | Unblock a card |
| `POST` | `/api/v1/cards/{cardId}/replacement` | Issue replacement, with optional `reason` |
| `PATCH` | `/api/v1/cards/{cardId}/limit` | Change credit limit |
| `GET` | `/api/v1/usage` | Example usage metrics |

## Switching to MySQL later

The tables and SQL were designed to be close to MySQL. To migrate, replace the H2 dependency with MySQL Connector/J and update `spring.datasource.*` in `src/main/resources/application.properties`. The UI and REST API can remain unchanged.
