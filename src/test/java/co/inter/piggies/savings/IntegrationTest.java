package co.inter.piggies.savings;

import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.kafka.KafkaContainer;              // Testcontainers 2.x: módulo testcontainers-kafka
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;    // Testcontainers 2.x: package novo (na 1.x era org.testcontainers.containers)

import java.util.Map;

/**
 * Classe BASE para todos os testes de integração.
 *
 * Ela NÃO é um teste (é abstrata e não tem @Test). Sua função é:
 *   1. Subir um PostgreSQL e um Kafka reais em containers Docker (Testcontainers);
 *   2. Dizer ao Micronaut onde esses containers estão (URL do banco, endereço do Kafka).
 *
 * Como usar: qualquer classe de teste que precise de banco/Kafka faz
 *
 *     @MicronautTest
 *     class MeuTeste extends IntegrationTest { ... }
 */
// PER_CLASS: o JUnit cria UMA instância da classe de teste para todos os métodos.
// O Micronaut exige isso para conseguir chamar getProperties() ANTES de subir o contexto da aplicação.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTest implements TestPropertyProvider {

    // ------------------------------------------------------------------
    // Containers
    // ------------------------------------------------------------------
    // "static final" = existe um único container para TODA a suíte de testes.
    // Assim, Postgres e Kafka sobem uma vez só e são reaproveitados por todas
    // as classes que estendem IntegrationTest ("singleton container pattern").
    // Isso deixa os testes MUITO mais rápidos do que subir containers por classe.

    // Mesma imagem pedida no PDF (docker pull postgres:16-alpine).
    // O Testcontainers cria banco, usuário e senha automaticamente.
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    // Mesma imagem pedida no PDF (docker pull apache/kafka-native:4.3.1).
    // A classe org.testcontainers.kafka.KafkaContainer suporta as imagens oficiais apache/kafka e apache/kafka-native.
    static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka-native:4.3.1");

    // Bloco estático: roda UMA vez, quando a classe é carregada pela JVM,
    // ou seja, antes de qualquer teste e antes do Micronaut subir.
    static {
        // deepStart sobe os dois containers EM PARALELO (mais rápido que um depois do outro).
        // join() faz esperar até os dois estarem prontos para receber conexões.
        Startables.deepStart(POSTGRES, KAFKA).join();
        // Não precisamos parar os containers no final: o Testcontainers
        // (container "Ryuk") remove tudo automaticamente quando a JVM termina.
    }

    // ------------------------------------------------------------------
    // Configuração do Micronaut
    // ------------------------------------------------------------------
    /**
     * O Micronaut chama este método ANTES de iniciar o ApplicationContext do teste.
     * Tudo que for retornado aqui SOBRESCREVE o application.yml / application.properties.
     *
     * É aqui que "ligamos" a aplicação aos containers: as portas dos containers
     * são aleatórias, então só sabemos os endereços em tempo de execução.
     */
    @Override
    public Map<String, String> getProperties() {
        return Map.of(
                // Banco: datasource "default", usado pelo JPA/Hibernate e pelos repositórios.
                "datasources.default.url", POSTGRES.getJdbcUrl(),          // ex: jdbc:postgresql://localhost:54321/test
                "datasources.default.username", POSTGRES.getUsername(),
                "datasources.default.password", POSTGRES.getPassword(),
                "datasources.default.driver-class-name", "org.postgresql.Driver",

                // Kafka: endereço do broker. Todo @KafkaClient (producer) e
                // @KafkaListener (consumer) da aplicação vai se conectar aqui.
                "kafka.bootstrap.servers", KAFKA.getBootstrapServers()     // ex: localhost:54322
        );
    }
}