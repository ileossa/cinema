CREATE TABLE movies (
                        id BIGSERIAL PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        duration_minutes INT NOT NULL,
                        poster_file_name VARCHAR(255)
);

ALTER TABLE movies ADD CONSTRAINT chk_movies_duration CHECK (duration_minutes > 0);

CREATE TABLE screenings (
                            id BIGSERIAL PRIMARY KEY,
                            movie_id BIGINT NOT NULL,
                            start_date_time TIMESTAMP NOT NULL,
                            room_name VARCHAR(50) NOT NULL,
                            available_seats INT NOT NULL,
                            unit_price NUMERIC(5, 2) NOT NULL,
                            CONSTRAINT fk_screenings_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE
);
CREATE INDEX idx_screenings_movie_id ON screenings (movie_id);
CREATE INDEX idx_screenings_start_date_time ON screenings (start_date_time);
ALTER TABLE screenings ADD CONSTRAINT chk_screenings_seats CHECK (available_seats >= 0);
ALTER TABLE screenings ADD CONSTRAINT chk_screenings_price CHECK (unit_price >= 0);