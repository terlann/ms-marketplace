package az.kapitalbank.marketplace.client.atlas.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardResponse {
    String uid;
}
