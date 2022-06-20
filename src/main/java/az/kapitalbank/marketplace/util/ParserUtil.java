package az.kapitalbank.marketplace.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ParserUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public static <T> T parseTo(String json, Class<T> typeClass) {
        return objectMapper.readValue(json, typeClass);
    }
}
