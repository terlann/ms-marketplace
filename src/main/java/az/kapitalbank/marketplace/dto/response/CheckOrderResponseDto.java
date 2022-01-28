package az.kapitalbank.marketplace.dto.response;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckOrderResponseDto {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String eteOrderId;
    @NotNull
    UUID trackId;
    @NotNull
    BigDecimal totalAmount;
}
