package az.kapitalbank.marketplace.repository;

import az.kapitalbank.marketplace.entity.CustomerEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    Optional<CustomerEntity> findByUmicoUserId(String umicoUserId);

}
