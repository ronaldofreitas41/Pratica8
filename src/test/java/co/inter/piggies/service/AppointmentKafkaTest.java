package co.inter.piggies.service;

import co.inter.piggies.domain.appointment.Appointment;
import co.inter.piggies.domain.appointment.AppointmentRequestDTO;
import co.inter.piggies.domain.appointment.AppointmentResponseDTO;
import co.inter.piggies.domain.doctor.Doctor;
import co.inter.piggies.domain.doctor.DoctorRequestDTO;
import co.inter.piggies.domain.patient.Patient;
import co.inter.piggies.domain.patient.PatientRequestDTO;
import co.inter.piggies.domain.specialty.Specialty;
import co.inter.piggies.domain.specialty.SpecialtyRequestDTO;
import co.inter.piggies.kafka.Topics;
import co.inter.piggies.kafka.events.AppointmentCreatedEventDTO;
import co.inter.piggies.kafka.events.AppointmentProcessedEventDTO;
import co.inter.piggies.repository.AppointmentRepository;
import co.inter.piggies.repository.DoctorRepository;
import co.inter.piggies.repository.PatientRepository;
import co.inter.piggies.repository.SpecialtyRepository;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/*
 *-----------------------------------------------------------------------*
 * Teste ASSÍNCRONO do Appointment com Kafka + Awaitility.                  *
 * O Kafka processa a mensagem em OUTRA thread, um tempo depois.         *
 * O Awaitility repete a verificação até passar (ou estourar o tempo).   *
 * @MicronautTest: sobe o Micronaut usando o application.yml (Docker).   *
 * transactional = false: OBRIGATÓRIO. O consumer roda em outra          *
 * transação e não enxergaria os dados de um teste com rollback.         *
 * Os dados FICAM no banco -> limpeza no @AfterEach.                     *
 * Testes síncronos ficam no AppointmentServiceTest (transactional = true). *
 *-----------------------------------------------------------------------*
 */
@MicronautTest(transactional = false)
class AppointmentKafkaTest {

