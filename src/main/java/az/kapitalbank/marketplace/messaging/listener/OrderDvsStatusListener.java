package az.kapitalbank.marketplace.messaging.listener;

import java.util.Objects;
import java.util.function.Consumer;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionRequest;
import az.kapitalbank.marketplace.constants.ScoringStatus;
import az.kapitalbank.marketplace.constants.TransactionStatus;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.messaging.event.OrderDvsStatusEvent;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.service.ScoringService;
import az.kapitalbank.marketplace.utils.GenerateUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderDvsStatusListener {

    AtlasClient atlasClient;
    ObjectMapper objectMapper;
    ScoringService scoringService;
    OperationRepository operationRepository;
    OptimusClient optimusClient;
    CustomerRepository customerRepository;
    UmicoClient umicoClient;

    @NonFinal
    @Value("${purchase.terminal-name}")
    String terminalName;

    @NonFinal
    @Value("${umico.api-key}")
    String apiKey;

    @Bean
    public Consumer<String> orderDvsStatus() {
        return message -> {
            if (Objects.nonNull(message)) {
                try {
                    OrderDvsStatusEvent orderDvsStatusEvent = objectMapper
                            .readValue(message, OrderDvsStatusEvent.class);
                    log.info("order dvs status consumer. Message - [{}]", orderDvsStatusEvent);
                    if (orderDvsStatusEvent != null) {
                        var operationEntityOptional = operationRepository.findByDvsOrderId(
                                orderDvsStatusEvent.getOrderId());
                        if (operationEntityOptional.isPresent()) {
                            var operationEntity = operationEntityOptional.get();
                            var customerEntity = operationEntity.getCustomer();
                            var completeScoring = CompleteScoring.builder()
                                    .trackId(operationEntity.getId())
                                    .businessKey(operationEntity.getBusinessKey())
                                    .additionalNumber1(customerEntity.getAdditionalPhoneNumber1())
                                    .additionalNumber2(customerEntity.getAdditionalPhoneNumber2())
                                    .build();
                            scoringService.completeScoring(completeScoring);

                            operationEntity.setDvsOrderStatus(orderDvsStatusEvent.getStatus());
                            operationRepository.saveAndFlush(operationEntity);
                            var orders = operationEntity.getOrders();
                            var cardPan = optimusClient.getProcessVariable(operationEntity.getBusinessKey(), "pan");
                            var cardUid = atlasClient.findByPan(cardPan).getUid();
                            customerEntity.setCardUUID(cardUid);
                            customerRepository.saveAndFlush(customerEntity);
                            for (OrderEntity orderEntity : orders) {
                                var rrn = GenerateUtil.rrn();
                                var purchaseRequest = PurchaseRequest.builder()
                                        .rrn(rrn)
                                        .amount(orderEntity.getTotalAmount())
                                        .description("purchase") //TODO text ?
                                        .currency(944)
                                        .terminalName(terminalName)
                                        .uid(cardUid)
                                        .build();
                                var purchaseResponse = atlasClient.purchase(purchaseRequest);
                                orderEntity.setRrn(rrn);
                                orderEntity.setTransactionId(purchaseResponse.getId());
                                orderEntity.setApprovalCode(purchaseResponse.getApprovalCode());
                                orderEntity.setTransactionStatus(TransactionStatus.PURCHASE);
                            }
                            var umicoScoringDecisionRequest = UmicoScoringDecisionRequest.builder()
                                    .trackId(operationEntity.getId())
                                    .scoringStatus(ScoringStatus.APPROVED.name())
                                    .loanTerm(operationEntity.getLoanTerm())
                                    .customerId(customerEntity.getId())
                                    .build();
                            umicoClient.sendDecisionScoring(umicoScoringDecisionRequest, apiKey);
                        }
                    }
                } catch (JsonProcessingException j) {
                    log.error("order dvs status consume.Message - [{}], JsonProcessingException - {}",
                            message,
                            j.getMessage());
                }
            }
        };
    }
}
