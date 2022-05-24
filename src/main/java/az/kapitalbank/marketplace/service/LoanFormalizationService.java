package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.AtlasConstant.UID;

import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.constant.DvsStatus;
import az.kapitalbank.marketplace.constant.FraudResultStatus;
import az.kapitalbank.marketplace.constant.OperationRejectReason;
import az.kapitalbank.marketplace.constant.RejectedBusinessError;
import az.kapitalbank.marketplace.constant.ScoringStatus;
import az.kapitalbank.marketplace.constant.SendLeadReason;
import az.kapitalbank.marketplace.constant.TaskDefinitionKey;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.messaging.event.BusinessErrorData;
import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.messaging.event.InUserActivityData;
import az.kapitalbank.marketplace.messaging.event.PrePurchaseEvent;
import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
import az.kapitalbank.marketplace.messaging.event.VerificationResultEvent;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoanFormalizationService {

    UmicoService umicoService;
    OrderService orderService;
    ScoringService scoringService;
    LeadService leadService;
    VerificationService verificationService;
    OperationRepository operationRepository;
    SmsService smsService;

    @Transactional
    public void fraudResultProcess(FraudCheckResultEvent fraudCheckResultEvent) {
        log.info("Fraud result process is stared : message - {}", fraudCheckResultEvent);
        var trackId = fraudCheckResultEvent.getTrackId();
        var fraudResultStatus = fraudCheckResultEvent.getFraudResultStatus();
        var operationOptional = operationRepository.findById(trackId);
        if (operationOptional.isPresent()) {
            var operationEntity = operationOptional.get();
            if (fraudResultStatus == FraudResultStatus.BLACKLIST) {
                log.info("This operation was found in blacklist : trackId - {}", trackId);
                var umicoDecisionStatus = umicoService.sendRejectedDecision(trackId);
                operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
                operationEntity.setRejectReason(OperationRejectReason.BLACKLIST);
                operationRepository.save(operationEntity);
            } else if (fraudResultStatus == FraudResultStatus.SUSPICIOUS_TELESALES) {
                log.info("Fraud case was found and sent to Telesales : trackId - {}",
                        trackId);
                operationEntity.setSendLeadReason(SendLeadReason.FRAUD_LIST);
                leadService.sendLead(operationEntity, fraudCheckResultEvent.getTypes());
                operationRepository.save(operationEntity);
            } else if (fraudResultStatus == FraudResultStatus.SUSPICIOUS_UMICO) {
                log.info("Fraud case was found and sent to umico : trackId - {}", trackId);
                var umicoDecisionStatus = umicoService.sendRejectedDecision(trackId);
                operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
                operationEntity.setRejectReason(OperationRejectReason.FRAUD_RULE);
                operationRepository.save(operationEntity);
            } else {
                noFraudProcess(operationEntity);
            }
        }
    }

    @Transactional
    public void scoringResultProcess(ScoringResultEvent scoringResultEvent) {
        var businessKey = scoringResultEvent.getBusinessKey();
        var operation = operationRepository.findByBusinessKey(businessKey);
        if (operation.isPresent()
                && operation.get().getScoringStatus() == null
                && operation.get().getIsSendLead() == null) {
            switch (scoringResultEvent.getProcessStatus()) {
                case IN_USER_ACTIVITY:
                    inUserActivityProcess(scoringResultEvent, operation.get());
                    break;
                case COMPLETED:
                    scoringCompletedProcess(operation.get());
                    break;
                case BUSINESS_ERROR:
                    scoringBusinessErrorProcess(scoringResultEvent, operation.get());
                    break;
                case INCIDENT_HAPPENED:
                    scoringIncidentProcess(scoringResultEvent, operation.get());
                    break;
                default:
            }
        }
    }

    @Transactional
    public void verificationResultProcess(VerificationResultEvent verificationResultEvent) {
        var trackId = verificationResultEvent.getTrackId();
        var operationOptional = operationRepository.findById(trackId);
        if (operationOptional.isPresent() && operationOptional.get().getIsSendLead() == null) {
            var operation = operationOptional.get();
            var dvsOrderStatus = operation.getDvsOrderStatus();
            if (dvsOrderStatus == null || dvsOrderStatus == DvsStatus.PENDING) {
                var verificationStatus = verificationResultEvent.getStatus();
                switch (verificationStatus) {
                    case "confirmed":
                        onVerificationConfirmed(operation);
                        break;
                    case "pending":
                        if (dvsOrderStatus != DvsStatus.PENDING) {
                            onVerificationPending(operation);
                        }
                        break;
                    case "rejected":
                        onVerificationRejected(operation);
                        break;
                    default:
                }
            }
        }
    }

    private void onVerificationConfirmed(OperationEntity operationEntity) {
        log.info("Verification status result confirmed : trackId - {}", operationEntity.getId());
        var completeScoring = scoringService.completeScoring(operationEntity);
        if (completeScoring.isEmpty()) {
            operationEntity.setSendLeadReason(SendLeadReason.OPTIMUS_FAIL_COMPLETE_SCORING);
            leadService.sendLead(operationEntity, null);
        } else {
            operationEntity.setDvsOrderStatus(DvsStatus.CONFIRMED);
        }
        operationRepository.save(operationEntity);
    }

    private void onVerificationRejected(OperationEntity operationEntity) {
        log.info("Verification status result rejected : trackId - {}", operationEntity.getId());
        var umicoDecisionStatus = umicoService.sendRejectedDecision(operationEntity.getId());
        operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        operationEntity.setRejectReason(OperationRejectReason.DVS);
        operationEntity.setDvsOrderStatus(DvsStatus.REJECTED);
        operationRepository.save(operationEntity);
    }

    private void onVerificationPending(OperationEntity operationEntity) {
        log.info("Verification status result pending : trackId - {}", operationEntity.getId());
        var umicoDecisionStatus = umicoService.sendPendingDecision(operationEntity.getId());
        operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        operationEntity.setDvsOrderStatus(DvsStatus.PENDING);
        operationRepository.save(operationEntity);
    }

    private void inUserActivityProcess(ScoringResultEvent scoringResultEvent,
                                       OperationEntity operationEntity) {
        var processResponse = scoringService.getProcess(operationEntity);
        if (processResponse.isPresent()) {
            var inUserActivityData = (InUserActivityData) scoringResultEvent.getData();
            var taskId = processResponse.get().getTaskId();
            var taskDefinitionKey = inUserActivityData.getTaskDefinitionKey();
            if (taskDefinitionKey.equalsIgnoreCase(TaskDefinitionKey.USER_TASK_SCORING.name())) {
                var scoredAmount =
                        processResponse.get().getVariables().getSelectedOffer().getCardOffer()
                                .getAvailableLoanAmount();
                userTaskScoringProcess(operationEntity, scoredAmount, taskId);
            } else if (taskDefinitionKey.equalsIgnoreCase(
                    TaskDefinitionKey.USER_TASK_SIGN_DOCUMENTS.name())) {
                userTaskSignDocumentsProcess(operationEntity, processResponse.get(), taskId);
            }
        } else {
            operationEntity.setSendLeadReason(SendLeadReason.OPTIMUS_FAIL_GET_PROCESS);
            leadService.sendLead(operationEntity, null);
            operationRepository.save(operationEntity);
        }
    }


    private void userTaskScoringProcess(OperationEntity operationEntity, BigDecimal scoredAmount,
                                        String taskId) {
        log.info("Start scoring result : trackId - {}, businessKey - {}",
                operationEntity.getId(), operationEntity.getBusinessKey());
        operationEntity.setTaskId(taskId);
        operationEntity.setScoredAmount(scoredAmount);
        var selectedAmount = operationEntity.getTotalAmount().add(operationEntity.getCommission());
        if (scoredAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Start scoring result - Scoring amount is zero : trackId - {}",
                    operationEntity.getId());
            leadService.sendLead(operationEntity, null);
            var umicoDecisionStatus = umicoService.sendPendingDecision(operationEntity.getId());
            operationEntity.setSendLeadReason(SendLeadReason.SCORED_AMOUNT_IS_ZERO);
            operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        } else if (scoredAmount.compareTo(selectedAmount) < 0) {
            log.info("Start scoring result - No enough amount : selectedAmount - {},"
                            + " scoredAmount - {}, trackId - {}", selectedAmount, scoredAmount,
                    operationEntity.getId());
            operationEntity.setSendLeadReason(SendLeadReason.OPTIMUS_NO_ENOUGH_AMOUNT);
            leadService.sendLead(operationEntity, null);
        } else {
            var createScoring = scoringService.createScoring(operationEntity);
            if (createScoring.isEmpty()) {
                operationEntity.setSendLeadReason(SendLeadReason.OPTIMUS_FAIL_CREATE_SCORING);
                leadService.sendLead(operationEntity, null);
            }
        }
        operationRepository.save(operationEntity);
    }

    private void userTaskSignDocumentsProcess(OperationEntity operationEntity,
                                              ProcessResponse processResponse, String taskId) {
        log.info("Create scoring result : trackId - {}, businessKey - {}",
                operationEntity.getId(),
                operationEntity.getBusinessKey());
        var dvsId = processResponse.getVariables().getDvsOrderId();
        var start = processResponse.getVariables().getCreateCardCreditRequest().getStartDate();
        var end = processResponse.getVariables().getCreateCardCreditRequest().getEndDate();
        operationEntity.setLoanContractStartDate(start);
        operationEntity.setLoanContractEndDate(end);
        operationEntity.setTaskId(taskId);
        operationEntity.setDvsOrderId(dvsId);
        var trackId = operationEntity.getId();
        var dvsUrl = verificationService.getDvsUrl(trackId, dvsId);
        if (dvsUrl.isPresent()) {
            var umicoDecisionStatus = umicoService.sendPreApprovedDecision(trackId, dvsUrl.get(),
                    UmicoDecisionStatus.PREAPPROVED);
            smsService.sendPreapproveSms(operationEntity);
            operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        } else {
            operationEntity.setSendLeadReason(SendLeadReason.DVS_URL_FAIL);
            leadService.sendLead(operationEntity, null);
        }
        operationRepository.save(operationEntity);
    }

    private void scoringCompletedProcess(OperationEntity operationEntity) {
        var trackId = operationEntity.getId();
        log.info("Complete scoring result : trackId - {}, businessKey - {}",
                trackId, operationEntity.getBusinessKey());
        operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.APPROVED);
        operationEntity.setScoringDate(LocalDateTime.now());
        operationEntity.setScoringStatus(ScoringStatus.APPROVED);
        Optional<String> cardId = scoringService.getCardId(operationEntity, UID);
        if (cardId.isEmpty()) {
            operationRepository.save(operationEntity);
            return;
        }
        var customerEntity = operationEntity.getCustomer();
        customerEntity.setCardId(cardId.get());
        var lastTempAmount = orderService.prePurchaseOrders(operationEntity, cardId.get());
        if (lastTempAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Scoring complete result : Pre purchase was finished : trackId - {}", trackId);
            smsService.sendCompleteScoringSms(operationEntity);
            smsService.sendPrePurchaseSms(operationEntity);
        } else {
            log.info("Scoring complete result : Pre purchase was failed : trackId - {}", trackId);
        }
        var customerId = customerEntity.getId();
        var umicoDecisionStatus =
                umicoService.sendApprovedDecision(operationEntity, customerId);
        operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        operationRepository.save(operationEntity);
        log.info("Scoring complete result : Customer was finished end-to-end process : "
                + "trackId - {} , customerId - {}", trackId, customerId);
    }

    private void noFraudProcess(OperationEntity operationEntity) {
        var businessKey =
                scoringService.startScoring(operationEntity);
        if (businessKey.isEmpty()) {
            operationEntity.setSendLeadReason(SendLeadReason.OPTIMUS_FAIL_START_SCORING);
            leadService.sendLead(operationEntity, null);
        } else {
            operationEntity.setBusinessKey(businessKey.get());
        }
        operationRepository.save(operationEntity);
    }

    private void scoringBusinessErrorProcess(ScoringResultEvent scoringResultEvent,
                                             OperationEntity operationEntity) {
        var businessErrorData = (BusinessErrorData[]) scoringResultEvent.getData();
        log.error("Scoring result : business error , response - {}",
                Arrays.toString(businessErrorData));
        var rejectedBusinessError = checkRejectedBusinessErrors(businessErrorData);
        if (rejectedBusinessError.isPresent()) {
            var umicoDecisionStatus = umicoService.sendRejectedDecision(operationEntity.getId());
            operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
            operationEntity.setRejectReason(OperationRejectReason.BUSINESS_ERROR);
        } else {
            leadService.sendLead(operationEntity, null);
            operationEntity.setSendLeadReason(SendLeadReason.OPTIMUS_FAIL_BUSINESS_ERROR);
        }
        operationRepository.save(operationEntity);
    }

    private void scoringIncidentProcess(ScoringResultEvent scoringResultEvent,
                                        OperationEntity operationEntity) {
        log.error("Scoring result : incident happened , response - {}",
                scoringResultEvent.getData());
        leadService.sendLead(operationEntity, null);
        operationEntity.setSendLeadReason(SendLeadReason.OPTIMUS_FAIL_INCIDENT_HAPPENED);
        operationRepository.save(operationEntity);
    }

    private Optional<String> checkRejectedBusinessErrors(BusinessErrorData[] businessErrors) {
        if (businessErrors == null) {
            return Optional.empty();
        }
        for (var businessError : businessErrors) {
            try {
                var rejectedBusinessError = RejectedBusinessError.valueOf(businessError.getId());
                return Optional.of(rejectedBusinessError.name());
            } catch (Exception ex) {
                log.info(
                        "Optimus business error not found in rejected business error business - {}",
                        businessError);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void prePurchaseProcess(PrePurchaseEvent prePurchaseEvent) {
        var trackId = prePurchaseEvent.getTrackId();
        var operationEntity = operationRepository.findById(trackId)
                .orElseThrow(() -> new OperationNotFoundException("trackId - " + trackId));
        var lastTempAmount = orderService.prePurchaseOrders(operationEntity,
                operationEntity.getCustomer().getCardId());
        if (lastTempAmount.compareTo(BigDecimal.ZERO) == 0) {
            umicoService.sendPrePurchaseResult(trackId);
            smsService.sendPrePurchaseSms(operationEntity);
            log.info("Pre purchase result was sent to umico : trackId - {}", trackId);
        }
        operationRepository.save(operationEntity);
        log.info("Pre purchase consumer process was finished: trackId - {}", trackId);
    }
}
