package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.dto.request.ScoringOrderRequestDto;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;

public interface ScoringService {

    ResponseEntity<Object> scoringOrder(ScoringOrderRequestDto request);

    Optional<String> startScoring(String trackId, String pinCode, String phoneNumber);

    void createScoring(String trackId,String businessKey, BigDecimal loanAmount);

    void completeScoring(CompleteScoring completeScoring);

    Optional<ProcessResponse> getProcess(String trackId, String businessKey);
}
