CREATE TABLE IF NOT EXISTS roles (
    idRole INT NOT NULL AUTO_INCREMENT,
    roleName ENUM('ROLE_USER','ROLE_ADMIN','ROLE_SUPERADMIN') NOT NULL, 
    PRIMARY KEY (idRole),
    UNIQUE KEY roleName_UNIQUE (roleName) 
);

CREATE TABLE IF NOT EXISTS users (
    idUser INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    fullName VARCHAR(30) NOT NULL, 
    email VARCHAR(255) NOT NULL, 
    dateOfBirth DATE NOT NULL, 
    accountStatus ENUM('ACTIVE','TEMPORARILY_BLOCKED','BLOCKED','PERMANENTLY_SUSPENDED') NOT NULL,
    PRIMARY KEY (idUser),
    UNIQUE KEY username_UNIQUE (username), 
    UNIQUE KEY email_UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id INT NOT NULL, 
    role_id INT NOT NULL, 
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(idUser) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(idRole)
);

CREATE TABLE IF NOT EXISTS login_attempts (
    idLoginAttempt BIGINT NOT NULL AUTO_INCREMENT, 
    timestamp DATETIME(6) NOT NULL, 
    successful BIT(1) NOT NULL, 
    ipAddress VARCHAR(45) NOT NULL, 
    loginFailureReason ENUM('INCORRECT_PASSWORD','USER_NOT_FOUND','MISSING_FIELD','ACCOUNT_LOCKED','OTHER') DEFAULT NULL,
    idUser INT DEFAULT NULL,
    PRIMARY KEY (idLoginAttempt),
    KEY idUser_idx (idUser), 
    CONSTRAINT fk_idUser FOREIGN KEY (idUser) REFERENCES users(idUser) ON DELETE SET NULL 
);