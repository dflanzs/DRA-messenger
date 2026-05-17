package com.tfg.backend.TrustCircles;

/**
 * Estado de una solicitud de comunicación entre dos usuarios que no comparten
 * círculo de confianza.
 */
public enum CommunicationRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
