CREATE TABLE movies (
                        id BIGSERIAL PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        duration_minutes INT NOT NULL,
                        poster_file_name VARCHAR(255)
);

CREATE TABLE screenings (
                            id BIGSERIAL PRIMARY KEY,
                            movie_id BIGINT NOT NULL,
                            start_date_time TIMESTAMP NOT NULL,
                            room_name VARCHAR(50) NOT NULL,
                            available_seats INT NOT NULL,
                            unit_price NUMERIC(5, 2) NOT NULL,
                            CONSTRAINT fk_screenings_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE
);