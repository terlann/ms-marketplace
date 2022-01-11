package az.kapitalbank.marketplace.exception.models;

import az.kapitalbank.marketplace.constants.AdpOptimusLevels;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoringCustomerException extends RuntimeException {

    static final String MESSAGE = "Scoring client throw Exception. track_id - [%s],Level - [%s]";

    public ScoringCustomerException(String trackId, AdpOptimusLevels level) {
        super(String.format(MESSAGE,trackId,level.toString()));
    }
}
