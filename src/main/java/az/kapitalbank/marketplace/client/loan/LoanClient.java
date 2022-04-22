package az.kapitalbank.marketplace.client.loan;

import az.kapitalbank.marketplace.dto.request.LoanRequest;
import az.kapitalbank.marketplace.dto.response.LoanResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "loan-client", url = "${client.loan.url}")
public interface LoanClient {

    @PostMapping("/lead/loan")
    LoanResponse sendLead(@RequestHeader("X-lEAD-SOURCE") String source,
                          @RequestBody LoanRequest request);
}
