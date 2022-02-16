package az.kapitalbank.marketplace.entity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import az.kapitalbank.marketplace.constants.CustomerStatus;
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
    Boolean isAgreement;
    String cardUUID;
    CustomerStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperationEntity> operations = new ArrayList<>();
}
