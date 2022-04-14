package az.kapitalbank.marketplace.dto.request;

import java.util.UUID;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDto {
    private String umicoUserId;
    private UUID customerId;
    @NotBlank
    private String orderNo;
}
