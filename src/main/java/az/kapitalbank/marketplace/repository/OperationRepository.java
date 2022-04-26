package az.kapitalbank.marketplace.repository;

import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.OperationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface OperationRepository extends JpaRepository<OperationEntity, UUID> {

    Optional<OperationEntity> findByTelesalesOrderId(String telesalesOrderId);

    Optional<OperationEntity> findByBusinessKey(String businessKey);

    @Query(nativeQuery = true,
            value = "select CASE WHEN (count(*) > 0) THEN 'true'  ELSE 'false' END "
                    + "from KB_MARKETPLACE_OPERATION "
                    + "where CUSTOMER_ID = :customerId "
                    + "AND (UMICO_DECISION_STATUS is null "
                    + "or UMICO_DECISION_STATUS in (:decisions))")
    boolean existsByCustomerIdAndUmicoDecisionStatuses(
            String customerId, List<UmicoDecisionStatus> decisions);

}
