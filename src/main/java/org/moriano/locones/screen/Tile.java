package org.moriano.locones.screen;

import org.moriano.locones.memory.PatternTables;
import org.moriano.locones.util.ByteUtil;

import java.util.Arrays;

/**
 * Each Tile represents an 8x8 pixel square.
 *
 * Tiles compose the pattern table. See {@link PatternTables} for details.
 */
public class Tile {

    /**
     * Each element of this array will take values 0 1 2 or 3 only
     */
    private  int[][] tileData = new int[8][8];

    public Tile(int[][] tileData) {
        for (int tileRow[] : tileData) {
            for (int tileByte : tileRow) {
                if (tileByte > 3) {
                    throw new RuntimeException("Trying to build a tile with invalid values. Values should be only " +
                            "0 1 2 or 3. Received value was " + tileByte);
                }
            }
        }
        this.tileData = tileData;
    }

    /**
     * Given a 16bytes array, generate a tile.
     *
     * A reminder of the logic (also present on {@link PatternTables}
     * Each entry in the pattern table consits of 16bytes. Now repeat with me: those 16 bytes will give you a tile. A tile
     * consists on a 8x8pixel square. In the NES the screen is composef of 256x240px OR 32x30 tiles (it is the same, but
     * trust me, it is better to think of tiles rather than pixels).
     * <p>
     * So, say you read 16bytes, that is going to give you a tile, that is 8x8 pixels. Lets see an example
     * <p>
     * Say your 16 bytes are
     * High byte 0xC1, 0xC2 0x44 0x48 0x10 0x20 0x40 0x80
     * Low byte  0x01  0x02 0x04 0x08 0x16 0x21 0x42 0x87
     * <p>
     * Now what we are going to do is to arange each of those bytes in a table 8x8, each value of the table is either
     * 0 or 1.
     * <p>
     * Lets start with the high byte
     * <p>
     * 0x41  0 1 0 0 0 0 0 1
     * 0xC2  1 1 0 0 0 0 1 0
     * 0x44  0 1 0 0 0 1 0 0
     * 0x48  0 1 0 0 1 0 0 0     ===> Notice how this high bytes shows the "picture" =>  1/
     * 0x10  0 0 0 1 0 0 0 0                                     No, really, look at it, it is a one with a fraction line
     * 0x20  0 0 1 0 0 0 0 0
     * 0x40  0 1 0 0 0 0 0 0
     * 0x80  1 0 0 0 0 0 0 0
     * <p>
     * And now the low byte
     * <p>
     * 0x01  0 0 0 0 0 0 0 1
     * 0x02  0 0 0 0 0 0 1 0
     * 0x04  0 0 0 0 0 1 0 0
     * 0x08  0 0 0 0 1 0 0 0    ===> Notice how this high bytes shows the "picture" =>  /2
     * 0x16  0 0 0 1 0 1 1 0
     * 0x21  0 0 1 0 0 0 0 1
     * 0x42  0 1 0 0 0 0 1 0
     * 0x87  1 0 0 0 0 1 1 1
     * <p>
     * Now, we COMBINE both values, it is a bit confusing as the LOW byte side, will represent the Upper bit. When
     * whe combine the values, the resulting 8x8 table. Each value being either 0x00 0x01 0x10 or 0x11 for
     * simplicity i will who the values in decimal (so either 0, 1, 2 or 3)
     * <p>
     * 0 1 0 0 0 0 0 3
     * 1 1 0 0 0 0 3 0
     * 0 1 0 0 0 3 0 0
     * 0 1 0 0 3 0 0 0
     * 0 0 0 3 0 2 2 0 ===> Now make a bit of an effort, we have the "picture" 1/2 here
     * 0 0 3 0 0 0 0 2      the actual values 0 1 2 and 3 will represent different colors, but for that we need to
     * 0 3 0 0 0 0 2 0      load the frame palettes
     * 3 0 0 0 0 2 2 2
     * @param patternTableBytes
     * @return
     */
    public static Tile fromPatternTable(int[] patternTableBytes) {
        if (patternTableBytes.length != 16) {
            throw new RuntimeException("Trying to build a tile by passing an incorrect array of bytes. Expected " +
                    "is 16 bytes but got " + patternTableBytes.length);
        }

        /*
        First, lets separate into high 8bytes and low 8bytes
         */
        int[] highBytes = Arrays.copyOfRange(patternTableBytes, 0, 8);
        int[] lowBytes = Arrays.copyOfRange(patternTableBytes, 8, patternTableBytes.length);

        /*
        Convert the arrays of bytes now

        High bytes are valued either 0 or 1
        Low bytes are valued either 0 or 2

        Then we add them, and that gives the final value
         */
        String[] highTileStr = ByteUtil.printAs8x8Binary(highBytes);
        String[] lowTileStr = ByteUtil.printAs8x8Binary(lowBytes);

        int[][] highSideBytes = new int[8][8];
        for (int i = 0; i<highBytes.length; i++) {
            int highByte = highBytes[i];
            for(int byteIdx = 0; byteIdx < 8; byteIdx++) {
                highSideBytes[i][7-byteIdx] = ByteUtil.getBit(highByte, byteIdx);
            }
        }


        int[][] lowSideBytes = new int[8][8];
        for (int i = 0; i<lowBytes.length; i++) {
            int lowByte = lowBytes[i];
            for(int byteIdx = 0; byteIdx < 8; byteIdx++) {
                int lowByteValue = ByteUtil.getBit(lowByte, byteIdx);
                /*
                Remember! the lowside bytes will represent the most significant bit at the end
                as such, we are assigning them values that either 0x10 or 0x00 that is decimal 0
                or 2.
                 */
                lowByteValue = lowByteValue == 0 ? 0 : 2;
                lowSideBytes[i][7-byteIdx] = lowByteValue;
            }
        }

        int[][] finalTileBytes = new int[8][8];
        for (int i = 0; i < lowSideBytes.length; i++) {
            for (int j = 0; j < lowSideBytes[0].length; j++) {
                finalTileBytes[i][j] = highSideBytes[i][j] + lowSideBytes[i][j];
            }
        }

        Tile newTile = new Tile(finalTileBytes);
        System.out.println(newTile);

        return newTile;
    }

    public int[][] getTileData() {
        return tileData;
    }

    public String toString() {
        String result = "";
        for (int rowIx = 0; rowIx < 8; rowIx++) {
            for (int colIdx = 0; colIdx < 8; colIdx++) {
                String draw = "  ";
                int value = this.tileData[rowIx][colIdx];
                if (value == 1) {
                    draw = "..";
                } else if (value == 2 ) {
                    draw = "oo";
                } else if (value == 3) {
                    draw = "**";
                }
                result += draw;
            }
            result += "\n";
        }
        return result;
    }



}
