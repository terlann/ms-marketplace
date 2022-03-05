package az.kapitalbank.marketplace.constant;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public enum ProcessStatus {
    COMPLETED,
    BUSINESS_ERROR,
    IN_USER_ACTIVITY,
    INCIDENT_HAPPENED
}