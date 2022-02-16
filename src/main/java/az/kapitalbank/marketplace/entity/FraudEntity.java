package az.kapitalbank.marketplace.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import az.kapitalbank.marketplace.constants.FraudMark;
import az.kapitalbank.marketplace.constants.FraudReason;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "KB_MARKETPLACE_FRAUD")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FraudEntity extends BaseEntity {
    UUID trackId;
    @Enumerated(EnumType.STRING)
    FraudReason fraudReason;
    @Enumerated(EnumType.STRING)
    FraudMark fraudMark;
    String fraudDescription;
}
