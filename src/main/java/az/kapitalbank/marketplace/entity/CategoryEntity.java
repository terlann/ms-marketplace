package az.kapitalbank.marketplace.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
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
@Table(name = "KB_MARKETPLACE_CATEGORY")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryEntity {
    @Id
    Integer id;
    Integer parentId;
    String nameAz;
    String nameRu;
}
