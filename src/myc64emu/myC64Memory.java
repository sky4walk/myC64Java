/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */

package myc64emu;

import static myc64emu.myC64Tools.byte2hex;

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
        loadRom(myC64Config.addrBasicRomStart,    myC64RomBasic.mem);
        loadRom(myC64Config.addrCharacterRomStart,myC64RomCharacters.mem);
        loadRom(myC64Config.addrKernalRomStart,   myC64RomKernal.mem);
        reset();
    }
    public void reset() {
        pla.reset();
        pla.setCHAREN();
        pla.setHIRAM();
        pla.setLORAM();
        writeRamByteDirect(
                myC64Config.addrProzessorPortReg,
                pla.getProzessorport());
    }
    public int getMemRamSize() {
        return memRam.length;
    }
    public int getMemRomSize() {
        return memRom.length;
    }
    /**
     * reads one byte from ram memory
     * @param addr ram memory adress
     * @return byte value
     */
    public int readRamByteDirect(int addr) {
        if ( myC64Tools.isInsideAdr(0,getMemRamSize()-1,addr) ){
            return memRam[addr] & 0xFF;
        } else {
            return 0;
        }
    }
    /**
     * reads one byte from rom memory
     * @param addr rom memory adress
     * @return byte value
     */
    public int readRomByteDirect(int addr) {
        if ( myC64Tools.isInsideAdr(0,getMemRomSize()-1,addr) ){
            return memRom[addr] & 0xFF;
        } else {
            return 0;
        }
    }
    public void writeRamByteDirect(int addr, int val) {
        if ( myC64Tools.isInsideAdr(0,getMemRamSize()-1,addr) ){
            memRam[addr] = val & 0xFF;
        } 
    }
    public void writeRomByteDirect(int addr, int val) {
        if ( myC64Tools.isInsideAdr(0,getMemRomSize()-1,addr) ){
            memRom[addr] = val & 0xFF;
        } 
    }
    public int readRamWordDirect(int addr) {
        int lowByte  = readRamByteDirect(addr);
        int highByte = readRamByteDirect(addr+1);
        return myC64Tools.getWord(lowByte,highByte);
    }
    public int readSystemWord(int addr) {
        int lowByte  = readSystemByte(addr);
        int highByte = readSystemByte(addr+1);
        return myC64Tools.getWord(lowByte,highByte);
    }
    public void writeRamWordDirect(int addr,int val) {
        int lowByte  = myC64Tools.getLowByte(val);        
        int highByte = myC64Tools.getHighByte(val);
        writeRamByteDirect(addr,  lowByte);
        writeRamByteDirect(addr+1,highByte);        
    }
    public void writeSystemWord(int addr,int val) {
        int lowByte  = myC64Tools.getLowByte(val);        
        int highByte = myC64Tools.getHighByte(val);
        writeSystemByte(addr,  lowByte);
        writeSystemByte(addr+1,highByte);        
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
    public void printOut() {
        for (int i = 0; i < getMemRamSize(); i++ ) {
            String outStr = "";
            int val = readSystemByte(i);
            outStr += "0x";
            outStr += myC64Tools.byte2hex( (byte)(val & 0xFF) );
            outStr += " ";
            if ( i > 0 && (i % 10)==0 )
                outStr += "\n";
            myC64Tools.printOut( outStr );
        }
    }
}
