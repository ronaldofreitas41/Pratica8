package co.inter.piggies.service;

import co.inter.piggies.domain.appointment.Appointment;
import co.inter.piggies.domain.appointment.AppointmentRequestDTO;
import co.inter.piggies.domain.appointment.AppointmentResponseDTO;
import co.inter.piggies.domain.doctor.Doctor;
import co.inter.piggies.domain.patient.Patient;
import co.inter.piggies.kafka.events.AppointmentCreatedEventDTO;
import co.inter.piggies.kafka.producer.AppointmentCreatedProducer;
import co.inter.piggies.repository.AppointmentRepository;
import co.inter.piggies.repository.DoctorRepository;
import co.inter.piggies.repository.PatientRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.problem.HttpStatusType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;

import java.time.OffsetDateTime;
import java.util.UUID;

@Singleton
public class AppointmentService {

    @Inject
    AppointmentCreatedProducer producer;

    @Inject
    AppointmentRepository appointmentRepository;

    @Inject
    DoctorRepository doctorRepository;

    @Inject
    PatientRepository patientRepository;

    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO body) {

        //1 -> Paciente precisa existir
        Patient patient = patientRepository.findById(body.patientId()).
                orElseThrow(() -> notFound("Paciente", body.patientId()));

        //2 -> Medico precisa existir
        Doctor doctor = doctorRepository.findById(body.doctorId())
                .orElseThrow(() -> notFound("Médico", body.doctorId()));

        //3 -> Medico is Active
        if (!doctor.isActive()) {
            throw  Problem.builder()
                    .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
                    .withTitle("Não encontrado")
                    .withDetail("Medico de Id: " + body.doctorId() + " está desativado!")
                    .build();
        }

        //4-> Não aceitar agendamentos em datas passadas.
        if(body.scheduledAt().isBefore(OffsetDateTime.now())){
            throw  Problem.builder()
                    .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
                    .withTitle("Não encontrado")
                    .withDetail("Data de agendamento tem que ser superior a atual")
                    .build();
        }

        //5 -> Criar a consulta com status PENDING.
        Appointment appointment = new Appointment();

        appointment.setId(UUID.randomUUID());
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setStatus("PENDING");
        appointment.setScheduledAt(body.scheduledAt());
        appointment.setCreatedAt(OffsetDateTime.now());

        Appointment appointmentSaved = appointmentRepository.save(appointment);

        AppointmentCreatedEventDTO event = new AppointmentCreatedEventDTO(
                appointmentSaved.getId(),
                appointmentSaved.getPatient().getId(),
                appointmentSaved.getDoctor().getId(),
                appointmentSaved.getScheduledAt()
        );

        producer.send(appointmentSaved.getDoctor().getId(),event);

        return AppointmentResponseDTO.from(appointmentSaved);
    }

    private static ThrowableProblem notFound(String nome, UUID id) {

        return Problem.builder()
                .withStatus(new HttpStatusType(HttpStatus.NOT_FOUND))
                .withTitle("Não encontrado")
                .withDetail(nome + " de Id: " + id + " não existe!")
                .build();
    }

}
