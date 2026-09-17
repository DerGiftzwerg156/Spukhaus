package de.spukhaus.backend.storage;

public record StoredImage(String storageKey, String contentType, long sizeBytes) {
}
