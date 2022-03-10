package az.kapitalbank.marketplace.messaging.listener;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerDecision;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.constant.DvsStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.messaging.event.VerificationResultEvent;
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
public class VerificationListener {

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
    public Consumer<String> verificationResult() {
        return message -> {
            if (Objects.nonNull(message)) {
                try {
                    var verificationResultEvent = objectMapper.readValue(message, VerificationResultEvent.class);
                    log.info("Verification status consumer. Message - {}", verificationResultEvent);
                    if (verificationResultEvent != null) {
                        var trackId = verificationResultEvent.getTrackId();
                        var operationEntity = operationRepository.findById(trackId)
                                .orElseThrow(() -> new OperationNotFoundException("trackId - " + trackId));

                        var businessKey = operationEntity.getBusinessKey();
                        var verificationStatus = verificationResultEvent.getStatus();
                        switch (verificationStatus) {
                            case "pending":
                                log.info("Verification status result. Response - {}", verificationResultEvent);
                                var umicoPendingDecisionRequest = UmicoDecisionRequest.builder()
                                        .trackId(operationEntity.getId())
                                        .decisionStatus(UmicoDecisionStatus.PENDING)
                                        .loanTerm(operationEntity.getLoanTerm())
                                        .build();
                                log.info("Verification status result. Send decision request - {}",
                                        umicoPendingDecisionRequest);
                                umicoClient.sendDecisionToUmico(umicoPendingDecisionRequest, apiKey);
                                log.info("Verification status sent to umico like PENDING.");
                                operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.PENDING);
                                operationEntity.setDvsOrderStatus(DvsStatus.PENDING);
                                operationRepository.save(operationEntity);
                                break;
                            case "rejected":
                                log.info("Verification status result. Response - {}", verificationResultEvent);
                                var umicoRejectedDecisionRequest = UmicoDecisionRequest.builder()
                                        .trackId(operationEntity.getId())
                                        .decisionStatus(UmicoDecisionStatus.REJECTED)
                                        .loanTerm(operationEntity.getLoanTerm())
                                        .build();
                                log.info("Verification status result. Send decision request - {}",
                                        umicoRejectedDecisionRequest);
                                umicoClient.sendDecisionToUmico(umicoRejectedDecisionRequest, apiKey);
                                log.info("Verification status sent to umico like REJECTED. trackId - {}",
                                        operationEntity.getId());
                                operationEntity.setDvsOrderStatus(DvsStatus.REJECTED);
                                operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.REJECTED);
                                operationRepository.save(operationEntity);
                                if (operationEntity.getTaskId() != null &&
                                        operationEntity.getLoanContractDeletedAt() == null) {
                                    operationEntity.setLoanContractDeletedAt(LocalDateTime.now());
                                    operationRepository.save(operationEntity);
                                    try {
                                        optimusClient.deleteLoan(businessKey);
                                    } catch (Exception e) {
                                        log.error("Delete loan process error in verification rejected status , " +
                                                "businessKey - {}, exception - {}", businessKey, e.getMessage());
                                    }
                                }
                                break;
                            case "confirmed":
                                log.info("Verification status result. Response - {}", verificationResultEvent);
                                var completeScoringWithConfirm = CompleteScoring.builder()
                                        .trackId(operationEntity.getId())
                                        .taskId(operationEntity.getTaskId())
                                        .businessKey(operationEntity.getBusinessKey())
                                        .additionalNumber1(operationEntity.getAdditionalPhoneNumber1())
                                        .additionalNumber2(operationEntity.getAdditionalPhoneNumber2())
                                        .customerDecision(CustomerDecision.CONFIRM_CREDIT)
                                        .build();
                                scoringService.completeScoring(completeScoringWithConfirm);
                                break;
                            default:
                        }
                    }
                } catch (JsonProcessingException j) {
                    log.error("Verification status consume.Message - {}, JsonProcessingException - {}",
                            message,
                            j.getMessage());
                }
            }
        };
    }
}
