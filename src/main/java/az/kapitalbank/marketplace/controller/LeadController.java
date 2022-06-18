package az.kapitalbank.marketplace.controller;

import az.kapitalbank.marketplace.constant.SendLeadType;
import az.kapitalbank.marketplace.service.LeadService;
import az.kapitalbank.marketplace.service.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeadController {

    OrderService orderService;
    LeadService leadService;

    @PostMapping("order/payback")
    public ResponseEntity<Void> autoRefundOrder() {
        orderService.autoPayback();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send/{sendLeadType}")
    public ResponseEntity<Void> retrySendLead(@PathVariable SendLeadType sendLeadType) {
        leadService.sendLeadManual(sendLeadType);
        return ResponseEntity.ok().build();
    }

    @PostMapping("order/pre-purchase")
    public ResponseEntity<Void> retryPrePurchaseOrder() {
        orderService.retryPrePurchaseOrder();
        return ResponseEntity.ok().build();
    }
}
