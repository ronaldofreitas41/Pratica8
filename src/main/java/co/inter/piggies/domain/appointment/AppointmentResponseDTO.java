package co.inter.piggies.domain.appointment;

import io.micronaut.serde.annotation.Serdeable;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/*
 *-------------------------------------------------------------------------*
 * @Serdeable: o Micronaut Serialization gera, em tempo de compilação, o   *
 * código que converte JSON em objeto (e vice-versa). Sem ele, o Micronaut *
 * não consegue ler o body da requisição nem devolver este DTO como JSON.  *
 *-------------------------------------------------------------------------*
 */
@Serdeable
public record AppointmentResponseDTO(
        UUID appointmentId,
        String patient,
        String doctor,
        String sheduledAt,
        String status
) {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    public static AppointmentResponseDTO from(Appointment appointment) {
        return new AppointmentResponseDTO(
                appointment.getId(),
                appointment.getPatient().getName(),
                appointment.getDoctor().getName(),
                formataData(appointment.getScheduledAt()),
                appointment.getStatus()
        );
    }

    private static String formataData(OffsetDateTime horario) {
        if (horario == null) {
            return null;
        }

        return horario.atZoneSameInstant(SAO_PAULO).format(FORMATO);
    }
}
