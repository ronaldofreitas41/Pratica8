package co.inter.piggies.domain.doctor;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/*
 *-------------------------------------------------------------------------*
 * @Serdeable: o Micronaut Serialization gera, em tempo de compilação, o   *
 * código que converte JSON em objeto (e vice-versa). Sem ele, o Micronaut *
 * não consegue ler o body da requisição nem devolver este DTO como JSON.  *
 *-------------------------------------------------------------------------*
 */

@Serdeable
public record DoctorRequestDTO(
        @NotBlank
        String name,
        @NotBlank
        String crm,
        @NotBlank
        String specialty
        ) {
}
