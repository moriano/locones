package org.moriano.locones.memory;

import org.moriano.locones.AddressingMode;
import org.moriano.locones.CPU;
import org.moriano.locones.cartridge.Cartridge;

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
 *   $2000 - $2007       8 bytes             Input / Output registers
 *   $2008 - $3FFF       8184 bytes          Mirror of $2000-$2007 (mulitple times)
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
    private final MainMemory mainMemory = new MainMemory();

    private Cartridge cartridge;

    public Memory(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    public int read(CPU cpu, AddressingMode addresingMode, int value) {
        if(addresingMode == AddressingMode.ACCUMULATOR) {
            return cpu.getRegisterA();
        } else if(addresingMode == AddressingMode.INMEDIATE) {
            return value;
        } else {
            int address = addresingMode.getAddress(cpu, value, this);
            return this.read(address);
        }
    }

    public int read(int address) {

        if(address <= 0x1FFF) { //Ram memory (or any of its three mirrors)
            return this.mainMemory.getFromAddress(address & 0x7FF);
        } else if(address <= 0x3FFF) { //PPU register (mirrored every 8 bytes)
            throw new UnsupportedOperationException("Reads to address " + address + " not implemented yet");
        } else if(address <= 0x401F) { //Input/Output registers
            throw new UnsupportedOperationException("Reads to address " + address + " not implemented yet");
        } else if(address <= 0x5FFF) { //Expansion ROM - Used with Nintendo's MMC5 to expand the capabilities of VRAM.
            throw new UnsupportedOperationException("Reads to address " + address + " not implemented yet");
        } else if(address <= 0x7FFF) { //SRAM - Save Ram used to save data between game plays.
            throw new UnsupportedOperationException("Reads to address " + address + " not implemented yet");
        } else if(address <= 0xBFFF ) { //PRG-ROM lower bank - executable code
            int finalAddres = address & 0x7FFF;
            int value = this.cartridge.readPRG(finalAddres);
            return value;
        } else if(address <= 0xFFFF) { //PRG-ROM upper bank - executable code

            int realAddress = address - 0xC000;
            int value = this.cartridge.readPRG(realAddress);

            return value;
        } else if(address <= 0x10000) {
            return this.cartridge.readPRG(address & 0x3FFF);
        } else {
            throw new IllegalArgumentException("Impossible to read from address " + address);
        }
    }



    public void write(CPU cpu, AddressingMode addresingMode, int address, int value) {
        if(addresingMode == AddressingMode.ACCUMULATOR) {
            throw new UnsupportedOperationException("Addressing mode accumulator not supported");
        } else if(addresingMode == AddressingMode.INMEDIATE) {
            /*
            Immediate addressing allows the programmer to directly specify an 8 bit constant within the instruction.
            It is indicated by a '#' symbol followed by an numeric expression. For example:
             */
            this.write(address, value);
        } else if(addresingMode == AddressingMode.ZERO_PAGE) {
            /*
            An instruction using zero page addressing mode has only an 8 bit address operand. This limits it to
            addressing only the first 256 bytes of memory (e.g. $0000 to $00FF) where the most significant byte of
            the address is always zero. In zero page mode only the least significant byte of the address is held in
            the instruction making it shorter by one byte (important for space saving) and one less memory fetch
            during execution (important for speed).

            An assembler will automatically select zero page addressing mode if the operand evaluates to a zero page
            address and the instruction supports the mode (not all do).
             */
            this.write(address, value);
        } else if(addresingMode == AddressingMode.ABSOLUTE) {
            /*
            Instructions using absolute addressing contain a full 16 bit address to identify the target location.
             */
            this.write(address, value);
        } else if(addresingMode == AddressingMode.INDEXED_INDIRECT) {
            //throw new UnsupportedOperationException("REVIEW THIS MATE!");
            int finalAddress = addresingMode.getAddress(cpu, address, this);
            this.write(finalAddress, value);
            cpu.incrementProgramCounter(); // TODO not sure about this

        } else if(addresingMode == AddressingMode.INDIRECT_INDEXED) {
            //throw new UnsupportedOperationException("REVIEW THIS MATE!");
            int finalAddress = addresingMode.getAddress(cpu, address, this);
            this.write(finalAddress, value);
            cpu.incrementProgramCounter(); // TODO not sure about this
        } else if(addresingMode == AddressingMode.ABSOLUTE_Y) {
            int finalAddress = addresingMode.getAddress(cpu, address, this);
            this.write(finalAddress, value);
            cpu.incrementProgramCounter(); // TODO not sure about this
        } else if(addresingMode == AddressingMode.ZERO_PAGE_X) {
            int finalAddress = addresingMode.getAddress(cpu, address, this);
            this.write(finalAddress, value);
        } else {       //TODO implement the rest of addressing modes...
            //this.write(address, value);
            //cpu.incrementProgramCounter();
            throw new UnsupportedOperationException("Ouch!, addressing mode " + addresingMode + " not supported");
            //address = addresingMode.getAddress(cpu, value, this);
            //this.write(address, value);
        }
    }

    public void write(int address, int value) {
        if(address <= 0x1FFF) {
            this.mainMemory.set(address & 0x7FF, value);
        } else {
            throw new IllegalArgumentException("Impossible to write to address " + address);
        }
    }
}
