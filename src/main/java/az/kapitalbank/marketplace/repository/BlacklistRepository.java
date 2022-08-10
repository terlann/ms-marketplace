package az.kapitalbank.marketplace.repository;

import az.kapitalbank.marketplace.entity.BlacklistEntity;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlacklistRepository extends JpaRepository<BlacklistEntity, UUID> {
    void deleteByValueIn(Set<String> value);
}
