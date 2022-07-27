package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.ConstantObject.getOperationEntity;
import static az.kapitalbank.marketplace.constants.TestConstants.BUSINESS_KEY;
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
import feign.FeignException;
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
                .thenThrow(FeignException.class);

        scoringService.startScoring(getOperationEntity());
        verify(optimusClient).scoringStart(any(StartScoringRequest.class));
    }

    @Test
    void getCardId() {
        var processVariableResponse =
                new ProcessVariableResponse("pan", "uid", "0130179", "BUMM123");
        when(optimusClient.getProcessVariable(BUSINESS_KEY.getValue(), null))
                .thenReturn(processVariableResponse);

        scoringService.getProcessVariable(getOperationEntity(), null);
        verify(optimusClient).getProcessVariable(BUSINESS_KEY.getValue(),null);
    }

    @Test
    void getCardId_OptimusClientException() {
        when(optimusClient.getProcessVariable(BUSINESS_KEY.getValue(), null))
                .thenThrow(FeignException.class);
        scoringService.getProcessVariable(getOperationEntity(), null);
        verify(optimusClient).getProcessVariable(BUSINESS_KEY.getValue(), null);
    }

    @Test
    void createScoring_Success() {
        scoringService.createScoring(getOperationEntity());
        verify(optimusClient).scoringCreate(eq(TASK_ID.getValue()),
                any(CreateScoringRequest.class));
    }

    @Test
    void getProcess_Success() {
        when(optimusClient.getProcess(BUSINESS_KEY.getValue())).thenReturn(
                ProcessResponse.builder().build());
        scoringService.getProcess(getOperationEntity());
        verify(optimusClient).getProcess(BUSINESS_KEY.getValue());
    }

    @Test
    void getProcess_OptimusClientException() {
        when(optimusClient.getProcess(BUSINESS_KEY.getValue())).thenThrow(FeignException.class);

        scoringService.getProcess(getOperationEntity());
        verify(optimusClient).getProcess(BUSINESS_KEY.getValue());
    }

    @Test
    void completeScoring_Success() {
        scoringService.completeScoring(getOperationEntity());
        verify(optimusClient).scoringComplete(eq(TASK_ID.getValue()),
                any(CompleteScoringRequest.class));
    }

}
