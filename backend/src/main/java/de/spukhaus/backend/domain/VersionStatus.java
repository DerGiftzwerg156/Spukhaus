package de.spukhaus.backend.domain;

public enum VersionStatus {
    /** In Bearbeitung durch den Creator, noch nicht zur Prüfung eingereicht. */
    DRAFT,
    /** Vom Creator eingereicht, wartet auf Prüfung durch einen Admin. */
    PENDING_REVIEW,
    /** Von einem Admin bestätigt und aktuell für alle sichtbar. */
    PUBLISHED,
    /** Von einem Admin abgelehnt, Creator kann sie überarbeiten (-> zurück zu DRAFT). */
    REJECTED,
    /** Ehemals veröffentlichte Version, wurde durch eine neuere PUBLISHED-Version abgelöst. */
    SUPERSEDED
}
