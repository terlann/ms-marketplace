package az.kapitalbank.marketplace.mapper.qualifier;

import az.kapitalbank.marketplace.constant.FraudType;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;


@Component
public class TelesalesQualifier {

    @Named("mapFraudTypes")
    public String mapFraudTypes(List<FraudType> fraudTypes) {
        return fraudTypes.stream().map(Object::toString).collect(Collectors.joining(";"));
    }
}
