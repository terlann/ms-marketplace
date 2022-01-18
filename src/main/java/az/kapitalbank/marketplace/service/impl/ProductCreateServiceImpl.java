package az.kapitalbank.marketplace.service.impl;

import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderRequest;
import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderResponse;
import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessData;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionResponse;
import az.kapitalbank.marketplace.constants.FraudResultStatus;
import az.kapitalbank.marketplace.constants.OrderScoringStatus;
import az.kapitalbank.marketplace.constants.ProcessStatus;
import az.kapitalbank.marketplace.constants.TaskDefinitionKey;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.exception.models.FeignClientException;
import az.kapitalbank.marketplace.exception.models.ScoringCustomerException;
import az.kapitalbank.marketplace.mappers.LoanFormalizeMapper;
import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.messaging.event.InUserActivityData;
import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.service.ProductCreateService;
import az.kapitalbank.marketplace.service.ScoringService;
import az.kapitalbank.marketplace.service.TelesalesService;
import az.kapitalbank.marketplace.service.VerificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductCreateServiceImpl implements ProductCreateService {

    @Value("${umico.api-key}")
    String apiKey;

    final OrderRepository orderRepository;
    final CustomerRepository customerRepository;

    final ScoringService scoringService;
    final VerificationService verificationService;
    final TelesalesService telesalesService;

    final UmicoClient umicoClient;

    final LoanFormalizeMapper loanFormalizeMapper;


    @Transactional
    @Override
    public void startScoring(FraudCheckResultEvent fraudCheckResultEvent) {
        log.info("product create start... message - {}", fraudCheckResultEvent);
        var trackId = fraudCheckResultEvent.getTrackId();

        if (fraudCheckResultEvent.getFraudResultStatus() == FraudResultStatus.BLACKLIST) {
            log.info("product create customer is at blacklist. track_id - [{}]", trackId);
            sendDecision(OrderScoringStatus.REJECTED, trackId, "", "");
            return;
        }
        CustomerEntity customerEntity = customerRepository.getById(trackId);
        OrderEntity orderEntity = orderRepository.findById(trackId).get();
        try {
            Optional<String> businessKey = scoringService.startScoring(trackId,
                    customerEntity.getIdentityNumber(),
                    customerEntity.getMobileNumber());
            if (businessKey.isPresent()) {
                orderEntity.setBusinessKey(businessKey.get());
                orderRepository.save(orderEntity);
                log.info("product create start-scoring. track_id - [{}], business_key - [{}]",
                        trackId,
                        businessKey);
            }
        } catch (ScoringCustomerException e) {
            log.error("product create complete-scoring finish. Redirect to telesales track_id - [{}]", trackId);
            telesalesService.sendLead(trackId);
        }
    }

    @Transactional
    @Override
    public void createScoring(ScoringResultEvent scoringResultEvent) {
        var businessKey = scoringResultEvent.getBusinessKey();
        Optional<OrderEntity> orderEntity = orderRepository.findByBusinessKey(businessKey);
        if (orderEntity.isPresent()) {
            var trackId = orderEntity.get().getId();
            CustomerEntity customerEntity = orderEntity.get().getCustomer();
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
                        sendDecision(OrderScoringStatus.APPROVED,
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


    @Override
    public void completeScoring(String trackId, String taskId) {
        try {
            CustomerEntity customerEntity = customerRepository.findById(trackId).get();
            var additionalPhoneNumber1 = customerEntity.getAdditionalPhoneNumber1();
            var additionalPhoneNumber2 = customerEntity.getAdditionalPhoneNumber2();
            CompleteScoring completeScoring = CompleteScoring.builder()
                    .trackId(trackId)
                    .businessKey(taskId)
                    .additionalNumber1(additionalPhoneNumber1)
                    .additionalNumber2(additionalPhoneNumber2)
                    .build();
            scoringService.completeScoring(completeScoring);
            log.info("product create create-scoring finish... track_id - [{}]", trackId);
        } catch (ScoringCustomerException e) {
            log.error("product create create-scoring finish. Redirect to telesales track_id - [{}]", trackId);
            telesalesService.sendLead(trackId);
        }
    }


    @Transactional
    public void sendDecision(OrderScoringStatus orderScoringStatus, String trackId, String dvsId, String dvsUrl) {
        OrderEntity orderEntity = orderRepository.findById(trackId).get();
        orderEntity.setScoringStatus(orderScoringStatus.getStatus());
        orderEntity.setScoringDate(LocalDateTime.now());
        UmicoScoringDecisionRequest umicoScoringDecisionRequest = UmicoScoringDecisionRequest.builder()
                .marketplaceTrackId(trackId)
                .status(orderScoringStatus.toString())
                .build();
        if (orderScoringStatus == OrderScoringStatus.APPROVED) {
            umicoScoringDecisionRequest.setDvsUrl(dvsUrl);
            umicoScoringDecisionRequest.setDvsId(dvsId);
            orderEntity.setDvsOrderId(dvsId);
        }
        log.info("product create send decision.track_id - [{}], Request - [{}]", trackId,
                umicoScoringDecisionRequest);
        try {
            UmicoScoringDecisionResponse umicoScoringDecisionResponse = umicoClient
                    .sendDecisionScoring(umicoScoringDecisionRequest, apiKey);
            log.info("product create send decision.track_id - [{}], Response - [{}]",
                    trackId,
                    umicoScoringDecisionResponse);
            if (umicoScoringDecisionResponse.getHttpStatus() == 201) {
                orderEntity.setSendDecisionScoring(1);
                orderEntity.setSendDecisionScoringDate(LocalDateTime.now());
            }
            orderRepository.save(orderEntity);
        } catch (FeignClientException e) {
            log.error("product create send decision. track_id - [{}], FeignException - {}",
                    trackId,
                    e.getMessage());
        }
    }


}