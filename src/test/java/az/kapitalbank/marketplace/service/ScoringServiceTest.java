package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.TestConstants.RRN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.response.PurchaseResponse;
import az.kapitalbank.marketplace.client.dvs.DvsClient;
import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionResponse;
import az.kapitalbank.marketplace.constant.Currency;
import az.kapitalbank.marketplace.constant.ScoringStatus;
import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.request.TelesalesResultRequestDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.mapper.ScoringMapper;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        var purchaseRequest = PurchaseRequest.builder().rrn(RRN.getValue())
                .amount(orderEntity.getTotalAmount().add(orderEntity.getCommission()))
                .description("fee=" + orderEntity.getCommission())
                .currency(Currency.AZN.getCode()).terminalName(terminalName)
                .uid(operationEntity.getCustomer().getCardId()).build();
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
}
