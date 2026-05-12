package com.alexgit95.MyTrips.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "accommodation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du logement est obligatoire")
    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    @NotNull(message = "La date d'arrivée est obligatoire")
    @Column(nullable = false)
    private LocalDate arrivalDate;

    @NotNull(message = "La date de départ est obligatoire")
    @Column(nullable = false)
    private LocalDate departureDate;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Trip trip;
}
