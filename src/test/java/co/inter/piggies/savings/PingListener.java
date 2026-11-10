package co.inter.piggies.savings; // ⚠️ use o mesmo package do seu src/main/java

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * CONSUMER de teste: escuta um tópico do Kafka e guarda o que recebeu.
 *
 * Diferente do producer, aqui é uma CLASSE (precisa ter o código que processa a mensagem).
 * O Micronaut cria este bean e começa a escutar o tópico automaticamente quando a aplicação sobe.
 *
 * No teste, usamos a lista "received" para verificar se a mensagem enviada chegou.
 * Esse padrão ("collector") também serve para testar os eventos da aplicação real:
 * você cria um listener de teste que guarda os eventos e faz asserts sobre eles.
 */
@KafkaListener(
        // groupId: identifica o "grupo de consumo".
        // Cada grupo recebe TODAS as mensagens do tópico; dentro do mesmo grupo,
        // as mensagens são divididas entre as instâncias.
        // ⚠️ Nunca use o mesmo groupId de um consumer real da aplicação,
        //    senão os dois vão "dividir" as mensagens e um deles não recebe.
        groupId = "smoke-test",

        // EARLIEST: na primeira conexão do grupo, ler o tópico DESDE O INÍCIO.
        // ⚠️ Essencial em teste: a mensagem pode ser enviada antes de o listener
        //    terminar de se conectar. Com LATEST (padrão), ela seria perdida
        //    e o teste falharia "às vezes".
        offsetReset = OffsetReset.EARLIEST
)
public class PingListener {

    // Fila thread-safe: o listener roda em uma thread do Kafka e o teste lê
    // em outra thread, então uma lista comum (ArrayList) poderia dar problema.
    public final Queue<String> received = new ConcurrentLinkedQueue<>();

    // @Topic: de qual tópico ler. É chamado UMA vez para cada mensagem recebida.
    // O parâmetro é o VALOR da mensagem, já convertido (aqui, String).
    // Se este método lançar exceção, o Micronaut trata como falha de processamento.
    // Se terminar normalmente, o offset é confirmado (commit) e a mensagem conta como processada.
    @Topic("smoke.ping")
    void receive(String message) {
        received.add(message);
    }
}