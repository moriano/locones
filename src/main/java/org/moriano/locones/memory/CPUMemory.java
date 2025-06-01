package org.moriano.locones.memory;

/**
 * Created by moriano on 15/11/14.
 * Represents the CPU memory of the NES.
 *
 * This means 2048 bytes of memory.
 *
 * When started
 * All internal memory ($0000-$07FF) was consistently set to $ff except for a few bytes, which probably vary from console to console:
 *
 *  $0008=$F7
 *  $0009=$EF
 *  $000a=$DF
 *  $000f=$BF
 *
 * Memory map
 *
 *   $0000 - $07FF       2048                Game Ram
 *   ($0000 - $00FF)     256                 Zero Page - Special Zero Page addressing modes give faster memory read/write access
 *   ($0100 - $01FF)     256                 Stack memory
 *   ($0200 - $07FF)     1536                RAM
 *
 *   $0800 - $0FFF       2048                Mirror of $0000-$07FF
 *   ($0800 - $08FF)     256                 Zero Page
 *   ($0900 - $09FF)     256                 Stack
 *   ($0A00 - $0FFF)     1024                Ram
 *
 *   $1000 - $17FF       2048 bytes          Mirror of $0000-$07FF
 *   ($1000 - $10FF)     256                 Zero Page
 *   $1100 - $11FF       256                 Stack
 *   $1200 - $17FF       1024                RAM
 *
 *   $1800 - $1FFF       2048 bytes          Mirror of $0000-$07FF
 *   ($1800 - $18FF)     256                 Zero Page
 *   ($1900 - $19FF)     256                 Stack
 *   ($1A00 - $1FFF)     1024                RAM
 *
 */
class CPUMemory {
    private int[] memory = new int[2048];


    public CPUMemory() {
        for(int i =0; i<memory.length; i++) {
            this.memory[i] = 0xFF;
        }

        this.memory[0x08] = 0xF7;
        this.memory[0x09] = 0xEF;
        this.memory[0x0A] = 0xDF;
        this.memory[0x0F] = 0xBF;
    }

    public void set(int address, int value) {
        this.memory[address & 0x07FF] = value;
    }

    public int getFromAddress(int address) {
        return this.memory[address & 0x07FF];
    }
}
