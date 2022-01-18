package az.kapitalbank.marketplace.messaging.event;

import lombok.Data;

@Data
public class BusinessErrorData {
    String id;
    String reason;
    String type;
}