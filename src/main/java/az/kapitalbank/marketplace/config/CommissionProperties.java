package az.kapitalbank.marketplace.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@RefreshScope
@Component
@ConfigurationProperties("commission")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CommissionProperties {

     Map<Integer, BigDecimal> values=new HashMap<>();

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        log.info("::: Consul config reloaded :::" + event.getName());
    }
}