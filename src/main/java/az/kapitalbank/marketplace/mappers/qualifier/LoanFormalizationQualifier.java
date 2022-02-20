package az.kapitalbank.marketplace.mappers.qualifier;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;


@Component
public class LoanFormalizationQualifier {

    @Named("toString")
    public String toString(Integer source) {
        return String.valueOf(source);
    }

    @Named("asList")
    public List<String> asList(String source) {
        return List.of(source);
    }

    @Named("toLocalDateTime")
    public LocalDateTime toLocalDateTime(Date source) {
        return LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
    }
}
