package com.cinema.cinema.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="movies")
@Getter @Setter
public class MovieEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private int duration_minutes;
    private String poster_file_name;
}
