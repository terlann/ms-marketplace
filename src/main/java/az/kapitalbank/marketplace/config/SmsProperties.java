package az.kapitalbank.marketplace.config;

import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Builder
@RefreshScope
@Component
@ConfigurationProperties("sms")
@PropertySource(value = "classpath:/config/sms.yml")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SmsProperties {
    Map<String, String> values = new HashMap<>();

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        log.info("::: Consul config reloaded :::" + event.getName());
    }
}
