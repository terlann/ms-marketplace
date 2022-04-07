package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.exception.UmicoClientException;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.OperationEntity;
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

    public Optional<String> sendPendingDecision(UUID trackId) {
        var umicoDecisionRequest =
                UmicoDecisionRequest.builder().decisionStatus(UmicoDecisionStatus.PENDING)
                        .trackId(trackId).build();
        return sendDecision(umicoDecisionRequest);
    }

    public Optional<String> sendRejectedDecision(UUID trackId) {
        var umicoDecisionRequest =
                UmicoDecisionRequest.builder().decisionStatus(UmicoDecisionStatus.REJECTED)
                        .trackId(trackId).build();
        return sendDecision(umicoDecisionRequest);
    }

    public Optional<String> sendPreApprovedDecision(UUID trackId,
                                                    String dvsUrl,
                                                    UmicoDecisionStatus decisionStatus) {
        var umicoDecisionRequest =
                UmicoDecisionRequest.builder()
                        .trackId(trackId)
                        .dvsUrl(dvsUrl)
                        .decisionStatus(decisionStatus)
                        .build();
        return sendDecision(umicoDecisionRequest);
    }

    public Optional<String> sendApprovedDecision(OperationEntity operationEntity, UUID customerId) {
        var umicoApprovedDecisionRequest =
                UmicoDecisionRequest.builder()
                        .trackId(operationEntity.getId())
                        .commission(operationEntity.getCommission())
                        .customerId(customerId)
                        .decisionStatus(operationEntity.getUmicoDecisionStatus())
                        .loanTerm(operationEntity.getLoanTerm())
                        .loanLimit(operationEntity.getScoredAmount())
                        .loanContractStartDate(operationEntity.getLoanContractStartDate())
                        .loanContractEndDate(operationEntity.getLoanContractEndDate()).build();
        return sendDecision(umicoApprovedDecisionRequest);
    }


    private Optional<String> sendDecision(UmicoDecisionRequest umicoDecisionRequest) {
        var trackId = umicoDecisionRequest.getTrackId();
        log.info("Send decision to umico is started : trackId - {} , request - {}", trackId,
                umicoDecisionRequest);
        try {
            var umicoDecisionResponse = umicoClient.sendDecision(umicoDecisionRequest, apiKey);
            log.info("Send decision to umico was finished : trackId - {} , response - {}", trackId,
                    umicoDecisionResponse);
            return Optional.empty();
        } catch (UmicoClientException e) {
            log.error(
                    "Send decision to umico was failed : trackId - {} , UmicoClientException - {}",
                    trackId, e);
            return Optional.ofNullable(e.getMessage());
        }
    }
}
