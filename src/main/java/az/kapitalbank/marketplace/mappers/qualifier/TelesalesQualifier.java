package az.kapitalbank.marketplace.mappers.qualifier;

import java.util.List;
import java.util.stream.Collectors;

import az.kapitalbank.marketplace.constants.FraudReason;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;


@Component
public class TelesalesQualifier {

    @Named("mapFraudReasons")
    public String mapFraudReasons(List<FraudReason> fraudReasons) {
        return fraudReasons.stream().map(Object::toString).collect(Collectors.joining(";"));
    }
}
