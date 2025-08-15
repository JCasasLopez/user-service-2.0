# User Service 2.0

An authentication and user management microservice that uses **JWTs** (JSON Web Tokens), a classic refresh token system, and **Spring Security** to implement login and logout capabilities, along with typical account-related features (see *API* section for details).

The microservice prioritizes security through:

- **Short-lived, purpose-specific JWTs** (verification, access, and refresh).
- **Account lockout mechanism** after 3 failed login attempts (tracked via Redis) to prevent brute-force attacks.
- **Automatic unlock** after 24 hours (default values are configurable).
- **Password encryption** using BCrypt.
- **Role-based endpoint restrictions**.

Basic validation rules are enforced, such as:

- Unique usernames and emails.
- Strong password requirements (see *Security Features* section for details).

All login attempts —whether failed or successful— are logged in a separate database table to enable future analysis for security monitoring and usage patterns.  
This version of the microservice does not yet make use of those records.

## Tech Stack

- **Java 17**
- **Spring Boot 3**
- **Spring Security**
- **Spring Data JPA**
- **MySQL 8**
- **Redis 7**
- **JWT** (JSON Web Tokens)
- **JJWT** Library

### Testing

- **JUnit 5**
- **Mockito**
- **MockMvc**
- **TestRestTemplate**

### Infrastructure

- **Docker** – Containerization
- **Docker Compose** – Multi-container orchestration
- **MySQL** & **Redis** containers for local and production environments
- **Environment variables** for sensitive configuration (see next point)
- HTTPS *(planned, see [Design Decisions](#design-decisions))*

### Prerequisites

- Docker & Docker Compose
- Copy `.env.example` to `.env` and fill in your values (do not commit real secrets).

**Required variables**
- `JWT_SECRETKEY` — Base64 encoded secret key (512 bits recommended for HS512).
- `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` — Required only outside demo mode.

> ⚠️ Never publish real credentials in the repo or README.



