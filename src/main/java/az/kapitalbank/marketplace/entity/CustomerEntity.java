package az.kapitalbank.marketplace.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = CustomerEntity.TABLE_NAME)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerEntity {

    static final String TABLE_NAME = "CUSTOMER";

    @Id
    @Column(name = "track_id")
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    String id;
    String ip;
    String email;
    String device;
    String identityNumber;
    String employerName;
    String mobileNumber;
    String umicoUserId;
    String fullName;
    String additionalPhoneNumber1;
    String additionalPhoneNumber2;
    @MapsId
    @JoinColumn(name = "track_id")
    @OneToOne(cascade = CascadeType.ALL)
    OrderEntity order;
}
