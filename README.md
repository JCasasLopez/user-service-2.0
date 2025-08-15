# Classrooms-App 
Classrooms-App is a fullstack application designed for the management and reservation of classrooms. The system is composed of three microservices, with only the **user management service** currently included in this repository. It handles authentication, registration, email verification, password recovery, and basic 
account operations.
This monorepo includes both the backend (Spring Boot) and the frontend (Angular), along with the infrastructure needed to run everything using Docker Compose.


## Project Structure
```
.
├── backend/ # Spring Boot microservice for user management
├── frontend/ # Angular frontend interface
├── docker-compose.yml
└── README.md # This file
```


## Tech Stack
  ### Backend
  - Java 17
  - Spring Boot 3
  - Spring Security
  - Spring Data JPA
  - MySQL 8 
  - Redis 7
  - JWT (JSON Web Tokens)

  ### Testing
  - JUnit 5
  - Mockito
  - MockMvc
  - TestRestTemplate
  
  ### Frontend
  - Angular 17
  - Responsive UI with a clean and modern layout

  ### Infrastructure
  - Docker and Docker Compose


## Prerequisites
- Docker and Docker Compose installed
- Node.js (if running frontend separately)
- Java 17+ (if running backend separately)
  

## How to run the application
This will build and start the backend, frontend, database, and Redis cache:
```bash
docker compose up --build user-service angular-frontend
```

Once running, you can access the application at the following URLs:
- Frontend: [http://localhost:4200](http://localhost:4200)
- Backend API: [http://localhost:8000](http://localhost:8000)
- API Docs (Swagger UI): [http://localhost:8000/swagger-ui.html](http://localhost:8000/swagger-ui.html)

If you want to run the tests:
```bash
docker compose up --build user-tests
```
This will start a temporary MySQL container (mysql-test), wait until services are healthy, run all backend tests and shut down the test container afterward.


## Configuration
The application requires the following environment variables:
- `JWT_SECRETKEY`: Secret key for JWT token signing
- `SPRING_MAIL_USERNAME`: Email address for sending notifications (registration, password recovery)
- `SPRING_MAIL_PASSWORD`: Application password for the email service
  
These variables are configured in the respective `.env` files for each service.


The `mysql` container comes preloaded with a fully functional database, including three users (one per role), so that you can test the app's features right away:

| Username       | Role        | Email                  | Password       |
|----------------|-------------|------------------------|----------------|
| `user1`        | `USER`      | user1@example.com      | `Password123!` |
| `admin1`       | `ADMIN`     | admin1@example.com     | `Password123!` |
| `superadmin1`  | `SUPERADMIN`| superadmin1@example.com| `Password123!` |

Please note the following functional restrictions:

- Only users with the `SUPERADMIN` role are allowed to use the **"Upgrade user"** functionality.
- Only users with the `ADMIN` or `SUPERADMIN` roles are allowed to use the **"Update account status"** functionality.
- Accounts with the status `PERMANENTLY_SUSPENDED` **cannot be reactivated** under any circumstances.


## Security Overview
- JWT-based stateless authentication.
- Role-based access control (SUPERADMIN, ADMIN, and USER).
- Short-lived, purpose-based tokens.
- Email verification for registration and password recovery workflows.  
- Account lockout after 3 failed login attempts (tracked via Redis), with automatic unlock after 24 hours  *(Default values are configurable)*.  
- Token revocation on logout.


## API Documentation
The user-service exposes a full OpenAPI (Swagger) specification, available at:  
[http://localhost:8000/user/swagger-ui/index.html#/](http://localhost:8000/user/swagger-ui/index.html#/)


## Design Decisions
The following features were intentionally left out to prioritize core functionality. Both are planned for future implementation:
- HTTPS is not configured; production deployments should include a reverse proxy for SSL termination.
- The frontend does not implement token refresh logic. Access tokens are short-lived and require manual re-login upon expiration.

  
## Individual repositories
This monorepo consolidates both backend and frontend for convenience. However, the services are modular, and you can also explore them separately:
- [User Service (Spring Boot)](https://github.com/JCasasLopez/user-service-2.0)
- [Frontend (Angular)](https://github.com/JCasasLopez/classrooms-frontend)


## Author
Designed and implemented by Jorge Casas López.
Feel free to connect via [LinkedIn](https://www.linkedin.com/in/your-link) or contact me at j.casas.lopez.26@gmail.com.  
You can find more of my projects at [https://github.com/JCasasLopez](https://github.com/JCasasLopez).
