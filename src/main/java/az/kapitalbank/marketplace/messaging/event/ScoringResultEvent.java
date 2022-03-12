package az.kapitalbank.marketplace.messaging.event;

import az.kapitalbank.marketplace.constant.ProcessStatus;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoringResultEvent {
    ProcessStatus processStatus;
    String businessKey;
    String username;
    String messageType;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            property = "processStatus", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = CompletedData.class, name = "COMPLETED"),
            @JsonSubTypes.Type(value = String.class, name = "INCIDENT_HAPPENED"),
            @JsonSubTypes.Type(value = BusinessErrorData[].class, name = "BUSINESS_ERROR"),
            @JsonSubTypes.Type(value = InUserActivityData.class, name = "IN_USER_ACTIVITY")
    })
    Object data;
}

