package co.inter.piggies.domain.doctor;

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
public record DoctorResponseDTO(
    UUID id,
    String specialty,
    String name,
    String crm,
    Boolean active,
    OffsetDateTime createdAt
) {
    public static DoctorResponseDTO from(Doctor doctor) {
        return new DoctorResponseDTO(
                doctor.getId(),
                doctor.getSpecialty().getName(),
                doctor.getName(),
                doctor.getName(),
                doctor.isActive(),
                doctor.getCreatedAt()
        );
    }
}
