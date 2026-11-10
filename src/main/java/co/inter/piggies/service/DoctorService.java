package co.inter.piggies.service;

import co.inter.piggies.domain.doctor.Doctor;
import co.inter.piggies.domain.doctor.DoctorRequestDTO;
import co.inter.piggies.domain.doctor.DoctorResponseDTO;
import co.inter.piggies.domain.doctor.DoctorStatusRequestDTO;
import co.inter.piggies.domain.specialty.Specialty;
import co.inter.piggies.repository.DoctorRepository;
import co.inter.piggies.repository.SpecialtyRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.problem.HttpStatusType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 *-----------------------------------------------------------------------*
 * @Singleton registra a classe como um bean do Micronaut com uma única  *
 * instância compartilhada por toda a aplicação. É o que permite injetar *
 * este service no controller (via @Inject ou construtor). Por ser       *
 * compartilhado, não guarde estado de requisição em campos da classe.   *
 *-----------------------------------------------------------------------*
 */
@Singleton
public class DoctorService {

    /*
     *-------------------------------------------------------------------------*
     * @Inject: o Micronaut cria o repository e o injeta neste campo.          *
     * O campo não pode ser private nem final (o Micronaut evita reflection).  *
     * Alternativa: receber o repository pelo construtor (permite usar final). *
     *-------------------------------------------------------------------------*
     */
    @Inject
    DoctorRepository doctorRepository;

    @Inject
    SpecialtyRepository specialtyRepository;

    /*
     *--------------------------------------------------------------------*
     * Metodo: getDoctorById                                            *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Doctor pelo seu id se ele      *
     *            existir.                                                *
     *--------------------------------------------------------------------*
     * Params:  UUID id                                               *
     * Returns: DoctorResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public DoctorResponseDTO getDoctorById(UUID id) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // lança um Problem 404 com a mensagem de não existente para aquele id
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> notFound(id));

        //Retorna a conversão da Entity em um ResponseDTO
        return DoctorResponseDTO.from(doctor);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: getAllDoctor                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma lista com todas as Doctors     *
     *            presentes no banco de dados.                            *
     *--------------------------------------------------------------------*
     * Params:  None                                                      *
     * Returns: List<DoctorResponseDTO>                                 *
     *--------------------------------------------------------------------*
     */

    public List<DoctorResponseDTO> getAllDoctor() {

        //Busca todas as Entities presentes no banco
        List<Doctor> doctors = doctorRepository.findAll();

        //Retorna a conversão da Lista de Entities em uma Lista de ResponseDTO
        return doctors.stream().map(DoctorResponseDTO::from).toList();
    }


