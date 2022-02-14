package az.kapitalbank.marketplace.entity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDate;
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
    String email;
    String pin;
    String workPlace;
    String mobileNumber;
    String umicoUserId;
    String fullName;
    Boolean isAgreement;
    String additionalPhoneNumber1;
    String additionalPhoneNumber2;
    LocalDate birthday;
    String cardUUID;
    CustomerStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperationEntity> operations = new ArrayList<>();
}
