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
    /**
     * memory 64KB
     */
    private myC64Memory memory;
    
    private boolean CHAREN;
    private boolean HIRAM;
    private boolean LORAM;
    private boolean EXROM;
    private boolean GAME;   
    
    public void myC64Pla(myC64Memory mem) {
        CHAREN = false;
        HIRAM  = false;
        LORAM  = false;
        EXROM  = false;
        GAME   = false;
        memory = mem;
    }
    
    public void setCHAREN(){
        CHAREN = true;
    }
    public void clearCHAREN(){
        CHAREN = false;        
    }
    public void setHIRAM(){
        HIRAM = true;
    }
    public void clearHIRAM(){
        HIRAM = false;        
    }
    public void setLORAM(){
        LORAM = true;
    }
    public void clearLORAM(){
        LORAM = false;        
    }
    
    public void setProzessorport() {
        //addrProzessorPortReg
    }
    
}
