package org.moriano.locones.memory.ppuregister;

import org.moriano.locones.util.ByteUtil;

/**
 * The PPU Status register, see https://www.nesdev.org/wiki/PPU_registers
 *
 * <pre>
 * 7  bit  0
 * ---- ----
 * VSOx xxxx
 * |||| ||||
 * |||+-++++- (PPU open bus or 2C05 PPU identifier)
 * ||+------- Sprite overflow flag
 * |+-------- Sprite 0 hit flag
 * +--------- Vblank flag, cleared on read. Unreliable; see below.
 * </pre>
 *
 * PPUSTATUS (the "status" register) reflects the state of rendering-related events and is primarily used for timing.
 * The three flags in this register are automatically cleared on dot 1 of the prerender scanline; see PPU rendering
 * for more information on the set and clear timing.
 *
 * Reading this register has the side effect of clearing the PPU's internal w register. It is commonly read before
 * writes to PPUSCROLL and PPUADDR to ensure the writes occur in the correct order.
 *
 */
public class PPURegisterSTATUS  extends PPURegister {


    public PPURegisterSTATUS(int rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * Vblank flag
     * The vblank flag is set at the start of vblank (scanline 241, dot 1). Reading PPUSTATUS will return the current
     * state of this flag and then clear it. If the vblank flag is not cleared by reading, it will be cleared
     * automatically on dot 1 of the prerender scanline.
     * @return
     */
    @Override
    public int getRawValue() {
        int toReturn = super.getRawValue();
        /*
        Hacky hack, return a value that sets the vBlank to 1, no matter wyat
         */
        toReturn = ByteUtil.setBit(toReturn, 7, 1);

        /*
        No matter what, reading from this status sets the VBlank to 0
         */
        int newValue = ByteUtil.setBit(super.getRawValue(), 7, 0);
        this.rawValue = newValue;
        return toReturn;
    }

    public boolean isSprintOverFlow() {
        return ByteUtil.getBit(this.rawValue, 5) == 0 ? false : true;
    }

    public boolean isSpriteZeroHit() {
        return ByteUtil.getBit(this.rawValue, 6) == 0 ? false : true;
    }

    public boolean isvBlank() {
        return ByteUtil.getBit(this.rawValue, 7) == 0 ? false : true;
    }
}
