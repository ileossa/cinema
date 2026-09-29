ALTER TABLE movies
    ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX idx_movies_is_published ON movies (is_published);
