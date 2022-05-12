package az.kapitalbank.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = "az.kapitalbank.marketplace.client")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
