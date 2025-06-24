# User Service 2.0
An authentication and user management microservice that uses JWTs (JSON Web Tokens), a classic refresh token system, and Spring Security to implement login and logout 
capabilities, along with typical account-related features such as:
- User registration and password reset (via verification tokens sent by email)
- Password change
- User upgrade to ADMIN role (only allowed for SUPERADMIN users)
- Account status update (allowed for SUPERADMIN and ADMIN users)
- Account deletion
- Inter-service notifications (for other microservices)

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


# API Endpoints

## Authentication Endpoints

| Method | Endpoint | Description | Authentication | Request Body | Response Codes |
|--------|----------|-------------|----------------|--------------|----------------|
| `POST` | `/login` | Authenticates user and returns tokens | None | `application/x-www-form-urlencoded`<br>`username=myuser&password=Password123!` | `200` Login successful<br>`400` Username or password missing<br>`401` Bad credentials<br>`403` Account is locked |
| `POST` | `/logout` | Logs out the authenticated user | Bearer Token | `string` (refresh token) | `200` Logout successful<br>`401` Unauthorized – invalid or missing refresh token |
| `POST` | `/refreshToken` | Generates new refresh and access tokens | Bearer Token | None | `201` New refresh and access tokens sent successfully<br>`401` Unauthorized – refresh token is missing, expired, blacklisted, or invalid |

## User Registration & Account Management

| Method | Endpoint | Description | Authentication | Request Body | Response Codes |
|--------|----------|-------------|----------------|--------------|----------------|
| `POST` | `/initiateUserRegistration` | Initiates the registration process by sending a verification email | None | `UserDto` object (JSON) | `200` Verification token created and sent successfully<br>`400` Validation failed for one or more fields |
| `POST` | `/userRegistration` | Finalizes user registration | Bearer Token | Request processed from header | `201` Account created successfully<br>`400` Validation failed for one or more fields or invalid input<br>`401` Unauthorized – token is missing, expired, or invalid<br>`409` User with that email or username already exists<br>`500` Error processing the JSON payload |
| `DELETE` | `/deleteAccount` | Deletes the authenticated user account | Bearer Token | None | `200` Account deleted successfully<br>`401` Unauthorized – token is missing, expired, or invalid |

## Password Management

| Method | Endpoint | Description | Authentication | Request Body | Response Codes |
|--------|----------|-------------|----------------|--------------|----------------|
| `POST` | `/forgotPassword` | Initiates password reset process | None | `string` (email address)<br>Example: `"user@example.com"` | `200` Token created successfully and sent to the user to reset password<br>`400` Invalid email format or empty field<br>`404` User not found in the database |
| `PUT` | `/resetPassword` | Resets the user password | Bearer Token | `string` (new password)<br>Example: `"Password123!"` | `200` Password reset successfully<br>`400` Missing or invalid password format<br>`401` Unauthorized – token is missing, expired, or invalid |
| `PUT` | `/changePassword` | Changes the password of the currently authenticated user | Bearer Token | JSON object with old and new passwords<br>```json<br>{<br>  "oldPassword": "Password123!",<br>  "newPassword": "NewPassword456!"<br>}<br>``` | `200` Password changed successfully<br>`400` Missing password fields or password does not meet the security criteria<br>`401` Unauthorized – token is missing, expired, or invalid |

## Administrative Endpoints

| Method | Endpoint | Description | Authentication | Required Role | Request Body | Response Codes |
|--------|----------|-------------|----------------|---------------|--------------|----------------|
| `PUT` | `/upgradeUser` | Grants admin privileges to a user | Bearer Token | SUPERADMIN | `string` (email address)<br>Example: `"user@example.com"` | `200` User upgraded successfully to admin<br>`400` Invalid or missing email<br>`401` Unauthorized – token is missing, expired, or invalid<br>`403` User does not have SUPERADMIN privileges<br>`404` User not found in the database |
| `PUT` | `/updateAccountStatus` | Updates the account status of a user | Bearer Token | ADMIN/SUPERADMIN | `string` (email address)<br>Query param: `newAccountStatus` | `200` Account status updated successfully<br>`400` Invalid or missing email or account status value<br>`401` Unauthorized – token is missing, expired, or invalid<br>`403` User does not have ADMIN or SUPERADMIN privileges<br>`404` User not found in the database |
| `POST` | `/sendNotification` | Sends a notification to a user | Bearer Token | Authenticated User | JSON object with notification details<br>```json<br>{<br>  "Recipient": "123",<br>  "Subject": "Account warning",<br>  "Message": "Your account has been temporarily suspended due to policy violations."<br>}<br>``` | `200` Notification sent successfully<br>`400` Missing or invalid message fields<br>`401` Unauthorized – token is missing, expired, or invalid<br>`404` User not found in the database |

## Account Status Values

When using the `/updateAccountStatus` endpoint, the following values are accepted for the `newAccountStatus` parameter:

| Status | Description |
|--------|-------------|
| `ACTIVE` | The account is in good standing and fully usable |
| `BLOCKED` | The account is suspended for a period (e.g., due to suspicious activity) |
| `TEMPORARILY_BLOCKED` | The account is blocked due to too many failed login attempts. It will reactivate automatically after a defined period of time (24h by default) |
| `PERMANENTLY_SUSPENDED` | The account is permanently deactivated and cannot be used |

## Response Format

All endpoints return a standardized response format:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "message": "Operation completed successfully",
  "data": null,
  "status": "OK"
}
```

## Authentication

- **Bearer Token**: Include the access token in the Authorization header: `Authorization: Bearer <your_access_token>`
- **Token Refresh**: Use the refresh token with the `/refreshToken` endpoint to obtain new tokens
- **Token Format**: JWT tokens are used for authentication and authorization

## Notes

- All timestamps are in ISO 8601 format
- Email addresses must be valid and properly formatted
- Passwords must meet the security criteria defined in the application
- The `/login` and `/logout` endpoints are handled by the authentication filter chain and are not exposed as standard controller endpoints
- Account unlock happens automatically after the time period specified in application.properties for temporarily blocked accounts


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
