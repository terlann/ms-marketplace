package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.OptimusConstant.ADDITIONAL_NAME_1;
import static az.kapitalbank.marketplace.constant.OptimusConstant.ADDITIONAL_NAME_2;
import static az.kapitalbank.marketplace.constant.OptimusConstant.OPTIMUS_CLIENT_EXCEPTION;

import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.exception.OptimusClientException;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CompleteScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CreateScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerContact;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerDecision;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerNumber;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringVariable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
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

    OptimusClient optimusClient;

    public Optional<String> startScoring(UUID trackId, String pin, String mobileNumber) {
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

    public Optional<String> getCardId(String businessKey, String variableName) {
        log.info("Get card uid process is started : businessKey - {}", businessKey);
        try {
            var cardId = optimusClient.getProcessVariable(businessKey, variableName).getUid();
            log.info("Get card uid was finished : businessKey - {}, cardId - {}",
                    businessKey,
                    cardId);
            return Optional.of(cardId);
        } catch (OptimusClientException e) {
            log.error("Get card uid process was failed : businessKey - {},"
                    + " OptimusClientException - {}", businessKey, e);
            return Optional.empty();
        }
    }

    public Optional<Boolean> createScoring(UUID trackId, String taskId, BigDecimal loanAmount) {
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

    public Optional<ProcessResponse> getProcess(String businessKey) {
        log.info("Get process is started : businessKey - {}", businessKey);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted exception in getProcess: ex - ", ex);
        }
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

    public Optional<Boolean> completeScoring(String taskId, String businessKey,
                                             String additionalPhoneNumber1,
                                             String additionalPhoneNumber2) {
        var customerNumbers = Arrays.asList(
                new CustomerNumber(ADDITIONAL_NAME_1, additionalPhoneNumber1),
                new CustomerNumber(ADDITIONAL_NAME_2, additionalPhoneNumber2));

        var completeScoringRequest = CompleteScoringRequest.builder()
                .customerContact(new CustomerContact(customerNumbers))
                .customerDecision(CustomerDecision.CONFIRM_CREDIT).build();
        log.info("Complete scoring process is started : businessKey - {}, Request - {}",
                businessKey, completeScoringRequest);
        try {
            optimusClient.scoringComplete(taskId, completeScoringRequest);
            log.info("Complete scoring process was finished : businessKey - {}", businessKey);
            return Optional.of(true);
        } catch (OptimusClientException e) {
            log.error("Complete scoring process was failed : " + OPTIMUS_CLIENT_EXCEPTION,
                    businessKey, e);
            return Optional.empty();
        }
    }
}
