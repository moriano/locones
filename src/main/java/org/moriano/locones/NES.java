package org.moriano.locones;

import org.moriano.locones.cartridge.Cartridge;
import org.moriano.locones.memory.Memory;
import org.moriano.locones.screen.Screen;
import org.moriano.locones.util.LogReader;
import org.moriano.locones.util.LogStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: moriano
 * Date: 2/11/13
 * Time: 4:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class NES {

    private static final Logger log = LoggerFactory.getLogger(NES.class);
    private CPU cpu;
    private Memory memory;
    private PPU ppu;
    private final LogReader logReader = new LogReader();
    private int totalMemoryErrors = 0;
    private final Screen screen = new Screen();

    public NES(Cartridge cartridge, int initialPC) {
        this.cpu = new CPU(initialPC);
        log.info("Emulating with cart ==> " + cartridge);
        long paletteStart = System.currentTimeMillis();
        this.screen.showSystemPalette();
        long paletteEnd = System.currentTimeMillis();
        log.info("Palette generated in " + (paletteEnd-paletteStart) + "ms");
        this.memory = new Memory(cartridge);
        this.ppu = new PPU(memory, 241); // Initialize SL to 241 as per nestest.log
        this.cpu.setMemory(memory);
        this.cpu.setPpu(ppu);
        long start = System.currentTimeMillis();
        int totalFrames = 2000;
        for(int i = 0; i<=totalFrames; i++) {
            this.screen.generateRandomFrame();
        }
        long end = System.currentTimeMillis();

        double seconds = (end-start)/1000f;
        log.info("We have run " + totalFrames + "frames in " + seconds + " seconds, which is " + totalFrames/seconds + "Frames per second");

        int a = 1;
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
                //this.printTrace(status, cpuIterations);
                this.checkIterationSanity(status, expected, cpuIterations);

            }
        }
    }

    private void printTrace(LogStatus current, int iteration) {
        System.out.println(current.toNesTestFormat(iteration, this.cpu.getLastCode(),
                this.cpu.getInstruction(),
                this.cpu.getFirstInstructionArg(), this.cpu.getSecondInstructionArg()));
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

        current.setMemoryOperations(new ArrayList<>(this.memory.getOperationsHistory()));


        if (current.getMemoryOperations().size() != expected.getMemoryOperations().size()) {
            totalMemoryErrors++;
            System.out.println("Number of memory operations do not match, expected is " + expected.getMemoryOperations().size() + " actual is " + current.getMemoryOperations().size());


            int size = Math.max(current.getMemoryOperations().size(), expected.getMemoryOperations().size());
            System.out.println("\tExpected --vs-- Current");
            for (int i = 0; i<= size; i++) {
                String currentOp = "      ";
                String expectedOp = "     ";
                if (current.getMemoryOperations().size() > i) {
                    currentOp = current.getMemoryOperations().get(i);
                }

                if (expected.getMemoryOperations().size() > i) {
                    expectedOp = expected.getMemoryOperations().get(i);
                }
                System.out.println("\t"+expectedOp + " -- vs -- " + currentOp);
            }
            System.out.println("==> Total memory errors so far => " + totalMemoryErrors );
            System.out.println("\n");
        }

        this.memory.clearOpHistory();

        if (failed) {
            throw new RuntimeException("Ouch!");
        }

    }




    public void startEmulation() {
        this.run();
    }
}
