package az.kapitalbank.marketplace.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = OrderProductEntity.TABLE_NAME)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderProductEntity {

    static final String TABLE_NAME = "MARKETPLACE_ORDER_PRODUCTS";

    @Id
    @SequenceGenerator(name = "seq_marketplace_order_products_gen",
            sequenceName = "seq_marketplace_order_products",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_marketplace_order_products_gen")
    Long id;
    String productId;
    BigDecimal productAmount;
    String productName;
    String partnerCmsId;
    String orderNo;
    String itemType;
    String trancheId;
    LocalDateTime createdTrancheDate;
    BigDecimal lastAmount;
    Integer deliveryStatus;
    LocalDateTime deliveryDate;
    String deliveryAddress;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    OrderEntity order;

}
