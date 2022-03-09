package az.kapitalbank.marketplace.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskedMobileNum {
    public static String maskedMobNumber(String mobNumb) {

        char[] arr = mobNumb.toCharArray();
        for (int i = 0; i < arr.length - 4; i++) {
            arr[i] = '*';
        }
        return String.valueOf(arr);
    }
}
