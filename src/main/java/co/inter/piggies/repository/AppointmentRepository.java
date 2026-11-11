package co.inter.piggies.repository;

import co.inter.piggies.domain.appointment.Appointment;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jpa.repository.JpaRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 *--------------------------------------------------------------------------*
 * @Repository: marca a interface como um repository do Micronaut Data.     *
 * A implementação é gerada em tempo de compilação, então basta declarar    *
 * a interface. Os métodos do JpaRepository (save, findById, findAll,       *
 * update, delete...) já vêm prontos, e métodos novos são criados pelo nome *
 * (ex.: findByNome(String nome) gera a consulta sozinho).                  *
 *--------------------------------------------------------------------------*
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("SELECT COUNT(a) FROM Appointment a "
            + "WHERE a.doctor.id = :doctorId "
            + "AND a.status = :status "
            + "AND a.scheduledAt BETWEEN :inicio AND :fim")
    long horarioOcupado(@Param("doctorId") UUID doctorId,
                       @Param("status") String status,
                       @Param("inicio") OffsetDateTime inicio,
                       @Param("fim") OffsetDateTime fim);

    List<Appointment> findAllByDoctorId(UUID id);

    List<Appointment> findAllByPatientId(UUID id);
}
