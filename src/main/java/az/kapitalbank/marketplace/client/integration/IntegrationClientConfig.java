package az.kapitalbank.marketplace.client.integration;

import static az.kapitalbank.marketplace.constant.CommonConstant.TOKEN_PREFIX;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntegrationClientConfig implements RequestInterceptor {

    final OAuth2AuthorizedClientManager manager;
    @Value("${spring.security.oauth2.client.registration.keycloak.provider}")
    String registrationId;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    String principal;

    @Override
    public void apply(final RequestTemplate requestTemplate) {
        OAuth2AuthorizeRequest authorizeRequest =
                OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
                        .principal(principal)
                        .build();
        OAuth2AuthorizedClient authorizedClient = manager.authorize(authorizeRequest);
        if (Objects.nonNull(authorizedClient)) {
            requestTemplate.header(AUTHORIZATION,
                    TOKEN_PREFIX.concat(authorizedClient.getAccessToken().getTokenValue()));
        }
    }

    @Bean
    Logger.Level loggerLevel() {
        return Logger.Level.FULL;
    }
}
