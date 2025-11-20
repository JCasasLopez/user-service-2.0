# User Service 2.0
An authentication and user management microservice that uses *JWTs* (*JSON Web Tokens*v), a classic refresh token system, and *Spring Security* to implement login and logout capabilities, along with typical account-related features (see *API* section for details).

The microservice prioritizes security through short-lived, purpose-specific *JWTs*, account lockout mechanism, automatic unlock, password encryption and role-based endpoint restrictions. Also, basic validation rules are enforced, such as unique usernames and email, and strong password requirements.


## Table of Contents
1. [Tech Stack](#tech-stack)
2. [System Requirements](#system-requirements)
3. [Local Deployment](#local-deployment)
4. [Live Deployment](#live-deployment)
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
As mentioned in the 'Contribution' section, this project is a personal demo, so external contributions are not being accepted. Still, the following information may be useful to understand the project structure.

The project consists of a Backend (Spring Boot + Java) and a Frontend (Angular). For local exploration (optional), you would need:
- Java 17+ and Maven 3.9+ for the backend.
- Node.js 18+ and Angular CLI for the frontend.
- Docker only if you want to run container-based tests.

If you only want to preview the application in production, note that it uses MySQL and Redis, but all infrastructure is fully managed by Railway, so no local installation is required.

## Local Deployment
The application can be run locally for demonstration purposes if desired, but the live version is fully deployed on Railway with all infrastructure managed automatically.

## Live Deployment
*Content for build and run...*

## API Endpoints
### Authentication
| Method | Endpoint      | Description             | Auth Required |
|--------|---------------|-------------------------|---------------|
| POST   | /login        | User authentication     | No            |
| POST   | /logout       | User logout             | Yes           |
| POST   | /refreshToken | Generate new token pair | Yes           |

### User Management
| Method | Endpoint                  | Description                | Auth Required |
|--------|----------------------------|---------------------------|---------------|
| POST   | /initiateUserRegistration | Start registration process | No            |
| POST   | /userRegistration         | Complete user registration | Yes           |
| DELETE | /deleteAccount            | Delete user account        | Yes           |

### Password Management
| Method | Endpoint        | Description               | Auth Required |
| ------ | --------------- | ------------------------- | ------------- |
| POST   | /forgotPassword | Request password reset    | No            |
| PUT    | /resetPassword  | Reset password with token | Yes           |
| PUT    | /changePassword | Change current password   | Yes           |

### Administrative
| Method | Endpoint             | Description            | Auth Required | Role             |
| ------ | -------------------- | ---------------------- | ------------- | ---------------- |
| PUT    | /upgradeUser         | Grant admin privileges | Yes           | SUPERADMIN       |
| PUT    | /updateAccountStatus | Update account status  | Yes           | ADMIN/SUPERADMIN |
| POST   | /sendNotification    | Send user notification | Yes           | Any              |

## Security Features
### Token types and lifetimes
The system makes use of *JWT*s to achieve stateless authentication. These tokens are created and validated using *Spring Security* and the *JJWT* library.
Token lifetimes are configurable in the *application.properties* file.
- *Access* tokens: short-lived tokens for API authentication (default value: 15 minutes).
- *Verification* tokens: purpose-specific tokens for email verification and similar operations (default value: 5 minutes).
- *Refresh* tokens: longer-lived tokens for obtaining new access tokens (default value: 24 hours).
  
### Account Protection
- Account lockout mechanism to prevent brute-force attacks. Account is automatically unlocked after a pre-established period of time (Default values, set in *application.properties*: 3 failed attempts and 24 hours, respectively). The unlocking mechanism is outlined in the [Authentication Flow Overview](#authentication-flow-overview) section, located further down.
  
- Email verification for *'Create Account'* and *'Reset Password*' functionalities.
  
- *BCrypt* password encryption.
  
- Strong password policies enforcement (must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character).
  
- Possible account atatus:

| Status                | Description                                                                                                                         |
|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| ACTIVE                | Account fully functional                                                                                                            |
| BLOCKED               | Manually suspended by an admin/superadmin. The account is **not** automatically unlocked; only an administrator can reactivate it.  |
| TEMPORARILY_BLOCKED   | Auto-blocked after failed login attempts. It is automatically unlocked after a specified period of time.                            |
| PERMANENTLY_SUSPENDED | This status can only be assigned by an administrator. Once suspended, the account cannot be reactivated.                            |

### Data Security
- Unique username and unique email validation.
  
- Standard validation rules are applied at the controller, DTO, and database levels to ensure data integrity and correctness.

### Access Control
- The system differentiates three different roles: *USER*, *ADMIN* and *SUPER-ADMIN*. Only *ADMIN* and *SUPER-ADMIN* users can change an account status. Only a *SUPER-ADMIN* user can update a user to admin.
  
- Role-based endpoint restrictions, using method-level authorization as opposed to *SecurityFilterChain* configuration.

### Authentication Flow Overview
*Spring Security* filters and handlers are heavily customized, as *JWT*s do not integrate well with the framework:

- A custom *UsernamePasswordAuthenticationFilter* checks whether the account is locked before proceeding with the login process. If locked, the filter verifies whether a corresponding *Redis* entry exists for the user attempting authentication. If the entry is present, the lockout period is still active and the request is rejected. If the entry is missing, the lockout period has expired and the account status can be switched back to ACTIVE in the database.
  
- The *AuthenticationFilter* is at the core of the authentication workflow. It extracts the *JWT* from the HTTP request header, identifies its type (*verification*, *access*, or *refresh*), and routes the request accordingly.  
  This filter is designed to implement a deny-by-default policy, meaning it denies access whenever any validation step fails — such as an invalid token, an unsupported HTTP method, or an attempt to access a restricted path.

- Custom login, authentication, and authorization handlers return HTTP responses that provide relevant information to the user without exposing sensitive details. For example, both an incorrect username and an incorrect password return *'Bad Credentials'* to avoid leaking information.
  
## Design Decisions
- The short lifespan of *verification* and *access* tokens largely guarantees the security of the system on its own. *Refresh* tokens, however, are complemented by an explicit blacklisting mechanism implemented through *Redis*.
  
- To prevent the controllers from becoming bloated with business logic, an *AccountOrchestrationService* was introduced as an intermediate layer. This service coordinates the interaction between specialized services — such as those responsible for password management, token handling, email delivery, or notifications — ensuring that the controller remains thin and focused on request handling. This approach improves modularity, readability, and testability of the codebase.
  
- The first version of this microservice (see the *'user-service 1.0'* repository) featured a different lockout mechanism: failed login attempts were stored in the users table in the database. The move to *Redis* represents a substantial improvement, both performance-wise, as *Redis* read/write operations are much faster than their database counterparts, and architecture-wise, as it cleanly decouples failed login tracking from the database user table.  
  In addition, this version introduces a dedicated table to persist all login attempts — whether failed or successful. Although not yet actively used, these records lay the groundwork for improved scalability and advanced security features.

- A *GlobalExceptionHandler* class has been implemented to centralize exception handling and return consistent, structured HTTP responses. This improves code maintainability and ensures clear, informative feedback to API clients. A class was also created to encapsulate and standardize HTTP responses. This proved extremely useful when integrating the backend with the frontend, as it ensures consistency and simplifies error handling.
  
- Token lifetimes values are not hardcoded but loaded from the *application properties* file. A configuration class maps these values to a Map, which is then wrapped in a *TokensLifetimes* bean and injected wherever needed. This promotes a clean separation between configuration and logic, and, more importantly, it opens the door to hot-reloading token lifetimes without requiring a service restart.
  
## Planned Future improvements
- HTTPS has not been implemented in the current version, as the focus was placed on core functionalities. However, it is planned for a future release to ensure secure data transmission, especially for sensitive operations such as authentication.
  
- Event though the tokens short lifespan offers an adequate security baseline, a fully comprehensive token-blacklisting solution is planned for a future iteration. If the backend receives an already blacklisted token, the account gets blocked automatically.
  
- *Hot-reloading* of token lifetimes.
  
## Tests
This microservice currently includes 106 tests and reaches a test coverage of 85%. All tests pass when run locally, but two failed in the Docker environment due to a timezone/datetime serialization mismatch, likely due to differences in timezone settings or Jackson configuration between local and containerized environments. This problem could not be fixed.

Given its reliance on frameworks such as *Spring Boot* and *Spring Security*, as well as external tools like *Redis* and *JJWT*, most of the complexity lies in orchestration rather than in business logic.

For this reason, unit tests are used selectively, only in areas where they provide clear and isolated value, such as verifying username and email uniqueness, validating entity relationships, or enforcing password constraints. The primary testing approach is based on integration tests, that are significantly more valuable in this context. Achieving 85% coverage demonstrates that this strategy has been effective.

The tests have been designed to replicate real external behavior with minimal coupling to internal implementation details. This approach ensures meaningful, robust coverage that remains reliable during refactoring and avoids misleading false positives.

Before running the tests, ensure that the required containers —*MySQL* and *Redis*— are up and running.

To mimic a production-like environment, containerized services are used: a *MySQL* instance instead of an in-memory H2 database, and *Redis* containers managed through Testcontainers. The *MySQL* instance is initialized cleanly for each test run, retaining only the required preloaded user roles.

In order for the integration tests to work correctly without needing external configuration files, a *JWT* secret key has been included directly in the tests *application.properties* file. This key is used only for signing and verifying tokens during test execution. Since this microservice is solely for demonstration purposes and this key is never used in production environments (where secrets are managed via environment variables), this direct inclusion does not pose a security risk.

### Running the Tests
1. **Clone the repository**
```bash
   git clone <repository-url>
   cd <project-directory>
```

2. **Start the required containers**
```bash
   docker compose up -d
```

3. **Run the tests**
```bash
   docker compose run --rm user-tests
```

## Contribution and License
### Contributing
As this project is intended as a personal demo, external contributions are not being accepted at this time.

### License
This project is licensed under the MIT License.  
See the [LICENSE](./LICENSE) file for details.

## Contact
Created by Jorge Casas López.  
Email: [contact@jorgecasaslopez.dev](mailto:contact@jorgecasaslopez.dev) 
