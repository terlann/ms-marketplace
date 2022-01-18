package az.kapitalbank.marketplace.repository;

import az.kapitalbank.marketplace.entity.OrderProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OrderProductsRepository extends JpaRepository<OrderProductEntity, String> {

}
