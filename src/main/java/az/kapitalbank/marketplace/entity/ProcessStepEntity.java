package az.kapitalbank.marketplace.entity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
@Table(name = "KB_MARKETPLACE_PROCESS_STEP")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessStepEntity extends BaseEntity {

    String value;

    @ManyToOne
    OperationEntity operation;
}
