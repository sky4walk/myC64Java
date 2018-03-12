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
    public static void writeBinJavaString(byte[] binData, String filePath, int add){
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
                outStr += byte2hex(binData[i],add);
                out.write(outStr);
            }
            out.flush();
        } catch (IOException ex) {
        } 
    }    
    public static String byte2hex(int val, int add){
        String outStr = "";
        outStr += "0x";
        outStr += String.format("%02x", addByte(val,add));
        return outStr;
    }
    public static String word2hex(int val){
        String outStr = "";
        outStr += "0x";
        outStr += String.format("%04x", val);
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
        return  ( addr >> 8 ) & 0xFF;
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
    public static boolean pageJumpAdd(int adrBase,int adrAdd) {
        int newAdr = (adrBase & 0xFFFF) + (adrAdd & 0xFFFF);
        if ( getHighByte(adrBase & 0xFFFF) !=
             getHighByte(newAdr & 0xFFFF) ) {
            return true;
        }
        return false;
    }
    /**
     * add two Byte parameters with wrap around.
     * @param val1 one byte.
     * @param val2 second byte.
     * @return added val1 + val2 with wrap around.
     */
    public static int addByte( int val1, int val2 ) {
        return ( (val1 & 0xFF) + (val2 & 0xFF) ) & 0xFF;
    }
    /** subtract two Byte parameters with wrap around.
     * @param val1 one byte.
     * @param val2 second byte.
     * @return added val1 - val2 with wrap around.
     */ 
    public static int subByte( int val1, int val2 ) {
        return addByte( val1, ( 0xFF - (val2 & 0xFF) + 1) );
    }
    /**
     * XOR operator
     * @param val1
     * @param val2
     * @return val1 xor val2
     */
    public static int xor( int val1, int val2 ) {
        return val1 ^ val2;
    }
    public static int getSignedByte(int val) {
        val &= 0xFF;
        if ( testBit(val,7) ) {
            return ( val - 0xFF ) - 1;
        } else {
            return val;
        }
    }
    public static int getUnsignedByte(int val) {
        val &= 0xFF;
        if ( testBit(val,7) ) {
            return val;
        } else {
            return val;
        }
    }
}
