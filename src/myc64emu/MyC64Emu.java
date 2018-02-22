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

    private myC64Memory mem = new myC64Memory();
    private myC64CPU    cpu = new myC64CPU(mem);
    
    public static void convertBin2Data(){
        byte[] bin = myC64Tools.readBinData("basic.bin");
        myC64Tools.writeBinJavaString(bin, "basic.txt");
        
        bin = myC64Tools.readBinData("characters.bin");
        myC64Tools.writeBinJavaString(bin, "char.txt");
        
        bin = myC64Tools.readBinData("kernal.bin");
        myC64Tools.writeBinJavaString(bin, "kernal.txt");
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }
    
}
