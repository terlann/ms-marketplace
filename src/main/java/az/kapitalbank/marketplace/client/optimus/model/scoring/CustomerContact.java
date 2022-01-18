package az.kapitalbank.marketplace.client.optimus.model.scoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerContact {
    @JsonProperty("others")
    List<CustomerNumber> customerNumberList;
}
