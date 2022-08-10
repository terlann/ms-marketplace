package az.kapitalbank.marketplace.repository;

import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.OperationEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
            String customerId, List<String> decisions);

    List<OperationEntity> findByUpdatedAtBeforeAndUmicoDecisionStatusIn(
            OffsetDateTime updateAt, Set<UmicoDecisionStatus> umicoDecisionStatuses);

    List<OperationEntity> findByUmicoDecisionStatusAndIsSendLeadIsFalse(
            UmicoDecisionStatus decisionStatus);

    @Query(value = "SELECT distinct p FROM OperationEntity p JOIN FETCH"
            + " p.orders o where o.transactionStatus=:transactionStatus")
    List<OperationEntity> findAllOperationByTransactionStatus(TransactionStatus transactionStatus);

    @Query(value =
            "SELECT o.PIN "
                    + "             FROM   KB_MARKETPLACE_CUSTOMER c, "
                    + "                    KB_MARKETPLACE_OPERATION o "
                    + "             WHERE  c.ID = o.CUSTOMER_ID "
                    + "                    AND c.UMICO_USER_ID = :umicoUserId"
                    +
                    "                    AND o.UMICO_DECISION_STATUS = 'REJECTED') ", nativeQuery = true)
    List<String> getRejectedPinWithCurrentUmicoUserId(String umicoUserId);
}
