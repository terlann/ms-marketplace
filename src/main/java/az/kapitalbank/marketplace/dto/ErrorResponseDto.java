package az.kapitalbank.marketplace.dto;

import java.util.Map;

import az.kapitalbank.marketplace.constant.Error;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorResponseDto {
    String code;
    String message;
    Map<String, String> checks;

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
