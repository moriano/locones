package org.moriano.locones.cartridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created with IntelliJ IDEA.
 * User: moriano
 * Date: 8/10/13
 * Time: 6:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class Cartridge {

    private static final Logger log = LoggerFactory.getLogger(Cartridge.class);

    private CartrigdeHeader cartrigdeHeader;
    private int[] prgROM;
    private int[] chrROM;

    private Cartridge(int[] rawBytes) {
        this.cartrigdeHeader = new CartrigdeHeader(rawBytes);
        log.info("Cartridge header " + this.cartrigdeHeader);

        int startPosition = 16; //Header is 16 bytes
        if(this.cartrigdeHeader.containsTrainer()) {
           startPosition += 512;
        }

        prgROM = new int[this.cartrigdeHeader.getPrgRomSize()];
        chrROM = new int[this.cartrigdeHeader.getChrRomSize()];

        int j = 0;
        int limit = startPosition+this.cartrigdeHeader.getPrgRomSize();
        for(int i = startPosition; i<limit; i++) {

            prgROM[j] = rawBytes[i];
            if (rawBytes[i] == 0x4C || rawBytes[i] == 0x6C) {
                log.info("GOOD AT POSITION " + j + " hex " + Integer.toHexString(j));
            }
            j++;
        }

        startPosition += this.cartrigdeHeader.getChrRomSize();
        j = 0;
        for(int i = startPosition; i<startPosition+this.cartrigdeHeader.getChrRomSize(); i++) {
            chrROM[j] = rawBytes[i];
            j++;
        }



    }

    public int readPRG(int address) {
        return this.prgROM[address];
    }

    public static Cartridge loadFromFile(String fullPath)  {
        Path path = Paths.get(fullPath);
        byte[] data = null;
        int[] cleanData = null;
        try {
            data = Files.readAllBytes(path);
            cleanData = new int[data.length];

            for(int i = 0; i<data.length; i++) {
                cleanData[i] = data[i]  & 0xff; //Convert int into bytes
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        return new Cartridge(cleanData);
    }

    public CartrigdeHeader getCartrigdeHeader() {
        return cartrigdeHeader;
    }

    @Override
    public String toString() {
        return "Cartridge{" +
                "cartrigdeHeader=" + cartrigdeHeader +
                '}';
    }
}
