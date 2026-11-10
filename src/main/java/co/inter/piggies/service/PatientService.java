package co.inter.piggies.service;

import co.inter.piggies.domain.patient.Patient;
import co.inter.piggies.domain.patient.PatientRequestDTO;
import co.inter.piggies.domain.patient.PatientResponseDTO;
import co.inter.piggies.repository.PatientRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
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
public class PatientService {

    /*
     *-------------------------------------------------------------------------*
     * @Inject: o Micronaut cria o repository e o injeta neste campo.          *
     * O campo não pode ser private nem final (o Micronaut evita reflection).  *
     * Alternativa: receber o repository pelo construtor (permite usar final). *
     *-------------------------------------------------------------------------*
     */
    @Inject
    PatientRepository patientRepository;

    /*
     *--------------------------------------------------------------------*
     * Metodo: getPatientById                                            *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Patient pelo seu id se ele      *
     *            existir.                                                *
     *--------------------------------------------------------------------*
     * Params:  UUID id                                               *
     * Returns: PatientResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public PatientResponseDTO getPatientById(UUID id) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // retornará um Status 404 com a mensagem de dados nao encontrado para aquele id
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                        "Dado não encontrado para o id: " + id));

        //Retorna a conversão da Entity em um ResponseDTO
        return PatientResponseDTO.from(patient);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: getAllPatient                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma lista com todas as Patients     *
     *            presentes no banco de dados.                            *
     *--------------------------------------------------------------------*
     * Params:  None                                                      *
     * Returns: List<PatientResponseDTO>                                 *
     *--------------------------------------------------------------------*
     */

    public List<PatientResponseDTO> getAllPatient() {

        //Busca todas as Entities presentes no banco
        List<Patient> patients = patientRepository.findAll();

        //Retorna a conversão da Lista de Entities em uma Lista de ResponseDTO
        return patients.stream().map(PatientResponseDTO::from).toList();
    }


    /*
     *--------------------------------------------------------------------*
     * Metodo: createPatient                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para criar uma Patient com os dados passados    *
     *            via body da requisição                                  *
     *--------------------------------------------------------------------*
     * Params:  PatientRequestDTO body                                   *
     * Returns: PatientResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public PatientResponseDTO createPatient(PatientRequestDTO body) {

        if (patientRepository.existsByEmail(body.email())) {
           throw conflito("email");
        }

        //Converte o Body que é um RequestDTO em uma Entity
        Patient patient = new Patient();

        patient.setCreatedAt(OffsetDateTime.now());
        patient.setName(body.name());
        patient.setEmail(body.email());
        patient.setPhone(body.phone());


        // Salvando Entity no banco e recebendo os dados que foram salvos
        Patient patientSaved = patientRepository.save(patient);

        //Retorna a conversão da Entidade que foi salva em um ResponseDTO
        return PatientResponseDTO.from(patientSaved);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: updatePatient                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Patient pelo seu id se ele      *
     *            existir. E se ela existir editar com os dados passados  *
     *            via body da requisição                                  *
     *--------------------------------------------------------------------*
     * Params:  UUID id, PatientRequestDTO body                      *
     * Returns: PatientResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO body) {

        if (patientRepository.existsByEmailAndIdNot(body.email(),id)){
            throw conflito("E-mail");
        }

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // retornará um Status 404 com a mensagem de dados nao encontrado para aquele id
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                        "Dado não encontrado para o id: " + id));

        patient.setName(body.name());
        patient.setEmail(body.email());
        patient.setPhone(body.phone());

        // Atualizando Entity referente ao id passado no banco
        Patient patientUpdated = patientRepository.update(patient);

        //Retorna a conversão da RequestDTO em um ResponseDTO
        return PatientResponseDTO.from(patientUpdated);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: deletePatient                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Patient pelo seu id se ele      *
     *            existir. E se ela existir ela será excluída             *
     *--------------------------------------------------------------------*
     * Params:  UUID id                                               *
     * Returns: None                                                      *
     *--------------------------------------------------------------------*
     */

    public void deletePatient(UUID id) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // retornará um Status 404 com a mensagem de dados nao encontrado para aquele id
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                        "Dado não encontrado para o id: " + id));

        // Excluindo Entity referente ao id passado no banco se ela existir
        patientRepository.delete(patient);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: conlfito                                          *
     *--------------------------------------------------------------------*
     * Descrição: Monta o Problem 409 lançado quando a $Entity$ tem uma   *
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