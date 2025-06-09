package org.moriano.locones.memory.ppuregister;

public abstract class PPURegister {
    int rawValue;

    public int getRawValue() {
        return rawValue;
    }

    public void write(int value) {
        this.rawValue = value;
    }
}
