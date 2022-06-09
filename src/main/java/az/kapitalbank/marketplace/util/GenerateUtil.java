package az.kapitalbank.marketplace.util;

import java.security.SecureRandom;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GenerateUtil {

    private static final SecureRandom random = new SecureRandom();

    public static String rrn() {
        var rrnNumber = System.currentTimeMillis() + random.nextInt(900) + 100;
        return String.valueOf(rrnNumber).replaceAll(".$", "");
    }
}
