package org.moriano.locones.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: moriano
 * Date: 29/03/14
 * Time: 1:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class LogReader {

    private Map<Integer, LogStatus> log = new HashMap<>();

    public LogReader() {
        InputStream fis;
        BufferedReader br = null;
        try {

        String line;

        fis = new FileInputStream("/home/moriano/dev/code/locones/src/main/resources/nestest.log");
        br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
        int lineNo = 1;
        while ((line = br.readLine()) != null) {
            String rawAddress = line.substring(0, 4);
            String rawRegisterA = line.substring(50, 52);
            String rawRegisterX = line.substring(55, 57);
            String rawRegisterY = line.substring(60, 62);
            String rawRegisterP = line.substring(65, 67);
            String rawRegisterSP = line.substring(71, 73);
            String instruction = line.substring(16, 19);
            int cycles = Integer.parseInt(line.substring(78, 81).replace(" ", ""));
            int scanLines = Integer.parseInt(line.substring(85));

            LogStatus logStatus = new LogStatus(Integer.valueOf(rawAddress, 16) , instruction, Integer.valueOf(rawRegisterA, 16),
                    Integer.valueOf(rawRegisterX, 16), Integer.valueOf(rawRegisterY, 16), Integer.valueOf(rawRegisterP, 16),
                    Integer.valueOf(rawRegisterSP, 16), cycles, scanLines);
            log.put(lineNo, logStatus);

            lineNo++;
        }

        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch(Exception e) {
                    e.printStackTrace();;
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
