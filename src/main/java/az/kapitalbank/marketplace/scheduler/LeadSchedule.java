package az.kapitalbank.marketplace.scheduler;

import az.kapitalbank.marketplace.service.LeadService;
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
public class LeadSchedule {

    LeadService leadService;

    @Async("threadPoolExecutor")
    @SchedulerLock(name = "LeadSchedule_sendLeadNoActionDvs")
    @Scheduled(initialDelay = 15 * 1000, fixedDelay = 59 * 60 * 1000, zone = "Asia/Baku")
    public void sendLeadNoActionDvs() {
        log.info("Send lead schedule started at {}", LocalDateTime.now());
        leadService.sendLeadNoActionDvs();
        log.info("Send lead schedule finished at {}", LocalDateTime.now());
    }

    @Async("threadPoolExecutor")
    @SchedulerLock(name = "LeadSchedule_retrySendLead")
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = 60 * 60 * 1000, zone = "Asia/Baku")
    public void retrySendLead() {
        log.info("Retry send lead schedule started at {}", LocalDateTime.now());
        leadService.retrySendLead();
        log.info("Retry send lead schedule finished at {}", LocalDateTime.now());

    }
}

