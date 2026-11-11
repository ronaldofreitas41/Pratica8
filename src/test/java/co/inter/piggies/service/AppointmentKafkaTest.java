package co.inter.piggies.service;

import co.inter.piggies.domain.appointment.Appointment;
import co.inter.piggies.domain.appointment.AppointmentRequestDTO;
import co.inter.piggies.domain.appointment.AppointmentResponseDTO;
import co.inter.piggies.kafka.Topics;
import co.inter.piggies.kafka.events.AppointmentCreatedEventDTO;
import co.inter.piggies.kafka.events.AppointmentProcessedEventDTO;
import co.inter.piggies.repository.AppointmentRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

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

    @Inject
    public static AppointmentServiceTest a = new AppointmentServiceTest();

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
     * Metodo auxiliar: salvarAppointment                                    *
     *--------------------------------------------------------------------*
     * Descrição: Cria e SALVA uma Appointment no Postgres.                  *
     *            Sem rollback: o dado é REALMENTE gravado e              *
     *            o consumer consegue enxergá-lo.                         *
     *--------------------------------------------------------------------*
     */
    private Appointment salvarAppointment() {
        return a.salvarAppointment();
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
        AppointmentResponseDTO resp = appointmentService.createAppointment(criarRequest());

        // Then: espera o evento DESTA Appointment chegar no tópico
        aguardar(() -> {
            AppointmentCreatedEventDTO evento = listener.getRecebidas().stream()
                    .filter(e -> e.appointmentId().equals(resp.appointmentId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Evento ainda não chegou"));
            // Confira aqui os demais campos do evento...
        });
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: criarRequest                                      *
     *--------------------------------------------------------------------*
     * Descrição: Monta um AppointmentRequestDTO de exemplo (body).          *
     *--------------------------------------------------------------------*
     */
    private AppointmentRequestDTO criarRequest() {

        Appointment appointment = a.salvarAppointment();

        return new AppointmentRequestDTO(
                appointment.getPatient().getId(),
                appointment.getDoctor().getId(),
                appointment.getScheduledAt()
        );
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
        aguardarEstavel(() -> assertEquals("CREATED",
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus()));
    }

    @Test
    void naoDeveAlterarEmDobroREJECTEDQuandoAppointMentCreateChegarDuasVezes() {
        // Given: uma Appointment gravada e o valor ANTES do processamento
        Appointment appointment = salvarAppointment();
        String StautsAntes = appointment.getStatus();
        AppointmentCreatedEventDTO evento = criarEvento(appointment);

        // When: o MESMO evento é enviado 2 vezes
        producer.enviar(evento.doctorId(), evento);
        producer.enviar(evento.doctorId(), evento);

        // Then: mudou UMA vez só (ex.: estoque 10 -> 9, nunca 8)
        aguardarEstavel(() -> assertEquals("REJECTED",
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus()));
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