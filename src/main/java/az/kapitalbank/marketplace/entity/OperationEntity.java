package az.kapitalbank.marketplace.entity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import az.kapitalbank.marketplace.constants.OperationStatus;
import az.kapitalbank.marketplace.constants.ScoringStatus;
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
@Table(name = "KB_MARKETPLACE_OPERATION")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OperationEntity extends BaseEntity {
    String eteOrderId;
    BigDecimal totalAmount;
    Integer loanTerm;
    OperationStatus status;
    LocalDateTime deletedAt;
    String latitude;
    String longitude;
    String ip;
    String userAgent;
    String taskId;
    String businessKey;
    ScoringStatus scoringStatus;
    LocalDateTime scoringDate;
    LocalDate loanStartDate;
    LocalDate loanEndDate;
    String dvsOrderId;
    String dvsOrderStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    CustomerEntity customer;

    @Builder.Default
    @OneToMany(mappedBy = "operation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderEntity> orders = new ArrayList<>();

    @Override
    public String toString() {
        return "OperationEntity{" +
                "eteOrderId='" + eteOrderId + '\'' +
                ", totalAmount=" + totalAmount +
                ", orders=" + orders +
                '}';
    }
}
