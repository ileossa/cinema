package com.cinema.cinema.domain.model;


public record Movie(
        Long id,
        String title,
        String description,
        int time,
        String poster,
        String posterUrl
) {
    public Movie withUrlPoster(String newUrl){
        return new Movie(id,title,description,time,poster,newUrl);
    }
}
