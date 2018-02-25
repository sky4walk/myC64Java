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
    public int getCycles() {
        return cycleCntr;
    }
    public int getRegPC() {
        return regPC;
    }
    /**
     * set the Program counter
     * it is public for debugging
     * @param reg 
     */
    public void setRegPC(int reg) {
        regPC = reg;
    }
    public int getRegSR() {
        return regSR;
    }
    private void setRegSR(int reg) {
        regSR = reg;
    }
    public int getRegSP() {
        return regSP;
    }
    private void setRegSP(int reg) {
        regSP = reg;
    }
    public int getRegA() {
        return regA;
    }
    public void setRegA(int reg) {
        regA = reg;
    }
    public int getRegX() {
        return regX;
    }
    public void setRegX(int reg) {
        regX = reg;
    }
    public int getRegY() {
        return regY;
    }
    public void setRegY(int reg) {
        regY = reg;
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
        return ( myC64Tools.testBit(getRegSR(),7) );
    }
    /**
     * negative flag
     * @param val negative flag
     */
    private void setFlagN(int val){
        if ( myC64Tools.testBit(val,7) )
            setRegSR(myC64Tools.setBit(getRegSR(),7,true));
        else
            setRegSR(myC64Tools.setBit(getRegSR(),7,false));
    }
    /**
     * Overflow flag
     * @return overflow flag
     */
    private boolean getFlagV(){
        return ( myC64Tools.testBit(getRegSR(),6) );
    }
    /**
     * Overflow flag
     * @param val overflow flag
     */
    private void setFlagV(boolean val){
        setRegSR(myC64Tools.setBit(getRegSR(),6,val));
    }
   /**
     * Break flag
     * @return break flag
     */
    private boolean getFlagB(){
        return ( myC64Tools.testBit(getRegSR(),4) );
    }
    /**
     * Break flag
     * @param val break flag
     */
    private void setFlagB(boolean val){
        setRegSR(myC64Tools.setBit(getRegSR(),4,val));
    }
    /**
     * Break flag
     * @return break flag
     */
    private boolean getFlagD(){
        return ( myC64Tools.testBit(getRegSR(),3) );
    }
    /**
     * Break flag
     * @param val break flag
     */
    private void setFlagD(boolean val){
        setRegSR(myC64Tools.setBit(getRegSR(),3,val));
    }
    /**
     * Interrupt flag
     * @return Interrupt flag
     */
    private boolean getFlagI(){
        return ( myC64Tools.testBit(getRegSR(),2) );
    }
    /**
     * Interrupt flag
     * @param val Interrupt flag
     */
    private void setFlagI(boolean val){
        setRegSR(myC64Tools.setBit(getRegSR(),2,val));
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
     * @param val byte to test for zero
     */
    private void setFlagZ(int val){
        if ( 0 == val )
            setRegSR(myC64Tools.setBit(regSR,1,true));
        else
            setRegSR(myC64Tools.setBit(regSR,1,false));
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
        setRegSR(myC64Tools.setBit(regSR,0,val));
    }
    private void incPC() {
        setRegPC(1+getRegPC());
    }
    private void incSP() {
        setRegSP(1+getRegSP());
    }
    private void decSP() {
        setRegSP(getRegSP()-1);
    }
    /**
     * get the actual operation 8 Bit value
     * @return operation 8Bit value from PC
     */
    public int getActOp() {
        int val = memory.readSystemByte(getRegPC());
        incPC();
        return val;
    }
    /**
     * get the actual operation
     * @return return 16Bit Adresse
     */
    public int getActOpWord() {
        int val = memory.readSystemWord(getRegPC());
        incPC();
        incPC();
        return val;
    }
    /**
     * schreibt einen wert in den StackPointer
     * @param val wert auf dem stapel ablegen.
     */
    private void push(int val) {
        memory.writeSystemByte(
                myC64Config.addrBaseStack+getRegSP(), val);
        decSP();
    }
    /**
     * liest vom Stapel einen wert.
     * @return oberster stapel wert.
     */
    private int pop() {
        incSP();
        return memory.readSystemByte(
                    myC64Config.addrBaseStack+getRegSP());
    }
    /**
     * https://www.c64-wiki.de/wiki/Adressierung#Zeropage_Adressierung
     * Zero Page Adressierung
     * Bereich 0x00 - 0xFF
     */
    private int zeroPage() {
        return getActOp();
    }
    /**
     * https://www.c64-wiki.de/wiki/Adressierung#Direkte_Adressierung
     */
    private int directAdr() {
        return getActOp();
    }
    /**
     * https://www.c64-wiki.de/wiki/Adressierung#Absolute_Adressierung
     */
    private int absoluteAdr() {
        return getActOpWord();
    }
    /**
     * https://www.c64-wiki.de/wiki/Adressierung#Indirekte_X-indizierte_Zeropage-Adressierung
     * Indirekte X-indizierte Zeropage Adressierung
     */
    private int indirektIndiziertZero_X() {
        return indirektIndiziertZero(getRegX());        
    }
    private int indirektIndiziertZero(int reg) {
        // wrap around Page
        int addRZero = ( zeroPage() + reg ) & 0xFF;
        return memory.readSystemWord(addRZero);
    }
    /**
     * https://www.c64-wiki.de/wiki/Opcode
     * interpretiert die Opcodes.
     * http://www.oxyron.de/html/opcodes02.html
     * for cycle counter
     */
    public void interpreteOP() {
        int op = getActOp();
        switch(op) {
            case 0x00: brk(); break; // BRK                 
            case 0x01: // ORA
                ora(memory.readSystemByte(indirektIndiziertZero_X()),6); break; 
            case 0x05: // ORA
                ora(memory.readSystemByte(zeroPage()),3); break; 
            case 0x06: // ASL
                asl_err(zeroPage(),5); break; 
            case 0x08: // PHP https://www.c64-wiki.de/wiki/PHP
                push(getRegSR());addCycleCnt(3);break;
            case 0x09: // ORA https://www.c64-wiki.de/wiki/ORA_(RAUTE)$nn 
                ora(directAdr(),2); break;
            case 0x0A: // ASL https://www.c64-wiki.de/wiki/ASL
                setRegA(asl(getRegA()));addCycleCnt(2);break;
            case 0x0D: // ORA https://www.c64-wiki.de/wiki/ORA_$hhll
                ora(memory.readSystemByte(absoluteAdr()),4);break;
            case 0x0E:
                asl_err(absoluteAdr(),6); break;
            default:                                
        }
    }
    /**
     * https://www.c64-wiki.de/wiki/BRK
     */
    private void brk() {
        getActOp();
        push(myC64Tools.getHighByte(getRegPC()));
        setFlagB(true);
        push(myC64Tools.getLowByte(getRegPC()));
        push(getRegSR());
        setFlagI(true);
        setRegPC(memory.readSystemWord(myC64Config.addrIRQVector));
        addCycleCnt(7);
    }
    /**
     * https://www.c64-wiki.de/wiki/ORA_($ll,_X)
     */
    private void ora(int val,int cycles) {
        setRegA( getRegA() | val );
        setFlagZ(getRegA());
        setFlagN(getRegA());
        addCycleCnt(cycles);
    }
    /**
     * https://www.c64-wiki.de/wiki/ASL_$ll
     */
    private void asl_err(int adr,int cycles) {
        int val = memory.readSystemByte(adr);
        memory.writeSystemByte(adr, val);
        val = asl(val);
        memory.writeSystemByte(adr, val);
        addCycleCnt(cycles);
    }
    private int asl(int val) {
        int t = ( val << 1 ) & 0xFF;
        if ( myC64Tools.testBit(val,7) )
            setFlagC(true);
        else
            setFlagC(false);
        setFlagZ(t);
        setFlagN(t);
        return t;
    }
    public void printOut() {
        String outStr = "";
        outStr += "regA: ";
        outStr += myC64Tools.byte2hex(getRegA());
        outStr += "\nregX: ";
        outStr += myC64Tools.byte2hex(getRegX());
        outStr += "\nregY: ";
        outStr += myC64Tools.byte2hex(getRegY());
        outStr += "\nregSR: ";
        outStr += myC64Tools.byte2hex(getRegSR());
        outStr += "\nregSP: ";
        outStr += myC64Tools.byte2hex(getRegSP());
        outStr += "\nregPC: ";
        outStr += myC64Tools.byte2hex(getRegPC());
        outStr += "\ncycles: ";
        outStr += Integer.toString(getCycles());        
        outStr += "\n";
        myC64Tools.printOut( outStr );
    } 
}
