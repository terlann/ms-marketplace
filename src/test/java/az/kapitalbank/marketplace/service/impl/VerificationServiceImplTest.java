package az.kapitalbank.marketplace.service.impl;

import az.kapitalbank.marketplace.client.dvs.DvsClient;
import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;
import az.kapitalbank.marketplace.exception.models.FeignClientException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static az.kapitalbank.marketplace.property.OrderConstants.TRACK_ID;
import static az.kapitalbank.marketplace.property.VerificationServiceConstants.BUSINESS_CASE;
import static az.kapitalbank.marketplace.property.VerificationServiceConstants.DOC_DOWNLOAD_URL;
import static az.kapitalbank.marketplace.property.VerificationServiceConstants.DVS_ID;
import static az.kapitalbank.marketplace.property.VerificationServiceConstants.ERROR_MESSAGE;
import static az.kapitalbank.marketplace.property.VerificationServiceConstants.METHOD_KEY;
import static az.kapitalbank.marketplace.property.VerificationServiceConstants.ORDER_ID;
import static az.kapitalbank.marketplace.property.VerificationServiceConstants.WEB_URL;


@ExtendWith(MockitoExtension.class)
class VerificationServiceImplTest {

    @Mock
    DvsClient dvsClient;

    @InjectMocks
    VerificationServiceImpl verificationService;

    @Test
    void whenGetDetailsCall_thenShouldBeSuccess() {
        DvsGetDetailsResponse dvsGetDetailsResponse = generateDvsGetDetailsResponse();
        Mockito.when(dvsClient.getDetails(ORDER_ID, DVS_ID)).thenReturn(dvsGetDetailsResponse);
        Optional<DvsGetDetailsResponse> dvsGetDetailsResponseResult = verificationService
                .getDetails(TRACK_ID, ORDER_ID, DVS_ID);
        Assertions.assertThat(dvsGetDetailsResponseResult.get())
                .usingRecursiveComparison().isEqualTo(dvsGetDetailsResponse);
        Mockito.verify(dvsClient).getDetails(ORDER_ID, DVS_ID);
    }

    @Test
    void whenGetDetailsCall_thenShouldBeNull() {
        Mockito.when(dvsClient.getDetails(ORDER_ID, DVS_ID))
                .thenThrow(new FeignClientException(METHOD_KEY, ERROR_MESSAGE));
        Optional<DvsGetDetailsResponse> dvsGetDetailsResponseResult = verificationService
                .getDetails(TRACK_ID, ORDER_ID, DVS_ID);
        Assertions.assertThat(dvsGetDetailsResponseResult).isEmpty();
        Mockito.verify(dvsClient).getDetails(ORDER_ID, DVS_ID);
    }


    public DvsGetDetailsResponse generateDvsGetDetailsResponse() {
        return DvsGetDetailsResponse.builder()
                .webUrl(WEB_URL)
                .businessCase(BUSINESS_CASE)
                .docDownloadUrl(DOC_DOWNLOAD_URL)
                .build();
    }
}