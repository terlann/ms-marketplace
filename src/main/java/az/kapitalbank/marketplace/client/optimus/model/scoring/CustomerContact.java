package az.kapitalbank.marketplace.client.optimus.model.scoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerContact {
    @JsonProperty("others")
    List<CustomerNumber> customerNumberList;
}
