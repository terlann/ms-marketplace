package az.kapitalbank.marketplace.service.impl;

import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringVariable;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CreateScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CompleteScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerDecision;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerNumber;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerContact;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionResponse;
import az.kapitalbank.marketplace.constants.AdpOptimusLevels;
import az.kapitalbank.marketplace.constants.OrderScoringStatus;
import az.kapitalbank.marketplace.constants.UmicoOrderStatus;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.dto.request.ScoringOrderRequestDto;
import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.exception.models.OrderNotFindException;
import az.kapitalbank.marketplace.exception.models.OrderIsInactiveException;
import az.kapitalbank.marketplace.exception.models.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.models.ChangeScoringStatusException;
import az.kapitalbank.marketplace.exception.models.ScoringCustomerException;
import az.kapitalbank.marketplace.exception.models.FeignClientException;
import az.kapitalbank.marketplace.mappers.ScoringMapper;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.service.ScoringService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoringServiceImpl implements ScoringService {

    @Value("${umico.api-key}")
    private String apiKey;

    @Value("${optimus.process.key}")
    private String processKey;
    @Value("${optimus.process.product-type}")
    private String productType;

    final OrderRepository orderRepository;
    final UmicoClient umicoClient;
    final OptimusClient optimusClient;
    final ScoringMapper scoringMapper;

    @Override
    @Transactional
    public ResponseEntity scoringOrder(ScoringOrderRequestDto request) {
        String eteOrderId = request.getEteOrderId().trim();
        log.info("scoring order start... ete_order_id - [{}]", eteOrderId);
        Optional<OrderEntity> orderEntity = orderRepository.findByEteOrderId(eteOrderId);
        orderEntity.orElseThrow(() -> new OrderNotFindException(String.format("ete_order_id - [%S]", eteOrderId)));
        log.info("scoring order find order in db. ete_order_id - [{}]", eteOrderId);
        orderEntity
                .filter(o -> {
                    if (o.getIsActive() == 0) {
                        log.error("scoring order is inactive. ete_order_id - [{}]", eteOrderId);
                        throw new OrderIsInactiveException(String.format("ete_order_id - [%s]", eteOrderId));
                    }
                    if (o.getScoringStatus() != null) {
                        log.error("scoring order is already have been score. ete_order_id - [{}]", eteOrderId);
                        throw new OrderAlreadyScoringException(eteOrderId);
                    }
                    return true;
                });

        var creditId = request.getCreditId();

        if (creditId != null) {
            creditId = creditId.trim();
        }

        OrderEntity marketplaceOrderEntity = orderEntity.get();
        marketplaceOrderEntity.setScoringDate(LocalDateTime.now());
        marketplaceOrderEntity.setScoringStatus(request.getScoringStatus().getStatus());
        if (request.getScoringStatus().equals(OrderScoringStatus.APPROVED)) {
            marketplaceOrderEntity.setCreditId(creditId);
            marketplaceOrderEntity.setLoanStartDate(request.getLoanStartDate());
            marketplaceOrderEntity.setLoanEndDate(request.getLoanEndDate());
        }
        Optional.of(marketplaceOrderEntity)
                .map(OrderEntity::getIsActive)
                .filter(isActive -> isActive == 1)
                .orElseThrow(() -> new ChangeScoringStatusException(eteOrderId));
        try {
            sendDecisionScoring(marketplaceOrderEntity, eteOrderId, request.getScoringStatus().getStatus());
        } catch (Exception e) {
            log.error("scoring order dont send the decision to umico. ete_order_id - [{}],Exception - ", eteOrderId, e);
        }

        log.info("scoring order saving the result. ete_order_id - [{}],track_id - [{}]",
                eteOrderId,
                marketplaceOrderEntity.getId());
        log.info("scoring order find finish... ete_order_id - [{}]", eteOrderId);

        WrapperResponseDto<Object> wrapperResponseDto = WrapperResponseDto.ofSuccess();
        return ResponseEntity.ok(wrapperResponseDto);
    }


    @Transactional
    public void sendDecisionScoring(OrderEntity orderEntity, String eteOrderId, Integer result) {
        try {
            var status = result == 1 ? UmicoOrderStatus.Approved : UmicoOrderStatus.Declined;
            var trackId = orderEntity.getId();
            UmicoScoringDecisionRequest umicoScoringDecisionRequest = UmicoScoringDecisionRequest.builder()
                    .orderId(eteOrderId)
                    .status(status.toString())
                    .marketplaceTrackId(trackId)
                    .build();
            log.info("scoring order send decision to umico. Request - [{}] ",
                    umicoScoringDecisionRequest.toString());
            UmicoScoringDecisionResponse umicoScoringDecisionResponse = umicoClient
                    .sendDecisionScoring(umicoScoringDecisionRequest, apiKey);
            log.info("scoring order send decision to umico. Response - [{}] , ete_order_id - [{}]",
                    umicoScoringDecisionResponse.toString(),
                    eteOrderId);
            if (umicoScoringDecisionResponse.getHttpStatus() == 201) {
                orderEntity.setSendDecisionScoring(1);
                orderEntity.setSendDecisionScoringDate(LocalDateTime.now());
            }
            orderRepository.save(orderEntity);
        } catch (FeignClientException e) {
            log.error("send decision scoring to umico. ete_order_id - [{}] " +
                        ",FeignException - {}", eteOrderId, e.getMessage());
        }
    }

    @Override
    public Optional<String> startScoring(String trackId, String pinCode, String phoneNumber) {
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

    @Override
    public void createScoring(String trackId, String businessKey, BigDecimal loanAmount) {
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

    @Override
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

    @Override
    public Optional<ProcessResponse> getProcess(String trackId, String businessKey) {
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
