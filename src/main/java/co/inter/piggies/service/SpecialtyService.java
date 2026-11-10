package co.inter.piggies.service;

import co.inter.piggies.domain.specialty.Specialty;
import co.inter.piggies.domain.specialty.SpecialtyRequestDTO;
import co.inter.piggies.domain.specialty.SpecialtyResponseDTO;
import co.inter.piggies.repository.SpecialtyRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.problem.HttpStatusType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;

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
public class SpecialtyService {

    /*
     *-------------------------------------------------------------------------*
     * @Inject: o Micronaut cria o repository e o injeta neste campo.          *
     * O campo não pode ser private nem final (o Micronaut evita reflection).  *
     * Alternativa: receber o repository pelo construtor (permite usar final). *
     *-------------------------------------------------------------------------*
     */
    @Inject
    SpecialtyRepository specialtyRepository;

    /*
     *--------------------------------------------------------------------*
     * Metodo: getSpecialtyById                                            *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Specialty pelo seu id se ele      *
     *            existir.                                                *
     *--------------------------------------------------------------------*
     * Params:  UUID id                                               *
     * Returns: SpecialtyResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public SpecialtyResponseDTO getSpecialtyById(UUID id) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // lança um Problem 404 com a mensagem de não existente para aquele id
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> notFound(id));

        //Retorna a conversão da Entity em um ResponseDTO
        return SpecialtyResponseDTO.from(specialty);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: getAllSpecialty                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma lista com todas as Specialtys     *
     *            presentes no banco de dados.                            *
     *--------------------------------------------------------------------*
     * Params:  None                                                      *
     * Returns: List<SpecialtyResponseDTO>                                 *
     *--------------------------------------------------------------------*
     */

    public List<SpecialtyResponseDTO> getAllSpecialty() {

        //Busca todas as Entities presentes no banco
        List<Specialty> specialtys = specialtyRepository.findAll();

        //Retorna a conversão da Lista de Entities em uma Lista de ResponseDTO
        return specialtys.stream().map(SpecialtyResponseDTO::from).toList();
    }


    /*
     *--------------------------------------------------------------------*
     * Metodo: createSpecialty                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para criar uma Specialty com os dados passados    *
     *            via body da requisição                                  *
     *--------------------------------------------------------------------*
     * Params:  SpecialtyRequestDTO body                                   *
     * Returns: SpecialtyResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public SpecialtyResponseDTO createSpecialty(SpecialtyRequestDTO body) {

        if(specialtyRepository.existsByName(body.name())){
            throw conflito("Nome da Especialidade");
        }

        //Converte o Body que é um RequestDTO em uma Entity
        Specialty specialty = new Specialty();

        specialty.setName(body.name());
        specialty.setDescription(body.description());

        // Salvando Entity no banco e recebendo os dados que foram salvos
        Specialty specialtySaved = specialtyRepository.save(specialty);

        //Retorna a conversão da Entidade que foi salva em um ResponseDTO
        return SpecialtyResponseDTO.from(specialtySaved);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: updateSpecialty                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Specialty pelo seu id se ele      *
     *            existir. E se ela existir editar com os dados passados  *
     *            via body da requisição                                  *
     *--------------------------------------------------------------------*
     * Params:  UUID id, SpecialtyRequestDTO body                      *
     * Returns: SpecialtyResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public SpecialtyResponseDTO updateSpecialty(UUID id, SpecialtyRequestDTO body) {

        if(specialtyRepository.existsByNameAndIdNot(body.name(),id)){
            throw conflito("Nome da Especialidade");
        }

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // lança um Problem 404 com a mensagem de não existente para aquele id
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> notFound(id));

        //Setando a entidade com os novos dados...
        specialty.setName(body.name());
        specialty.setDescription(body.description());

        // Atualizando Entity referente ao id passado no banco
        Specialty specialtyUpdated = specialtyRepository.update(specialty);

        //Retorna a conversão da RequestDTO em um ResponseDTO
        return SpecialtyResponseDTO.from(specialtyUpdated);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: deleteSpecialty                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Specialty pelo seu id se ele      *
     *            existir. E se ela existir ela será excluída             *
     *--------------------------------------------------------------------*
     * Params:  UUID id                                               *
     * Returns: None                                                      *
     *--------------------------------------------------------------------*
     */

    public void deleteSpecialty(UUID id) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // lança um Problem 404 com a mensagem de não existente para aquele id
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> notFound(id));

        // Excluindo Entity referente ao id passado no banco se ela existir
        specialtyRepository.delete(specialty);
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: notFound                                          *
     *--------------------------------------------------------------------*
     * Descrição: Monta o Problem 404 lançado quando a Specialty não       *
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
                .withDetail("Specialty não existente para o id: " + id)
                .build();
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo auxiliar: conlfito                                          *
     *--------------------------------------------------------------------*
     * Descrição: Monta o Problem 409 lançado quando a Specialty tem uma   *
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
