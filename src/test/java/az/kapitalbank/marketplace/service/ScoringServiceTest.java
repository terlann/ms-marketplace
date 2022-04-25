package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.ConstantObject.getOperationEntity;
import static az.kapitalbank.marketplace.constants.TestConstants.TASK_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessVariableResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CompleteScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CreateScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringResponse;
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

        scoringService.startScoring(getOperationEntity());
        verify(optimusClient).scoringStart(any(StartScoringRequest.class));
    }

    @Test
    void startScoring_OptimusClientException() {
        when(optimusClient.scoringStart(any(StartScoringRequest.class)))
                .thenThrow(new RuntimeException(""));

        scoringService.startScoring(getOperationEntity());
        verify(optimusClient).scoringStart(any(StartScoringRequest.class));
    }

    @Test
    void getCardId() {
        var processVariableResponse = new ProcessVariableResponse("pan", "uid");
        when(optimusClient.getProcessVariable("businessKey", "uid"))
                .thenReturn(processVariableResponse);

        scoringService.getCardId(getOperationEntity(), "uid");
        verify(optimusClient).getProcessVariable("businessKey", "uid");
    }

    @Test
    void getCardId_OptimusClientException() {
        when(optimusClient.getProcessVariable("businessKey", "uid")).thenThrow(
                new RuntimeException(""));
        scoringService.getCardId(getOperationEntity(), "uid");
        verify(optimusClient).getProcessVariable("businessKey", "uid");
    }

    @Test
    void createScoring_Success() {
        scoringService.createScoring(getOperationEntity());
        verify(optimusClient).scoringCreate(eq(TASK_ID.getValue()),
                any(CreateScoringRequest.class));
    }

    @Test
    void getProcess_Success() {
        when(optimusClient.getProcess("businessKey")).thenReturn(ProcessResponse.builder().build());
        scoringService.getProcess(getOperationEntity());
        verify(optimusClient).getProcess("businessKey");
    }

    @Test
    void getProcess_OptimusClientException() {

        when(optimusClient.getProcess("businessKey"))
                .thenThrow(new RuntimeException(""));

        scoringService.getProcess(getOperationEntity());
        verify(optimusClient).getProcess("businessKey");
    }

    @Test
    void completeScoring_Success() {
        scoringService.completeScoring(getOperationEntity());
        verify(optimusClient).scoringComplete(eq(TASK_ID.getValue()),
                any(CompleteScoringRequest.class));
    }

}
