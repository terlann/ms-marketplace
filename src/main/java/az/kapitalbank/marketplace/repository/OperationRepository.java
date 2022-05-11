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
            value = "SELECT CASE WHEN (COUNT(*) > 0) THEN 'true'  ELSE 'false' END "
                    + "FROM KB_MARKETPLACE_OPERATION "
                    + "WHERE CUSTOMER_ID = :customerId "
                    + "AND (UMICO_DECISION_STATUS IS NULL "
                    + "OR UMICO_DECISION_STATUS IN :decisions)")
    boolean existsByCustomerIdAndUmicoDecisionStatuses(
            String customerId, List<UmicoDecisionStatus> decisions);
}
