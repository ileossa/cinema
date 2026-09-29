-- 1. Table des rôles
CREATE TABLE roles
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

-- 2. Table d'association utilisateurs <-> rôles
CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

-- 3. Insertion des rôles par défaut
INSERT INTO roles (name, description)
VALUES ('CLIENT', 'Standard client user with booking privileges'),
       ('ADMIN', 'Administrator user with management privileges');

-- 4. Attribution du rôle CLIENT à tous les utilisateurs existants
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         CROSS JOIN roles r
WHERE r.name = 'CLIENT';

-- 5. Création du compte administrateur initial (root@root.root / mot de passe root)
INSERT INTO users (email, full_name, created_at)
VALUES ('root@root.root', 'Root Administrator', CURRENT_TIMESTAMP);

-- 6. Attribution du rôle ADMIN au compte root
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         CROSS JOIN roles r
WHERE u.email = 'root@root.root'
  AND r.name = 'ADMIN';
