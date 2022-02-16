package az.kapitalbank.marketplace.entity;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    UUID id;

    @CreationTimestamp
    @Column(name = "CREATED_AT", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime updatedAt;
}
