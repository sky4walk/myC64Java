/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */

package myc64emu;

/**
 *
 * @author Andre Betz mail@AndreBetz.de
 */
public class MyC64Emu {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        byte[] bin = myC64Tools.readBinData("D:\\betzan8u\\Download\\emudore-master\\assets\\roms\\basic.901226-01.bin");
        myC64Tools.writeBinJavaString(bin, "D:\\betzan8u\\Download\\basic.java");
        
        bin = myC64Tools.readBinData("D:\\betzan8u\\Download\\emudore-master\\assets\\roms\\characters.901225-01.bin");
        myC64Tools.writeBinJavaString(bin, "D:\\betzan8u\\Download\\char.java");
        
        bin = myC64Tools.readBinData("D:\\betzan8u\\Download\\emudore-master\\assets\\roms\\kernal.901227-03.bin");
        myC64Tools.writeBinJavaString(bin, "D:\\betzan8u\\Download\\kernal.java");
    }
    
}
