package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessVariableResponse;
import az.kapitalbank.marketplace.constant.DvsStatus;
import az.kapitalbank.marketplace.constant.Error;
import az.kapitalbank.marketplace.constant.FraudResultStatus;
import az.kapitalbank.marketplace.constant.ProcessStatus;
import az.kapitalbank.marketplace.constant.RejectedBusinessError;
import az.kapitalbank.marketplace.constant.ScoringStatus;
import az.kapitalbank.marketplace.constant.TaskDefinitionKey;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.ProcessStepEntity;
import az.kapitalbank.marketplace.exception.CommonException;
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
import java.util.Collections;
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
            if (fraudResultStatus == null) {
                noFraudProcess(operationEntity);
            } else {
                operationEntity.setProcessStatus(fraudResultStatus.name());
                var processStep =
                        ProcessStepEntity.builder().value(fraudResultStatus.name()).build();
                operationEntity.setProcessSteps(Collections.singletonList(processStep));
                processStep.setOperation(operationEntity);
                if (FraudResultStatus.existRejected(fraudResultStatus)) {
                    log.info("It was rejected by fraud : trackId - {}, fraudReason - {}",
                            trackId, fraudResultStatus);
                    var umicoDecisionStatus = umicoService.sendRejectedDecision(trackId);
                    operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
                } else {
                    log.info("It was sent to telesales by fraud : trackId - {}, fraudReason - {}",
                            trackId, fraudResultStatus);
                    leadService.sendLead(operationEntity);
                }
            }
        } else {
            log.error("Fraud result process, operation not found : trackId - {}", trackId);
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
        operationEntity.setDvsOrderStatus(DvsStatus.CONFIRMED);
        var completeScoring = scoringService.completeScoring(operationEntity);
        if (completeScoring.isEmpty()) {
            var processStatus = ProcessStatus.OPTIMUS_FAIL_COMPLETE_SCORING;
            operationEntity.setProcessStatus(processStatus);
            var processStep = ProcessStepEntity.builder().value(processStatus).build();
            operationEntity.setProcessSteps(Collections.singletonList(processStep));
            processStep.setOperation(operationEntity);
            leadService.sendLead(operationEntity);
        }
    }

    private void onVerificationRejected(OperationEntity operationEntity) {
        log.info("Verification status result rejected : trackId - {}", operationEntity.getId());
        var processStatus = ProcessStatus.DVS_REJECT;
        operationEntity.setProcessStatus(processStatus);
        var processStep = ProcessStepEntity.builder().value(processStatus).build();
        operationEntity.setProcessSteps(Collections.singletonList(processStep));
        processStep.setOperation(operationEntity);
        operationEntity.setDvsOrderStatus(DvsStatus.REJECTED);
        var umicoDecisionStatus = umicoService.sendRejectedDecision(operationEntity.getId());
        operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
    }

    private void onVerificationPending(OperationEntity operationEntity) {
        log.info("Verification status result pending : trackId - {}", operationEntity.getId());
        var umicoDecisionStatus = umicoService.sendPendingDecision(operationEntity.getId());
        operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        operationEntity.setDvsOrderStatus(DvsStatus.PENDING);
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
            var processStatus = ProcessStatus.OPTIMUS_FAIL_GET_PROCESS;
            operationEntity.setProcessStatus(processStatus);
            var processStep = ProcessStepEntity.builder().value(processStatus).build();
            operationEntity.setProcessSteps(Collections.singletonList(processStep));
            processStep.setOperation(operationEntity);
            leadService.sendLead(operationEntity);
        }
    }


    private void userTaskScoringProcess(OperationEntity operationEntity, BigDecimal scoredAmount,
                                        String taskId) {
        log.info("Start scoring result : trackId - {}, businessKey - {}",
                operationEntity.getId(), operationEntity.getBusinessKey());
        operationEntity.setTaskId(taskId);
        operationEntity.setScoredAmount(scoredAmount);
        var selectedAmount = operationEntity.getTotalAmount().add(operationEntity.getCommission());
        String processStatus = null;
        if (scoredAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Start scoring result - Scoring amount is zero : trackId - {}",
                    operationEntity.getId());
            processStatus = ProcessStatus.OPTIMUS_SCORED_AMOUNT_ZERO;
        } else if (scoredAmount.compareTo(selectedAmount) < 0) {
            log.info("Start scoring result - No enough amount : selectedAmount - {},"
                            + " scoredAmount - {}, trackId - {}", selectedAmount, scoredAmount,
                    operationEntity.getId());
            processStatus = ProcessStatus.OPTIMUS_NO_ENOUGH_AMOUNT;
        } else {
            var createScoring = scoringService.createScoring(operationEntity);
            if (createScoring.isEmpty()) {
                processStatus = ProcessStatus.OPTIMUS_FAIL_CREATE_SCORING;
            }
        }

        if (processStatus != null) {
            operationEntity.setProcessStatus(processStatus);
            var processStep = ProcessStepEntity.builder().value(processStatus).build();
            operationEntity.setProcessSteps(Collections.singletonList(processStep));
            processStep.setOperation(operationEntity);
            leadService.sendLead(operationEntity);
        }
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
            var processStatus = ProcessStatus.DVS_FAIL_URL;
            operationEntity.setProcessStatus(processStatus);
            var processStep = ProcessStepEntity.builder().value(processStatus).build();
            operationEntity.setProcessSteps(Collections.singletonList(processStep));
            processStep.setOperation(operationEntity);
            leadService.sendLead(operationEntity);
        }
    }

    private void scoringCompletedProcess(OperationEntity operationEntity) {
        var trackId = operationEntity.getId();
        log.info("Complete scoring result : trackId - {}, businessKey - {}",
                trackId, operationEntity.getBusinessKey());
        operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.APPROVED);
        operationEntity.setScoringDate(LocalDateTime.now());
        operationEntity.setScoringStatus(ScoringStatus.APPROVED);
        var processVariables = scoringService.getProcessVariable(operationEntity, null);
        if (processVariables.isEmpty()) {
            return;
        }
        updateCifAndContractByGetProcessData(operationEntity, processVariables.get());
        var customerEntity = operationEntity.getCustomer();
        customerEntity.setCardId(processVariables.get().getUid());
        var lastTempAmount =
                orderService.prePurchaseOrders(operationEntity, processVariables.get().getUid());
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
        log.info("Scoring complete result : Customer was finished end-to-end process : "
                + "trackId - {} , customerId - {}", trackId, customerId);
    }

    private void updateCifAndContractByGetProcessData(OperationEntity operationEntity,
                                                      ProcessVariableResponse
                                                              processVariables) {
        operationEntity.setCif(processVariables.getCif());
        operationEntity.setContractNumber(processVariables.getCardCreditContractNumber());
    }


    private void noFraudProcess(OperationEntity operationEntity) {
        var businessKey =
                scoringService.startScoring(operationEntity);
        if (businessKey.isEmpty()) {
            var processStatus = ProcessStatus.OPTIMUS_FAIL_START_SCORING;
            operationEntity.setProcessStatus(processStatus);
            var processStep = ProcessStepEntity.builder().value(processStatus).build();
            operationEntity.setProcessSteps(Collections.singletonList(processStep));
            processStep.setOperation(operationEntity);
            leadService.sendLead(operationEntity);
        } else {
            operationEntity.setBusinessKey(businessKey.get());
        }
    }

    private void scoringBusinessErrorProcess(ScoringResultEvent scoringResultEvent,
                                             OperationEntity operationEntity) {
        var businessErrorData = (BusinessErrorData[]) scoringResultEvent.getData();
        log.error("Scoring result : business error , trackId - {}, response - {}",
                operationEntity.getId(), Arrays.toString(businessErrorData));

        var processStatus = businessErrorData == null ? ProcessStatus.BUSINESS_ERROR_EMPTY :
                ProcessStatus.BUSINESS_ERROR_PREFIX + businessErrorData[0].getId();
        operationEntity.setProcessStatus(processStatus);
        var processStep = ProcessStepEntity.builder().value(processStatus).build();
        operationEntity.setProcessSteps(Collections.singletonList(processStep));
        processStep.setOperation(operationEntity);

        if (businessErrorData != null && isRejectedBusinessErrors(businessErrorData)) {
            var umicoDecisionStatus = umicoService.sendRejectedDecision(operationEntity.getId());
            operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        } else {
            leadService.sendLead(operationEntity);
        }
    }

    private void scoringIncidentProcess(ScoringResultEvent scoringResultEvent,
                                        OperationEntity operationEntity) {
        log.error("Scoring result : incident happened , trackId - {}, response - {}",
                operationEntity.getId(), scoringResultEvent.getData());
        var processStatus = ProcessStatus.OPTIMUS_INCIDENT_HAPPENED;
        operationEntity.setProcessStatus(processStatus);
        var processStep = ProcessStepEntity.builder().value(processStatus).build();
        operationEntity.setProcessSteps(Collections.singletonList(processStep));
        processStep.setOperation(operationEntity);
        leadService.sendLead(operationEntity);
    }

    private boolean isRejectedBusinessErrors(BusinessErrorData[] businessErrors) {
        for (var businessError : businessErrors) {
            try {
                RejectedBusinessError.valueOf(businessError.getId());
                return true;
            } catch (Exception ex) {
                log.error("Optimus business error not found in rejected business error,"
                        + " businessError - {}", businessError);
            }
        }
        return false;
    }

    @Transactional
    public void prePurchaseProcess(PrePurchaseEvent prePurchaseEvent) {
        var trackId = prePurchaseEvent.getTrackId();
        var operationEntity = operationRepository.findById(trackId)
                .orElseThrow(() -> new CommonException(Error.OPERATION_NOT_FOUND,
                        "Operation not found : trackId - " + trackId));
        var lastTempAmount = orderService.prePurchaseOrders(operationEntity,
                operationEntity.getCustomer().getCardId());
        if (lastTempAmount.compareTo(BigDecimal.ZERO) == 0) {
            umicoService.sendPrePurchaseResult(trackId);
            smsService.sendPrePurchaseSms(operationEntity);
            log.info("Pre purchase result was sent to umico : trackId - {}", trackId);
        }
        log.info("Pre purchase consumer process was finished: trackId - {}", trackId);
    }
}
