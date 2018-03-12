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
    private final myC64Memory memory;
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
        regSP = 0;
        regSR = 0;
        decSP();
        regPC = memory.readSystemWord(myC64Config.addrResetVector);
        cycleCntr = 0;
    }
    public int getCycles() {
        return cycleCntr;
    }
    /**
     * Program Counter
     * @return 
     */
    public int getRegPC() {
        return regPC & 0xFFFF;
    }
    /**
     * set the Program counter
     * it is public for debugging
     * @param reg 
     */
    public void setRegPC(int reg) {
        regPC = reg & 0xFFFF;
    }
    /**
     * Status Register 
     * Bit 4: B-Flag always true.
     * Bit 5: always true.
     * enthaelt Flags
     * @return 
     */
    public int getRegSR() {
        // Bit 5 is always on
        int regTmp = myC64Tools.setBit(regSR & 0xFF, 5, true);
        // B Flag is always returned true.
        return myC64Tools.setBit(regTmp, 4, true);
    }
    private void setRegSR(int reg) {
        regSR = reg & 0xFF;        
    }
    /**
     * Stack Pointer
     * @return 
     */
    public int getRegSP() {
        return regSP & 0xFF;
    }
    private void setRegSP(int reg) {
        regSP = reg & 0xFF;
    }
    /**
     * Akkumulator 
     * @return 
     */
    public int getRegA() {
        return regA & 0xFF;
    }
    public void setRegA(int reg) {
        regA = reg & 0xFF;
    }
    /**
     * Register X
     * @return 
     */
    public int getRegX() {
        return regX & 0xFF;
    }
    public void setRegX(int reg) {
        regX = reg & 0xFF;
    }
    /**
     * Register Y
     * @return 
     */
    public int getRegY() {
        return regY & 0xFF;
    }
    public void setRegY(int reg) {
        regY = reg & 0xFF;
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
     * Dezimal flag
     * @return break flag
     */
    private boolean getFlagD(){
        return ( myC64Tools.testBit(getRegSR(),3) );
    }
    /**
     * Dezimal flag
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
    /**
     * erhoeht Program Counter Flag
     */
    private void incPC() {
        setRegPC( getRegPC()+1 );
    }
    /**
     * erhoeht StackPointer
     */
    private void incSP() {
        // wrap around
        setRegSP( getRegSP()+1 );
    }
    /**
     * erniedrigt StackPointer
     */
    private void decSP() {
        // wrap around
        if ( 0 == getRegSP() )
            setRegSP( 0xFF );
        else 
            setRegSP( getRegSP()-1 );
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
        int adr = myC64Config.addrBaseStack+getRegSP();
        memory.writeSystemByte(adr , val);
        myC64Tools.printOut("PUSH Adr:"+myC64Tools.word2hex(adr) +
                            " Val:"+myC64Tools.byte2hex(val, 0));
        decSP();
    }
    /**
     * liest vom Stapel einen wert.
     * @return oberster stapel wert.
     */
    private int pop() {
        incSP();
        int adr = myC64Config.addrBaseStack+getRegSP();
        int val = memory.readSystemByte(adr);                    
        myC64Tools.printOut("POP: Adr:"+myC64Tools.word2hex(adr) +
                            " Val:"+myC64Tools.byte2hex(val, 0));
        return val;
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
     * https://www.c64-wiki.de/wiki/Adressierung#Zeropage_X-indizierte_Adressierung
     * @return 
     */
    private int zeroXAdr() {
        /* wrap around zero page */
        return ( zeroPage() + getRegX() ) & 0xFF;
    }
    /**
     * https://www.c64-wiki.de/wiki/Adressierung#Zeropage_Y-indizierte_Adressierung
     * @return 
     */
    private int zeroYAdr() {
        /* wrap around zero page */
        return ( zeroPage() + getRegY() ) & 0xFF;
    }
    /**
     * https://www.c64-wiki.de/wiki/Adressierung#Direkte_Adressierung
     * KOnstanter Wert wird direkt als Operand verwendet.
     * direkte adressierung
     */
    private int immidiate() {
        return getActOp();
    }
    /**
     * https://www.c64-wiki.de/wiki/Adressierung#Absolute_Adressierung
     */
    private int absoluteAdr() {
        return getActOpWord();
    }
    /**
     * https://www.c64-wiki.de/wiki/Adressierung#Absolute_Y-indizierte_Adressierung
     * @return 
     */
    private int absoluteIndiziertY() {
        return getActOpWord() + getRegY();
    }
    /**
     * https://www.c64-wiki.de/wiki/Adressierung#Absolute_X-indizierte_Adressierung
     * @return 
     */
    private int absoluteIndiziertX() {
        return getActOpWord() + getRegX();
    }
    /**
     * https://www.c64-wiki.de/wiki/Adressierung#Indirekte_X-indizierte_Zeropage-Adressierung
     * Indirekte X-indizierte Zeropage Adressierung
     */
    private int indirektIndiziertZero_X() {
        int addRZero = ( zeroPage() + getRegX() ) & 0xFF;
        return memory.readSystemWord(addRZero);        
    }
    /**
     * https://www.c64-wiki.de/wiki/Adressierung#Indirekte_Y-nachindizierte_Zeropage-Adressierung
     * @return 
     */
    private int indirektNachindiziertZero_Y() {
        return memory.readSystemWord( zeroPage() ) + getRegY() ;
    }
    /**
     * https://www.c64-wiki.de/wiki/Opcode
     * interpretiert die Opcodes.
     * http://www.oxyron.de/html/opcodes02.html
     * for cycle counter
     */
    public boolean emulate() {
        int op = getActOp();
        switch(op) {
            case 0x00: brk(); break; // BRK                 
            case 0x01: // ORA
                ora(memory.readSystemByte(indirektIndiziertZero_X()),6); break; 
            case 0x05: // ORA
                ora(memory.readSystemByte(zeroPage()),3); break; 
            case 0x06: // ASL
                aslMemRead(zeroPage(),5); break; 
            case 0x08: // PHP https://www.c64-wiki.de/wiki/PHP
                int regTmp = getRegSR();
                push(regTmp);
                addCycleCnt(3);
                break;
            case 0x09: // ORA https://www.c64-wiki.de/wiki/ORA_(RAUTE)$nn 
                ora(immidiate(),2); break;
            case 0x0A: // ASL https://www.c64-wiki.de/wiki/ASL
                setRegA(asl(getRegA()));addCycleCnt(2);break;
            case 0x0D: // ORA https://www.c64-wiki.de/wiki/ORA_$hhll
                ora(memory.readSystemByte(absoluteAdr()),4);break;
            case 0x0E: // ASL          
                aslMemRead(absoluteAdr(),6); break;
            case 0x10: // BPL
                bpl(); break;
            case 0x11: // ORA
                ora(memory.readSystemByte(indirektNachindiziertZero_Y()),5); break;
            case 0x15: // ORA
                ora(memory.readSystemByte(zeroXAdr()),4); break;
            case 0x16: // ASL https://www.c64-wiki.de/wiki/ASL_$ll,_X
                aslMemRead(zeroXAdr(),6); break;
            case 0x18: // CLC https://www.c64-wiki.de/wiki/CLC
                setFlagC(false);addCycleCnt(2);break;
            case 0x19: // ORA https://www.c64-wiki.de/wiki/ORA_$hhll,_Y
                ora(memory.readSystemByte(absoluteIndiziertY()),4); break;
            case 0x1D: // ASL https://www.c64-wiki.de/wiki/ASL_$hhll,_X
                aslMemRead(absoluteIndiziertY(),7); break;
            case 0x20: // JSR
                jsr(); break;
            case 0x21: // AND https://www.c64-wiki.de/wiki/AND_($ll,_X)
                and(memory.readSystemByte(indirektIndiziertZero_X()),6); break;
            case 0x24: // BIT 
                bit(zeroPage(),3); break;
            case 0x25: // AND https://www.c64-wiki.de/wiki/AND_$ll
                and(memory.readSystemByte(zeroPage()),3);break;
            case 0x26: // ROL https://www.c64-wiki.de/wiki/ROL_$ll
                rolMemRead(zeroPage(),5); break;
            case 0x28: // PLP https://www.c64-wiki.de/wiki/PLP
                setRegSR( pop() ); addCycleCnt(4); break;
            case 0x29: // AND https://www.c64-wiki.de/wiki/AND_$hhll
                and(immidiate(),2); break;
            case 0x2A: // ROL https://www.c64-wiki.de/wiki/ROL
                setRegA( rol( getRegA() ) ); addCycleCnt(2); break;
            case 0x2C: // BIT https://www.c64-wiki.de/wiki/BIT_$hhll
                bit(absoluteAdr(),4); break;
            case 0x2D: // AND https://www.c64-wiki.de/wiki/AND_$hhll
                and(memory.readSystemByte( absoluteAdr() ),4); break;
            case 0x2E: // ROL https://www.c64-wiki.de/wiki/ROL_$hhll
                rolMemRead(absoluteAdr(),6); break;
            case 0x30: // BMI https://www.c64-wiki.de/wiki/BMI_$hhll
                bmi(); break;
            case 0x31: // AND https://www.c64-wiki.de/wiki/AND_($ll),_Y
                and(memory.readSystemByte(indirektNachindiziertZero_Y()),3);break;
            case 0x35: // AND https://www.c64-wiki.de/wiki/AND_$ll,_X
                and(memory.readSystemByte(zeroXAdr()),4); break;
            case 0x36: // ROL https://www.c64-wiki.de/wiki/ROL_$ll,_X
                rolMemRead(memory.readSystemByte(zeroXAdr()), 6); break;
            case 0x38: // SEC https://www.c64-wiki.de/wiki/SEC
                setFlagC(true); addCycleCnt(2); break;
            case 0x39: // AND https://www.c64-wiki.de/wiki/AND_$hhll,_X
                and(memory.readSystemByte(absoluteIndiziertY()),4); break;
            case 0x3D: // AND https://www.c64-wiki.de/wiki/AND_$hhll,_X
                and(memory.readSystemByte(absoluteIndiziertX()),4); break;                
            case 0x3E: // AND https://www.c64-wiki.de/wiki/AND_$hhll,_X
                rolMemRead(memory.readSystemByte(absoluteIndiziertX()),7); break;
            case 0x40: // RTI
                rti(); break;
            case 0x41: // EOR https://www.c64-wiki.de/wiki/EOR_($ll,_X)
                eor(memory.readSystemByte(indirektIndiziertZero_X()),6); break;
            case 0x45: // EOR https://www.c64-wiki.de/wiki/EOR_$ll
                eor(memory.readSystemByte(zeroPage()),3); break;
            case 0x46: // LSR https://www.c64-wiki.de/wiki/LSR_$ll
                lsrMemRead(zeroPage(), 5); break;
            case 0x48: // PHA  https://www.c64-wiki.de/wiki/PHA
                push(getRegA());addCycleCnt(3);break;
            case 0x49: // EOR
                eor(getActOp(),2); break;
            case 0x4A: // LSR https://www.c64-wiki.de/wiki/Opcode
                setRegA( lsr( getRegA())); addCycleCnt(2); break;
            case 0x4C: // JMP https://www.c64-wiki.de/wiki/JMP_$hhll
                setRegPC(absoluteAdr()); addCycleCnt(3); break;
            case 0x4D: // EOR https://www.c64-wiki.de/wiki/JMP_$hhll
                eor(memory.readSystemByte(absoluteAdr()),4); break;
            case 0x4E: // LSR https://www.c64-wiki.de/wiki/LSR_$hhll
                lsrMemRead(absoluteAdr(), 6); break;
            case 0x50: // BVC https://www.c64-wiki.de/wiki/BVC_$hhll
                bvc(); break;
            case 0x51: // EOR https://www.c64-wiki.de/wiki/EOR_($ll),_Y
                eor(memory.readSystemByte(indirektNachindiziertZero_Y()),5);break;
            case 0x55: // EOR https://www.c64-wiki.de/wiki/EOR_$ll,_X                
                eor(memory.readSystemByte(zeroXAdr()),4); break;
            case 0x56: // LSR https://www.c64-wiki.de/wiki/LSR_$ll,_X
                lsrMemRead(zeroXAdr(), 6); break;
            case 0x58: // CLI https://www.c64-wiki.de/wiki/CLI
                setFlagI(false); addCycleCnt(2); break;
            case 0x59: // EOR https://www.c64-wiki.de/wiki/EOR_$hhll,_Y
                eor(memory.readSystemByte(absoluteIndiziertY()),4); break;
            case 0x5D: // EOR https://www.c64-wiki.de/wiki/EOR_$hhll,_X
                eor(memory.readSystemByte(absoluteIndiziertX()),4); break;
            case 0x5E: // LSR https://www.c64-wiki.de/wiki/LSR_$hhll,_X
                lsrMemRead(absoluteIndiziertX(), 7); break;
            case 0x60: // RTS https://www.c64-wiki.de/wiki/RTS
                rts();break;
            case 0x61: // ADC https://www.c64-wiki.de/wiki/ADC_($ll,_X)
                adc(memory.readSystemByte(indirektIndiziertZero_X()),6);break;
            case 0x65: // ADC https://www.c64-wiki.de/wiki/ADC
                adc(memory.readSystemByte(zeroPage()),5);break;
            case 0x66: // ROR https://www.c64-wiki.de/wiki/ROR_$ll
                rorMemRead(zeroPage(),5); break;
            case 0x68: // PLA https://www.c64-wiki.de/wiki/PLA
                pla(); break;
            case 0x69: // ADC https://www.c64-wiki.de/wiki/ADC_(RAUTE)$nn
                adc(getActOp(),2); break;
            case 0x6A: // ROR 
                setRegA(ror(getRegA()));addCycleCnt(2);break;
            case 0x6C: // JMP 
                setRegPC(memory.readSystemWord(absoluteAdr()));addCycleCnt(3);break;                
            case 0x6D: // ADC
                adc(memory.readSystemByte(absoluteAdr()),4);break;
            case 0x6E: // ROR
                rorMemRead(absoluteAdr(), 6); break;
            case 0x70: // BVS https://www.c64-wiki.de/wiki/BVS_$hhll
                bvs(); break;
            case 0x71: // ADC https://www.c64-wiki.de/wiki/ADC_($ll),_Y
                adc(memory.readSystemByte(indirektNachindiziertZero_Y()),5);break;
            case 0x75: // ADC https://www.c64-wiki.de/wiki/ADC_$ll,_X
                adc(memory.readSystemByte(zeroXAdr()),4);break;
            case 0x76: // ROR https://www.c64-wiki.de/wiki/ROR_$ll,_X
                rorMemRead(zeroXAdr(), 6); break;
            case 0x78: // SEI https://www.c64-wiki.de/wiki/SEI
                setFlagI(true); addCycleCnt(2); break;
            case 0x79: // ADC https://www.c64-wiki.de/wiki/ADC_$hhll,_Y
                adc(memory.readSystemByte(absoluteIndiziertY()),4); break;
            case 0x7D: // ADC  https://www.c64-wiki.de/wiki/ADC_$hhll,_X
                adc(absoluteIndiziertX(),4); break;
            case 0x7E: // ROR https://www.c64-wiki.de/wiki/ROR_$hhll,_X
                rorMemRead(absoluteIndiziertX(), 7); break;
            case 0x81: // STA https://www.c64-wiki.de/wiki/STA_($ll,_X)
                sta(indirektIndiziertZero_X(),6); break;
            case 0x84: // STY https://www.c64-wiki.de/wiki/STY_$ll
                sty(zeroPage(),3);break;
            case 0x85: // STA https://www.c64-wiki.de/wiki/STY_$ll
                sta(zeroPage(),3);break;
            case 0x86: // STA https://www.c64-wiki.de/wiki/STX_$ll
                stx(zeroPage(),3);break;
            case 0x88: // DEY https://www.c64-wiki.de/wiki/DEY
                dey(); break;
            case 0x8A: // TXA https://www.c64-wiki.de/wiki/TXA
                txa();break;
            case 0x8C: // STY https://www.c64-wiki.de/wiki/STY_$hhll
                sty(absoluteAdr(),4);break;
            case 0x8D: // STY https://www.c64-wiki.de/wiki/STA_$hhll
                sta(absoluteAdr(),4);break;
            case 0x8E: // STY https://www.c64-wiki.de/wiki/STX_$hhll
                sta(absoluteAdr(),4);break;
            case 0x90: // BCC https://www.c64-wiki.de/wiki/BCC_$hhll
                bcc(); break;
            case 0x91: // STA https://www.c64-wiki.de/wiki/STA_($ll),_Y
                sta(indirektNachindiziertZero_Y(),6);break;
            case 0x94: // STY https://www.c64-wiki.de/wiki/STY_$ll,_X
                sty(zeroXAdr(),4); break;
            case 0x95: // STA https://www.c64-wiki.de/wiki/STA_$ll,_X
                sta(zeroXAdr(),4);break;
            case 0x96: // STX https://www.c64-wiki.de/wiki/STX_$ll,_Y
                stx(zeroYAdr(),4);break;
            case 0x98: // TYA https://www.c64-wiki.de/wiki/TYA
                tya();break;
            case 0x99: // STA https://www.c64-wiki.de/wiki/STA_$hhll,_Y
                sta(absoluteIndiziertY(),5);break;
            case 0x9A: // TXS https://www.c64-wiki.de/wiki/TXS
                setRegSP(getRegX());addCycleCnt(2);break;
            case 0x9D: // STA https://www.c64-wiki.de/wiki/STA_$hhll,_X
                sta(absoluteIndiziertX(),5);break;
            case 0xA0: // LDY https://www.c64-wiki.de/wiki/LDY_(RAUTE)$nn
                ldy(getActOp(),2);break;
            case 0xA1: // LDA https://www.c64-wiki.de/wiki/LDA_($ll,_X)
                lda(memory.readSystemByte(indirektIndiziertZero_X()),6);break;
            case 0xA2: // LDX https://www.c64-wiki.de/wiki/LDX_(RAUTE)$nn
                ldx(getActOp(),2);break;
            case 0xA4: // LDY https://www.c64-wiki.de/wiki/LDY_$ll
                ldy(memory.readSystemByte(zeroPage()),3);break;
            case 0xA5: // LDA https://www.c64-wiki.de/wiki/LDA_$ll
                lda(memory.readSystemByte(zeroPage()),3);break;
            case 0xA6: // LDX https://www.c64-wiki.de/wiki/LDX_$ll
                ldx(memory.readSystemByte(zeroPage()),3);break;
            case 0xA8: // TAY https://www.c64-wiki.de/wiki/TAY
                tay(); break;
            case 0xA9: // LDA https://www.c64-wiki.de/wiki/LDA_(RAUTE)$nn
                lda(getActOp(),2);break;
            case 0xAA: // TAX https://www.c64-wiki.de/wiki/TAX
                tax();break;
            case 0xAC: // LDY https://www.c64-wiki.de/wiki/LDY_$hhll
                ldy(memory.readSystemByte(absoluteAdr()),4);break;
            case 0xAD: // LDA https://www.c64-wiki.de/wiki/LDA_$hhll
                lda(memory.readSystemByte(absoluteAdr()),4);break;
            case 0xAE: // LDX https://www.c64-wiki.de/wiki/LDX_$hhll
                ldx(memory.readSystemByte(absoluteAdr()),4);break;
            case 0xB0: // BCS https://www.c64-wiki.de/wiki/BCS_$hhll
                bcs(); break;
            case 0xB1: // LDA https://www.c64-wiki.de/wiki/LDA_($ll),_Y
                lda(memory.readSystemByte(indirektNachindiziertZero_Y()),5);break;
            case 0xB4: // LDY https://www.c64-wiki.de/wiki/LDY_$ll,_X
                ldy(memory.readSystemByte(zeroXAdr()),3);break;
            case 0xB5: // LDA https://www.c64-wiki.de/wiki/LDA_$ll,_X
                lda(memory.readSystemByte(zeroXAdr()),3);break;
            case 0xB6: // LDA https://www.c64-wiki.de/wiki/LDX_$ll,_Y
                ldx(memory.readSystemByte(zeroYAdr()),3);break;
            case 0xB8: // CLV https://www.c64-wiki.de/wiki/CLV
                setFlagV(false);addCycleCnt(2);break;
            case 0XB9: // LDA https://www.c64-wiki.de/wiki/LDA_$hhll,_Y
                lda(memory.readSystemByte(absoluteIndiziertY()),4);break;
            case 0xBA: // TSX https://www.c64-wiki.de/wiki/TSX
                tsx();break;
            case 0xBC: // LDY https://www.c64-wiki.de/wiki/LDY_$hhll,_X
                ldy(memory.readSystemByte(absoluteIndiziertX()),4);break;
            case 0xBD: // LDA https://www.c64-wiki.de/wiki/LDA_$hhll,_X
                lda(memory.readSystemByte(absoluteIndiziertX()),4);break;
            case 0xBE: // LDX https://www.c64-wiki.de/wiki/LDX_$hhll,_Y
                ldx(memory.readSystemByte(absoluteIndiziertY()),4);break;
            case 0xC0: // CPY https://www.c64-wiki.de/wiki/CPY_(RAUTE)$nn
                cpy(getActOp(),2); break;
            case 0xC1: // CMP https://www.c64-wiki.de/wiki/CMP_($ll,_X)
                cmp(memory.readSystemByte(indirektIndiziertZero_X()),6); break;
            case 0xC4: // CPY https://www.c64-wiki.de/wiki/CPY_$ll
                cpy(memory.readSystemByte(zeroPage()),3); break;
            case 0xC5: // CMP https://www.c64-wiki.de/wiki/CPY_$ll
                cmp(memory.readSystemByte(zeroPage()),3); break;
            case 0xC6: // DEC
                dec(zeroPage(),5); break;
            case 0xC8: // INY
                iny();break;
            case 0xC9: // CMP
                cmp(getActOp(),2); break;
            case 0xCA: // DEX
                dex(); break;
            case 0xCC: // CMP 
                cpy(memory.readSystemByte(absoluteAdr()),4); break;
            case 0xCD: // CMP
                cmp(memory.readSystemByte(absoluteAdr()),4); break;
            case 0xCE: //DEC 
                dec(absoluteAdr(),6); break;
            case 0xD0: // BNE https://www.c64-wiki.de/wiki/BNE_$hhll
                bne(); break;
            case 0xD1: // CMP https://www.c64-wiki.de/wiki/CMP_($ll),_Y
                cmp(memory.readSystemByte(indirektNachindiziertZero_Y()),5);break;
            case 0xD5: // CMP https://www.c64-wiki.de/wiki/CMP_$ll,_X
                cmp(memory.readSystemByte(zeroXAdr()),4);break;
            case 0xD6: // DEC https://www.c64-wiki.de/wiki/DEC_$ll,_X
                dec(zeroXAdr(),6);break;
            case 0xD8: // CLD https://www.c64-wiki.de/wiki/CLD
                setFlagD(false);addCycleCnt(2);break;
            case 0xD9: // CMP https://www.c64-wiki.de/wiki/CMP_$hhll,_Y
                cmp(memory.readSystemByte(absoluteIndiziertY()),4);break;
            case 0xDD: // CMP https://www.c64-wiki.de/wiki/CMP_$hhll,_X
                cmp(memory.readSystemByte(absoluteIndiziertX()),4);break;
            case 0xDE: //DEC https://www.c64-wiki.de/wiki/DEC_$hhll,_X
                dec(absoluteIndiziertX(),7); break;
            case 0xE0: // CPX https://www.c64-wiki.de/wiki/CPX_(RAUTE)$nn
                cpx(getActOp(),2);break;
            case 0xE1: // SBC https://www.c64-wiki.de/wiki/SBC_($ll,_X)
                sbc(memory.readSystemByte(indirektIndiziertZero_X()),6); break;
            case 0xE4: // CPX https://www.c64-wiki.de/wiki/CPX_$ll
                cpx(memory.readSystemByte(zeroPage()),3);break;
            case 0xE5: // SBC https://www.c64-wiki.de/wiki/SBC_$ll
                sbc(memory.readSystemByte(zeroPage()),3); break;
            case 0xE6: // INC https://www.c64-wiki.de/wiki/INC_$ll
                inc(zeroPage(),5);break;
            case 0xE8: // INX https://www.c64-wiki.de/wiki/INX
                inx();break;
            case 0xE9: // SBC https://www.c64-wiki.de/wiki/SBC_(RAUTE)$nn
                sbc(getActOp(),2);break;
            case 0xEA: // NOP https://www.c64-wiki.de/wiki/NOP
                addCycleCnt(2); break;
            case 0xEC: // CPX https://www.c64-wiki.de/wiki/CPX_$hhll
                cpx(memory.readSystemByte(absoluteAdr()),4);break;
            case 0xED: // SBC https://www.c64-wiki.de/wiki/SBC_$hhll
                sbc(memory.readSystemByte(absoluteAdr()),4);break;
            case 0xEE: // INC https://www.c64-wiki.de/wiki/INC_$hhll
                inc(absoluteAdr(),4);break;
            case 0xF0: // BEQ https://www.c64-wiki.de/wiki/BEQ_$hhll
                beq();break;
            case 0xF1: // SBC https://www.c64-wiki.de/wiki/SBC_($ll),_Y
                sbc(memory.readSystemByte(indirektNachindiziertZero_Y()),5);break;
            case 0xF5: // SBC https://www.c64-wiki.de/wiki/SBC_$ll,_X
                sbc(memory.readRamByteDirect(zeroXAdr()),4);break;
            case 0xF6: // INC https://www.c64-wiki.de/wiki/INC_$ll,_X
                inc(zeroXAdr(),6);break;
            case 0xF8: // SED https://www.c64-wiki.de/wiki/SED
                setFlagD(true);addCycleCnt(2);break;                
            case 0xF9: // SBC https://www.c64-wiki.de/wiki/SBC_$hhll,_Y
                sbc(memory.readSystemByte(absoluteIndiziertY()),4);break;
            case 0xFD: // SBC https://www.c64-wiki.de/wiki/SBC_$hhll,_X
                sbc(memory.readSystemByte(absoluteIndiziertX()),4);break;
            case 0xFE: // INC https://www.c64-wiki.de/wiki/INC_$hhll,_X
                inc(absoluteIndiziertX(),7);break;
            default:
                myC64Tools.printOut("Unknown instruction: "+op+" at "+getRegPC());
                return false;
        }
        return true;
    }
    /**
     * INC
     * @param val
     * @param cycles  
     */
    private void inc(int adr, int cycles) {
        int val = memory.readSystemByte(adr);
        // read write write
        memory.writeSystemByte(adr, val);
        if ( val == 0xFF )
            val = 0x00;
        else
            val++;
        memory.writeSystemByte(adr, val);
        setFlagZ(val);
        setFlagN(val);
        addCycleCnt(2);
    }
    /**
     * INX
     */
    private void inx() {
        if ( getRegX() == 0xFF )
            setRegX(0x00);
        else
            setRegX(getRegX()+1);
        setFlagZ(getRegX());
        setFlagN(getRegX());
        addCycleCnt(2);
    }
    /**
     * INY
     */
    private void iny() {
        if ( getRegY() == 0xFF )
            setRegY(0x00);
        else
            setRegY(getRegY()+1);
        setFlagZ(getRegY());
        setFlagN(getRegY());
        addCycleCnt(2);
    }
    /**
     * DEC
     * @param val
     * @param cycles 
     */
    private void dec(int adr, int cycles) {
        int val = memory.readSystemByte(adr);
        // read write write
        memory.writeSystemByte(adr, val);
        if ( val == 0 )
            val = 0xFF;
        else
            val--;
        memory.writeSystemByte(adr, val);
        setFlagZ(val);
        setFlagN(val);
        addCycleCnt(cycles);
        
    }
    /**
     * DEX
     */
    private void dex() {
        // wrap around
        if ( 0 == getRegX() )
            setRegX( 0xFF );
        else 
            setRegX( getRegX()-1 );
        setFlagZ(getRegX());
        setFlagN(getRegX());
        addCycleCnt(2);
    }
    /**
     * DEY
     */
    private void dey() {
        // wrap around
        if ( 0 == getRegY() )
            setRegY( 0xFF );
        else 
            setRegY( getRegY()-1 );
        setFlagZ(getRegY());
        setFlagN(getRegY());
        addCycleCnt(2);
    }
    /**
     * CMP 
     * http://www.6502.org/tutorials/compare_beyond.html
     * @param val 8Bit value to compare with reg A
     * @param cycles number of cycles used.
     */
    private void cmp(int val, int cycles) {
        int res = getRegA() - ( val & 0xFF );
        if ( ( val & 0xFF ) <= getRegA() )
            setFlagC(true);
        else
            setFlagC(false);
        setFlagZ( res & 0xFF );
        setFlagN( res & 0xFF );
        addCycleCnt(cycles);
    }
    /**
     * CPX 
     * @param val 8Bit value to compare with reg X
     * @param cycles number of cycles used.
     */
    private void cpx(int val, int cycles) {
        int res = getRegX() - ( val & 0xFF );
        if ( ( val & 0xFF ) <= getRegX() )
            setFlagC(true);
        else
            setFlagC(false);
        setFlagZ( res & 0xFF );
        setFlagN( res & 0xFF );
        addCycleCnt(cycles);
    }
    /**
     * CPY 
     * @param val 8Bit value to compare with reg Y
     * @param cycles number of cycles used.
     */
    private void cpy(int val, int cycles) {
        int res = getRegY() - ( val & 0xFF );
        if ( ( val & 0xFF ) <= getRegY() )
            setFlagC(true);
        else
            setFlagC(false);
        setFlagZ( res & 0xFF );
        setFlagN( res & 0xFF );
        addCycleCnt(cycles);
    }    
    /**
     * TSX
     */
    private void tsx() {
        setRegX(getRegSP());
        setFlagZ(getRegX());
        setFlagN(getRegX());
        addCycleCnt(2);
    }
    /**
     * TAY
     */
    private void tay() {
        setRegY(getRegA());
        setFlagZ(getRegY());
        setFlagN(getRegY());
        addCycleCnt(2);
    }
    /**
     * TYA
     */
    private void tya() {
        setRegA(getRegY());
        setFlagZ(getRegA());
        setFlagN(getRegA());
        addCycleCnt(2);
    }
    /**
     * TAX
     */
    private void tax() {
        setRegX(getRegA());
        setFlagZ(getRegX());
        setFlagN(getRegX());
        addCycleCnt(2);
    }
    /**
     * TXA
     */
    private void txa() {
        setRegA(getRegX());
        setFlagZ(getRegA());
        setFlagN(getRegA());
        addCycleCnt(2);
    }    
    /**
     * LDA
     */
    private void lda(int val, int cycles){
        setRegA(val);
        setFlagZ(getRegA());
        setFlagN(getRegA());
        addCycleCnt(cycles);
    }
    /**
     * STA 
     */
    private void sta(int val, int cycles){
        memory.writeSystemByte(val, getRegA());
        addCycleCnt(cycles);
    }
    /**
     * LDX
     */
    private void ldx(int val, int cycles){
        setRegX(val);
        setFlagZ(getRegX());
        setFlagN(getRegX());
        addCycleCnt(cycles);
    }
    /**
     * STX
     */
    private void stx(int val, int cycles){
        memory.writeSystemByte(val, getRegX());
        addCycleCnt(cycles);
    }
    /**
     * LDY
     */
    private void ldy(int val, int cycles){
        setRegY(val);
        setFlagZ(getRegY());
        setFlagN(getRegY());
        addCycleCnt(cycles);
    }
    /**
     * STY
     */
    private void sty(int val, int cycles){
        memory.writeSystemByte(val, getRegY());
        addCycleCnt(cycles);
    }
    /**
     * PLA
     * https://www.c64-wiki.de/wiki/PLA
     */
    private void pla() {
        setRegA(pop());
        setFlagZ(getRegA());
        setFlagN(getRegA());
        addCycleCnt(4);
    }
    /**
     * ADC 
     * @param adr adress
     * @param cycles amount of cycle
     */
    private void adc(int val, int cycles) {
        int res = getRegA() + (val & 0xFF) + ( getFlagC() ? 1 : 0);
        if ( getFlagD() ) {
            // BCD codiert https://de.wikipedia.org/wiki/BCD-Code
            int xorAdd = myC64Tools.xor(myC64Tools.xor(getRegA(),val ),res);
            if ( (xorAdd & 0x10) == 0x10) 
                res += 0x06;
            if ((res & 0xF0) > 0x90)
                res += 0x60;
        } 
        if ( res > 0xFF )
            setFlagC(true);
        else
            setFlagC(false);
        res &= 0xFF;
        // https://www.c64-wiki.de/wiki/Statusregister#Overflow-Flag
        // https://stackoverflow.com/questions/29193303/6502-emulation-proper-way-to-implement-adc-and-sbc
        if ( !myC64Tools.testBit(myC64Tools.xor(getRegA(),val ),7 ) && 
              myC64Tools.testBit(myC64Tools.xor(getRegA(),res ),7 ) ) {
            setFlagV(true);
        } else {
            setFlagV(true);
        }
        setFlagZ(res);
        setFlagN(res);
        setRegA(res);
    }
    /**
     * https://www.c64-wiki.de/wiki/SBC_($ll,_X)
     * @param val
     * @param cycles 
     */
    private void sbc(int val, int cycles) {
        // call adc with inverted val -> -val-1
        adc(myC64Tools.xor(val, 0xFF),cycles);
    }
    /**
     * special lri which have an added write command 
     * @param adr adress
     * @param cycles amount of cycle
     */
    private void lsrMemRead(int adr, int cycles) {
        int val = memory.readSystemByte(adr);
        memory.writeSystemByte(adr, val);
        memory.writeSystemByte(adr, lsr(val));
        addCycleCnt(cycles);
    }
    /**
     * http://www.6502.org/tutorials/6502opcodes.html#LSR
     * @param val
     * @return 
     */
    private int lsr(int val) {
        if ( myC64Tools.testBit(val,0) )
            setFlagC(true);
        else
            setFlagC(false);
        val = val >> 1;
        val = myC64Tools.setBit(val,0, false);
        setFlagZ(val);
        setFlagN(val);
        return val;
    }
    /**
     * EOR
     * exclusive oder
     * http://www.6502.org/tutorials/6502opcodes.html#EOR
     */
    private void eor(int val,int cycles) {
        setRegA( myC64Tools.xor(getRegA(),val ) );
        setFlagZ(getRegA());
        setFlagN(getRegA());
        addCycleCnt(cycles); 
    }
    /**
     * special asl which have an added write command 
     * @param adr adress
     * @param cycles amount of cycle
     */
    private void rorMemRead(int adr, int cycles) {
        int val = memory.readSystemByte(adr);
        memory.writeSystemByte(adr, val);
        memory.writeSystemByte(adr, ror(val));
        addCycleCnt(cycles);
    }
    /**
     * http://www.6502.org/tutorials/6502opcodes.html#ROR
     * @param val value
     * @return 
     */
    private int ror(int val) {
        boolean flagC = getFlagC();
        if ( myC64Tools.testBit(val,0) )
            setFlagC(true);
        else
            setFlagC(false);
        val = val >> 1;
        val = myC64Tools.setBit(val,7, flagC);
        setFlagZ(val);
        setFlagN(val);
        return val;
    }
    /**
     * special asl which have an added write command 
     * @param adr adress
     * @param cycles amount of cycle
     */
    private void rolMemRead(int adr, int cycles) {
        int val = memory.readSystemByte(adr);
        memory.writeSystemByte(adr, val);
        memory.writeSystemByte(adr, rol(val));
        addCycleCnt(cycles);
    }
    /**
     * http://www.6502.org/tutorials/6502opcodes.html#ROL
     * @param val value
     * @return 
     */
    private int rol(int val) {
        boolean flagC = getFlagC();
        if ( myC64Tools.testBit(val,7) )
            setFlagC(true);
        else
            setFlagC(false);
        val = val << 1;
        val = myC64Tools.setBit(val,0, flagC);
        setFlagZ(val);
        setFlagN(val);
        return val;
    }
    /**
     * https://www.c64-wiki.de/wiki/BIT_$ll
     * @param adr adress
     * @param cycles amount of cycle
     */
    private void bit(int adr,int cycles) {
        int val = memory.readSystemByte(adr);
        if ( myC64Tools.testBit(val,7) )
            setFlagV(true);
        else
            setFlagV(false);
        setFlagZ(val & getRegA());
        setFlagN(val);
        addCycleCnt(cycles);
    }
    /**
     * AND Operand
     */
    private void and(int val,int cycles) {
        setRegA( getRegA() & val );
        setFlagZ(getRegA());
        setFlagN(getRegA());
        addCycleCnt(cycles); 
    }
    /**
     * brranch if equal.
     * https://www.c64-wiki.de/wiki/BEQ_$hhll
     */
    private void beq() {
        int adrAdd = getActOp();
        addCycleCnt(2);
        if ( getFlagZ() ) {
            addCycleCnt(1);
            // jump over page needs extra cycle
            if ( myC64Tools.pageJumpAdd( getRegPC(), adrAdd) )
                addCycleCnt(1);
            setRegPC( getRegPC() + myC64Tools.getSignedByte(adrAdd) );
        }
    }
    /**
     * https://www.c64-wiki.de/wiki/cmp_$hhll
     */
    private void bne() {
        int adrAdd = getActOp();
        addCycleCnt(2);
        if ( !getFlagZ() ) {
            addCycleCnt(1);
            // jump over page needs extra cycle
            if ( myC64Tools.pageJumpAdd( getRegPC(), adrAdd) )
                addCycleCnt(1);
            int t = myC64Tools.getSignedByte(adrAdd);
            setRegPC( getRegPC() + myC64Tools.getSignedByte(adrAdd) );
        }
    }
    /**
     * http://www.6502.org/tutorials/6502opcodes.html#PC
     * http://www.6502.org/tutorials/6502opcodes.html#BMI
     * https://www.c64-wiki.de/wiki/BMI_$hhll
     */
    private void bmi() {
        int adrAdd = getActOp();
        addCycleCnt(2);
        if ( getFlagN() ) {
            addCycleCnt(1);
            // jump over page needs extra cycle
            if ( myC64Tools.pageJumpAdd( getRegPC(), adrAdd) )
                addCycleCnt(1);
            setRegPC( getRegPC() + myC64Tools.getSignedByte(adrAdd) );
        }
    }
    /**
     * https://www.c64-wiki.de/wiki/BCC_$hhll
     */
    private void bcc() {
        int adrAdd = getActOp();
        addCycleCnt(2);
        if ( !getFlagC() ) {
            addCycleCnt(1);
            // jump over page needs extra cycle
            if ( myC64Tools.pageJumpAdd( getRegPC(), adrAdd) )
                addCycleCnt(1);
            setRegPC( getRegPC() + myC64Tools.getSignedByte(adrAdd) );
        }       
    }
    /**
     * branch Carry = 1
     * https://www.c64-wiki.de/wiki/BCS_$hhll
     */
    private void bcs() {
        int adrAdd = getActOp();
        addCycleCnt(2);
        if ( getFlagC() ) {
            addCycleCnt(1);
            // jump over page needs extra cycle
            if ( myC64Tools.pageJumpAdd( getRegPC(), adrAdd) )
                addCycleCnt(1);
            setRegPC( getRegPC() + myC64Tools.getSignedByte(adrAdd) );
        }       
    }
    /**
     * https://www.c64-wiki.de/wiki/BPL_$hhll
     */
    private void bpl() {
        int adrAdd = getActOp();
        addCycleCnt(2);
        if ( !getFlagN() ) {
            addCycleCnt(1);
            // jump over page needs extra cycle
            if ( myC64Tools.pageJumpAdd( getRegPC(), adrAdd) )
                addCycleCnt(1);
            setRegPC( getRegPC() + myC64Tools.getSignedByte(adrAdd) );
        }       
    }
    /**
     * http://www.6502.org/tutorials/6502opcodes.html#BVC
     * branch when overflow false
     */
    private void bvc() {
        int adrAdd = getActOp();
        addCycleCnt(2);
        if ( !getFlagV()) {
            addCycleCnt(1);
            // jump over page needs extra cycle
            if ( myC64Tools.pageJumpAdd( getRegPC(), adrAdd) )
                addCycleCnt(1);
            setRegPC( getRegPC() + myC64Tools.getSignedByte(adrAdd) );
        }               
    }
    /**
     * http://www.6502.org/tutorials/6502opcodes.html#BVS
     * branch when overflow true
     */
    private void bvs() {
        int adrAdd = getActOp();
        addCycleCnt(2);
        if ( getFlagV()) {
            addCycleCnt(1);
            // jump over page needs extra cycle
            if ( myC64Tools.pageJumpAdd( getRegPC(), adrAdd) )
                addCycleCnt(1);
            setRegPC( getRegPC() + myC64Tools.getSignedByte(adrAdd) );
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
     * https://www.c64-wiki.de/wiki/RTI
     */
    private void rti() {
        setRegSR( pop() );
        int lowByte  = pop();
        int highByte = pop();
        setRegPC(myC64Tools.getWord(lowByte,highByte));
        addCycleCnt(6);
    }
        /**
     *
     * https://www.c64-wiki.de/wiki/ORA_($ll,_X)
     */
    private void ora(int val,int cycles) {
        setRegA( getRegA() | val );
        setFlagZ(getRegA());
        setFlagN(getRegA());
        addCycleCnt(cycles);
    }
    /**
     * special asl which have an added write command 
     * https://www.c64-wiki.de/wiki/ASL_$ll
     */
    private void aslMemRead(int adr,int cycles) {
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
    /**
     * https://www.c64-wiki.de/wiki/JSR_$hhll
     */
    private void jsr() {
        int adrJmp = absoluteAdr();
        int adrPC = getRegPC();
        int adrLo = myC64Tools.getLowByte(adrPC-1);
        int adrHi = myC64Tools.getHighByte(adrPC-1);
        push(adrHi);
        push(adrLo);
        setRegPC(adrJmp);
        addCycleCnt(6);
    }
    /**
     * https://www.c64-wiki.de/wiki/RTS
     */
    public void rts() {
        int lowByte = pop();
        int highByte = pop();
        int adr = myC64Tools.getWord(lowByte,highByte);
        setRegPC(adr);
        incPC();
        addCycleCnt(6);
    }
    public void printOut() {
        String outStr = "";
        outStr += "PC\tMem[PC]\tMem[P2]\tMem[P3]\tA\tX\tY\tSR\tSP\tCyc\tNV_BDIZC\n";
        outStr += myC64Tools.word2hex(getRegPC())   + "\t";
        outStr += myC64Tools.byte2hex(memory.readSystemByte(getRegPC()),0)   + "\t";
        outStr += myC64Tools.byte2hex(memory.readSystemByte(getRegPC()+1),0) + "\t";
        outStr += myC64Tools.byte2hex(memory.readSystemByte(getRegPC()+2),0) + "\t";
        outStr += myC64Tools.byte2hex(getRegA(),0)  + "\t";
        outStr += myC64Tools.byte2hex(getRegX(),0)  + "\t";
        outStr += myC64Tools.byte2hex(getRegY(),0)  + "\t";
        outStr += myC64Tools.byte2hex(getRegSR(),0) + "\t";
        outStr += myC64Tools.byte2hex(getRegSP(),0) + "\t";
        outStr += Integer.toString(getCycles())     + "\t";
        outStr += getFlagN() ? 1 : 0;
        outStr += getFlagV() ? 1 : 0;
        outStr += "0";
        outStr += getFlagB() ? 1 : 0;
        outStr += getFlagD() ? 1 : 0;
        outStr += getFlagI() ? 1 : 0;
        outStr += getFlagZ() ? 1 : 0;
        outStr += getFlagC() ? 1 : 0;
        outStr += "\n";
        myC64Tools.printOut( outStr );
    } 
}
