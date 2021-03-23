package org.moriano.locones;

import org.moriano.locones.memory.Memory;

/**
 * The PPU exposes eight memory-mapped registers to the CPU.
 *
 * These nominally sit at $2000 through $2007 in the CPU's address space, but because they're incompletely decoded,
 * they're mirrored in every 8 bytes from $2008 through $3FFF, so a write to $3456 is the same as a write to $2006.
 *
 * Created by moriano on 19/09/15.
 */
public class PPU {

    private int[] registers = new int[8];
    private int cycles = 0;
    private int scanLine = 0;
    private int fragmentCycles = 0;
    private boolean frameComplete = false;

    private Memory memory;

    public PPU(Memory memory) {
        this.memory = memory;
    }

    public void cycle() {
        this.cycles++;
        this.frameComplete = false;
        if (this.cycles >= 341) {
            this.cycles = 0;
            this.scanLine++;
            if (this.scanLine >= 261) {
                this.scanLine = -1;
                this.frameComplete = true;
            }
        }
    }

    public int getScanLine() {
        return scanLine;
    }

    public int getCycles() {
        return cycles;
    }

    public int getFragmentCycles() {
        return fragmentCycles;
    }

    public int incrementCycles() {
        this.cycles++;
        return this.cycles;
    }
}
