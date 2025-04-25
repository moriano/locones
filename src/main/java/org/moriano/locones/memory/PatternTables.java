package org.moriano.locones.memory;

import org.moriano.locones.screen.Tile;

/**
 * Pattern tables in the NES contain all the different "graphics" that a game can show. This is something that is
 * stored in the cartridge as a read only memory (the CHR ROM).
 * <p>
 * The way in which this works is very interesting, so lets describe it.
 * <p>
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
 *
 * Each pattern table has 256 tiles
 */
public class PatternTables {

    private Tile[] leftPatternTableTiles = new Tile[256];
    private Tile[] rightPatternTableTiles = new Tile[256];

    public PatternTables(Tile[] leftPatternTableTiles, Tile[] rightPatternTableTiles) {
        if (leftPatternTableTiles.length != 256) {
            throw new RuntimeException("Invalid left pattern table, expected 256 tiles but " +
                    "got " + leftPatternTableTiles.length);
        }

        if (rightPatternTableTiles.length != 256) {
            throw new RuntimeException("Invalid right pattern table, expected 256 tiles but " +
                    "got " + rightPatternTableTiles.length);
        }

        this.leftPatternTableTiles = leftPatternTableTiles;
        this.rightPatternTableTiles = rightPatternTableTiles;
    }

    /**
     * The CHR ROM contains the pattern tables.
     *
     * Left one is located between 0x0000 to 0x0FFF
     * Right one is located between 0x1000 to 0x1FFF
     *
     * @param chrRom
     * @return
     */
    public static PatternTables fromCHRRom(int[] chrRom) {
        Tile[] leftPatternTableTiles = findTiles(chrRom, 0x0000, 0x0FFF);
        Tile[] rightPatternTableTiles = findTiles(chrRom, 0x1000, 0x1FFF);

        return new PatternTables(leftPatternTableTiles, rightPatternTableTiles);
    }

    private static Tile[] findTiles(int[] chrRom, int startPosition, int endPosition) {
        Tile[] tiles = new Tile[256];
        int tileNumber = 0;
        for(int i = startPosition; i<endPosition; i=i+16) {
            /*
            Read blocks of 16 bytes and then build tiles from them
             */
            int[] tileRawData = new int[16];
            for (int tileIdx = i; tileIdx<i+16; tileIdx++) {
                System.out.println("CHR ROM at position " + tileIdx + " has value " + chrRom[tileIdx]);
                tileRawData[tileIdx-i] = chrRom[tileIdx];
            }
            Tile aTile = Tile.fromPatternTable(tileRawData);
            tiles[tileNumber] = aTile;
            tileNumber++;
        }
        return tiles;
    }

    public Tile[] getLeftPatternTableTiles() {
        return leftPatternTableTiles;
    }

    public Tile[] getRightPatternTableTiles() {
        return rightPatternTableTiles;
    }
}
