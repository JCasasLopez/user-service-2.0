# User Service 2.0
An authentication and user management microservice that uses **JWTs** (JSON Web Tokens), a classic refresh token system, and **Spring Security** to implement login and logout capabilities, along with typical account-related features (see *API* section for details).

The microservice prioritizes security through short-lived, purpose-specific JWTs, account lockout mechanism, automatic unlock, password encryption and role-based endpoint restrictions. Also, basic validation rules are enforced, such as unique usernames and email, and strong password requirements.


## Table of Contents
1. [Tech Stack](#tech-stack)
2. [Prerequisites](#prerequisites)
3. [Development Environment Setup](#development-environment-setup)
4. [Build and Run](#build-and-run)
5. [Key API Endpoints](#key-api-endpoints)
6. [Security Features](#security-features)
7. [Design Decisions](#design-decisions)
8. [Planned Future Improvements](#planned-future-improvements)
9. [Tests](#tests)
10. [Contribution and License](#contribution-and-license)
11. [Contact](#contact)


## Tech Stack
### Backend
- **Java 17** - Runtime
- **Spring Boot 3** - Framework
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Database abstraction
- **MySQL** - Primary database (via Docker)
- **Redis** - Session storage & caching (via Docker)
- **Spring Mail** – Email sending
- **SpringDoc OpenAPI** – API documentation (Swagger UI)
- **JJWT 0.12.6** - JWT token handling
- **Logback** – Logging framework
  
### Testing
- **JUnit 5** - Unit testing
- **Mockito** - Mock dependencies
- **MockMvc** - Web layer testing
- **TestRestTemplate** - Integration testing

### Deployment
- **Docker** - Containerization
- **Docker Compose** - Multi-service setup
- **MySQL Container** - Database service
- **Redis Container** - Cache service

## Prerequisites  
*Content for prerequisites...*

## Development Environment Setup
*Content for development setup...*

## Build and Run
*Content for build and run...*

## Key API Endpoints
*Content for API endpoints...*

## Security Features
### JWT Implementation
- Short-lived access tokens (15-30 minutes)
- Secure refresh token rotation
- Purpose-specific tokens for different operations

### Account Protection
- Progressive account lockout after failed attempts
- Automatic unlock after configurable time period
- Brute-force attack prevention

### Data Security
- BCrypt password encryption
- Unique username and email validation
- Strong password policies enforcement

### Access Control
- Role-based endpoint restrictions
- Principle of least privilege implementation
- Secure session management

## Design Decisions
- There are three token types—verification, access, and refresh—with default lifetimes of 15 minutes, 5 minutes, and 24 hours respectively. The short lifespan of verification and access tokens largely guarantees the security of the system on its own. Refresh tokens, however, are complemented by an explicit blacklisting mechanism implemented through Redis. 

- To prevent the controllers from becoming bloated with business logic, an AccountOrchestrationService was introduced as an intermediate layer. This service coordinates the interaction between specialized services — such as those responsible for password management, token handling, email delivery, or notifications — ensuring that the controller remains thin and focused on request handling. This approach improves modularity, readability, and testability of the codebase.
  
- The first version of this microservice (see the user-service 1.0 repository) featured a different lockout mechanism: failed login attempts were stored in the users table in the database. The move to Redis represents a substantial improvement, both performance-wise, as Redis read/write operations are much faster than their database counterparts, and architecture-wise, as it cleanly decouples failed login tracking from the database user table. In addition, this version introduces a dedicated table to persist all login attempts — whether failed or successful. Although not yet actively used, these records lay the groundwork for improved scalability and advanced security features.
  
- A GlobalExceptionHandler class has been implemented to centralize exception handling and return consistent, structured HTTP responses. This improves code maintainability and ensures clear, informative feedback to API clients. A class was also created to encapsulate and standardize HTTP responses. This proved extremely useful when integrating the backend with the frontend, as it ensures consistency and simplifies error handling.
    
- Token lifetime values (verification, access, and refresh) are not hardcoded but loaded from the application properties file. A configuration class maps these values to a Map, which is then wrapped in a TokensLifetimes bean and injected wherever needed. This promotes a clean separation between configuration and logic, and, more importantly, it opens the door to hot-reloading token lifetimes without requiring a service restart.
  
- Standard validation rules are applied at the controller, DTO, and database levels to ensure data integrity and correctness.

## Planned Future improvements
- HTTPS has not been implemented in the current version, as the focus was placed on core functionalities. However, it is planned for a future release to ensure secure data transmission, especially for sensitive operations such as authentication.
  
- Event though the tokens short lifespan offers an adequate security baseline, a fully comprehensive token-blacklisting solution is planned for a future iteration.
  
- Hot-reloading of token lifetimes.
  
## Tests
This microservice currently includes 106 tests and reaches a test coverage of 85%. Given its reliance on frameworks such as Spring Boot and Spring Security, as well as external tools like Redis and JJWT, most of the complexity lies in orchestration rather than in business logic.

For this reason, unit tests are used selectively, only in areas where they provide clear and isolated value, such as verifying username and email uniqueness, validating entity relationships, or enforcing password constraints. The primary testing approach is based on integration tests, that are significantly more valuable in this context. Achieving 85% coverage demonstrates that this strategy has been effective.

The tests have been designed to replicate real external behavior with minimal coupling to internal implementation details. This approach ensures meaningful, robust coverage that remains reliable during refactoring and avoids misleading false positives.

Before running the tests, ensure that the required containers —MySQL and Redis— are up and running.

To mimic a production-like environment, containerized services are used: a MySQL instance instead of an in-memory H2 database, and Redis containers managed through Testcontainers. The MySQL instance is initialized cleanly for each test run, retaining only the required preloaded user roles.

In order for the integration tests to work correctly without needing external configuration files, a JWT secret key has been included directly in the tests application.properties file. This key is used only for signing and verifying tokens during test execution. Since this microservice is solely for demonstration purposes and this key is never used in production environments (where secrets are managed via environment variables), this direct inclusion does not pose a security risk.

**INSTRUCCIONES AQUÍ**

## Contribution and License
### Contributing
As this project is intended as a personal demonstration, external contributions are not being accepted at this time.

### License
This project is licensed under the MIT License.  
See the [LICENSE](./LICENSE) file for details.

## Contact
Created by Jorge Casas López.  
Email: [jorgecasaslopez.dev]  
