package org.moriano.locones;

import org.moriano.locones.memory.Memory;

/**
 * Created with IntelliJ IDEA.
 * User: moriano
 * Date: 7/28/13
 * Time: 9:07 AM
 * To change this template use File | Settings | File Templates.
 */
public enum AddressingMode {

    INDEXED_INDIRECT("Indexed indirect", 12);

    private String name;
    private int id;

    private AddressingMode(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public int getAddress(CPU currentCPU, int argument, Memory memory) {
        return this.getAddress(currentCPU, argument, memory, false);
    }

    public int getAddress(CPU currentCPU, int argument, Memory memory, boolean checkPageCross){
        //TODO this is confusing and seems to SUCK!!
        switch (this) {



            case INDEXED_INDIRECT:
                /*
                Indexed indirect ==> Seems to work

                Indexed indirect addressing is normally used in conjunction with a table of address held on zero page.
                The address of the table is taken from the instruction and the X register added to it (with zero page
                wrap around) to give the location of the least significant byte of the target address.
                 */
                int lower = memory.read((argument + currentCPU.getRegisterX() ) & 0xFF);
                int higher = memory.read((argument + currentCPU.getRegisterX() + 1) & 0xFF) << 8;
                int finalAddress = higher | lower;



                return finalAddress;
                //throw new UnsupportedOperationException("Indexed indirect addressing not supported!");



                //return argument + currentCPU.getRegisterY();
            default:
                return 0;
        }
    }
}
