package az.kapitalbank.marketplace.repository;

import java.util.UUID;

import az.kapitalbank.marketplace.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {
}
