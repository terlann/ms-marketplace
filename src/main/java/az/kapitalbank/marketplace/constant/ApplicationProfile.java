package az.kapitalbank.marketplace.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApplicationProfile {
    public static final String LOCAL = "local";
    public static final String DEV = "dev";
    public static final String PRE_PROD = "preprod";
    public static final String PROD = "prod";
}
