package co.inter.piggies.kafka.producer;

import co.inter.piggies.kafka.Topics;
import co.inter.piggies.kafka.events.AppointmentProcessedEventDTO;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;

import java.util.UUID;

/*
 *--------------------------------------------------------------------------*
 * @KafkaClient: marca a interface como um producer do Kafka. O Micronaut   *
 * gera a implementação em tempo de compilação, então basta declarar os     *
 * métodos. O id identifica o producer nas configurações (application.yml). *
 *--------------------------------------------------------------------------*
 * Para usar: injete o producer no service (@Inject) e chame o método       *
 * send(...) depois de salvar a entidade no banco.                          *
 *--------------------------------------------------------------------------*
 */
@KafkaClient(id = "appointment-processed-producer")
public interface AppointmentProcessedProducer {

    /*
     *--------------------------------------------------------------------*
     * Metodo: send                                                       *
     *--------------------------------------------------------------------*
     * @Topic: define para qual tópico a mensagem será enviada.           *
     * @KafkaKey: define a chave da mensagem. Mensagens com a mesma chave *
     * vão para a mesma partição, o que garante a ordem entre elas (ex.:  *
     * todos os eventos de um mesmo registro chegam em ordem).            *
     *--------------------------------------------------------------------*
     * Params:  id (chave da mensagem), event (corpo da mensagem)         *
     * Returns: None (o envio é feito de forma síncrona)                  *
     *--------------------------------------------------------------------*
     */
    @Topic(Topics.APPOINTMENTPROCESSED)
    void send(@KafkaKey UUID id, AppointmentProcessedEventDTO event);
}
