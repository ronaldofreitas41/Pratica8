package co.inter.piggies.controller;

import co.inter.piggies.domain.appointment.AppointmentCancelRequestDTO;
import co.inter.piggies.domain.appointment.AppointmentCancelResponseDTO;
import co.inter.piggies.domain.appointment.AppointmentRequestDTO;
import co.inter.piggies.domain.appointment.AppointmentResponseDTO;
import co.inter.piggies.service.AppointmentService;
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

@Controller("/api/appointment")
@ExecuteOn(TaskExecutors.BLOCKING)
public class AppointmentController {

    /*
     *-----------------------------------------------------------------------------*
     * @Inject: o Micronaut cria o service e o injeta neste campo automaticamente. *
     * O campo não pode ser private nem final (o Micronaut evita usar reflection). *
     * Alternativa: receber o service pelo construtor (permite usar final).        *
     *-----------------------------------------------------------------------------*
     */

    @Inject
    AppointmentService appointmentService;


    /*
     *--------------------------------------------------------------------*
     * Metodo: POST                                                       *
     * Path: /api/appointment                                          *
     *--------------------------------------------------------------------*
     * Descrição: Criar um registro novo referente a Entidade Appointment.   *
     *--------------------------------------------------------------------*
     * Params:  AppointmentRequestDTO body                                   *
     * Returns: AppointmentResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    @Post
    public HttpResponse<AppointmentResponseDTO> createAppointment(@Valid @Body AppointmentRequestDTO body) {
        return HttpResponse.created(appointmentService.createAppointment(body));
    }

    @Patch("/{id}/cancel")
    public HttpResponse<AppointmentCancelResponseDTO> cancelAppointment
            (@PathVariable UUID id, @Valid @Body AppointmentCancelRequestDTO body) {
        return HttpResponse.ok(appointmentService.cancelAppointment(id, body));
    }

    @Get("/{id}")
    public HttpResponse<AppointmentResponseDTO> getAppointmentById(@PathVariable UUID id) {
        return HttpResponse.ok(appointmentService.getAppointmentById(id));
    }

    @Get("patient/{id}")
    public HttpResponse<List<AppointmentResponseDTO>> getPatientAppointments(@PathVariable UUID id) {
        return HttpResponse.ok(appointmentService.getPatientAppointments(id));
    }

    @Get("doctor/{id}")
    public HttpResponse<List<AppointmentResponseDTO>> getDoctorAppointments(@PathVariable UUID id) {
        return HttpResponse.ok(appointmentService.getDoctorAppointments(id));
    }

}
