package co.inter.piggies.savings; 

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * "Smoke test" de infraestrutura: só prova que o AMBIENTE funciona.
 * Não testa regra de negócio nenhuma.
 *
 * Se estes dois testes passam, sabemos que:
 *   - os containers sobem;
 *   - a aplicação conecta no Postgres do container;
 *   - a aplicação consegue publicar E consumir mensagens no Kafka do container.
 *
 * É o ponto de partida antes de implementar qualquer funcionalidade.
 */
// @MicronautTest: sobe a aplicação Micronaut (ApplicationContext) para o teste
// e permite usar @Inject nos campos.
// Por padrão ele também abre uma TRANSAÇÃO por teste e faz rollback no final,
// por isso o EntityManager funciona no primeiro teste sem nenhuma configuração extra.
//
// "extends IntegrationTest": herda os containers e o getProperties(),
// então a aplicação já sobe apontando para o Postgres e o Kafka dos containers.
@MicronautTest
class InfraSmokeTest extends IntegrationTest {

    // Beans injetados pelo Micronaut:
    @Inject EntityManager entityManager; // JPA/Hibernate, conectado ao Postgres do container
    @Inject PingProducer producer;       // implementação gerada pelo Micronaut a partir da interface
    @Inject PingListener listener;       // o mesmo bean que está escutando o tópico

    @Test
    void postgresIsUp() {
        // Executa um SQL puro no banco. Se não houvesse conexão, aqui lançaria exceção.
        Object result = entityManager.createNativeQuery("select 1").getSingleResult();

        // AssertJ: comparamos como Number porque o driver pode devolver Integer ou Long.
        assertThat(((Number) result).intValue()).isEqualTo(1);
    }

    @Test
    void kafkaRoundTrip() {
        // Mensagem ÚNICA: os containers são compartilhados entre os testes,
        // então podem existir mensagens antigas no tópico. Com um UUID,
        // garantimos que estamos verificando exatamente a NOSSA mensagem.
        String msg = UUID.randomUUID().toString();

        // 1) Publica no tópico "smoke.ping"
        producer.send(msg);

        // 2) O consumo é ASSÍNCRONO: o listener recebe "um pouco depois", em outra thread.
        //    Por isso NÃO dá para fazer o assert logo em seguida, e NÃO se usa Thread.sleep.
        //
        //    Awaitility: repete o assert até ele passar, por no máximo 15 segundos.
        //    - Passou antes disso? O teste continua na hora (não espera à toa).
        //    - Estourou 15s? O teste falha mostrando o último erro do assert.
        await().atMost(Duration.ofSeconds(15))
               .untilAsserted(() -> assertThat(listener.received).contains(msg));
    }
}