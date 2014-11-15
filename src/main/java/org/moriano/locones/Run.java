package org.moriano.locones;

import org.moriano.locones.cartridge.Cartridge;

/**
 * Created by moriano on 15/11/14.
 */
public class Run {

    public static void main(String[] args) {
        Cartridge cartridge = Cartridge.loadFromFile("/home/moriano/dev/code/locones/src/main/resources/nestest.nes");
        NES myNes = new NES(cartridge);
        myNes.startEmulation();
    }
}
