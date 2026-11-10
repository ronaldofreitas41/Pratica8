package co.inter.piggies.kafka.events;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/*
 *-------------------------------------------------------------------------*
 * @Serdeable: o Micronaut Serialization gera, em tempo de compilação, o   *
 * código que converte JSON em objeto (e vice-versa). Sem ele, o Micronaut *
 * não consegue ler o body da requisição nem devolver este DTO como JSON.  *
 *-------------------------------------------------------------------------*
 */

@Serdeable
public record AppointmentProcessedEventDTO(
    UUID appointmentId,
    String status,
    String reason
) {}