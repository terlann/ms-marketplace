package az.kapitalbank.marketplace.client.integration.model;

import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@ToString
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IamasResponse {
    boolean active;
    String documentNumber;
    String pin;
    IamasPerson personAz;

    @Getter
    @Setter
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class ExpiryDate {
        LocalDate date;
        LocalDate dateEx;
    }
}
