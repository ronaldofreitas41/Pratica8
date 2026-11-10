package co.inter.piggies.savings;

import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste gerado automaticamente pelo "mn create-app" (o nome do arquivo pode variar).
 *
 * Única alteração necessária: "extends IntegrationTest".
 *
 * Por quê? Sem isso, a aplicação sobe SEM a URL do banco (não usamos Micronaut
 * Test Resources), o Hibernate não consegue conectar e o teste quebra.
 * Herdando de IntegrationTest, ele usa os mesmos containers dos outros testes.
 */
@MicronautTest
class SavingsTest extends IntegrationTest {

    // EmbeddedApplication representa a aplicação Micronaut em execução (servidor HTTP Netty incluso).
    @Inject
    EmbeddedApplication<?> application;

    @Test
    void testItWorks() {
        // Verifica que a aplicação subiu por completo.
        assertThat(application.isRunning()).isTrue();
    }
}