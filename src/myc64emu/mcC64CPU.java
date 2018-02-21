/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */

package myc64emu;

/**
 *
 * @author Andre Betz mail@AndreBetz.de
 */
public class mcC64CPU {
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
    
    public void myC64Memory(myC64Memory mem) {
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
        if ( (regSR & 0x80) == 1 ) return true;
        else return false;
    }
    /**
     * negative flag
     * @param val negative flag
     */
    private void setFlagN(boolean val){
        if ( val ) regSR |= 0x80;
        else regSR &= 0x7F;
    }
    /**
     * Overflow flag
     * @return overflow flag
     */
    private boolean getFlagV(){
        if ( (regSR & 0x40) == 1 ) return true;
        else return false;
    }
    /**
     * Overflow flag
     * @param val overflow flag
     */
    private void setFlagV(boolean val){
        if ( val ) regSR |= 0x40;
        else regSR &= 0xBF;
    }
   /**
     * Break flag
     * @return break flag
     */
    private boolean getFlagB(){
        if ( (regSR & 0x10) == 1 ) return true;
        else return false;
    }
    /**
     * Break flag
     * @param val break flag
     */
    private void setFlagB(boolean val){
        if ( val ) regSR |= 0x10;
        else regSR &= 0xEF;
    }
    /**
     * Break flag
     * @return break flag
     */
    private boolean getFlagD(){
        if ( (regSR & 0x08) == 1 ) return true;
        else return false;
    }
    /**
     * Break flag
     * @param val break flag
     */
    private void setFlagD(boolean val){
        if ( val ) regSR |= 0x08;
        else regSR &= 0xF7;
    }
    /**
     * Interrupt flag
     * @return Interrupt flag
     */
    private boolean getFlagI(){
        if ( (regSR & 0x04) == 1 ) return true;
        else return false;
    }
    /**
     * Interrupt flag
     * @param val Interrupt flag
     */
    private void setFlagI(boolean val){
        if ( val ) regSR |= 0x04;
        else regSR &= 0xFB;
    }
    /**
     * Zero flag
     * @return Zero flag
     */
    private boolean getFlagZ(){
        if ( (regSR & 0x02) == 1 ) return true;
        else return false;
    }
    /**
     * Zero flag
     * @param val Zero flag
     */
    private void setFlagZ(boolean val){
        if ( val ) regSR |= 0x02;
        else regSR &= 0xFD;
    }
    /**
     * Carry flag
     * @return Carry flag
     */
    private boolean getFlagC(){
        if ( (regSR & 0x01) == 1 ) return true;
        else return false;
    }
    /**
     * Carry flag
     * @param val Carry flag
     */
    private void setFlagC(boolean val){
        if ( val ) regSR |= 0x01;
        else regSR &= 0xFE;
    }
}
