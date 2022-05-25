package az.kapitalbank.marketplace.scheduler;

import az.kapitalbank.marketplace.service.OrderService;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefundSchedule {
    OrderService orderService;

    @Scheduled(cron = "0 22 * * * ?", zone = "Asia/Baku")
    public void autoRefundOrder() {
        log.info("Auto refund schedule started at {}", LocalDateTime.now());
        orderService.autoRefundOrderSchedule();
        log.info("Auto refund schedule finished at {}", LocalDateTime.now());
    }
}
