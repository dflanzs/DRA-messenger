package com.tfg.backend.Chat.dto;

import com.tfg.backend.TrustCircles.dto.CommunicationRequestDto;

/**
 * Resultado de pedir un chat directo: o bien el chat está activo (los usuarios
 * comparten círculo de confianza), o bien se ha generado una solicitud de
 * comunicación pendiente.
 *
 * <p>{@code chat} es no nulo cuando {@code status} es {@code "ACTIVE"};
 * {@code request} es no nulo cuando {@code status} es {@code "PENDING"}.
 */
public record DirectChatResultDto(
    String status,
    DirectChatSummaryDto chat,
    CommunicationRequestDto request
) {
}
