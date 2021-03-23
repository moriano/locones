package org.moriano.locones.memory;

/**
 * The PPU exposes eight memory-mapped registers to the CPU.
 *
 * These nominally sit at $2000 through $2007 in the CPU's address space, but because they're incompletely decoded,
 * they're mirrored in every 8 bytes from $2008 through $3FFF, so a write to $3456 is the same as a write to $2006.
 *
 */
public class PPUMemory {

    private int[] registers = new int[8];

    public void set(int address, int value) {
        this.registers[this.getRegisterFromAddress(address)] = value;
    }

    public int getFromAddress(int address) {
        return this.registers[this.getRegisterFromAddress(address)];
    }

    private int getFromRegister(int register) {
        return this.registers[register];
    }

    private int getRegisterFromAddress(int address) {
        return (address - 0x2000) & 0x2007;
    }
}
