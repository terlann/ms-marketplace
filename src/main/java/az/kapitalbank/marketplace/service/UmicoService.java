package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.PrePurchaseResultRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.OperationEntity;
import feign.FeignException;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UmicoService {

    @NonFinal
    @Value("${umico.api-key}")
    String apiKey;
    UmicoClient umicoClient;

    public Optional<UUID> sendPrePurchaseResult(UUID trackId) {
        log.info("Send pre purchase result to umico is started : trackId - {} ", trackId);
        try {
            umicoClient.sendPrePurchaseResult(new PrePurchaseResultRequest(trackId), apiKey);
            log.info("Send pre purchase result to umico was finished : trackId - {} ", trackId);
            return Optional.of(trackId);
        } catch (FeignException e) {
            log.error(
                    "Send pre purchase result to umico was failed : trackId - {} , exception - {}",
                    trackId, e);
            return Optional.empty();
        }
    }

    public UmicoDecisionStatus sendPendingDecision(UUID trackId) {
        var umicoDecisionRequest =
                UmicoDecisionRequest.builder().decisionStatus(UmicoDecisionStatus.PENDING)
                        .trackId(trackId).build();
        var sendDecision = sendDecision(umicoDecisionRequest);
        return sendDecision.orElse(UmicoDecisionStatus.FAIL_IN_PENDING);
    }

    public UmicoDecisionStatus sendRejectedDecision(UUID trackId) {
        var umicoDecisionRequest =
                UmicoDecisionRequest.builder().decisionStatus(UmicoDecisionStatus.REJECTED)
                        .trackId(trackId).build();
        var sendDecision = sendDecision(umicoDecisionRequest);
        return sendDecision.orElse(UmicoDecisionStatus.FAIL_IN_REJECTED);
    }

    public UmicoDecisionStatus sendPreApprovedDecision(UUID trackId,
                                                       String dvsUrl,
                                                       UmicoDecisionStatus decisionStatus) {
        var umicoDecisionRequest =
                UmicoDecisionRequest.builder()
                        .trackId(trackId)
                        .dvsUrl(dvsUrl)
                        .decisionStatus(decisionStatus)
                        .build();
        var sendDecision = sendDecision(umicoDecisionRequest);
        return sendDecision.orElse(UmicoDecisionStatus.FAIL_IN_PREAPPROVED);
    }

    public UmicoDecisionStatus sendApprovedDecision(OperationEntity operationEntity,
                                                    UUID customerId) {
        var umicoDecisionRequest =
                UmicoDecisionRequest.builder()
                        .trackId(operationEntity.getId())
                        .commission(operationEntity.getCommission())
                        .customerId(customerId)
                        .decisionStatus(UmicoDecisionStatus.APPROVED)
                        .loanTerm(operationEntity.getLoanTerm())
                        .loanLimit(operationEntity.getScoredAmount())
                        .loanContractStartDate(operationEntity.getLoanContractStartDate())
                        .loanContractEndDate(operationEntity.getLoanContractEndDate()).build();
        var sendDecision = sendDecision(umicoDecisionRequest);
        return sendDecision.orElse(UmicoDecisionStatus.FAIL_IN_APPROVED);
    }


    private Optional<UmicoDecisionStatus> sendDecision(UmicoDecisionRequest umicoDecisionRequest) {
        var trackId = umicoDecisionRequest.getTrackId();
        log.info("Send decision to umico is started : trackId - {} , request - {}", trackId,
                umicoDecisionRequest);
        try {
            var umicoDecisionResponse = umicoClient.sendDecision(umicoDecisionRequest, apiKey);
            log.info("Send decision to umico was finished : trackId - {} , response - {}", trackId,
                    umicoDecisionResponse);
            return Optional.of(umicoDecisionRequest.getDecisionStatus());
        } catch (FeignException e) {
            log.error(
                    "Send decision to umico was failed : trackId - {} , exception - {}",
                    trackId, e);
            return Optional.empty();
        }
    }
}
