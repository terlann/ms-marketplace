package az.kapitalbank.marketplace.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.math.BigDecimal;

import az.kapitalbank.marketplace.constants.Currency;
import az.kapitalbank.marketplace.constants.PurchaseStatus;
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
@Table(name = "PURCHASE")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PurchaseEntity extends BaseEntity {
    String terminalName;
    String rrn;
    String cardUUID;
    BigDecimal amount;
    @Enumerated(EnumType.STRING)
    Currency currency;
    String approvalCode;
    @Enumerated(EnumType.STRING)
    PurchaseStatus status;
    String purchaseDate;
    String rejectedDate;
    String completePurchaseDate;
}
