package co.inter.piggies.kafka.consumer;

import co.inter.piggies.kafka.Topics;
import co.inter.piggies.kafka.events.AppointmentCreatedEventDTO;
import co.inter.piggies.service.AppointmentService;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/*
 *-----------------------------------------------------------------------*
 * @KafkaListener: marca a classe como um consumer do Kafka. O Micronaut *
 * cria a conexão e chama os métodos anotados com @Topic quando chega    *
 * uma mensagem.                                                         *
 *-----------------------------------------------------------------------*
 * groupId: consumers com o mesmo groupId dividem as mensagens entre si  *
 * (cada mensagem é processada por apenas um deles). Grupos diferentes   *
 * recebem todas as mensagens.                                           *
 *-----------------------------------------------------------------------*
 * offsetReset = EARLIEST: na primeira vez que o grupo conecta, lê o     *
 * tópico desde o início. Com LATEST, leria só as mensagens novas.       *
 *-----------------------------------------------------------------------*
 */
@KafkaListener(groupId = "appointment-processing", offsetReset = OffsetReset.EARLIEST)
public class AppointmentProcessingConsumer {

    @Inject
    private static Logger LOG = LoggerFactory.getLogger(AppointmentProcessingConsumer.class);

    @Inject
    private AppointmentService appointmentService;

    /*
     *---------------------------------------------------------------------*
     * Metodo: receive                                                     *
     *---------------------------------------------------------------------*
     * @Topic: define de qual tópico este método consome as mensagens.     *
     * @KafkaKey: recebe a chave da mensagem enviada pelo producer.        *
     * O Micronaut converte o JSON da mensagem no DTO automaticamente      *
     * (o DTO precisa de @Serdeable).                                      *
     *---------------------------------------------------------------------*
     * Atenção: se o método lançar exceção, por padrão o erro é logado e a *
     * mensagem é pulada. Para retentar, configure o errorStrategy no      *
     * @KafkaListener.                                                     *
     *---------------------------------------------------------------------*
     * Params:  id (chave da mensagem), event (corpo da mensagem)          *
     * Returns: None                                                       *
     *---------------------------------------------------------------------*
     */
    @Topic(Topics.APPOINTMENTCREATED)
    public void receive(@KafkaKey UUID id, AppointmentCreatedEventDTO event) {

        // Log para acompanhar as mensagens recebidas
        LOG.info("Mensagem recebida do tópico {} | id: {}", Topics.APPOINTMENTCREATED, id);

        // Coloque aqui o processamento da mensagem
        appointmentService.processingAppointment(event);
    }
}
