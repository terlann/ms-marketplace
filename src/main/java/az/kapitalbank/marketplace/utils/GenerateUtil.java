package az.kapitalbank.marketplace.utils;

import java.util.UUID;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GenerateUtil {

    public static String rrn() {
        return "K-U-M" + UUID.randomUUID().toString().substring(8);
    }
}
