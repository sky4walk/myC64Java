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
    private static final int memSize = 0x10000;
    private final int[] memRam;
    private final int[] memRom;

    private final myC64Pla pla = new myC64Pla();
    
    public myC64Memory() {
        memRam = new int[memSize];
        memRom = new int[memSize];
        loadRom(myC64Config.addrBasicRomStart,myC64RomBasic.mem);
        loadRom(myC64Config.addrCharacterRomStart,myC64RomCharacters.mem);
        loadRom(myC64Config.addrKernalRomStart,myC64RomKernal.mem);
        reset();
    }
    public void reset() {
        pla.reset();
        pla.setCHAREN();
        pla.clearHIRAM();
        pla.setLORAM();
        writeRamByteDirect(
                myC64Config.addrProzessorPortReg,
                pla.getProzessorport());
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
    public int readSystemByte(int addr){
        int retVal = 0;
        if ( myC64Tools.isInsideAdr(
                myC64Config.addrVicRegisterStart,
                myC64Config.addrVicRegisterEnde,
                addr) ) {
            if ( myC64Pla.AdressraumValues.IO == 
                    pla.getAdressraum(addr) ) {
                //retVal = VIC
            } else if ( myC64Pla.AdressraumValues.CHARSETROM == 
                    pla.getAdressraum(addr) ) {
                retVal = readRomByteDirect(addr);                                
            } else {
                retVal = readRamByteDirect(addr);                
            }
        } else if ( myC64Tools.isInsideAdr(
                myC64Config.addrCia1RegisterStart,
                myC64Config.addrCia1RegisterEnde,
                addr) ) {
            if ( myC64Pla.AdressraumValues.IO == 
                    pla.getAdressraum(addr) ) {
                //retVal = CIA1
            } else {
                retVal = readRamByteDirect(addr);                               
            }
        } else if ( myC64Tools.isInsideAdr(
                myC64Config.addrCia2RegisterStart,
                myC64Config.addrCia2RegisterEnde,
                addr) ) {
            if ( myC64Pla.AdressraumValues.IO == 
                    pla.getAdressraum(addr) ) {
                //retVal = CIA2
            } else {
                retVal = readRamByteDirect(addr);                               
            }
        } else if ( myC64Tools.isInsideAdr(
                myC64Config.addrBasicRomStart,
                myC64Config.addrBasicRomEnde,
                addr) ) {
            if ( myC64Pla.AdressraumValues.BASICROM == 
                    pla.getAdressraum(addr) ) {
                retVal = readRomByteDirect(addr);
            } else {
                retVal = readRamByteDirect(addr);                               
            }
        } else if ( myC64Tools.isInsideAdr(
                myC64Config.addrKernalRomStart,
                myC64Config.addrKernalRomEnde,
                addr) ) {
            if ( myC64Pla.AdressraumValues.KERNALROM == 
                    pla.getAdressraum(addr) ) {
                retVal = readRomByteDirect(addr);
            } else {
                retVal = readRamByteDirect(addr);                               
            }
        } else {
            retVal = readRamByteDirect(addr);
        }
        return retVal;
    }
    public void writeSystemByte(int addr, int val){
        if ( myC64Tools.isInsideAdr(
                myC64Config.addrZeroPageStart,
                myC64Config.addrZeroPageEnde,
                addr) ) {
            if ( myC64Config.addrProzessorPortReg == addr ) {
                pla.setProzessorport(val);
            }
            writeRamByteDirect(addr,val);
        } else if ( myC64Tools.isInsideAdr(
                myC64Config.addrVicRegisterStart,
                myC64Config.addrVicRegisterEnde,
                addr) ) {
            if ( myC64Pla.AdressraumValues.IO == 
                    pla.getAdressraum(addr) ) {
                //write VIC
            } else {
                writeRamByteDirect(addr,val);
            }
        } else if ( myC64Tools.isInsideAdr(
                myC64Config.addrCia1RegisterStart,
                myC64Config.addrCia1RegisterEnde,
                addr) ) {
            if ( myC64Pla.AdressraumValues.IO == 
                    pla.getAdressraum(addr) ) {
                //write CIA1
            } else {
                writeRamByteDirect(addr,val);                               
            }
        } else if ( myC64Tools.isInsideAdr(
                myC64Config.addrCia2RegisterStart,
                myC64Config.addrCia2RegisterEnde,
                addr) ) {
            if ( myC64Pla.AdressraumValues.IO == 
                    pla.getAdressraum(addr) ) {
                //write CIA2
            } else {
                writeRamByteDirect(addr,val);                               
            }        
        } else {
            writeRamByteDirect(addr,val);
        }
    }
    public void loadRom(int addrStart, int[] Rom) {
        for (int i = 0; i < Rom.length; i++ ) {
            writeRomByteDirect(addrStart,Rom[i]);
            addrStart++;
        }        
    }
    public void loadRam(int addrStart, int[] Ram) {
        for (int i = 0; i < Ram.length; i++ ) {
            writeRamByteDirect(addrStart,Ram[i]);
            addrStart++;
        }        
    }
}
