package org.moriano.locones.memory;

/**
 * The NES APU is the audio processing unit in the NES console which generates sound for games.
 * It is implemented in the RP2A03 (NTSC) and RP2A07 (PAL) chips.
 *
 * Its registers are mapped in the range $4000-$4013, $4015 and $4017.
 *
 * TODO No idea what on earth would be at 0x4014
 */
public class APUMemory {

    private int[] memory = new int[24]; // 0x18 elements

    public void set(int address, int value) {
        int target = address - 0x4000;
        this.memory[target] = value;
    }

    public int getFromAddress(int address) {
        int target = address - 0x4000;
        return this.memory[target];
    }
}
