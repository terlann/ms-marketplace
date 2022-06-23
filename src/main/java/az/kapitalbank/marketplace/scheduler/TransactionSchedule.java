package az.kapitalbank.marketplace.scheduler;

import az.kapitalbank.marketplace.service.OrderService;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionSchedule {
    OrderService orderService;

    @Async("marketplaceThreadPool")
    @SchedulerLock(name = "TransactionSchedule_autoPayback")
    @Scheduled(cron = "0 0 22 * * ?", zone = "Asia/Baku")
    public void autoPayback() {
        log.info("Auto payback schedule started at {}", LocalDateTime.now());
        orderService.autoPayback();
        log.info("Auto payback schedule finished at {}", LocalDateTime.now());
    }

    @Async("threadPoolExecutor")
    @SchedulerLock(name = "TransactionSchedule_retryPrePurchase")
    @Scheduled(cron = "0 0 14 * * ?", zone = "Asia/Baku")
    public void retryPrePurchase() {
        log.info("Retry prePurchase schedule started at {}", LocalDateTime.now());
        orderService.retryPrePurchaseOrder();
        log.info("Retry prePurchase schedule finished at {}", LocalDateTime.now());
    }
}
