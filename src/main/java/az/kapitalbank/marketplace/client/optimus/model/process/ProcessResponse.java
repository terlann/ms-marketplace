package az.kapitalbank.marketplace.client.optimus.model.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProcessResponse {
    @JsonProperty("id")
    String taskId;
    ProcessData variables;
    Date processCreateTime;
}
