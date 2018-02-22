/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */

package myc64emu;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Andre Betz mail@Andrebetz.de
 */
public class myC64Tools {
    public static byte[] readBinData(String filePath)  {
        byte[] allBytes = null;        
        try {
            InputStream inputStream = new FileInputStream(filePath);
            long fileSize = new File(filePath).length();
            allBytes = new byte[(int) fileSize];
             inputStream.read(allBytes);
        } catch (IOException ex) {
	}        
        return allBytes;
    }
    public static void writeBinJavaString(byte[] binData, String filePath){
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
            for ( int i = 0; i < binData.length; i++ ){
                String outStr = "";
                if ( 0 != i ) {
                    outStr += ", ";
                    if ( 0 == i % 10 ) {
                        outStr += "\n";
                    }
                }
                outStr += byte2hex(binData[i]);
                out.write(outStr);
            }
            out.flush();
        } catch (IOException ex) {
        } 
    }        
    public static String byte2hex(int val){
        String outStr = "";
        outStr += "0x";
        outStr += myC64Tools.byte2hex( (byte)(val & 0xFF) );
        outStr += String.format("%02x", val);
        return outStr;
    }
    public static boolean testBit(int reg, int pos) {
        return (reg & (1 << pos)) != 0;
    }
    public static int setBit(int reg, int pos, boolean val) {
        if ( val )  return ( reg |  (1 << pos) );
        else        return ( reg & ~(1 << pos) );
    }
    public static boolean isInsideAdr(int start, int end,int addr) {
        return  ( start <= addr && end >= addr );
    }
    public static int getLowByte(int addr) {
        return  addr & 0x00FF;
    }
    public static int getHighByte(int addr) {
        return  addr & 0xFF00;
    }
    /**
     * https://www.c64-wiki.de/wiki/Byte
     * 16Bit Adressen werden im Ram in der Reihe
     * Low Byte (adr) dann High Byte (adr+1) abgelegt
     * @param lowByte low Byte (adr)
     * @param highByte High Byte  (adr+1)
     * @return 16Bit Adresse
     */
    public static int getWord(int lowByte,int highByte) {
        return  highByte*256+lowByte;
    }
    public static void printOut(String text) {
        System.out.println(text);
    }
}
