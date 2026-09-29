# Cinema Booking and Management Application

## Project Overview

The **Cinema** application is a robust, production-grade backend service built with **Java 24** and **Spring Boot
4.1.1**, adhering to **Clean Architecture / Hexagonal Architecture** principles. It provides comprehensive cinema
booking and management capabilities, including:

- **Movie & Screening Management:** Atomic creation, update, publication lifecycle (`isPublished`), and media management
  (posters and video/YouTube trailers).
- **State-Driven Booking Flow:** Robust state pattern (`BookingFlowContext`, `SelectMovieState`, `SelectScreeningState`,
  `SelectSeatsState`, `LoginRequiredState`, `AuthenticatedState`, `PaymentState`, `ConfirmedState`) managing guest
  checkout, 15-minute temporary seat holding, payment processing, QR code generation, and email confirmation.
- **Dynamic Role-Based Access Control:** Database-backed roles (`CLIENT`, `ADMIN`) with double security verification
  (JWT signature + database role check) and role elevation endpoints.
- **Storage & Infrastructure:** S3-compatible RustFS storage integration for media files, Flyway database migrations for
  PostgreSQL, ZXing for ticket QR code generation, and scheduled background tasks for expired booking cleanup.

## Technology Stack

- **Language:** Java 24
- **Framework:** Spring Boot 4.1.1 (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`,
  `spring-boot-starter-security`, `spring-boot-starter-flyway`)
- **Database:** PostgreSQL with Flyway migrations (`src/main/resources/db/migration`)
- **Storage:** AWS SDK for Java (S3 compatible RustFS storage)
- **Utilities:** ZXing (QR code generation)
- **Testing:** JUnit 5, Spring Boot Test, H2 Database (for test profiles), Testcontainers / Integration test harness
  (`AbstractIntegrationTest`)

## Architecture & Package Structure

The codebase is organized following Hexagonal / Clean Architecture:

- `com.cinema.cinema.domain`: Core domain models (`Booking`, `Movie`, `Screening`, `User`, etc.), state machine
  (`domain/model/state`), and domain events (`domain/event/MediaUploadedEvent`).
- `com.cinema.cinema.port`: Hexagonal boundaries defining input use cases (`port/in`) and output SPI ports (`port/out`).
- `com.cinema.cinema.service`: Application business logic implementations coordinating use cases.
- `com.cinema.cinema.infrastructure`: Framework-specific implementations, including:
    - `adapter/in/web`: REST controllers, request/response DTOs, and global handlers.
    - `adapter/out`: Persistence adapters (`PostgresMovieAdapter`, `PostgresBookingAdapter`, etc.), S3/RustFS storage
      adapter, notification adapter, QR code adapter (`ZxingQrCodeAdapter`), and payment gateway adapter.
    - `config`: Security, async, application, and S3 configurations.
    - `security`: JWT filter and authentication mechanisms.

## Building and Running

### Prerequisites

- Java 24 JDK installed.
- Docker & Docker Compose (optional, for running PostgreSQL / RustFS infrastructure).

### Key Maven Commands

- **Build the project:**
  ```bash
  ./mvnw clean install
  ```
- **Run the application:**
  ```bash
  ./mvnw spring-boot:run
  ```
- **Run tests:**
  ```bash
  ./mvnw test
  ```

### Docker Infrastructure

Start local dependencies (PostgreSQL, RustFS) via Docker Compose:

```bash
docker-compose up -d
```

## Development Conventions & Guidelines

- **Architecture Boundary Enforcement:** Keep domain logic pure and independent of infrastructure frameworks. Use
  input/output ports for all external interactions.
- **Database Migrations:** All schema changes must be managed via versioned Flyway migration scripts (`V1__...` through
  `V5__...`) located in `src/main/resources/db/migration`.
- **Testing Standards:**
    - Unit tests (`*Test.java`) reside alongside domain and service components.
    - Integration tests extend `AbstractIntegrationTest` to validate end-to-end flows, security enforcement, and
      database integrity.
- **Security:** Ensure all administrative endpoints enforce both valid JWT authentication and database-backed role
  validation. Never bypass authorization checks.
