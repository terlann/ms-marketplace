package az.kapitalbank.marketplace.service;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;
import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessData;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionResponse;
import az.kapitalbank.marketplace.constant.FraudResultStatus;
import az.kapitalbank.marketplace.constant.ProcessStatus;
import az.kapitalbank.marketplace.constant.ScoringLevel;
import az.kapitalbank.marketplace.constant.TaskDefinitionKey;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.exception.FeignClientException;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.exception.ScoringException;
import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.messaging.event.InUserActivityData;
import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
import az.kapitalbank.marketplace.repository.OperationRepository;
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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductCreateService {

    @NonFinal
    @Value("${umico.api-key}")
    String apiKey;

    OperationRepository operationRepository;
    ScoringService scoringService;
    VerificationService verificationService;
    TelesalesService telesalesService;
    UmicoClient umicoClient;
    OptimusClient optimusClient;


    @Transactional
    public void startScoring(FraudCheckResultEvent fraudCheckResultEvent) {
        log.info("ProductCreateService: optimus start process... fraud_result_event - {}", fraudCheckResultEvent);
        var trackId = fraudCheckResultEvent.getTrackId();
        var fraudResultStatus = fraudCheckResultEvent.getFraudResultStatus();

        if (fraudResultStatus == FraudResultStatus.BLACKLIST) {
            log.info("ProductCreateService: this order was found in blacklist. track_id - [{}]", trackId);
            sendDecision(UmicoDecisionStatus.DECLINED_BY_BLACKLIST, trackId, null);
            return;
        }

        if (fraudResultStatus == FraudResultStatus.SUSPICIOUS || fraudResultStatus == FraudResultStatus.WARNING) {
            log.info("ProductCreateService: fraud case was found in order. track_id - [{}]", trackId);
            var telesalesOrderId = telesalesService.sendLead(trackId);
            updateOperationTelesalesOrderId(trackId, telesalesOrderId);
            sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
            return;
        }

        var operationEntityOptional = operationRepository.findById(trackId);
        if (operationEntityOptional.isPresent()) {
            var operationEntity = operationEntityOptional.get();
            try {
                Optional<String> businessKey = scoringService.startScoring(trackId,
                        operationEntity.getPin(), operationEntity.getMobileNumber());
                if (businessKey.isPresent()) {
                    operationEntity.setScoringLevel(ScoringLevel.START);
                    operationEntity.setBusinessKey(businessKey.get());
                    operationRepository.save(operationEntity);
                    log.info("ProductCreateService: optimus stared scoring.track_id - [{}], business_key - [{}]",
                            trackId, businessKey);
                }
            } catch (ScoringException e) {
                log.error("ProductCreateService: " +
                        "optimus start scoring was failed.Redirect to telesales track_id - [{}]", trackId);
                var telesalesOrderId = telesalesService.sendLead(trackId);
                updateOperationTelesalesOrderId(trackId, telesalesOrderId);
                sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
            }
        }

    }

    @Transactional
    public void createScoring(ScoringResultEvent scoringResultEvent) {
        var businessKey = scoringResultEvent.getBusinessKey();
        var operationEntity = operationRepository.findByBusinessKey(businessKey).orElseThrow(
                () -> new OperationNotFoundException("businessKey - " + businessKey));
        var trackId = operationEntity.getId();
        if (scoringResultEvent.getProcessStatus().equals(ProcessStatus.IN_USER_ACTIVITY)) {
            var processResponse = scoringService.getProcess(trackId, businessKey).orElseThrow(
                    () -> new RuntimeException("Optimus getProcess is null"));
            InUserActivityData inUserActivityData = (InUserActivityData) scoringResultEvent.getData();
            String taskDefinitionKey = inUserActivityData.getTaskDefinitionKey();
            if (taskDefinitionKey.equalsIgnoreCase(TaskDefinitionKey.USER_TASK_SCORING)) {
                var taskId = processResponse.getTaskId();
                ProcessData processData = processResponse.getVariables();
                var scoredAmount = processData.getSelectedOffer().getCashOffer().getAvailableLoanAmount();
                if (scoredAmount.compareTo(operationEntity.getTotalAmount().add(operationEntity.getCommission())) < 0) {
                    var telesalesOrderId = telesalesService.sendLead(trackId);
                    updateOperationTelesalesOrderId(trackId, telesalesOrderId);
                    sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
                    return;
                }
                operationEntity.setTaskId(taskId);
                operationRepository.save(operationEntity);
                scoringService.createScoring(trackId, taskId, scoredAmount);
            } else if (taskDefinitionKey.equalsIgnoreCase(TaskDefinitionKey.USER_TASK_SIGN_DOCUMENTS)) {
                try {
                    var dvsId = processResponse.getVariables().getDvsOrderId();
                    var taskId = processResponse.getTaskId();

                    var start = processResponse.getVariables().getCreateCardCreditRequest().getStartDate();
                    var end = processResponse.getVariables().getCreateCardCreditRequest().getEndDate();
                    operationEntity.setLoanContractStartDate(start);
                    operationEntity.setLoanContractEndDate(end);
                    operationEntity.setTaskId(taskId);
                    operationEntity.setDvsOrderId(dvsId);
                    operationRepository.save(operationEntity);

                    DvsGetDetailsResponse dvsGetDetailsResponse = verificationService.getDetails(trackId, dvsId)
                            .orElseThrow(() -> new RuntimeException("DVS order getDetails response is null"));
                    sendDecision(UmicoDecisionStatus.PREAPPROVED, trackId, dvsGetDetailsResponse.getWebUrl());
                } catch (Exception e) {
                    optimusClient.deleteLoan(businessKey);
                    var telesalesOrderId = telesalesService.sendLead(trackId);
                    updateOperationTelesalesOrderId(trackId, telesalesOrderId);
                    sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
                }
            }
        } else if (scoringResultEvent.getProcessStatus()
                .equals(ProcessStatus.INCIDENT_HAPPENED) || scoringResultEvent.getProcessStatus()
                .equals(ProcessStatus.BUSINESS_ERROR)) {
            log.error("Optimus incident or business error happened , processStatus - {}",
                    scoringResultEvent.getProcessStatus());
            var telesalesOrderId = telesalesService.sendLead(trackId);
            updateOperationTelesalesOrderId(trackId, telesalesOrderId);
            sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
        }

    }

    @Transactional
    public void sendDecision(UmicoDecisionStatus umicoDecisionStatus, UUID trackId, String dvsUrl) {
        var operationEntity = operationRepository.findById(trackId).orElseThrow(
                () -> new OperationNotFoundException("trackId - " + trackId));
        operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        UmicoDecisionRequest umicoScoringDecisionRequest = UmicoDecisionRequest.builder()
                .trackId(trackId).dvsUrl(dvsUrl).decisionStatus(umicoDecisionStatus)
                .loanTerm(operationEntity.getLoanTerm()).build();
        log.info("product create send decision.track_id - [{}], Request - [{}]",
                trackId, umicoScoringDecisionRequest);
        try {
            UmicoDecisionResponse umicoScoringDecisionResponse = umicoClient
                    .sendDecisionToUmico(umicoScoringDecisionRequest, apiKey);
            log.info("product create send decision.track_id - [{}], Response - [{}]",
                    trackId, umicoScoringDecisionResponse);
        } catch (FeignClientException e) {
            log.error("product create send decision. track_id - [{}], FeignException - {}", trackId, e.getMessage());
        }
    }

    @Transactional
    void updateOperationTelesalesOrderId(UUID trackId, Optional<String> telesalesOrderId) {
        var operationEntityOptional = operationRepository.findById(trackId);
        if (operationEntityOptional.isPresent() && telesalesOrderId.isPresent()) {
            operationEntityOptional.get().setTelesalesOrderId(telesalesOrderId.get());
            operationRepository.save(operationEntityOptional.get());
        }
    }
}