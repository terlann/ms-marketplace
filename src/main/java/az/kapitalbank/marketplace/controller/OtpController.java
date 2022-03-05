package az.kapitalbank.marketplace.controller;

import az.kapitalbank.marketplace.dto.request.SendOtpRequestDto;
import az.kapitalbank.marketplace.dto.response.SendOtpResponseDto;
import az.kapitalbank.marketplace.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController {

    OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<SendOtpResponseDto> send(SendOtpRequestDto request) {
        return ResponseEntity.ok(otpService.send(request));
    }

}
