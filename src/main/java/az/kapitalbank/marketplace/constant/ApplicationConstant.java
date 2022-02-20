package az.kapitalbank.marketplace.constant;

import java.util.List;

public interface ApplicationConstant {
    String UMICO_MARKETPLACE = "umico_marketplace";
    String PURCHASE_DESCRIPTION = "Umico marketplace purchase operation";
    String COMPLETE_DESCRIPTION = "Umico marketplace complete operation";
    String REVERSE_DESCRIPTION = "Umico marketplace reverse operation";
    List<Integer> CUSTOM_HTTP_STATUS_CODES = List.of(400, 404);

}
