package az.kapitalbank.marketplace.repository;

import java.util.List;
import java.util.UUID;

import az.kapitalbank.marketplace.constant.FraudMark;
import az.kapitalbank.marketplace.entity.FraudEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface FraudRepository extends JpaRepository<FraudEntity, UUID> {

    List<FraudEntity> findByIdAndFraudMark(UUID trackId, FraudMark fraudMark);
}
