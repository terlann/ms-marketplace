package az.kapitalbank.marketplace.mapper.qualifier;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;


@Component
public class CreateOrderQualifier {

    @Named("mapDeliveryAddresses")
    public Set<String> mapDeliveryAddresses(List<OrderProductDeliveryInfo> deliveryInfo) {
        return deliveryInfo
                .stream()
                .map(OrderProductDeliveryInfo::getDeliveryAddress)
                .collect(Collectors.toSet());
    }
}
