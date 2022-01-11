package az.kapitalbank.marketplace.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.vault.config.SecretBackendConfigurer;
import org.springframework.cloud.vault.config.VaultConfigurer;
import org.springframework.cloud.vault.config.VaultKeyValueBackendProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static az.kapitalbank.marketplace.constants.ApplicationProfiles.DEV;
import static az.kapitalbank.marketplace.constants.ApplicationProfiles.PRE_PROD;
import static az.kapitalbank.marketplace.constants.ApplicationProfiles.PROD;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile(value = {DEV, PRE_PROD,PROD})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VaultConfig implements VaultConfigurer {

    VaultKeyValueBackendProperties vaultKeyValueBackendProperties;

    @Override
    public void addSecretBackends(SecretBackendConfigurer configurer) {
        final var backend = vaultKeyValueBackendProperties.getBackend();
        final var contextPath = vaultKeyValueBackendProperties.getDefaultContext();
        final var applicationName = vaultKeyValueBackendProperties.getApplicationName();
        final var profileSeparator = vaultKeyValueBackendProperties.getProfileSeparator();
        final var profile = vaultKeyValueBackendProperties.getProfiles().get(0);

        String path = String.join(profileSeparator, backend, contextPath, applicationName, profile);

        log.info("vault path: {}", path);

        configurer.add(path);
        configurer.registerDefaultDiscoveredSecretBackends(true);
    }
}
