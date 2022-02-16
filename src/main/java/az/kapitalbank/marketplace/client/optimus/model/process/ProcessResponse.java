package az.kapitalbank.marketplace.client.optimus.model.process;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessResponse {
    @JsonProperty("id")
    String taskId;
    ProcessData variables;
    Date processCreateTime;
}
