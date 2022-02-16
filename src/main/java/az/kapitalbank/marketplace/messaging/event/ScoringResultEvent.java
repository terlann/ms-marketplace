package az.kapitalbank.marketplace.messaging.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import static az.kapitalbank.marketplace.constants.ProcessStatus.BUSINESS_ERROR;
import static az.kapitalbank.marketplace.constants.ProcessStatus.COMPLETED;
import static az.kapitalbank.marketplace.constants.ProcessStatus.INCIDENT_HAPPENED;
import static az.kapitalbank.marketplace.constants.ProcessStatus.IN_USER_ACTIVITY;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoringResultEvent {
    String processStatus;
    String businessKey;
    String username;
    String messageType;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "processStatus", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = String.class, name = COMPLETED),
            @JsonSubTypes.Type(value = String.class, name = INCIDENT_HAPPENED),
            @JsonSubTypes.Type(value = BusinessErrorData[].class, name = BUSINESS_ERROR),
            @JsonSubTypes.Type(value = InUserActivityData.class, name = IN_USER_ACTIVITY)
    })
    Object data;
}

