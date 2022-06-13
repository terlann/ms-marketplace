package az.kapitalbank.marketplace.client.integration;

import static org.springframework.cloud.openfeign.security.OAuth2FeignRequestInterceptor.AUTHORIZATION;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;


@Profile("!local && !dev")
public class IntegrationAuthRequestInterceptor implements RequestInterceptor {
    private final OAuth2AuthorizedClientManager manager;
    @Value("${spring.security.oauth2.client.registration.keycloak.provider}")
    private String registrationId;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String principal;

    public IntegrationAuthRequestInterceptor(OAuth2AuthorizedClientManager manager) {
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
                    "Bearer " + authorizedClient.getAccessToken().getTokenValue());
        }
    }

}
