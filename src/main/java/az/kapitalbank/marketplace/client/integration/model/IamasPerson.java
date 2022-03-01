package az.kapitalbank.marketplace.client.integration.model;

import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IamasPerson {
    boolean active;
    IamasBirthDate birthDate;

    @Getter
    @Setter
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class IamasBirthDate {
        LocalDate date;
        LocalDate dateEx;
    }

}
