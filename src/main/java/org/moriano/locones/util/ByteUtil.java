package org.moriano.locones.util;

import java.nio.charset.StandardCharsets;

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

    /**
     * Given an exactly 8byte array, display it as a 8x8 table, where each value is either 0 or 1
     * @param tileData
     * @return
     */
    public static String[] printAs8x8Binary(int[] tileData) {
        if (tileData.length != 8) {
            throw new RuntimeException("Expected exactly 8 bytes. but got " + tileData.length);
        }

        String[] output = new String[tileData.length];
        for (int i = 0; i<tileData.length; i++) {
            int myByte = tileData[i];
            output[i] = "";
            for(int byteIdx = 0; byteIdx < 8; byteIdx++) {
                output[i] += ByteUtil.getBit(myByte, byteIdx) +" ";
            }
            output[i] = new StringBuilder(output[i]).reverse().toString();
        }

       return output;
    }

    /**
     * Provides a very human readable representation of this array of bytes.
     * @param raws
     * @return
     */
    public static String prettyPrint(int[] raws) {
        StringBuilder result = new StringBuilder("" +
                "       +--------------------------------------------------+----------------------------------+------------------+\n" +
                "       | 00 01 02 03 04 05 06 07 08 09 0A 0B C0 D0 E0 0F  | 0 1 2 3 4 5 6 7 8 9 A B C D E F  |      Ascii       |\n" +
                "+------+--------------------------------------------------+----------------------------------+------------------+\n");
        StringBuilder preffix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();

        int columnCount = 1;
        int rowCount = 0;
        for (int raw : raws) {

            String niceByte = String.format("%02X ", raw);
            String niceString = new String(new byte[]{(byte)raw}, StandardCharsets.US_ASCII);
            if (!(raw >=0x21 && raw <= 0x7E)) {
                /*
                As per the ascii table.

                Characters that are not printable are assigned a dot, so that the printing of the array
                of bytes makes sense to the human eye
                 */
                niceString = ".";
            }

            preffix.append(niceByte);
            suffix.append(niceString + " ");
            if (columnCount >= 16 && columnCount % 16 == 0) {
                result.append("| " + String.format("%04X", rowCount) + " | ");
                result.append(preffix);
                result.append(" | ");
                result.append(suffix);
                result.append(" | ");
                result.append(suffix.toString().replace(" ", ""));
                result.append(" |\n");

                preffix = new StringBuilder();
                suffix = new StringBuilder();
                rowCount++;
            }
            columnCount++;
        }

        if (preffix.length() > 0) {
            result.append("| " + String.format("%04X", rowCount) + " | ");
            result.append(preffix);
            int expectedSize = 16 * 3;
            int missingGaps = expectedSize - preffix.length();
            for (int i = 0; i < missingGaps; i++) {
                result.append(" ");
            }
            result.append(" | ");
            result.append(suffix);

            expectedSize = (16 * 2);
            missingGaps = expectedSize - suffix.length();
            for (int i = 0; i < missingGaps; i++) {
                result.append(" ");
            }

            result.append(" | ");
            result.append(suffix.toString().replace(" ", ""));
            expectedSize = 16;
            missingGaps = expectedSize - suffix.toString().replace(" ", "").length();
            for (int i = 0; i < missingGaps; i++) {
                result.append(" ");
            }
            result.append(" | \n");
        }
        result.append("+------+--------------------------------------------------+----------------------------------+------------------+\n");

        return result.toString();
    }
}
