package az.kapitalbank.marketplace.client.optimus.model.scoring;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerNumber {
    String name;
    Number number;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Number {
        String number;
    }
}
