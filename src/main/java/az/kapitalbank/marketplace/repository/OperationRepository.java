package az.kapitalbank.marketplace.repository;

import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.entity.OperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface OperationRepository extends JpaRepository<OperationEntity, UUID> {
    Optional<OperationEntity> findByTelesalesOrderId(String telesalesOrderId);

    Optional<OperationEntity> findByBusinessKey(String businessKey);

    Optional<OperationEntity> findByDvsOrderId(String dvsOrderId);

    @Query(nativeQuery = true,
            value = "select count(*) " +
                    "from   kb_marketplace_customer c" +
                    "       inner join kb_marketplace_operation o " +
                    "               on c.id = o.customer_id " +
                    "where  pin = :pin " +
                    "       and o.umico_decision_status in( 'APPROVED', 'PREAPPROVED', 'PENDING' )")
    int operationCountByPinAndDecisionStatus(String pin);
}
