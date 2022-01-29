package az.kapitalbank.marketplace.utils;

import java.util.UUID;

public class GenerateUtil {

    public static String rrn() {
        return "umico" + UUID.randomUUID().toString().substring(8);
    }
}
