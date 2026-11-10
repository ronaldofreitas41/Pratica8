package co.inter.piggies.controller;

import co.inter.piggies.domain.patient.PatientRequestDTO;
import co.inter.piggies.domain.patient.PatientResponseDTO;
import co.inter.piggies.service.PatientService;
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

@Controller("/api/patient")
@ExecuteOn(TaskExecutors.BLOCKING)
public class PatientController {

    /*
     *-----------------------------------------------------------------------------*
     * @Inject: o Micronaut cria o service e o injeta neste campo automaticamente. *
     * O campo não pode ser private nem final (o Micronaut evita usar reflection). *
     * Alternativa: receber o service pelo construtor (permite usar final).        *
     *-----------------------------------------------------------------------------*
     */

    @Inject
    PatientService patientService;

    /*
     *----------------------------------------------------------------------*
     * Metodo: GET                                                          *
     * Path: /api/patient                                            *
     *----------------------------------------------------------------------*
     * Descrição: Função para buscar todos os Patients presentes no banco. *
     *----------------------------------------------------------------------*
     * Params:  None                                                        *
     * Returns: List<PatientResponseDTO>                                   *
     *----------------------------------------------------------------------*
     */

    @Get
    public HttpResponse<List<PatientResponseDTO>> getAllPatient() {
        return HttpResponse.ok(patientService.getAllPatient());
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: GET                                                                       *
     * Path: /api/patient/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path. *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id                                                              *
     * Returns: PatientResponseDTO                                                      *
     *-----------------------------------------------------------------------------------*
     */

    @Get("/{id}")
    public HttpResponse<PatientResponseDTO> getPatientById(@PathVariable UUID id) {
        return HttpResponse.ok(patientService.getPatientById(id));
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: POST                                                       *
     * Path: /api/patient                                          *
     *--------------------------------------------------------------------*
     * Descrição: Criar um registro novo referente a Entidade Patient.   *
     *--------------------------------------------------------------------*
     * Params:  PatientRequestDTO body                                   *
     * Returns: PatientResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    @Post
    public HttpResponse<PatientResponseDTO> createPatient(@Valid @Body PatientRequestDTO body) {
        return HttpResponse.created(patientService.createPatient(body));
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: PUT                                                                       *
     * Path: /api/patient/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path, *
     *            e editá-lo conforme o body passado.                                    *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id, PatientRequestDTO body                                     *
     * Returns: PatientResponseDTO                                                      *
     *-----------------------------------------------------------------------------------*
     */
    @Put("/{id}")
    public HttpResponse<PatientResponseDTO> updatePatient(@PathVariable UUID id, @Valid @Body PatientRequestDTO body) {
        return HttpResponse.ok(patientService.updatePatient(id, body));
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: DELETE                                                                    *
     * Path: /api/patient/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path, *
     *            e excluir o mesmo                                                      *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id                                                              *
     * Returns: apenas o status                                                          *
     *-----------------------------------------------------------------------------------*
     */
    @Delete("/{id}")
    public HttpResponse<Void> deletePatient(@PathVariable UUID id) {
        patientService.deletePatient(id);
        return HttpResponse.noContent();
    }
}
