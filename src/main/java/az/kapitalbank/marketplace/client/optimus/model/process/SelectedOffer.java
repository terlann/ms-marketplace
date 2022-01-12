package az.kapitalbank.marketplace.client.optimus.model.process;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SelectedOffer {
    String id;
    String offerType;
    Offer cashOffer;
    Offer cardOffer;
}
