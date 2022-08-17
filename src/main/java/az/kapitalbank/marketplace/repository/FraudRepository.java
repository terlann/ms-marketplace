package az.kapitalbank.marketplace.repository;

import az.kapitalbank.marketplace.entity.FraudEntity;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudRepository extends JpaRepository<FraudEntity, UUID> {

    @Modifying
    @Query(nativeQuery = true,
            value = "UPDATE KB_MARKETPLACE_FRAUD "
                    + "SET is_deleted = 1 "
                    + "WHERE value IN :values")
    void softDeleteByValueIn(Set<String> values);
}
