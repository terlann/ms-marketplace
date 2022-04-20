package az.kapitalbank.marketplace.annotation;

import az.kapitalbank.marketplace.constant.ScoringStatus;
import az.kapitalbank.marketplace.dto.request.TelesalesResultRequestDto;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NotNullForTelesalesResultValidator
        implements ConstraintValidator<NotNullForTelesalesResult, TelesalesResultRequestDto> {

    @Override
    public boolean isValid(TelesalesResultRequestDto value, ConstraintValidatorContext context) {
        if (value.getScoringStatus().equals(ScoringStatus.APPROVED)) {
            return value.getLoanContractStartDate() != null
                    && value.getLoanContractEndDate() != null
                    && value.getUid() != null
                    && value.getScoredAmount() != null;
        }
        return true;
    }
}
