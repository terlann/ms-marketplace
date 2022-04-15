package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.ConstantObject.getOperationEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.exception.UmicoClientException;
import az.kapitalbank.marketplace.client.umico.model.PrePurchaseResultRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionResponse;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.constants.TestConstants;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UmicoServiceTest {

    @Mock
    private UmicoClient umicoClient;
    @InjectMocks
    private UmicoService umicoService;

    @Test
    void sendPrePurchaseResult() {
        umicoService.sendPrePurchaseResult(UUID.fromString(TestConstants.TRACK_ID.getValue()));
        verify(umicoClient).sendPrePurchaseResult(any(PrePurchaseResultRequest.class), eq(null));
    }

    @Test
    void sendPrePurchaseResult_Exception() {
        doThrow(new UmicoClientException("exception"))
                .when(umicoClient)
                .sendPrePurchaseResult(any(PrePurchaseResultRequest.class), eq(null));
        umicoService.sendPrePurchaseResult(UUID.fromString(TestConstants.TRACK_ID.getValue()));
        verify(umicoClient).sendPrePurchaseResult(any(PrePurchaseResultRequest.class), eq(null));
    }

    @Test
    void testSendPendingDecision_Success() {
        when(umicoClient.sendDecision(any(UmicoDecisionRequest.class), eq(null)))
                .thenReturn(new UmicoDecisionResponse("status", 0));

        umicoService.sendPendingDecision(UUID.fromString("d379a893-bb0a-490a-86ab-3869015ceb31"));
        verify(umicoClient).sendDecision(any(UmicoDecisionRequest.class), eq(null));
    }

    @Test
    void testSendRejectedDecision() {
        when(umicoClient.sendDecision(any(UmicoDecisionRequest.class), eq(null)))
                .thenReturn(new UmicoDecisionResponse("status", 0));

        umicoService.sendRejectedDecision(UUID.fromString("70e5bb3d-0120-456a-a5d3-fa953bcd5d7b"));
        verify(umicoClient).sendDecision(any(UmicoDecisionRequest.class), eq(null));
    }

    @Test
    void testSendPreApprovedDecision() {
        when(umicoClient.sendDecision(any(UmicoDecisionRequest.class), eq(null)))
                .thenReturn(new UmicoDecisionResponse("status", 0));

        umicoService.sendPreApprovedDecision(
                UUID.fromString("70e5bb3d-0120-456a-a5d3-fa953bcd5d7b"), "dvsUrl",
                UmicoDecisionStatus.APPROVED);
        verify(umicoClient).sendDecision(any(UmicoDecisionRequest.class), eq(null));
    }

    @Test
    void testSendApprovedDecision() {
        when(umicoClient.sendDecision(any(UmicoDecisionRequest.class), eq(null)))
                .thenReturn(new UmicoDecisionResponse("status", 0));

        umicoService.sendApprovedDecision(getOperationEntity(),
                UUID.fromString("7a18702d-e8dc-471c-8dd5-09c1a1c772b5"));
        verify(umicoClient).sendDecision(any(UmicoDecisionRequest.class), eq(null));
    }
}
