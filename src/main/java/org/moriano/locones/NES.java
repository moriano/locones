package org.moriano.locones;

import org.moriano.locones.cartridge.Cartridge;
import org.moriano.locones.memory.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: moriano
 * Date: 2/11/13
 * Time: 4:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class NES {

    private static final Logger log = LoggerFactory.getLogger(NES.class);
    private CPU cpu = new CPU();
    private Memory memory;

    public NES(Cartridge cartridge) {
        log.info("Emulating with cart ==> " + cartridge);
        this.memory = new Memory(cartridge);
        this.cpu.setMemory(memory);
    }

    public void startEmulation() {
        this.cpu.run();
    }
}
