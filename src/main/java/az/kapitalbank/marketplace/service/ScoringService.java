package az.kapitalbank.marketplace.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.client.dvs.DvsClient;
import az.kapitalbank.marketplace.client.dvs.exception.DvsClientException;
import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.exception.OptimusClientException;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CompleteScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CreateScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerContact;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerDecision;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerNumber;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringRequest;
import az.kapitalbank.marketplace.client.umico.UmicoClient;
import az.kapitalbank.marketplace.client.umico.exception.UmicoClientException;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.constant.ApplicationConstant;
import az.kapitalbank.marketplace.constant.Currency;
import az.kapitalbank.marketplace.constant.DvsStatus;
import az.kapitalbank.marketplace.constant.FraudResultStatus;
import az.kapitalbank.marketplace.constant.ScoringStatus;
import az.kapitalbank.marketplace.constant.TaskDefinitionKey;
import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.dto.request.TelesalesResultRequestDto;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.exception.OperationAlreadyScoredException;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.mapper.ScoringMapper;
import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.messaging.event.InUserActivityData;
import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.util.GenerateUtil;
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
    @NonFinal
    @Value("${purchase.terminal-name}")
    String terminalName;

    DvsClient dvsClient;
    UmicoClient umicoClient;
    AtlasClient atlasClient;
    OptimusClient optimusClient;
    ScoringMapper scoringMapper;
    TelesalesService telesalesService;
    CustomerRepository customerRepository;
    OperationRepository operationRepository;

    @Transactional /* Optimus call this */
    public void telesalesResult(TelesalesResultRequestDto request) {
        var telesalesOrderId = request.getTelesalesOrderId().trim();
        log.info("telesales loan result is started... request - {}", request);
        var operationEntity = operationRepository.findByTelesalesOrderId(telesalesOrderId)
                .orElseThrow(() -> new OperationNotFoundException("telesalesOrderId - " + telesalesOrderId));

        if (operationEntity.getScoringStatus() != null)
            throw new OperationAlreadyScoredException("telesalesOrderId - " + telesalesOrderId);

        if (request.getScoringStatus() == ScoringStatus.APPROVED) {
            operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.APPROVED);
            operationEntity.setScoringStatus(ScoringStatus.APPROVED);
            operationEntity.setLoanContractStartDate(request.getLoanStartDate());
            operationEntity.setLoanContractEndDate(request.getLoanEndDate());
            var customerEntity = operationEntity.getCustomer();
            var cardUid = atlasClient.findByPan(request.getPan()).getUid();
            customerEntity.setCardId(cardUid);
            customerEntity.setCompleteProcessDate(LocalDateTime.now());
            operationEntity.setCustomer(customerEntity);
        } else {
            operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.REJECTED);
            operationEntity.setScoringStatus(ScoringStatus.REJECTED);
        }

        var orderEntities = new ArrayList<OrderEntity>();
        for (var orderEntity : operationEntity.getOrders()) {
            var rrn = GenerateUtil.rrn();
            var purchaseRequest = PurchaseRequest.builder()
                    .rrn(rrn)
                    .amount(orderEntity.getTotalAmount().add(orderEntity.getCommission()))
                    .description("fee=" + orderEntity.getCommission())
                    .currency(Currency.AZN.getCode())
                    .terminalName(terminalName)
                    .uid(operationEntity.getCustomer().getCardId())
                    .build();
            var purchaseResponse = atlasClient.purchase(purchaseRequest);
            orderEntity.setRrn(rrn);
            orderEntity.setTransactionId(purchaseResponse.getId());
            orderEntity.setApprovalCode(purchaseResponse.getApprovalCode());
            orderEntity.setTransactionStatus(TransactionStatus.PURCHASE);
            orderEntities.add(orderEntity);
        }
        operationEntity.setOrders(orderEntities);
        operationEntity = operationRepository.save(operationEntity);

        sendDecisionStatus(operationEntity);
        log.info("telesales loan result was finished... telesalesOrderId - {}", telesalesOrderId);
    }


    private void sendDecisionStatus(OperationEntity operationEntity) {
        try {
            var umicoScoringDecisionRequest = UmicoDecisionRequest.builder()
                    .trackId(operationEntity.getId())
                    .decisionStatus(operationEntity.getUmicoDecisionStatus())
                    .loanContractStartDate(operationEntity.getLoanContractStartDate())
                    .loanContractEndDate(operationEntity.getLoanContractEndDate())
                    .customerId(operationEntity.getCustomer().getId())
                    .commission(operationEntity.getCommission())
                    .loanLimit(operationEntity.getTotalAmount().add(operationEntity.getCommission()))
                    .loanTerm(operationEntity.getLoanTerm())
                    .build();
            log.info("Decision status is sent to umico. request - {} ", umicoScoringDecisionRequest);
            var umicoScoringDecisionResponse = umicoClient
                    .sendDecisionToUmico(umicoScoringDecisionRequest, apiKey);
            log.info("Decision status was sent to umico. response - {} , telesalesOrderId - {}",
                    umicoScoringDecisionResponse,
                    operationEntity.getTelesalesOrderId());
        } catch (UmicoClientException e) {
            log.error("Decision status was failed. telesalesOrderId - {} " +
                    ",FeignException - {}", operationEntity.getTelesalesOrderId(), e.getMessage());
        }
    }

    @Transactional
    public void fraudResultProcess(FraudCheckResultEvent fraudCheckResultEvent) {
        log.info("Fraud result process is stared... Message - {}", fraudCheckResultEvent);
        var trackId = fraudCheckResultEvent.getTrackId();
        var fraudResultStatus = fraudCheckResultEvent.getFraudResultStatus();

        if (fraudResultStatus == FraudResultStatus.BLACKLIST) {
            log.warn("This operation was found in blacklist. trackId - {}", trackId);
            sendDecision(UmicoDecisionStatus.DECLINED_BY_BLACKLIST, trackId, null);
            return;
        }

        if (fraudResultStatus == FraudResultStatus.SUSPICIOUS || fraudResultStatus == FraudResultStatus.WARNING) {
            log.warn("Fraud case was found in this operation. trackId - {}", trackId);
            var telesalesOrderId = telesalesService.sendLead(trackId);
            updateOperationTelesalesOrderId(trackId, telesalesOrderId);
            sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
            return;
        }

        var operationEntity = operationRepository.findById(trackId)
                .orElseThrow(() -> new OperationNotFoundException("trackId - " + trackId));

        var businessKey = startScoring(trackId, operationEntity.getPin(), operationEntity.getMobileNumber());
        if (businessKey.isEmpty()) {
            var telesalesOrderId = telesalesService.sendLead(trackId);
            updateOperationTelesalesOrderId(trackId, telesalesOrderId);
            sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
            return;
        }

        operationEntity.setBusinessKey(businessKey.get());
        operationRepository.save(operationEntity);
        log.info("Fraud result process was finished.trackId - {}, businessKey - {}",
                trackId,
                businessKey);
    }

    @Transactional
    public void scoringResultProcess(ScoringResultEvent scoringResultEvent) {
        var businessKey = scoringResultEvent.getBusinessKey();
        var operationEntity = operationRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new OperationNotFoundException("businessKey - " + businessKey));
        var trackId = operationEntity.getId();
        switch (scoringResultEvent.getProcessStatus()) {
            case IN_USER_ACTIVITY:
                inUserActivityProcess(scoringResultEvent, businessKey, operationEntity, trackId);
                break;
            case COMPLETED:
                var cardPan = optimusClient.getProcessVariable(operationEntity.getBusinessKey(),
                        "pan");
                var cardId = atlasClient.findByPan(cardPan).getUid();
                log.info("Pan was taken and changed to card uid.Purchase process starts. cardId - {}",
                        cardId);
                var customerEntity = operationEntity.getCustomer();
                customerEntity.setCardId(cardId);
                customerRepository.save(customerEntity);
                var orders = operationEntity.getOrders();
                var customer = operationEntity.getCustomer();
                for (var order : orders) {
                    var rrn = GenerateUtil.rrn();
                    var purchaseRequest = PurchaseRequest.builder()
                            .rrn(rrn)
                            .amount(order.getTotalAmount().add(order.getCommission()))
                            .description("fee=" + order.getCommission())
                            .currency(Currency.AZN.getCode())
                            .terminalName(terminalName)
                            .uid(customer.getCardId())
                            .build();
                    var purchaseResponse = atlasClient.purchase(purchaseRequest);

                    order.setRrn(rrn);
                    order.setTransactionId(purchaseResponse.getId());
                    order.setApprovalCode(purchaseResponse.getApprovalCode());
                    order.setTransactionStatus(TransactionStatus.PURCHASE);
                    order.setOperation(operationEntity);
                }
                operationEntity.setOrders(orders);
                operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.APPROVED);
                operationEntity.setDvsOrderStatus(DvsStatus.CONFIRMED);
                operationEntity.setScoringDate(LocalDateTime.now());
                operationEntity.setScoringStatus(ScoringStatus.APPROVED);
                customerEntity.setCompleteProcessDate(LocalDateTime.now());
                operationEntity.setCustomer(customerEntity);
                operationRepository.save(operationEntity);
                log.info("Purchased all orders.");

                var umicoApprovedDecisionRequest = UmicoDecisionRequest.builder()
                        .trackId(operationEntity.getId())
                        .commission(operationEntity.getCommission())
                        .customerId(customer.getId())
                        .decisionStatus(UmicoDecisionStatus.APPROVED)
                        .loanTerm(operationEntity.getLoanTerm())
                        .loanLimit(operationEntity.getTotalAmount())
                        .loanContractStartDate(operationEntity.getLoanContractStartDate())
                        .loanContractEndDate(operationEntity.getLoanContractEndDate())
                        .build();
                log.info("Optimus in complete. Send decision request - {}",
                        umicoApprovedDecisionRequest);
