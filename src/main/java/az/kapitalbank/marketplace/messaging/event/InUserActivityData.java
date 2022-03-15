package az.kapitalbank.marketplace.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InUserActivityData {
    String taskDefinitionKey;
    String taskId;
}
