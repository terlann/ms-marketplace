package az.kapitalbank.marketplace.service.impl;

import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessData;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CompleteScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CreateScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerContact;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerDecision;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerNumber;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringVariable;
import az.kapitalbank.marketplace.constants.AdpOptimusLevels;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.exception.models.FeignClientException;
import az.kapitalbank.marketplace.exception.models.ScoringCustomerException;
import az.kapitalbank.marketplace.mappers.ScoringMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static az.kapitalbank.marketplace.property.CustomerConstants.ADDITIONAL_PHONE_NUMBER1;
import static az.kapitalbank.marketplace.property.CustomerConstants.ADDITIONAL_PHONE_NUMBER2;
import static az.kapitalbank.marketplace.property.CustomerConstants.CIF;
import static az.kapitalbank.marketplace.property.CustomerConstants.PHONE_NUMBER;
import static az.kapitalbank.marketplace.property.CustomerConstants.PINCODE;
import static az.kapitalbank.marketplace.property.OrderConstants.LOAN_AMOUNT;
import static az.kapitalbank.marketplace.property.OrderConstants.TRACK_ID;
import static az.kapitalbank.marketplace.property.ScoringConstants.BUSINESS_KEY;
import static az.kapitalbank.marketplace.property.ScoringConstants.PROCESS_KEY;
import static az.kapitalbank.marketplace.property.ScoringConstants.PRODUCT_TYPE;
import static az.kapitalbank.marketplace.property.ScoringConstants.TASK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringServiceImplTest {

    @Mock
    OptimusClient optimusClient;
    @Mock
    ScoringMapper scoringMapper;
    @InjectMocks
    ScoringServiceImpl scoringService;

    @Test
    void whenStartScoringCall_ThenItShouldBeSuccess() {
        StartScoringVariable startScoringVariable = generateStartScoringVariable();
        StartScoringRequest startScoringRequest = generateStartScoringRequest();
        StartScoringResponse startScoringResponse = generateStartScoringResponse();

        when(scoringMapper.toCustomerScoringVariable(PINCODE, PHONE_NUMBER, PRODUCT_TYPE))
                .thenReturn(startScoringVariable);

        when(optimusClient.scoringStart(startScoringRequest)).thenReturn(startScoringResponse);

        Optional<String> result = scoringService.startScoring(TRACK_ID, PINCODE, PHONE_NUMBER);

        assertEquals(BUSINESS_KEY, result.get());
        assertNotNull(startScoringResponse.getBusinessKey());
        verify(scoringMapper).toCustomerScoringVariable(PINCODE, PHONE_NUMBER, PRODUCT_TYPE);
        verify(optimusClient).scoringStart(startScoringRequest);
    }

    @Test
    void whenStartScoringCall_ThenShouldBeEmpty() {
        when(optimusClient.scoringStart(ArgumentMatchers.any())).thenThrow(FeignClientException.class);
        Optional<String> result = scoringService.startScoring(TRACK_ID, PINCODE, PHONE_NUMBER);
        assertEquals(Optional.empty(), result);
    }

    @Test
    void whenCreateScoringCall_ThenShouldBeSuccess() {
        CreateScoringRequest createScoringRequest = generateScoringRequest();
        doNothing().when(optimusClient).scoringCreate(TASK_ID, createScoringRequest);
        scoringService.createScoring(TRACK_ID, TASK_ID, new BigDecimal(LOAN_AMOUNT));
        verify(optimusClient).scoringCreate(TASK_ID, createScoringRequest);
    }

    @Test
    void whenCreateScoringCalling_ThenIsShouldThrowScoringCustomerException() {
        CreateScoringRequest createScoringRequest = generateScoringRequest();

        doThrow(new FeignClientException("Test", "Test")).when(optimusClient)
                .scoringCreate(TASK_ID, createScoringRequest);

        ScoringCustomerException scoringCustomerException = Assertions.assertThrows(ScoringCustomerException.class,
                () -> scoringService.createScoring(TRACK_ID, TASK_ID, new BigDecimal(LOAN_AMOUNT)));

        Assertions.assertEquals("Scoring client throw Exception. track_id - [" + TRACK_ID + "]" +
                        ",Level - [" + AdpOptimusLevels.CREATE + "]",
                scoringCustomerException.getMessage());

        verify(optimusClient).scoringCreate(TASK_ID, createScoringRequest);
    }


    @Test
    void whenCompleteScoringCall_ThenShouldBeSuccess() {
        CompleteScoringRequest completeScoringRequest = generateCompleteScoringRequest();
        doNothing().when(optimusClient).scoringComplete(TASK_ID, completeScoringRequest);
        scoringService.completeScoring(generateCompleteScoring());
        verify(optimusClient).scoringComplete(TASK_ID, completeScoringRequest);
    }

    @Test
    void whenCompleteScoringCall_ThenShouldThrowScoringCustomerException() {
        CompleteScoringRequest completeScoringRequest = generateCompleteScoringRequest();
        doThrow(new FeignClientException("Test", "Test")).when(optimusClient)
                .scoringComplete(TASK_ID, completeScoringRequest);

        ScoringCustomerException scoringCustomerException = Assertions.assertThrows(ScoringCustomerException.class,
                () -> scoringService.completeScoring(generateCompleteScoring()));

        Assertions.assertEquals("Scoring client throw Exception. track_id - [" + TRACK_ID + "]" +
                        ",Level - [" + AdpOptimusLevels.COMPLETE + "]",
                scoringCustomerException.getMessage());

        verify(optimusClient).scoringComplete(TASK_ID, completeScoringRequest);
    }

    @Test
    void whenGetProcessCall_ThenShouldBeSuccess() {
        ProcessResponse processResponse = generateProcessResponse();
        when(optimusClient.getProcess(BUSINESS_KEY)).thenReturn(processResponse);
        ProcessResponse processResponseResult = scoringService.getProcess(TRACK_ID, BUSINESS_KEY).get();
        assertEquals(processResponse.getVariables().getCif(), processResponseResult.getVariables().getCif());
        verify(optimusClient).getProcess(BUSINESS_KEY);
    }

    @Test
    void whenGetProcessCall_ThenShouldBeEmpty() {
        when(optimusClient.getProcess(BUSINESS_KEY)).thenThrow(new FeignClientException("Test", "Test"));
        Optional<ProcessResponse> processResponseResult = scoringService.getProcess(TRACK_ID, BUSINESS_KEY);
        assertEquals(Optional.empty(), processResponseResult);
        verify(optimusClient).getProcess(BUSINESS_KEY);
    }

    public StartScoringVariable generateStartScoringVariable() {
        return StartScoringVariable.builder()
                .pin(PINCODE)
                .phoneNumber(PHONE_NUMBER)
                .scoreCash(true)
                .phoneNumberVerified(true)
                .build();
    }

    public StartScoringRequest generateStartScoringRequest() {
        return StartScoringRequest.builder()
                .processKey(PROCESS_KEY)
                .variables(generateStartScoringVariable())
                .build();
    }

    public StartScoringResponse generateStartScoringResponse() {
        return StartScoringResponse.builder()
                .businessKey(BUSINESS_KEY)
                .build();
    }

    public CreateScoringRequest generateScoringRequest() {
        return CreateScoringRequest.builder()
                .cashDemandedAmount(LOAN_AMOUNT)
                .customerDecision(CustomerDecision.CREATE_CREDIT)
                .build();
    }

    public CompleteScoringRequest generateCompleteScoringRequest() {
        CustomerNumber customerNumber1 = CustomerNumber.builder()
                .number(CustomerNumber.Number.builder()
                        .number(ADDITIONAL_PHONE_NUMBER1)
                        .build())
                .build();

        CustomerNumber customerNumber2 = CustomerNumber.builder()
                .number(CustomerNumber.Number.builder()
                        .number(ADDITIONAL_PHONE_NUMBER2)
                        .build())
                .build();

        List<CustomerNumber> customerNumberList = Arrays.asList(customerNumber1, customerNumber2);

        CustomerContact customerContact = CustomerContact.builder()
                .customerNumberList(customerNumberList)
                .build();

        return CompleteScoringRequest.builder()
                .customerContact(customerContact)
                .customerDecision(CustomerDecision.CONFIRM_CREDIT)
                .build();
    }

    public CompleteScoring generateCompleteScoring() {
        return CompleteScoring.builder()
                .trackId(TRACK_ID)
                .businessKey(BUSINESS_KEY)
                .additionalNumber1(ADDITIONAL_PHONE_NUMBER1)
                .additionalNumber2(ADDITIONAL_PHONE_NUMBER2)
                .build();
    }

    public ProcessResponse generateProcessResponse() {
        return ProcessResponse.builder()
                .taskId(TASK_ID)
                .variables(ProcessData.builder()
                        .cif(CIF)
                        .build())
                .build();
    }

}