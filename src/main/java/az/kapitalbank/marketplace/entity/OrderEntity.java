package az.kapitalbank.marketplace.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import az.kapitalbank.marketplace.constants.TransactionStatus;
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
@Table(name = "KB_MARKETPLACE_ORDER")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderEntity extends BaseEntity {
    BigDecimal orderLastAmount;
    BigDecimal totalAmount;
    String orderNo;
    @Column(length = 500)
    String deliveryAddress;
    BigDecimal commission;
    String transactionId;
    String rrn;
    String approvalCode;
    @Enumerated(EnumType.STRING)
    TransactionStatus transactionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id", nullable = false, updatable = false)
    OperationEntity operation;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductEntity> products = new ArrayList<>();
}
