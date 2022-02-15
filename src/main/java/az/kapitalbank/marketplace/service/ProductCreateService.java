package az.kapitalbank.marketplace.service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderRequest;
import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderResponse;
import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;
import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessData;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionResponse;
import az.kapitalbank.marketplace.constants.FraudResultStatus;
import az.kapitalbank.marketplace.constants.ProcessStatus;
import az.kapitalbank.marketplace.constants.TaskDefinitionKey;
import az.kapitalbank.marketplace.constants.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.exception.FeignClientException;
import az.kapitalbank.marketplace.exception.ScoringCustomerException;
import az.kapitalbank.marketplace.mappers.LoanFormalizeMapper;
import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.messaging.event.InUserActivityData;
import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
import az.kapitalbank.marketplace.repository.CustomerRepository;
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
    CustomerRepository customerRepository;
    ScoringService scoringService;
    VerificationService verificationService;
    TelesalesService telesalesService;
    UmicoClient umicoClient;
    OptimusClient optimusClient;
    LoanFormalizeMapper loanFormalizeMapper;


    @Transactional
    public void startScoring(FraudCheckResultEvent fraudCheckResultEvent) {
        log.info("ProductCreateService: optimus start process... fraud_result_event - {}", fraudCheckResultEvent);
        var trackId = fraudCheckResultEvent.getTrackId();
        var fraudResultStatus = fraudCheckResultEvent.getFraudResultStatus();

        if (fraudResultStatus == FraudResultStatus.BLACKLIST) {
            log.info("ProductCreateService: this order was found in blacklist. track_id - [{}]", trackId);
            sendDecision(UmicoDecisionStatus.DECLINED_BY_BLACKLIST, trackId, "", "");
            return;
        }

        if (fraudResultStatus == FraudResultStatus.SUSPICIOUS || fraudResultStatus == FraudResultStatus.WARNING) {
            log.info("ProductCreateService: fraud case was found in order. track_id - [{}]", trackId);
            var telesalesOrderId = telesalesService.sendLead(trackId);
            updateOperationTelesalesOrderId(trackId, telesalesOrderId);
            sendDecision(UmicoDecisionStatus.PENDING, trackId, "", "");
            return;
        }

        var operationEntityOptional = operationRepository.findById(trackId);
        if (operationEntityOptional.isPresent()) {
            var operationEntity = operationEntityOptional.get();
            try {
                Optional<String> businessKey = scoringService.startScoring(trackId,
                        operationEntity.getPin(),
                        operationEntity.getMobileNumber());
                if (businessKey.isPresent()) {
                    operationEntity.setBusinessKey(businessKey.get());
                    operationRepository.save(operationEntity);
                    log.info("ProductCreateService: optimus stared scoring.track_id - [{}], business_key - [{}]",
                            trackId,
                            businessKey);
                }
            } catch (ScoringCustomerException e) {
                log.error("ProductCreateService: " +
                        "optimus start scoring was failed.Redirect to telesales track_id - [{}]", trackId);
                var telesalesOrderId = telesalesService.sendLead(trackId);
                updateOperationTelesalesOrderId(trackId, telesalesOrderId);
                sendDecision(UmicoDecisionStatus.PENDING, trackId, "", "");
            }
        }

    }

    @Transactional
    public void createScoring(ScoringResultEvent scoringResultEvent) {
        var businessKey = scoringResultEvent.getBusinessKey();
        var operationEntity = operationRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new RuntimeException("Operation not found"));
        var trackId = operationEntity.getId();
        if (scoringResultEvent.getProcessStatus().equals(ProcessStatus.IN_USER_ACTIVITY)) {
            var processResponse = scoringService.getProcess(trackId, businessKey)
                    .orElseThrow(() -> new RuntimeException("Optimus getProcess is null"));
            InUserActivityData inUserActivityData = (InUserActivityData) scoringResultEvent.getData();
            String taskDefinitionKey = inUserActivityData.getTaskDefinitionKey();
            if (taskDefinitionKey.equalsIgnoreCase(TaskDefinitionKey.USER_TASK_SCORING)) {
                var taskId = processResponse.getTaskId();
                ProcessData processData = processResponse.getVariables();
                var scoredAmount = processData.getSelectedOffer().getCashOffer().getAvailableLoanAmount();
                if (scoredAmount.compareTo(operationEntity.getTotalAmount().add(operationEntity.getCommission())) < 0) {
                    var telesalesOrderId = telesalesService.sendLead(trackId);
                    updateOperationTelesalesOrderId(trackId, telesalesOrderId);
                    sendDecision(UmicoDecisionStatus.PENDING, trackId, "", "");
                    return;
                }
                scoringService.createScoring(trackId, taskId, scoredAmount);
                operationEntity.setTaskId(taskId);
                operationRepository.save(operationEntity);
            } else if (taskDefinitionKey.equalsIgnoreCase(TaskDefinitionKey.USER_TASK_SIGN_DOCUMENTS)) {
                try {
                    CustomerEntity customerEntity = operationEntity.getCustomer();
                    DvsCreateOrderRequest dvsCreateOrderRequest = loanFormalizeMapper
                            .toDvsCreateOrderRequest(customerEntity,
                                    processResponse,
                                    operationEntity.getPin(),
                                    operationEntity.getMobileNumber());
                    DvsCreateOrderResponse dvsCreateOrderResponse = verificationService
                            .createOrder(dvsCreateOrderRequest, trackId)
                            .orElseThrow(() -> new RuntimeException("DVS create order response is null"));
                    String dvsId = dvsCreateOrderResponse.getOrderId();
                    operationEntity.setDvsOrderId(dvsId);
                    operationRepository.save(operationEntity);
                    DvsGetDetailsResponse dvsGetDetailsResponse = verificationService.getDetails(trackId,
                                    processResponse.getTaskId(),
                                    dvsId)
                            .orElseThrow(() -> new RuntimeException("DVS order getDetails response is null"));
                    sendDecision(UmicoDecisionStatus.PREAPPROVED,
                            trackId,
                            dvsId,
                            dvsGetDetailsResponse.getWebUrl());
                } catch (Exception e) {
                    optimusClient.deleteLoan(businessKey);
                    var telesalesOrderId = telesalesService.sendLead(trackId);
                    updateOperationTelesalesOrderId(trackId, telesalesOrderId);
                    sendDecision(UmicoDecisionStatus.PENDING, trackId, "", "");
                }
            }
        } else if (scoringResultEvent.getProcessStatus().equals(ProcessStatus.INCIDENT_HAPPENED)) {
            var telesalesOrderId = telesalesService.sendLead(trackId);
            updateOperationTelesalesOrderId(trackId, telesalesOrderId);
            sendDecision(UmicoDecisionStatus.PENDING, trackId, "", "");
        }

    }


    public void completeScoring(UUID trackId, String taskId) {
        try {
            var customerEntity = customerRepository.findById(trackId);
            if (customerEntity.isPresent()) {
                var additionalPhoneNumber1 = customerEntity.get().getAdditionalPhoneNumber1();
                var additionalPhoneNumber2 = customerEntity.get().getAdditionalPhoneNumber2();
                CompleteScoring completeScoring = CompleteScoring.builder()
                        .trackId(trackId)
                        .businessKey(taskId)
                        .additionalNumber1(additionalPhoneNumber1)
                        .additionalNumber2(additionalPhoneNumber2)
                        .build();
                scoringService.completeScoring(completeScoring);
                log.info("product create create-scoring finish... track_id - [{}]", trackId);
            }
        } catch (ScoringCustomerException e) {
            log.error("product create create-scoring finish. Redirect to telesales track_id - [{}]", trackId);
            var telesalesOrderId = telesalesService.sendLead(trackId);
            updateOperationTelesalesOrderId(trackId, telesalesOrderId);
            sendDecision(UmicoDecisionStatus.PENDING, trackId, "", "");
        }
    }


    @Transactional
    public void sendDecision(UmicoDecisionStatus umicoDecisionStatus, UUID trackId, String dvsId, String dvsUrl) {
        var operationEntity = operationRepository.findById(trackId)
                .orElseThrow(() -> new RuntimeException("Operation not found: " + trackId));
        operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        operationEntity.setScoringDate(LocalDateTime.now());
        UmicoScoringDecisionRequest umicoScoringDecisionRequest = UmicoScoringDecisionRequest.builder()
                .trackId(trackId)
                .decisionStatus(umicoDecisionStatus)
                .loanTerm(operationEntity.getLoanTerm())
                .build();
        if (umicoDecisionStatus == UmicoDecisionStatus.APPROVED) {
            umicoScoringDecisionRequest.setDvsUrl(dvsUrl);
            operationEntity.setDvsOrderId(dvsId);
        }
        log.info("product create send decision.track_id - [{}], Request - [{}]", trackId,
                umicoScoringDecisionRequest);
        try {
            UmicoScoringDecisionResponse umicoScoringDecisionResponse = umicoClient
                    .sendDecisionScoring(umicoScoringDecisionRequest, apiKey);
            log.info("product create send decision.track_id - [{}], Response - [{}]",
                    trackId,
                    umicoScoringDecisionResponse);
        } catch (FeignClientException e) {
            log.error("product create send decision. track_id - [{}], FeignException - {}",
                    trackId,
                    e.getMessage());
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