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
8. [Tests](#tests)
9. [Contribution and License](#contribution-and-license)


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
- **Environment Config** - Secure configuration

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
*Content for design decisions...*

## Tests
*Content for tests...*

## Contribution and License
*Content for contribution and license...*
