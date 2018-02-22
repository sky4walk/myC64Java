/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */

package myc64emu;

/**
 *
 * @author Andre Betz mail@AndreBetz.de
 */
public class myC64CPU {
    /**
     * memory 64KB
     */
    private myC64Memory memory;
    /**
     * Akkumulator Register 8Bit
     */
    private int regA;
    private int regX;
    private int regY;
    /**
     * Statusregister Flags 8Bit
     */
    private int regSR;
    /**
     * Stack Pointer 8Bit
     */
    private int regSP;
    /**
     *  Program Counter 16Bit 
     */
    private int regPC; 
    
    public myC64CPU(myC64Memory mem) {
        memory = mem;
        reset();
    }
    
    private void reset() {
        regA = 0;
        regX = 0;
        regY = 0;
        regSR = 0;
        regSP = 0;
        regPC = 0;
    }
    /**
     * negative flag
     * @return negative flag
     */
    private boolean getFlagN(){
        return ( myC64Tools.testBit(regSR,7) );
    }
    /**
     * negative flag
     * @param val negative flag
     */
    private void setFlagN(boolean val){
        myC64Tools.setBit(regSR,7,val);
    }
    /**
     * Overflow flag
     * @return overflow flag
     */
    private boolean getFlagV(){
        return ( myC64Tools.testBit(regSR,6) );
    }
    /**
     * Overflow flag
     * @param val overflow flag
     */
    private void setFlagV(boolean val){
        myC64Tools.setBit(regSR,6,val);
    }
   /**
     * Break flag
     * @return break flag
     */
    private boolean getFlagB(){
        return ( myC64Tools.testBit(regSR,4) );
    }
    /**
     * Break flag
     * @param val break flag
     */
    private void setFlagB(boolean val){
        myC64Tools.setBit(regSR,4,val);
    }
    /**
     * Break flag
     * @return break flag
     */
    private boolean getFlagD(){
        return ( myC64Tools.testBit(regSR,3) );
    }
    /**
     * Break flag
     * @param val break flag
     */
    private void setFlagD(boolean val){
        myC64Tools.setBit(regSR,3,val);
    }
    /**
     * Interrupt flag
     * @return Interrupt flag
     */
    private boolean getFlagI(){
        return ( myC64Tools.testBit(regSR,2) );
    }
    /**
     * Interrupt flag
     * @param val Interrupt flag
     */
    private void setFlagI(boolean val){
        myC64Tools.setBit(regSR,2,val);
    }
    /**
     * Zero flag
     * @return Zero flag
     */
    private boolean getFlagZ(){
        return ( myC64Tools.testBit(regSR,1) );
    }
    /**
     * Zero flag
     * @param val Zero flag
     */
    private void setFlagZ(boolean val){
        myC64Tools.setBit(regSR,1,val);
    }
    /**
     * Carry flag
     * @return Carry flag
     */
    private boolean getFlagC(){
        return ( myC64Tools.testBit(regSR,0) );
    }
    /**
     * Carry flag
     * @param val Carry flag
     */
    private void setFlagC(boolean val){
        myC64Tools.setBit(regSR,0,val);
    }
}
