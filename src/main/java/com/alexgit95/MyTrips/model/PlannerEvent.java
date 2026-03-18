package com.alexgit95.MyTrips.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "planner_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannerEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de l'événement est obligatoire")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "La date et heure sont obligatoires")
    @Column(nullable = false)
    private LocalDateTime eventDateTime;

    @Column
    private String location;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Trip trip;
}
