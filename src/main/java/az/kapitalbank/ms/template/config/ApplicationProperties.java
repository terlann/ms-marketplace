package az.kapitalbank.ms.template.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Getter
@RefreshScope
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

    private final Swagger swagger = new Swagger();

    @Getter
    @Setter
    public static class Swagger {
        private String title = SwaggerDefaults.title;
        private String description = SwaggerDefaults.description;
        private String version = SwaggerDefaults.version;
        private String termsOfServiceUrl = SwaggerDefaults.termsOfServiceUrl;
        private String contactName = SwaggerDefaults.contactName;
        private String contactUrl = SwaggerDefaults.contactUrl;
        private String contactEmail = SwaggerDefaults.contactEmail;
        private String license = SwaggerDefaults.license;
        private String licenseUrl = SwaggerDefaults.licenseUrl;
        private String defaultIncludePattern = SwaggerDefaults.defaultIncludePattern;
        private String host = SwaggerDefaults.host;
        private String[] protocols = SwaggerDefaults.protocols;
        private boolean useDefaultResponseMessages = SwaggerDefaults.useDefaultResponseMessages;
    }
}
