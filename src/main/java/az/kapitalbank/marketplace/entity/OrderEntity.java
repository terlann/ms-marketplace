package az.kapitalbank.marketplace.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = OrderEntity.TABLE_NAME)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderEntity {

    static final String TABLE_NAME = "MARKETPLACE_ORDERS";

    @Id
    @Column(name = "track_id")
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    String id;
    String eteOrderId;
    @Builder.Default
    Integer isActive = 1;
    @Builder.Default
    LocalDateTime insertedDate = LocalDateTime.now();
    BigDecimal totalAmount;
    Integer scoringStatus;
    LocalDateTime scoringDate;
    String creditId;
    LocalDate loanStartDate;
    LocalDate loanEndDate;
    Integer shippingStatus;
    LocalDateTime deactivatedDate;
    Integer changeLimitStatus;
    LocalDateTime changeLimitDate;
    @Column(name = "scoring_decision_send_status")
    Integer sendDecisionScoring;
    @Column(name = "scoring_decision_send_date")
    LocalDateTime sendDecisionScoringDate;
    LocalDateTime scoringRejectedReason;
    String dvsOrderId;
    String loanDuration;
    String taskId;
    String businessKey;
    String orderStatus;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<OrderProductEntity> products = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    CustomerEntity customer;
}
