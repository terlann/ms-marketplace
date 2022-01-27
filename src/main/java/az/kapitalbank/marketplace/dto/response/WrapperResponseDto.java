package az.kapitalbank.marketplace.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WrapperResponseDto<T> {

    String code;
    String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    T data;

    public static WrapperResponseDto<Object> of(String code, String message) {
        WrapperResponseDto<Object> wrapperResponseDto = new WrapperResponseDto();
        wrapperResponseDto.setCode(code);
        wrapperResponseDto.setMessage(message);
        return wrapperResponseDto;
    }

    public static WrapperResponseDto<Object> ofSuccess() {
        WrapperResponseDto<Object> wrapperResponseDto = new WrapperResponseDto<>();
        wrapperResponseDto.setCode("200");
        wrapperResponseDto.setMessage("SUCCESS");
        return wrapperResponseDto;
    }

//TODO this class will remove
}
