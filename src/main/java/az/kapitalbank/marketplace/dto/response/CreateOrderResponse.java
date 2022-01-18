package az.kapitalbank.marketplace.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "of")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateOrderResponse {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String eteOrderId;
    final String marketplaceTrackId;
}
