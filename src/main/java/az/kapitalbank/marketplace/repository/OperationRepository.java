package az.kapitalbank.marketplace.repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OperationRepository extends JpaRepository<OperationEntity, UUID> {
    Optional<OperationEntity> findByTelesalesOrderId(String telesalesOrderId);

    Optional<OperationEntity> findByBusinessKey(String businessKey);

    boolean existsByCustomerAndUmicoDecisionStatusIn(CustomerEntity customer,
                                                     Set<UmicoDecisionStatus> umicoDecisionStatus);

}
