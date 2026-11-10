package co.inter.piggies.savings; // ⚠️ use o mesmo package do seu src/main/java

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.Topic;

/**
 * PRODUCER de teste: envia mensagens para o Kafka.
 *
 * Repare que é só uma INTERFACE, sem implementação.
 * O Micronaut gera a classe que implementa isso em tempo de COMPILAÇÃO
 * (é por isso que o Micronaut não usa reflection e sobe rápido).
 *
 * Para usar, basta injetar:  @Inject PingProducer producer;
 * e chamar:                  producer.send("olá");
 *
 * Fica em src/test porque só existe para validar o ambiente.
 * Na aplicação real (src/main) você terá producers como este, por exemplo
 * um PiggyEventProducer que publica em "piggies.transactions".
 */
@KafkaClient // marca a interface como um cliente (producer) Kafka
public interface PingProducer {

    // @Topic: nome do tópico onde a mensagem será publicada.
    // Se o tópico não existir, o broker cria automaticamente na primeira escrita.
    //
    // O parâmetro "message" vira o VALOR da mensagem no Kafka.
    // Aqui é uma String simples; em um caso real seria um record com @Serdeable,
    // que o Micronaut converte para JSON automaticamente.
    //
    // Retorno void = envio SÍNCRONO: o método só retorna depois que o Kafka confirmar o recebimento.
    //
    // (Em um caso real você também passaria uma chave, ex: send(@KafkaKey UUID piggyId, Evento e),
    //  para garantir a ordem das mensagens de uma mesma entidade.)
    @Topic("smoke.ping")
    void send(String message);
}