package co.inter.piggies.service;

import co.inter.piggies.domain.doctor.Doctor;
import co.inter.piggies.domain.doctor.DoctorRequestDTO;
import co.inter.piggies.domain.doctor.DoctorResponseDTO;
import co.inter.piggies.domain.doctor.DoctorStatusRequestDTO;
import co.inter.piggies.domain.specialty.Specialty;
import co.inter.piggies.domain.specialty.SpecialtyRequestDTO;
import co.inter.piggies.domain.specialty.SpecialtyResponseDTO;
import co.inter.piggies.repository.DoctorRepository;
import co.inter.piggies.repository.SpecialtyRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.zalando.problem.ThrowableProblem;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/*
 *-----------------------------------------------------------------------*
 * Teste de INTEGRAÇÃO do DoctorService usando o banco (e o Kafka) do  *
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
class DoctorServiceTest {

    // Service REAL, injetado pelo Micronaut
    @Inject
    DoctorService doctorService;

    // Repository REAL: prepara dados (Given) e confere o que foi gravado (Then)
    @Inject
    DoctorRepository doctorRepository;

    @Inject
    SpecialtyRepository specialtyRepository;

    @Inject
    SpecialtyService specialtyService;

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: salvarDoctor                                    *
     *--------------------------------------------------------------------*
     * Descrição: Cria e SALVA uma Doctor no Postgres do Docker.        *
     * (Desfeito no rollback ao final do teste.)                          *
     *--------------------------------------------------------------------*
     */
    private Doctor salvarDoctor() {

        //Chama função que simula um body passado
        DoctorRequestDTO body = criarRequest();

        Doctor doctor = new Doctor();

        // Preencha aqui os campos obrigatórios da Doctor...
        //Ex: doctor.setType(body.type);
        doctor.setName(body.name());
        doctor.setCrm(body.crm());
        doctor.setSpecialty(criarSpecialty());

        return doctorRepository.save(doctor);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: criarRequest                                      *
     *--------------------------------------------------------------------*
     * Descrição: Monta um DoctorRequestDTO de exemplo (body).          *
     *--------------------------------------------------------------------*
     */
    private DoctorRequestDTO criarRequest() {


        return new DoctorRequestDTO(
                "Ronaldo " + UUID.randomUUID(),
                "CRM " + UUID.randomUUID(),
                criarSpecialty().getName()
        );
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: criarSpecialty                                    *
     *--------------------------------------------------------------------*
     * Descrição: Monta um SpecialtyRequestDTO de exemplo (body).         *
     *--------------------------------------------------------------------*
     */

    private Specialty criarSpecialty() {
        SpecialtyRequestDTO body = new SpecialtyRequestDTO(
                "Specialty " + UUID.randomUUID(),
                "Description " + UUID.randomUUID()
        );

        SpecialtyResponseDTO res = specialtyService.createSpecialty(body);

        return new Specialty(
                res.id(),
                res.name(),
                res.description(),
                new ArrayList<Doctor>()
        );
    }

    // ===================== getDoctorById =====================

    @Test
    void deveRetornarDoctorQuandoIdExistir() {
        // Given: uma entidade gravada no banco
        Doctor salvo = salvarDoctor();

        // When
        DoctorResponseDTO resp = doctorService.getDoctorById(salvo.getId());

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
                () -> doctorService.getDoctorById(id));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Doctor não existente para o id: " + id, ex.getDetail());
    }

    // ===================== getAllDoctor =====================

    @Test
    void deveRetornarTodasAsDoctors() {
        // Given: quantos registros JÁ existem no banco do Docker
        long antes = doctorRepository.count();
        salvarDoctor();
        salvarDoctor();

        // When
        List<DoctorResponseDTO> resp = doctorService.getAllDoctor();

        // Then: o banco pode ter dados prévios, então compara com o "antes"
        assertEquals(antes + 2, resp.size());
    }

    // ===================== createDoctor =====================

    @Test
    void deveCriarDoctor() {
        // When
        DoctorResponseDTO resp = doctorService.createDoctor(criarRequest());

        // Then: foi REALMENTE gravado no Postgres
        assertNotNull(resp.id());
        assertTrue(doctorRepository.existsById(resp.id()));
    }

    @Test
    void deveRetornar409PorCRMRepetido() {
        DoctorRequestDTO req = criarRequest();

        // When
        DoctorResponseDTO resp = doctorService.createDoctor(req);

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> doctorService.createDoctor(req));

        assertEquals(409, ex.getStatus().getStatusCode());
        assertEquals("CRM" + " já está sendo utilizado!", ex.getDetail());
    }

    @Test
    void deveRetornar404PorSpecialtyInexistente() {
        DoctorRequestDTO req = new DoctorRequestDTO(
                "Ronaldo",
                "CRM",
                "Spec"
        );

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> doctorService.createDoctor(req));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Especialidade: " + req.specialty() +" Não existe", ex.getDetail());
    }

    // ===================== updateDoctor =====================

    @Test
    void deveAtualizarDoctorQuandoIdExistir() {
        // Given
        Doctor salvo = salvarDoctor();

        // When
        doctorService.updateDoctor(salvo.getId(), criarRequest());

        // Then: busca de novo no banco e confere os dados alterados
        Doctor atualizado = doctorRepository.findById(salvo.getId()).orElseThrow();
        assertNotNull(atualizado);
        // Confira aqui se os campos foram alterados com os dados do body...
    }

    @Test
    void deveinativarMedico() {
        Doctor salvo = salvarDoctor();

        doctorService.updateDoctorStatus(salvo.getId(), new DoctorStatusRequestDTO(false));

        Doctor atualizado = doctorRepository.findById(salvo.getId()).orElseThrow();
        assertNotNull(atualizado);
    }

    @Test
    void deveRetornar409PorCRMRepetidoNoUpdate() {
        // Given
        Doctor salvo = salvarDoctor();
        Doctor salvo2 = salvarDoctor();

        DoctorRequestDTO req = new DoctorRequestDTO(
                salvo2.getName(),
                salvo.getCrm(),
                salvo2.getSpecialty().getName()
        );


        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> doctorService.updateDoctor(salvo2.getId(), req));

        assertEquals(409, ex.getStatus().getStatusCode());
        assertEquals("CRM" + " já está sendo utilizado!", ex.getDetail());
    }

    @Test
    void deveRetornar404PorSpecialtyInexistenteNoUpdate() {
        DoctorRequestDTO req = new DoctorRequestDTO(
                "Ronaldo",
                "CRM",
                "Spec"
        );

        Doctor salvo = salvarDoctor();

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> doctorService.updateDoctor(salvo.getId(),req));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Especialidade: " + req.specialty() +" Não existe", ex.getDetail());
    }

    @Test
    void deveRetornar404QuandoIdNaoExistirAoAtualizar() {
        // Given: um id que com certeza NÃO existe no banco
        var id = UUID.randomUUID(); // ex.: UUID.randomUUID()

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> doctorService.updateDoctor(id, criarRequest()));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Doctor não existente para o id: " + id, ex.getDetail());
    }

    @Test
    void deveRetornar404QuandoIdNaoExistirAomudarStatus() {
        // Given: um id que com certeza NÃO existe no banco
        var id = UUID.randomUUID(); // ex.: UUID.randomUUID()

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> doctorService.updateDoctorStatus(id, new DoctorStatusRequestDTO(false)));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Doctor não existente para o id: " + id, ex.getDetail());
    }

    // ===================== deleteDoctor =====================

    @Test
    void deveExcluirDoctorQuandoIdExistir() {
        // Given
        Doctor salvo = salvarDoctor();

        // When
        doctorService.deleteDoctor(salvo.getId());

        // Then: não existe mais no banco
        assertFalse(doctorRepository.existsById(salvo.getId()));
    }

    @Test
    void deveRetornar404QuandoIdNaoExistirAoExcluir() {
        // Given: um id que com certeza NÃO existe no banco
        var id = UUID.randomUUID(); // ex.: UUID.randomUUID()

        // When + Then
        ThrowableProblem ex = assertThrows(ThrowableProblem.class,
                () -> doctorService.deleteDoctor(id));

        assertEquals(404, ex.getStatus().getStatusCode());
        assertEquals("Doctor não existente para o id: " + id, ex.getDetail());
    }
}