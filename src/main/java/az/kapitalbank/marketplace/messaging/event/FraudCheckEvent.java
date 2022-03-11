package az.kapitalbank.marketplace.messaging.event;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.Set;
import java.util.UUID;
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
public class FraudCheckEvent {
    UUID trackId;
    String umicoUserId;
    String ip;
    String pin;
    String email;
    String userAgent;
    String workPlace;
    String mobileNumber;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    Set<String> deliveryAddresses;
}
