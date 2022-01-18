package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;

public interface ProductCreateService {

    void startScoring(FraudCheckResultEvent checkFraudResultEvent);

    void createScoring(ScoringResultEvent scoringResultEvent);

    void completeScoring(String trackId,String taskId);

}
