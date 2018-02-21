/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */

package myc64emu;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
            ex.printStackTrace();
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
                outStr += "0x";
                outStr += byte2hex(binData[i]);
                out.write(outStr);
            }
            out.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        } 
    }        
    public static String byte2hex(byte val){
        return String.format("%02x", val);
    }
}
