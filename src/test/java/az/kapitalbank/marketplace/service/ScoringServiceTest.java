package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.ConstantObject.getOperationEntity;
import static az.kapitalbank.marketplace.constants.TestConstants.BUSINESS_KEY;
import static az.kapitalbank.marketplace.constants.TestConstants.MOBILE_NUMBER;
import static az.kapitalbank.marketplace.constants.TestConstants.PIN;
import static az.kapitalbank.marketplace.constants.TestConstants.TASK_ID;
import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.exception.OptimusClientException;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessVariableResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CompleteScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CreateScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock
    private OptimusClient optimusClient;
    @InjectMocks
    private ScoringService scoringService;


    @Test
    void startScoring_Success() {
        when(optimusClient.scoringStart(any(StartScoringRequest.class)))
                .thenReturn(new StartScoringResponse("businessKey"));

        scoringService.startScoring(UUID.fromString(TRACK_ID.getValue()), PIN.getValue(),
                MOBILE_NUMBER.getValue());
        verify(optimusClient).scoringStart(any(StartScoringRequest.class));
    }

    @Test
    void startScoring_OptimusClientException() {
        when(optimusClient.scoringStart(any(StartScoringRequest.class)))
                .thenThrow(new OptimusClientException("", ""));

        scoringService.startScoring(UUID.fromString(TRACK_ID.getValue()), PIN.getValue(),
                MOBILE_NUMBER.getValue());
        verify(optimusClient).scoringStart(any(StartScoringRequest.class));
    }

    @Test
    void getCardId() {
        var processVariableResponse = new ProcessVariableResponse("pan", "uid");
        when(optimusClient.getProcessVariable("businessKey", "uid"))
                .thenReturn(processVariableResponse);

        scoringService.getCardId("businessKey", "uid");
        verify(optimusClient).getProcessVariable("businessKey", "uid");
    }

    @Test
    void getCardId_OptimusClientException() {
        when(optimusClient.getProcessVariable("businessKey", "uid")).thenThrow(
                new OptimusClientException("", ""));
        scoringService.getCardId("businessKey", "uid");
        verify(optimusClient).getProcessVariable("businessKey", "uid");
    }

    @Test
    void createScoring_Success() {
        scoringService.createScoring(UUID.fromString(TRACK_ID.getValue()), TASK_ID.getValue(),
                BigDecimal.ONE);
        verify(optimusClient).scoringCreate(eq(TASK_ID.getValue()),
                any(CreateScoringRequest.class));
    }

    @Test
    void getProcess_Success() {
        when(optimusClient.getProcess("businessKey")).thenReturn(ProcessResponse.builder().build());
        scoringService.getProcess("businessKey");
        verify(optimusClient).getProcess("businessKey");
    }

    @Test
    void getProcess_OptimusClientException() {

        when(optimusClient.getProcess("businessKey"))
                .thenThrow(new OptimusClientException("", ""));

        scoringService.getProcess("businessKey");
        verify(optimusClient).getProcess("businessKey");
    }

    @Test
    void completeScoring_Success() {
        scoringService.completeScoring(TASK_ID.getValue(), BUSINESS_KEY.getValue(), "a1", "a2");
        verify(optimusClient).scoringComplete(eq(TASK_ID.getValue()),
                any(CompleteScoringRequest.class));
    }
    @Test
    void deleteLoan_Success() {
        scoringService.deleteLoan(getOperationEntity());
        verify(optimusClient).deleteLoan(getOperationEntity().getBusinessKey());
    }
}
