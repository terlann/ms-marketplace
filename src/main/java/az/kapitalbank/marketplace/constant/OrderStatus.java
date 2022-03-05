package az.kapitalbank.marketplace.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum OrderStatus {
    SUCCESS,
    FAIL
}
