package az.kapitalbank.marketplace.dto.request;

import java.util.UUID;

import az.kapitalbank.marketplace.client.otp.model.ChannelRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendOtpRequestDto {
    UUID trackId;
}
