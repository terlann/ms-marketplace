package az.kapitalbank.marketplace.repository;

import az.kapitalbank.marketplace.entity.ProductEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

}
