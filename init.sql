CREATE TABLE IF NOT EXISTS roles (
    idRole INT AUTO_INCREMENT PRIMARY KEY,
    roleName VARCHAR(50) UNIQUE NOT NULL
);

INSERT INTO roles (roleName) VALUES ('ROLE_USER'), ('ROLE_ADMIN'), ('ROLE_SUPERADMIN');

CREATE TABLE IF NOT EXISTS users (
    idUser INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(20) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fullName VARCHAR(30),
    email VARCHAR(255) UNIQUE NOT NULL,
    dateOfBirth DATE,
    accountStatus VARCHAR(50)
);

-- password123 (codificado)
INSERT INTO users (username, password, full_name, email, date_of_birth, account_status)
VALUES 
('user1', '$2a$10$2dNBxwcHe7fWs3ZIOTx0GOk6K3M5ZFU/RO6X8oRF0AmRUIoyMLqTm', 'User One', 'user1@example.com', '2000-01-01', 'ACTIVE'),
('admin1', '$2a$10$mrqs1b.RZA5SO3.wWnTNo./DzcsJ1oz9JPQeptR3X6PDVxSSVq/PG', 'Admin One', 'admin1@example.com', '1990-01-01', 'ACTIVE'),
('superadmin1', '$2a$10$J9LgCk7Qpn5XU3Tqs9GRLu1fOjwpNQhCQK.WaEx0ZrqqRpDVy5FKS', 'Superadmin One', 'superadmin1@example.com', '1980-01-01', 'ACTIVE');

CREATE TABLE IF NOT EXISTS user_roles (
    user_id INT,
    role_id INT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(idUser),
    FOREIGN KEY (role_id) REFERENCES roles(idRole)
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.idUser, r.idRole FROM users u, roles r 
WHERE 
    (u.username = 'user1' AND r.roleName = 'ROLE_USER') OR
    (u.username = 'admin1' AND r.roleName = 'ROLE_ADMIN') OR
    (u.username = 'superadmin1' AND r.roleName = 'ROLE_SUPERADMIN');