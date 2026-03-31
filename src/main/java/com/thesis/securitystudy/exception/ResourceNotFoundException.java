package com.thesis.securitystudy.exception;

/**
 * Einfache Anwendungsexception für nicht gefundene Ressourcen.
 * Unterstützt sowohl single-message Konstruktor als auch
 * resource/field/value Konstruktor (häufig genutzt).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Komfort-Konstruktor, z. B. new ResourceNotFoundException("User", "id", 5)
     * erzeugt: "User not found with id : '5'"
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, String.valueOf(fieldValue)));
    }
}
