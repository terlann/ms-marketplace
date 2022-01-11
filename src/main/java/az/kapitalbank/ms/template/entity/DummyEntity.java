package az.kapitalbank.ms.template.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "dummy_table")
public class DummyEntity implements Serializable {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "dummy_field")
    private String dummyField;
}
