package co.inter.piggies.repository;

import co.inter.piggies.domain.doctor.Doctor;
import io.micronaut.data.jpa.repository.JpaRepository;
import jakarta.data.repository.Repository;

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
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    boolean existsByCrm(String crm);
    boolean existsByCrmAndIdNot(String crm, UUID id);
}
