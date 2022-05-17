package az.kapitalbank.marketplace.controller;

import az.kapitalbank.marketplace.scheduler.LeadSchedule;
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

    RefundSchedule refundSchedule;
    LeadSchedule leadSchedule;

    @PostMapping("/refund")
    public ResponseEntity<Void> autoRefundOrders() {
        refundSchedule.autoRefundOrder();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-lead")
    public ResponseEntity<Void> sendLead() {
        leadSchedule.sendLead();
        return ResponseEntity.ok().build();
    }
}
