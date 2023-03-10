package az.kapitalbank.marketplace.client.optimus;

import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessVariableResponse;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CompleteScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.CreateScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringRequest;
import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringResponse;
import feign.Logger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "optimus-client",
        url = "${client.adp-optimus.url}",
        configuration = OptimusClient.FeignConfiguration.class)
public interface OptimusClient {

    @PostMapping("/scoring/start")
    StartScoringResponse scoringStart(@RequestBody StartScoringRequest request);

    @PostMapping("/scoring/{task-id}/create")
    void scoringCreate(@PathVariable("task-id") String taskId,
                       @RequestBody CreateScoringRequest request);

    @PostMapping("/scoring/{task-id}/complete")
    void scoringComplete(@PathVariable("task-id") String taskId,
                         @RequestBody CompleteScoringRequest request);

    @GetMapping("/process/task/{business-key}")
    ProcessResponse getProcess(@PathVariable("business-key") String businessKey);

    @GetMapping("/process/loan/{business-key}/variables")
    ProcessVariableResponse getProcessVariable(@PathVariable("business-key") String businessKey,
                                               @RequestParam String variableName);

    @DeleteMapping("/process/loan/{business-key}")
    void deleteLoan(@PathVariable("business-key") String businessKey);

    class FeignConfiguration {
        @Bean
        Logger.Level loggerLevel() {
            return Logger.Level.FULL;
        }
    }
}
