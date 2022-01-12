package az.kapitalbank.marketplace.messaging.event;

import lombok.Data;

@Data
public class InUserActivityData {
    String taskDefinitionKey;
    String taskId;
}