package co.inter.piggies.service;

import co.inter.piggies.domain.appointment.Appointment;
import co.inter.piggies.domain.appointment.AppointmentCancelRequestDTO;
import co.inter.piggies.domain.appointment.AppointmentRequestDTO;
import co.inter.piggies.domain.appointment.AppointmentResponseDTO;
import co.inter.piggies.domain.doctor.Doctor;
import co.inter.piggies.domain.doctor.DoctorRequestDTO;
import co.inter.piggies.domain.patient.Patient;
import co.inter.piggies.domain.patient.PatientRequestDTO;
import co.inter.piggies.domain.specialty.Specialty;
import co.inter.piggies.domain.specialty.SpecialtyRequestDTO;
import co.inter.piggies.repository.AppointmentRepository;
import co.inter.piggies.repository.DoctorRepository;
import co.inter.piggies.repository.PatientRepository;
import co.inter.piggies.repository.SpecialtyRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.core.ThrowingRunnable;
import org.zalando.problem.ThrowableProblem;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/*
 *-----------------------------------------------------------------------*
 * Teste de INTEGRAÇÃO do AppointmentService usando o banco (e o Kafka) do  *
 * SEU Docker, configurados no application.yml.                          *
 * @MicronautTest: sobe o Micronaut, lê o application.yml normalmente e  *
 * injeta os beans reais.                                                *
 *                                                                       *
 * IMPORTANTE: o Docker (docker compose up) precisa estar rodando ANTES  *
 * de executar os testes.                                                *
 *                                                                       *
 * transactional = true (padrão): cada teste roda dentro de uma          *
 * transação que sofre ROLLBACK no final -> nada do que os testes gravam *
 * fica no seu banco.                                                    *
 *-----------------------------------------------------------------------*
 */
@MicronautTest(transactional = true)
@NoArgsConstructor
class AppointmentServiceTest {

    // Service REAL, injetado pelo Micronaut
    @Inject
    AppointmentService appointmentService;

    // Repository REAL: prepara dados (Given) e confere o que foi gravado (Then)
    @Inject
    AppointmentRepository appointmentRepository;

    @Inject
    DoctorRepository doctorRepository;

    @Inject
    PatientRepository patientRepository;

    @Inject
    SpecialtyRepository specialtyRepository;


    // Tempo máximo de espera e intervalo entre as tentativas
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration INTERVALO = Duration.ofMillis(200);

    private void salvarAppointmentParaPatient(Patient p) {

        //Chama função que simula um body passado
        AppointmentRequestDTO body = criarAppointmentRequest();

        Appointment appointment = new Appointment();

        appointment.setId(UUID.randomUUID());
        appointment.setDoctor(doctorRepository.findById(body.doctorId()).orElse(null));
        appointment.setPatient(p);
        appointment.setStatus("PENDING");
        appointment.setScheduledAt(body.scheduledAt());
        appointment.setCreatedAt(OffsetDateTime.now());

        appointmentRepository.save(appointment);
    }

