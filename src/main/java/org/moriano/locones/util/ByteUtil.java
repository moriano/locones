package org.moriano.locones.util;

/**
 * Created by moriano on 15/11/14.
 */
public class ByteUtil {

    public static int getBit(int value, int bitPosition) {
        int result = 0;

        int tmp = value & (int)Math.pow(2, bitPosition);
        result = tmp > 0 ? 1 : 0;
        return result;
    }
}
