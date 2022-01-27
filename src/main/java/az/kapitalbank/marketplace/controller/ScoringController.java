package az.kapitalbank.marketplace.controller;

import javax.validation.Valid;

import az.kapitalbank.marketplace.dto.request.ScoringOrderRequestDto;
import az.kapitalbank.marketplace.service.ScoringService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/marketplace/scoring")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ScoringController {

    ScoringService service;

    // TODO update customer,operation after telesales scoring and dvs
    @PostMapping
    public ResponseEntity<Void> scoringOrder(@Valid @RequestBody ScoringOrderRequestDto request) {
        service.scoringOrder(request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
