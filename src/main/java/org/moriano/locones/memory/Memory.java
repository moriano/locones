package org.moriano.locones.memory;

import org.moriano.locones.cartridge.Cartridge;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the whole memory of a NES
 *
 * Memory map is
 *
 *  Address Range        Size in bytes       Notes (Page size = 256bytes)
 *   (Hexadecimal)
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
 *   $2000 - $2007       8 bytes             Input / Output registers (PPU REGISTERS) <== Read by PPU and CPU
 *   $2008 - $3FFF       8184 bytes          Mirror of $2000-$2007 (multiple times)
 *
 *   $4000 - $401F       32 bytes            Input / Output registers
 *   $4020 - $5FFF       8160 bytes          Expansion ROM - Used with Nintendo's MMC5 to expand the capabilities of VRAM.
 *
 *   $6000 - $7FFF       8192 bytes          SRAM - Save Ram used to save data between game plays.
 *
 *   $8000 - $BFFF       16384 bytes         PRG-ROM lower bank - executable code
 *   $C000 - $FFFF       16384 bytes         PRG-ROM upper bank - executable code
 *   $FFFA - $FFFB       2 bytes             Address of Non Maskable Interrupt (NMI) handler routine
 *   $FFFC - $FFFD       2 bytes             Address of Power on reset handler routine
 *   $FFFE - $FFFF       2 bytes             Address of Break (BRK instruction) handler routine
 *
 */
public class Memory {

    /*
    Represents really up to address 0x800 (2048).
    The mirror is emulated by reading/writing from/to the same address, that means that if we try to
    read from address 0x801, internally we will just read from address 0x001, in order to do that, a simple
    bitwise AND is enough, using address 0x1FFF (previous value of 0x2000
     */
    private final CPUMemory cpuMemory = new CPUMemory();

    private final APUMemory apuMemory = new APUMemory();
    private final PPUMemory ppuMemory;
    private final PPURegisters ppuRegisters = new PPURegisters();
    private final List<String>  operationsHistory = new ArrayList<>(); // Stores a list of READ/WRITE ops

    private Cartridge cartridge;

    public Memory(Cartridge cartridge) {
        this.cartridge = cartridge;
        this.ppuMemory = new PPUMemory(this.cartridge.getChrROM(), this.ppuRegisters);
    }

    public int read(int address) {
        operationsHistory.add("      READ      $"+toHex(address));
        if(address <= 0x1FFF) { //Ram memory (or any of its three mirrors)
            return this.cpuMemory.getFromAddress(address);
        } else if(address <= 0x3FFF) { //PPU register (mirrored every 8 bytes)
            /*
            These are the memory addresses that CPU and PPU use to communicate with one another.
             */
            return this.ppuRegisters.getFromAddress(address);

        } else if(address <= 0x401F) { //Input/Output registers
            return this.apuMemory.getFromAddress(address);
            //throw new UnsupportedOperationException("Reads to address " + address + " not implemented yet");
        } else if (address <= 0x401F) {
            throw new UnsupportedOperationException("We are not supposed to be reading from " + Integer.toHexString(address) + "[" + address + "]");
        } else if(address <= 0x5FFF) { //Expansion ROM - Used with Nintendo's MMC5 to expand the capabilities of VRAM.
            throw new UnsupportedOperationException("Reads to address " + address + " not implemented yet");
        } else if(address <= 0x7FFF) { //SRAM - Save Ram used to save data between game plays.
            throw new UnsupportedOperationException("Reads to address " + address + " not implemented yet");
        } else if(address <= 0xBFFF ) { //PRG-ROM lower bank - executable code
            int finalAddres = address & 0x7FFF;
            int value = this.cartridge.readPRG(finalAddres);
            return value;
        } else if(address <= 0xFFFF) { //PRG-ROM upper bank - executable code
            int realAddress = address - 0xC000; // TODO Moriano, you really need to understand this (right now you DON'T). Understand how/if mappers impact here
            int value = this.cartridge.readPRG(realAddress);

            return value;
        } else if(address <= 0x10000) {
            return this.cartridge.readPRG(address & 0x3FFF);
        } else {
            throw new IllegalArgumentException("Impossible to read from address " + Integer.toHexString(address) + " [" + address + "]");
        }
    }

    public void write(int address, int value) {
        operationsHistory.add("      WRITE     $"+toHex(address));
        if(value < 0) {
            value += 128;
        }
        if(address <= 0x1FFF) {
            this.cpuMemory.set(address, value);
        } else if(address <=  0x3FFF) { //PPU register (mirrored every 8 bytes)
            /*
            These are the memory addresses that CPU and PPU use to communicate with one another.
             */
            this.ppuRegisters.set(address, value);
        } else if (address <= 0X4017) {
            this.apuMemory.set(address, value);
        } else if (address <= 0xFFFF) {
            throw new IllegalArgumentException("You need to cartridge space to write to " + Integer.toHexString(address) + "[" + address + "]");

        } else {
            throw new IllegalArgumentException("Impossible to write to address " + address);
        }
    }

    public void clearOpHistory() {
        this.operationsHistory.clear();
    }

    public List<String> getOperationsHistory() {
        return operationsHistory;
    }

    private String toHex(int value) {
        String result = Integer.toHexString(value).toUpperCase();
        if (result.length() == 3) {
            result = "0"+result;
        }
        return result.toUpperCase();
    }
}
