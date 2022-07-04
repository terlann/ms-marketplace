package az.kapitalbank.marketplace.entity;

import az.kapitalbank.marketplace.constant.DvsStatus;
import az.kapitalbank.marketplace.constant.OperationRejectReason;
import az.kapitalbank.marketplace.constant.ScoringStatus;
import az.kapitalbank.marketplace.constant.SendLeadReason;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "KB_MARKETPLACE_OPERATION")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OperationEntity extends BaseEntity {
    String telesalesOrderId;
    String leadId;
    BigDecimal totalAmount;
    BigDecimal commission;
    Integer loanTerm;
    String latitude;
    String longitude;
    String ip;
    String userAgent;
    String email;
    String pin;
    String workPlace;
    String mobileNumber;
    String fullName;
    String additionalPhoneNumber1;
    String additionalPhoneNumber2;
    String taskId;
    BigDecimal loanPercent;
    String businessKey;
    LocalDateTime scoringDate;
    @Enumerated(EnumType.STRING)
    ScoringStatus scoringStatus;
    LocalDate loanContractStartDate;
    LocalDate loanContractEndDate;
    Long dvsOrderId;
    @Enumerated(EnumType.STRING)
    DvsStatus dvsOrderStatus;
    @Enumerated(EnumType.STRING)
    UmicoDecisionStatus umicoDecisionStatus;
    @Enumerated(EnumType.STRING)
    OperationRejectReason rejectReason;
    Boolean isSendLead;
    @Enumerated(EnumType.STRING)
    SendLeadReason sendLeadReason;
    BigDecimal scoredAmount;
    Boolean isOtpOperation;
    String businessErrorReason;
    String deviceTokenId;
    String cif;
    String cardCreditContractNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    CustomerEntity customer;

    @Builder.Default
    @OneToMany(mappedBy = "operation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderEntity> orders = new ArrayList<>();
}
