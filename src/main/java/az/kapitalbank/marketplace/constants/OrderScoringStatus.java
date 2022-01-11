package az.kapitalbank.marketplace.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;


@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum OrderScoringStatus {
    APPROVED(1),
    REJECTED(0);

    int status;

    public int getStatus() {
        return status;
    }
}
