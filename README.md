# User service 2.0
An authentication and user management microservice that uses JWTs (JSON Web Tokens), a classic refresh token system, and Spring Security to implement login and logout 
capabilities, along with typical account-related features such as:
- User registration and password reset (via verification tokens sent by email)
- Password change
- User upgrade to ADMIN role (only allowed for SUPERADMIN users)
- Account status update (allowed for SUPERADMIN and ADMIN users)
- Account deletion
- Inter-service notifications (for other microservices)

The microservice prioritizes security through short-lived, purpose-specific JWTs (verification, access and refresh) and an account lockout mechanism after 3 failed login attempts (tracked via Redis), to prevent brute-force attacks. Accounts are automatically unlocked after 24 hours (default values are configurable). Passwords are encripted using BCrypt, and some of the endpoints are role-restricted.

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


  ## Security features

  From a technical standpoint, the most prominent feature of the microservice is the use of JWTs with specific purposes (verification, access, and refresh) for authentication, along with a refresh token system that improves the user experience. To that end, the microservice uses Spring Security and the JJWT library to create and validate tokens.
  
  As previously mentioned, the microservice implements an account lockout mechanism to prevent brute-force attacks. After 3 failed login attempts — which are tracked via Redis — the account is locked. It is automatically unlocked after 24 hours. All these values — including token lifetimes, the maximum number of failed attempts, and the unlock delay — are configurable in the `application.properties` file.
  
  It is worth noting that the Spring Security filters and handlers are heavily customized, as JWTs often do not integrate well with the framework. Below is a brief overview of the security flow:
  
  - A custom implementation of `UsernamePasswordAuthenticationFilter` checks whether the account is locked before proceeding with the login process. There are three possible statuses (or “states” in the terminology used by the microservice) that indicate a locked account:
    
    1. **TEMPORARILY_BLOCKED**: The account has been blocked due to too many failed login attempts. It is automatically unlocked after a specified period of time (as verified via the corresponding Redis entry).
    
    2. **BLOCKED**: An administrator (with either `admin` or `superadmin` role) may manually block an account if it is suspected to be compromised or there is another reason to temporarily disable it. In this state, the account is **not** automatically unlocked, and only an administrator can reactivate it.
    
    3. **PERMANENTLY_SUSPENDED**: This status can also only be assigned by an administrator. Once suspended, the account cannot be reactivated.
  
  - `AuthenticationFilter` is at the core of the authentication process. It extracts the token from the HTTP request header, determines its type (verification, access, or refresh), and routes the request accordingly.
  
  - Custom login, authentication, and authorization handlers are implemented to return HTTP responses that provide as much relevant information as possible to the user.
  
  Passwords are encrypted using BCrypt.
  
  In accordance with business rules, duplicate usernames or email addresses are not allowed. Furthermore, passwords must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character.

  ## Testing
  This microservice includes a total of 81 tests. While unit tests have been implemented where they provide clear value—such as validating the uniqueness of usernames 
  and email addresses, ensuring correct entity relationships, or enforcing password rules— the primary focus has been on integration testing.
  
  Given that the microservice contains relatively little business logic but involves complex flows across multiple layers, including filters and Spring Security, 
  I believe integration tests offer significantly more value. These tests have been carefully designed to replicate the internal behavior of the system as closely as           possible, with the goal of providing realistic and meaningful coverage.
