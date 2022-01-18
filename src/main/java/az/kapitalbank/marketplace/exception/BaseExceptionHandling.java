package az.kapitalbank.marketplace.exception;

import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.UnexpectedTypeException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class BaseExceptionHandling extends ResponseEntityExceptionHandler {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<WrapperResponseDto<?>> internalError(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.INTERNAL_SERVER_ERROR.getCode();
        var message = ex.getMessage();
        var wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(wrapperResponseDto);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<WrapperResponseDto<?>> feignException(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.INTERNAL_SERVER_ERROR.getCode();
        var message = ErrorCodes.INTERNAL_SERVER_ERROR.getMessage();
        var wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(wrapperResponseDto);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<WrapperResponseDto<?>> sqlException(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.INTERNAL_SERVER_ERROR.getCode();
        var message = ErrorCodes.INTERNAL_SERVER_ERROR.getMessage();
        var wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(wrapperResponseDto);
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<WrapperResponseDto<?>> sqlIntegrityConstraintViolationException(Exception ex, Object body) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.INTERNAL_SERVER_ERROR.getCode();
        var message = ErrorCodes.INTERNAL_SERVER_ERROR.getMessage();
        var wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(wrapperResponseDto);
    }

    @ExceptionHandler(UnexpectedTypeException.class)
    public ResponseEntity<WrapperResponseDto<?>> sqlIntegrityConstraintViolationException(UnexpectedTypeException ex) {
        log.error("Exception: {}", ex);
        var code = ErrorCodes.REQUEST_FIELD_TYPES.getCode();
        var message = ErrorCodes.REQUEST_FIELD_TYPES.getMessage();
        var wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(wrapperResponseDto);
    }


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {
        log.error("Request - [{}], Exception: ", request.toString(), ex);
        List<String> errorFields = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            errorFields.add(fieldName);
        });
        var code = ErrorCodes.REQUEST_VALIDATE.getCode();
        var message = String.format(ErrorCodes.REQUEST_VALIDATE.getMessage(), errorFields.toArray());
        var wrapperResponseDto = WrapperResponseDto.of(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapperResponseDto);
    }
}
