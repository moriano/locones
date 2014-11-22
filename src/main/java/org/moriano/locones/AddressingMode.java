package org.moriano.locones;

import org.moriano.locones.memory.Memory;

/**
 * Created with IntelliJ IDEA.
 * User: moriano
 * Date: 7/28/13
 * Time: 9:07 AM
 * To change this template use File | Settings | File Templates.
 */
public enum AddressingMode {

    IMPLICIT("Implicit", 1),
    ACCUMULATOR("Accumulator", 2),
    INMEDIATE("Inmediate", 3),
    ZERO_PAGE("Zero page", 4),
    ZERO_PAGE_X("Zero page X", 5),
    ZERO_PAGE_Y("Zero page Y", 6),
    RELATIVE("Relative", 7),
    ABSOLUTE("Absolute", 8),
    ABSOLUTE_X("Absolute X", 9),
    ABSOLUTE_Y("Absolute Y", 10),
    INDIRECT("Indirect", 11),
    INDEXED_INDIRECT("Indexed indirect", 12),
    INDIRECT_INDEXED("Indirect indexed", 13);

    private String name;
    private int id;

    private AddressingMode(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public int getAddress(CPU currentCPU, int argument, Memory memory){
        //TODO this is confusing and seems to SUCK!!
        switch (this.id) {
            case 1:
                /*
                IMPLICIT
                For many 6502 instructions the source and destination of the information to be manipulated is implied
                directly by the function of the instruction itself and no further operand needs to be specified.
                Operations like 'Clear Carry Flag' (CLC) and 'Return from Subroutine' (RTS) are implicit.
                 */
                return 0;
            case 2:
                /*
                ACCUMULATOR ==> Not used yet
                Some instructions have an option to operate directly upon the accumulator. The programmer specifies
                this by using a special operand value, 'A'. For example:
                 */
                return 0; //TODO
            case 3:
                /*
                Immediate ==> Seems to work
                Immediate addressing allows the programmer to directly specify an 8 bit constant within the instruction.
                It is indicated by a '#' symbol followed by an numeric expression. For example:
                 */
                return argument;
            case 4:
                /*
                Zero page ==> Seems to work
                An instruction using zero page addressing mode has only an 8 bit address operand. This limits it to
                addressing only the first 256 bytes of memory (e.g. $0000 to $00FF) where the most significant byte
                of the address is always zero. In zero page mode only the least significant byte of the address is held
                in the instruction making it shorter by one byte (important for space saving) and one less memory fetch
                during execution (important for speed).
                 */
                return argument;
            case 5:
                /*
                Zero page x ==> Not used yet
                The address to be accessed by an instruction using indexed zero page addressing is calculated by taking
                the 8 bit zero page address from the instruction and adding the current value of the X register to it.
                For example if the X register contains $0F and the instruction LDA $80,X is executed then the
                accumulator will be loaded from $008F (e.g. $80 + $0F => $8F).
                 */
                return argument + currentCPU.getRegisterX();
            case 6:
                /*
                Zero page y ==> Not used yet
                The address to be accessed by an instruction using indexed zero page addressing is calculated by taking
                the 8 bit zero page address from the instruction and adding the current value of the Y register to it.
                This mode can only be used with the LDX and STX instructions.
                 */
                return argument + currentCPU.getRegisterY();
            case 7:
                /*
                Relative ==> Not used yet
                Relative addressing mode is used by branch instructions (e.g. BEQ, BNE, etc.) which contain a signed 8
                bit relative offset (e.g. -128 to +127) which is added to program counter if the condition is true. As
                the program counter itself is incremented during instruction execution by two the effective address
                range for the target instruction must be with -126 to +129 bytes of the branch.
                 */
                return argument + currentCPU.getProgramCounter();
            case 8:
                /*
                Absolute ==> Seems to work

                Instructions using absolute addressing contain a full 16 bit address to identify the target location.
                 */
                return argument;
            case 9:
                /*
                Absolute,X ==> Not used yet
                The address to be accessed by an instruction using X register indexed absolute addressing is computed
                by taking the 16 bit address from the instruction and added the contents of the X register. For example
                if X contains $92 then an STA $2000,X instruction will store the accumulator at $2092 (e.g. $2000 + $92).
                 */
                return argument + currentCPU.getRegisterX();
            case 10:
                /*
                Absolute Y ==> Not used yet
                The Y register indexed absolute addressing mode is the same as the previous mode only with the contents
                of the Y register added to the 16 bit address from the instruction.
                 */
                return argument + currentCPU.getRegisterY();
            case 11:
                /*
                Indirect ==> Not used yet

                JMP is the only 6502 instruction to support indirection. The instruction contains a 16 bit address
                which identifies the location of the least significant byte of another 16 bit memory address which is
                the real target of the instruction.

                For example if location $0120 contains $FC and location $0121 contains $BA then the instruction JMP
                ($0120) will cause the next instruction execution to occur at $BAFC (e.g. the contents of $0120 and
                $0121).
                 */
                return memory.read(memory.read(argument));
            case 12:
                /*
                Indexed indirect ==> Seems to work

                Indexed indirect addressing is normally used in conjunction with a table of address held on zero page.
                The address of the table is taken from the instruction and the X register added to it (with zero page
                wrap around) to give the location of the least significant byte of the target address.
                 */
                int lower = memory.read((argument + currentCPU.getRegisterX() ) & 0xFF);
                int higher = memory.read((argument + currentCPU.getRegisterX() + 1) & 0xFF) << 8;
                int finalAddress = higher | lower;
                return finalAddress;
                //throw new UnsupportedOperationException("Indexed indirect addressing not supported!");

            case 13:
                /*
                Indirect indexed ==> Not used yet

                Indirect indirect addressing is the most common indirection mode used on the 6502. In instruction
                contains the zero page location of the least significant byte of 16 bit address. The Y register is
                dynamically added to this value to generated the actual target address for operation.
                 */
                throw new UnsupportedOperationException("Implemente Indirect indexed addressing mode!");
                //return argument + currentCPU.getRegisterY();
            default:
                return 0;
        }
    }
}