//                umicoClient.sendDecisionToUmico(umicoApprovedDecisionRequest, apiKey);
                log.info("Order Dvs status sent to umico like APPROVED. trackId - {}",
                        operationEntity.getId());
                break;
            case INCIDENT_HAPPENED:
            case BUSINESS_ERROR:
                log.error("Optimus incident or business error happened , response - {}",
                        scoringResultEvent);
                var telesalesOrderId = telesalesService.sendLead(trackId);
                updateOperationTelesalesOrderId(trackId, telesalesOrderId);
                if (operationEntity.getTaskId() != null && operationEntity.getLoanContractDeletedAt() == null) {
                    operationEntity.setLoanContractDeletedAt(LocalDateTime.now());
                    operationRepository.save(operationEntity);
                    try {
                        optimusClient.deleteLoan(businessKey);
                    } catch (Exception e) {
                        log.error("Optimus delete loan process error in incident happened/business error , " +
                                "exception - {}", e.getMessage());
                    }
                }
                sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
                break;
            default:
        }

    }

    private void inUserActivityProcess(ScoringResultEvent scoringResultEvent, String businessKey,
                                       OperationEntity operationEntity, UUID trackId) {
        var processResponse = getProcess(trackId, businessKey);
        if (processResponse.isPresent()) {
            var inUserActivityData = (InUserActivityData) scoringResultEvent.getData();

            var taskDefinitionKey = inUserActivityData.getTaskDefinitionKey().toUpperCase(Locale.ROOT);

            if (taskDefinitionKey.equalsIgnoreCase(TaskDefinitionKey.USER_TASK_SCORING.name())) {
                var taskId = processResponse.get().getTaskId();
                var scoredAmount = processResponse.get()
                        .getVariables()
                        .getSelectedOffer()
                        .getCardOffer()
                        .getAvailableLoanAmount();
                var selectedAmount = operationEntity.getTotalAmount().add(operationEntity.getCommission());
                if (scoredAmount.compareTo(selectedAmount) < 0) {
                    var telesalesOrderId = telesalesService.sendLead(trackId);
                    updateOperationTelesalesOrderId(trackId, telesalesOrderId);
                    sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
                    return;
                }
                operationEntity.setTaskId(taskId);
                operationRepository.save(operationEntity);
                createScoring(trackId, taskId, scoredAmount);
            } else if (taskDefinitionKey.equalsIgnoreCase(TaskDefinitionKey.USER_TASK_SIGN_DOCUMENTS.name())) {
                var dvsId = processResponse.get().getVariables().getDvsOrderId();
                var taskId = processResponse.get().getTaskId();

                var start = processResponse.get().getVariables().getCreateCardCreditRequest().getStartDate();
                var end = processResponse.get().getVariables().getCreateCardCreditRequest().getEndDate();
                operationEntity.setLoanContractStartDate(start);
                operationEntity.setLoanContractEndDate(end);
                operationEntity.setTaskId(taskId);
                operationEntity.setDvsOrderId(dvsId);
                operationRepository.save(operationEntity);
                try {
                    var webUrl = dvsClient.getDetails(trackId, dvsId).getWebUrl();
                    log.info("Dvs client web url - {}", webUrl);
                    sendDecision(UmicoDecisionStatus.PREAPPROVED, trackId, webUrl);
                } catch (DvsClientException e) {
                    log.info("Dvs client get details exception. trackId - {}, exception - {}",
                            trackId, e.getMessage());
                    optimusClient.deleteLoan(businessKey);
                    log.info("Optimus delete loan. trackId - {}", trackId);
                    var telesalesOrderId = telesalesService.sendLead(trackId);
                    updateOperationTelesalesOrderId(trackId, telesalesOrderId);
                    sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
                }
            }
        }
    }

    private Optional<String> startScoring(UUID trackId, String pinCode, String phoneNumber) {
        log.info("Optimus start scoring process is started... trackId - {}", trackId);
        var startScoringVariable = scoringMapper.toStartScoringVariable(pinCode,
                phoneNumber,
                productType);
        var startScoringRequest = StartScoringRequest.builder()
                .processKey(processKey)
                .variables(startScoringVariable)
                .build();
        log.info("Optimus start scoring process. trackId - {}, Request - {}", trackId, startScoringRequest);
        try {
            var startScoringResponse = optimusClient.scoringStart(startScoringRequest);
            log.info("Optimus start scoring process was finished successfully... trackId - {}, Response - {}",
                    trackId,
                    startScoringResponse);
            return Optional.of(startScoringResponse.getBusinessKey());
        } catch (OptimusClientException e) {
            log.error("Optimus start scoring process was finished unsuccessfully... trackId - {}, FeignException - {}",
                    trackId,
                    e.getMessage());
            return Optional.empty();
        }
    }

    private void createScoring(UUID trackId, String taskId, BigDecimal loanAmount) {
        log.info("Optimus create scoring process is started... trackId - {}", trackId);
        var createScoringRequest = CreateScoringRequest.builder()
                .cardDemandedAmount(loanAmount.toString())
                .nameOnCard(" ")
                .customerDecision(CustomerDecision.CREATE_CREDIT)
                .salesSource(ApplicationConstant.UMICO_MARKETPLACE)
                .preApproval(false)
                .build();
        log.info("Optimus create scoring process. trackId - {}, Request - {}", trackId, createScoringRequest);
        try {
            optimusClient.scoringCreate(taskId, createScoringRequest);
            log.info("Optimus create scoring process was finished successfully.. trackId - {}", trackId);
        } catch (OptimusClientException e) {
            log.error("Optimus create scoring process was finished unsuccessfully... trackId - {},FeignException - {}",
                    trackId,
                    e.getMessage());
            var telesalesOrderId = telesalesService.sendLead(trackId);
            updateOperationTelesalesOrderId(trackId, telesalesOrderId);
            sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
        }
    }


    public void completeScoring(CompleteScoring completeScoring) {
        var trackId = completeScoring.getTrackId();
        log.info("Optimus complete scoring process is started... trackId - {}", trackId);
        var customerNumbers = Arrays.asList(
                new CustomerNumber("A", completeScoring.getAdditionalNumber1()),
                new CustomerNumber("B", completeScoring.getAdditionalNumber2()));

        CompleteScoringRequest completeScoringRequest = CompleteScoringRequest.builder()
                .customerContact(new CustomerContact(customerNumbers))
                .customerDecision(completeScoring.getCustomerDecision())
                .deliveryBranchCode("299")
                .build();

        log.info("Optimus complete scoring process. trackId - {}, Request - {}", trackId, completeScoringRequest);
        try {
            optimusClient.scoringComplete(completeScoring.getTaskId(), completeScoringRequest);
            log.info("Optimus complete scoring process was finished successfully... trackId - {}", trackId);
        } catch (OptimusClientException e) {
            var telesalesOrderId = telesalesService.sendLead(trackId);
            updateOperationTelesalesOrderId(trackId, telesalesOrderId);
            sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
            log.error("Optimus complete scoring process was finished unsuccessfully...trackId - {},FeignException - {}",
                    trackId,
                    e.getMessage());
        }
    }

    private Optional<ProcessResponse> getProcess(UUID trackId, String businessKey) {
        log.info("Optimus get process is started... trackId - {},business_key - {}", trackId, businessKey);
        try {
            ProcessResponse processResponse = optimusClient.getProcess(businessKey);
            log.info("Optimus get process scoring process was finished successfully. trackId - {}, Response - {}",
                    trackId, processResponse);
            return Optional.of(processResponse);
        } catch (OptimusClientException f) {
            var telesalesOrderId = telesalesService.sendLead(trackId);
            updateOperationTelesalesOrderId(trackId, telesalesOrderId);
            sendDecision(UmicoDecisionStatus.PENDING, trackId, null);
            log.error("Optimus get process scoring process was finished unsuccessfully." +
                            "trackId - {}, FeignException - {}",
                    trackId, f.getMessage());
            return Optional.empty();
        }
    }

    void updateOperationTelesalesOrderId(UUID trackId, Optional<String> telesalesOrderId) {
        var operationEntityOptional = operationRepository.findById(trackId);
        if (operationEntityOptional.isPresent() && telesalesOrderId.isPresent()) {
            operationEntityOptional.get().setTelesalesOrderId(telesalesOrderId.get());
            operationRepository.save(operationEntityOptional.get());
        }
    }


    public void sendDecision(UmicoDecisionStatus umicoDecisionStatus, UUID trackId, String dvsUrl) {
        var operationEntity = operationRepository.findById(trackId)
                .orElseThrow(() -> new OperationNotFoundException("trackId - " + trackId));
        operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        UmicoDecisionRequest umicoScoringDecisionRequest = UmicoDecisionRequest.builder()
                .trackId(trackId)
                .dvsUrl(dvsUrl)
                .decisionStatus(umicoDecisionStatus)
                .loanTerm(operationEntity.getLoanTerm())
                .build();
        log.info("Umico send decision.trackId - {}, Request - {}",
                trackId,
                umicoScoringDecisionRequest);
        try {
//            UmicoDecisionResponse umicoScoringDecisionResponse = umicoClient
//                    .sendDecisionToUmico(umicoScoringDecisionRequest, apiKey);
            log.info("Umico send decision was finished successfully.trackId - {}",
                    trackId
                    //,                    umicoScoringDecisionResponse
            );
        } catch (UmicoClientException e) {
            log.error("Umico send decision was finished unsuccessfully. trackId - {}, FeignException - {}",
                    trackId,
                    e.getMessage());
        }
    }
}
