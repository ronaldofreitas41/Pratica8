package co.inter.piggies.service;

import co.inter.piggies.domain.specialty.Specialty;
import co.inter.piggies.domain.specialty.SpecialtyRequestDTO;
import co.inter.piggies.domain.specialty.SpecialtyResponseDTO;
import co.inter.piggies.repository.SpecialtyRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.zalando.problem.ThrowableProblem;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/*
 *-----------------------------------------------------------------------*
 * Teste de INTEGRAÇÃO do SpecialtyService usando o banco (e o Kafka) do  *
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
class SpecialtyServiceTest {

    // Service REAL, injetado pelo Micronaut
    @Inject
    SpecialtyService specialtyService;

    // Repository REAL: prepara dados (Given) e confere o que foi gravado (Then)
    @Inject
    SpecialtyRepository specialtyRepository;

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
        SpecialtyRequestDTO body = criarRequest();

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
    private SpecialtyRequestDTO criarRequest() {
        return new SpecialtyRequestDTO(
                "Spec "+ UUID.randomUUID(),
                "Description tra la la"
        );
    }

    // ===================== getSpecialtyById =====================

    @Test
    void deveRetornarSpecialtyQuandoIdExistir() {
        // Given: uma entidade gravada no banco
        Specialty salvo = salvarSpecialty();

        // When
        SpecialtyResponseDTO resp = specialtyService.getSpecialtyById(salvo.getId());

        // Then
        assertEquals(salvo.getId(), resp.id());
        // Confira aqui os demais campos do ResponseDTO...
    }

    @Test
    void deveRetornar404QuandoIdNaoExistirAoBuscar() {
        // Given: um id que com certeza NÃO existe no banco
        var id = UUID.randomUUID(); // ex.: UUID.randomUUID()

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> specialtyService.getSpecialtyById(id));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Specialty não existente para o id: " + id, ex.getDetail());
    }

    // ===================== getAllSpecialty =====================

    @Test
    void deveRetornarTodasAsSpecialtys() {
        // Given: quantos registros JÁ existem no banco do Docker
        long antes = specialtyRepository.count();
        salvarSpecialty();
        salvarSpecialty();

        // When
        List<SpecialtyResponseDTO> resp = specialtyService.getAllSpecialty();

        // Then: o banco pode ter dados prévios, então compara com o "antes"
        assertEquals(antes + 2, resp.size());
    }

    // ===================== createSpecialty =====================

    @Test
    void deveCriarSpecialty() {
        // When
        SpecialtyResponseDTO resp = specialtyService.createSpecialty(criarRequest());

        // Then: foi REALMENTE gravado no Postgres
        assertNotNull(resp.id());
        assertTrue(specialtyRepository.existsById(resp.id()));
    }

    @Test
    void deveRetornar409PorContadeSpecRepetida(){
        Specialty spec = salvarSpecialty();

        SpecialtyRequestDTO req = new SpecialtyRequestDTO(
                spec.getName(),
                "bbbb"
        );

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> specialtyService.createSpecialty(req));

        assertEquals(409, ex.getStatus().getStatusCode());
        assertEquals("Nome da Especialidade já está sendo utilizado!", ex.getDetail());

    }

    // ===================== updateSpecialty =====================

    @Test
    void deveAtualizarSpecialtyQuandoIdExistir() {
        // Given
        Specialty salvo = salvarSpecialty();

        // When
        specialtyService.updateSpecialty(salvo.getId(), criarRequest());

        // Then: busca de novo no banco e confere os dados alterados
        Specialty atualizado = specialtyRepository.findById(salvo.getId()).orElseThrow();
        assertNotNull(atualizado);
        // Confira aqui se os campos foram alterados com os dados do body...
    }

    @Test
    void deveRetornar409PorContadeSpecRepetidaAoFazerUpdate(){
        Specialty spec1 = salvarSpecialty();
        Specialty spec2 = salvarSpecialty();

        SpecialtyRequestDTO req = new SpecialtyRequestDTO(
                spec1.getName(),
                "bbbb"
        );

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> specialtyService.updateSpecialty(spec2.getId(),req));

        assertEquals(409, ex.getStatus().getStatusCode());
        assertEquals("Nome da Especialidade já está sendo utilizado!", ex.getDetail());

    }

    @Test
    void deveRetornar404QuandoIdNaoExistirAoAtualizar() {
        // Given: um id que com certeza NÃO existe no banco
        var id = UUID.randomUUID(); // ex.: UUID.randomUUID()

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> specialtyService.updateSpecialty(id, criarRequest()));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Specialty não existente para o id: " + id, ex.getDetail());
    }

    // ===================== deleteSpecialty =====================

    @Test
    void deveExcluirSpecialtyQuandoIdExistir() {
        // Given
        Specialty salvo = salvarSpecialty();

        // When
        specialtyService.deleteSpecialty(salvo.getId());

        // Then: não existe mais no banco
        assertFalse(specialtyRepository.existsById(salvo.getId()));
    }

    @Test
    void deveRetornar404QuandoIdNaoExistirAoExcluir() {
        // Given: um id que com certeza NÃO existe no banco
        var id = UUID.randomUUID(); // ex.: UUID.randomUUID()

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> specialtyService.deleteSpecialty(id));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Specialty não existente para o id: " + id, ex.getDetail());
    }
}