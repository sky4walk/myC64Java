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
    private byte[] memRam;
    private byte[] memRom;    
    private static int memSize = 0x10000;
    
    public void myC64Memory() {
        memRam = new byte[memSize];
        memRom = new byte[memSize];
    }
    public byte readRamDirect(int addr) {
        if ( 0 <= addr && addr < memSize ){
            return memRam[addr];
        } else {
            return 0;
        }
    }
    public byte readRomDirect(int addr) {
        if ( 0 <= addr && addr < memSize ){
            return memRom[addr];
        } else {
            return 0;
        }
    }
    public void writeRAmDirect(int addr, byte val) {
        if ( 0 <= addr && addr < memSize ){
            memRam[addr] = val;
        } 
    }
    public void writeRomDirect(int addr, byte val) {
        if ( 0 <= addr && addr < memSize ){
            memRom[addr] = val;
        } 
    }
}
