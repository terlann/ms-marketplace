package az.kapitalbank.marketplace.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CompleteScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CreateScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerContact;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerDecision;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerNumber;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringVariable;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionResponse;
import az.kapitalbank.marketplace.constants.AdpOptimusLevels;
import az.kapitalbank.marketplace.constants.ScoringStatus;
import az.kapitalbank.marketplace.constants.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.dto.request.ScoringOrderRequestDto;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.exception.FeignClientException;
import az.kapitalbank.marketplace.exception.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.exception.ScoringCustomerException;
import az.kapitalbank.marketplace.mappers.ScoringMapper;
import az.kapitalbank.marketplace.repository.OperationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScoringService {

    @NonFinal
    @Value("${umico.api-key}")
    String apiKey;
    @NonFinal
    @Value("${optimus.process.key}")
    String processKey;
    @NonFinal
    @Value("${optimus.process.product-type}")
    String productType;

    OperationRepository operationRepository;
    UmicoClient umicoClient;
    OptimusClient optimusClient;
    ScoringMapper scoringMapper;

    @Transactional
    public void telesalesResult(ScoringOrderRequestDto request) {
        String telesalesOrderId = request.getTelesalesOrderId().trim();
        log.info("telesales loan result start... telesales_order_id - [{}]", telesalesOrderId);
        var exceptionMessage = String.format("telesales_order_id - [%S]", telesalesOrderId);
        var operationEntity = operationRepository.findByTelesalesOrderId(telesalesOrderId)
                .orElseThrow(() -> new OrderNotFoundException(exceptionMessage));
        log.info("telesales_order_id found in db. telesales_order_id - [{}]", telesalesOrderId);

        if (operationEntity.getScoringStatus() != null) {
            log.error("Order already have scored. telesales_order_id - [{}]",
                    telesalesOrderId);
            throw new OrderAlreadyScoringException(telesalesOrderId);
        }

        var umicoDecisionStatus = request.getScoringStatus() == ScoringStatus.APPROVED ?
                UmicoDecisionStatus.APPROVED : UmicoDecisionStatus.REJECTED;
        operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        operationEntity.setScoringStatus(request.getScoringStatus());
        operationEntity.setScoringDate(LocalDateTime.now());
        var cardUid = request.getCardPan(); // TODO optimus send me pan and change to cardUid
        if (request.getScoringStatus().equals(ScoringStatus.APPROVED)) {
            operationEntity.getCustomer().setCardUUID(cardUid);
            operationEntity.setLoanContractStartDate(request.getLoanContractStartDate()); // TODO optimus send me
            operationEntity.setLoanContractEndDate(request.getLoanContractEndDate()); // TODO optimus send me
        }
        operationRepository.save(operationEntity);

        //TODO purchase all orders
        try {
            sendDecisionScoring(operationEntity);
        } catch (Exception e) {
            log.error("scoring order dont send the decision to umico. ete_order_id - [{}],Exception - ",
                    telesalesOrderId, e);
        }
        log.info("telesales loan result finished... telesales_order_id - [{}]", telesalesOrderId);
    }


    public void sendDecisionScoring(OperationEntity operationEntity) {
        try {
            UmicoScoringDecisionRequest umicoScoringDecisionRequest = UmicoScoringDecisionRequest.builder()
                    .trackId(operationEntity.getId())
                    .decisionStatus(operationEntity.getUmicoDecisionStatus())
//                    .loanContractStartDate()
//                    .loanContractEndDate()
                    .customerId(operationEntity.getCustomer().getId())
                    .commission(operationEntity.getCommission())
                    .loanLimit(operationEntity.getTotalAmount())
                    .loanTerm(operationEntity.getLoanTerm())
                    .build();
            log.info("scoring order send decision to umico. Request - [{}] ",
                    umicoScoringDecisionRequest.toString());
            UmicoScoringDecisionResponse umicoScoringDecisionResponse = umicoClient
                    .sendDecisionScoring(umicoScoringDecisionRequest, apiKey);
            log.info("scoring order send decision to umico. Response - [{}] , ete_order_id - [{}]",
                    umicoScoringDecisionResponse.toString(),
                    operationEntity.getTelesalesOrderId());
        } catch (FeignClientException e) {
            log.error("send decision scoring to umico. ete_order_id - [{}] " +
                    ",FeignException - {}", operationEntity.getTelesalesOrderId(), e.getMessage());
        }
    }

    public Optional<String> startScoring(UUID trackId, String pinCode, String phoneNumber) {
        log.info("scoring service customer-scoring start... track_id - [{}]", trackId);
        StartScoringVariable startScoringVariable = scoringMapper.toCustomerScoringVariable(pinCode,
                phoneNumber,
                productType);
        StartScoringRequest startScoringRequest = StartScoringRequest.builder()
                .processKey(processKey)
                .variables(startScoringVariable)
                .build();
        log.info("scoring service customer-scoring. track_id - [{}], Request - {}", trackId, startScoringRequest);
        try {
            StartScoringResponse startScoringResponse = optimusClient.scoringStart(startScoringRequest);
            log.info("scoring service customer-scoring finish... track_id - [{}],Response - {}",
                    trackId,
                    startScoringResponse);
            return Optional.of(startScoringResponse.getBusinessKey());
        } catch (FeignClientException e) {
            log.error("scoring service customer-scoring finish... track_id - [{}] ,FeignException - {}",
                    trackId,
                    e.getMessage());
            return Optional.empty();
        }
    }

    public void createScoring(UUID trackId, String businessKey, BigDecimal loanAmount) {
        log.info("scoring service create-scoring start... track_id - [{}]", trackId);
        CreateScoringRequest createScoringRequest = CreateScoringRequest.builder()
                .cashDemandedAmount(loanAmount.toString())
                .customerDecision(CustomerDecision.CREATE_CREDIT)
                .build();
        log.info("scoring service create-scoring. track_id - [{}], Request - {}", trackId, createScoringRequest);
        try {
            optimusClient.scoringCreate(businessKey, createScoringRequest);
            log.info("scoring service create-scoring finish... track_id - [{}]", trackId);
        } catch (FeignClientException e) {
            log.error("scoring service create-scoring finish... track_id - [{}],FeignException - {}",
                    trackId,
                    e.getMessage());
            throw new ScoringCustomerException(trackId, AdpOptimusLevels.CREATE);
        }
    }

    public void completeScoring(CompleteScoring completeScoring) {
        var trackId = completeScoring.getTrackId();
        log.info("scoring service complete-scoring start... track_id - [{}]", trackId);

        List<CustomerNumber> customerNumberList = Arrays.asList(CustomerNumber.builder()
                        .number(CustomerNumber.Number.builder()
                                .number(completeScoring.getAdditionalNumber1())
                                .build())
                        .build(),

                CustomerNumber.builder()
                        .number(CustomerNumber.Number.builder()
                                .number(completeScoring.getAdditionalNumber2())
                                .build())
                        .build());

        CompleteScoringRequest completeScoringRequest = CompleteScoringRequest.builder()
                .customerContact(CustomerContact.builder()
                        .customerNumberList(customerNumberList)
                        .build())
                .customerDecision(CustomerDecision.CONFIRM_CREDIT)
                .build();

        log.info("scoring service complete-scoring. track_id - [{}], Request - {}", trackId, completeScoringRequest);
        try {
            optimusClient.scoringComplete(completeScoring.getBusinessKey(), completeScoringRequest);
            log.info("scoring service complete-scoring finish... track_id - [{}]", trackId);
        } catch (FeignClientException f) {
            log.error("scoring service complete-scoring finish... track_id - [{}],FeignException - {}",
                    trackId,
                    f.getMessage());
            throw new ScoringCustomerException(trackId, AdpOptimusLevels.COMPLETE);
        }
    }

    public Optional<ProcessResponse> getProcess(UUID trackId, String businessKey) {
        log.info("scoring service get process start... track_id - [{}],business_key - [{}]", trackId, businessKey);
        try {
            ProcessResponse processResponse = optimusClient.getProcess(businessKey);
            log.info("scoring service get-process. track_id - [{}], Response - {}", trackId, processResponse);
            return Optional.of(processResponse);
        } catch (FeignClientException f) {
            log.error("scoring service get-process. track_id - [{}], FeignException - {}", trackId, f.getMessage());
        }
        return Optional.empty();
    }
}
