package az.kapitalbank.marketplace.service.impl;

import az.kapitalbank.marketplace.client.customercheck.CustomerCheckClient;
import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import az.kapitalbank.marketplace.exception.models.PinCodeInCorrectException;
import az.kapitalbank.marketplace.service.CheckCustomerService;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckCustomerServiceImpl implements CheckCustomerService {

    CustomerCheckClient customerCheckClient;

    @Override
    public ResponseEntity<WrapperResponseDto> checkPinCode(String pinCode) {
        log.info("check customer pin-code service start... pin_code - [{}]", pinCode);

        WrapperResponseDto wrapperResponseDto = WrapperResponseDto.ofSuccess();
        ResponseEntity<WrapperResponseDto> response = ResponseEntity.ok(wrapperResponseDto);
        try {
            Boolean result = customerCheckClient.checkPinCode(pinCode);
            log.info("check customer pin-code service.pin_code - [{}], Response - {}", pinCode, result);
            if (result) {
                return response;
            } else {
                throw new PinCodeInCorrectException("this pin code is incorrect");
            }
        } catch (FeignException f) {
            log.error("check customer pin-code service.pin_code - [{}], FeignException - {}", pinCode, f.getMessage());
        }
        log.info("check customer pin-code service finish.. pin_code - [{}]", pinCode);
        return response;
    }
}
