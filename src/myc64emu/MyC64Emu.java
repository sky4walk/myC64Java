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
    
    /**
     * load a testprogram which test the cpu op codes.
     * https://github.com/Klaus2m5/6502_65C02_functional_tests
     * @return returns true if cpu works correct.
     */
    public boolean testCpu() {
        int regPC = 0;
        int loadAdr = 0x0400;
        /* use whole RAM memory, no IO and ROMs */
        mem.writeSystemByte(myC64Config.addrProzessorPortReg, 0);        
        /* load test program to memory */
        mem.loadRam(loadAdr, myC64PrgCpuTest.getMem1());
        mem.loadRam(loadAdr + myC64PrgCpuTest.getMem1().length, 
                myC64PrgCpuTest.getMem2());
        mem.loadRam(loadAdr + 
                myC64PrgCpuTest.getMem1().length +
                myC64PrgCpuTest.getMem2().length,
                myC64PrgCpuTest.getMem3());
        cpu.setRegPC(loadAdr);
        /* run test program */
        while ( true ) {
            if ( regPC == cpu.getRegPC() ) {
                myC64Tools.printOut("infinite loop at" + regPC + "\n");
                return false;
            } else if ( 0x3463 == cpu.getRegPC() ) {
                myC64Tools.printOut("test passed\n");
                break;
            }
            regPC = cpu.getRegPC();
            myC64Tools.printOut("Adr: "+myC64Tools.word2hex(regPC) + " OP: " +
                    myC64Tools.byte2hex(mem.readRamByteDirect(regPC),0));
            cpu.printOut();
            if ( !cpu.emulate() ) {
                return false;
            }
        }
        return true;
    }
    public static void convertBin2Data(){
        byte[] bin = myC64Tools.readBinData("basic.bin");
        myC64Tools.writeBinJavaString(bin, "basic.txt",0);
        
        bin = myC64Tools.readBinData("characters.bin");
        myC64Tools.writeBinJavaString(bin, "char.txt",0);
        
        bin = myC64Tools.readBinData("kernal.bin");
        myC64Tools.writeBinJavaString(bin, "kernal.txt",0);
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        MyC64Emu emu = new MyC64Emu();
        emu.testCpu();        
    }
    
}
