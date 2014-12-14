package org.moriano.locones.util;

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

    public LogStatus(int address, String instruction, int registerA, int registerX, int registerY, int registerP, int registerSP, int cycles) {
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

    @Override
    public String toString() {
        return "LogStatus{" +
                "address=" + address +
                ", instruction='" + instruction + '\'' +
                ", registerA=" + registerA +
                ", registerX=" + registerX +
                ", registerY=" + registerY +
                ", registerP=" + registerP +
                ", registerSP=" + registerSP +
                ", cycles=" + cycles +
                '}';
    }
}
