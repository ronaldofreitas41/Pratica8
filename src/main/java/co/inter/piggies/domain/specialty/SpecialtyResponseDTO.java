package co.inter.piggies.domain.specialty;

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
public record SpecialtyResponseDTO(
        UUID id,
        String name,
        String description
) {
    public static SpecialtyResponseDTO from(Specialty specialty) {
        return new SpecialtyResponseDTO(
                specialty.getId(),
                specialty.getName(),
                specialty.getDescription()
                );
    }
}
