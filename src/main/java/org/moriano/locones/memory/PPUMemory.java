package org.moriano.locones.memory;

/**
 * The PPU exposes eight memory-mapped registers to the CPU.
 *
 * These nominally sit at $2000 through $2007 in the CPU's address space, but because they're incompletely decoded,
 * they're mirrored in every 8 bytes from $2008 through $3FFF, so a write to $3456 is the same as a write to $2006.
 *
 * PPU memory includes
 *
 * 0x0000 to 0x1FFF pattern memory. This info comes from the Cartridge CHR ROM. This is static
 * 0x2000 to 0x2FFF name table this is dynamically changed
 * 0x3F00 to 0x3FFF palettes
 *
 */
class PPUMemory {

    private int[] patternMemory = new int[8*1024];
    private int[] nameTableMemory = new int[8*1024];
    private int[] paletteMemory = new int[256];
    private PPURegisters ppuRegisters;





    public PPUMemory(int[] chrROM, PPURegisters ppuRegisters) {
        this.ppuRegisters = ppuRegisters;
        if (chrROM.length != 8*1024) {
            throw new RuntimeException("Watch out, passed a CHR ROM to the PPU of size " + chrROM.length +
                    " we expected 8192");
        }
        this.patternMemory = chrROM;
    }

    public void set(int address, int value) {
        if (address <= 0x1FFF) {
            throw new RuntimeException("Watch out!! You cannot write into the CHR ROM of the PPU, that is static!");
        } else if (address <= 0x2000) {

        }
    }

    public int getFromAddress(int address) {
        if (address <= 0x1FFF) { // Reading the PatternMemory
            return this.patternMemory[address];
        } else if (address <= 0x2000) { // Reading from NameTable
            return 0;
        } else if (address <= 0x3FFF) { // Reading from Palettes
            return 0;
        }
        return 0;

    }

}
