package az.kapitalbank.marketplace.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

import az.kapitalbank.marketplace.constant.TelesalesResult;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TelesalesResultRequestDto {
    @NotBlank
    String telesalesOrderId;
    @NotNull
    TelesalesResult telesalesResult;
    @NotNull
    LocalDate loanContractStartDate;
    @NotNull
    LocalDate loanContractEndDate;
    @NotBlank
    @Size(min = 16, max = 16)
    String cardPan;
}
