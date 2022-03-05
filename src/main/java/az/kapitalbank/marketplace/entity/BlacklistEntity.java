package az.kapitalbank.marketplace.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import az.kapitalbank.marketplace.constant.BlacklistType;
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
@Table(name = "KB_MARKETPLACE_BLACKLIST")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlacklistEntity extends BaseEntity {

    @Column(unique = true, nullable = false)
    String name;
    @Enumerated(EnumType.STRING)
    BlacklistType type;
}