package co.inter.piggies.service;

import co.inter.piggies.domain.appointment.*;
import co.inter.piggies.domain.doctor.Doctor;
import co.inter.piggies.domain.patient.Patient;
import co.inter.piggies.kafka.consumer.AppointmentProcessingConsumer;
import co.inter.piggies.kafka.events.AppointmentCreatedEventDTO;
import co.inter.piggies.kafka.events.AppointmentProcessedEventDTO;
import co.inter.piggies.kafka.producer.AppointmentCreatedProducer;
import co.inter.piggies.repository.AppointmentRepository;
import co.inter.piggies.repository.DoctorRepository;
import co.inter.piggies.repository.PatientRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.problem.HttpStatusType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;

import java.time.OffsetDateTime;
import java.util.List;
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

    @Inject
    private static Logger LOG = LoggerFactory.getLogger(AppointmentProcessingConsumer.class);

    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO body) {

        //1 -> Paciente precisa existir
        Patient patient = patientRepository.findById(body.patientId()).
                orElseThrow(() -> notFound("Paciente", body.patientId()));

        //2 -> Medico precisa existir
        Doctor doctor = doctorRepository.findById(body.doctorId())
                .orElseThrow(() -> notFound("Médico", body.doctorId()));

        //3 -> Medico is Active
        if (!doctor.isActive()) {
            throw Problem.builder()
                    .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
                    .withTitle("Não encontrado")
                    .withDetail("Medico de Id: " + body.doctorId() + " está desativado!")
                    .build();
        }

        //4-> Não aceitar agendamentos em datas passadas.
        if (body.scheduledAt().isBefore(OffsetDateTime.now())) {
            throw Problem.builder()
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

        producer.send(appointmentSaved.getDoctor().getId(), event);

        return AppointmentResponseDTO.from(appointmentSaved);
    }

    @Transactional
    public void processingAppointment(AppointmentCreatedEventDTO event) {

        //1 -> Buscar o agendamento pelo ID.
        Appointment appointment = appointmentRepository.findById(event.appointmentId()).orElse(null);

        if (appointment == null) {
            makeWarnLogNotExists("Agendamento", event.appointmentId());
            return;
        }

        //2 -> Verificar se ainda está PENDING.
        if (!appointment.getStatus().equals("PENDING")) {
            LOG.warn("Agendamento de id: {} já foi processado", appointment.getId());
            return;
        }

        //3 -> Verificar se o paciente existe.
        Patient patient = patientRepository.findById(event.patientId()).orElse(null);
        if (patient == null) {
            makeWarnLogNotExists("Paciente", event.patientId());
            appointment.setStatus("REJECTED");
            appointment.setCancellationReason("PATIENT_NOT_FOUND");
        }

        //4 -> Verificar se o médico existe e está ativo.
        Doctor doctor = doctorRepository.findById(event.doctorId()).orElse(null);

        if (doctor == null) {
            makeWarnLogNotExists("Medico", event.patientId());
            appointment.setStatus("REJECTED");
            appointment.setCancellationReason("DOCTOR_NOT_FOUND");
        }

        if (doctor.isActive()) {
            LOG.warn("Medico de id: {}, está inativo", doctor.getId());
            appointment.setStatus("REJECTED");
            appointment.setCancellationReason("DOCTOR_INACTIVE");
        }

        //5 -> Verificar se já existe uma consulta CONFIRMED para o mesmo médico e horário.
        //7 -> Se estiver ocupado, alterar para REJECTED.
        OffsetDateTime inicio = appointment.getScheduledAt().minusMinutes(30);
        OffsetDateTime fim = appointment.getScheduledAt().plusMinutes(30);
        if (appointmentRepository.existsByDoctorIdAndStatusAndScheduledAtGreaterThanAndScheduledAtLessThan(
                event.doctorId(),
                "CONFIRMED",
                inicio,
                fim
        )) {
            LOG.warn("Horario não disponivel para o Medico de id: {} ", event.doctorId());
            appointment.setStatus("REJECTED");
            appointment.setCancellationReason("DOCTOR_UNAVAILABLE");
        } else {
            //6 -> Se estiver disponível, alterar para CONFIRMED.
            appointment.setStatus("CONFIRMED");
        }
        //8 -> Publicar o resultado no tópico appointment.processed.
        Appointment appointmentUpdated = appointmentRepository.update(appointment);

        AppointmentProcessedEventDTO eventProcessed = new AppointmentProcessedEventDTO(
                appointmentUpdated.getId(),
                appointmentUpdated.getStatus(),
                appointmentUpdated.getCancellationReason()
                );
    }

    public AppointmentCancelResponseDTO cancelAppointment(UUID id,AppointmentCancelRequestDTO body){
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(()-> notFound("Agendamento",id));

        appointment.setStatus("CANCELED");
        appointment.setCancellationReason(body.reason());

        Appointment appointmentUpdated = appointmentRepository.update(appointment);

        return AppointmentCancelResponseDTO.from(appointmentUpdated);
    }

    public AppointmentResponseDTO getAppointmentById(UUID id){
        Appointment appointment = appointmentRepository
                .findById(id).orElseThrow(()-> notFound("Agendamento",id));

        return AppointmentResponseDTO.from(appointment);
    }

    public List<AppointmentResponseDTO> getPatientAppointments(UUID id){
        Patient patient = patientRepository
                .findById(id).orElseThrow(()-> notFound("Paciente",id));

        return patient.getApointments().stream().map(AppointmentResponseDTO::from).toList();
    }

    public List<AppointmentResponseDTO> getDoctorAppointments(UUID id){
        Doctor doctor = doctorRepository
                .findById(id).orElseThrow(()-> notFound("Medico",id));

        return doctor.getApointments().stream().map(AppointmentResponseDTO::from).toList();
    }

    private static ThrowableProblem notFound(String nome, UUID id) {

        return Problem.builder()
                .withStatus(new HttpStatusType(HttpStatus.NOT_FOUND))
                .withTitle("Não encontrado")
                .withDetail(nome + " de Id: " + id + " não existe!")
                .build();
    }

    private static void makeWarnLogNotExists(String nome, UUID id) {
        LOG.warn("{} de id: {} não existe!", nome, id);
    }


}
