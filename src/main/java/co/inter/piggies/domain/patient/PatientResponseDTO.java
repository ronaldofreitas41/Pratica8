package co.inter.piggies.domain.patient;

import io.micronaut.serde.annotation.Serdeable;

import java.time.OffsetDateTime;
import java.util.UUID;

/*
 *-------------------------------------------------------------------------*
 * @Serdeable: o Micronaut Serialization gera, em tempo de compilação, o   *
 * código que converte JSON em objeto (e vice-versa). Sem ele, o Micronaut *
 * não consegue ler o body da requisição nem devolver este DTO como JSON.  *
 *-------------------------------------------------------------------------*
 */
@Serdeable
public record PatientResponseDTO(
        UUID id,
        String name,
        String email,
        String phone,
        OffsetDateTime createdAt
) {
    // Converte a entidade em DTO num lugar só
    public static PatientResponseDTO from(Patient patient) {
        return new PatientResponseDTO(
                patient.getId(),
                patient.getName(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getCreatedAt()
        );
    }
}
