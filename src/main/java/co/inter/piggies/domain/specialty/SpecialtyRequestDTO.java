package co.inter.piggies.domain.specialty;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

/*
 *-------------------------------------------------------------------------*
 * @Serdeable: o Micronaut Serialization gera, em tempo de compilação, o   *
 * código que converte JSON em objeto (e vice-versa). Sem ele, o Micronaut *
 * não consegue ler o body da requisição nem devolver este DTO como JSON.  *
 *-------------------------------------------------------------------------*
 */

@Serdeable
public record SpecialtyRequestDTO(
        @NotBlank
        String name,
        String description
) {
}
