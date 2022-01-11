package az.kapitalbank.ms.template.config;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults (makeFinal = true, level = AccessLevel.PUBLIC)
public class SwaggerDefaults {
    static String title = "API";
    static String description = "API documentation";
    static String version = "0.0.1";
    static String termsOfServiceUrl = null;
    static String contactName = null;
    static String contactUrl = null;
    static String contactEmail = null;
    static String license = null;
    static String licenseUrl = null;
    static String defaultIncludePattern = ".*";
    static String host = null;
    static String[] protocols = {};
    static boolean useDefaultResponseMessages = true;
}
