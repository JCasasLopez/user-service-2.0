# User Service 2.0
An authentication and user management microservice that uses JWTs (JSON Web Tokens), a classic refresh token system, and Spring Security to implement login and logout 
capabilities, along with typical account-related features (see API section for details).

The microservice prioritizes security through short-lived, purpose-specific JWTs (verification, access and refresh) and an account lockout mechanism after 3 failed login attempts (tracked via Redis), to prevent brute-force attacks. Accounts are automatically unlocked after 24 hours (default values are configurable). Passwords are encrypted using BCrypt, and some of the endpoints are role-restricted.

Basic validation rules are enforced, such as unique usernames/emails and strong password requirements (see section 'Security Features' for details).

All login attempts —whether failed or successful— are logged in a separate database table to enable future analysis for security monitoring and usage patterns. 
This version of the microservice does not yet make use of those records (Logged fields include: timestamp, success/failure flag, IP address, failure reason, and user ID — when relevant).

  ## Tech Stack
  ### Microservice
  - Java 17
  - Spring Boot 3
  - Spring Security
  - Spring Data JPA
  - MySQL 8 
  - Redis 7
  - JWT (JSON Web Tokens)
  - JJWT Library

  ### Testing
  - JUnit 5
  - Mockito
  - MockMvc
  - TestRestTemplate

  ### Infrastructure:
  - Docker & Docker Compose


Perfecto, gracias por la información adicional. Con eso puedo darte un **apartado completo y mejorado de "Getting Started"** para tu README del microservicio `user-service`, integrando:

* La distinción entre base de datos principal (con datos precargados) y base de tests (vacía).
* Las variables obligatorias.
* Los comandos correctos y lo que hace cada servicio.
* La tabla de usuarios de ejemplo.

Aquí lo tienes:

---

## How to run the application

This service can be run locally using Docker. Follow these steps to build and launch the application:

### 1. Prerequisites

* Docker and Docker Compose installed
* A `.env` file in the project root with the following variables:

```env
# Required for JWT signing and validation
JWT_SECRETKEY=1bZB+WJHnYqK+0bL1zZjlEZ7WjZq3FP1eRbF1VKxN25DlRZtk4o2JQ6Tly9X7qVmTO3rJJwnDBIvV6J3hG8e4Q==

# Required to enable email-based features (registration, password reset, etc.)
SPRING_MAIL_USERNAME=j.casas.lopez.26@gmail.com
SPRING_MAIL_PASSWORD=uuqlojwafojnzbvv
```

> ⚠️ **Important:** These values must be exact.
> Never commit your `.env` file to version control.

---

### 2. Start the service

Run the following command to start the backend, database, and Redis cache:

```bash
docker-compose -f docker-compose.dev.yml up --build user-service
```

This will launch:

