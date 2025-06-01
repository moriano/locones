package org.moriano.locones;

import org.moriano.locones.cartridge.Cartridge;
import org.moriano.locones.memory.PatternTables;
import org.moriano.locones.screen.PatternTableUI;


/**
 * Created by moriano on 15/11/14.
 */

public class Run {

    public static void main(String[] args) throws  Exception {

        Cartridge cartridge = Cartridge.loadFromFile("/home/moriano/dev/code/locones/src/main/resources/nestest.nes");
        PatternTables patternTables = PatternTables.fromCHRRom(cartridge.getChrROM());
        PatternTableUI patternTableUI = new PatternTableUI(patternTables);

        //Thread.sleep(1000);
        //NES myNes = new NES(cartridge, 0xC000, true); // 0xC004 is where i suspect the nestest program starts, this matches fceux debugger and log
        NES myNes = new NES(cartridge, 0xC004, false);
        myNes.startEmulation();
    }
}
