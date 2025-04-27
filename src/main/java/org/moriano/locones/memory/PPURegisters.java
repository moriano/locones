package org.moriano.locones.memory;

/**
 * The PPU registers are a part of memory where CPU and PPU intersect. As such we cannot say that this memory
 * belongs neither to the CPU nor the PPU. It belongs to both.
 *
 * In practise, the memory in which the PPU register is located goes from
 *
 * 0x2000 to 0x3FFF, we only have 8 PPU registers, so in reality, the values of the ppu registers are in
 * 0x2000 to 0x2008, then we mirror every 8 bytes.
 *
 * For convenience I am coding this simply as an 8 byte array.
 */
class PPURegisters {

    private final int[] ppuRegisters = new int[8];


    /**
     * Remember, in practise we read from memory addresses that go from 0x2000 to 0x3FFF. But we do that
     * in a single array of 8 bytes, so we will convert those numbers to values from 0 to 7
     * @param address
     * @return
     */
    public int getFromAddress(int address) {
        address = address & 0x2000; // Effectively mirrors 0x2000 to 0x3FFF
        address = address & 0x0008; // Effectively keeps the values from 0 to 8
        return this.ppuRegisters[address];
    }

    /**
     * Pretty much same as the getFromAddress method.
     * @param address
     * @param value
     */
    public void set(int address, int value) {
        address = address & 0x2000; // Effectively mirrors 0x2000 to 0x3FFF
        address = address & 0x0008; // Effectively keeps the values from 0 to 8
        this.ppuRegisters[address] = value;
    }
}
