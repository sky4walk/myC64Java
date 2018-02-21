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
    
    public void myC64Memory() {
        memRam = new int[memSize];
        memRom = new int[memSize];
    }
    public int readRamDirect(int addr) {
        if ( 0 <= addr && addr < memSize ){
            return memRam[addr];
        } else {
            return 0;
        }
    }
    public int readRomDirect(int addr) {
        if ( 0 <= addr && addr < memSize ){
            return memRom[addr];
        } else {
            return 0;
        }
    }
    public void writeRAmDirect(int addr, int val) {
        if ( 0 <= addr && addr < memSize ){
            memRam[addr] = val & 0xFF;
        } 
    }
    public void writeRomDirect(int addr, int val) {
        if ( 0 <= addr && addr < memSize ){
            memRom[addr] = val & 0xFF;
        } 
    }
}
