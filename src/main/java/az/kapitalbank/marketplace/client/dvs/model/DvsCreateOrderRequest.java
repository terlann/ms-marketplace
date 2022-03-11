package az.kapitalbank.marketplace.client.dvs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DvsCreateOrderRequest {
    String fullName;
    String email;
    String mobile;
    String cif;
    String pin;
    Boolean confirmed;
    @JsonProperty("salary_project")
    Boolean salaryProject;
    @JsonProperty("phone_number_verified")
    Boolean phoneNumberVerified;
    @JsonProperty("process_id")
    String processId;
    @JsonProperty("sales_channel")
    String salesChannel;
    @JsonProperty("credit_created_at")
    LocalDateTime creditCreatedAt;
    String product;
    @JsonProperty("cash_contract")
    String cashContract;
    @JsonProperty("cash_amount")
    String cashAmount;
    String annuity;
    @JsonProperty("loan_percentage")
    String loanPercentage;
    @JsonProperty("bircard_percentage")
    String bircardPercentage;
    Integer tenure;
    @JsonProperty("sales_source")
    String salesSource;
    @JsonProperty("branch_code")
    Integer branchCode;
    String agent;
    List<String> documents;
}
