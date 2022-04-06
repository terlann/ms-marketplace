package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.dvs.DvsClient;
import az.kapitalbank.marketplace.client.dvs.exception.DvsClientException;
import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private DvsClient dvsClient;
    @InjectMocks
    private VerificationService verificationService;

    @Test
    void getDvsUrl_Success() {
        when(dvsClient.getDetails(UUID.fromString(TRACK_ID.getValue()),
                12345L)).thenReturn(DvsGetDetailsResponse.builder().build());
        verificationService.getDvsUrl(
                UUID.fromString(TRACK_ID.getValue()), 12345L);
        verify(dvsClient).getDetails(UUID.fromString(TRACK_ID.getValue()),
                12345L);
    }

    @Test
    void getDvsUrl_DvsClientException() {
        when(dvsClient.getDetails(UUID.fromString(TRACK_ID.getValue()),
                12345L)).thenThrow(new DvsClientException("", ""));
        verificationService.getDvsUrl(
                UUID.fromString(TRACK_ID.getValue()), 12345L);
        verify(dvsClient).getDetails(UUID.fromString(TRACK_ID.getValue()),
                12345L);
    }
}
