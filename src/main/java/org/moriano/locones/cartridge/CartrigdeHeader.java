package org.moriano.locones.cartridge;

import org.moriano.locones.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The iNES format (file name suffix .nes) is the de facto standard for distribution of NES binary programs. It consists of the following sections, in order:
 * Header (16 bytes)
 *
 *
 * The format of the header is as follows:
 * 0-3: Constant $4E $45 $53 $1A ("NES" followed by MS-DOS end-of-file)
 * 4: Size of PRG ROM in 16 KB units
 * 5: Size of CHR ROM in 8 KB units (Value 0 means the board uses CHR RAM)
 * 6: Flags 6
 * 7: Flags 7
 * 8: Size of PRG RAM in 8 KB units (Value 0 infers 8 KB for compatibility; see PRG RAM circuit)
 * 9: Flags 9
 * 10: Flags 10 (unofficial)
 * 11-15: Zero filled
 */
public class CartrigdeHeader {

    private static final Logger log = LoggerFactory.getLogger(CartrigdeHeader.class);

    private int prgRomSize;
    private int chrRomSize;
    private int prgRamSize;
    private boolean horizontalMirroring = false;
    private boolean verticalMirroring = false;
    private int mapperNumber;
    private boolean pal = false;
    private boolean trainer = false;


    public CartrigdeHeader(int[] rawBytes) {


        this.prgRomSize = rawBytes[4] * 16 * 1024;
        this.chrRomSize = rawBytes[5] * 8 * 1024;
        this.prgRamSize = rawBytes[8] == 0 ? 8 * 1024 : rawBytes[8] * 8 * 1024;
        int flags6 = rawBytes[6];
        /*
        76543210
        ||||||||
        ||||+||+- 0xx0: vertical arrangement/horizontal mirroring (CIRAM A10 = PPU A11)
        |||| ||   0xx1: horizontal arrangement/vertical mirroring (CIRAM A10 = PPU A10)
        |||| ||   1xxx: four-screen VRAM
        |||| |+-- 1: SRAM in CPU $6000-$7FFF, if present, is battery backed
        |||| +--- 1: 512-byte trainer at $7000-$71FF (stored before PRG data)
        ++++----- Lower nybble of mapper number
         */

        this.horizontalMirroring = ByteUtil.getBit(flags6, 0) == 1 ? true : false;
        this.verticalMirroring = ByteUtil.getBit(flags6, 1) == 1 ? true : false;
        this.trainer = ByteUtil.getBit(flags6, 2) == 1 ? true : false;

        String lowerRawMapper = Integer.toString(ByteUtil.getBit(flags6, 7)) + Integer.toString(ByteUtil.getBit(flags6, 6)) + Integer.toString(ByteUtil.getBit(flags6, 5)) + Integer.toString(ByteUtil.getBit(flags6, 4));

        int flags7 = rawBytes[7];

        /*
        76543210
        ||||||||
        |||||||+- VS Unisystem
        ||||||+-- PlayChoice-10 (8KB of Hint Screen data stored after CHR data)
        ||||++--- If equal to 2, flags 8-15 are in NES 2.0 format
        ++++----- Upper nybble of mapper number
         */
        String upperMapper = Integer.toString(ByteUtil.getBit(flags7, 7)) + Integer.toString(ByteUtil.getBit(flags7, 6)) + Integer.toString(ByteUtil.getBit(flags7, 5)) + Integer.toString(ByteUtil.getBit(flags7, 4));

        this.mapperNumber = Integer.parseInt(upperMapper+lowerRawMapper, 2);

        int flags9 = rawBytes[9];
        /*
        76543210
        ||||||||
        |||||||+- TV system (0: NTSC; 1: PAL)
        +++++++-- Reserved, set to zero
         */
        this.pal = ByteUtil.getBit(flags9, 0) == 1 ? false : true;

    }

    public boolean containsTrainer() {
        return this.trainer;
    }

    public int getMapperNumber() {
        return mapperNumber;
    }

    public boolean isHorizontalMirroring() {
        return horizontalMirroring;
    }

    public boolean isVerticalMirroring() {
        return verticalMirroring;
    }

    public int getPrgRomSize() {
        return prgRomSize;
    }

    public int getChrRomSize() {
        return chrRomSize;
    }

    public int getPrgRamSize() {
        return prgRamSize;
    }

    @Override
    public String toString() {
        return "CartrigdeHeader{" +
                "prgRomSize=" + prgRomSize +
                ", chrRomSize=" + chrRomSize +
                ", prgRamSize=" + prgRamSize +
                ", horizontalMirroring=" + horizontalMirroring +
                ", verticalMirroring=" + verticalMirroring +
                ", mapperNumber=" + mapperNumber +
                ", pal=" + pal +
                '}';
    }
}
