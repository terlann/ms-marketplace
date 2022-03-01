package az.kapitalbank.marketplace.client.dvs.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DvsGetDetailsResponse {
    String webUrl;
    String businessCase;
    String docDownloadUrl;
}
