package org.moriano.locones;


import com.sun.jndi.cosnaming.IiopUrl;
import org.moriano.locones.memory.Memory;
import org.moriano.locones.util.ByteUtil;
import org.moriano.locones.util.LogReader;
import org.moriano.locones.util.LogStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created with IntelliJ IDEA.
 * User: moriano
 * Date: 7/28/13
 * Time: 9:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class CPU {

    private static final Logger log = LoggerFactory.getLogger(CPU.class);

    private int registerX;
    private int registerY;
    private int registerA; //Accumulator

    /*
    The NMOS 65xx processors have 256 bytes of stack memory, ranging
    from $0100 to $01FF. The S register is a 8-bit offset to the stack
    page. In other words, whenever anything is being pushed on the
    stack, it will be stored to the address $0100+S.

    The Stack pointer can be read and written by transfering its value
    to or from the index register X (see below) with the TSX and TXS
    instructions.
     */
    private int registerS; //Stack register

    /*
    This register points the address from which the next instruction
    byte (opcode or parameter) will be fetched. Unlike other
    registers, this one is 16 bits in length. The low and high 8-bit
    halves of the register are called PCL and PCH, respectively. The
    Program Counter may be read by pushing its value on the stack.
    This can be done either by jumping to a subroutine or by causing
    an interrupt.
     */
    private int programCounter;

    private boolean carryFlag;
    private boolean zeroFlag;
    private boolean interruptDisable;
    private boolean decimalMode;
    private boolean breakCommand;
    private boolean overflowFlag;
    private boolean negativeFlag;

    private int cycles;

    private Memory memory;


    private int lastCode = 0;

    private transient String firstInstructionArg =  "";
    private transient String secondInstructionArg = "";

    private int iteration = 0;

    private LogReader logReader = new LogReader();

    public CPU() {
        /*
        The following results are from a US (NTSC) NES, original front-loading design, RP2A03G CPU chip, NES-CPU-07
         main board revision, manufactured in 1988. The memory values are probably slightly different for each
         individual NES console. Please note that you should NOT rely on the state of any registers after Power-UP
         and especially not the stack register and RAM ($0000-$07FF).

        At power-up

            P = $34 (IRQ disabled)*
            A, X, Y = 0
            S = $FD
         */
        this.zeroFlag = false;
        this.interruptDisable = true;
        this.registerA = 0;
        this.registerX = 0;
        this.registerY = 0;
        this.registerS = 0xFD;
        this.programCounter = 0xC000;
    }

    public int incrementProgramCounter() {
        this.programCounter++;
        return this.programCounter;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    /**
     * The cpu has an extra register called P. Such register is used for status flags, however it is not declared as a
     * register in the code.
     *
     * Reason is, it is just easier to use a bunch of booleans in order to represent the behaviour, however it may be
     * desirable to calculate the value for testing purposes.
     *
     * The P register is a byte looking like this
     *
     * 7654 3210
     * || | ||||
     * || | |||+- C: 1 if last addition or shift resulted in a carry, or if
     * || | |||   last subtraction resulted in no borrow
     * || | ||+-- Z: 1 if last operation resulted in a 0 value
     * || | |+--- I: Interrupt priority level
     * || | |     (0: /IRQ and /NMI get through; 1: only /NMI gets through)
     * || | +---- D: 1 to make ADC and SBC use binary-coded decimal arithmetic
     * || |       (ignored on second-source 6502 like that in the NES)
     * || +------ (exists only on the stack copy, see note below)
     * |+-------- V: 1 if last ADC or SBC resulted in signed overflow,
     * |          or D6 from last BIT
     * +--------- N: Set to bit 7 of the last operation
     *
     * @return
     */
    private int calculateRegisterP() {

        int registerP = 0;
        if(this.carryFlag) {
            registerP += 1;
        }

        if(this.zeroFlag) {
            registerP += 2;
        }

        if(this.interruptDisable) {
            registerP += 4;
        }

        if(this.decimalMode) {
            registerP += 8;
        }



        if(this.overflowFlag) {
            registerP += 64;
        }

        /*
        The third byte should be set to 1, but on the logtest is set to zero so...
         */
        registerP += 32;

        if(this.negativeFlag) {
            registerP += 128;
        }

        return registerP;
    }

    /**
     * Performs a simple CPU cycle that means.
     *
     * 1-Read from memory and fetch the operation code
     * 2-Interpret the operation code and fetch the argument (if needed)
     * 3-Execute the instruction, that may or may not alter the CPU and memory statys
     *
     *
     */
    public void cycle() {
        iteration++;
        int opCode = this.memory.read(this.programCounter);
        this.lastCode = opCode;
        String hex = Integer.toHexString(opCode).toUpperCase();
        String hexPC = Integer.toHexString(this.programCounter).toUpperCase();
        int oldPC = this.programCounter;
        String instruction = null;

        String oldXHex = Integer.toHexString(this.getRegisterX()).toUpperCase();
        String oldYHex = Integer.toHexString(this.getRegisterY()).toUpperCase();
        String oldAHex = Integer.toHexString(this.getRegisterA()).toUpperCase();
        String oldSPHex = Integer.toHexString(this.registerS).toUpperCase();

        int oldX = this.getRegisterX();
        int oldP = this.calculateRegisterP();
        int oldY = this.getRegisterY();
        int oldA = this.getRegisterA();
        int oldSp = this.registerS;

        String addresingModeSymbol = "";

        this.firstInstructionArg ="";
        this.secondInstructionArg ="";

        if(oldXHex.length() == 1) {
            oldXHex = "0"+oldXHex;
        }

        if(oldYHex.length() == 1) {
            oldYHex = "0"+oldYHex;
        }

        if(oldAHex.length() == 1) {
            oldAHex = "0"+oldAHex;
        }

        if(hex.length() == 1) {
            hex = "0"+hex;
        }

        int registerP = calculateRegisterP();
        String hexRegisterP = Integer.toHexString(registerP).toUpperCase();

        switch (opCode) {
            //ADC
            case 0x69:
                instruction = "ADC";
                this.ADC(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x65:
                instruction = "ADC";
                this.ADC(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x75:
                instruction = "ADC";
                this.ADC(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x6D:
                instruction = "ADC";
                this.ADC(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x7D:
                instruction = "ADC";
                this.ADC(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x79:
                instruction = "ADC";
                this.ADC(AddressingMode.ABSOLUTE_Y, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x61:
                instruction = "ADC";
                this.ADC(AddressingMode.INDEXED_INDIRECT, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x71:
                instruction = "ADC";
                this.ADC(AddressingMode.INDIRECT_INDEXED, this.getInstructionArg(1));
                this.programCounter++;
                break;

            //AND
            case 0x29:
                instruction = "AND";
                this.AND(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x25:
                instruction = "AND";
                this.AND(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x35:
                instruction = "AND";
                this.AND(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x2D:
                instruction = "AND";
                this.AND(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x3D:
                instruction = "AND";
                this.AND(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x39:
                instruction = "AND";
                this.AND(AddressingMode.ABSOLUTE_Y, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x21:
                instruction = "AND";
                this.AND(AddressingMode.INDEXED_INDIRECT, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x31:
                instruction = "AND";
                this.AND(AddressingMode.INDIRECT_INDEXED, this.getInstructionArg(1));
                this.programCounter++;
                break;

            //ASL
            case 0x0A:
                instruction = "ASL";
                this.ASL(AddressingMode.ACCUMULATOR, this.registerA); //TODO Check this
                this.programCounter++; //TODO check this
                break;
            case 0x06:
                instruction = "ASL";
                this.ASL(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x16:
                instruction = "ASL";
                this.ASL(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x0E:
                instruction = "ASL";
                this.ASL(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x1E:
                instruction = "ASL";
                this.ASL(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;

            //BCC
            case 0x90:
                instruction = "BCC";
                this.BCC(this.getInstructionArg(1));
                this.programCounter++;
                break;

            //BCS
            case 0xB0:
                instruction = "BCS";
                this.BCS(this.getInstructionArg(1));
                this.programCounter++;
                break;

            //BEQ
            case 0xF0:
                instruction = "BEQ";
                this.BEQ(this.getInstructionArg(1));
                this.programCounter++;
                break;

            //BIT
            case 0x24:
                instruction = "BIT";
                this.BIT(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;

            case 0x2C:
                instruction = "BIT";
                this.BIT(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;


            //BMI
            case 0x30:
                instruction = "BMI";
                this.BMI(this.getInstructionArg(1));
                this.programCounter++;
                break;

            //BNE
            case 0xD0:
                instruction = "BNE";
                this.BNE(this.getInstructionArg(1));
                this.programCounter++;
                break;

            //BPL
            case 0x10:
                instruction = "BPL";
                this.BPL(this.getInstructionArg(1));
                this.programCounter++;
                break;

            //BRK
            case 0x00:
                throw new UnsupportedOperationException("BRK instruction not implemented!");

            //BVC
            case 0x50:
                instruction = "BVC";
                this.BVC(this.getInstructionArg(1));
                this.programCounter++;
                break;

            //BVS
            case 0x70:
                instruction = "BVS";
                this.BVS(this.getInstructionArg(1));
                this.programCounter++;
                break;

            //CLC
            case 0x18:
                instruction = "CLC";
                this.CLC();
                this.programCounter++;
                break;

            //CLD
            case 0xD8:
                this.CLD();
                instruction = "CLD";
                this.programCounter++;
                break;

            //CLI
            case 0x58:
                instruction = "CLI";
                this.CLI();
                this.programCounter++;
                break;

            //CLV
            case 0xB8:
                this.CLV();
                instruction = "CLV";
                this.programCounter++;
                break;

            //CMP
            case 0xC9:
                instruction = "CMP";
                this.CMP(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xC5:
                instruction = "CMP";
                this.CMP(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xD5:
                instruction = "CMP";
                this.CMP(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xCD:
                instruction = "CMP";
                this.CMP(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0xDD:
                instruction = "CMP";
                this.CMP(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0xD9:
                instruction = "CMP";
                this.CMP(AddressingMode.ABSOLUTE_Y, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0xC1:
                instruction = "CMP";
                this.CMP(AddressingMode.INDEXED_INDIRECT, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xD1:
                instruction = "CMP";
                this.CMP(AddressingMode.INDIRECT_INDEXED, this.getInstructionArg(1));
                this.programCounter++;
                break;

            //CPX
            case 0xE0:
                this.CPX(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                instruction = "CPX";
                this.programCounter++;
                break;
            case 0xE4:
                this.CPX(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                instruction = "CPX";
                this.programCounter++;
                break;
            case 0xEC:
                this.CPX(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                instruction = "CPX";
                this.programCounter++;
                break;

            //CPY
            case 0xC0:
                this.CPY(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                instruction = "CPY";
                this.programCounter++;
                break;
            case 0xC4:
                this.CPY(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                instruction = "CPY";
                this.programCounter++;
                break;
            case 0xCC:
                this.CPY(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                instruction = "CPY";
                this.programCounter++;
                break;

            //DEC
            case 0xC6:
                instruction = "DEC";
                this.DEC(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xD6:
                instruction = "DEC";
                this.DEC(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xCE:
                instruction = "DEC";
                this.DEC(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0xDE:
                instruction = "DEC";
                this.DEC(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;

            //DEX
            case 0xCA:
                instruction = "DEX";
                this.DEX();
                this.programCounter++;
                break;

            //DEY
            case 0x88:
                instruction = "DEY";
                this.DEY();
                this.programCounter++;
                break;

            //EOR
            case 0x49:
                this.EOR(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                instruction = "EOR";
                this.programCounter++;
                break;
            case 0x45:
                this.EOR(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                instruction = "EOR";
                this.programCounter++;
                break;
            case 0x55:
                this.EOR(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                instruction = "EOR";
                this.programCounter++;
                break;
            case 0x4D:
                this.EOR(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                instruction = "EOR";
                this.programCounter++;
                break;
            case 0x5D:
                this.EOR(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                instruction = "EOR";
                this.programCounter++;
                break;
            case 0x59:
                this.EOR(AddressingMode.ABSOLUTE_Y, this.getInstructionArg(2));
                instruction = "EOR";
                this.programCounter++;
                break;
            case 0x41:
                this.EOR(AddressingMode.INDEXED_INDIRECT, this.getInstructionArg(1));
                instruction = "EOR";
                this.programCounter++;
                break;
            case 0x51:
                this.EOR(AddressingMode.INDIRECT_INDEXED, this.getInstructionArg(1));
                instruction = "EOR";
                this.programCounter++;
                break;

            //INC
            case 0xE6:
                instruction = "INC";
                this.INC(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xF6:
                instruction = "INC";
                this.INC(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xEE:
                instruction = "INC";
                this.INC(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0xFE:
                instruction = "INC";
                this.INC(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;

            //INX
            case 0xE8:
                instruction = "INX";
                this.INX();
                this.programCounter++;
                break;

            //INY
            case 0xC8:
                instruction = "INY";
                this.INY();
                this.programCounter++;
                break;

            //JMP
            case 0x4C:
                instruction = "JMP";
                int arg = this.getInstructionArg(2);
                this.JMP(AddressingMode.ABSOLUTE, arg);
                break;
            case 0x6C:
                instruction = "JMP";
                this.JMP(AddressingMode.INDIRECT, this.getInstructionArg(2));
                break;

            //JSR
            case 0x20:
                instruction = "JSR";
                this.JSR(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                break;

            //LAX => Unofficial instruction
            case 0xA3:
                instruction = "LAX";
                this.LAX(AddressingMode.INDEXED_INDIRECT, this.getInstructionArg(1));
                this.programCounter++;
                break;

            case 0xA7:
                instruction = "LAX";
                this.LAX(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;

            case 0xAF:
                instruction = "LAX";
                this.LAX(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;

            case 0xB3:
                instruction = "LAX";
                this.LAX(AddressingMode.INDIRECT_INDEXED, this.getInstructionArg(1));
                this.programCounter++;
                break;

            case 0xB7:
                instruction = "LAX";
                this.LAX(AddressingMode.ZERO_PAGE_Y, this.getInstructionArg(1));
                this.programCounter++;
                break;

            case 0xBF:
                instruction = "LAX";
                this.LAX(AddressingMode.ABSOLUTE_Y, this.getInstructionArg(2));
                this.programCounter++;
                break;

            //LDA
            case 0xA9:
                instruction = "LDA";
                this.LDA(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xA5:
                instruction = "LDA";
                this.LDA(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xB5:
                instruction = "LDA";
                this.LDA(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xAD:
                instruction = "LDA";
                this.LDA(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0xBD:
                instruction = "LDA";
                this.LDA(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0xB9:
                instruction = "LDA";
                this.LDA(AddressingMode.ABSOLUTE_Y, this.getInstructionArg(2));
                this.programCounter++;
                break;

            case 0xA1:
                instruction = "LDA";
                this.LDA(AddressingMode.INDEXED_INDIRECT, this.getInstructionArg(1));
                this.programCounter++;
                break;


            case 0xB1:
                instruction = "LDA";
                this.LDA(AddressingMode.INDIRECT_INDEXED, this.getInstructionArg(1));
                this.programCounter++;
                break;

            //LDX
            case 0xA2:
                instruction = "LDX";
                this.LDX(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xA6:
                instruction = "LDX";
                this.LDX(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xB6:
                instruction = "LDX";
                this.LDX(AddressingMode.ZERO_PAGE_Y, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xAE:
                instruction = "LDX";
                this.LDX(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0xBE:
                instruction = "LDX";
                this.LDX(AddressingMode.ABSOLUTE_Y, this.getInstructionArg(2));
                this.programCounter++;
                break;

            //LDY
            case 0xA0:
                this.LDY(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                instruction = "LDY";
                this.programCounter++;
                break;
            case 0xA4:
                this.LDY(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                instruction = "LDY";
                this.programCounter++;
                break;
            case 0xB4:
                this.LDY(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                instruction = "LDY";
                this.programCounter++;
                break;
            case 0xAC:
                this.LDY(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                instruction = "LDY";
                this.programCounter++;
                break;
            case 0xBC:
                this.LDY(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                instruction = "LDY";
                this.programCounter++;
                break;

            //LSR
            case 0x4A:
                instruction = "LSR";
                this.LSR(AddressingMode.ACCUMULATOR, this.registerA); //TODO Check this
                this.programCounter++; //TODO check this
                break;
            case 0x46:
                instruction = "LSR";
                this.LSR(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x56:
                instruction = "LSR";
                this.LSR(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x4E:
                instruction = "LSR";
                this.LSR(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x5E:
                instruction = "LSR";
                this.LSR(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;

            //NOP
            case 0xEA:
                instruction = "NOP";
                this.NOP();
                this.programCounter++;
                break;

            case 0x1A:
                instruction = "NOP";
                this.NOP();
                this.programCounter++;
                break;

            case 0x3A:
                instruction = "NOP";
                this.NOP();
                this.programCounter++;
                break;

            case 0x5A:
                instruction = "NOP";
                this.NOP();
                this.programCounter++;
                break;

            case 0x7A:
                instruction = "NOP";
                this.NOP();
                this.programCounter++;
                break;

            case 0xDA:
                instruction = "NOP";
                this.NOP();
                this.programCounter++;
                break;
            case 0xFA:
                instruction = "NOP";
                this.NOP();
                this.programCounter++;
                break;


            //And with you unnoficial codes, in this case DOP (Double NOP), still programmed as a NOP
            //DOP
            case 0x04:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0x44:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0x64:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0x74:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0xD4:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0xF4:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0x80:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0x82:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0x89:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0xC2:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0xE2:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0x14:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0x34:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            case 0x54:
                instruction ="NOP";
                this.getInstructionArg(1);
                this.NOP();
                this.programCounter++;
                break;

            //And with you unnoficial codes, in this case TOP (Triple NOP), still programmed as a NOP
            //TOP
            case 0x0C:
                instruction ="NOP";
                this.getInstructionArg(2);
                this.NOP();
                this.programCounter++;
                break;

            case 0x1C:
                instruction ="NOP";
                this.getInstructionArg(2);
                this.NOP();
                this.programCounter++;
                break;

            case 0x3C:
                instruction ="NOP";
                this.getInstructionArg(2);
                this.NOP();
                this.programCounter++;
                break;

            case 0x5C:
                instruction ="NOP";
                this.getInstructionArg(2);
                this.NOP();
                this.programCounter++;
                break;

            case 0x7C:
                instruction ="NOP";
                this.getInstructionArg(2);
                this.NOP();
                this.programCounter++;
                break;

            case 0xDC:
                instruction ="NOP";
                this.getInstructionArg(2);
                this.NOP();
                this.programCounter++;
                break;

            case 0xFC:
                instruction ="NOP";
                this.getInstructionArg(2);
                this.NOP();
                this.programCounter++;
                break;



            //ORA
            case 0x09:
                this.ORA(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                instruction = "ORA";
                this.programCounter++;
                break;
            case 0x05:
                this.ORA(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                instruction = "ORA";
                this.programCounter++;
                break;
            case 0x15:
                instruction = "ORA";
                this.ORA(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x0D:
                instruction = "ORA";
                this.ORA(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x1D:
                instruction = "ORA";
                this.ORA(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x19:
                instruction = "ORA";
                this.ORA(AddressingMode.ABSOLUTE_Y, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x01:
                this.ORA(AddressingMode.INDEXED_INDIRECT, this.getInstructionArg(1));
                instruction = "ORA";
                this.programCounter++;
                break;
            case 0x11:
                this.ORA(AddressingMode.INDIRECT_INDEXED, this.getInstructionArg(1));
                instruction = "ORA";
                this.programCounter++;
                break;

            //PHA
            case 0x48:
                this.PHA();
                instruction = "PHA";
                this.programCounter++;
                break;

            //PHP
            case 0x08:
                this.PHP();
                instruction = "PHP";
                this.programCounter++;
                break;

            //PLA
            case 0x68:
                instruction = "PLA";
                this.PLA();
                this.programCounter++;
                break;

            //PLP
            case 0x28:
                instruction = "PLP";
                this.PLP();
                this.programCounter++;
                break;

            //ROL
            case 0x2A:
                instruction = "ROL";
                this.ROL(AddressingMode.ACCUMULATOR, 0);
                this.programCounter++;
                break;
            case 0x26:
                instruction = "ROL";
                this.ROL(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x36:
                instruction = "ROL";
                this.ROL(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x2E:
                instruction = "ROL";
                this.ROL(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x3E:
                instruction = "ROL";
                this.ROL(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;

            //ROR
            case 0x6A:
                instruction = "ROR";
                this.ROR(AddressingMode.ACCUMULATOR, 0);
                this.programCounter++;
                break;
            case 0x66:
                instruction = "ROR";
                this.ROR(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x76:
                instruction = "ROR";
                this.ROR(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x6E:
                instruction = "ROR";
                this.ROR(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x7E:
                instruction = "ROR";
                this.ROR(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;

            //RTI
            case 0x40:
                instruction = "RTI";
                this.RTI();
                break;

            //RTS
            case 0x60:
                instruction = "RTS";
                this.RTS();
                break;

            //SAX => Unofficial instruction
            case 0x83:
                instruction = "SAX";
                this.SAX(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                this.programCounter++;
                break;

            //SBC
            case 0xE9:
                instruction = "SBC";
                this.SBC(AddressingMode.INMEDIATE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xE5:
                instruction = "SBC";
                this.SBC(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xF5:
                instruction = "SBC";
                this.SBC(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xED:
                instruction = "SBC";
                this.SBC(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0xFD:
                instruction = "SBC";
                this.SBC(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0xF9:
                instruction = "SBC";
                this.SBC(AddressingMode.ABSOLUTE_Y, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0xE1:
                instruction = "SBC";
                this.SBC(AddressingMode.INDEXED_INDIRECT, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0xF1:
                instruction = "SBC";
                this.SBC(AddressingMode.INDIRECT_INDEXED, this.getInstructionArg(1));
                this.programCounter++;
                break;

            //SEC
            case 0x38:
                instruction = "SEC";
                this.SEC();
                this.programCounter++;
                break;

            //SED
            case 0xF8:
                instruction = "SED";
                this.SED();
                this.programCounter++;
                break;

            //SEI
            case 0x78:
                instruction = "SEI";
                this.SEI();
                this.programCounter++;
                break;

            //STA
            case 0x85:
                instruction = "STA";
                this.STA(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x95:
                instruction = "STA";
                this.STA(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x8D:
                instruction = "STA";
                this.STA(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;
            case 0x9D:
                instruction = "STA";
                this.STA(AddressingMode.ABSOLUTE_X, this.getInstructionArg(2));
                break;
            case 0x99:
                instruction = "STA";
                this.STA(AddressingMode.ABSOLUTE_Y, this.getInstructionArg(2));
                break;
            case 0x81:
                instruction = "STA";
                this.STA(AddressingMode.INDEXED_INDIRECT, this.getInstructionArg(1));
                break;
            case 0x91:
                instruction = "STA";
                this.STA(AddressingMode.INDIRECT_INDEXED, this.getInstructionArg(1));
                break;

            //STX
            case 0x86:
                instruction = "STX";
                this.STX(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x96:
                instruction = "STX";
                this.STX(AddressingMode.ZERO_PAGE_Y, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x8E:
                instruction = "STX";
                this.STX(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;

            //STY
            case 0x84:
                instruction = "STY";
                this.STY(AddressingMode.ZERO_PAGE, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x94:
                instruction = "STY";
                this.STY(AddressingMode.ZERO_PAGE_X, this.getInstructionArg(1));
                this.programCounter++;
                break;
            case 0x8C:
                instruction = "STY";
                this.STY(AddressingMode.ABSOLUTE, this.getInstructionArg(2));
                this.programCounter++;
                break;

            //TAX
            case 0xAA:
                instruction = "TAX";
                this.TAX();
                this.programCounter++;
                break;

            //TAY
            case 0xA8:
                instruction = "TAY";
                this.TAY();
                this.programCounter++;
                break;

            //TSX
            case 0xBA:
                instruction = "TSX";
                this.TSX();
                this.programCounter++;
                break;

            //TXA
            case 0x8A:
                instruction = "TXA";
                this.TXA();
                this.programCounter++;
                break;

            //TXS
            case 0x9A:
                instruction = "TXS";
                this.TXS();
                this.programCounter++;
                break;

            //TYA
            case 0x98:
                instruction = "TYA";
                this.TYA();
                this.programCounter++;
                break;

            //default:
            //    throw new UnsupportedOperationException("Unknown opCode " + opCode);
        }

        String instructionArgument = this.firstInstructionArg + this.secondInstructionArg;
        instructionArgument = instructionArgument.replace(" ", "");

        if(instructionArgument.length() > 0) {
            instructionArgument = addresingModeSymbol + "$" + instructionArgument;
        }

        while(instructionArgument.length() <= 4) {
            instructionArgument += " ";
        }

        String firstAndSecondInstructions = this.firstInstructionArg + "-" + this.secondInstructionArg;
        //firstAndSecondInstructions = firstAndSecondInstructions.replace(" ", "");
        firstAndSecondInstructions = firstAndSecondInstructions.replace("-", " ");

        while(firstAndSecondInstructions.length() <= 4) {
            firstAndSecondInstructions += " ";
        }

        String ok = null;
        boolean stop = false;

        if(this.checkIterationSanity(instruction, oldPC, iteration, oldA, oldX, oldY, oldSp, oldP)) {
            ok = "OK";
        } else {
            stop = true;
        }

        String iterationStr = "";
        if(iteration < 10) {
            iterationStr = "   "+iteration;
        } else if(iteration < 100) {
            iterationStr = "  "+iteration;
        } else if(iteration < 1000) {
            iterationStr = " "+iteration;
        } else if(iteration < 10000) {
            iterationStr = " "+iteration;
        }

        System.out.printf("%s %s  %s %s  %s %s\t\t\tA:%s X:%s Y:%s P:%s SP:%s    %s\n",
                iterationStr, hexPC, hex, firstAndSecondInstructions, instruction, instructionArgument, oldAHex, oldXHex, oldYHex, hexRegisterP, oldSPHex, ok);
        if(!this.zeroFlag) {
            int a = 1;
        }

        if(stop) {
            System.exit(1);
        }

    }

    private boolean checkIterationSanity(String instruction, int programCounter, int iteration, int oldA, int oldX, int oldY, int oldSP, int oldP) {
        LogStatus status = this.logReader.getLogStatus(iteration);

        if(status.getAddress() == programCounter &&
                status.getInstruction().equals(instruction) &&
                status.getRegisterA() == oldA &&
                status.getRegisterP() == oldP &&
                status.getRegisterSP() == oldSP &&
                status.getRegisterX() == oldX &&
                status.getRegisterY() == oldY) {
            return true;
        } else {
            System.out.println("\n");
            if (status.getAddress() != programCounter) {
                System.out.printf("Address does not match, expected vs current %s -- %s\n", Integer.toHexString(status.getAddress()).toUpperCase(), Integer.toHexString(programCounter).toUpperCase());
            }
            if (!status.getInstruction().equals(instruction)) {
                System.out.printf("Instruction does not match, expected vs current %s -- %s\n", status.getInstruction(), instruction );
            }
            if (status.getRegisterA() != oldA) {
                System.out.printf("Register A does not match, expected vs current %d -- %d\n", status.getRegisterA(), oldA );
            }
            if (status.getRegisterSP() != oldSP) {
                System.out.printf("SP does not match, expected vs current %d -- %d\n", status.getRegisterSP(), oldSP );
            }
            if (status.getRegisterX() != oldX) {
                System.out.printf("Register X does not match, expected vs current %d -- %d\n", status.getRegisterX(), oldX );
            }
            if (status.getRegisterY() != oldY) {
                System.out.printf("Register Y does not match, expected vs current %d -- %d\n", status.getRegisterY(), oldY );
            }
            if(status.getRegisterP() != oldP) {
                System.out.printf("Register P does not match, expected vs current %d -- %d\n", status.getRegisterP(), oldP );

            }
            return false;
        }

    }

    public void run() {
        while(true) {
            this.cycle();

        }

    }

    /**
     * Some instructions will use an argument compossed of several memory buckets.
     *
     * This instruction performs the needed calculation
     * @param totalBytes
     * @return
     */
    private int getInstructionArg(int totalBytes) {
        if(totalBytes == 1) {
            int arg = this.memory.read(this.programCounter + 1);
            this.programCounter += 1;
            this.firstInstructionArg = Integer.toHexString(arg).toUpperCase();
            if(this.firstInstructionArg.length() == 1) {
                this.firstInstructionArg = "0"+this.firstInstructionArg;
            }
            this.secondInstructionArg = "  ";
            return arg;
        }
        else if(totalBytes == 2) {
            int second = this.memory.read(this.programCounter + 1);
            int first = this.memory.read(this.programCounter + 2);

            this.firstInstructionArg = Integer.toHexString(first).toUpperCase();
            if(this.firstInstructionArg.length() == 1) {
                this.firstInstructionArg = "0"+this.firstInstructionArg;
            }

            this.secondInstructionArg = Integer.toHexString(second).toUpperCase();
            if(this.secondInstructionArg.length() == 1) {
                this.secondInstructionArg = "0"+this.secondInstructionArg;
            }


            this.programCounter += 2;
            int arg = first + (first * 0xFF) + second;
            return arg;
        } else {
            throw new UnsupportedOperationException("Unspported operation for a totalBytes value of " + totalBytes);
        }
    }

    /**
     * ADC - Add with Carry
     *
     * This instruction adds the contents of a memory location to the accumulator together with the carry bit.
     * If overflow occurs the carry bit is set, this enables multiple byte addition to be performed.
     *
     * Logic:
     *    t = A + M + P.C
     *    P.V = (A.7!=t.7) ? 1:0
     *    P.N = A.7
     *    P.Z = (t==0) ? 1:0
     *    IF (P.D)
     *      t = bcd(A) + bcd(M) + P.C
     *      P.C = (t>99) ? 1:0
     *    ELSE
     *      P.C = (t>255) ? 1:0
     *    A = t & 0xFF
     *
     * @param addressingMode
     * @param arg
     */
    private void ADC(AddressingMode addressingMode, int arg) {

        int value = this.memory.read(this, addressingMode, arg);
        int result = this.registerA + value + (this.carryFlag ? 1 : 0);  //TODO ---> check negative flags, if so, you have to substract mate!!

        /*
        Belive it or not...

        Formulas for the overflow flag
        There are several different formulas that can be used to compute the overflow bit. By checking the eight cases in the above table, these formulas can easily be verified.
        A common definition of overflow is V = C6 xor C7. That is, overflow happens if the carry into bit 7 is different from the carry out.

        A second formula simply expresses the two lines that cause overflow: if the sign bits (M7 and N7) are 0 and the carry in is 1, or the sign bits are 1 and the carry in is 0:
        V = (!M7&!N7&C6) | (M7&N7&!C6)

        The above formula can be manipulated with De Morgan's laws to yield the formula that is actually implemented in the 6502 hardware:
        V = not (((m7 nor n7) and c6) nor ((M7 nand N7) nor c6))
        Overflow can be computed simply in C++ from the inputs and the result. Overflow occurs if (M^result)&(N^result)&0x80 is nonzero. That is, if the sign of both inputs is different from the sign of the result. (Anding with 0x80 extracts just the sign bit from the result.) Another C++ formula is !((M^N) & 0x80) && ((M^result) & 0x80). This means there is overflow if the inputs do not have different signs and the input sign is different from the output sign (link).

        Detailed explanation ==> http://www.righto.com/2012/12/the-6502-overflow-flag-explained.html
         */
        boolean overflow;
        boolean carry;


        int M = value;
        int N = this.registerA;

        int aux = (M ^ result)&(N^result)&0x80;

        if(aux != 0) {
            overflow = true;
        }  else {
            overflow = false;
        }

        if(this.decimalMode) {
            //TODO truly horrible way of converting to binary coded decimals.
            //See http://en.wikipedia.org/wiki/Binary-coded_decimal
            int high = Integer.valueOf(Integer.toHexString((value & 15) >> 4));
            int low = Integer.valueOf(Integer.toHexString(value & 128));

            int temp = this.registerA + ((high*10) + low)+ (this.carryFlag ? 1 : 0);
            carry = temp > 99 ? true : false;
        } else {
            carry = result > 0xFF ? true : false;
        }

        this.registerA = result & 0xFF;
        this.negativeFlag = ByteUtil.getBit(this.registerA, 7) == 1 ? true : false; //negative;
        this.overflowFlag = overflow;
        this.zeroFlag = this.registerA == 0 ? true : false;
        this.carryFlag = carry;
    }

    /**
     * AND - Logical AND
     *
     * A logical AND is performed, bit by bit, on the accumulator contents using the contents of a byte of memory.
     */
    private void AND(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);

        this.registerA &= value;

        if(this.registerA == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(this.registerA > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }

    }

    /**
     * ASL - Arithmetic Shift Left
     *
     * This operation shifts all the bits of the accumulator or memory contents one bit left. Bit 0 is set to 0 and
     * bit 7 is placed in the carry flag. The effect of this operation is to multiply the memory contents by 2
     * (ignoring 2's complement considerations), setting the carry if the result will not fit in 8 bits.
     *
     */
    private void ASL(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);
        int result = (value << 1) & 0xFF;

        this.carryFlag = ByteUtil.getBit(value, 7) == 1 ? true : false;


        this.zeroFlag = result == 0 ? true : false;
        this.negativeFlag = result > 127 ? true : false;

        if(addressingMode.equals(AddressingMode.ACCUMULATOR)) {
            this.registerA = result;

        } else {
            //Check if this is correct mate
            int address = addressingMode.getAddress(this, arg, this.memory);
            this.memory.write(address, result);
            //throw new UnsupportedOperationException("Ouch!");
        }

    }

    /**
     * BCS - Branch if Carry clear
     * If the carry flag is clear then add the relative displacement to the program counter to cause a branch to a new location.
     * @param arg
     */
    private void BCC(int arg) {
        if(!this.isCarryFlag()) {
            this.programCounter += arg;
        }
    }

    /**
     * BCS - Branch if Carry Set
     * If the carry flag is set then add the relative displacement to the program counter to cause a branch to a new location.
     * @param arg
     */
    private void BCS(int arg) {
        if(this.carryFlag) {
            this.programCounter += arg;
        }
    }

    /**
     * BEQ - Branch if Equal
     * If the zero flag is set then add the relative displacement to the program counter to cause a branch to a new location.
     * @param arg
     */
    private void BEQ(int arg) {
        if(this.isZeroFlag()) {
            this.programCounter += arg;
        }
    }

    /**
     * BIT - Bit Test
     *
     * This instructions is used to test if one or more bits are set in a target memory location. The mask pattern in
     * A is ANDed with the value in memory to set or clear the zero flag, but the result is not kept. Bits 7 and 6 of
     * the value from memory are copied into the N and V flags.
     *
     *  t = A & M
     *  P.N = t.7
     *  P.V = t.6
     *  P.Z = (t==0) ? 1:0
     *
     *
     *
     * @param arg
     */
    private void BIT(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);
        int result = this.registerA & value;
        if(result == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        int bit6 = ByteUtil.getBit(value, 6);
        this.overflowFlag = bit6 == 0 ? false : true;
        int bit7 = ByteUtil.getBit(value, 7);
        this.negativeFlag = bit7 == 0 ? false : true;
    }



    /**
     * BMI - Branch if Minus
     * If the negative flag is set then add the relative displacement to the program counter to cause a branch to a new location.
     * @param arg
     */
    private void BMI(int arg) {
        if(this.isNegativeFlag()) {
            this.programCounter += arg;
        }
    }

    /**
     * BNE - Branch if Not Equal
     * If the zero flag is clear then add the relative displacement to the program counter to cause a branch to a new
     * location.
     * @param arg
     */
    private void BNE(int arg) {
        if(!this.isZeroFlag()) {
            //Boy, this arg could represent a negative number so...
            if(ByteUtil.getBit(arg, 7) == 1) {
                this.programCounter += (arg - 0xFF) - 1; //TODO CHECK ALL BRANCH INSTRUCTIONS to consider negative values!!!!!
            } else {
                this.programCounter += arg;
            }
        }
    }

    /**
     * BPL - Branch if Positive
     * If the negative flag is clear then add the relative displacement to the program counter to cause a branch to a
     * new location.
     * @param arg
     */
    private void BPL(int arg) {
        if(!this.isNegativeFlag()) {
            this.programCounter += arg;
        }
    }

    /**
     * BRK - Force Interrupt
     *
     * The BRK instruction forces the generation of an interrupt request. The program counter and processor status are
     * pushed on the stack then the IRQ interrupt vector at $FFFE/F is loaded into the PC and the break flag in the
     * status set to one.
     */
    private void BRK() {
        this.breakCommand = true;
        this.cycles += 7;
    }

    /**
     * BVC - Branch if Overflow Clear
     * If the overflow flag is clear then add the relative displacement to the program counter to cause a branch to a
     * new location.
     * @param arg
     */
    private void BVC(int arg) {
        if(!this.isOverflowFlag()) {
            this.programCounter += arg;
        }
    }

    /**
     * BVS - Branch if Overflow Set
     * If the overflow flag is set then add the relative displacement to the program counter to cause a branch to a new
     * location.
     * @param arg
     */
    private void BVS(int arg) {
        if(this.isOverflowFlag()) {
            this.programCounter += arg;
        }
    }

    /**
     * CLC - Clear Carry Flag
     * Set the carry flag to zero.
     */
    private void CLC() {
        this.carryFlag = false;
        this.cycles += 2;
    }

    /**
     * CLD - Clear Decimal Mode
     * Sets the decimal mode flag to zero.
     */
    private void CLD() {
        this.decimalMode = false;
        this.cycles += 2;
    }

    /**
     * CLI - Clear Interrupt Disable
     * Clears the interrupt disable flag allowing normal interrupt requests to be serviced.
     */
    private void CLI() {
        this.interruptDisable = false;
        this.cycles += 2;
    }

    /**
     * CLV - Clear Overflow Flag
     * Clears the overflow flag.
     */
    private void CLV() {
        this.overflowFlag = false;
        this.cycles += 2;
    }

    /**
     * CMP - Compare
     * This instruction compares the contents of the accumulator with another memory held value and sets the zero and
     * carry flags as appropriate.
     * @param addressingMode
     * @param arg
     */
    private void CMP(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);
        if(this.registerA == value) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(this.registerA >= value) {
            this.carryFlag = true;
        } else {
            this.carryFlag = false;
        }

        int t = this.registerA - value;
        if(t > 127 || t < 0) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }

        int a = this.calculateRegisterP();
    }

    /**
     * CPX - Compare X Register
     * This instruction compares the contents of the X register with another memory held value and sets the zero and carry flags as appropriate.
     */
    private void CPX(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);

        if(this.registerX >= value) {
            this.carryFlag = true;
        } else {
            this.carryFlag = false;
        }

        if(this.registerX == value) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        int t = this.registerX - value;
        if(t > 127 || t < 0) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }
    }

    /**
     * CPY - Compare Y Register
     * This instruction compares the contents of the Y register with another memory held value and sets the zero and carry flags as appropriate.
     * @param addressingMode
     * @param arg
     */
    private void CPY(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);

        if(this.registerY >= value) {
            this.carryFlag = true;
        } else {
            this.carryFlag = false;
        }

        if(this.registerY == value) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        int t = this.registerY - value;
        if(t > 127 || t < 0) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }
    }

    /**
     * DEC - Decrement Memory
     * Subtracts one from the value held at a specified memory location setting the zero and negative flags as
     * appropriate.
     * @param addressingMode
     * @param arg
     */
    private void DEC(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);
        value--;

        value = value & 0xFF;

        if(value == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(value > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }

        int address = addressingMode.getAddress(this, arg, this.memory);
        this.memory.write(address, value);

    }

    /**
     * DEX - Decrement X Register
     *
     * Subtracts one from the X register setting the zero and negative flags as appropriate.
     */
    private void DEX() {
        int value = this.registerX;
        value--;

        value = value & 0xFF;

        if(value == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(value > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }

        this.registerX = value;

    }

    /**
     * DEY - Decrement Y Register
     *
     * Subtracts one from the Y register setting the zero and negative flags as appropriate.
     */
    private void DEY() {
        int value = this.registerY;
        value--;

        value = value & 0xFF;

        if(value == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(value > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }


        this.registerY = value;

    }

    /**
     * EOR - Exclusive OR
     *
     * An exclusive OR is performed, bit by bit, on the accumulator contents using the contents of a byte of memory.
     * @param addressingMode
     * @param arg
     */
    private void EOR(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);

        this.registerA ^= value;

        if(this.registerA == 0) {
            this.zeroFlag = true;
        } else if(this.registerA > 127) {
            this.negativeFlag = true;
        }

    }

    /**
     * INC - Increment Memory
     *
     * Adds one to the value held at a specified memory location setting the zero and negative flags as appropriate.
     */
    private void INC(AddressingMode addressingMode, int arg) {
        int address = addressingMode.getAddress(this, arg, this.memory);
        int value = this.memory.read(address);
        value++;

        value = value & 0xFF;

        if(value == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(value > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }

        this.memory.write(address, value);
    }

    /**
     * INX - Increment X Register
     *
     * Adds one to the X register setting the zero and negative flags as appropriate.
     *
     */
    private void INX() {
        this.registerX++;

        this.registerX = this.registerX & 0xFF;

        if(this.registerX == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(this.registerX > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }

    }

    /**
     * INY - Increment Y Register
     *
     * Adds one to the Y register setting the zero and negative flags as appropriate.
     *
     */
    private void INY() {
        this.registerY++;

        this.registerY = this.registerY & 0xFF;



        if(this.registerY == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(this.registerY > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }
    }

    /**
     * JMP - Jump
     *
     * Sets the program counter to the address specified by the operand.
     *
     * @param addressingMode
     * @param arg
     */
    private void JMP(AddressingMode addressingMode, int arg) {
        /*
        http://forums.nesdev.com/viewtopic.php?t=6621&start=15

        DBB5  6C FF 02  JMP ($02FF) = A900              A:60 X:07 Y:00 P:65 SP:F9 CYC:180 SL:63
        0300  A9 AA     LDA #$AA                        A:60 X:07 Y:00 P:65 SP:F9 CYC:195 SL:63

        JMP ($02FF) = A900, and after that the PC is at 0300, and not at A900. did i get wrong the indirect addressing
         mode?[/code]

        Answer => Indirect jump is bugged on the 6502, it doesn't add 1 to the full 16-bit value when it reads the
        second byte, it adds 1 to the low byte only. So JMP (03FF) reads from 3FF and 300, not 3FF and 400.


         */

        int address = addressingMode.getAddress(this, arg, this.memory);
        this.programCounter = address;
    }

    /**
     * JSR - Jump to Subroutine
     *
     * The JSR instruction pushes the address (minus one) of the return point on to the stack and then sets the
     * program counter to the target memory address.
     */
    private void JSR(AddressingMode addressingMode, int arg) {
        /*
        Note that this instruction needs to store the PC into the stack, now, the stack holds BYTES and the PC
        is bigger than that, so it has to be split and TWO pushes to the stack will be needed.

        So, lets assume we want to store

        0xC601 into the stack, this number is composed of TWO bytes

        Hight byte (0xC6) is 1100 0110
        Low byte   (0x01) is 0000 0001

        If we play with bitwise operators

        0xC601 & 0xFF00 = 0xC600;   0xC600 >> 8 ==> 0x00C6 ==> We store this as the high byte

        0xC601 & 0xFF   = 0x01 ==> We store this as the low byte
         */
        int rawValue = this.programCounter; //This does not feel right...

        int highByte = (rawValue & 0xFF00) >> 8;
        int lowByte = rawValue & 0xFF;

        this.stackPush(highByte);
        this.stackPush(lowByte);

        this.programCounter = arg;
    }

    /**
     * LAX - Load accumulator and X register with memory.
     *
     * This is an UNOFFICIAL instruction
     * Status flags: N,Z
     *
     * This opcode loads both the accumulator and the X register with the contents of a memory location.
     *
     * @param addressingMode
     * @param arg
     */
    private void LAX(AddressingMode addressingMode, int arg) {
        this.LDA(addressingMode, arg);
        this.LDX(addressingMode, arg);
    }

    /**
     * LDA - Load Accumulator
     *
     * Loads a byte of memory into the accumulator setting the zero and negative flags as appropriate.
     */
    private void LDA(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);

        this.registerA = value;
        if(this.registerA == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(this.registerA > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }

        int a = this.calculateRegisterP();

    }

    /**
     * LDX - Load X Register
     *
     * Loads a byte of memory into the X register setting the zero and negative flags as appropriate.
     */
    private void LDX(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);

        this.registerX = value;
        if(this.registerX == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(this.registerX > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }

    }

    /**
     * LDY - Load Y Register
     *
     * Loads a byte of memory into the Y register setting the zero and negative flags as appropriate.
     */
    private void LDY(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);

        this.registerY = value;
        if(this.registerY == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(this.registerY > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }
    }

    /**
     * LSR - Logical Shift Right
     *
     * Each of the bits in A or M is shift one place to the right. The bit that was in bit 0 is shifted into the carry
     * flag. Bit 7 is set to zero.
     */
    private void LSR(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);
        int result = value >> 1;

        this.carryFlag = ByteUtil.getBit(value, 0) == 1 ? true : false;
        this.negativeFlag = false;
        this.zeroFlag = result == 0 ? true : false;

        if(addressingMode.equals(AddressingMode.ACCUMULATOR)) {
            this.registerA = result;
        } else {
            //TODO check if this correct mate!
            int address = addressingMode.getAddress(this, arg, this.memory);
            this.memory.write(address, result);
        }








    }

    /**
     * The NOP instruction causes no changes to the processor other than the normal incrementing of the program counter
     * to the next instruction.
     */
    private void NOP() {

    }

    /**
     * ORA - Logical Inclusive OR
     *
     * An inclusive OR is performed, bit by bit, on the accumulator contents using the contents of a byte of memory.
     */
    private void ORA(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);

        this.registerA = this.registerA | value;

        if(this.registerA == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(this.registerA > 127) {
            this.negativeFlag = true;
        }

    }

    /**
     * PHA - Push Accumulator
     *
     * Pushes a copy of the accumulator on to the stack.
     */
    private void PHA() {
        this.stackPush(this.registerA);
    }

    /**
     * PHP - Push Processor Status
     *
     * Pushes a copy of the status flags on to the stack.
     */
    private void PHP() {

        int regP = this.calculateRegisterP();

        /*
        Bit 4 is always set when PHP is used
        see http://wiki.nesdev.com/w/index.php/CPU_status_flag_behavior
        */

        regP += 16;

        this.stackPush(regP);
    }

    /**
     * PLA - Pull Accumulator
     *
     * Pulls an 8 bit value from the stack and into the accumulator. The zero and negative flags are set as appropriate.
     */
    private void PLA() {

        int value = this.stackPop();
        //
        // value = value - 16;
        this.registerA = value;
        if(this.registerA == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(value > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }
    }

    /**
     * PLP - Pull Processor Status
     * Pulls an 8 bit value from the stack and into the processor flags. The flags will take on new states as determined by the value pulled.
     *
     * Remember that the code does not have a processor status but a set of flags.
     *
     * The P register is a byte looking like this
     *
     * 7654 3210
     * || | ||||
     * || | |||+- C: 1 if last addition or shift resulted in a carry, or if
     * || | |||   last subtraction resulted in no borrow
     * || | ||+-- Z: 1 if last operation resulted in a 0 value
     * || | |+--- I: Interrupt priority level
     * || | |     (0: /IRQ and /NMI get through; 1: only /NMI gets through)
     * || | +---- D: 1 to make ADC and SBC use binary-coded decimal arithmetic
     * || |       (ignored on second-source 6502 like that in the NES)
     * || +------ (exists only on the stack copy, see note below)
     * |+-------- V: 1 if last ADC or SBC resulted in signed overflow,
     * |          or D6 from last BIT
     * +--------- N: Set to bit 7 of the last operation
     *
     */
    private void PLP() {
        int value = this.stackPop();
        this.carryFlag = ByteUtil.getBit(value, 0) == 1 ? true : false;
        this.zeroFlag = ByteUtil.getBit(value, 1) == 1 ? true : false;
        this.interruptDisable = ByteUtil.getBit(value, 2) == 1 ? true : false;
        this.decimalMode = ByteUtil.getBit(value, 3) == 1 ? true : false;
        this.overflowFlag = ByteUtil.getBit(value, 6) == 1 ? true : false;
        this.negativeFlag = ByteUtil.getBit(value, 7) == 1 ? true : false;


    }

    /**
     * ROL - Rotate Left
     * Move each of the bits in either A or M one place to the left. Bit 0 is filled with the current value of the
     * carry flag whilst the old bit 7 becomes the new carry flag value.
     */
    private void ROL(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);
        int result = ((value<<1) + (this.carryFlag ? 1 : 0)) & 0xFF;

        this.carryFlag = ByteUtil.getBit(value, 7) == 1 ? true : false;
        this.negativeFlag = result > 127;
        this.zeroFlag = result == 0 ? true : false;

        if(addressingMode.equals(AddressingMode.ACCUMULATOR)) {
            this.registerA = result;

        } else {
            int address = addressingMode.getAddress(this, arg, this.memory);
            this.memory.write(address, result);
        }
    }

    /**
     * ROR - Rotate Right
     *
     * Move each of the bits in either A or M one place to the right. Bit 7 is filled with the current value of the
     * carry flag whilst the old bit 0 becomes the new carry flag value.
     */
    private void ROR(AddressingMode addressingMode, int arg) {
        int value = this.memory.read(this, addressingMode, arg);
        int result = (value>>1) + (this.carryFlag ? 128 : 0);

        this.carryFlag = ByteUtil.getBit(value, 0) == 1 ? true : false;
        this.negativeFlag = result > 127;
        this.zeroFlag = result == 0 ? true : false;
        if(addressingMode.equals(AddressingMode.ACCUMULATOR)) {
            this.registerA = result;

        } else {
            int address = addressingMode.getAddress(this, arg, this.memory);
            this.memory.write(address, result);
            //TODO Check
            //throw new UnsupportedOperationException("Ouch!");
        }

    }

    /**
     * RTI - Return from Interrupt
     *
     * The RTI instruction is used at the end of an interrupt processing routine. It pulls the processor flags from the
     * stack followed by the program counter.
     *
     */
    private void RTI() {
        int rawStatus = this.stackPop();
        int low = this.stackPop();
        int high = this.stackPop();
        int total = (high << 8) + (low & 0xFF);

        this.carryFlag = ByteUtil.getBit(rawStatus, 0) == 1 ? true : false;
        this.zeroFlag = ByteUtil.getBit(rawStatus, 1) == 1 ? true : false;
        this.interruptDisable = ByteUtil.getBit(rawStatus, 2) == 1 ? true : false;
        this.decimalMode = ByteUtil.getBit(rawStatus, 3) == 1 ? true : false;
        this.overflowFlag = ByteUtil.getBit(rawStatus, 6) == 1 ? true : false;
        this.negativeFlag = ByteUtil.getBit(rawStatus, 7) == 1 ? true : false;


        this.programCounter = total;
    }

    /**
     * RTS - Return from Subroutine
     *
     * The RTS instruction is used at the end of a subroutine to return to the calling routine. It pulls the program
     * counter (minus one) from the stack.
     */
    private void RTS() {
        /*
        Remember, as this needs to get the PC from the stack, AND the stack does only holds single bytes
        while the PC is composed of two bytes, two pull operations will be needed and the final value
        will need to be assembled by using bitwise operations.
         */

        int lowByte = this.stackPop();
        int highByte = this.stackPop();

        int address = (highByte << 8) + lowByte;
        this.programCounter = address + 1;


    }


    /**
     * SAX => Unofficial operation code
     * SAX ANDs the contents of the A and X registers (leaving the contents of A
     * intact), subtracts an immediate value, and then stores the result in X.
     *
     * ... A few points might be made about the action of subtracting an immediate
     * value.  It actually works just like the CMP instruction, except that CMP
     * does not store the result of the subtraction it performs in any register.
     * This subtract operation is not affected by the state of the Carry flag,
     * though it does affect the Carry flag.  It does not affect the Overflow
     * flag.
     *
     * Example:
     *      SAX #$5A        ;CB 5A
     *
     *    Equivalent instructions:
     *      STA $02
     *      TXA
     *      AND $02
     *      SEC
     *      SBC #$5A
     *      TAX
     *      LDA $02
     *
     *
     *      OR MAYBE
     *
     *      AND X register with accumulator and store result in memory.
     Status flags: N,Z
     * @param addressingMode
     * @param arg
     */
    private void SAX(AddressingMode addressingMode, int arg) {
        int finalAddress = addressingMode.getAddress(this, arg, this.memory);
        int result = this.registerX & this.registerA;
        this.memory.write(finalAddress, result);

        //this.zeroFlag = result == 0 ? true : false;
        //this.negativeFlag = result > 127 ? true : false;
        //TODO status flags?
    }

    /**
     * SBC - Subtract with Carry
     *
     * This instruction subtracts the contents of a memory location to the accumulator together with the not of the
     * carry bit. If overflow occurs the carry bit is clear, this enables multiple byte subtraction to be performed.
     *
     * Logic:
     *    IF (P.D)
     *      t = bcd(A) - bcd(M) - !P.C
     *      P.V = (t>99 OR t<0) ? 1:0
     *    ELSE
     *      t = A - M - !P.C
     *      P.V = (t>127 OR t<-128) ? 1:0
     *
     *    P.C = (t>=0) ? 1:0
     *    P.N = t.7
     *    P.Z = (t==0) ? 1:0
     *    A = t & 0xFF
     *
     * @param addressingMode
     * @param arg
     */
    private void SBC(AddressingMode addressingMode, int arg) {

        int value = this.memory.read(this, addressingMode, arg);

        int result = this.registerA - value - (this.carryFlag ? 0 : 1);

         /*
        Belive it or not...

        Formulas for the overflow flag
        There are several different formulas that can be used to compute the overflow bit. By checking the eight cases in the above table, these formulas can easily be verified.
        A common definition of overflow is V = C6 xor C7. That is, overflow happens if the carry into bit 7 is different from the carry out.

        A second formula simply expresses the two lines that cause overflow: if the sign bits (M7 and N7) are 0 and the carry in is 1, or the sign bits are 1 and the carry in is 0:
        V = (!M7&!N7&C6) | (M7&N7&!C6)

        The above formula can be manipulated with De Morgan's laws to yield the formula that is actually implemented in the 6502 hardware:
        V = not (((m7 nor n7) and c6) nor ((M7 nand N7) nor c6))
        Overflow can be computed simply in C++ from the inputs and the result. Overflow occurs if (M^result)&(N^result)&0x80 is nonzero. That is, if the sign of both inputs is different from the sign of the result. (Anding with 0x80 extracts just the sign bit from the result.) Another C++ formula is !((M^N) & 0x80) && ((M^result) & 0x80). This means there is overflow if the inputs do not have different signs and the input sign is different from the output sign (link).

        Detailed explanation ==> http://www.righto.com/2012/12/the-6502-overflow-flag-explained.html
         */
        int M = value;
        int N = this.registerA;
        boolean overflow;
        boolean carry;

        //Taken from the HalfNES emulator code...
        boolean myOverflow = (((N ^ value) & 0x80) != 0) && (((N ^ result) & 0x80) != 0);

        if(myOverflow) {
            overflow = true;
        }  else {
            overflow = false;
        }

        if(this.decimalMode) {
            //TODO truly horrible way of converting to binary coded decimals.
            //See http://en.wikipedia.org/wiki/Binary-coded_decimal
            int high = Integer.valueOf(Integer.toHexString((value & 15) >> 4));
            int low = Integer.valueOf(Integer.toHexString(value & 128));
            int temp = this.registerA - ((high*10) - low) - (this.carryFlag ? 1 : 0);
            carry = temp >= 0 ? true : false;
        } else {
            carry = result >=0 ? true : false;
        }

        this.registerA = result & 0xFF;
        this.negativeFlag = ByteUtil.getBit(this.registerA, 7) == 1 ? true : false;
        this.overflowFlag = overflow;
        this.zeroFlag = this.registerA == 0 ? true : false;
        this.carryFlag = carry;

    }



    /**
     * SEC Set carry flag
     */
    private void SEC() {
        this.carryFlag = true;
    }

    /**
     * SED Set decimal flag
     */
    private void SED() {
        this.decimalMode = true;
    }

    /**
     * SEI Set interrupt disable
     */
    private void SEI() {
        this.interruptDisable = true;
    }

    /**
     * STA - Store Accumulator
     *
     * Stores the contents of the accumulator into memory.
     */
    private void STA(AddressingMode addressingMode, int arg) {
        this.memory.write(this, addressingMode, arg, this.getRegisterA());
    }

    /**
     * STX - Store register x
     *
     * Stores the contents of the register x into memory.
     */
    private void STX(AddressingMode addressingMode, int arg) {
        this.memory.write(this, addressingMode, arg, this.getRegisterX());
    }

    /**
     * STX - Store register y
     *
     * Stores the contents of the register y into memory.
     */
    private void STY(AddressingMode addressingMode, int arg) {
        this.memory.write(this, addressingMode, arg, this.getRegisterY());
    }

    /**
     * TAX - Transfer Accumulator to X
     *
     * Copies the current contents of the accumulator into the X register and sets the zero and negative flags as
     * appropriate.
     */
    private void TAX() {
        this.registerX = this.registerA;

        if(this.registerX == 0){
            this.zeroFlag = true;
        } else if(this.registerX > 127) {
            this.negativeFlag = true;
        }
    }

    /**
     * TAY - Transfer Accumulator to Y
     *
     * Copies the current contents of the accumulator into the Y register and sets the zero and negative flags as
     * appropriate.
     */
    private void TAY() {
        this.registerY = this.registerA;

        if(this.registerY == 0){
            this.zeroFlag = true;
        } else if(this.registerY > 127) {
            this.negativeFlag = true;
        }
    }

    /**
     * TSX - Transfer Stack Pointer to X
     * X = S
     * Copies the current contents of the stack register into the X register and sets the zero and negative flags as
     * appropriate.
     */
    private void TSX() {
        this.registerX = this.registerS;
        this.zeroFlag = this.registerX == 0 ? true : false;
        this.negativeFlag = this.registerX > 127 ? true : false;
    }

    /**
     * TXA - Transfer X to Accumulator
     *
     * Copies the current contents of the X register into the accumulator and sets the zero and negative flags as
     * appropriate.
     */
    private void TXA() {
        this.registerA = this.registerX;
        if(this.registerA == 0) {
            this.zeroFlag = true;
        } else {
            this.zeroFlag = false;
        }

        if(this.registerA > 127) {
            this.negativeFlag = true;
        } else {
            this.negativeFlag = false;
        }
    }

    /**
     * TXS - Transfer X to Stack Pointer
     * S = X
     * Copies the current contents of the X register into the stack register.
     */
    private void TXS() {
        this.registerS = this.registerX;
    }

    /**
     * TYA - Transfer Y to Accumulator
     *
     * Copies the current contents of the Y register into the accumulator and sets the zero and negative flags as
     * appropriate.
     */
    private void TYA() {
        this.registerA = this.registerY;
        if(this.registerA == 0) {
            this.zeroFlag = true;
        } else if(this.registerA > 127) {
            this.negativeFlag = true;
        }
    }


    /**
     * The memory map defines addresses
     *
     * ($0100 - $01FF)     256                 Stack memory
     *
     * That means 0xFF (256) slots.
     * @param value
     */
    private void stackPush(int value) {


        this.memory.write(0x100 + this.registerS, value);
        //this.dumpStack();
        this.registerS--;



    }

    private int stackPop() {
        this.registerS++;
        int value = this.memory.read(0x100 + this.registerS);

        //this.dumpStack();

        return value;
    }

    private void dumpStack() {
        System.out.println("--------------------------------------------------------------");
        for(int i = 0x100; i<=0x1FF; i++) {
            if(i - 0x100 == this.registerS) {
                System.out.println("\t\t==>["+Integer.toHexString(i)+"]["+i+"] = [" + Integer.toHexString(this.memory.read(i))+"][" + this.memory.read(i) +"]");
            } else {
                System.out.println("\t["+Integer.toHexString(i)+"]["+i+"] = [" + Integer.toHexString(this.memory.read(i))+"][" + this.memory.read(i) +"]");
            }
        }
        System.out.println("--------------------------------------------------------------");

    }







    public int getRegisterX() {
        return registerX;
    }

    public int getRegisterY() {
        return registerY;
    }

    public int getRegisterA() {
        return registerA;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public boolean isCarryFlag() {
        return carryFlag;
    }

    public boolean isZeroFlag() {
        return zeroFlag;
    }

    public boolean isInterruptDisable() {
        return interruptDisable;
    }

    public boolean isDecimalMode() {
        return decimalMode;
    }

    public boolean isBreakCommand() {
        return breakCommand;
    }

    public boolean isOverflowFlag() {
        return overflowFlag;
    }

    public boolean isNegativeFlag() {
        return negativeFlag;
    }
}
