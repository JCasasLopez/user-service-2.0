INSERT INTO roles (roleName) VALUES ('ROLE_USER'), ('ROLE_ADMIN'), ('ROLE_SUPERADMIN');

-- Password123! (codificado)
INSERT INTO users (username, password, fullName, email, dateOfBirth, accountStatus)
VALUES 
('user1', '$2a$10$2dNBxwcHe7fWs3ZIOTx0GOk6K3M5ZFU/RO6X8oRF0AmRUIoyMLqTm', 'User One', 'user1@example.com', '2000-01-01', 'ACTIVE'),
('admin1', '$2a$10$mrqs1b.RZA5SO3.wWnTNo./DzcsJ1oz9JPQeptR3X6PDVxSSVq/PG', 'Admin One', 'admin1@example.com', '1990-01-01', 'ACTIVE'),
('superadmin1', '$2a$10$J9LgCk7Qpn5XU3Tqs9GRLu1fOjwpNQhCQK.WaEx0ZrqqRpDVy5FKS', 'Superadmin One', 'superadmin1@example.com', '1980-01-01', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id)
SELECT u.idUser, r.idRole FROM users u, roles r 
WHERE 
    (u.username = 'user1' AND r.roleName = 'ROLE_USER') OR
    (u.username = 'admin1' AND r.roleName = 'ROLE_ADMIN') OR
    (u.username = 'superadmin1' AND r.roleName = 'ROLE_SUPERADMIN');