* `mysql`: MySQL 8.0, preloaded with schema and sample data
* `redis`: Redis 7.2
* `user-service`: Spring Boot app available at [http://localhost:8000](http://localhost:8000)

You can access the API documentation at:
👉 [http://localhost:8000/swagger-ui.html](http://localhost:8000/swagger-ui.html)

---

### 3. Default users

The main database comes preloaded with three example users to help you explore the system:

| Username      | Role         | Email                                                     | Password       |
| ------------- | ------------ | --------------------------------------------------------- | -------------- |
| `user1`       | `USER`       | [user1@example.com](mailto:user1@example.com)             | `Password123!` |
| `admin1`      | `ADMIN`      | [admin1@example.com](mailto:admin1@example.com)           | `Password123!` |
| `superadmin1` | `SUPERADMIN` | [superadmin1@example.com](mailto:superadmin1@example.com) | `Password123!` |

---

### 4. Run backend tests

To run the integration tests in isolation:

```bash
docker-compose -f docker-compose.dev.yml up --build user-tests
```

This will:

* Start a separate MySQL container (`mysql-test`) with the same schema but no initial data
* Wait until MySQL and Redis are healthy
* Run all tests inside the `user-tests` container
* Shut down the test container automatically

---


# API Endpoints

## Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/login` | User authentication | No |
| `POST` | `/logout` | User logout | Yes |
| `POST` | `/refreshToken` | Generate new token pair | Yes |

## User Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/initiateUserRegistration` | Start registration process | No |
| `POST` | `/userRegistration` | Complete user registration | Yes |
| `DELETE` | `/deleteAccount` | Delete user account | Yes |

## Password Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/forgotPassword` | Request password reset | No |
| `PUT` | `/resetPassword` | Reset password with token | Yes |
| `PUT` | `/changePassword` | Change current password | Yes |

## Administrative

| Method | Endpoint | Description | Auth Required | Role |
|--------|----------|-------------|---------------|------|
| `PUT` | `/upgradeUser` | Grant admin privileges | Yes | SUPERADMIN |
| `PUT` | `/updateAccountStatus` | Update account status | Yes | ADMIN/SUPERADMIN |
| `POST` | `/sendNotification` | Send user notification | Yes | Any |

## Account Status Values

| Status | Description |
|--------|-------------|
| `ACTIVE` | Account fully functional |
| `BLOCKED` | Manually suspended by admin |
| `TEMPORARILY_BLOCKED` | Auto-blocked after failed login attempts |
| `PERMANENTLY_SUSPENDED` | Permanently deactivated |

---

**Note**: Complete API documentation with request/response formats, validation rules, and status codes is available via Swagger UI at [http://localhost:8000/user/swagger-ui/index.html#/](http://localhost:8000/user/swagger-ui/index.html#/)


  ## Security features

  From a technical standpoint, the most prominent feature of the microservice is the use of JWTs with specific purposes (verification, access, and refresh) for authentication, along with a refresh token system that improves the user experience. To that end, the microservice uses Spring Security and the JJWT library to create and validate tokens.
  
  As previously mentioned, the microservice implements an account lockout mechanism to prevent brute-force attacks. After 3 failed login attempts — which are tracked via Redis — the account is locked. It is automatically unlocked after 24 hours. All these values — including token lifetimes, the maximum number of failed attempts, and the unlock delay — are configurable in the `application.properties` file.
  
  It is worth noting that the Spring Security filters and handlers are heavily customized, as JWTs often do not integrate well with the framework. Below is a brief overview of the security flow:
  
  - A custom implementation of `UsernamePasswordAuthenticationFilter` checks whether the account is locked before proceeding with the login process. There are three possible states (or “account status” in the terminology used by the microservice) that indicate a locked account:
    
    1. **TEMPORARILY_BLOCKED**: The account has been blocked due to too many failed login attempts. It is automatically unlocked after a specified period of time (as verified via the corresponding Redis entry).
    
    2. **BLOCKED**: An administrator (with either `admin` or `superadmin` role) may manually block an account if it is suspected to be compromised or there is another reason to temporarily disable it. In this state, the account is **not** automatically unlocked, and only an administrator can reactivate it.
    
    3. **PERMANENTLY_SUSPENDED**: This status can also only be assigned by an administrator. Once suspended, the account cannot be reactivated.
  
  - `AuthenticationFilter` is at the core of the authentication process. It extracts the token from the HTTP request header, determines its type (verification, access, or refresh), and routes the request accordingly.
  
  - Custom login, authentication, and authorization handlers are implemented to return HTTP responses that provide as much relevant information as possible to the user.
  
  Passwords must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character, and are encrypted using BCrypt.


  ## Design decisions

- HTTPS has not been implemented in the current version, as the focus was placed on core functionalities. However, it is planned for a future release to ensure secure data transmission, especially for sensitive operations such as authentication. HTTPS will be integrated using a reverse proxy (e.g., Nginx) or through Spring Boot’s built-in SSL support, depending on the deployment environment.

- The first version of this microservice (see user-service 1.0) featured a different lockout mechanism: failed login attempts were stored in the `users` table in the database. The move to Redis simplifies the logic and results in cleaner code. In addition, this version introduces a dedicated table to persist all login attempts — whether failed or successful. Although not yet actively used, these records lay the groundwork for improved scalability and advanced security features.

- A `GlobalExceptionHandler` class has been implemented to centralize exception handling and return consistent, structured HTTP responses. This improves code maintainability and ensures clear, informative feedback to API clients.

- The `StandardResponse` class (see DTO folder) was created to encapsulate and standardize HTTP responses. This proved extremely useful when integrating the backend with the frontend, as it ensures consistency and simplifies error handling.

- To prevent the controllers from becoming bloated with business logic, an `AccountOrchestrationService` was introduced as an intermediate layer. This service coordinates the interaction between specialized services — such as those responsible for password management, token handling, email delivery, or notifications — ensuring that the controller remains thin and focused on request handling. This approach improves modularity, readability, and testability of the codebase.

- Token lifetime values (verification, access, and refresh) are not hardcoded but loaded from the application properties file. A configuration class (`TokensLifetimesConfiguration`) maps these values to a `Map<TokenType, Integer>`, which is then wrapped in a `TokensLifetimes` bean and injected wherever needed. This promotes clean separation between configuration and logic, improves testability, and ensures consistency across the application.


  ## Testing
  This microservice includes a total of 81 tests. While unit tests have been implemented where they provide clear value—such as validating the uniqueness of usernames 
  and email addresses, ensuring correct entity relationships, or enforcing password rules— the primary focus has been on integration testing.
  
  Given that the microservice contains relatively little business logic but involves complex flows across multiple layers, including filters and Spring Security, 
  I believe integration tests offer significantly more value. These tests have been carefully designed to replicate the internal behavior of the system as closely as           possible, with the goal of providing realistic and meaningful coverage.
