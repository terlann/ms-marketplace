package az.kapitalbank.marketplace.dto;

import az.kapitalbank.marketplace.constant.Error;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto {
    private String code;
    private String message;
    private Map<String, String> checks;

    public ErrorResponseDto(Error error, Map<String, String> checks) {
        this.code = error.getCode();
        this.message = error.getMessage();
        this.checks = checks;
    }

    public ErrorResponseDto(Error error) {
        this.code = error.getCode();
        this.message = error.getMessage();
    }
}
