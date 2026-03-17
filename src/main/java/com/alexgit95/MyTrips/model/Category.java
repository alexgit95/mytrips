package com.alexgit95.MyTrips.model;

public enum Category {
    TRANSPORT("Transport"),
    HÉBERGEMENT("Hébergement"),
    RESTAURATION("Restauration"),
    LOISIRS("Loisirs"),
    SHOPPING("Shopping"),
    COURSES("Courses"),
    SORTIES("Sorties"),
    AUTRE("Autre");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
