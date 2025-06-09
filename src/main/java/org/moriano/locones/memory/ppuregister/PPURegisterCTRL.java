package org.moriano.locones.memory.ppuregister;

import org.moriano.locones.screen.SpriteSize;
import org.moriano.locones.util.ByteUtil;

import javax.print.DocFlavor;

/**
 * The CTRL PPU register. See https://www.nesdev.org/wiki/PPU_registers
 *
 * <pre>
 * 7  bit  0
 * ---- ----
 * VPHB SINN
 * |||| ||||
 * |||| ||++- Base nametable address
 * |||| ||    (0 = $2000; 1 = $2400; 2 = $2800; 3 = $2C00)
 * |||| |+--- VRAM address increment per CPU read/write of PPUDATA
 * |||| |     (0: add 1, going across; 1: add 32, going down)
 * |||| +---- Sprite pattern table address for 8x8 sprites
 * ||||       (0: $0000; 1: $1000; ignored in 8x16 mode)
 * |||+------ Background pattern table address (0: $0000; 1: $1000)
 * ||+------- Sprite size (0: 8x8 pixels; 1: 8x16 pixels – see PPU OAM#Byte 1)
 * |+-------- PPU master/slave select
 * |          (0: read backdrop from EXT pins; 1: output color on EXT pins)
 * +--------- Vblank NMI enable (0: off, 1: on)
 * </pre>
 *
 * PPUCTRL (the "control" or "controller" register) contains a mix of settings related to rendering, scroll position,
 * vblank NMI, and dual-PPU configurations. After power/reset, writes to this register are ignored until the first
 * pre-render scanline.
 *
 * For implementation purposes, we expose directly the booleans as way to facilitate the code
 *
 */
public class PPURegisterCTRL  extends PPURegister {

    private int rawValue;

    public PPURegisterCTRL(int rawValue) {
        this.rawValue = rawValue;
    }

    public int getBaseNameTableAddress() {
        int low = ByteUtil.getBit(this.rawValue, 0);
        int high = ByteUtil.getBit(this.rawValue, 1) * 2;
        int total = low + high;
        if (total == 0) {
            return 0x2000;
        } else if (total == 1) {
            return 0x2400;
        } else if (total == 2) {
            return 0x2800;
        } else {
            return 0x2C00;
        }
    }

    public int getRawValue() {
        return rawValue;
    }

    public int getVramAddress() {
        throw new RuntimeException("Not supported!");
    }

    public int getSpritePatternTableAddress() {
        int value = ByteUtil.getBit(this.rawValue, 3);
        if (value == 0) {
            return 0x0000;
        } else {
            return 0x1000;
        }
    }

    public int getBackgrounPatternTableAddress() {
        if (ByteUtil.getBit(this.rawValue, 4) == 1) {
            return 0x1000;
        } else {
            return 0x0000;
        }

    }


    public SpriteSize getSpriteSize() {
        if (ByteUtil.getBit(this.rawValue, 5) == 1) {
            return SpriteSize.SIZE_8x8_PIXELS;
        } else {
            return SpriteSize.SIZE_8x16_PIXELS;
        }
    }

    public boolean isMaster() {
        return ByteUtil.getBit(this.rawValue, 6) == 0;
    }

    public boolean isVblankNMIEnable() {
        return ByteUtil.getBit(this.rawValue, 7) == 1;
    }


}
