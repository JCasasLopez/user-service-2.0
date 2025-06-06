CREATE TABLE IF NOT EXISTS roles (
    idRole INT AUTO_INCREMENT PRIMARY KEY,
    roleName VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    idUser INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(20) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fullName VARCHAR(30),
    email VARCHAR(255) UNIQUE NOT NULL,
    dateOfBirth DATE,
    accountStatus VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id INT,
    role_id INT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(idUser) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(idRole)
);

CREATE TABLE IF NOT EXISTS login_attempts (
    idLoginAttempt BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME(6),
    successful BIT NOT NULL,
    ipAddress VARCHAR(255),
    loginFailureReason TINYINT CHECK (loginFailureReason BETWEEN 0 AND 4),
    idUser INT NULL,
    FOREIGN KEY (idUser) REFERENCES users(idUser) ON DELETE SET NULL
);
