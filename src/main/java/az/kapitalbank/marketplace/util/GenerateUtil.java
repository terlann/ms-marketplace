package az.kapitalbank.marketplace.util;

import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GenerateUtil {

    public static String rrn() {
        var temp = UUID.randomUUID();
        var uuidString = Long.toHexString(temp.getMostSignificantBits())
                + Long.toHexString(temp.getLeastSignificantBits());
        return "bumm-" + uuidString.substring(3);
    }
}
