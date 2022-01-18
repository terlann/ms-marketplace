package az.kapitalbank.marketplace.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import az.kapitalbank.marketplace.constants.FraudMark;
import az.kapitalbank.marketplace.constants.FraudReason;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = FraudEntity.TABLE_NAME)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FraudEntity {

    static final String TABLE_NAME = "FRAUD";

    @Id
    String trackId;
    @Enumerated(EnumType.STRING)
    FraudReason fraudReason;
    @Enumerated(EnumType.STRING)
    FraudMark fraudMark;
    String fraudDescription;
}
