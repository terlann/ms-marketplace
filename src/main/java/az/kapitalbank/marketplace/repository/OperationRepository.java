package az.kapitalbank.marketplace.repository;

import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.entity.OperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OperationRepository extends JpaRepository<OperationEntity, UUID> {
    Optional<OperationEntity> findByEteOrderId(String eteOrderId);

    Optional<OperationEntity> findByBusinessKey(String businessKey);

}
