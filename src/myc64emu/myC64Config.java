/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */

package myc64emu;

/**
 *
 * @author betzan8u
 */
public class myC64Config {
    // Speicherbelegung https://www.c64-wiki.de/wiki/Speicherbelegungsplan
    public static int addrZeroPageStart                 = 0x0000;
    public static int addrZeroPageEnde                  = 0x00FF;
    public static int addrProzessorStapelStart          = 0x0100;
    public static int addrProzessorStapelEnde           = 0x01FF;
    public static int addrSystemVarBasicStart           = 0x0200;
    public static int addrSystemVarBasicEnde            = 0x03FF;
    public static int addrBildschirmSpeicherStart       = 0x0400;
    public static int addrBildschirmSpeicherEnde        = 0x07FF;
    public static int addrBasicRamStart                 = 0x0800;
    public static int addrBasicRamEnde                  = 0x09FF;   
    public static int addrSteckmodulStart               = 0x8000;
    public static int addrSteckmodulEnde                = 0x9FFF;
    public static int addrBasicRomStart                 = 0xA000;
    public static int addrBasicRomEnde                  = 0xBFFF;
    public static int addrFreeRamStart                  = 0xC000;
    public static int addrFreeRamStop                   = 0xCFFF;    
    public static int addrCharacterRomStart             = 0xD000;  
    public static int addrCharacterRomStop              = 0xDFFF;
    public static int addrVicRegisterStart              = 0xD000;  
    public static int addrVicRegisterEnde               = 0xD02E;  
    public static int addrSidRegisterStart              = 0xD400;  
    public static int addrSidRegisterEnde               = 0xD7FF;  
    public static int addrFarbSpeicherStart             = 0xD800;  
    public static int addrFarbSpeicherEnde              = 0xDBFF;  
    public static int addrCiaRegisterStart              = 0xDC00;  
    public static int addrCiaRegisterEnde               = 0xDDFF;  
    public static int addrErweiterungenStart            = 0xDE00;  
    public static int addrErweiterungenEnde             = 0xDFFF;      
    public static int addrKernalRomStart                = 0xE000;
    public static int addrKernalRomEnde                 = 0xFFFF;
    
    public static int addrNMIVector             = 0xfffa;
}
