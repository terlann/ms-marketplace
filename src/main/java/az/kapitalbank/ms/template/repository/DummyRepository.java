package az.kapitalbank.ms.template.repository;

import az.kapitalbank.ms.template.entity.DummyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DummyRepository extends JpaRepository<DummyEntity, Long> {

}
