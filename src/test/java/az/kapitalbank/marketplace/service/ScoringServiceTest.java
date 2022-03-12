package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.TestConstants.BUSINESS_KEY;
import static az.kapitalbank.marketplace.constants.TestConstants.RRN;
import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.response.PurchaseResponse;
import az.kapitalbank.marketplace.client.dvs.DvsClient;
import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;
import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.model.process.CreateCardCreditRequest;
import az.kapitalbank.marketplace.client.optimus.model.process.Offer;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessData;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessVariableResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.SelectedOffer;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringVariable;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionResponse;
import az.kapitalbank.marketplace.constant.FraudResultStatus;
import az.kapitalbank.marketplace.constant.FraudType;
import az.kapitalbank.marketplace.constant.ProcessStatus;
import az.kapitalbank.marketplace.constant.TaskDefinitionKey;
import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.LeadDto;
import az.kapitalbank.marketplace.dto.request.TelesalesResultRequestDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.mapper.ScoringMapper;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.messaging.event.InUserActivityData;
import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {
    @Mock
    DvsClient dvsClient;
    @Mock
    UmicoClient umicoClient;
    @Mock
    AtlasClient atlasClient;
    @Mock
    OptimusClient optimusClient;
    @Mock
    ScoringMapper scoringMapper;
    @Mock
    TelesalesMapper telesalesMapper;
    @Mock
    TelesalesService telesalesService;
    @Mock
    CustomerRepository customerRepository;
    @Mock
    OperationRepository operationRepository;
    @InjectMocks
    ScoringService scoringService;

    @Value("${umico.api-key}")
    String apiKey;
    @Value("${optimus.process.key}")
    String processKey;
    @Value("${optimus.process.product-type}")
    String productType;
    @Value("${purchase.terminal-name}")
    String terminalName;

    @Test
    void telesalesResult_Success() {
        var telesalesResultRequestDto = TelesalesResultRequestDto.builder()
                .telesalesOrderId("198d9ce8-a126-11ec-b909-0242ac120002")
                .build();
        var customerEntity = CustomerEntity.builder()
                .cardId("31e93364-a127-11ec-b909-0242ac120002")
                .completeProcessDate(LocalDateTime.now())
                .build();
        var orderEntity = OrderEntity.builder()
                .rrn(RRN.getValue())
                .totalAmount(BigDecimal.valueOf(900))
                .commission(BigDecimal.valueOf(20))
                .build();
        var operationEntity = OperationEntity.builder()
                .umicoDecisionStatus(UmicoDecisionStatus.APPROVED)
                .loanContractStartDate(LocalDate.now())
                .loanContractEndDate(LocalDate.now())
                .customer(customerEntity)
                .orders(List.of(orderEntity))
                .commission(BigDecimal.valueOf(20))
                .totalAmount(BigDecimal.valueOf(900))
                .build();
        var orderEntities = new ArrayList<OrderEntity>();

        var purchaseResponse = PurchaseResponse.builder()
                .id("31e93364-a127-11ec-b909-0242ac120002")
                .approvalCode("123456789")
                .build();
        orderEntity.setRrn(RRN.getValue());
        orderEntity.setTransactionId(purchaseResponse.getId());
        orderEntity.setApprovalCode(purchaseResponse.getApprovalCode());
        orderEntity.setTransactionStatus(TransactionStatus.PURCHASE);
        orderEntities.add(orderEntity);
        operationEntity.setOrders(orderEntities);

        var umicoScoringDecisionRequest =
                UmicoDecisionRequest.builder().trackId(operationEntity.getId())
                        .decisionStatus(UmicoDecisionStatus.REJECTED)
                        .loanContractStartDate(operationEntity.getLoanContractStartDate())
                        .loanContractEndDate(operationEntity.getLoanContractEndDate())
                        .customerId(operationEntity.getCustomer().getId())
                        .commission(operationEntity.getCommission())
                        .loanLimit(operationEntity.getTotalAmount()
                                .add(operationEntity.getCommission()))
                        .loanTerm(operationEntity.getLoanTerm()).build();
        var umicoScoringDecisionResponse = UmicoDecisionResponse.builder()
                .httpStatus(5)
                .status("success")
                .build();


        when(umicoClient.sendDecisionToUmico(umicoScoringDecisionRequest, apiKey))
                .thenReturn(umicoScoringDecisionResponse);
        when(operationRepository.findByTelesalesOrderId(telesalesResultRequestDto
                .getTelesalesOrderId())).thenReturn(Optional.of(operationEntity));
        when(atlasClient.purchase(any(PurchaseRequest.class))).thenReturn(purchaseResponse);
        when(operationRepository.save(any(OperationEntity.class))).thenReturn(operationEntity);

        scoringService.telesalesResult(telesalesResultRequestDto);

        verify(operationRepository).save(any(OperationEntity.class));

    }


    @Test
    void scoringResultProcess_InUserActivity_USER_TASK_SCORING() {
        String businessKey = "asdkljasdl";
        var operationEntity = OperationEntity.builder()
                .taskId("123")
                .totalAmount(BigDecimal.ONE)
                .commission(BigDecimal.ONE)
                .businessKey("asdkljasdl").build();
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey(TaskDefinitionKey.USER_TASK_SCORING.name())
                .build();
        var scoringResultEvent = ScoringResultEvent.builder()
                .businessKey("asdkljasdl")
                .processStatus(ProcessStatus.IN_USER_ACTIVITY)
                .data(inUserActivityData)
                .build();
        var processResponse = ProcessResponse.builder()
                .variables(ProcessData.builder()
                        .selectedOffer(SelectedOffer.builder()
                                .cardOffer(Offer.builder()
                                        .availableLoanAmount(BigDecimal.valueOf(1000))
                                        .build())
                                .build())
                        .build())
                .build();

        when(operationRepository.findByBusinessKey(businessKey))
                .thenReturn(Optional.of(operationEntity));
        when(optimusClient.getProcess(businessKey)).thenReturn(processResponse);
        scoringService.scoringResultProcess(scoringResultEvent);

        verify(optimusClient).getProcess(businessKey);
    }

    @Test
    void scoringResultProcess_InUserActivity_USER_TASK_SIGN_DOCUMENTS() {
        String businessKey = "asdkljasdl";
        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .taskId("123")
                .totalAmount(BigDecimal.ONE)
                .commission(BigDecimal.ONE)
                .businessKey("asdkljasdl").build();
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey(TaskDefinitionKey.USER_TASK_SIGN_DOCUMENTS.name())
                .build();
        var scoringResultEvent = ScoringResultEvent.builder()
                .businessKey("asdkljasdl")
                .processStatus(ProcessStatus.IN_USER_ACTIVITY)
                .data(inUserActivityData)
                .build();
        var processResponse = ProcessResponse.builder()
                .variables(ProcessData.builder()
                        .createCardCreditRequest(CreateCardCreditRequest.builder().build())
                        .build())
                .build();

        when(operationRepository.findByBusinessKey(businessKey))
                .thenReturn(Optional.of(operationEntity));
        when(optimusClient.getProcess(businessKey)).thenReturn(processResponse);
        when(dvsClient.getDetails(UUID.fromString(TRACK_ID.getValue()), null))
                .thenReturn(DvsGetDetailsResponse.builder().build());
        when(operationRepository.findById(UUID.fromString(TRACK_ID.getValue())))
                .thenReturn(Optional.of(operationEntity));

        scoringService.scoringResultProcess(scoringResultEvent);

        verify(optimusClient).getProcess(businessKey);
    }

    @Test
    void scoringResultProcess_COMPLETED() {
        String businessKey = "asdkljasdl";
        var operationEntity = OperationEntity.builder()
                .taskId("123")
                .totalAmount(BigDecimal.ONE)
                .commission(BigDecimal.ONE)
                .customer(CustomerEntity.builder().build())
                .businessKey("asdkljasdl").build();
        var inUserActivityData = InUserActivityData.builder()
                .taskDefinitionKey("USER_TASK_SCORING")
                .build();
        var scoringResultEvent = ScoringResultEvent.builder()
                .businessKey("asdkljasdl")
                .processStatus(ProcessStatus.COMPLETED)
                .data(inUserActivityData)
                .build();

        var processVariableResponse = ProcessVariableResponse.builder()
                .build();


        when(operationRepository.findByBusinessKey(businessKey))
                .thenReturn(Optional.of(operationEntity));
        when(optimusClient.getProcessVariable(operationEntity.getBusinessKey(),
                "pan")).thenReturn(processVariableResponse);

        scoringService.scoringResultProcess(scoringResultEvent);
        verify(optimusClient).getProcessVariable(operationEntity.getBusinessKey(), "pan");
    }


    @Test
    void fraudResultProcess_Blacklist() {
        var fraudCheckResultEvent = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .fraudResultStatus(FraudResultStatus.BLACKLIST)
                .build();
        var trackId = fraudCheckResultEvent.getTrackId();
        var fraudResultStatus = fraudCheckResultEvent.getFraudResultStatus();

        var operationEntity = OperationEntity.builder()
                .umicoDecisionStatus(UmicoDecisionStatus.APPROVED)
                .loanContractStartDate(LocalDate.now())
                .loanContractEndDate(LocalDate.now())
                .commission(BigDecimal.valueOf(20))
                .totalAmount(BigDecimal.valueOf(900))
                .build();
        operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.DECLINED_BY_BLACKLIST);

        UmicoDecisionRequest umicoScoringDecisionRequest = UmicoDecisionRequest.builder()
                .trackId(trackId)
                .decisionStatus(UmicoDecisionStatus.DECLINED_BY_BLACKLIST)
                .loanTerm(operationEntity.getLoanTerm())
                .build();
        var umicoScoringDecisionResponse = UmicoDecisionResponse.builder()
                .httpStatus(5)
                .status("success")
                .build();


        when(operationRepository.findById(trackId)).thenReturn(Optional.of(operationEntity));
        when(umicoClient.sendDecisionToUmico(umicoScoringDecisionRequest, apiKey))
                .thenReturn(umicoScoringDecisionResponse);

        scoringService.fraudResultProcess(fraudCheckResultEvent);
        verify(umicoClient).sendDecisionToUmico(umicoScoringDecisionRequest, apiKey);
    }

    @Test
    void fraudResultProcess_Suspicious() {
        var fraudCheckResultEvent = FraudCheckResultEvent.builder()
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .fraudResultStatus(FraudResultStatus.SUSPICIOUS)
                .build();
        var leadDto = LeadDto.builder()
                .fraudResultStatus(FraudResultStatus.SUSPICIOUS)
                .trackId(UUID.fromString(TRACK_ID.getValue()))
                .types(List.of(FraudType.UMICO_USER_ID))
                .build();
        String telesalesOrderId = "eb813fa4-a142-11ec-b909-0242ac120002";
        var operationEntityOptional = OperationEntity.builder()
                .umicoDecisionStatus(UmicoDecisionStatus.APPROVED)
                .loanContractStartDate(LocalDate.now())
                .loanContractEndDate(LocalDate.now())
                .commission(BigDecimal.valueOf(20))
                .totalAmount(BigDecimal.valueOf(900))
                .build();
        operationEntityOptional.setTelesalesOrderId(telesalesOrderId);


        when(telesalesMapper.toLeadDto(fraudCheckResultEvent)).thenReturn(leadDto);
        when(telesalesService.sendLead(leadDto)).thenReturn(Optional.of(telesalesOrderId));
        when(operationRepository.findById(UUID.fromString(TRACK_ID.getValue()))).thenReturn(
                Optional.ofNullable(operationEntityOptional));

        scoringService.fraudResultProcess(fraudCheckResultEvent);
        verify(telesalesService).sendLead(leadDto);
    }

    @Test
    void fraudResultProcess_NoFraudDetectedBehavior() {
        var fraudCheckResultEvent = FraudCheckResultEvent.builder()
                .trackId(UUID.randomUUID())
                .build();
        var operationEntity = OperationEntity.builder()
                .umicoDecisionStatus(UmicoDecisionStatus.APPROVED)
                .loanContractStartDate(LocalDate.now())
                .loanContractEndDate(LocalDate.now())
                .commission(BigDecimal.valueOf(20))
                .totalAmount(BigDecimal.valueOf(900))
                .pin("5JR9R11")
                .mobileNumber("+994559996655")
                .businessKey(BUSINESS_KEY.getValue())
                .build();
        var startScoringVariable = StartScoringVariable.builder()
                .build();
        var startScoringRequest =
                StartScoringRequest.builder().build();
        var startScoringResponse = StartScoringResponse.builder()
                .businessKey(BUSINESS_KEY.getValue())
                .build();


        when(operationRepository.findById(fraudCheckResultEvent.getTrackId()))
                .thenReturn(Optional.of(operationEntity));
        when(optimusClient.scoringStart(startScoringRequest))
                .thenReturn(startScoringResponse);

        scoringService.fraudResultProcess(fraudCheckResultEvent);

        verify(optimusClient).scoringStart(startScoringRequest);


    }
}
