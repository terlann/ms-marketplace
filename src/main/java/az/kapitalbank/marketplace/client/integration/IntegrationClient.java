package az.kapitalbank.marketplace.client.integration;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import az.kapitalbank.marketplace.client.integration.model.IamasResponse;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "integration",
        url = "${client.integration.url}",
        configuration = IntegrationClient.IntegrationClientConfig.class)
public interface IntegrationClient {

    @GetMapping("/iamas/id-card/by/pin/{pin}")
    List<IamasResponse> findPersonByPin(@PathVariable("pin") String pin);

    @Slf4j
    class IntegrationClientConfig implements RequestInterceptor {

        private final OAuth2AuthorizedClientManager manager;
        @Value("${spring.security.oauth2.client.registration.keycloak.provider}")
        private String registrationId;
        @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
        private String principal;

        public IntegrationClientConfig(OAuth2AuthorizedClientManager manager) {
            this.manager = manager;
        }

        @Override
        public void apply(final RequestTemplate requestTemplate) {
            OAuth2AuthorizeRequest authorizeRequest =
                    OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
                            .principal(principal)
                            .build();
            OAuth2AuthorizedClient authorizedClient = manager.authorize(authorizeRequest);
            if (Objects.nonNull(authorizedClient)) {
                requestTemplate.header(AUTHORIZATION,
                        "Bearer ".concat(authorizedClient.getAccessToken().getTokenValue()));
            }

            class FeignConfiguration {
                @Bean
                Logger.Level loggerLevel() {
                    return Logger.Level.FULL;
                }
            }
        }
    }
}
