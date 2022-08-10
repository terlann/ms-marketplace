package az.kapitalbank.marketplace.repository;

import az.kapitalbank.marketplace.entity.FraudEntity;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudRepository extends JpaRepository<FraudEntity, UUID> {
    void deleteByValueIn(Set<String> value);
}
