package com.cinema.cinema.domain.model;


public record Movie(
        Long id,
        String title,
        String description,
        int durationMinutes,
        String poster,
        String posterUrl
) {

    public Movie {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        title = title.trim();

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        description = description.trim();

        durationMinutes = durationMinutes > 0 ? durationMinutes : 0;
        if (durationMinutes == 0) {
            throw new IllegalArgumentException("Duration cannot be zero");
        }
        if (poster == null || poster.isBlank()) {
            throw new IllegalArgumentException("Poster cannot be null");
        }
        if (posterUrl != null) {
            posterUrl = posterUrl.trim();
            if (posterUrl.isEmpty()) {
                throw new IllegalArgumentException("Poster URL cannot be empty");
            }
        }
    }

    public Movie withPosterUrl(String newUrl){
        return new Movie(id,title,description, durationMinutes,poster,newUrl);
    }



}
