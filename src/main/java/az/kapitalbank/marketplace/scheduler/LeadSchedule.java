package az.kapitalbank.marketplace.scheduler;

import az.kapitalbank.marketplace.service.LeadService;
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
public class LeadSchedule {
    LeadService leadService;

    @Scheduled(initialDelay = 15 * 1000, fixedDelay = 59 * 60 * 1000, zone = "Asia/Baku")
    public void sendLead() {
        log.info("Send lead schedule started at {}", LocalDateTime.now());
        leadService.sendLeadSchedule();
        log.info("Send lead schedule finished at {}", LocalDateTime.now());
    }
}

