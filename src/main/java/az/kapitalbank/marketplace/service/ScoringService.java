package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.OptimusConstant.ADDITIONAL_NAME_1;
import static az.kapitalbank.marketplace.constant.OptimusConstant.ADDITIONAL_NAME_2;
import static az.kapitalbank.marketplace.constant.OptimusConstant.OPTIMUS_CLIENT_EXCEPTION;

import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessVariableResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CompleteScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CreateScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerContact;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerDecision;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CustomerNumber;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringVariable;
import az.kapitalbank.marketplace.entity.OperationEntity;
import feign.FeignException;
import java.util.Arrays;
import java.util.Optional;
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

    public Optional<String> startScoring(OperationEntity operationEntity) {
        var trackId = operationEntity.getId();
        var startScoringRequest =
                StartScoringRequest.builder().variables(StartScoringVariable.builder()
                        .pin(operationEntity.getPin())
                        .phoneNumber(operationEntity.getMobileNumber()).build()).build();
        log.info("Start scoring process is started : trackId - {}, request - {}", trackId,
                startScoringRequest);
        try {
            var startScoringResponse = optimusClient.scoringStart(startScoringRequest);
            log.info("Start scoring process was finished : trackId - {}," + " response - {}",
                    trackId, startScoringResponse);
            return Optional.of(startScoringResponse.getBusinessKey());
        } catch (FeignException e) {
            log.error("Start scoring process was failed : trackId - {}, exception - {}",
                    trackId, e);
            return Optional.empty();
        }
    }

    public Optional<ProcessVariableResponse> getProcessVariable(OperationEntity operationEntity,
                                                                String variableName) {
        var trackId = operationEntity.getId();
        var businessKey = operationEntity.getBusinessKey();
        log.info("Get process variable is started : trackId - {}, businessKey - {}",
                trackId, businessKey);
        try {
            var processVariableResponse =
                    optimusClient.getProcessVariable(businessKey, variableName);
            log.info("Get process variable was finished : trackId - {}, businessKey - {}, cardId - {}",
                    trackId, businessKey, processVariableResponse.getUid());
            return Optional.of(processVariableResponse);
        } catch (FeignException e) {
            log.error("Get card uid process was failed :"
                            + " trackId - {}, businessKey - {}, exception - {}",
                    trackId, businessKey, e);
            return Optional.empty();
        }
    }

    public Optional<Boolean> createScoring(OperationEntity operationEntity) {
        var trackId = operationEntity.getId();
        var createScoringRequest =
                CreateScoringRequest.builder()
                        .cardDemandedAmount(operationEntity.getScoredAmount().toString())
                        .customerDecision(CustomerDecision.CREATE_CREDIT)
                        .build();
        log.info("Create scoring process is started : trackId - {}, request - {}", trackId,
                createScoringRequest);
        try {
            optimusClient.scoringCreate(operationEntity.getTaskId(), createScoringRequest);
            log.info("Create scoring process was finished : trackId - {}", trackId);
            return Optional.of(true);
        } catch (FeignException e) {
            log.error("Create scoring process was failed : trackId - {} ,exception - {}",
                    trackId, e);
            return Optional.empty();
        }
    }

    public Optional<ProcessResponse> getProcess(OperationEntity operationEntity) {
        var trackId = operationEntity.getId();
        var businessKey = operationEntity.getBusinessKey();
        log.info("Get process is started : trackId - {}, businessKey - {}", trackId, businessKey);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted exception in getProcess: trackId - {}, exception - {}", trackId,
                    ex);
        }
        try {
            ProcessResponse processResponse = optimusClient.getProcess(businessKey);
            log.info("Get process was finished :  trackId - {}, businessKey - {}, response - {}",
                    trackId, businessKey, processResponse);
            return Optional.of(processResponse);
        } catch (FeignException ex) {
            log.error("Get process was failed : " + OPTIMUS_CLIENT_EXCEPTION,
                    trackId, businessKey, ex);
            return Optional.empty();
        }
    }

    public Optional<Boolean> completeScoring(OperationEntity operationEntity) {
        var customerNumbers = Arrays.asList(
                new CustomerNumber(ADDITIONAL_NAME_1, operationEntity.getAdditionalPhoneNumber1()),
                new CustomerNumber(ADDITIONAL_NAME_2, operationEntity.getAdditionalPhoneNumber2()));
        var trackId = operationEntity.getId();
        var businessKey = operationEntity.getBusinessKey();
        var completeScoringRequest = CompleteScoringRequest.builder()
                .customerContact(new CustomerContact(customerNumbers))
                .customerDecision(CustomerDecision.CONFIRM_CREDIT).build();
        log.info(
                "Complete scoring process is started :"
                        + " trackId - {}, businessKey - {}, request - {}",
                trackId, businessKey, completeScoringRequest);
        try {
            optimusClient.scoringComplete(operationEntity.getTaskId(), completeScoringRequest);
            log.info("Complete scoring process was finished : trackId - {}, businessKey - {}",
                    trackId, businessKey);
            return Optional.of(true);
        } catch (FeignException e) {
            log.error("Complete scoring process was failed :"
                            + " trackId - {}, businessKey - {}, exception - {}",
                    trackId, businessKey, e);
            return Optional.empty();
        }
    }
}
