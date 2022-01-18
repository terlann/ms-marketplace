package az.kapitalbank.marketplace.mappers.qualifier;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


@Component
public class LoanFormalizationQualifier {

    @Named("toString")
    public String toString(Integer source) {
        return String.valueOf(source);
    }

    @Named("asList")
    public List<String> asList(String source) {
        return Arrays.asList(source);
    }

    @Named("toLocalDateTime")
    public LocalDateTime toLocalDateTime(Date source) {
        return LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
    }
}
