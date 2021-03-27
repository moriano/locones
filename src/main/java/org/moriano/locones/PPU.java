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
    private int internalCycles = 0; // DO NOT expose this outside, it does not represent the number of cycles
    private int scanLine = 0;
    private boolean frameComplete = false;
    private Memory memory;

    public PPU(Memory memory, int initialScanLine) {
        this.memory = memory;
        this.scanLine = initialScanLine;
    }

    public void cycle(int currentCPUCycle) {
        this.frameComplete = false;

        this.internalCycles = currentCPUCycle * 3;

        if (this.internalCycles >= 341) {
            this.internalCycles = 0;
            this.scanLine++;
            if (this.scanLine >= 261) {
                this.scanLine = -1;
                this.frameComplete = true;
            }
        }
    }



    public int getScanLine() {
        return this.scanLine;
    }

}
