package az.kapitalbank.marketplace.repository;

import java.util.UUID;

import az.kapitalbank.marketplace.entity.PurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseEntity, UUID> {

}
