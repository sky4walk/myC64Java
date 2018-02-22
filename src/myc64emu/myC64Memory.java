/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */

package myc64emu;

/**
 *
 * @author Andre Betz mail@AndreBetz.de
 */
public class myC64Memory {
    private int[] memRam;
    private int[] memRom;
    private static int memSize = 0x10000;

    private myC64Pla pla = new myC64Pla();
    
    public myC64Memory() {
        memRam = new int[memSize];
        memRom = new int[memSize];
    }
    public void reset() {
        pla.reset();
    }
    public int readRamByteDirect(int addr) {
        if ( myC64Tools.isInsideAdr(0,memSize-1,addr) ){
            return memRam[addr] & 0xFF;
        } else {
            return 0;
        }
    }
    public int readRomByteDirect(int addr) {
        if ( myC64Tools.isInsideAdr(0,memSize-1,addr) ){
            return memRom[addr] & 0xFF;
        } else {
            return 0;
        }
    }
    public void writeRamByteDirect(int addr, int val) {
        if ( myC64Tools.isInsideAdr(0,memSize-1,addr) ){
            memRam[addr] = val & 0xFF;
        } 
    }
    public void writeRomByteDirect(int addr, int val) {
        if ( myC64Tools.isInsideAdr(0,memSize-1,addr) ){
            memRom[addr] = val & 0xFF;
        } 
    }
}
