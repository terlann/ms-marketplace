package az.kapitalbank.marketplace.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompletedData {
    String endActivityId;
    String endActivityName;
}