    /*
     *--------------------------------------------------------------------*
     * Metodo: createDoctor                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para criar uma Doctor com os dados passados    *
     *            via body da requisição                                  *
     *--------------------------------------------------------------------*
     * Params:  DoctorRequestDTO body                                   *
     * Returns: DoctorResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public DoctorResponseDTO createDoctor(DoctorRequestDTO body) {

        if (doctorRepository.existsByCrm(body.crm())) {
            throw conflito("CRM");
        }

        Specialty specialty = specialtyRepository.findByName(body.specialty()).orElseThrow(() ->
                Problem.builder()
                        .withStatus(new HttpStatusType(HttpStatus.NOT_FOUND))
                        .withTitle("Não encontrado")
                        .withDetail("Especialidade: " + body.name() + " Não existe")
                        .build()
        );

        //Converte o Body que é um RequestDTO em uma Entity
        Doctor doctor = new Doctor();


        //Setando Entity...
        doctor.setName(body.name());
        doctor.setCrm(body.crm());
        doctor.setActive(true);
        doctor.setSpecialty(specialty);
        doctor.setCreatedAt(OffsetDateTime.now());

        // Salvando Entity no banco e recebendo os dados que foram salvos
        Doctor doctorSaved = doctorRepository.save(doctor);

        //Retorna a conversão da Entidade que foi salva em um ResponseDTO
        return DoctorResponseDTO.from(doctorSaved);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: updateDoctor                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Doctor pelo seu id se ele      *
     *            existir. E se ela existir editar com os dados passados  *
     *            via body da requisição                                  *
     *--------------------------------------------------------------------*
     * Params:  UUID id, DoctorRequestDTO body                      *
     * Returns: DoctorResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public DoctorResponseDTO updateDoctor(UUID id, DoctorRequestDTO body) {

        if (doctorRepository.existsByCrmAndIdNot(body.crm(),id)) {
            throw conflito("CRM");
        }

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // lança um Problem 404 com a mensagem de não existente para aquele id
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> notFound(id));

        Specialty specialty = specialtyRepository.findByName(body.specialty()).orElseThrow(() ->
                Problem.builder()
                        .withStatus(new HttpStatusType(HttpStatus.NOT_FOUND))
                        .withTitle("Não encontrado")
                        .withDetail("Especialidade: " + body.name() + " Não existe")
                        .build()
        );

        //Setando Entity...
        doctor.setName(body.name());
        doctor.setCrm(body.crm());
        doctor.setActive(true);
        doctor.setSpecialty(specialty);

        // Atualizando Entity referente ao id passado no banco
        Doctor doctorUpdated = doctorRepository.update(doctor);

        //Retorna a conversão da RequestDTO em um ResponseDTO
        return DoctorResponseDTO.from(doctorUpdated);
    }

    public DoctorResponseDTO updateDoctorStatus(UUID id, DoctorStatusRequestDTO body){

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // lança um Problem 404 com a mensagem de não existente para aquele id
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> notFound(id));

        doctor.setActive(body.avtive());

        Doctor doctorUpdated = doctorRepository.update(doctor);

        return DoctorResponseDTO.from(doctorUpdated);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: deleteDoctor                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Doctor pelo seu id se ele      *
     *            existir. E se ela existir ela será excluída             *
     *--------------------------------------------------------------------*
     * Params:  UUID id                                               *
     * Returns: None                                                      *
     *--------------------------------------------------------------------*
     */

    public void deleteDoctor(UUID id) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // lança um Problem 404 com a mensagem de não existente para aquele id
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> notFound(id));

        // Excluindo Entity referente ao id passado no banco se ela existir
        doctorRepository.delete(doctor);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: notFound                                          *
     *--------------------------------------------------------------------*
     * Descrição: Monta o Problem 404 lançado quando a Doctor não       *
     *            existe. Centraliza a mensagem para os 3 métodos.        *
     *            O detail aparece no body da resposta (Postman).         *
     *--------------------------------------------------------------------*
     * Params:  UUID id                                               *
     * Returns: ThrowableProblem                                          *
     *--------------------------------------------------------------------*
     */

    private ThrowableProblem notFound(UUID id) {
        return Problem.builder()
                .withStatus(new HttpStatusType(HttpStatus.NOT_FOUND))
                .withTitle("Não encontrado")
                .withDetail("Doctor não existente para o id: " + id)
                .build();
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: conlfito                                          *
     *--------------------------------------------------------------------*
     * Descrição: Monta o Problem 409 lançado quando a Doctor tem uma   *
     *            de suas propriedades definidas como unique = true,      *
     *            centralizando a mensagem exibida e podendo ser usada    *
     *            em mais de um caso.                                     *
     *--------------------------------------------------------------------*
     * Params:  String nomeProp                                           *
     * Returns: ThrowableProblem                                          *
     *--------------------------------------------------------------------*
     */

    private ThrowableProblem conflito(String nomeProp) {
        return Problem.builder()
                .withStatus(new HttpStatusType(HttpStatus.CONFLICT))
                .withTitle("Conflito")
                .withDetail(nomeProp + " já está sendo utilizado!")
                .build();
    }
}
