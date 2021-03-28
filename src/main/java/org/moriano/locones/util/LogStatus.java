package org.moriano.locones.util;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * User: moriano
 * Date: 29/03/14
 * Time: 1:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class LogStatus {

    private int address;
    private String instruction;
    private int registerA;
    private int registerX;
    private int registerY;
    private int registerP;
    private int registerSP;
    private int cycles;

    public LogStatus(int address, String instruction,
                     int registerA, int registerX,
                     int registerY, int registerP,
                     int registerSP, int cycles) {
        this.address = address;
        this.instruction = instruction;
        this.registerA = registerA;
        this.registerX = registerX;
        this.registerY = registerY;
        this.registerP = registerP;
        this.registerSP = registerSP;
        this.cycles = cycles;
    }

    public int getCycles() {
        return cycles;
    }

    public int getAddress() {
        return address;
    }

    public String getInstruction() {
        return instruction;
    }

    public int getRegisterA() {
        return registerA;
    }

    public int getRegisterX() {
        return registerX;
    }

    public int getRegisterY() {
        return registerY;
    }

    public int getRegisterP() {
        return registerP;
    }

    public int getRegisterSP() {
        return registerSP;
    }



    public String toNesTestFormat(int iteration, int cpuCode, String cpuInstruction, String firstInstructionArg, String secondInstructionArg) {
        // Print
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

        String hexPC = this.registerToHexLeadingZero(this.getAddress());
        String hex = this.registerToHexLeadingZero(cpuCode);
        String oldAHex = this.registerToHexLeadingZero(this.getRegisterA());
        String hexRegisterP = this.registerToHexLeadingZero(this.getRegisterP());
        String oldXHex = this.registerToHexLeadingZero(this.getRegisterX());
        String oldYHex = this.registerToHexLeadingZero(this.getRegisterY());
        String oldSPHex = this.registerToHexLeadingZero(this.getRegisterSP());


        String firstAndSecondInstructions = firstInstructionArg + "-" + secondInstructionArg;
        firstAndSecondInstructions = firstAndSecondInstructions.replace("-", " ");
        while(firstAndSecondInstructions.length() <= 4) {
            firstAndSecondInstructions += " ";
        }
        String instruction = cpuInstruction;


        String instructionArgument = firstInstructionArg + secondInstructionArg;
        instructionArgument = instructionArgument.replace(" ", "");

        String addresingModeSymbol = "";
        if(instructionArgument.length() > 0) {
            instructionArgument = addresingModeSymbol + "$" + instructionArgument;
        }

        while(instructionArgument.length() <= 4) {
            instructionArgument += " ";
        }

        int ppuX = Math.floorDiv((this.getCycles() * 3),  341);
        int ppuY = (this.getCycles() * 3) - (ppuX * 341);

        return String.format("%s %s  %s %s  %s %s\t\t\tA:%s X:%s Y:%s P:%s SP:%s PPU: %d, %d CPUC:%d",
                iterationStr, hexPC, hex, firstAndSecondInstructions, instruction, instructionArgument, oldAHex, oldXHex, oldYHex, hexRegisterP, oldSPHex, ppuX, ppuY, this.getCycles());

    }

    public void printDiff(LogStatus other) {
        System.out.println("\n");
        if (this.getAddress() != other.getAddress()) {
            System.out.printf("Address does not match, expected vs current %s -- %s\n", Integer.toHexString(this.getAddress()).toUpperCase(), Integer.toHexString(other.getAddress()).toUpperCase());
        }
        if (!this.getInstruction().equals(other.getInstruction())) {
            System.out.printf("Instruction does not match, expected vs current %s -- %s\n", this.getInstruction(), other.getInstruction() );
        }
        if (this.getRegisterA() != other.getRegisterA()) {
            System.out.printf("Register A does not match, expected vs current %d -- %d\n", this.getRegisterA(), other.getRegisterA() );
        }
        if (this.getRegisterSP() != other.getRegisterSP()) {
            System.out.printf("SP does not match, expected vs current %d -- %d\n", this.getRegisterSP(), other.getRegisterSP() );
        }
        if (this.getRegisterX() != other.getRegisterX()) {
            System.out.printf("Register X does not match, expected vs current %d -- %d\n", this.getRegisterX(), other.getRegisterX() );
        }
        if (this.getRegisterY() != other.getRegisterY()) {
            System.out.printf("Register Y does not match, expected vs current %d -- %d\n", this.getRegisterY(), other.getRegisterY() );
        }
        if(this.getRegisterP() != other.getRegisterP()) {
            System.out.printf("Register P does not match, expected vs current %d -- %d\n", this.getRegisterP(), other.getRegisterP() );

        }
        if(this.getCycles() != other.getCycles()) {
            System.out.printf("Cycles does not match, expected vs current %d -- %d\n", this.getCycles(), other.getCycles() );
        }

    }

    private String registerToHexLeadingZero(int regValue) {
        if (regValue <= 0xF) {
            return "0"+Integer.toHexString(regValue).toUpperCase();
        } else {
            return Integer.toHexString(regValue).toUpperCase();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogStatus logStatus = (LogStatus) o;
        return address == logStatus.address &&
                registerA == logStatus.registerA &&
                registerX == logStatus.registerX &&
                registerY == logStatus.registerY &&
                registerP == logStatus.registerP &&
                registerSP == logStatus.registerSP &&
                cycles == logStatus.cycles &&
                Objects.equals(instruction, logStatus.instruction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, instruction, registerA, registerX, registerY, registerP, registerSP, cycles);
    }
}
