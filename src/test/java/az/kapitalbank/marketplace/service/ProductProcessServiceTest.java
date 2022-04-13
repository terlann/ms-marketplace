package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.ConstantObject.getOperationEntity;
import static az.kapitalbank.marketplace.constants.TestConstants.BUSINESS_KEY;
import static az.kapitalbank.marketplace.constants.TestConstants.CARD_UID;
import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.optimus.model.process.CreateCardCreditRequest;
import az.kapitalbank.marketplace.client.optimus.model.process.Offer;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessData;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.SelectedOffer;
import az.kapitalbank.marketplace.constant.FraudResultStatus;
import az.kapitalbank.marketplace.constant.ProcessStatus;
import az.kapitalbank.marketplace.messaging.event.BusinessErrorData;
import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.messaging.event.InUserActivityData;
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
class ProductProcessServiceTest {

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
    @InjectMocks
    private ProductProcessService productProcessService;

    @Test
    void fraudResultProcess_BlackList() {
        var request = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .fraudResultStatus(FraudResultStatus.BLACKLIST)
                .build();
        when(operationRepository.findById(request.getTrackId())).thenReturn(
                Optional.of(getOperationEntity()));
        when(umicoService.sendRejectedDecision(request.getTrackId())).thenReturn(Optional.empty());
        productProcessService.fraudResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void fraudResultProcess_SuspiciousSendTelesales() {
        var request = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .fraudResultStatus(FraudResultStatus.SUSPICIOUS_TELESALES)
                .build();
        when(operationRepository.findById(request.getTrackId())).thenReturn(
                Optional.of(getOperationEntity()));
        productProcessService.fraudResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void fraudResultProcess_SuspiciousSendUmico() {
        var request = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .fraudResultStatus(FraudResultStatus.SUSPICIOUS_UMICO)
                .build();
        when(operationRepository.findById(request.getTrackId())).thenReturn(
                Optional.of(getOperationEntity()));
        productProcessService.fraudResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void fraudResultProcess_NoFraud_NoBusinessKey() {
        var request = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .build();
        when(operationRepository.findById(request.getTrackId())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.startScoring(getOperationEntity().getId(),
                getOperationEntity().getPin(),
                getOperationEntity().getMobileNumber())).thenReturn(Optional.empty());
        productProcessService.fraudResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void fraudResultProcess_NoFraud() {
        var request = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .build();
        when(operationRepository.findById(request.getTrackId())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.startScoring(getOperationEntity().getId(),
                getOperationEntity().getPin(),
                getOperationEntity().getMobileNumber())).thenReturn(Optional.of("asdf"));
        productProcessService.fraudResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }

    @Test
    void scoringResultProcess_InUserActivity_UserTaskSignDocuments() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("RULE_HAS_WRITTEN_OF_CREDIT").build()};
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey("USER_TASK_SIGN_DOCUMENTS").build();
        var request = ScoringResultEvent.builder()
                .processStatus(ProcessStatus.IN_USER_ACTIVITY)
                .data(inUserActivityData)
                .businessKey(BUSINESS_KEY.getValue()).build();
        var processResponse = ProcessResponse.builder()
                .variables(ProcessData.builder()
                        .dvsOrderId(12345L)
                        .createCardCreditRequest(CreateCardCreditRequest.builder()
                                .startDate(LocalDate.parse("2020-02-02"))
                                .endDate(LocalDate.parse("2020-02-02"))
                                .build())
                        .selectedOffer(SelectedOffer.builder()
                                .cardOffer(Offer.builder()
                                        .availableLoanAmount(BigDecimal.ONE)
                                        .build())
                                .build()).build()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.getProcess(getOperationEntity().getBusinessKey())).thenReturn(
                Optional.of(processResponse));
        when(verificationService.getDvsUrl(getOperationEntity().getId(),
                getOperationEntity().getDvsOrderId())).thenReturn(Optional.of("Https//dvs.com"));

        productProcessService.scoringResultProcess(request);

        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_InUserActivity_UserTaskSignDocuments_NoDvsUrl() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("RULE_HAS_WRITTEN_OF_CREDIT").build()};
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey("USER_TASK_SIGN_DOCUMENTS").build();
        var request = ScoringResultEvent.builder()
                .processStatus(ProcessStatus.IN_USER_ACTIVITY)
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
        when(scoringService.getProcess(getOperationEntity().getBusinessKey())).thenReturn(
                Optional.of(processResponse));
        productProcessService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_InUserActivity_UserTaskScoring() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("RULE_HAS_WRITTEN_OF_CREDIT").build()};
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey("USER_TASK_SCORING").build();
        var request = ScoringResultEvent.builder()
                .processStatus(ProcessStatus.IN_USER_ACTIVITY)
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
        when(scoringService.getProcess(getOperationEntity().getBusinessKey())).thenReturn(
                Optional.of(processResponse));
        productProcessService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_Completed() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("RULE_HAS_WRITTEN_OF_CREDIT").build()};
        var request = ScoringResultEvent.builder()
                .processStatus(ProcessStatus.COMPLETED)
                .data(arr)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.getCardId(getOperationEntity().getBusinessKey(), "uid")).thenReturn(
                Optional.of(CARD_UID.getValue()));
        productProcessService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_Completed_NoCardId() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("RULE_HAS_WRITTEN_OF_CREDIT").build()};
        var request = ScoringResultEvent.builder()
                .processStatus(ProcessStatus.COMPLETED)
                .data(arr)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));
        when(scoringService.getCardId(getOperationEntity().getBusinessKey(), "uid")).thenReturn(
                Optional.empty());
        productProcessService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_BusinessError() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("RULE_HAS_WRITTEN_OF_CREDIT").build()};
        var request = ScoringResultEvent.builder()
                .processStatus(ProcessStatus.BUSINESS_ERROR)
                .data(arr)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));

        productProcessService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_BusinessError_noData() {
        BusinessErrorData[] arr = new BusinessErrorData[] {
                BusinessErrorData.builder().id("test").build()};
        var request = ScoringResultEvent.builder()
                .processStatus(ProcessStatus.BUSINESS_ERROR)
                .data(arr)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));

        productProcessService.scoringResultProcess(request);
        verify(operationRepository).findByBusinessKey(request.getBusinessKey());
    }

    @Test
    void scoringResultProcess_Incident() {
        var request = ScoringResultEvent.builder()
                .processStatus(ProcessStatus.INCIDENT_HAPPENED)
                .businessKey(BUSINESS_KEY.getValue()).build();
        when(operationRepository.findByBusinessKey(request.getBusinessKey())).thenReturn(
                Optional.of(getOperationEntity()));

        productProcessService.scoringResultProcess(request);
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

        productProcessService.verificationResultProcess(request);
        verify(operationRepository).findById(request.getTrackId());
    }
}
