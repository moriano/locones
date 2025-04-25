package org.moriano.locones.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: moriano
 * Date: 29/03/14
 * Time: 1:54 PM
 *
 * Reads a file like
 *
 * C000  4C F5 C5  JMP $C5F5                       A:00 X:00 Y:00 P:24 SP:FD CPUC:0

 *
 * Critical explanation regarding the PPU and CYC fields.
 *
 * CPUC: Refers to the number of CPU cycles used BEFORE the instruction was executed
 *
 */
public class LogReader {

    private Map<Integer, LogStatus> log = new HashMap<>();

    public LogReader() {
        InputStream fis;
        BufferedReader br = null;
        try {

        String line;

        fis = new FileInputStream("/home/moriano/dev/code/locones/src/main/resources/nestestCPUCycles.log");
        br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
        int lineNo = 1;
        List<String> memoryOperations = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            if (line.startsWith(" ")) {
                memoryOperations.add(line);
            } else {
                String rawAddress = line.substring(0, 4);
                String rawRegisterA = line.substring(50, 52);
                String rawRegisterX = line.substring(55, 57);
                String rawRegisterY = line.substring(60, 62);
                String rawRegisterP = line.substring(65, 67);
                String rawRegisterSP = line.substring(71, 73);
                String instruction = line.substring(16, 19);

                int cycles = Integer.parseInt(line.substring(79));

                LogStatus logStatus = new LogStatus(Integer.valueOf(rawAddress, 16) , instruction, Integer.valueOf(rawRegisterA, 16),
                        Integer.valueOf(rawRegisterX, 16), Integer.valueOf(rawRegisterY, 16), Integer.valueOf(rawRegisterP, 16),
                        Integer.valueOf(rawRegisterSP, 16), cycles);
                log.put(lineNo, logStatus);
                if (lineNo > 1) {
                    log.get(lineNo - 1).setMemoryOperations(new ArrayList<>(memoryOperations));
                }
                lineNo++;
                memoryOperations.clear();
            }
        }

        } catch(Exception e) {
            throw new RuntimeException("Problems while reading test file!!!", e);
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            br = null;
            fis = null;
        }

    }

    public LogStatus getLogStatus(int line) {
        return this.log.get(line);
    }
}
