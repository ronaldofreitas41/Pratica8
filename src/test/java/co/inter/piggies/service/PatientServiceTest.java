package co.inter.piggies.service;

import co.inter.piggies.domain.patient.Patient;
import co.inter.piggies.domain.patient.PatientRequestDTO;
import co.inter.piggies.domain.patient.PatientResponseDTO;
import co.inter.piggies.repository.PatientRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/*
 *-----------------------------------------------------------------------*
 * Teste de INTEGRAÇÃO do PatientService usando o banco (e o Kafka) do  *
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
class PatientServiceTest {

    // Service REAL, injetado pelo Micronaut
    @Inject
    PatientService patientService;

    // Repository REAL: prepara dados (Given) e confere o que foi gravado (Then)
    @Inject
    PatientRepository patientRepository;

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
        PatientRequestDTO body = criarRequest();

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
    private PatientRequestDTO criarRequest() {
        return new PatientRequestDTO(
               "Ronaldo",
                geraEmailAleatorio(),
                "31971525231"
        );
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: e-mail                                            *
     *--------------------------------------------------------------------*
     * Descrição: Monta um e-mial aleatorio. por conta do Unique          *
     *--------------------------------------------------------------------*
     */
    private String geraEmailAleatorio() {
        return "teste-"+UUID.randomUUID()+"@gmail.com";
    }

    // ===================== getPatientById =====================

    @Test
    void deveRetornarPatientQuandoIdExistir() {
        // Given: uma entidade gravada no banco
        Patient salvo = salvarPatient();

        // When
        PatientResponseDTO resp = patientService.getPatientById(salvo.getId());

        // Then
        assertEquals(salvo.getId(), resp.id());
        // Confira aqui os demais campos do ResponseDTO...
    }

    @Test
    void deveRetornar404QuandoIdNaoExistirAoBuscar() {
        // When + Then: use um id que com certeza NÃO existe no seu banco
        //Uso do RandomUUID pois é muito dificil do UUID ser igual a um existente no banco
        HttpStatusException ex = assertThrows(HttpStatusException.class,
                () -> patientService.getPatientById(UUID.randomUUID()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ===================== getAllPatient =====================

    @Test
    void deveRetornarTodasAsPatients() {
        // Given: quantos registros JÁ existem no banco do Docker
        long antes = patientRepository.count();
        salvarPatient();
        salvarPatient();

        // When
        List<PatientResponseDTO> resp = patientService.getAllPatient();

        // Then: o banco pode ter dados prévios, então compara com o "antes"
        assertEquals(antes + 2, resp.size());
    }

    // ===================== createPatient =====================

    @Test
    void deveCriarPatient() {
        // When
        PatientResponseDTO resp = patientService.createPatient(criarRequest());

        // Then: foi REALMENTE gravado no Postgres
        assertNotNull(resp.id());
        assertTrue(patientRepository.existsById(resp.id()));
    }

    // ===================== updatePatient =====================

    @Test
    void deveAtualizarPatientQuandoIdExistir() {
        // Given
        Patient salvo = salvarPatient();

        // When
        patientService.updatePatient(salvo.getId(), criarRequest());

        // Then: busca de novo no banco e confere os dados alterados
        Patient atualizado = patientRepository.findById(salvo.getId()).orElseThrow();
        assertNotNull(atualizado);
        // Confira aqui se os campos foram alterados com os dados do body...
    }

    @Test
    void deveRetornar404QuandoIdNaoExistirAoAtualizar() {
        HttpStatusException ex = assertThrows(HttpStatusException.class,
                () -> patientService.updatePatient(UUID.randomUUID(), criarRequest()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ===================== deletePatient =====================

    @Test
    void deveExcluirPatientQuandoIdExistir() {
        // Given
        Patient salvo = salvarPatient();

        // When
        patientService.deletePatient(salvo.getId());

        // Then: não existe mais no banco
        assertFalse(patientRepository.existsById(salvo.getId()));
    }

    @Test
    void deveRetornar404QuandoIdNaoExistirAoExcluir() {
        //Possibilidade baixa de existir esse ID
        HttpStatusException ex = assertThrows(HttpStatusException.class,
                () -> patientService.deletePatient(UUID.randomUUID()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}