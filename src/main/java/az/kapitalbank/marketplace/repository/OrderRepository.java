package az.kapitalbank.marketplace.repository;

import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.entity.OrderEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByOrderNo(String orderNo);

    List<OrderEntity> findByTransactionDateBeforeAndTransactionStatusIn(
            LocalDateTime transactionDate,
            List<TransactionStatus> transactionStatuses);

}
