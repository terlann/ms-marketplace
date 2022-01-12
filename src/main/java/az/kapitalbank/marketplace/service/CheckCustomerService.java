package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import org.springframework.http.ResponseEntity;

public interface CheckCustomerService {
    ResponseEntity<WrapperResponseDto> checkPinCode(String pinCode);
}
