package az.kapitalbank.marketplace.repository;

import az.kapitalbank.marketplace.entity.FraudEntity;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudRepository extends JpaRepository<FraudEntity, UUID> {

    @Query(nativeQuery = true,
            value = "UPDATE KB_MARKETPLACE_FRAUD "
                    + "SET isDeleted = 1 "
                    + "WHERE value IN :value")
    void softDeleteByValueIn(Set<String> values);
}
