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
    /**
     * count the cycles
     */
    private int cycleCntr;
    
    public myC64CPU(myC64Memory mem) {
        memory = mem;
        reset();
    }
    private void reset() {
        regA  = 0;
        regX  = 0;
        regY  = 0;
        regSR = 0;
        regSP = 0;
        regPC = memory.readSystemWord(myC64Config.addrResetVector);
        cycleCntr = 0;
    }
    /**
     * zaehlt die cycles.
     * @param val cycles to add
     */
    private void addCycleCnt(int val) {
        cycleCntr += val;
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
    /**
     * get the actual operation
     * @return operation 
     */
    public int getActOp() {
        int val = memory.readSystemByte(regPC);
        regPC++;
        return val;
    }
    /**
     * schreibt einen wert in den StackPointer
     * @param val wert auf dem stapel ablegen.
     */
    private void push(int val) {
        memory.writeSystemByte(
                myC64Config.addrBaseStack+regSP, val);
        regSP--;
    }
    /**
     * liest vom Stapel einen wert.
     * @return oberster stapel wert.
     */
    private int pop() {
        regSP++;
        return memory.readSystemByte(
                    myC64Config.addrBaseStack+regSP);
    }
    /**
     * https://www.c64-wiki.de/wiki/Opcode
     * interpretiert die Opcodes.
     */
    public void interpreteOP() {
        int op = getActOp();
        switch(op) {
            case 0x00: // BRK https://www.c64-wiki.de/wiki/BRK
                brk();
                break;
                
                
        }
    }
    private void brk() {
        getActOp();
        push(myC64Tools.getHighByte(regPC));
        setFlagB(true);
        push(myC64Tools.getLowByte(regPC));
        push(regSR);
        setFlagI(true);
        regPC = memory.readSystemWord(myC64Config.addrIRQVector);
        addCycleCnt(7);
    }
    public void printOut() {
        String outStr = "";
        outStr += "regA: ";
        outStr += myC64Tools.byte2hex(regA);
        outStr += "\nregX: ";
        outStr += myC64Tools.byte2hex(regX);
        outStr += "\nregY: ";
        outStr += myC64Tools.byte2hex(regY);
        outStr += "\nregSR: ";
        outStr += myC64Tools.byte2hex(regSR);
        outStr += "\nregSP: ";
        outStr += myC64Tools.byte2hex(regSP);
        outStr += "\nregPC: ";
        outStr += myC64Tools.byte2hex(regPC);
        outStr += "\n";
        myC64Tools.printOut( outStr );
    } 
}
