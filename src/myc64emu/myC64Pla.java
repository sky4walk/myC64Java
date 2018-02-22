/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */

package myc64emu;

/**
 * https://www.c64-wiki.de/wiki/PLA_(C64-Chip)
 * @author Andre Betz mail@Andrebetz.de
 */
public class myC64Pla {
    public static class AdressraumValues {
        public static int RAM          = 0;
        public static int OPEN         = 1;
        public static int BASICROM     = 2;
        public static int CHARSETROM   = 3;
        public static int KERNALROM    = 4;
        public static int IO           = 5;
        public static int CartLow      = 6;
        public static int CartHigh     = 7;        
    }
    private boolean CHAREN;
    private boolean HIRAM;
    private boolean LORAM;
    private boolean EXROM;
    private boolean GAME;
    private int prozessorPort;
    
    public myC64Pla() {
        reset();
    }

    public void reset() {
        setCHAREN();
        setHIRAM();
        setLORAM();
        setEXROM();
        setGAME();
        prozessorPort = 0;
    }

    public void setCHAREN(){
        CHAREN = true;
        myC64Tools.setBit(prozessorPort,2,CHAREN);
    }
    public void clearCHAREN(){
        CHAREN = false;
        myC64Tools.setBit(prozessorPort,2,CHAREN);
    }
    public void setHIRAM(){
        HIRAM = true;
        myC64Tools.setBit(prozessorPort,1,HIRAM);
    }
    public void clearHIRAM(){
        HIRAM = false;
        myC64Tools.setBit(prozessorPort,1,HIRAM);
    }
    public void setLORAM(){
        LORAM = true;
        myC64Tools.setBit(prozessorPort,0,LORAM);
    }
    public void clearLORAM(){
        LORAM = false;
        myC64Tools.setBit(prozessorPort,0,LORAM);
    }
    public void setEXROM(){
        EXROM = true;
    }
    public void clearEXROM(){
        EXROM = false;        
    }
    public void setGAME(){
        GAME = true;
    }
    public void clearGAME(){
        GAME = false;        
    }
    /**
     * Prozessorport werden nur HIRAM,LORAM und CHAREN verwendet
     * da die anderen Leitungen nicht aus der CPU kommen
     */
    public int getProzessorport(int adr) {
        return prozessorPort;
    }
    /**
     * Prozessorport werden nur HIRAM,LORAM und CHAREN verwendet
     * da die anderen Leitungen nicht aus der CPU kommen
     */
    public void setProzessorport(int port) {
        prozessorPort = port;
        if ( myC64Tools.testBit(prozessorPort,2) )  setCHAREN();
        else                                        clearCHAREN();
        if ( myC64Tools.testBit(prozessorPort,1) )  setHIRAM();
        else                                        clearHIRAM();
        if ( myC64Tools.testBit(prozessorPort,0) )  setLORAM();
        else                                        clearLORAM();
    }
    public int getAdressraum(int addr) {
        if      ( myC64Tools.isInsideAdr( 0x1000, 0x7FFF, addr ) )
            return getAdressraum_0x1000_0x7FFF();
        else if ( myC64Tools.isInsideAdr( 0x8000, 0x9FFF, addr ) )
            return getAdressraum_0x8000_0x9FFF();
        else if ( myC64Tools.isInsideAdr( 0xA000, 0xBFFF, addr ) )
            return getAdressraum_0xA000_0xBFFF();
        else if ( myC64Tools.isInsideAdr( 0xC000, 0xCFFF, addr ) )
            return getAdressraum_0xC000_0xCFFF();
        else if ( myC64Tools.isInsideAdr( 0xD000, 0xDFFF, addr ) )
            return getAdressraum_0xD000_0xDFFF();
        else if ( myC64Tools.isInsideAdr( 0xE000, 0xFFFF, addr ) ) 
            return getAdressraum_0xE000_0xFFFF();                                
        return AdressraumValues.RAM;            
    }
    public int getAdressraum_0x1000_0x7FFF(){
        if      (  GAME &&  EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //1
            return AdressraumValues.RAM;
        else if (  GAME &&            CHAREN &&  HIRAM && !LORAM   )  //2
            return AdressraumValues.RAM;
        else if (  GAME &&            CHAREN && !HIRAM &&  LORAM   )  //3
            return AdressraumValues.RAM;
        else if (  GAME &&                      !HIRAM && !LORAM   )  //4
            return AdressraumValues.RAM;
        else if (  GAME &&  EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //5
            return AdressraumValues.RAM;
        else if (  GAME &&           !CHAREN &&  HIRAM && !LORAM   )  //6
            return AdressraumValues.RAM;
        else if (  GAME &&           !CHAREN && !HIRAM && !LORAM   )  //7
            return AdressraumValues.RAM;
        else if (  GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //8
            return AdressraumValues.RAM;
        else if (  GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //9
            return AdressraumValues.RAM;
        else if ( !GAME &&  EXROM                                  )  //10
            return AdressraumValues.OPEN;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //11
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM && !LORAM   )  //12
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM &&  CHAREN && !HIRAM &&  LORAM   )  //13
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM &&            !HIRAM && !LORAM   )  //14
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //15
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM && !LORAM   )  //16
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM && !CHAREN && !HIRAM &&  LORAM   )  //17
            return AdressraumValues.RAM;                  
        return AdressraumValues.RAM;
    }
    public int getAdressraum_0x8000_0x9FFF(){
        if      (  GAME &&  EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //1
            return AdressraumValues.RAM;
        else if (  GAME &&            CHAREN &&  HIRAM && !LORAM   )  //2
            return AdressraumValues.RAM;
        else if (  GAME &&            CHAREN && !HIRAM &&  LORAM   )  //3
            return AdressraumValues.RAM;
        else if (  GAME &&                      !HIRAM && !LORAM   )  //4
            return AdressraumValues.RAM;
        else if (  GAME &&  EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //5
            return AdressraumValues.RAM;
        else if (  GAME &&           !CHAREN &&  HIRAM && !LORAM   )  //6
            return AdressraumValues.RAM;
        else if (  GAME &&           !CHAREN && !HIRAM && !LORAM   )  //7
            return AdressraumValues.RAM;
        else if (  GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //8
            return AdressraumValues.CartLow;
        else if (  GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //9
            return AdressraumValues.CartLow;
        else if ( !GAME &&  EXROM                                  )  //10
            return AdressraumValues.CartLow;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //11
            return AdressraumValues.CartLow;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM && !LORAM   )  //12
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM &&  CHAREN && !HIRAM &&  LORAM   )  //13
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM &&            !HIRAM && !LORAM   )  //14
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //15
            return AdressraumValues.CartLow;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM && !LORAM   )  //16
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM && !CHAREN && !HIRAM &&  LORAM   )  //17
            return AdressraumValues.RAM;                  
        return AdressraumValues.RAM;
    }
    public int getAdressraum_0xA000_0xBFFF(){
        if      (  GAME &&  EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //1
            return AdressraumValues.BASICROM;
        else if (  GAME &&            CHAREN &&  HIRAM && !LORAM   )  //2
            return AdressraumValues.RAM;
        else if (  GAME &&            CHAREN && !HIRAM &&  LORAM   )  //3
            return AdressraumValues.RAM;
        else if (  GAME &&                      !HIRAM && !LORAM   )  //4
            return AdressraumValues.RAM;
        else if (  GAME &&  EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //5
            return AdressraumValues.BASICROM;
        else if (  GAME &&           !CHAREN &&  HIRAM && !LORAM   )  //6
            return AdressraumValues.RAM;
        else if (  GAME &&           !CHAREN && !HIRAM && !LORAM   )  //7
            return AdressraumValues.RAM;
        else if (  GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //8
            return AdressraumValues.BASICROM;
        else if (  GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //9
            return AdressraumValues.BASICROM;
        else if ( !GAME &&  EXROM                                  )  //10
            return AdressraumValues.OPEN;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //11
            return AdressraumValues.CartHigh;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM && !LORAM   )  //12
            return AdressraumValues.CartHigh;
        else if ( !GAME && !EXROM &&  CHAREN && !HIRAM &&  LORAM   )  //13
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM &&            !HIRAM && !LORAM   )  //14
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //15
            return AdressraumValues.CartHigh;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM && !LORAM   )  //16
            return AdressraumValues.CartHigh;
        else if ( !GAME && !EXROM && !CHAREN && !HIRAM &&  LORAM   )  //17
            return AdressraumValues.RAM;                  
        return AdressraumValues.RAM;
    }
    public int getAdressraum_0xC000_0xCFFF(){
        if      (  GAME &&  EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //1
            return AdressraumValues.RAM;
        else if (  GAME &&            CHAREN &&  HIRAM && !LORAM   )  //2
            return AdressraumValues.RAM;
        else if (  GAME &&            CHAREN && !HIRAM &&  LORAM   )  //3
            return AdressraumValues.RAM;
        else if (  GAME &&                      !HIRAM && !LORAM   )  //4
            return AdressraumValues.RAM;
        else if (  GAME &&  EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //5
            return AdressraumValues.RAM;
        else if (  GAME &&           !CHAREN &&  HIRAM && !LORAM   )  //6
            return AdressraumValues.RAM;
        else if (  GAME &&           !CHAREN && !HIRAM && !LORAM   )  //7
            return AdressraumValues.RAM;
        else if (  GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //8
            return AdressraumValues.RAM;
        else if (  GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //9
            return AdressraumValues.RAM;
        else if ( !GAME &&  EXROM                                  )  //10
            return AdressraumValues.OPEN;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //11
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM && !LORAM   )  //12
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM &&  CHAREN && !HIRAM &&  LORAM   )  //13
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM &&            !HIRAM && !LORAM   )  //14
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //15
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM && !LORAM   )  //16
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM && !CHAREN && !HIRAM &&  LORAM   )  //17
            return AdressraumValues.RAM;                  
        return AdressraumValues.RAM;
    }
    public int getAdressraum_0xD000_0xDFFF(){
        if      (  GAME &&  EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //1
            return AdressraumValues.IO;
        else if (  GAME &&            CHAREN &&  HIRAM && !LORAM   )  //2
            return AdressraumValues.IO;
        else if (  GAME &&            CHAREN && !HIRAM &&  LORAM   )  //3
            return AdressraumValues.IO;
        else if (  GAME &&                      !HIRAM && !LORAM   )  //4
            return AdressraumValues.RAM;
        else if (  GAME &&  EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //5
            return AdressraumValues.CHARSETROM;
        else if (  GAME &&           !CHAREN &&  HIRAM && !LORAM   )  //6
            return AdressraumValues.CHARSETROM;
        else if (  GAME &&           !CHAREN && !HIRAM && !LORAM   )  //7
            return AdressraumValues.CHARSETROM;
        else if (  GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //8
            return AdressraumValues.IO;
        else if (  GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //9
            return AdressraumValues.CHARSETROM;
        else if ( !GAME &&  EXROM                                  )  //10
            return AdressraumValues.IO;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //11
            return AdressraumValues.IO;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM && !LORAM   )  //12
            return AdressraumValues.IO;
        else if ( !GAME && !EXROM &&  CHAREN && !HIRAM &&  LORAM   )  //13
            return AdressraumValues.IO;
        else if ( !GAME && !EXROM &&            !HIRAM && !LORAM   )  //14
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //15
            return AdressraumValues.CHARSETROM;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM && !LORAM   )  //16
            return AdressraumValues.CHARSETROM;
        else if ( !GAME && !EXROM && !CHAREN && !HIRAM &&  LORAM   )  //17
            return AdressraumValues.RAM;                  
        return AdressraumValues.RAM;
    }
    public int getAdressraum_0xE000_0xFFFF(){
        if      (  GAME &&  EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //1
            return AdressraumValues.KERNALROM;
        else if (  GAME &&            CHAREN &&  HIRAM && !LORAM   )  //2
            return AdressraumValues.KERNALROM;
        else if (  GAME &&            CHAREN && !HIRAM &&  LORAM   )  //3
            return AdressraumValues.RAM;
        else if (  GAME &&                      !HIRAM && !LORAM   )  //4
            return AdressraumValues.RAM;
        else if (  GAME &&  EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //5
            return AdressraumValues.KERNALROM;
        else if (  GAME &&           !CHAREN &&  HIRAM && !LORAM   )  //6
            return AdressraumValues.KERNALROM;
        else if (  GAME &&           !CHAREN && !HIRAM && !LORAM   )  //7
            return AdressraumValues.RAM;
        else if (  GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //8
            return AdressraumValues.KERNALROM;
        else if (  GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //9
            return AdressraumValues.KERNALROM;
        else if ( !GAME &&  EXROM                                  )  //10
            return AdressraumValues.CartHigh;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM &&  LORAM   )  //11
            return AdressraumValues.KERNALROM;
        else if ( !GAME && !EXROM &&  CHAREN &&  HIRAM && !LORAM   )  //12
            return AdressraumValues.KERNALROM;
        else if ( !GAME && !EXROM &&  CHAREN && !HIRAM &&  LORAM   )  //13
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM &&            !HIRAM && !LORAM   )  //14
            return AdressraumValues.RAM;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM &&  LORAM   )  //15
            return AdressraumValues.KERNALROM;
        else if ( !GAME && !EXROM && !CHAREN &&  HIRAM && !LORAM   )  //16
            return AdressraumValues.KERNALROM;
        else if ( !GAME && !EXROM && !CHAREN && !HIRAM &&  LORAM   )  //17
            return AdressraumValues.RAM;                  
        return AdressraumValues.RAM;
    }
}
