package az.kapitalbank.marketplace.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OtpUtil {
    public static String maskMobileNumber(String mobileNumber) {
        char[] arr = mobileNumber.toCharArray();
        for (int i = 5; i < 8; i++) {
            arr[i] = '*';
        }
        return String.valueOf(arr);
    }
}
