package az.kapitalbank.marketplace.client.optimus.model.process;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SelectedOffer {
    String id;
    String offerType;
    Offer cashOffer;
    Offer cardOffer;
}
