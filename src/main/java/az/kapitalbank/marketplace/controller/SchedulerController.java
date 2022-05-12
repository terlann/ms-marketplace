package az.kapitalbank.marketplace.controller;

import az.kapitalbank.marketplace.scheduler.RefundSchedule;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scheduler")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SchedulerController {

    private final RefundSchedule refundSchedule;

    @PostMapping("/refund")
    public ResponseEntity<Void> autoRefundOrders() {
        refundSchedule.autoRefundOrders();
        return ResponseEntity.ok().build();
    }
}
