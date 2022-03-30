package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.OptimusConstant.OPTIMUS_CLIENT_EXCEPTION;

import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.exception.OptimusClientException;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CreateScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerDecision;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringVariable;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.constant.FraudResultStatus;
import az.kapitalbank.marketplace.constant.ProcessStatus;
import az.kapitalbank.marketplace.constant.ScoringStatus;
import az.kapitalbank.marketplace.constant.TaskDefinitionKey;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.LeadDto;
import az.kapitalbank.marketplace.dto.request.TelesalesResultRequestDto;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.exception.OperationAlreadyScoredException;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.messaging.event.BusinessErrorData;
import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.messaging.event.InUserActivityData;
import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScoringService {

    UmicoService umicoService;
    OrderService orderService;
    OptimusClient optimusClient;
    TelesalesService telesalesService;
    VerificationService verificationService;
    OperationRepository operationRepository;

    @Transactional
    public void telesalesResult(TelesalesResultRequestDto request) {
        var telesalesOrderId = request.getTelesalesOrderId().trim();
        log.info("Telesales result is started... request - {}", request);
        var operationEntity = operationRepository.findByTelesalesOrderId(telesalesOrderId)
                .orElseThrow(() -> new OperationNotFoundException(
                        "telesalesOrderId - " + telesalesOrderId));
        if (operationEntity.getScoringStatus() != null) {
            throw new OperationAlreadyScoredException("telesalesOrderId - " + telesalesOrderId);
        }
        Optional<String> sendDecision;
        if (request.getScoringStatus() == ScoringStatus.APPROVED) {
            orderService.prePurchaseOrders(operationEntity, request.getUid());
            operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.APPROVED);
            operationEntity.setScoringStatus(ScoringStatus.APPROVED);
            operationEntity.setScoringDate(LocalDateTime.now());
            operationEntity.setLoanContractStartDate(request.getLoanContractStartDate());
            operationEntity.setLoanContractEndDate(request.getLoanContractEndDate());
            var customerEntity = operationEntity.getCustomer();
            customerEntity.setCardId(request.getUid());
            customerEntity.setCompleteProcessDate(LocalDateTime.now());
            sendDecision =
                    umicoService.sendApprovedDecision(operationEntity, customerEntity.getId());
        } else {
            operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.REJECTED);
            operationEntity.setScoringStatus(ScoringStatus.REJECTED);
            sendDecision = umicoService.sendRejectedDecision(operationEntity.getId());
        }
        sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
        operationRepository.save(operationEntity);
    }

    @Transactional
    public void fraudResultProcess(FraudCheckResultEvent fraudCheckResultEvent) {
        log.info("Fraud result process is stared : message - {}", fraudCheckResultEvent);
        var trackId = fraudCheckResultEvent.getTrackId();
        var fraudResultStatus = fraudCheckResultEvent.getFraudResultStatus();
        var operationOptional = operationRepository.findById(trackId);
        if (operationOptional.isPresent()) {
            var operationEntity = operationOptional.get();
            if (fraudResultStatus == FraudResultStatus.BLACKLIST) {
                log.warn("This operation was found in blacklist : trackId - {}", trackId);
                var sendDecision = umicoService.sendRejectedDecision(trackId);
                sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
                operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.DECLINED_BY_BLACKLIST);
                operationRepository.save(operationEntity);
                return;
            }
            if (fraudResultStatus == FraudResultStatus.SUSPICIOUS) {
                log.warn("Fraud case was found in this operation : trackId - {}", trackId);
                telesalesService.sendLeadAndDecision(operationEntity);
                operationRepository.save(operationEntity);
                return;
            }
            noFraudDetectedBehavior(operationEntity);
        }
    }

    @Transactional
    public void scoringResultProcess(ScoringResultEvent scoringResultEvent) {
        var businessKey = scoringResultEvent.getBusinessKey();
        var operationEntity = operationRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new OperationNotFoundException("businessKey - " + businessKey));
        switch (scoringResultEvent.getProcessStatus()) {
            case IN_USER_ACTIVITY:
                inUserActivityProcess(scoringResultEvent, operationEntity);
                break;
            case COMPLETED:
                scoringCompletedProcess(operationEntity);
                break;
            case BUSINESS_ERROR:
            case INCIDENT_HAPPENED:
                scoringErrorProcess(scoringResultEvent, operationEntity);
                break;
            default:
        }
    }

    public Optional<LocalDateTime> deleteLoan(OperationEntity operationEntity) {
        var businessKey = operationEntity.getBusinessKey();
        if (operationEntity.getTaskId() != null
                && operationEntity.getDeleteLoanAttemptDate() == null) {
            log.info("Delete loan process is started : businessKey - {}", businessKey);
            try {
                optimusClient.deleteLoan(operationEntity.getBusinessKey());
                log.info("Delete loan process was finished : businessKey - {}", businessKey);
            } catch (OptimusClientException e) {
                log.error("Delete loan process was failed : " + OPTIMUS_CLIENT_EXCEPTION,
                        businessKey, e);
            }
            return Optional.of(LocalDateTime.now());
        }
        return Optional.empty();
    }

    private void noFraudDetectedBehavior(OperationEntity operationEntity) {
        var businessKey = startScoring(operationEntity.getId(), operationEntity.getPin(),
                operationEntity.getMobileNumber());
        if (businessKey.isEmpty()) {
            var leadDto = LeadDto.builder().trackId(operationEntity.getId()).build();
            var telesalesOrderId = telesalesService.sendLead(leadDto);
            telesalesOrderId.ifPresent(operationEntity::setTelesalesOrderId);
            var sendDecision = umicoService.sendPendingDecision(operationEntity.getId());
            sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
            operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.PENDING);
            operationRepository.save(operationEntity);
            return;
        }
        operationEntity.setBusinessKey(businessKey.get());
        operationRepository.save(operationEntity);
    }

    private void inUserActivityProcess(ScoringResultEvent scoringResultEvent,
                                       OperationEntity operationEntity) {
        var businessKey = operationEntity.getBusinessKey();
        var processResponse = getProcess(businessKey);
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
            var deleteLoan = deleteLoan(operationEntity);
            deleteLoan.ifPresent(operationEntity::setDeleteLoanAttemptDate);
            telesalesService.sendLeadAndDecision(operationEntity);
            operationRepository.save(operationEntity);
        }
    }

    private void userTaskScoringProcess(OperationEntity operationEntity, BigDecimal scoredAmount,
                                        String taskId) {
        log.info("Start scoring result : businessKey - {}", operationEntity.getBusinessKey());
        operationEntity.setTaskId(taskId);
        var selectedAmount = operationEntity.getTotalAmount().add(operationEntity.getCommission());
        if (scoredAmount.compareTo(selectedAmount) < 0) {
            log.info("Start scoring result - No enough amount : selectedAmount - {},"
                    + " scoredAmount - {}", selectedAmount, scoredAmount);
            var telesalesOrderId = telesalesService.sendLead(new LeadDto(operationEntity.getId()));
            telesalesOrderId.ifPresent(operationEntity::setTelesalesOrderId);
            var sendDecision = umicoService.sendPendingDecision(operationEntity.getId());
            sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
            operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.PENDING);
        } else {
            var createScoring = createScoring(operationEntity.getId(), taskId, scoredAmount);
            if (createScoring.isEmpty()) {
                var telesalesOrderId =
                        telesalesService.sendLead(new LeadDto(operationEntity.getId()));
                telesalesOrderId.ifPresent(operationEntity::setTelesalesOrderId);
                var sendDecision = umicoService.sendPendingDecision(operationEntity.getId());
                sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
                operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.PENDING);
            }
        }
        operationRepository.save(operationEntity);
    }

    private void userTaskSignDocumentsProcess(OperationEntity operationEntity,
                                              ProcessResponse processResponse, String taskId) {
        log.info("Create scoring result : businessKey - {}", operationEntity.getBusinessKey());
        var dvsId = processResponse.getVariables().getDvsOrderId();
        var start = processResponse.getVariables().getCreateCardCreditRequest().getStartDate();
        var end = processResponse.getVariables().getCreateCardCreditRequest().getEndDate();
        operationEntity.setLoanContractStartDate(start);
        operationEntity.setLoanContractEndDate(end);
        operationEntity.setTaskId(taskId);
        operationEntity.setDvsOrderId(dvsId);
        var umicoDecisionRequest = new UmicoDecisionRequest();
        var trackId = operationEntity.getId();
        umicoDecisionRequest.setTrackId(trackId);
        var dvsUrl = verificationService.getDvsUrl(trackId, dvsId);
        if (dvsUrl.isPresent()) {
            umicoDecisionRequest.setDvsUrl(dvsUrl.get());
            umicoDecisionRequest.setDecisionStatus(UmicoDecisionStatus.PREAPPROVED);
            operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.PREAPPROVED);
        } else {
            var leadDto = LeadDto.builder().trackId(operationEntity.getId()).build();
            var telesalesOrderId = telesalesService.sendLead(leadDto);
            telesalesOrderId.ifPresent(operationEntity::setTelesalesOrderId);
            umicoDecisionRequest.setDecisionStatus(UmicoDecisionStatus.PENDING);
            operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.PENDING);
        }
        var sendDecision = umicoService.sendDecision(umicoDecisionRequest);
        sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
        operationRepository.save(operationEntity);
    }

    private void scoringCompletedProcess(OperationEntity operationEntity) {
        log.info("Complete scoring result : businessKey - {}", operationEntity.getBusinessKey());
        var cardId =
                optimusClient.getProcessVariable(operationEntity.getBusinessKey(), "uid").getUid();
        orderService.prePurchaseOrders(operationEntity, cardId);
        log.info("Auto flow : orders purchase process was finished...");
        operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.APPROVED);
        operationEntity.setScoringDate(LocalDateTime.now());
        operationEntity.setScoringStatus(ScoringStatus.APPROVED);
        var customerEntity = operationEntity.getCustomer();
        customerEntity.setCardId(cardId);
        customerEntity.setCompleteProcessDate(LocalDateTime.now());
        var sendDecision =
                umicoService.sendApprovedDecision(operationEntity, customerEntity.getId());
        sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
        operationRepository.save(operationEntity);
        log.info("Customer was finished whole flow : trackId - {} , customerId - {}",
                operationEntity.getId(), customerEntity.getId());
    }


    private void scoringErrorProcess(ScoringResultEvent scoringResultEvent,
                                     OperationEntity operationEntity) {
        if (scoringResultEvent.getProcessStatus() == ProcessStatus.BUSINESS_ERROR) {
            log.error("Scoring result : business error , response - {}",
                    Arrays.toString((BusinessErrorData[]) scoringResultEvent.getData()));
        } else if (scoringResultEvent.getProcessStatus() == ProcessStatus.INCIDENT_HAPPENED) {
            log.error("Scoring result : incident happened , response - {}",
                    scoringResultEvent.getData());
        }
        var deleteLoan = deleteLoan(operationEntity);
        deleteLoan.ifPresent(operationEntity::setDeleteLoanAttemptDate);
        var leadDto = LeadDto.builder().trackId(operationEntity.getId()).build();
        var telesalesOrderId = telesalesService.sendLead(leadDto);
        telesalesOrderId.ifPresent(operationEntity::setTelesalesOrderId);
        var sendDecision = umicoService.sendPendingDecision(operationEntity.getId());
        sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
        operationRepository.save(operationEntity);
    }

    private Optional<String> startScoring(UUID trackId, String pin, String mobileNumber) {
        log.info("Start scoring process is started : trackId - {}", trackId);
        var startScoringRequest =
                StartScoringRequest.builder().variables(StartScoringVariable.builder()
                        .pin(pin).phoneNumber(mobileNumber).build()).build();
        log.info("Start scoring process : trackId - {}, request - {}", trackId,
                startScoringRequest);
        try {
            var startScoringResponse = optimusClient.scoringStart(startScoringRequest);
            log.info("Start scoring process was finished : trackId - {}," + " response - {}",
                    trackId, startScoringResponse);
            return Optional.of(startScoringResponse.getBusinessKey());
        } catch (OptimusClientException e) {
            log.error("Start scoring process was failed : trackId - {},"
                    + " OptimusClientException - {}", trackId, e);
            return Optional.empty();
        }
    }

    private Optional<Boolean> createScoring(UUID trackId, String taskId, BigDecimal loanAmount) {
        log.info("Create scoring process is started : trackId - {}", trackId);
        var createScoringRequest =
                CreateScoringRequest.builder()
                        .cardDemandedAmount(loanAmount.toString())
                        .customerDecision(CustomerDecision.CREATE_CREDIT)
                        .build();
        log.info("Create scoring process : trackId - {}, request - {}", trackId,
                createScoringRequest);
        try {
            optimusClient.scoringCreate(taskId, createScoringRequest);
            log.info("Create scoring process was finished : trackId - {}", trackId);
            return Optional.of(true);
        } catch (OptimusClientException e) {
            log.error("Create scoring process was failed : "
                    + "trackId - {} ,OptimusClientException - {}", trackId, e);
            return Optional.empty();
        }
    }

    private Optional<ProcessResponse> getProcess(String businessKey) {
        log.info("Get process is started : businessKey - {}", businessKey);
        try {
            ProcessResponse processResponse = optimusClient.getProcess(businessKey);
            log.info("Get process was finished :  businessKey - {}, response - {}", businessKey,
                    processResponse);
            return Optional.of(processResponse);
        } catch (OptimusClientException ex) {
            log.error("Get process was failed : " + OPTIMUS_CLIENT_EXCEPTION,
                    businessKey, ex);
            return Optional.empty();
        }
    }
}
