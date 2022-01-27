package az.kapitalbank.marketplace.repository;

import java.util.List;
import java.util.UUID;

import az.kapitalbank.marketplace.constants.FraudReason;
import az.kapitalbank.marketplace.entity.FraudEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface FraudRepository extends JpaRepository<FraudEntity, UUID> {

    @Query(nativeQuery = true,
            value = "select f.fraud_reason " +
                    "from   fraud f " +
                    "where  f.track_id = :trackId " +
                    "       and f.fraud_mark = 'SUSPICIOUS'")
    List<FraudReason> getAllSuspiciousFraudReasonByTrackId(UUID trackId);
}
