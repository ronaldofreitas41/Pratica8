package co.inter.piggies.controller;

import co.inter.piggies.domain.specialty.SpecialtyRequestDTO;
import co.inter.piggies.domain.specialty.SpecialtyResponseDTO;
import co.inter.piggies.service.SpecialtyService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.util.List;
import java.util.UUID;

/*
 *-------------------------------------------------------------------------*
 * @Controller marca a classe como um controller HTTP (bean do Micronaut). *
 * O valor passado (ex.: "/api/produtos") é o caminho base de todos os     *
 * endpoints da classe. O path de cada método (ex.: @Get("/{id}")) é       *
 * somado a ele, resultando em /api/produtos/{id}.                         *
 *-------------------------------------------------------------------------*
 * @ExecuteOn(TaskExecutors.BLOCKING) faz os métodos deste controller      *
 * rodarem no pool de threads para tarefas bloqueantes, e não no event     *
 * loop do Netty (padrão do Micronaut).                                    *
 *-------------------------------------------------------------------------*
 * Importância: o event loop usa poucas threads, que atendem todas as      *
 * requisições. Se uma delas ficar travada esperando banco (JDBC/JPA),     *
 * arquivo ou outra API, o servidor inteiro fica lento ou para.            *
 * Com o BLOCKING, essas chamadas rodam em outro pool e o event loop       *
 * continua livre para receber novas requisições.                          *
 *-------------------------------------------------------------------------*
 * Usar sempre que: O controller fizer chamadas bloqueantes (o caso comum  *
 * com repositories). Não é necessário em métodos reativos/assíncronos.    *
 *-------------------------------------------------------------------------*
 */

@Controller("/api/specialty")
@ExecuteOn(TaskExecutors.BLOCKING)
public class SpecialtyController {

    /*
     *-----------------------------------------------------------------------------*
     * @Inject: o Micronaut cria o service e o injeta neste campo automaticamente. *
     * O campo não pode ser private nem final (o Micronaut evita usar reflection). *
     * Alternativa: receber o service pelo construtor (permite usar final).        *
     *-----------------------------------------------------------------------------*
     */

    @Inject
    SpecialtyService specialtyService;

    /*
     *----------------------------------------------------------------------*
     * Metodo: GET                                                          *
     * Path: /api/specialty                                            *
     *----------------------------------------------------------------------*
     * Descrição: Função para buscar todos os Specialtys presentes no banco. *
     *----------------------------------------------------------------------*
     * Params:  None                                                        *
     * Returns: List<SpecialtyResponseDTO>                                   *
     *----------------------------------------------------------------------*
     */

    @Get
    public HttpResponse<List<SpecialtyResponseDTO>> getAllSpecialty() {
        return HttpResponse.ok(specialtyService.getAllSpecialty());
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: GET                                                                       *
     * Path: /api/specialty/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path. *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id                                                              *
     * Returns: SpecialtyResponseDTO                                                      *
     *-----------------------------------------------------------------------------------*
     */

    @Get("/{id}")
    public HttpResponse<SpecialtyResponseDTO> getSpecialtyById(@PathVariable UUID id) {
        return HttpResponse.ok(specialtyService.getSpecialtyById(id));
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: POST                                                       *
     * Path: /api/specialty                                          *
     *--------------------------------------------------------------------*
     * Descrição: Criar um registro novo referente a Entidade Specialty.   *
     *--------------------------------------------------------------------*
     * Params:  SpecialtyRequestDTO body                                   *
     * Returns: SpecialtyResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    @Post
    public HttpResponse<SpecialtyResponseDTO> createSpecialty(@Valid @Body SpecialtyRequestDTO body) {
        return HttpResponse.created(specialtyService.createSpecialty(body));
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: PUT                                                                       *
     * Path: /api/specialty/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path, *
     *            e editá-lo conforme o body passado.                                    *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id, SpecialtyRequestDTO body                                     *
     * Returns: SpecialtyResponseDTO                                                      *
     *-----------------------------------------------------------------------------------*
     */
    @Put("/{id}")
    public HttpResponse<SpecialtyResponseDTO> updateSpecialty(@PathVariable UUID id, @Valid @Body SpecialtyRequestDTO body) {
        return HttpResponse.ok(specialtyService.updateSpecialty(id, body));
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: DELETE                                                                    *
     * Path: /api/specialty/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path, *
     *            e excluir o mesmo                                                      *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id                                                              *
     * Returns: apenas o status                                                          *
     *-----------------------------------------------------------------------------------*
     */
    @Delete("/{id}")
    public HttpResponse<Void> deleteSpecialty(@PathVariable UUID id) {
        specialtyService.deleteSpecialty(id);
        return HttpResponse.noContent();
    }
}