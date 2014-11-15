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
 */
public class MainMemory {
    private int[] memory = new int[2048];


    public MainMemory() {
        for(int i =0; i<memory.length; i++) {
            this.memory[i] = 0xFF;
        }

        this.memory[0x08] = 0xF7;
        this.memory[0x09] = 0xEF;
        this.memory[0x0A] = 0xDF;
        this.memory[0x0F] = 0xBF;
    }

    public void set(int address, int value) {
        this.memory[address] = value;
    }

    public int getFromAddress(int address) {
        return this.memory[address];
    }
}
