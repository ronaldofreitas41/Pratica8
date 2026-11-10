package co.inter.piggies.controller;

import co.inter.piggies.domain.doctor.DoctorRequestDTO;
import co.inter.piggies.domain.doctor.DoctorResponseDTO;
import co.inter.piggies.domain.doctor.DoctorStatusRequestDTO;
import co.inter.piggies.service.DoctorService;
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

@Controller("/api/doctor")
@ExecuteOn(TaskExecutors.BLOCKING)
public class DoctorController {

    /*
     *-----------------------------------------------------------------------------*
     * @Inject: o Micronaut cria o service e o injeta neste campo automaticamente. *
     * O campo não pode ser private nem final (o Micronaut evita usar reflection). *
     * Alternativa: receber o service pelo construtor (permite usar final).        *
     *-----------------------------------------------------------------------------*
     */

    @Inject
    DoctorService doctorService;

    /*
     *----------------------------------------------------------------------*
     * Metodo: GET                                                          *
     * Path: /api/doctor                                            *
     *----------------------------------------------------------------------*
     * Descrição: Função para buscar todos os Doctors presentes no banco. *
     *----------------------------------------------------------------------*
     * Params:  None                                                        *
     * Returns: List<DoctorResponseDTO>                                   *
     *----------------------------------------------------------------------*
     */

    @Get
    public HttpResponse<List<DoctorResponseDTO>> getAllDoctor() {
        return HttpResponse.ok(doctorService.getAllDoctor());
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: GET                                                                       *
     * Path: /api/doctor/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path. *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id                                                              *
     * Returns: DoctorResponseDTO                                                      *
     *-----------------------------------------------------------------------------------*
     */

    @Get("/{id}")
    public HttpResponse<DoctorResponseDTO> getDoctorById(@PathVariable UUID id) {
        return HttpResponse.ok(doctorService.getDoctorById(id));
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: POST                                                       *
     * Path: /api/doctor                                          *
     *--------------------------------------------------------------------*
     * Descrição: Criar um registro novo referente a Entidade Doctor.   *
     *--------------------------------------------------------------------*
     * Params:  DoctorRequestDTO body                                   *
     * Returns: DoctorResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    @Post
    public HttpResponse<DoctorResponseDTO> createDoctor(@Valid @Body DoctorRequestDTO body) {
        return HttpResponse.created(doctorService.createDoctor(body));
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: PUT                                                                       *
     * Path: /api/doctor/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path, *
     *            e editá-lo conforme o body passado.                                    *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id, DoctorRequestDTO body                                     *
     * Returns: DoctorResponseDTO                                                      *
     *-----------------------------------------------------------------------------------*
     */
    @Put("/{id}")
    public HttpResponse<DoctorResponseDTO> updateDoctor(@PathVariable UUID id, @Valid @Body DoctorRequestDTO body) {
        return HttpResponse.ok(doctorService.updateDoctor(id, body));
    }

    @Patch("/{id}/status")
    public HttpResponse<DoctorResponseDTO> updateDoctorStatus(@PathVariable UUID id, @Valid @Body DoctorStatusRequestDTO body) {
        return HttpResponse.ok(doctorService.updateDoctorStatus(id, body));
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: DELETE                                                                    *
     * Path: /api/doctor/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path, *
     *            e excluir o mesmo                                                      *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id                                                              *
     * Returns: apenas o status                                                          *
     *-----------------------------------------------------------------------------------*
     */
    @Delete("/{id}")
    public HttpResponse<Void> deleteDoctor(@PathVariable UUID id) {
        doctorService.deleteDoctor(id);
        return HttpResponse.noContent();
    }
}