    /*
     *-----------------------------------------------------------------------*
     * Tempos de espera:                                                     *
     * TIMEOUT: tempo máximo que o Awaitility espera.                        *
     * INTERVALO: intervalo entre as tentativas.                             *
     * ESTAVEL: tempo que o valor precisa ficar IGUAL (idempotência).        *
     * TIMEOUT precisa ser MAIOR que ESTAVEL.                                *
     *-----------------------------------------------------------------------*
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration INTERVALO = Duration.ofMillis(200);
    private static final Duration ESTAVEL = Duration.ofSeconds(3);

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

    // Producer de TESTE: envia mensagens direto no tópico de ENTRADA
    @Inject
    AppointmentTestProducer producer;

    // Listener de TESTE: guarda o que o SERVICE publica no tópico de ENTRADA
    @Inject
    AppointmentTestListener listener;

    // Listener de TESTE: guarda o que o CONSUMER publica no tópico de SAÍDA
    // (remova se o consumer não publicar outro evento)
    @Inject
    AppointmentSaidaTestListener saidaListener;

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

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: gerarDataHora                                     *
     *--------------------------------------------------------------------*
     * Descrição: Gera um OffsetDateTime aleatório no FUTURO (1 a 365     *
     *            dias), entre 08:00 e 17:30, em slots de 30 minutos e    *
     *            sem segundos, no fuso de São Paulo (-03:00).            *
     *--------------------------------------------------------------------*
     */
    private OffsetDateTime gerarDataHora() {
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
        Specialty spec = salvarSpecialty();

        // Preencha aqui os campos obrigatórios da Doctor...
        //Ex: doctor.setType(body.type);
        doctor.setName(body.name());
        doctor.setCrm(body.crm());
        doctor.setSpecialty(spec);
        doctor.setActive(true);

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

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: aguardar                                          *
     *--------------------------------------------------------------------*
     * Descrição: Função GENÉRICA. Repete a verificação até               *
     *            ela passar ou até estourar o TIMEOUT.                   *
     *            Uso: aguardar(() -> assertEquals(1, lista.size()));     *
     *--------------------------------------------------------------------*
     */
    private void aguardar(ThrowingRunnable verificacao) {
        await()
                .atMost(TIMEOUT)
                .pollInterval(INTERVALO)
                .untilAsserted(verificacao);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: aguardarEstavel                                   *
     *--------------------------------------------------------------------*
     * Descrição: Igual ao aguardar, mas a verificação precisa            *
     *            CONTINUAR passando por ESTAVEL (3s) seguidos.           *
     *            Usado na idempotência: dá tempo da 2ª mensagem          *
     *            chegar e mostrar se gera efeito em dobro.               *
     *--------------------------------------------------------------------*
     */
    private void aguardarEstavel(ThrowingRunnable verificacao) {
        await()
                .during(ESTAVEL)
                .atMost(TIMEOUT)
                .pollInterval(INTERVALO)
                .untilAsserted(verificacao);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: criarEvento                                       *
     *--------------------------------------------------------------------*
     * Descrição: Monta o AppointmentCreatedEventDTO a partir de uma             *
     *            Appointment já gravada.                                    *
     *--------------------------------------------------------------------*
     */
    private AppointmentCreatedEventDTO criarEvento(Appointment appointment) {
        return new AppointmentCreatedEventDTO(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getDoctor().getId(),
                appointment.getScheduledAt()
        );
    }

    private AppointmentCreatedEventDTO criarEventoErradoM(Appointment appointment) {
        return new AppointmentCreatedEventDTO(
                appointment.getId(),
                appointment.getPatient().getId(),
                UUID.randomUUID(),
                appointment.getScheduledAt()
        );
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: limpar                                            *
     *--------------------------------------------------------------------*
     * Descrição: Roda depois de CADA teste. Como não há                  *
     *            rollback, apaga o que os testes gravaram.               *
     *            ORDEM: apague primeiro quem tem a FK                    *
     *            (ex.: appointment antes de appointment).                      *
     *            ATENÇÃO: deleteAll() apaga a tabela inteira.            *
     *--------------------------------------------------------------------*
     */
    @AfterEach
    void limpar() {
        listener.limpar();
        saidaListener.limpar();
        // appointmentRepository.deleteAll();
        // appointmentRepository.deleteAll();
    }

    // ===================== service PUBLICA no Kafka =====================

    @Test
    void devePublicarEventoAoCriarAppointment() {
        // When: o service cria e publica o evento
        AppointmentResponseDTO resp = appointmentService.createAppointment(criarAppointmentRequest());

        // Then: espera o evento DESTA Appointment chegar no tópico
        aguardar(() -> {
            AppointmentCreatedEventDTO evento = listener.getRecebidas().stream()
                    .filter(e -> e.appointmentId().equals(resp.appointmentId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Evento ainda não chegou"));
            // Confira aqui os demais campos do evento...
        });
    }

    // ===================== idempotência: ALTERA valor =====================

    @Test
    void naoDeveAlterarEmDobroCREATEDQuandoAppointMentCreateChegarDuasVezes() {
        // Given: uma Appointment gravada e o valor ANTES do processamento
        Appointment appointment = salvarAppointment();
        String StautsAntes = appointment.getStatus();
        AppointmentCreatedEventDTO evento = criarEvento(appointment);

        // When: o MESMO evento é enviado 2 vezes
        producer.enviar(evento.doctorId(), evento);
        producer.enviar(evento.doctorId(), evento);

        // Then: mudou UMA vez só (ex.: estoque 10 -> 9, nunca 8)
        aguardarEstavel(() -> assertEquals("CONFIRMED",
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus()));

        aguardarEstavel(() -> assertEquals(1,
                saidaListener.getRecebidas().stream()
                        .filter(e -> e.appointmentId().equals(appointment.getId()))
                        .count()));
    }

    @Test
    void naoDeveAlterarEmDobroREJECTEDQuandoAppointMentCreateChegarDuasVezes() {
        // Given: uma Appointment gravada e o valor ANTES do processamento
        Appointment appointment = salvarAppointment();
        String StautsAntes = appointment.getStatus();
        AppointmentCreatedEventDTO evento = criarEventoErradoM(appointment);

        // When: o MESMO evento é enviado 2 vezes
        producer.enviar(evento.doctorId(), evento);
        producer.enviar(evento.doctorId(), evento);

        // Then: mudou UMA vez só (ex.: estoque 10 -> 9, nunca 8)
        aguardarEstavel(() -> assertEquals("REJECTED",
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus()));

        aguardarEstavel(() -> assertEquals(1,
                saidaListener.getRecebidas().stream()
                        .filter(e -> e.appointmentId().equals(appointment.getId()))
                        .count()));
    }

    // ===================== idempotência: PUBLICA evento =====================

    @Test
    void naoDevePublicarEmDobroQuandoAppointMentCreateChegarDuasVezes() {
        // Given: uma Appointment REALMENTE gravada (transactional = false)
        Appointment appointment = salvarAppointment();
        AppointmentCreatedEventDTO evento = criarEvento(appointment);

        // When: o MESMO evento é enviado 2 vezes
        producer.enviar(appointment.getDoctor().getId(), evento);
        producer.enviar(appointment.getDoctor().getId(), evento);

        // Then: o evento seguinte foi publicado UMA vez só para esta Appointment
        aguardarEstavel(() -> assertEquals(1,
                saidaListener.getRecebidas().stream()
                .filter(e -> e.appointmentId().equals(appointment.getId()))
                .count()));
    }

    /*
     *-----------------------------------------------------------------------*
     * Producer de TESTE: envia mensagens direto para o tópico de ENTRADA.   *
     * Só existe no classpath de teste.                                      *
     *-----------------------------------------------------------------------*
     */
    @KafkaClient
    interface AppointmentTestProducer {
        @Topic(Topics.APPOINTMENTCREATED)
        void enviar(@KafkaKey UUID chave, AppointmentCreatedEventDTO evento);
    }

    /*
     *-----------------------------------------------------------------------*
     * Listener de TESTE do tópico de ENTRADA ().                    *
     * groupId próprio: não 'rouba' mensagens do consumer da aplicação.      *
     * EARLIEST: não perde mensagem se conectar depois do envio              *
     * (o filtro por id nos testes ignora mensagens antigas).                *
     * CopyOnWriteArrayList: lista segura para uso entre threads.            *
     *-----------------------------------------------------------------------*
     */
    @KafkaListener(groupId = "appointment-test-group", offsetReset = OffsetReset.EARLIEST)
    static class AppointmentTestListener {

        private final List<AppointmentCreatedEventDTO> recebidas = new CopyOnWriteArrayList<>();

        @Topic(Topics.APPOINTMENTCREATED)
        void receber(AppointmentCreatedEventDTO evento) {
            recebidas.add(evento);
        }

        List<AppointmentCreatedEventDTO> getRecebidas() {
            return recebidas;
        }

        void limpar() {
            recebidas.clear();
        }
    }

    /*
     *-----------------------------------------------------------------------*
     * Listener de TESTE do tópico de SAÍDA (): guarda o        *
     * evento que o CONSUMER publica depois de processar.                    *
     * Remova se o consumer não publicar outro evento.                       *
     *-----------------------------------------------------------------------*
     */
    @KafkaListener(groupId = "appointment-saida-test-group", offsetReset = OffsetReset.EARLIEST)
    static class AppointmentSaidaTestListener {

        private final List<AppointmentProcessedEventDTO> recebidas = new CopyOnWriteArrayList<>();

        @Topic(Topics.APPOINTMENTPROCESSED)
        void receber(AppointmentProcessedEventDTO evento) {
            recebidas.add(evento);
        }

        List<AppointmentProcessedEventDTO> getRecebidas() {
            return recebidas;
        }

        void limpar() {
            recebidas.clear();
        }
    }
}