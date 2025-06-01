package org.moriano.locones.memory;

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
 */
public class PPURegisterSTATUS {

    private int rawValue;

    public PPURegisterSTATUS(int rawValue) {
        this.rawValue = rawValue;
    }


}
