package az.kapitalbank.marketplace.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum OrderStatus {
    SUCCESS(1),
    FAIL(2),
    ERROR(0);

    int status;

    public int getStatus() {
        return status;
    }
}
