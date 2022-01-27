package az.kapitalbank.marketplace.service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderRequest;
import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderResponse;
import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessData;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionResponse;
import az.kapitalbank.marketplace.constants.FraudResultStatus;
import az.kapitalbank.marketplace.constants.ProcessStatus;
import az.kapitalbank.marketplace.constants.ScoringStatus;
import az.kapitalbank.marketplace.constants.TaskDefinitionKey;
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
import az.kapitalbank.marketplace.repository.OrderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductCreateService {

    @Value("${umico.api-key}")
    String apiKey;

    OperationRepository operationRepository;
    final OrderRepository orderRepository;
    final CustomerRepository customerRepository;

    final ScoringService scoringService;
    final VerificationService verificationService;
    final TelesalesService telesalesService;

    final UmicoClient umicoClient;

    final LoanFormalizeMapper loanFormalizeMapper;


    @Transactional
    public void startScoring(FraudCheckResultEvent fraudCheckResultEvent) {
        log.info("product create start... message - {}", fraudCheckResultEvent);
        var trackId = fraudCheckResultEvent.getTrackId();

        if (fraudCheckResultEvent.getFraudResultStatus() == FraudResultStatus.BLACKLIST) {
            log.info("product create customer is at blacklist. track_id - [{}]", trackId);
            sendDecision(ScoringStatus.REJECTED, trackId, "", "");
            return;
        }

        var operationEntityOptional = operationRepository.findById(trackId);
        if (operationEntityOptional.isPresent()) {
            var customerEntity = operationEntityOptional.get().getCustomer();
            var operationEntity = operationEntityOptional.get();
            try {
                Optional<String> businessKey = scoringService.startScoring(trackId,
                        customerEntity.getPin(),
                        customerEntity.getMobileNumber());
                if (businessKey.isPresent()) {
                    operationEntity.setBusinessKey(businessKey.get());
                    operationRepository.save(operationEntity);
                    log.info("product create start-scoring. track_id - [{}], business_key - [{}]",
                            trackId,
                            businessKey);
                }
            } catch (ScoringCustomerException e) {
                log.error("product create complete-scoring finish. Redirect to telesales track_id - [{}]", trackId);
                telesalesService.sendLead(trackId);
            }
        }

    }

    @Transactional
    public void createScoring(ScoringResultEvent scoringResultEvent) {
        var businessKey = scoringResultEvent.getBusinessKey();
        var operationEntity = operationRepository.findByBusinessKey(businessKey);
        if (operationEntity.isPresent()) {
            var trackId = operationEntity.get().getId();
            CustomerEntity customerEntity = operationEntity.get().getCustomer();
            if (scoringResultEvent.getProcessStatus().equals(ProcessStatus.IN_USER_ACTIVITY)) {
                InUserActivityData inUserActivityData = (InUserActivityData) scoringResultEvent.getData();
                Optional<ProcessResponse> processResponse = scoringService.getProcess(trackId, businessKey);
                ProcessData processData = processResponse.get().getVariables();
                var taskId = processResponse.get().getTaskId();
                String taskDefinitionKey = inUserActivityData.getTaskDefinitionKey();
                if (taskDefinitionKey.equalsIgnoreCase(TaskDefinitionKey.USER_TASK_SCORING)) {
                    scoringService.createScoring(trackId, taskId,
                            processData.getSelectedOffer().getCashOffer().getAvailableLoanAmount());
                } else if (taskDefinitionKey.equalsIgnoreCase(TaskDefinitionKey.USER_TASK_SIGN_DOCUMENTS)) {
                    DvsCreateOrderRequest dvsCreateOrderRequest = loanFormalizeMapper
                            .toDvsCreateOrderRequest(customerEntity, processResponse.get());
                    Optional<DvsCreateOrderResponse> dvsCreateOrderResponse = verificationService
                            .createOrder(dvsCreateOrderRequest, trackId);
                    String dvsId = String.valueOf(processData.getDvsOrderId());
                    Optional<DvsGetDetailsResponse> dvsGetDetailsResponse = verificationService.getDetails(trackId,
                            processResponse.get().getTaskId(),
                            dvsId);
                    if (dvsGetDetailsResponse.isPresent()) {
                        sendDecision(ScoringStatus.APPROVED,
                                trackId,
                                dvsId,
                                dvsGetDetailsResponse.get().getWebUrl());
                    }
                }
            } else if (scoringResultEvent.getProcessStatus().equals(ProcessStatus.INCIDENT_HAPPENED)) {
                telesalesService.sendLead(trackId);
            }
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
            telesalesService.sendLead(trackId);
        }
    }


    @Transactional
    public void sendDecision(ScoringStatus scoringStatus, UUID trackId, String dvsId, String dvsUrl) {
        var operationEntityOptional = operationRepository.findById(trackId);
        if (operationEntityOptional.isPresent()) {
            var operationEntity = operationEntityOptional.get();
            operationEntity.setScoringStatus(scoringStatus);
            operationEntity.setScoringDate(LocalDateTime.now());
            UmicoScoringDecisionRequest umicoScoringDecisionRequest = UmicoScoringDecisionRequest.builder()
                    .trackId(trackId)
                    .scoringStatus(scoringStatus.name())
                    .loanTerm(operationEntity.getLoanTerm())
                    .build();
            if (scoringStatus == ScoringStatus.APPROVED) {
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
    }
}