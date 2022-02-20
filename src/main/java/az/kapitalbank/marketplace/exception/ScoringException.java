package az.kapitalbank.marketplace.exception;

import java.util.UUID;

import az.kapitalbank.marketplace.constant.ScoringLevel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoringException extends RuntimeException {

    static final String MESSAGE = "Scoring client throw Exception. track_id - [%s],Level - [%s]";

    public ScoringException(UUID trackId, ScoringLevel level) {
        super(String.format(MESSAGE, trackId, level.toString()));
    }
}
