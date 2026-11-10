package co.inter.piggies.domain.appointment;

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
public record AppointmentCancelResponseDTO(
        UUID appointmentid,
        String patientName,
        String DoctorName,
        String status,
        String reason

) {
    public static AppointmentCancelResponseDTO from(Appointment appointment) {
        return new AppointmentCancelResponseDTO(
                appointment.getId(),
                appointment.getPatient().getName(),
                appointment.getDoctor().getName(),
                appointment.getStatus(),
                appointment.getCancellationReason()
        );
    }
}
