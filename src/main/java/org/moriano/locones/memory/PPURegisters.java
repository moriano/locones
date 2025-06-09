package org.moriano.locones.memory;

import org.moriano.locones.Run;
import org.moriano.locones.memory.ppuregister.*;

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


    private PPURegisterCTRL ppuRegisterCTRL = new PPURegisterCTRL(0); // Control register, at 0x2000;
    private PPURegisterMASK ppuRegisterMASK = new PPURegisterMASK(); // Mask register, at 0x2001;
    private PPURegisterSTATUS ppuRegisterSTATUS = new PPURegisterSTATUS(0); //Status 0x2002
    private PPURegisterOamADDR ppuRegisterOamADDR = new PPURegisterOamADDR(); // OAMAddr 0x2003
    private PPURegisterOamDATA ppuRegisterOamDATA = new PPURegisterOamDATA(); // OAMData 0x2004
    private PPURegisterScroll ppuRegisterScroll = new PPURegisterScroll(); // Scroll register 0x2005, 2 bytes
    private PPURegisterADDR ppuRegisterADDR = new PPURegisterADDR(); // Address register 0x2006, 2 bytes
    private PPURegisterDATA ppuRegisterDATA = new PPURegisterDATA(); // Data register at 0x2007

    /**
     * Remember, in practise we read from memory addresses that go from 0x2000 to 0x3FFF. But we do that
     * in a single array of 8 bytes, so we will convert those numbers to values from 0 to 7
     * @param address
     * @return
     */
    public int getFromAddress(int address) {
        if (address < 0x2000 || address > 0x3FFF) {
            throw new RuntimeException("Watch out, we are trying to read a PPU register on address "
                    + Integer.toHexString(address) + " that is an invalid range!!");
        }
        /*
        This converts the values to be from 0 to 7, mirroring every 8 bytes
         */
        int finalAddress = address & 0x7; // Effectively mirrors 0x2000 to 0x3FFF

        if (finalAddress == 0) {
            return this.ppuRegisterCTRL.getRawValue();
        } else if (finalAddress == 1) {
            return this.ppuRegisterMASK.getRawValue();
        } else if (finalAddress == 2) {
            return this.ppuRegisterSTATUS.getRawValue();
        } else if (finalAddress == 3) {
            return this.ppuRegisterOamADDR.getRawValue();
        } else if (finalAddress == 4) {
            return this.ppuRegisterOamDATA.getRawValue();
        } else if (finalAddress == 5) {
            return this.ppuRegisterScroll.getRawValue();
        } else if (finalAddress == 6) {
            return this.ppuRegisterADDR.getRawValue();
        } else if (finalAddress == 7) {
            return this.ppuRegisterDATA.getRawValue();
        } else {
            throw new RuntimeException("What on earth are we trying to read???");
        }
    }

    /**
     * Pretty much same as the getFromAddress method.
     * @param address
     * @param value
     */
    public void set(int address, int value) {
        /*
        This converts the values to be from 0 to 7, mirroring every 8 bytes
         */
        int finalAddress = address & 0x7; // Effectively mirrors 0x2000 to 0x3FFF

        if (finalAddress == 0) {
            this.ppuRegisterCTRL.write(value);
        } else if (finalAddress == 1) {
            this.ppuRegisterMASK.write(value);
        } else if (finalAddress == 2) {
            this.ppuRegisterSTATUS.write(value);
        } else if (finalAddress == 3) {
            this.ppuRegisterOamADDR.write(value);
        } else if (finalAddress == 4) {
            this.ppuRegisterOamDATA.write(value);
        } else if (finalAddress == 5) {
            this.ppuRegisterScroll.write(value);
        } else if (finalAddress == 6) {
            this.ppuRegisterADDR.write(value);
        } else if (finalAddress == 7) {
            this.ppuRegisterDATA.write(value);
        } else {
            throw new RuntimeException("What on earth are we trying to read???");
        }
    }

    public PPURegisterCTRL getPPURegisterCTRL() {
        return new PPURegisterCTRL(0);
    }
}
