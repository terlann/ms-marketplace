package az.kapitalbank.marketplace.repository;

import java.util.UUID;

import az.kapitalbank.marketplace.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

}
