package az.kapitalbank.marketplace.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByOrderNo(String orderNo);

    List<OrderEntity> findByOrderNoIn(List<String> orders);
}
