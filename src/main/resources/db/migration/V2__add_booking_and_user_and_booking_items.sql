-- 1. Table des utilisateurs
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       full_name VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_email ON users (email);

-- 2. Table des réservations (remplace le concept de panier séparé)
CREATE TABLE bookings (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          total_amount NUMERIC(7, 2) NOT NULL,
                          status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                          expires_at TIMESTAMP NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id),
                          CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
                          CONSTRAINT chk_bookings_total_amount CHECK (total_amount >= 0)
);
CREATE INDEX idx_bookings_user_id ON bookings (user_id);
CREATE INDEX idx_bookings_status_expires ON bookings (status, expires_at);

-- 3. Détail des places réservées par séance
CREATE TABLE booking_items (
                               id BIGSERIAL PRIMARY KEY,
                               booking_id BIGINT NOT NULL,
                               screening_id BIGINT NOT NULL,
                               seat_count INT NOT NULL,
                               unit_price NUMERIC(5, 2) NOT NULL,
                               CONSTRAINT fk_booking_items_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
                               CONSTRAINT fk_booking_items_screening FOREIGN KEY (screening_id) REFERENCES screenings (id),
                               CONSTRAINT chk_booking_items_seat_count CHECK (seat_count > 0),
                               CONSTRAINT chk_booking_items_unit_price CHECK (unit_price >= 0)
);
CREATE INDEX idx_booking_items_booking_id ON booking_items (booking_id);
CREATE INDEX idx_booking_items_screening_id ON booking_items (screening_id);