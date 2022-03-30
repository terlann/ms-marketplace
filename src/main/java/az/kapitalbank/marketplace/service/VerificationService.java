package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.OptimusConstant.ADDITIONAL_NAME_1;
import static az.kapitalbank.marketplace.constant.OptimusConstant.ADDITIONAL_NAME_2;
import static az.kapitalbank.marketplace.constant.OptimusConstant.OPTIMUS_CLIENT_EXCEPTION;

import az.kapitalbank.marketplace.client.dvs.DvsClient;
import az.kapitalbank.marketplace.client.dvs.exception.DvsClientException;
import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.exception.OptimusClientException;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CompleteScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerContact;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerDecision;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerNumber;
import az.kapitalbank.marketplace.constant.DvsStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.messaging.event.VerificationResultEvent;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class VerificationService {

    DvsClient dvsClient;
    UmicoService umicoService;
    OptimusClient optimusClient;
    ScoringService scoringService;
    TelesalesService telesalesService;
    OperationRepository operationRepository;

    public Optional<String> getDvsUrl(UUID trackId, Long dvsId) {
        log.info("Dvs get web url is started : trackId - {} , dvsId - {}", trackId, dvsId);
        try {
            var webUrl = dvsClient.getDetails(trackId, dvsId).getWebUrl();
            log.info("Dvs get web url was finished : trackId - {} , webUrl - {}", trackId,
                    webUrl);
            return Optional.ofNullable(webUrl);
        } catch (DvsClientException e) {
            log.error("Dvs get web url was failed : trackId - {} , DvsClientException - {}",
                    trackId, e);
            return Optional.empty();
        }
    }

    @Transactional
    public void verificationResultProcess(VerificationResultEvent verificationResultEvent) {
        var trackId = verificationResultEvent.getTrackId();
        var operationOptional = operationRepository.findById(trackId);
        if (operationOptional.isPresent()) {
            var operation = operationOptional.get();
            var verificationStatus = verificationResultEvent.getStatus();
            switch (verificationStatus) {
                case "confirmed":
                    onVerificationConfirmed(operation);
                    break;
                case "pending":
                    onVerificationPending(operation);
                    break;
                case "rejected":
                    onVerificationRejected(operation);
                    break;
                default:
            }
        } else {
            log.error("Verification status result : Operation not found : trackId - {}", trackId);
        }
    }

    private void onVerificationConfirmed(OperationEntity operationEntity) {
        log.info("Verification status result confirmed : trackId - {}", operationEntity.getId());
        var completeScoring = completeScoring(operationEntity.getTaskId(),
                operationEntity.getBusinessKey(),
                operationEntity.getAdditionalPhoneNumber1(),
                operationEntity.getAdditionalPhoneNumber2());
        if (completeScoring.isEmpty()) {
            var deleteLoan = scoringService.deleteLoan(operationEntity);
            deleteLoan.ifPresent(operationEntity::setDeleteLoanAttemptDate);
            telesalesService.sendLeadAndDecision(operationEntity);
        } else {
            operationEntity.setDvsOrderStatus(DvsStatus.CONFIRMED);
        }
        operationRepository.save(operationEntity);
    }

    private void onVerificationRejected(OperationEntity operationEntity) {
        log.info("Verification status result rejected : trackId - {}", operationEntity.getId());
        var deleteLoan = scoringService.deleteLoan(operationEntity);
        deleteLoan.ifPresent(operationEntity::setDeleteLoanAttemptDate);
        var sendDecision = umicoService.sendRejectedDecision(operationEntity.getId());
        sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
        operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.REJECTED);
        operationEntity.setDvsOrderStatus(DvsStatus.REJECTED);
        operationRepository.save(operationEntity);
    }

    private void onVerificationPending(OperationEntity operationEntity) {
        log.info("Verification status result pending : trackId - {}", operationEntity.getId());
        var sendDecision = umicoService.sendPendingDecision(operationEntity.getId());
        sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
        operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.PENDING);
        operationEntity.setDvsOrderStatus(DvsStatus.PENDING);
        operationRepository.save(operationEntity);
    }

    private Optional<Boolean> completeScoring(String taskId, String businessKey,
                                              String additionalPhoneNumber1,
                                              String additionalPhoneNumber2) {
        var customerNumbers = Arrays.asList(
                new CustomerNumber(ADDITIONAL_NAME_1, additionalPhoneNumber1),
                new CustomerNumber(ADDITIONAL_NAME_2, additionalPhoneNumber2));

        var completeScoringRequest = CompleteScoringRequest.builder()
                .customerContact(new CustomerContact(customerNumbers))
                .customerDecision(CustomerDecision.CONFIRM_CREDIT).build();
        log.info("Complete scoring process is started : businessKey - {}, Request - {}",
                businessKey, completeScoringRequest);
        try {
            optimusClient.scoringComplete(taskId, completeScoringRequest);
            log.info("Complete scoring process was finished : businessKey - {}", businessKey);
            return Optional.of(true);
        } catch (OptimusClientException e) {
            log.error("Complete scoring process was failed : " + OPTIMUS_CLIENT_EXCEPTION,
                    businessKey, e);
            return Optional.empty();
        }
    }
}
