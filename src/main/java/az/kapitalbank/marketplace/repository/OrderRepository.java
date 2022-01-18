package az.kapitalbank.marketplace.repository;

import az.kapitalbank.marketplace.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    Optional<OrderEntity> findByEteOrderId(String eteOrderId);

    Optional<OrderEntity> findById(String id);

    Optional<OrderEntity> findByBusinessKey(String businessKey);

/*    @Query(value = "update marketplace_orders mo " +
            "set    mo.order_status = :status " +
            "where  mo.track_id = :orderId",nativeQuery = true)
    void updateOrderStatusByOrderId(@Param("orderStatus") String orderStatus, @Param("orderId") String orderId);*/
}
