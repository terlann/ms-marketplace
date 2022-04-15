package az.kapitalbank.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Table(name = "KB_MARKETPLACE_CUSTOMER")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerEntity extends BaseEntity {
    String umicoUserId;
    boolean isAgreement;
    String cardId;
    @Builder.Default
    BigDecimal lastTempAmount = BigDecimal.ZERO;
    LocalDateTime completeProcessDate;

    @Builder.Default
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperationEntity> operations = new ArrayList<>();
}
