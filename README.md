# User service 2.0
An authentication and user management microservice that uses JWTs (JSON Web Tokens), a classic refresh token system, and Spring Security to implement login and logout 
capabilities, along with typical account-related features such as:
- User registration and password reset (via verification tokens sent by email)
- Password change
- User upgrade to ADMIN role (only allowed for SUPERADMIN users)
- Account status update (allowed for SUPERADMIN and ADMIN users)
- Account deletion
- Inter-service notifications (for other microservices)

The microservice prioritizes security through short-lived, purpose-specific JWTs (verification, access and refresh) and an account lockout mechanism after 3 failed login attempts 
(tracked via Redis), to prevent brute-force attacks. Accounts are automatically unlocked after 24 hours (default values are configurable). Passwords are encripted using BCrypt.

Basic validation rules are enforced, such as unique usernames/emails and strong password requirements (see section 'Security Rules' for details).

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

  ### Testing
  - JUnit 5
  - Mockito
  - MockMvc
  - TestRestTemplate

  ### Infrastructure:
  - Docker & Docker Compose


  ## Security features
  According to business logic rules, duplicate usernames or email addresses are not permitted. To enhance security even further, passwords must be at least 8 characters long 
  and include at least one uppercase letter, one lowercase letter, one number, and one symbol.


  ## Testing
  This microservice includes a total of 81 tests. While unit tests have been implemented where they provide clear value—such as validating the uniqueness of usernames 
  and email addresses, ensuring correct entity relationships, or enforcing password rules— the primary focus has been on integration testing.
  
  Given that the microservice contains relatively little business logic but involves complex flows across multiple layers, including filters and Spring Security, 
  I believe integration tests offer significantly more value. These tests have been carefully designed to replicate the internal behavior of the system as closely as possible, 
  with the goal of providing realistic and meaningful coverage.
