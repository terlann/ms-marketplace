package az.kapitalbank.marketplace.client.optimus.model.scoring;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerNumber {
    String name;
    NumberDto number;

    public CustomerNumber(String name, String number) {
        this.name = name;
        this.number = new NumberDto(number);
    }

    @Data
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    class NumberDto {
        String number;
    }
}
