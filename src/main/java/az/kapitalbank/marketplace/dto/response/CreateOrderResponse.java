package az.kapitalbank.marketplace.dto.response;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "of")
public class CreateOrderResponse {
    UUID trackId;
}
