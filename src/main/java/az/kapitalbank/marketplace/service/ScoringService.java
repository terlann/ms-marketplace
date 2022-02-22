package az.kapitalbank.marketplace.service;

import java.math.BigDecimal;
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
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionResponse;
import az.kapitalbank.marketplace.constant.ScoringLevel;
import az.kapitalbank.marketplace.constant.TelesalesResult;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.dto.request.TelesalesResultRequestDto;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.exception.FeignClientException;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.exception.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.exception.ScoringException;
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
    public void telesalesResult(TelesalesResultRequestDto request) {
        String telesalesOrderId = request.getTelesalesOrderId().trim();
        log.info("telesales loan result start... telesales_order_id - [{}]", telesalesOrderId);
        var exceptionMessage = String.format("telesales_order_id - [%S]", telesalesOrderId);
        var operationEntity = operationRepository.findByTelesalesOrderId(telesalesOrderId)
                .orElseThrow(() -> new OrderNotFoundException(exceptionMessage));
        log.info("telesales_order_id found in db. telesales_order_id - [{}]", telesalesOrderId);

        if (operationEntity.getScoringLevel() != null) {
            log.error("Order already have scored. telesales_order_id - [{}]",
                    telesalesOrderId);
            throw new OrderAlreadyScoringException(telesalesOrderId);
        }

        if (request.getTelesalesResult() == TelesalesResult.APPROVED) {
            var cardUid = request.getCardPan(); // TODO optimus send me pan and change to cardUid
            operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.APPROVED);
            operationEntity.setScoringLevel(ScoringLevel.COMPLETE);
            operationEntity.getCustomer().setCardId(cardUid);
            operationEntity.setLoanContractStartDate(request.getLoanContractStartDate()); // TODO optimus send me
            operationEntity.setLoanContractEndDate(request.getLoanContractEndDate()); // TODO optimus send me
        } else {
            operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.REJECTED);
            operationEntity.setScoringLevel(ScoringLevel.REJECT);
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
            UmicoDecisionRequest umicoScoringDecisionRequest = UmicoDecisionRequest.builder()
                    .trackId(operationEntity.getId())
                    .decisionStatus(operationEntity.getUmicoDecisionStatus())
                    .loanContractStartDate(null) // TODO ?
                    .loanContractEndDate(null)  // TODO ?
                    .customerId(operationEntity.getCustomer().getId())
                    .commission(operationEntity.getCommission())
                    .loanLimit(operationEntity.getTotalAmount().add(operationEntity.getCommission()))
                    .loanTerm(operationEntity.getLoanTerm())
                    .build();
            log.info("scoring order send decision to umico. Request - [{}] ",
                    umicoScoringDecisionRequest.toString());
            UmicoDecisionResponse umicoScoringDecisionResponse = umicoClient
                    .sendDecisionToUmico(umicoScoringDecisionRequest, apiKey);
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
        StartScoringVariable startScoringVariable = scoringMapper.toStartScoringVariable(pinCode,
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

    public void createScoring(UUID trackId, String taskId, BigDecimal loanAmount) {
        log.info("scoring service create-scoring start... track_id - [{}]", trackId);
        CreateScoringRequest createScoringRequest = CreateScoringRequest.builder()
                .cardDemandedAmount(loanAmount.toString())
                .salesSource("umico_marketplace")
                .customerDecision(CustomerDecision.CREATE_CREDIT)
                .build();
        log.info("scoring service create-scoring. track_id - [{}], Request - {}", trackId, createScoringRequest);
        try {
            optimusClient.scoringCreate(taskId, createScoringRequest);
            var operationEntity = operationRepository.findById(trackId)
                    .orElseThrow(() -> new OperationNotFoundException("trackId - " + trackId));
            operationEntity.setScoringLevel(ScoringLevel.CREATE);
            operationRepository.save(operationEntity);
            log.info("scoring service create-scoring finish... track_id - [{}]", trackId);
        } catch (FeignClientException e) {
            log.error("scoring service create-scoring finish... track_id - [{}],FeignException - {}",
                    trackId,
                    e.getMessage());
            throw new ScoringException(trackId, ScoringLevel.CREATE);
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
                .customerDecision(completeScoring.getCustomerDecision())
                .build();

        log.info("scoring service complete-scoring. track_id - [{}], Request - {}", trackId, completeScoringRequest);
        try {
            optimusClient.scoringComplete(completeScoring.getTaskId(), completeScoringRequest);
            log.info("scoring service complete-scoring finish... track_id - [{}]", trackId);
        } catch (FeignClientException f) {
            log.error("scoring service complete-scoring finish... track_id - [{}],FeignException - {}",
                    trackId,
                    f.getMessage());
            throw new ScoringException(trackId, ScoringLevel.COMPLETE);
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
