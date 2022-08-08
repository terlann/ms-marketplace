package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.ConstantObject.getOperationEntity;
import static az.kapitalbank.marketplace.constants.ConstantObject.getProcessResponse;
import static az.kapitalbank.marketplace.constants.TestConstants.BUSINESS_KEY;
import static az.kapitalbank.marketplace.constants.TestConstants.MOBILE_NUMBER;
import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.common.CommonClient;
import az.kapitalbank.marketplace.client.common.model.request.SendSmsRequest;
import az.kapitalbank.marketplace.client.optimus.model.process.CreateCardCreditRequest;
import az.kapitalbank.marketplace.client.optimus.model.process.Offer;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessData;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessVariableResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.SelectedOffer;
import az.kapitalbank.marketplace.config.SmsProperties;
import az.kapitalbank.marketplace.constant.FraudResultStatus;
import az.kapitalbank.marketplace.constant.OptimusProcessStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.messaging.event.BusinessErrorData;
import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.messaging.event.InUserActivityData;
import az.kapitalbank.marketplace.messaging.event.PrePurchaseEvent;
import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
import az.kapitalbank.marketplace.messaging.event.VerificationResultEvent;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanFormalizationServiceTest {

    @Mock
    UmicoService umicoService;
    @Mock
    OrderService orderService;
    @Mock
    ScoringService scoringService;
    @Mock
    CustomerService customerService;
    @Mock
    LeadService leadService;
    @Mock
    VerificationService verificationService;
    @Mock
    OperationRepository operationRepository;
    @Mock
    CommonClient commonClient;
    @Mock
    SmsProperties smsProperties;
    @Mock
    SmsService smsService;
    @InjectMocks
    private LoanFormalizationService loanFormalizationService;

    @Test
    void fraudResultProcess_BlackList() {
        var request = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .fraudResultStatus(FraudResultStatus.BLACKLIST.name())
                .build();
        when(operationRepository.findById(request.getTrackId())).thenReturn(
                Optional.of(getOperationEntity()));
        when(umicoService.sendRejectedDecision(request.getTrackId())).thenReturn(
                UmicoDecisionStatus.REJECTED);
        loanFormalizationService.fraudResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void fraudResultProcess_NotFoundOperation() {
        var request = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .fraudResultStatus(FraudResultStatus.BLACKLIST.name())
                .build();
        when(operationRepository.findById(request.getTrackId())).thenReturn(Optional.empty());
        loanFormalizationService.fraudResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void fraudResultProcess_SuspiciousSendTelesales() {
        var request = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .fraudResultStatus(FraudResultStatus.SUSPICIOUS_TELESALES.name())
                .build();
        when(operationRepository.findById(request.getTrackId())).thenReturn(
                Optional.of(getOperationEntity()));
        loanFormalizationService.fraudResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void fraudResultProcess_SuspiciousSendUmico() {
        var request = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .fraudResultStatus(FraudResultStatus.SUSPICIOUS_UMICO.name())
                .build();
        when(operationRepository.findById(request.getTrackId())).thenReturn(
                Optional.of(getOperationEntity()));
        loanFormalizationService.fraudResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void fraudResultProcess_NoFraud_NoBusinessKey() {
        var request = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .build();
        when(operationRepository.findById(request.getTrackId())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.startScoring(any(OperationEntity.class))).thenReturn(Optional.empty());
        loanFormalizationService.fraudResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void fraudResultProcess_NoFraud() {
        var request = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .build();
        when(operationRepository.findById(request.getTrackId())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.startScoring(any(OperationEntity.class))).thenReturn(
                Optional.of("asdf"));
        loanFormalizationService.fraudResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void scoringResultProcess_InUserActivity_UserTaskSignDocuments() {
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey("USER_TASK_SIGN_DOCUMENTS").build();
        var request = ScoringResultEvent.builder()
                .processStatus(OptimusProcessStatus.IN_USER_ACTIVITY)
                .data(inUserActivityData)
                .businessKey(BUSINESS_KEY.getValue()).build();
        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .mobileNumber(MOBILE_NUMBER.getValue())
                .build();
        var sendSmsRequest = SendSmsRequest.builder()
                .body(any())
                .phoneNumber(operationEntity.getMobileNumber())
                .build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.getProcess(any(OperationEntity.class))).thenReturn(
                Optional.of(getProcessResponse()));
        when(verificationService.getDvsUrl(getOperationEntity().getId(),
                getOperationEntity().getDvsOrderId())).thenReturn(Optional.of("Https//dvs.com"));
        loanFormalizationService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_GetProcessFail() {
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey("USER_TASK_SIGN_DOCUMENTS").build();
        var request = ScoringResultEvent.builder()
                .processStatus(OptimusProcessStatus.IN_USER_ACTIVITY)
                .data(inUserActivityData)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.getProcess(any(OperationEntity.class))).thenReturn(
                Optional.empty());
        loanFormalizationService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }


    @Test
    void scoringResultProcess_InUserActivity_UserTaskSignDocuments_NoDvsUrl() {
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey("USER_TASK_SIGN_DOCUMENTS").build();
        var request = ScoringResultEvent.builder()
                .processStatus(OptimusProcessStatus.IN_USER_ACTIVITY)
                .data(inUserActivityData)
                .businessKey(BUSINESS_KEY.getValue()).build();
        var processResponse = ProcessResponse.builder()
                .variables(ProcessData.builder()
                        .createCardCreditRequest(CreateCardCreditRequest.builder()
                                .startDate(LocalDate.parse("2020-02-02"))
                                .endDate(LocalDate.parse("2020-02-02"))
                                .build())
                        .selectedOffer(SelectedOffer.builder()
                                .cardOffer(Offer.builder()
                                        .availableLoanAmount(BigDecimal.ONE)
                                        .build())
                                .build())
                        .build())
                .build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.getProcess(any(OperationEntity.class))).thenReturn(
                Optional.of(processResponse));
        loanFormalizationService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_InUserActivity_UserTaskScoring() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("RULE_HAS_WRITTEN_OF_CREDIT").build()};
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey("USER_TASK_SCORING").build();
        var request = ScoringResultEvent.builder()
                .processStatus(OptimusProcessStatus.IN_USER_ACTIVITY)
                .data(inUserActivityData)
                .businessKey(BUSINESS_KEY.getValue()).build();
        var processResponse = ProcessResponse.builder()
                .variables(ProcessData.builder()
                        .selectedOffer(SelectedOffer.builder()
                                .cardOffer(Offer.builder()
                                        .availableLoanAmount(BigDecimal.ONE)
                                        .build())
                                .build())
                        .build())
                .build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.getProcess(any(OperationEntity.class))).thenReturn(
                Optional.of(processResponse));
        loanFormalizationService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_InUserActivity_UserTaskScoring_ScoringAmount_Is_Zero() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("RULE_HAS_WRITTEN_OF_CREDIT").build()};
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey("USER_TASK_SCORING").build();
        var request = ScoringResultEvent.builder()
                .processStatus(OptimusProcessStatus.IN_USER_ACTIVITY)
                .data(inUserActivityData)
                .businessKey(BUSINESS_KEY.getValue()).build();
        var processResponse = ProcessResponse.builder()
                .variables(ProcessData.builder()
                        .selectedOffer(SelectedOffer.builder()
                                .cardOffer(Offer.builder()
                                        .availableLoanAmount(BigDecimal.ZERO)
                                        .build())
                                .build())
                        .build())
                .build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.getProcess(any(OperationEntity.class))).thenReturn(
                Optional.of(processResponse));
        loanFormalizationService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_Completed() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("RULE_HAS_WRITTEN_OF_CREDIT").build()};
        var processVariableResponse =
                new ProcessVariableResponse("pan", "uid", "0130179", "BUMM123");
        var request = ScoringResultEvent.builder()
                .processStatus(OptimusProcessStatus.COMPLETED)
                .data(arr)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.getProcessVariable(any(OperationEntity.class), eq(null))).thenReturn(
                Optional.of(processVariableResponse));
        when(orderService.prePurchaseOrders(any(OperationEntity.class),
                eq(processVariableResponse.getUid()))).thenReturn(BigDecimal.ZERO);
        loanFormalizationService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_Completed_NoCardId() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("RULE_HAS_WRITTEN_OF_CREDIT").build()};
        var processVariableResponse = ProcessVariableResponse.builder().build();
        var request = ScoringResultEvent.builder()
                .processStatus(OptimusProcessStatus.COMPLETED)
                .data(arr)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.getProcessVariable(any(OperationEntity.class), eq(null))).thenReturn(
                Optional.of(processVariableResponse));
        when(orderService.prePurchaseOrders(any(OperationEntity.class),
                eq(processVariableResponse.getUid()))).thenReturn(BigDecimal.ONE);
        loanFormalizationService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_BusinessError() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("RULE_HAS_WRITTEN_OF_CREDIT").build()};
        var request = ScoringResultEvent.builder()
                .processStatus(OptimusProcessStatus.BUSINESS_ERROR)
                .data(arr)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));

        loanFormalizationService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_BusinessError_noData() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("test").build()};
        var request = ScoringResultEvent.builder()
                .processStatus(OptimusProcessStatus.BUSINESS_ERROR)
                .data(arr)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));

        loanFormalizationService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_Incident() {
        var request = ScoringResultEvent.builder()
                .processStatus(OptimusProcessStatus.INCIDENT_HAPPENED)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));

        loanFormalizationService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void verificationResultProcess_Confirmed_ScoringCompleteError() {
        verificationResultProcess("confirmed");
    }

    @Test
    void verificationResultProcess_Pending() {
        verificationResultProcess("pending");
    }

    @Test
    void verificationResultProcess_Rejected() {
        verificationResultProcess("rejected");
    }

    private void verificationResultProcess(String status) {
        var request = VerificationResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .status(status)
                .build();

        when(operationRepository.findById(request.getTrackId())).thenReturn(
                Optional.of(getOperationEntity()));

        loanFormalizationService.verificationResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void prePurchaseProcess() {
        var operationEntity = getOperationEntity();
        when(operationRepository.findById(operationEntity.getId())).thenReturn(
                Optional.of(getOperationEntity()));
        when(orderService.prePurchaseOrders(any(OperationEntity.class),
                eq(operationEntity.getCustomer().getCardId()))).thenReturn(BigDecimal.ZERO);
        loanFormalizationService.prePurchaseProcess(new PrePurchaseEvent(operationEntity.getId()));
        verify(operationRepository).findById(operationEntity.getId());
    }

    @Test
    void scoringResultProcess_InUserActivity_UserTaskSignDocuments_Exception() {
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey("USER_TASK_SIGN_DOCUMENTS").build();
        var request = ScoringResultEvent.builder()
                .processStatus(OptimusProcessStatus.IN_USER_ACTIVITY)
                .data(inUserActivityData)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.getProcess(any(OperationEntity.class))).thenReturn(
                Optional.of(getProcessResponse()));
        when(verificationService.getDvsUrl(getOperationEntity().getId(),
                getOperationEntity().getDvsOrderId())).thenReturn(Optional.of("Https//dvs.com"));
        loanFormalizationService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }
}
