package az.kapitalbank.marketplace.messaging.listener;

import java.util.Objects;
import java.util.function.Consumer;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerDecision;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.constant.DvsStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.messaging.event.DvsResultEvent;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.service.ScoringService;
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
public class VerificationResultListener {

    AtlasClient atlasClient;
    ObjectMapper objectMapper;
    ScoringService scoringService;
    OperationRepository operationRepository;
    OptimusClient optimusClient;
    CustomerRepository customerRepository;
    UmicoClient umicoClient;

    @NonFinal
    @Value("${umico.api-key}")
    String apiKey;

    @Bean
    public Consumer<String> orderDvsStatus() {
        return message -> {
            if (Objects.nonNull(message)) {
                try {
                    var orderDvsStatusEvent = objectMapper.readValue(message, DvsResultEvent.class);
                    log.info("order dvs status consumer. Message - {}", orderDvsStatusEvent);
                    if (orderDvsStatusEvent != null) {
                        var trackId = orderDvsStatusEvent.getTrackId();
                        var operationEntity = operationRepository.findById(trackId)
                                .orElseThrow(() -> new OperationNotFoundException("trackId - " + trackId));

                        var dvsOrderStatus = orderDvsStatusEvent.getStatus();
                        switch (dvsOrderStatus) {
                            case "pending":
                                log.info("Order Dvs status in pending. Order dvs status response - {}",
                                        orderDvsStatusEvent);
                                var umicoPendingDecisionRequest = UmicoDecisionRequest.builder()
                                        .trackId(operationEntity.getId())
                                        .decisionStatus(UmicoDecisionStatus.PENDING)
                                        .loanTerm(operationEntity.getLoanTerm())
                                        .build();
                                umicoClient.sendDecisionToUmico(umicoPendingDecisionRequest, apiKey);
                                log.info("Order Dvs status sent to umico like PENDING.");
                                operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.PENDING);
                                operationEntity.setDvsOrderStatus(DvsStatus.PENDING);
                                operationRepository.save(operationEntity);
                                break;
                            case "rejected":
                                log.info("Order Dvs status in rejected. Order dvs status response - {}",
                                        orderDvsStatusEvent);
                                optimusClient.deleteLoan(operationEntity.getBusinessKey());
                                log.info("Loan deleted in optimus. businessKey - {}",
                                        operationEntity.getBusinessKey());
                                var umicoRejectedDecisionRequest = UmicoDecisionRequest.builder()
                                        .trackId(operationEntity.getId())
                                        .decisionStatus(UmicoDecisionStatus.REJECTED)
                                        .loanTerm(operationEntity.getLoanTerm())
                                        .build();
                                umicoClient.sendDecisionToUmico(umicoRejectedDecisionRequest, apiKey);
                                log.info("Order Dvs status sent to umico like REJECTED. trackId - {}",
                                        operationEntity.getId());
                                operationEntity.setDvsOrderStatus(DvsStatus.REJECTED);
                                operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.REJECTED);
                                operationRepository.save(operationEntity);
                                break;
                            case "confirmed":
                                log.info("Order Dvs status in confirmed. Order dvs status response - {}",
                                        orderDvsStatusEvent);
                                var completeScoringWithConfirm = CompleteScoring.builder()
                                        .trackId(operationEntity.getId())
                                        .taskId(operationEntity.getTaskId())
                                        .businessKey(operationEntity.getBusinessKey())
                                        .additionalNumber1(operationEntity.getAdditionalPhoneNumber1())
                                        .additionalNumber2(operationEntity.getAdditionalPhoneNumber2())
                                        .customerDecision(CustomerDecision.CONFIRM_CREDIT)
                                        .build();
                                scoringService.completeScoring(completeScoringWithConfirm);
                                log.info("Optimus complete process was confirmed. businessKey - {}",
                                        operationEntity.getBusinessKey());

                                var cardPan = optimusClient.getProcessVariable(operationEntity.getBusinessKey(),
                                        "pan");
                                var cardId = atlasClient.findByPan(cardPan).getUid();
                                log.info("Pan was taken and changed to card uid.Purchase process starts. cardId - {}",
                                        cardId);
                                var customerEntity = operationEntity.getCustomer();
                                customerEntity.setCardId(cardId);
                                customerRepository.save(customerEntity);
                                break;
                            default:
                        }
                    }
                } catch (JsonProcessingException j) {
                    log.error("order dvs status consume.Message - {}, JsonProcessingException - {}",
                            message,
                            j.getMessage());
                }
            }
        };
    }
}
