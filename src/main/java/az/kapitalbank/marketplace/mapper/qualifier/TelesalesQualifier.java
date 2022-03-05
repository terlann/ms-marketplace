package az.kapitalbank.marketplace.mapper.qualifier;

import java.util.List;
import java.util.stream.Collectors;

import az.kapitalbank.marketplace.constant.FraudReason;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;


@Component
public class TelesalesQualifier {

    @Named("mapFraudReasons")
    public String mapFraudReasons(List<FraudReason> fraudReasons) {
        return fraudReasons.stream().map(Object::toString).collect(Collectors.joining(";"));
    }
}
