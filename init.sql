CREATE TABLE IF NOT EXISTS roles (
    id_role INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL
);

INSERT INTO roles (role_name) VALUES ('ROLE_USER'), ('ROLE_ADMIN'), ('ROLE_SUPERADMIN');

CREATE TABLE IF NOT EXISTS users (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(20) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(30),
    email VARCHAR(255) UNIQUE NOT NULL,
    date_of_birth DATE,
    account_status VARCHAR(50)
);

-- Password: password123 
INSERT INTO users (username, password, full_name, email, date_of_birth, account_status)
VALUES 
('user1', '$2a$10$k.jERhRh8ow3VJ.4Ug5NeO8a3xy9wPb6JSw3JxdWyRLMeaIozlwLC', 'User One', 'user1@example.com', '2000-01-01', 'ACTIVE'),
('admin1', '$2a$10$k.jERhRh8ow3VJ.4Ug5NeO8a3xy9wPb6JSw3JxdWyRLMeaIozlwLC', 'Admin One', 'admin1@example.com', '1990-01-01', 'ACTIVE'),
('superadmin1', '$2a$10$k.jERhRh8ow3VJ.4Ug5NeO8a3xy9wPb6JSw3JxdWyRLMeaIozlwLC', 'Superadmin One', 'superadmin1@example.com', '1980-01-01', 'ACTIVE');

CREATE TABLE IF NOT EXISTS user_roles (
    user_id INT,
    role_id INT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id_user),
    FOREIGN KEY (role_id) REFERENCES roles(id_role)
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id_user, r.id_role FROM users u, roles r 
WHERE 
    (u.username = 'user1' AND r.role_name = 'ROLE_USER') OR
    (u.username = 'admin1' AND r.role_name = 'ROLE_ADMIN') OR
    (u.username = 'superadmin1' AND r.role_name = 'ROLE_SUPERADMIN');