    private void salvarAppointmentParaDoctor(Doctor d) {

        //Chama função que simula um body passado
        AppointmentRequestDTO body = criarAppointmentRequest();
        Appointment appointment = new Appointment();

        appointment.setId(UUID.randomUUID());
        appointment.setDoctor(d);
        appointment.setPatient(patientRepository.findById(body.patientId()).orElse(null));
        appointment.setStatus("PENDING");
        appointment.setScheduledAt(body.scheduledAt());
        appointment.setCreatedAt(OffsetDateTime.now());

        appointmentRepository.save(appointment);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: salvarAppointment                                    *
     *--------------------------------------------------------------------*
     * Descrição: Cria e SALVA uma Appointment no Postgres do Docker.        *
     * (Desfeito no rollback ao final do teste.)                          *
     *--------------------------------------------------------------------*
     */
    public Appointment salvarAppointment() {

        //Chama função que simula um body passado
        AppointmentRequestDTO body = criarAppointmentRequest();

        Appointment appointment = new Appointment();

        appointment.setId(UUID.randomUUID());
        appointment.setDoctor(doctorRepository.findById(body.doctorId()).orElse(null));
        appointment.setPatient(patientRepository.findById(body.patientId()).orElse(null));
        appointment.setStatus("PENDING");
        appointment.setScheduledAt(body.scheduledAt());
        appointment.setCreatedAt(OffsetDateTime.now());

        return appointmentRepository.save(appointment);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: criarRequest                                      *
     *--------------------------------------------------------------------*
     * Descrição: Monta um AppointmentRequestDTO de exemplo (body).          *
     *--------------------------------------------------------------------*
     */
    private AppointmentRequestDTO criarAppointmentRequest() {
        Patient p = salvarPatient();
        Doctor d = salvarDoctor();

        return new AppointmentRequestDTO(
                p.getId(),
                d.getId(),
                gerarDataHora()
        );
    }

    private AppointmentCancelRequestDTO criarAppointmentCancelRequest() {
        return new AppointmentCancelRequestDTO(
                "Por que Deus quis"
        );
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: gerarDataHora                                     *
     *--------------------------------------------------------------------*
     * Descrição: Gera um OffsetDateTime aleatório no FUTURO (1 a 365     *
     *            dias), entre 08:00 e 17:30, em slots de 30 minutos e    *
     *            sem segundos, no fuso de São Paulo (-03:00).            *
     *--------------------------------------------------------------------*
     */
    public OffsetDateTime gerarDataHora() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        int dias = random.nextInt(1, 366);   // 1 a 365 dias à frente
        int hora = random.nextInt(8, 18);    // 08h a 17h
        int minuto = random.nextBoolean() ? 0 : 30;

        return OffsetDateTime.now(ZoneOffset.ofHours(-3))
                .plusDays(dias)
                .withHour(hora)
                .withMinute(minuto)
                .truncatedTo(ChronoUnit.MINUTES);  // zera segundos e nanos
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: salvarDoctor                                    *
     *--------------------------------------------------------------------*
     * Descrição: Cria e SALVA uma Doctor no Postgres do Docker.        *
     * (Desfeito no rollback ao final do teste.)                          *
     *--------------------------------------------------------------------*
     */
    private Doctor salvarDoctor() {

        //Chama função que simula um body passado
        DoctorRequestDTO body = criarDoctorRequest();

        Doctor doctor = new Doctor();

        // Preencha aqui os campos obrigatórios da Doctor...
        //Ex: doctor.setType(body.type);
        doctor.setName(body.name());
        doctor.setCrm(body.crm());
        doctor.setSpecialty(specialtyRepository.findByName(body.name()).orElse(null));

        return doctorRepository.save(doctor);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: criarRequest                                      *
     *--------------------------------------------------------------------*
     * Descrição: Monta um DoctorRequestDTO de exemplo (body).          *
     *--------------------------------------------------------------------*
     */
    private DoctorRequestDTO criarDoctorRequest() {


        return new DoctorRequestDTO(
                "Ronaldo " + UUID.randomUUID(),
                "CRM " + UUID.randomUUID(),
                salvarSpecialty().getName()
        );
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: salvarPatient                                    *
     *--------------------------------------------------------------------*
     * Descrição: Cria e SALVA uma Patient no Postgres do Docker.        *
     * (Desfeito no rollback ao final do teste.)                          *
     *--------------------------------------------------------------------*
     */
    private Patient salvarPatient() {

        //Chama função que simula um body passado
        PatientRequestDTO body = criarPatientRequest();

        Patient patient = new Patient();

        // Preencha aqui os campos obrigatórios da $Entity$...
        //Ex: patient.setType(body.type);

        patient.setName(body.name());
        patient.setEmail(body.email());
        patient.setPhone(body.phone());
        patient.setCreatedAt(OffsetDateTime.now());

        return patientRepository.save(patient);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: criarRequest                                      *
     *--------------------------------------------------------------------*
     * Descrição: Monta um PatientRequestDTO de exemplo (body).          *
     *--------------------------------------------------------------------*
     */
    private PatientRequestDTO criarPatientRequest() {
        return new PatientRequestDTO(
                "Ronaldo",
                "ronadlo" + UUID.randomUUID() + "@gmail.com",
                "31971525231"
        );
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: salvarSpecialty                                    *
     *--------------------------------------------------------------------*
     * Descrição: Cria e SALVA uma Specialty no Postgres do Docker.        *
     * (Desfeito no rollback ao final do teste.)                          *
     *--------------------------------------------------------------------*
     */
    private Specialty salvarSpecialty() {

        //Chama função que simula um body passado
        SpecialtyRequestDTO body = criarSpecialtyRequest();

        Specialty specialty = new Specialty();

        specialty.setName(body.name());
        specialty.setDescription(body.description());

        return specialtyRepository.save(specialty);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: criarRequest                                      *
     *--------------------------------------------------------------------*
     * Descrição: Monta um SpecialtyRequestDTO de exemplo (body).          *
     *--------------------------------------------------------------------*
     */
    private SpecialtyRequestDTO criarSpecialtyRequest() {
        return new SpecialtyRequestDTO(
                "Spec " + UUID.randomUUID(),
                "Description tra la la"
        );
    }

    // ===================== getAppointmentById =====================

    @Test
    void deveRetornarAppointmentQuandoIdExistir() {
        // Given: uma entidade gravada no banco
        Appointment salvo = salvarAppointment();

        // When
        AppointmentResponseDTO resp = appointmentService.getAppointmentById(salvo.getId());

        // Then
        assertEquals(salvo.getId(), resp.appointmentId());
        // Confira aqui os demais campos do ResponseDTO...
    }

    @Test
    void deveRetornar404QuandoIdNaoExistirAoBuscar() {
        // Given: um id que com certeza NÃO existe no banco
        var id = UUID.randomUUID(); // ex.: UUID.randomUUID()

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> appointmentService.getAppointmentById(id));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Agendamento de Id: " + id + " não existe!", ex.getDetail());
    }

    // ===================== getDoctorAppointmentById =====================
    @Test
    void deveRetornarDoctorAppointmentQuandoIdExistir() {
        // Given: uma entidade gravada no banco
        Doctor d = salvarDoctor();
        salvarAppointmentParaDoctor(d);
        salvarAppointmentParaDoctor(d);
        salvarAppointmentParaDoctor(d);
        salvarAppointmentParaDoctor(d);

        // When
        List<AppointmentResponseDTO> resp = appointmentService.getPatientAppointments(d.getId());

        // Then
        assertEquals(d.getId(), resp.getFirst().patientId());
        assertEquals(d.getId(), resp.getLast().patientId());
        // Confira aqui os demais campos do ResponseDTO...
    }

    @Test
    void deveRetornar404QuandoDoctorIdNaoExistirAoBuscar() {
        // Given: um id que com certeza NÃO existe no banco
        Doctor d = salvarDoctor();
        salvarAppointmentParaDoctor(d);
        salvarAppointmentParaDoctor(d);
        salvarAppointmentParaDoctor(d);
        salvarAppointmentParaDoctor(d);
        var id = UUID.randomUUID(); // ex.: UUID.randomUUID()

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> appointmentService.getAppointmentById(id));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Paciente" + " de Id: " + id + " não existe!", ex.getDetail());
    }

    // ===================== getPatientAppointmentById =====================

    @Test
    void deveRetornarPatientAppointmentQuandoIdExistir() {
        // Given: uma entidade gravada no banco
        Patient p = salvarPatient();
        salvarAppointmentParaPatient(p);
        salvarAppointmentParaPatient(p);
        salvarAppointmentParaPatient(p);
        salvarAppointmentParaPatient(p);

        // When
        List<AppointmentResponseDTO> resp = appointmentService.getPatientAppointments(p.getId());

        // Then
        assertEquals(p.getId(), resp.getFirst().patientId());
        assertEquals(p.getId(), resp.getLast().patientId());
        // Confira aqui os demais campos do ResponseDTO...
    }

    @Test
    void deveRetornar404QuandoPatientIdNaoExistirAoBuscar() {
        // Given: um id que com certeza NÃO existe no banco
        Patient p = salvarPatient();
        salvarAppointmentParaPatient(p);
        salvarAppointmentParaPatient(p);
        salvarAppointmentParaPatient(p);
        salvarAppointmentParaPatient(p);
        var id = UUID.randomUUID(); // ex.: UUID.randomUUID()

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> appointmentService.getAppointmentById(id));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Agendamento" + " de Id: " + id + " não existe!", ex.getDetail());
    }


    // ===================== createAppointment =====================

    @Test
    void deveCriarAppointment() {
        // When
        AppointmentResponseDTO resp = appointmentService.createAppointment(criarAppointmentRequest());

        // Then: foi REALMENTE gravado no Postgres
        assertNotNull(resp.appointmentId());
        assertTrue(appointmentRepository.existsById(resp.appointmentId()));
    }

    @Test
    void deveRetornar404PorCausadoPatientCriarAppointment() {
        Doctor d = salvarDoctor();
        UUID p = UUID.randomUUID();
        AppointmentRequestDTO req = new AppointmentRequestDTO(
                p,
                d.getId(),
                gerarDataHora()
        );

        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> appointmentService.createAppointment(req));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Paciente de Id: " + p + " não existe!", ex.getDetail());
    }

    @Test
    void deveRetornar404PorCausadoDoctorCriarAppointment() {
        Patient d = salvarPatient();
        UUID p = UUID.randomUUID();
        AppointmentRequestDTO req = new AppointmentRequestDTO(
                d.getId(),
                p,
                gerarDataHora()
        );

        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> appointmentService.createAppointment(req));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Médico de Id: " + p + " não existe!", ex.getDetail());
    }

    @Test
    void deveRetornar400PorCausadoDoctorCriarAppointment() {
        Patient p = salvarPatient();
        Doctor d = salvarDoctor();

        d.setActive(false);
        Doctor dInativo = doctorRepository.update(d);

        AppointmentRequestDTO req = new AppointmentRequestDTO(
                p.getId(),
                dInativo.getId(),
                gerarDataHora()
        );


        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> appointmentService.createAppointment(req));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Medico de Id: " + dInativo.getId() + " está desativado!" + p, ex.getDetail());
    }

    @Test
    void deveRetornar400PorCausadaHoraCriarAppointment() {
        Patient p = salvarPatient();
        Doctor d = salvarDoctor();
        OffsetDateTime agora = OffsetDateTime.now();

        AppointmentRequestDTO req = new AppointmentRequestDTO(
                p.getId(),
                d.getId(),
                agora.minusHours(2)
        );

        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> appointmentService.createAppointment(req));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Data de agendamento tem que ser superior a atual" + p, ex.getDetail());
    }

    // ===================== updateAppointment =====================

    @Test
    void deveAtualizarAppointmentParaCancelar() {
        // Given
        Appointment salvo = salvarAppointment();

        // When
        appointmentService.cancelAppointment(salvo.getId(), criarAppointmentCancelRequest());

        // Then: busca de novo no banco e confere os dados alterados
        Appointment atualizado = appointmentRepository.findById(salvo.getId()).orElseThrow();
        assertEquals(atualizado.getStatus(), "CANCELED");
        // Confira aqui se os campos foram alterados com os dados do body...
    }

    @Test
    void deveRetornar404QuandoIdNaoExistirAoAtualizar() {
        // Given: um id que com certeza NÃO existe no banco
        var id = UUID.randomUUID(); // ex.: UUID.randomUUID()

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> appointmentService.cancelAppointment(id, criarAppointmentCancelRequest()));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Agendamento de Id: " + id + " não existe!", ex.getDetail());
    }

    @Test
    void deveRetornar409QuandoCancelar2vezesAoAtualizar() {
        // Given: um id que com certeza NÃO existe no banco
        Appointment a = salvarAppointment();

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> appointmentService.cancelAppointment(a.getId(), criarAppointmentCancelRequest()));

        assertEquals(409, ex.getStatus().getStatusCode());
        assertEquals("Consulta de Id: " + a.getId() + " já foi cancelada", ex.getDetail());
    }
}