package az.kapitalbank.marketplace.exception.handler;

import javax.validation.UnexpectedTypeException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

import az.kapitalbank.marketplace.constants.ErrorCode;
import az.kapitalbank.marketplace.dto.ErrorResponseDto;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class BaseExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({SQLIntegrityConstraintViolationException.class,
            SQLException.class,
            FeignException.class})
    public ResponseEntity<ErrorResponseDto> multipleException(Exception ex) {
        log.error("Exception: {}", ex);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDto);
    }

    @ExceptionHandler(UnexpectedTypeException.class)
    public ResponseEntity<ErrorResponseDto> unexpectedTypeException(UnexpectedTypeException ex) {
        log.error("Exception: {}", ex);
        var errorResponseDto = new ErrorResponseDto(ErrorCode.REQUEST_FIELD_TYPES);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDto);
    }


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {
        log.error("Request - [{}], Exception: ", request.toString(), ex);
        List<String> errorFields = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errorFields.add(fieldName);
        });
        var code = ErrorCode.BAD_REQUEST.getCode();
        var message = String.format(ErrorCode.BAD_REQUEST.getMessage(), errorFields.toArray());
        var errorResponseDto = new ErrorResponseDto(code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }
}
