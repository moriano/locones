package org.moriano.locones;

import org.moriano.locones.cartridge.Cartridge;
import org.moriano.locones.memory.Memory;
import org.moriano.locones.util.LogReader;
import org.moriano.locones.util.LogStatus;
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
    private PPU ppu;
    private final LogReader logReader = new LogReader();

    public NES(Cartridge cartridge) {
        log.info("Emulating with cart ==> " + cartridge);
        this.memory = new Memory(cartridge);
        this.ppu = new PPU(memory, 241); // Initialize SL to 241 as per nestest.log
        this.cpu.setMemory(memory);
        this.cpu.setPpu(ppu);
    }


    public void run() {
        int iterations = 0;
        int cpuIterations = 0;
        int oldPPUCycles = 0;
        int ppuCycles = 0;
        int fragmentPPUCycles = 0;
        while(true) {

            iterations++;
            this.ppu.cycle(this.cpu.getCycles());

            if (iterations % 3 == 0) { // The CPU has a cycle every 3 cycles of the PPU

                ppuCycles = fragmentPPUCycles + (this.cpu.getCycles() * 3);
                if(ppuCycles >= 341) {
                    ppuCycles = ppuCycles - 341;
                    fragmentPPUCycles = ppuCycles;
                }

                oldPPUCycles = ppuCycles;
                cpuIterations++;
                LogStatus expected = logReader.getLogStatus(cpuIterations);
                if (expected == null) {
                    System.out.println("Tests passed!!!");
                    System.exit(0);
                }
                LogStatus status = this.cpu.cycle();
                this.checkIterationSanity(status, expected, cpuIterations);

            }
        }
    }


    private void checkIterationSanity(LogStatus current, LogStatus expected, int iteration) {
        boolean failed = false;
        if (!current.equals(expected)) {
            expected.printDiff(current);
            failed = true;
        }

        System.out.println(current.toNesTestFormat(iteration, this.cpu.getLastCode(),
                this.cpu.getInstruction(),
                this.cpu.getFirstInstructionArg(), this.cpu.getSecondInstructionArg()));
        if (failed) {
            throw new RuntimeException("Ouch!");
        }

    }




    public void startEmulation() {
        this.run();
    }
}
