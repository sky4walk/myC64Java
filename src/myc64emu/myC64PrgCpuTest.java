/*
 * Under Beerlicense
 * Andre Betz mail@Andrebetz.de  * 
 */
package myc64emu;

/**
 *
 * @author andre
 */
public class myC64PrgCpuTest {
    /**
     * CPU Test program from
     * https://github.com/Klaus2m5/6502_65C02_functional_tests
     * https://github.com/Klaus2m5/6502_65C02_functional_tests/blob/master/bin_files/6502_functional_test.lst
     * https://github.com/Klaus2m5/6502_65C02_functional_tests/blob/master/bin_files/6502_functional_test.bin      
     * http://visual6502.org/wiki/index.php?title=6502TestPrograms
     * 
     * AS65 Assembler for R6502 [1.42].  Copyright 1994-2007, Frank A. Kingswood                                                Page    1
        ---------------------------------------------------- 6502_functional_test.a65 ----------------------------------------------------

        6010 lines read, no errors in pass 1.
                                ;
                                ; 6 5 0 2   F U N C T I O N A L   T E S T
                                ;
                                ; Copyright (C) 2012-2015  Klaus Dormann
                                ;
                                ; This program is free software: you can redistribute it and/or modify
                                ; it under the terms of the GNU General Public License as published by
                                ; the Free Software Foundation, either version 3 of the License, or
                                ; (at your option) any later version.
                                ;
                                ; This program is distributed in the hope that it will be useful,
                                ; but WITHOUT ANY WARRANTY; without even the implied warranty of
                                ; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
                                ; GNU General Public License for more details.
                                ;
                                ; You should have received a copy of the GNU General Public License
                                ; along with this program.  If not, see <http://www.gnu.org/licenses/>.


                                ; This program is designed to test all opcodes of a 6502 emulator using all
                                ; addressing modes with focus on propper setting of the processor status
                                ; register bits.
                                ; 
                                ; version 21-oct-2015
                                ; contact info at http://2m5.de or email K@2m5.de
                                ;
                                ; assembled with AS65 from http://www.kingswood-consulting.co.uk/assemblers/
                                ; command line switches: -l -m -s2 -w -h0
                                ;                         |  |  |   |  no page headers in listing
                                ;                         |  |  |   wide listing (133 char/col)
                                ;                         |  |  write intel hex file instead of binary
                                ;                         |  expand macros in listing
                                ;                         generate pass2 listing
                                ;
                                ; No IO - should be run from a monitor with access to registers.
                                ; To run load intel hex image with a load command, than alter PC to 400 hex
                                ; (code_segment) and enter a go command.
                                ; Loop on program counter determines error or successful completion of test.
                                ; Check listing for relevant traps (jump/branch *).
                                ; Please note that in early tests some instructions will have to be used before
                                ; they are actually tested!
                                ;
                                ; RESET, NMI or IRQ should not occur and will be trapped if vectors are enabled.
                                ; Tests documented behavior of the original NMOS 6502 only! No unofficial
                                ; opcodes. Additional opcodes of newer versions of the CPU (65C02, 65816) will
                                ; not be tested. Decimal ops will only be tested with valid BCD operands and
                                ; N V Z flags will be ignored.
                                ;
                                ; Debugging hints:
                                ;     Most of the code is written sequentially. if you hit a trap, check the
                                ;   immediately preceeding code for the instruction to be tested. Results are
                                ;   tested first, flags are checked second by pushing them onto the stack and
                                ;   pulling them to the accumulator after the result was checked. The "real"
                                ;   flags are no longer valid for the tested instruction at this time!
                                ;     If the tested instruction was indexed, the relevant index (X or Y) must
                                ;   also be checked. Opposed to the flags, X and Y registers are still valid.
                                ;
                                ; versions:
                                ;   28-jul-2012  1st version distributed for testing
                                ;   29-jul-2012  fixed references to location 0, now #0
                                ;                added license - GPLv3
                                ;   30-jul-2012  added configuration options
                                ;   01-aug-2012  added trap macro to allow user to change error handling
                                ;   01-dec-2012  fixed trap in branch field must be a branch
                                ;   02-mar-2013  fixed PLA flags not tested
                                ;   19-jul-2013  allowed ROM vectors to be loaded when load_data_direct = 0
                                ;                added test sequence check to detect if tests jump their fence
                                ;   23-jul-2013  added RAM integrity check option
                                ;   16-aug-2013  added error report to standard output option
                                ;   13-dec-2014  added binary/decimal opcode table switch test
                                ;   14-dec-2014  improved relative address test
                                ;   23-aug-2015  added option to disable self modifying tests
                                ;   24-aug-2015  all self modifying immediate opcodes now execute in data RAM
                                ;                added small branch offset pretest
                                ;   21-oct-2015  added option to disable decimal mode ADC & SBC tests


                                ; C O N F I G U R A T I O N

                                ;ROM_vectors writable (0=no, 1=yes)
                                ;if ROM vectors can not be used interrupts will not be trapped
                                ;as a consequence BRK can not be tested but will be emulated to test RTI
        0001 =                  ROM_vectors = 1

                                ;load_data_direct (0=move from code segment, 1=load directly)
                                ;loading directly is preferred but may not be supported by your platform
                                ;0 produces only consecutive object code, 1 is not suitable for a binary image
        0000 =                  load_data_direct = 0

                                ;I_flag behavior (0=force enabled, 1=force disabled, 2=prohibit change, 3=allow
                                ;change) 2 requires extra code and is not recommended. SEI & CLI can only be
                                ;tested if you allow changing the interrupt status (I_flag = 3)
        0003 =                  I_flag = 3

                                ;configure memory - try to stay away from memory used by the system
                                ;zero_page memory start address, $50 (80) consecutive Bytes required
                                ;                                add 2 if I_flag = 2
        000a =                  zero_page = $a  

                                ;data_segment memory start address, $6A (106) consecutive Bytes required
        0200 =                  data_segment = $200  
                                    if (data_segment & $ff) != 0
                                        ERROR ERROR ERROR low byte of data_segment MUST be $00 !!
                                    endif  

                                ;code_segment memory start address, 13kB of consecutive space required
                                ;                                   add 2.5 kB if I_flag = 2
        0400 =                  code_segment = $400  

                                ;self modifying code may be disabled to allow running in ROM
                                ;0=part of the code is self modifying and must reside in RAM
                                ;1=tests disabled: branch range
        0000 =                  disable_selfmod = 0

                                ;report errors through I/O channel (0=use standard self trap loops, 1=include
                                ;report.i65 as I/O channel, add 3.5 kB)
        0000 =                  report = 0

                                ;RAM integrity test option. Checks for undesired RAM writes.
                                ;set lowest non RAM or RAM mirror address page (-1=disable, 0=64k, $40=16k)
                                ;leave disabled if a monitor, OS or background interrupt is allowed to alter RAM
        ffff =                  ram_top = -1

                                ;disable test decimal mode ADC & SBC, 0=enable, 1=disable,
                                ;2=disable including decimal flag in processor status
        0000 =                  disable_decimal = 0

                                        noopt       ;do not take shortcuts

                                ;macros for error & success traps to allow user modification
                                ;example:
                                ;trap    macro
                                ;        jsr my_error_handler
                                ;        endm
                                ;trap_eq macro
                                ;        bne skip\?
                                ;        trap           ;failed equal (zero)
                                ;skip\?
                                ;        endm
                                ;
                                ; my_error_handler should pop the calling address from the stack and report it.
                                ; putting larger portions of code (more than 3 bytes) inside the trap macro
                                ; may lead to branch range problems for some tests.
                                    if report = 0
                                trap    macro
                                        jmp *           ;failed anyway
                                        endm
                                trap_eq macro
                                        beq *           ;failed equal (zero)
                                        endm
                                trap_ne macro
                                        bne *           ;failed not equal (non zero)
                                        endm
                                trap_cs macro
                                        bcs *           ;failed carry set
                                        endm
                                trap_cc macro
                                        bcc *           ;failed carry clear
                                        endm
                                trap_mi macro
                                        bmi *           ;failed minus (bit 7 set)
                                        endm
                                trap_pl macro
                                        bpl *           ;failed plus (bit 7 clear)
                                        endm
                                trap_vs macro
                                        bvs *           ;failed overflow set
                                        endm
                                trap_vc macro
                                        bvc *           ;failed overflow clear
                                        endm
                                ; please observe that during the test the stack gets invalidated
                                ; therefore a RTS inside the success macro is not possible
                                success macro
                                        jmp *           ;test passed, no errors
                                        endm
                                    endif
                                    if report = 1
                                trap    macro
                                        jsr report_error
                                        endm
                                trap_eq macro
                                        bne skip\?
                                        trap           ;failed equal (zero)
                                skip\?
                                        endm
                                trap_ne macro
                                        beq skip\?
                                        trap            ;failed not equal (non zero)
                                skip\?
                                        endm
                                trap_cs macro
                                        bcc skip\?
                                        trap            ;failed carry set
                                skip\?
                                        endm
                                trap_cc macro
                                        bcs skip\?
                                        trap            ;failed carry clear
                                skip\?
                                        endm
                                trap_mi macro
                                        bpl skip\?
                                        trap            ;failed minus (bit 7 set)
                                skip\?
                                        endm
                                trap_pl macro
                                        bmi skip\?
                                        trap            ;failed plus (bit 7 clear)
                                skip\?
                                        endm
                                trap_vs macro
                                        bvc skip\?
                                        trap            ;failed overflow set
                                skip\?
                                        endm
                                trap_vc macro
                                        bvs skip\?
                                        trap            ;failed overflow clear
                                skip\?
                                        endm
                                ; please observe that during the test the stack gets invalidated
                                ; therefore a RTS inside the success macro is not possible
                                success macro
                                        jsr report_success
                                        endm
                                    endif


        0001 =                  carry   equ %00000001   ;flag bits in status
        0002 =                  zero    equ %00000010
        0004 =                  intdis  equ %00000100
        0008 =                  decmode equ %00001000
        0010 =                  break   equ %00010000
        0020 =                  reserv  equ %00100000
        0040 =                  overfl  equ %01000000
        0080 =                  minus   equ %10000000

        0001 =                  fc      equ carry
        0002 =                  fz      equ zero
        0003 =                  fzc     equ carry+zero
        0040 =                  fv      equ overfl
        0042 =                  fvz     equ overfl+zero
        0080 =                  fn      equ minus
        0081 =                  fnc     equ minus+carry
        0082 =                  fnz     equ minus+zero
        0083 =                  fnzc    equ minus+zero+carry
        00c0 =                  fnv     equ minus+overfl

        0030 =                  fao     equ break+reserv    ;bits always on after PHP, BRK
        0034 =                  fai     equ fao+intdis      ;+ forced interrupt disable
        0038 =                  faod    equ fao+decmode     ;+ ignore decimal
        003c =                  faid    equ fai+decmode     ;+ ignore decimal
        00ff =                  m8      equ $ff             ;8 bit mask
        00fb =                  m8i     equ $ff&~intdis     ;8 bit mask - interrupt disable

                                ;macros to allow masking of status bits.
                                ;masking test of decimal bit
                                ;masking of interrupt enable/disable on load and compare
                                ;masking of always on bits after PHP or BRK (unused & break) on compare
                                    if disable_decimal < 2
                                        if I_flag = 0
                                load_flag   macro
                                            lda #\1&m8i         ;force enable interrupts (mask I)
                                            endm
                                cmp_flag    macro
                                            cmp #(\1|fao)&m8i   ;I_flag is always enabled + always on bits
                                            endm
                                eor_flag    macro
                                            eor #(\1&m8i|fao)   ;mask I, invert expected flags + always on bits
                                            endm
                                        endif
                                        if I_flag = 1
                                load_flag   macro
                                            lda #\1|intdis      ;force disable interrupts
                                            endm
                                cmp_flag    macro
                                            cmp #(\1|fai)&m8    ;I_flag is always disabled + always on bits
                                            endm
                                eor_flag    macro
                                            eor #(\1|fai)       ;invert expected flags + always on bits + I
                                            endm
                                        endif
                                        if I_flag = 2
                                load_flag   macro
                                            lda #\1
                                            ora flag_I_on       ;restore I-flag
                                            and flag_I_off
                                            endm
                                cmp_flag    macro
                                            eor flag_I_on       ;I_flag is never changed
                                            cmp #(\1|fao)&m8i   ;expected flags + always on bits, mask I
                                            endm
                                eor_flag    macro
                                            eor flag_I_on       ;I_flag is never changed
                                            eor #(\1&m8i|fao)   ;mask I, invert expected flags + always on bits
                                            endm
                                        endif
                                        if I_flag = 3
                                load_flag   macro
                                            lda #\1             ;allow test to change I-flag (no mask)
                                            endm
                                cmp_flag    macro
                                            cmp #(\1|fao)&m8    ;expected flags + always on bits
                                            endm
                                eor_flag    macro
                                            eor #\1|fao         ;invert expected flags + always on bits
                                            endm
                                        endif
                                    else
                                        if I_flag = 0
                                load_flag   macro
                                            lda #\1&m8i         ;force enable interrupts (mask I)
                                            endm
                                cmp_flag    macro
                                            ora #decmode        ;ignore decimal mode bit
                                            cmp #(\1|faod)&m8i  ;I_flag is always enabled + always on bits
                                            endm
                                eor_flag    macro
                                            ora #decmode        ;ignore decimal mode bit
                                            eor #(\1&m8i|faod)  ;mask I, invert expected flags + always on bits
                                            endm
                                        endif
                                        if I_flag = 1
                                load_flag   macro
                                            lda #\1|intdis      ;force disable interrupts
                                            endm
                                cmp_flag    macro
                                            ora #decmode        ;ignore decimal mode bit
                                            cmp #(\1|faid)&m8   ;I_flag is always disabled + always on bits
                                            endm
                                eor_flag    macro
                                            ora #decmode        ;ignore decimal mode bit
                                            eor #(\1|faid)      ;invert expected flags + always on bits + I
                                            endm
                                        endif
                                        if I_flag = 2
                                load_flag   macro
                                            lda #\1
                                            ora flag_I_on       ;restore I-flag
                                            and flag_I_off
                                            endm
                                cmp_flag    macro
                                            eor flag_I_on       ;I_flag is never changed
                                            ora #decmode        ;ignore decimal mode bit
                                            cmp #(\1|faod)&m8i  ;expected flags + always on bits, mask I
                                            endm
                                eor_flag    macro
                                            eor flag_I_on       ;I_flag is never changed
                                            ora #decmode        ;ignore decimal mode bit
                                            eor #(\1&m8i|faod)  ;mask I, invert expected flags + always on bits
                                            endm
                                        endif
                                        if I_flag = 3
                                load_flag   macro
                                            lda #\1             ;allow test to change I-flag (no mask)
                                            endm
                                cmp_flag    macro
                                            ora #decmode        ;ignore decimal mode bit
                                            cmp #(\1|faod)&m8   ;expected flags + always on bits
                                            endm
                                eor_flag    macro
                                            ora #decmode        ;ignore decimal mode bit
                                            eor #\1|faod        ;invert expected flags + always on bits
                                            endm
                                        endif
                                    endif

                                ;macros to set (register|memory|zeropage) & status
                                set_stat    macro       ;setting flags in the processor status register
                                            load_flag \1
                                            pha         ;use stack to load status
                                            plp
                                            endm

                                set_a       macro       ;precharging accu & status
                                            load_flag \2
                                            pha         ;use stack to load status
                                            lda #\1     ;precharge accu
                                            plp
                                            endm

                                set_x       macro       ;precharging index & status
                                            load_flag \2
                                            pha         ;use stack to load status
                                            ldx #\1     ;precharge index x
                                            plp
                                            endm

                                set_y       macro       ;precharging index & status
                                            load_flag \2
                                            pha         ;use stack to load status
                                            ldy #\1     ;precharge index y
                                            plp
                                            endm

                                set_ax      macro       ;precharging indexed accu & immediate status
                                            load_flag \2
                                            pha         ;use stack to load status
                                            lda \1,x    ;precharge accu
                                            plp
                                            endm

                                set_ay      macro       ;precharging indexed accu & immediate status
                                            load_flag \2
                                            pha         ;use stack to load status
                                            lda \1,y    ;precharge accu
                                            plp
                                            endm

                                set_z       macro       ;precharging indexed zp & immediate status
                                            load_flag \2
                                            pha         ;use stack to load status
                                            lda \1,x    ;load to zeropage
                                            sta zpt
                                            plp
                                            endm

                                set_zx      macro       ;precharging zp,x & immediate status
                                            load_flag \2
                                            pha         ;use stack to load status
                                            lda \1,x    ;load to indexed zeropage
                                            sta zpt,x
                                            plp
                                            endm

                                set_abs     macro       ;precharging indexed memory & immediate status
                                            load_flag \2
                                            pha         ;use stack to load status
                                            lda \1,x    ;load to memory
                                            sta abst
                                            plp
                                            endm

                                set_absx    macro       ;precharging abs,x & immediate status
                                            load_flag \2
                                            pha         ;use stack to load status
                                            lda \1,x    ;load to indexed memory
                                            sta abst,x
                                            plp
                                            endm

                                ;macros to test (register|memory|zeropage) & status & (mask)
                                tst_stat    macro       ;testing flags in the processor status register
                                            php         ;save status
                                            pla         ;use stack to retrieve status
                                            pha
                                            cmp_flag \1
                                            trap_ne
                                            plp         ;restore status
                                            endm

                                tst_a       macro       ;testing result in accu & flags
                                            php         ;save flags
                                            cmp #\1     ;test result
                                            trap_ne
                                            pla         ;load status
                                            pha
                                            cmp_flag \2
                                            trap_ne
                                            plp         ;restore status
                                            endm

                                tst_x       macro       ;testing result in x index & flags
                                            php         ;save flags
                                            cpx #\1     ;test result
                                            trap_ne
                                            pla         ;load status
                                            pha
                                            cmp_flag \2
                                            trap_ne
                                            plp         ;restore status
                                            endm

                                tst_y       macro       ;testing result in y index & flags
                                            php         ;save flags
                                            cpy #\1     ;test result
                                            trap_ne
                                            pla         ;load status
                                            pha
                                            cmp_flag \2
                                            trap_ne
                                            plp         ;restore status
                                            endm

                                tst_ax      macro       ;indexed testing result in accu & flags
                                            php         ;save flags
                                            cmp \1,x    ;test result
                                            trap_ne
                                            pla         ;load status
                                            eor_flag \3
                                            cmp \2,x    ;test flags
                                            trap_ne     ;
                                            endm

                                tst_ay      macro       ;indexed testing result in accu & flags
                                            php         ;save flags
                                            cmp \1,y    ;test result
                                            trap_ne     ;
                                            pla         ;load status
                                            eor_flag \3
                                            cmp \2,y    ;test flags
                                            trap_ne
                                            endm

                                tst_z       macro       ;indexed testing result in zp & flags
                                            php         ;save flags
                                            lda zpt
                                            cmp \1,x    ;test result
                                            trap_ne
                                            pla         ;load status
                                            eor_flag \3
                                            cmp \2,x    ;test flags
                                            trap_ne
                                            endm

                                tst_zx      macro       ;testing result in zp,x & flags
                                            php         ;save flags
                                            lda zpt,x
                                            cmp \1,x    ;test result
                                            trap_ne
                                            pla         ;load status
                                            eor_flag \3
                                            cmp \2,x    ;test flags
                                            trap_ne
                                            endm

                                tst_abs     macro       ;indexed testing result in memory & flags
                                            php         ;save flags
                                            lda abst
                                            cmp \1,x    ;test result
                                            trap_ne
                                            pla         ;load status
                                            eor_flag \3
                                            cmp \2,x    ;test flags
                                            trap_ne
                                            endm

                                tst_absx    macro       ;testing result in abs,x & flags
                                            php         ;save flags
                                            lda abst,x
                                            cmp \1,x    ;test result
                                            trap_ne
                                            pla         ;load status
                                            eor_flag \3
                                            cmp \2,x    ;test flags
                                            trap_ne
                                            endm

                                ; RAM integrity test
                                ;   verifies that none of the previous tests has altered RAM outside of the
                                ;   designated write areas.
                                ;   uses zpt word as indirect pointer, zpt+2 word as checksum
                                        if ram_top > -1
                                check_ram   macro 
                                            cld
                                            lda #0
                                            sta zpt         ;set low byte of indirect pointer
                                            sta zpt+3       ;checksum high byte
                                          if disable_selfmod = 0
                                            sta range_adr   ;reset self modifying code
                                          endif
                                            clc
                                            ldx #zp_bss-zero_page ;zeropage - write test area
                                ccs3\?      adc zero_page,x
                                            bcc ccs2\?
                                            inc zpt+3       ;carry to high byte
                                            clc
                                ccs2\?      inx
                                            bne ccs3\?
                                            ldx #hi(abs1)   ;set high byte of indirect pointer
                                            stx zpt+1
                                            ldy #lo(abs1)   ;data after write & execute test area
                                ccs5\?      adc (zpt),y
                                            bcc ccs4\?
                                            inc zpt+3       ;carry to high byte
                                            clc
                                ccs4\?      iny
                                            bne ccs5\?
                                            inx             ;advance RAM high address
                                            stx zpt+1
                                            cpx #ram_top
                                            bne ccs5\?
                                            sta zpt+2       ;checksum low is
                                            cmp ram_chksm   ;checksum low expected
                                            trap_ne         ;checksum mismatch
                                            lda zpt+3       ;checksum high is
                                            cmp ram_chksm+1 ;checksum high expected
                                            trap_ne         ;checksum mismatch
                                            endm            
                                        else
                                check_ram   macro
                                            ;RAM check disabled - RAM size not set
                                            endm
                                        endif

                                next_test   macro           ;make sure, tests don't jump the fence
                                            lda test_case   ;previous test
                                            cmp #test_num
                                            trap_ne         ;test is out of sequence
                                test_num = test_num + 1
                                            lda #test_num   ;*** next tests' number
                                            sta test_case
                                            ;check_ram       ;uncomment to find altered RAM after each test
                                            endm

                                    if load_data_direct = 1
                                        data
                                    else
                                        bss                 ;uninitialized segment, copy of data at end of code!
                                    endif
        000a =                          org zero_page
                                ;break test interrupt save
        000a =                  irq_a   ds  1               ;a register
        000b =                  irq_x   ds  1               ;x register
                                    if I_flag = 2
                                ;masking for I bit in status
                                flag_I_on   ds  1           ;or mask to load flags   
                                flag_I_off  ds  1           ;and mask to load flags
                                    endif
        000c =                  zpt                         ;5 bytes store/modify test area
                                ;add/subtract operand generation and result/flag prediction
        000c =                  adfc    ds  1               ;carry flag before op
        000d =                  ad1     ds  1               ;operand 1 - accumulator
        000e =                  ad2     ds  1               ;operand 2 - memory / immediate
        000f =                  adrl    ds  1               ;expected result bits 0-7
        0010 =                  adrh    ds  1               ;expected result bit 8 (carry)
        0011 =                  adrf    ds  1               ;expected flags NV0000ZC (only binary mode)
        0012 =                  sb2     ds  1               ;operand 2 complemented for subtract
        0013 =                  zp_bss
        0013 =                  zp1     db  $c3,$82,$41,0   ;test patterns for LDx BIT ROL ROR ASL LSR
        0017 =                  zp7f    db  $7f             ;test pattern for compare  
                                ;logical zeropage operands
        0018 =                  zpOR    db  0,$1f,$71,$80   ;test pattern for OR
        001c =                  zpAN    db  $0f,$ff,$7f,$80 ;test pattern for AND
        0020 =                  zpEO    db  $ff,$0f,$8f,$8f ;test pattern for EOR
                                ;indirect addressing pointers
        0024 =                  ind1    dw  abs1            ;indirect pointer to pattern in absolute memory
        0026 =                          dw  abs1+1
        0028 =                          dw  abs1+2
        002a =                          dw  abs1+3
        002c =                          dw  abs7f
        002e =                  inw1    dw  abs1-$f8        ;indirect pointer for wrap-test pattern
        0030 =                  indt    dw  abst            ;indirect pointer to store area in absolute memory
        0032 =                          dw  abst+1
        0034 =                          dw  abst+2
        0036 =                          dw  abst+3
        0038 =                  inwt    dw  abst-$f8        ;indirect pointer for wrap-test store
        003a =                  indAN   dw  absAN           ;indirect pointer to AND pattern in absolute memory
        003c =                          dw  absAN+1
        003e =                          dw  absAN+2
        0040 =                          dw  absAN+3
        0042 =                  indEO   dw  absEO           ;indirect pointer to EOR pattern in absolute memory
        0044 =                          dw  absEO+1
        0046 =                          dw  absEO+2
        0048 =                          dw  absEO+3
        004a =                  indOR   dw  absOR           ;indirect pointer to OR pattern in absolute memory
        004c =                          dw  absOR+1
        004e =                          dw  absOR+2
        0050 =                          dw  absOR+3
                                ;add/subtract indirect pointers
        0052 =                  adi2    dw  ada2            ;indirect pointer to operand 2 in absolute memory
        0054 =                  sbi2    dw  sba2            ;indirect pointer to complemented operand 2 (SBC)
        0056 =                  adiy2   dw  ada2-$ff        ;with offset for indirect indexed
        0058 =                  sbiy2   dw  sba2-$ff
        005a =                  zp_bss_end

        0200 =                          org data_segment
        0200 =                  test_case   ds  1           ;current test number
        0201 =                  ram_chksm   ds  2           ;checksum for RAM integrity test
                                ;add/subtract operand copy - abs tests write area
        0203 =                  abst                        ;5 bytes store/modify test area
        0203 =                  ada2    ds  1               ;operand 2
        0204 =                  sba2    ds  1               ;operand 2 complemented for subtract
        0205 =                          ds  3               ;fill remaining bytes
        0208 =                  data_bss
                                    if load_data_direct = 1
                                ex_andi and #0              ;execute immediate opcodes
                                        rts
                                ex_eori eor #0              ;execute immediate opcodes
                                        rts
                                ex_orai ora #0              ;execute immediate opcodes
                                        rts
                                ex_adci adc #0              ;execute immediate opcodes
                                        rts
                                ex_sbci sbc #0              ;execute immediate opcodes
                                        rts
                                    else
        0208 =                  ex_andi ds  3
        020b =                  ex_eori ds  3
        020e =                  ex_orai ds  3
        0211 =                  ex_adci ds  3
        0214 =                  ex_sbci ds  3
                                    endif
        0217 =                  abs1    db  $c3,$82,$41,0   ;test patterns for LDx BIT ROL ROR ASL LSR
        021b =                  abs7f   db  $7f             ;test pattern for compare
                                ;loads
        021c =                  fLDx    db  fn,fn,0,fz      ;expected flags for load
                                ;shifts
        0220 =                  rASL                        ;expected result ASL & ROL -carry  
        0220 =                  rROL    db  $86,$04,$82,0   ; "
        0224 =                  rROLc   db  $87,$05,$83,1   ;expected result ROL +carry
        0228 =                  rLSR                        ;expected result LSR & ROR -carry
        0228 =                  rROR    db  $61,$41,$20,0   ; "
        022c =                  rRORc   db  $e1,$c1,$a0,$80 ;expected result ROR +carry
        0230 =                  fASL                        ;expected flags for shifts
        0230 =                  fROL    db  fnc,fc,fn,fz    ;no carry in
        0234 =                  fROLc   db  fnc,fc,fn,0     ;carry in
        0238 =                  fLSR
        0238 =                  fROR    db  fc,0,fc,fz      ;no carry in
        023c =                  fRORc   db  fnc,fn,fnc,fn   ;carry in
                                ;increments (decrements)
        0240 =                  rINC    db  $7f,$80,$ff,0,1 ;expected result for INC/DEC
        0245 =                  fINC    db  0,fn,fn,fz,0    ;expected flags for INC/DEC
                                ;logical memory operand
        024a =                  absOR   db  0,$1f,$71,$80   ;test pattern for OR
        024e =                  absAN   db  $0f,$ff,$7f,$80 ;test pattern for AND
        0252 =                  absEO   db  $ff,$0f,$8f,$8f ;test pattern for EOR
                                ;logical accu operand
        0256 =                  absORa  db  0,$f1,$1f,0     ;test pattern for OR
        025a =                  absANa  db  $f0,$ff,$ff,$ff ;test pattern for AND
        025e =                  absEOa  db  $ff,$f0,$f0,$0f ;test pattern for EOR
                                ;logical results
        0262 =                  absrlo  db  0,$ff,$7f,$80
        0266 =                  absflo  db  fz,fn,0,fn
        026a =                  data_bss_end


                                        code
        0400 =                          org code_segment
        0400 : d8               start   cld
        0401 : a2ff                     ldx #$ff
        0403 : 9a                       txs
        0404 : a900                     lda #0          ;*** test 0 = initialize
        0406 : 8d0002                   sta test_case
        0000 =                  test_num = 0

                                ;stop interrupts before initializing BSS
                                    if I_flag = 1
                                        sei
                                    endif

                                ;initialize I/O for report channel
                                    if report = 1
                                        jsr report_init
                                    endif

                                ;pretest small branch offset
        0409 : a205                     ldx #5
        040b : 4c3304                   jmp psb_test
        040e :                  psb_bwok
        040e : a005                     ldy #5
        0410 : d008                     bne psb_forw
                                        trap        ;branch should be taken
        0412 : 4c1204          >        jmp *           ;failed anyway

        0415 : 88                       dey         ;forward landing zone
        0416 : 88                       dey
        0417 : 88                       dey
        0418 : 88                       dey
        0419 : 88                       dey
        041a :                  psb_forw
        041a : 88                       dey
        041b : 88                       dey
        041c : 88                       dey
        041d : 88                       dey
        041e : 88                       dey
        041f : f017                     beq psb_fwok
                                        trap        ;forward offset
        0421 : 4c2104          >        jmp *           ;failed anyway


        0424 : ca                       dex         ;backward landing zone
        0425 : ca                       dex
        0426 : ca                       dex
        0427 : ca                       dex
        0428 : ca                       dex
        0429 :                  psb_back
        0429 : ca                       dex
        042a : ca                       dex
        042b : ca                       dex
        042c : ca                       dex
        042d : ca                       dex
        042e : f0de                     beq psb_bwok
                                        trap        ;backward offset
        0430 : 4c3004          >        jmp *           ;failed anyway

        0433 :                  psb_test
        0433 : d0f4                     bne psb_back
                                        trap        ;branch should be taken
        0435 : 4c3504          >        jmp *           ;failed anyway

        0438 :                  psb_fwok

                                ;initialize BSS segment
                                    if load_data_direct != 1
        0438 : a246                     ldx #zp_end-zp_init-1
        043a : bddc37           ld_zp   lda zp_init,x
        043d : 9513                     sta zp_bss,x
        043f : ca                       dex
        0440 : 10f8                     bpl ld_zp
        0442 : a261                     ldx #data_end-data_init-1
        0444 : bd2338           ld_data lda data_init,x
        0447 : 9d0802                   sta data_bss,x
        044a : ca                       dex
        044b : 10f7                     bpl ld_data
                                      if ROM_vectors = 1
        044d : a205                     ldx #5
        044f : bd8538           ld_vect lda vec_init,x
        0452 : 9dfaff                   sta vec_bss,x
        0455 : ca                       dex
        0456 : 10f7                     bpl ld_vect
                                      endif
                                    endif

                                ;retain status of interrupt flag
                                    if I_flag = 2
                                        php
                                        pla
                                        and #4          ;isolate flag
                                        sta flag_I_on   ;or mask
                                        eor #lo(~4)     ;reverse
                                        sta flag_I_off  ;and mask
                                    endif

                                ;generate checksum for RAM integrity test
                                    if ram_top > -1
                                        lda #0 
                                        sta zpt         ;set low byte of indirect pointer
                                        sta ram_chksm+1 ;checksum high byte
                                      if disable_selfmod = 0
                                        sta range_adr   ;reset self modifying code
                                      endif
                                        clc
                                        ldx #zp_bss-zero_page ;zeropage - write test area
                                gcs3    adc zero_page,x
                                        bcc gcs2
                                        inc ram_chksm+1 ;carry to high byte
                                        clc
                                gcs2    inx
                                        bne gcs3
                                        ldx #hi(abs1)   ;set high byte of indirect pointer
                                        stx zpt+1
                                        ldy #lo(abs1)   ;data after write & execute test area
                                gcs5    adc (zpt),y
                                        bcc gcs4
                                        inc ram_chksm+1 ;carry to high byte
                                        clc
                                gcs4    iny
                                        bne gcs5
                                        inx             ;advance RAM high address
                                        stx zpt+1
                                        cpx #ram_top
                                        bne gcs5
                                        sta ram_chksm   ;checksum complete
                                    endif
                                        next_test            
        0458 : ad0002          >            lda test_case   ;previous test
        045b : c900            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        045d : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0001 =                 >test_num = test_num + 1
        045f : a901            >            lda #test_num   ;*** next tests' number
        0461 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                    if disable_selfmod = 0
                                ;testing relative addressing with BEQ
        0464 : a0fe                     ldy #$fe        ;testing maximum range, not -1/-2 (invalid/self adr)
        0466 :                  range_loop
        0466 : 88                       dey             ;next relative address
        0467 : 98                       tya
        0468 : aa                       tax             ;precharge count to end of loop
        0469 : 1008                     bpl range_fw    ;calculate relative address
        046b : 18                       clc             ;avoid branch self or to relative address of branch
        046c : 6902                     adc #2
        046e : ea                       nop             ;offset landing zone - tolerate +/-5 offset to branch
        046f : ea                       nop
        0470 : ea                       nop
        0471 : ea                       nop
        0472 : ea                       nop
        0473 :                  range_fw
        0473 : ea                       nop
        0474 : ea                       nop
        0475 : ea                       nop
        0476 : ea                       nop
        0477 : ea                       nop
        0478 : 497f                     eor #$7f        ;complement except sign
        047a : 8d0605                   sta range_adr   ;load into test target
        047d : a900                     lda #0          ;should set zero flag in status register
        047f : 4c0505                   jmp range_op

        0482 : ca                       dex             ; offset landing zone - backward branch too far
        0483 : ca                       dex
        0484 : ca                       dex
        0485 : ca                       dex
        0486 : ca                       dex
                                        ;relative address target field with branch under test in the middle
        0487 : ca                       dex             ;-128 - max backward
        0488 : ca                       dex
        0489 : ca                       dex
        048a : ca                       dex
        048b : ca                       dex
        048c : ca                       dex
        048d : ca                       dex
        048e : ca                       dex
        048f : ca                       dex             ;-120
        0490 : ca                       dex
        0491 : ca                       dex
        0492 : ca                       dex
        0493 : ca                       dex
        0494 : ca                       dex
        0495 : ca                       dex
        0496 : ca                       dex
        0497 : ca                       dex
        0498 : ca                       dex
        0499 : ca                       dex             ;-110
        049a : ca                       dex
        049b : ca                       dex
        049c : ca                       dex
        049d : ca                       dex
        049e : ca                       dex
        049f : ca                       dex
        04a0 : ca                       dex
        04a1 : ca                       dex
        04a2 : ca                       dex
        04a3 : ca                       dex             ;-100
        04a4 : ca                       dex
        04a5 : ca                       dex
        04a6 : ca                       dex
        04a7 : ca                       dex
        04a8 : ca                       dex
        04a9 : ca                       dex
        04aa : ca                       dex
        04ab : ca                       dex
        04ac : ca                       dex
        04ad : ca                       dex             ;-90
        04ae : ca                       dex
        04af : ca                       dex
        04b0 : ca                       dex
        04b1 : ca                       dex
        04b2 : ca                       dex
        04b3 : ca                       dex
        04b4 : ca                       dex
        04b5 : ca                       dex
        04b6 : ca                       dex
        04b7 : ca                       dex             ;-80
        04b8 : ca                       dex
        04b9 : ca                       dex
        04ba : ca                       dex
        04bb : ca                       dex
        04bc : ca                       dex
        04bd : ca                       dex
        04be : ca                       dex
        04bf : ca                       dex
        04c0 : ca                       dex
        04c1 : ca                       dex             ;-70
        04c2 : ca                       dex
        04c3 : ca                       dex
        04c4 : ca                       dex
        04c5 : ca                       dex
        04c6 : ca                       dex
        04c7 : ca                       dex
        04c8 : ca                       dex
        04c9 : ca                       dex
        04ca : ca                       dex
        04cb : ca                       dex             ;-60
        04cc : ca                       dex
        04cd : ca                       dex
        04ce : ca                       dex
        04cf : ca                       dex
        04d0 : ca                       dex
        04d1 : ca                       dex
        04d2 : ca                       dex
        04d3 : ca                       dex
        04d4 : ca                       dex
        04d5 : ca                       dex             ;-50
        04d6 : ca                       dex
        04d7 : ca                       dex
        04d8 : ca                       dex
        04d9 : ca                       dex
        04da : ca                       dex
        04db : ca                       dex
        04dc : ca                       dex
        04dd : ca                       dex
        04de : ca                       dex
        04df : ca                       dex             ;-40
        04e0 : ca                       dex
        04e1 : ca                       dex
        04e2 : ca                       dex
        04e3 : ca                       dex
        04e4 : ca                       dex
        04e5 : ca                       dex
        04e6 : ca                       dex
        04e7 : ca                       dex
        04e8 : ca                       dex
        04e9 : ca                       dex             ;-30
        04ea : ca                       dex
        04eb : ca                       dex
        04ec : ca                       dex
        04ed : ca                       dex
        04ee : ca                       dex
        04ef : ca                       dex
        04f0 : ca                       dex
        04f1 : ca                       dex
        04f2 : ca                       dex
        04f3 : ca                       dex             ;-20
        04f4 : ca                       dex
        04f5 : ca                       dex
        04f6 : ca                       dex
        04f7 : ca                       dex
        04f8 : ca                       dex
        04f9 : ca                       dex
        04fa : ca                       dex
        04fb : ca                       dex
        04fc : ca                       dex
        04fd : ca                       dex             ;-10
        04fe : ca                       dex
        04ff : ca                       dex
        0500 : ca                       dex
        0501 : ca                       dex
        0502 : ca                       dex
        0503 : ca                       dex
        0504 : ca                       dex             ;-3
        0505 :                  range_op                ;test target with zero flag=0, z=1 if previous dex
        0506 =                  range_adr   = *+1       ;modifiable relative address
        0505 : f03e                     beq *+64        ;+64 if called without modification
        0507 : ca                       dex             ;+0
        0508 : ca                       dex
        0509 : ca                       dex
        050a : ca                       dex
        050b : ca                       dex
        050c : ca                       dex
        050d : ca                       dex
        050e : ca                       dex
        050f : ca                       dex
        0510 : ca                       dex
        0511 : ca                       dex             ;+10
        0512 : ca                       dex
        0513 : ca                       dex
        0514 : ca                       dex
        0515 : ca                       dex
        0516 : ca                       dex
        0517 : ca                       dex
        0518 : ca                       dex
        0519 : ca                       dex
        051a : ca                       dex
        051b : ca                       dex             ;+20
        051c : ca                       dex
        051d : ca                       dex
        051e : ca                       dex
        051f : ca                       dex
        0520 : ca                       dex
        0521 : ca                       dex
        0522 : ca                       dex
        0523 : ca                       dex
        0524 : ca                       dex
        0525 : ca                       dex             ;+30
        0526 : ca                       dex
        0527 : ca                       dex
        0528 : ca                       dex
        0529 : ca                       dex
        052a : ca                       dex
        052b : ca                       dex
        052c : ca                       dex
        052d : ca                       dex
        052e : ca                       dex
        052f : ca                       dex             ;+40
        0530 : ca                       dex
        0531 : ca                       dex
        0532 : ca                       dex
        0533 : ca                       dex
        0534 : ca                       dex
        0535 : ca                       dex
        0536 : ca                       dex
        0537 : ca                       dex
        0538 : ca                       dex
        0539 : ca                       dex             ;+50
        053a : ca                       dex
        053b : ca                       dex
        053c : ca                       dex
        053d : ca                       dex
        053e : ca                       dex
        053f : ca                       dex
        0540 : ca                       dex
        0541 : ca                       dex
        0542 : ca                       dex
        0543 : ca                       dex             ;+60
        0544 : ca                       dex
        0545 : ca                       dex
        0546 : ca                       dex
        0547 : ca                       dex
        0548 : ca                       dex
        0549 : ca                       dex
        054a : ca                       dex
        054b : ca                       dex
        054c : ca                       dex
        054d : ca                       dex             ;+70
        054e : ca                       dex
        054f : ca                       dex
        0550 : ca                       dex
        0551 : ca                       dex
        0552 : ca                       dex
        0553 : ca                       dex
        0554 : ca                       dex
        0555 : ca                       dex
        0556 : ca                       dex
        0557 : ca                       dex             ;+80
        0558 : ca                       dex
        0559 : ca                       dex
        055a : ca                       dex
        055b : ca                       dex
        055c : ca                       dex
        055d : ca                       dex
        055e : ca                       dex
        055f : ca                       dex
        0560 : ca                       dex
        0561 : ca                       dex             ;+90
        0562 : ca                       dex
        0563 : ca                       dex
        0564 : ca                       dex
        0565 : ca                       dex
        0566 : ca                       dex
        0567 : ca                       dex
        0568 : ca                       dex
        0569 : ca                       dex
        056a : ca                       dex
        056b : ca                       dex             ;+100
        056c : ca                       dex
        056d : ca                       dex
        056e : ca                       dex
        056f : ca                       dex
        0570 : ca                       dex
        0571 : ca                       dex
        0572 : ca                       dex
        0573 : ca                       dex
        0574 : ca                       dex
        0575 : ca                       dex             ;+110
        0576 : ca                       dex
        0577 : ca                       dex
        0578 : ca                       dex
        0579 : ca                       dex
        057a : ca                       dex
        057b : ca                       dex
        057c : ca                       dex
        057d : ca                       dex
        057e : ca                       dex
        057f : ca                       dex             ;+120
        0580 : ca                       dex
        0581 : ca                       dex
        0582 : ca                       dex
        0583 : ca                       dex
        0584 : ca                       dex
        0585 : ca                       dex
        0586 : ea                       nop             ;offset landing zone - forward branch too far
        0587 : ea                       nop
        0588 : ea                       nop
        0589 : ea                       nop
        058a : ea                       nop
        058b : f008                     beq range_ok    ;+127 - max forward
                                        trap            ; bad range
        058d : 4c8d05          >        jmp *           ;failed anyway

        0590 : ea                       nop             ;offset landing zone - tolerate +/-5 offset to branch
        0591 : ea                       nop
        0592 : ea                       nop
        0593 : ea                       nop
        0594 : ea                       nop
        0595 :                  range_ok
        0595 : ea                       nop
        0596 : ea                       nop
        0597 : ea                       nop
        0598 : ea                       nop
        0599 : ea                       nop
        059a : c000                     cpy #0
        059c : f003                     beq range_end   
        059e : 4c6604                   jmp range_loop
        05a1 :                  range_end               ;range test successful
                                    endif
                                        next_test
        05a1 : ad0002          >            lda test_case   ;previous test
        05a4 : c901            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        05a6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0002 =                 >test_num = test_num + 1
        05a8 : a902            >            lda #test_num   ;*** next tests' number
        05aa : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ;partial test BNE & CMP, CPX, CPY immediate
        05ad : c001                     cpy #1          ;testing BNE true
        05af : d003                     bne test_bne
                                        trap 
        05b1 : 4cb105          >        jmp *           ;failed anyway

        05b4 :                  test_bne
        05b4 : a900                     lda #0 
        05b6 : c900                     cmp #0          ;test compare immediate 
                                        trap_ne
        05b8 : d0fe            >        bne *           ;failed not equal (non zero)

                                        trap_cc
        05ba : 90fe            >        bcc *           ;failed carry clear

                                        trap_mi
        05bc : 30fe            >        bmi *           ;failed minus (bit 7 set)

        05be : c901                     cmp #1
                                        trap_eq 
        05c0 : f0fe            >        beq *           ;failed equal (zero)

                                        trap_cs
        05c2 : b0fe            >        bcs *           ;failed carry set

                                        trap_pl
        05c4 : 10fe            >        bpl *           ;failed plus (bit 7 clear)

        05c6 : aa                       tax 
        05c7 : e000                     cpx #0          ;test compare x immediate
                                        trap_ne
        05c9 : d0fe            >        bne *           ;failed not equal (non zero)

                                        trap_cc
        05cb : 90fe            >        bcc *           ;failed carry clear

                                        trap_mi
        05cd : 30fe            >        bmi *           ;failed minus (bit 7 set)

        05cf : e001                     cpx #1
                                        trap_eq 
        05d1 : f0fe            >        beq *           ;failed equal (zero)

                                        trap_cs
        05d3 : b0fe            >        bcs *           ;failed carry set

                                        trap_pl
        05d5 : 10fe            >        bpl *           ;failed plus (bit 7 clear)

        05d7 : a8                       tay 
        05d8 : c000                     cpy #0          ;test compare y immediate
                                        trap_ne
        05da : d0fe            >        bne *           ;failed not equal (non zero)

                                        trap_cc
        05dc : 90fe            >        bcc *           ;failed carry clear

                                        trap_mi
        05de : 30fe            >        bmi *           ;failed minus (bit 7 set)

        05e0 : c001                     cpy #1
                                        trap_eq 
        05e2 : f0fe            >        beq *           ;failed equal (zero)

                                        trap_cs
        05e4 : b0fe            >        bcs *           ;failed carry set

                                        trap_pl
        05e6 : 10fe            >        bpl *           ;failed plus (bit 7 clear)

                                        next_test
        05e8 : ad0002          >            lda test_case   ;previous test
        05eb : c902            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        05ed : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0003 =                 >test_num = test_num + 1
        05ef : a903            >            lda #test_num   ;*** next tests' number
        05f1 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test

                                ;testing stack operations PHA PHP PLA PLP

        05f4 : a2ff                     ldx #$ff        ;initialize stack
        05f6 : 9a                       txs
        05f7 : a955                     lda #$55
        05f9 : 48                       pha
        05fa : a9aa                     lda #$aa
        05fc : 48                       pha
        05fd : cdfe01                   cmp $1fe        ;on stack ?
                                        trap_ne
        0600 : d0fe            >        bne *           ;failed not equal (non zero)

        0602 : ba                       tsx
        0603 : 8a                       txa             ;overwrite accu
        0604 : c9fd                     cmp #$fd        ;sp decremented?
                                        trap_ne
        0606 : d0fe            >        bne *           ;failed not equal (non zero)

        0608 : 68                       pla
        0609 : c9aa                     cmp #$aa        ;successful retreived from stack?
                                        trap_ne
        060b : d0fe            >        bne *           ;failed not equal (non zero)

        060d : 68                       pla
        060e : c955                     cmp #$55
                                        trap_ne
        0610 : d0fe            >        bne *           ;failed not equal (non zero)

        0612 : cdff01                   cmp $1ff        ;remains on stack?
                                        trap_ne
        0615 : d0fe            >        bne *           ;failed not equal (non zero)

        0617 : ba                       tsx
        0618 : e0ff                     cpx #$ff        ;sp incremented?
                                        trap_ne
        061a : d0fe            >        bne *           ;failed not equal (non zero)

                                        next_test
        061c : ad0002          >            lda test_case   ;previous test
        061f : c903            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        0621 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0004 =                 >test_num = test_num + 1
        0623 : a904            >            lda #test_num   ;*** next tests' number
        0625 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ;testing branch decisions BPL BMI BVC BVS BCC BCS BNE BEQ
                                        set_stat $ff    ;all on
                               >            load_flag $ff    
        0628 : a9ff            >            lda #$ff                 ;allow test to change I-flag (no mask)
                               >
        062a : 48              >            pha         ;use stack to load status
        062b : 28              >            pl09b8p

        062c : 101a                     bpl nbr1        ;branches should not be taken
        062e : 501b                     bvc nbr2
        0630 : 901c                     bcc nbr3
        0632 : d01d                     bne nbr4
        0634 : 3003                     bmi br1         ;branches should be taken
                                        trap 
        0636 : 4c3606          >        jmp *           ;failed anyway

        0639 : 7003             br1     bvs br2
                                        trap 
        063b : 4c3b06          >        jmp *           ;failed anyway

        063e : b003             br2     bcs br3
                                        trap 
        0640 : 4c4006          >        jmp *           ;failed anyway

        0643 : f00f             br3     beq br4
                                        trap 
        0645 : 4c4506          >        jmp *           ;failed anyway

        0648 :                  nbr1
                                        trap            ;previous bpl taken 
        0648 : 4c4806          >        jmp *           ;failed anyway

        064b :                  nbr2
                                        trap            ;previous bvc taken
        064b : 4c4b06          >        jmp *           ;failed anyway

        064e :                  nbr3
                                        trap            ;previous bcc taken
        064e : 4c4e06          >        jmp *           ;failed anyway

        0651 :                  nbr4
                                        trap            ;previous bne taken
        0651 : 4c5106          >        jmp *           ;failed anyway

        0654 : 08               br4     php
        0655 : ba                       tsx
        0656 : e0fe                     cpx #$fe        ;sp after php?
                                        trap_ne
        0658 : d0fe            >        bne *           ;failed not equal (non zero)

        065a : 68                       pla
                                        cmp_flag $ff    ;returned all flags on?
        065b : c9ff            >            cmp #($ff    |fao)&m8    ;expected flags + always on bits

                                        trap_ne
        065d : d0fe            >        bne *           ;failed not equal (non zero)

        065f : ba                       tsx
        0660 : e0ff                     cpx #$ff        ;sp after php?
                                        trap_ne
        0662 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0      ;all off
                               >            load_flag 0      
        0664 : a900            >            lda #0                   ;allow test to change I-flag (no mask)
                               >
        0666 : 48              >            pha         ;use stack to load status
        0667 : 28              >            plp

        0668 : 301a                     bmi nbr11       ;branches should not be taken
        066a : 701b                     bvs nbr12
        066c : b01c                     bcs nbr13
        066e : f01d                     beq nbr14
        0670 : 1003                     bpl br11        ;branches should be taken
                                        trap 
        0672 : 4c7206          >        jmp *           ;failed anyway

        0675 : 5003             br11    bvc br12
                                        trap 
        0677 : 4c7706          >        jmp *           ;failed anyway

        067a : 9003             br12    bcc br13
                                        trap 
        067c : 4c7c06          >        jmp *           ;failed anyway

        067f : d00f             br13    bne br14
                                        trap 
        0681 : 4c8106          >        jmp *           ;failed anyway

        0684 :                  nbr11
                                        trap            ;previous bmi taken 
        0684 : 4c8406          >        jmp *           ;failed anyway

        0687 :                  nbr12
                                        trap            ;previous bvs taken 
        0687 : 4c8706          >        jmp *           ;failed anyway

        068a :                  nbr13
                                        trap            ;previous bcs taken 
        068a : 4c8a06          >        jmp *           ;failed anyway

        068d :                  nbr14
                                        trap            ;previous beq taken 
        068d : 4c8d06          >        jmp *           ;failed anyway

        0690 : 08               br14    php
        0691 : 68                       pla
                                        cmp_flag 0      ;flags off except break (pushed by sw) + reserved?
        0692 : c930            >            cmp #(0      |fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0694 : d0fe            >        bne *           ;failed not equal (non zero)

                                        ;crosscheck flags
                                        set_stat zero
                               >            load_flag zero
        0696 : a902            >            lda #zero             ;allow test to change I-flag (no mask)
                               >
        0698 : 48              >            pha         ;use stack to load status
        0699 : 28              >            plp

        069a : d002                     bne brzs1
        069c : f003                     beq brzs2
        069e :                  brzs1
                                        trap            ;branch zero/non zero
        069e : 4c9e06          >        jmp *           ;failed anyway

        06a1 : b002             brzs2   bcs brzs3
        06a3 : 9003                     bcc brzs4
        06a5 :                  brzs3
                                        trap            ;branch carry/no carry
        06a5 : 4ca506          >        jmp *           ;failed anyway

        06a8 : 3002             brzs4   bmi brzs5
        06aa : 1003                     bpl brzs6
        06ac :                  brzs5
                                        trap            ;branch minus/plus
        06ac : 4cac06          >        jmp *           ;failed anyway

        06af : 7002             brzs6   bvs brzs7
        06b1 : 5003                     bvc brzs8
        06b3 :                  brzs7
                                        trap            ;branch overflow/no overflow
        06b3 : 4cb306          >        jmp *           ;failed anyway

        06b6 :                  brzs8
                                        set_stat carry
                               >            load_flag carry
        06b6 : a901            >            lda #carry             ;allow test to change I-flag (no mask)
                               >
        06b8 : 48              >            pha         ;use stack to load status
        06b9 : 28              >            plp

        06ba : f002                     beq brcs1
        06bc : d003                     bne brcs2
        06be :                  brcs1
                                        trap            ;branch zero/non zero
        06be : 4cbe06          >        jmp *           ;failed anyway

        06c1 : 9002             brcs2   bcc brcs3
        06c3 : b003                     bcs brcs4
        06c5 :                  brcs3
                                        trap            ;branch carry/no carry
        06c5 : 4cc506          >        jmp *           ;failed anyway

        06c8 : 3002             brcs4   bmi brcs5
        06ca : 1003                     bpl brcs6
        06cc :                  brcs5
                                        trap            ;branch minus/plus
        06cc : 4ccc06          >        jmp *           ;failed anyway

        06cf : 7002             brcs6   bvs brcs7
        06d1 : 5003                     bvc brcs8
        06d3 :                  brcs7
                                        trap            ;branch overflow/no overflow
        06d3 : 4cd306          >        jmp *           ;failed anyway


        06d6 :                  brcs8
                                        set_stat minus
                               >            load_flag minus
        06d6 : a980            >            lda #minus             ;allow test to change I-flag (no mask)
                               >
        06d8 : 48              >            pha         ;use stack to load status
        06d9 : 28              >            plp

        06da : f002                     beq brmi1
        06dc : d003                     bne brmi2
        06de :                  brmi1
                                        trap            ;branch zero/non zero
        06de : 4cde06          >        jmp *           ;failed anyway

        06e1 : b002             brmi2   bcs brmi3
        06e3 : 9003                     bcc brmi4
        06e5 :                  brmi3
                                        trap            ;branch carry/no carry
        06e5 : 4ce506          >        jmp *           ;failed anyway

        06e8 : 1002             brmi4   bpl brmi5
        06ea : 3003                     bmi brmi6
        06ec :                  brmi5
                                        trap            ;branch minus/plus
        06ec : 4cec06          >        jmp *           ;failed anyway

        06ef : 7002             brmi6   bvs brmi7
        06f1 : 5003                     bvc brmi8
        06f3 :                  brmi7
                                        trap            ;branch overflow/no overflow
        06f3 : 4cf306          >        jmp *           ;failed anyway

        06f6 :                  brmi8
                                        set_stat overfl
                               >            load_flag overfl
        06f6 : a940            >            lda #overfl             ;allow test to change I-flag (no mask)
                               >
        06f8 : 48              >            pha         ;use stack to load status
        06f9 : 28              >            plp

        06fa : f002                     beq brvs1
        06fc : d003                     bne brvs2
        06fe :                  brvs1
                                        trap            ;branch zero/non zero
        06fe : 4cfe06          >        jmp *           ;failed anyway

        0701 : b002             brvs2   bcs brvs3
        0703 : 9003                     bcc brvs4
        0705 :                  brvs3
                                        trap            ;branch carry/no carry
        0705 : 4c0507          >        jmp *           ;failed anyway

        0708 : 3002             brvs4   bmi brvs5
        070a : 1003                     bpl brvs6
        070c :                  brvs5
                                        trap            ;branch minus/plus
        070c : 4c0c07          >        jmp *           ;failed anyway

        070f : 5002             brvs6   bvc brvs7
        0711 : 7003                     bvs brvs8
        0713 :                  brvs7
                                        trap            ;branch overflow/no overflow
        0713 : 4c1307          >        jmp *           ;failed anyway

        0716 :                  brvs8
                                        set_stat $ff-zero
                               >            load_flag $ff-zero
        0716 : a9fd            >            lda #$ff-zero             ;allow test to change I-flag (no mask)
                               >
        0718 : 48              >            pha         ;use stack to load status
        0719 : 28              >            plp

        071a : f002                     beq brzc1
        071c : d003                     bne brzc2
        071e :                  brzc1
                                        trap            ;branch zero/non zero
        071e : 4c1e07          >        jmp *           ;failed anyway

        0721 : 9002             brzc2   bcc brzc3
        0723 : b003                     bcs brzc4
        0725 :                  brzc3
                                        trap            ;branch carry/no carry
        0725 : 4c2507          >        jmp *           ;failed anyway

        0728 : 1002             brzc4   bpl brzc5
        072a : 3003                     bmi brzc6
        072c :                  brzc5
                                        trap            ;branch minus/plus
        072c : 4c2c07          >        jmp *           ;failed anyway

        072f : 5002             brzc6   bvc brzc7
        0731 : 7003                     bvs brzc8
        0733 :                  brzc7
                                        trap            ;branch overflow/no overflow
        0733 : 4c3307          >        jmp *           ;failed anyway

        0736 :                  brzc8
                                        set_stat $ff-carry
                               >            load_flag $ff-carry
        0736 : a9fe            >            lda #$ff-carry             ;allow test to change I-flag (no mask)
                               >
        0738 : 48              >            pha         ;use stack to load status
        0739 : 28              >            plp

        073a : d002                     bne brcc1
        073c : f003                     beq brcc2
        073e :                  brcc1
                                        trap            ;branch zero/non zero
        073e : 4c3e07          >        jmp *           ;failed anyway

        0741 : b002             brcc2   bcs brcc3
        0743 : 9003                     bcc brcc4
        0745 :                  brcc3
                                        trap            ;branch carry/no carry
        0745 : 4c4507          >        jmp *           ;failed anyway

        0748 : 1002             brcc4   bpl brcc5
        074a : 3003                     bmi brcc6
        074c :                  brcc5
                                        trap            ;branch minus/plus
        074c : 4c4c07          >        jmp *           ;failed anyway

        074f : 5002             brcc6   bvc brcc7
        0751 : 7003                     bvs brcc8
        0753 :                  brcc7
                                        trap            ;branch overflow/no overflow
        0753 : 4c5307          >        jmp *           ;failed anyway

        0756 :                  brcc8
                                        set_stat $ff-minus
                               >            load_flag $ff-minus
        0756 : a97f            >            lda #$ff-minus             ;allow test to change I-flag (no mask)
                               >
        0758 : 48              >            pha         ;use stack to load status
        0759 : 28              >            plp

        075a : d002                     bne brpl1
        075c : f003                     beq brpl2
        075e :                  brpl1
                                        trap            ;branch zero/non zero
        075e : 4c5e07          >        jmp *           ;failed anyway

        0761 : 9002             brpl2   bcc brpl3
        0763 : b003                     bcs brpl4
        0765 :                  brpl3
                                        trap            ;branch carry/no carry
        0765 : 4c6507          >        jmp *           ;failed anyway

        0768 : 3002             brpl4   bmi brpl5
        076a : 1003                     bpl brpl6
        076c :                  brpl5
                                        trap            ;branch minus/plus
        076c : 4c6c07          >        jmp *           ;failed anyway

        076f : 5002             brpl6   bvc brpl7
        0771 : 7003                     bvs brpl8
        0773 :                  brpl7
                                        trap            ;branch overflow/no overflow
        0773 : 4c7307          >        jmp *           ;failed anyway

        0776 :                  brpl8
                                        set_stat $ff-overfl
                               >            load_flag $ff-overfl
        0776 : a9bf            >            lda #$ff-overfl             ;allow test to change I-flag (no mask)
                               >
        0778 : 48              >            pha         ;use stack to load status
        0779 : 28              >            plp

        077a : d002                     bne brvc1
        077c : f003                     beq brvc2
        077e :                  brvc1
                                        trap            ;branch zero/non zero
        077e : 4c7e07          >        jmp *           ;failed anyway

        0781 : 9002             brvc2   bcc brvc3
        0783 : b003                     bcs brvc4
        0785 :                  brvc3
                                        trap            ;branch carry/no carry
        0785 : 4c8507          >        jmp *           ;failed anyway

        0788 : 1002             brvc4   bpl brvc5
        078a : 3003                     bmi brvc6
        078c :                  brvc5
                                        trap            ;branch minus/plus
        078c : 4c8c07          >        jmp *           ;failed anyway

        078f : 7002             brvc6   bvs brvc7
        0791 : 5003                     bvc brvc8
        0793 :                  brvc7
                                        trap            ;branch overflow/no overflow
        0793 : 4c9307          >        jmp *           ;failed anyway

        0796 :                  brvc8
                                        next_test
        0796 : ad0002          >            lda test_case   ;previous test
        0799 : c904            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        079b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0005 =                 >test_num = test_num + 1
        079d : a905            >            lda #test_num   ;*** next tests' number
        079f : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; test PHA does not alter flags or accumulator but PLA does
        07a2 : a255                     ldx #$55        ;x & y protected
        07a4 : a0aa                     ldy #$aa
                                        set_a 1,$ff     ;push
                               >            load_flag $ff     
        07a6 : a9ff            >            lda #$ff                  ;allow test to change I-flag (no mask)
                               >
        07a8 : 48              >            pha         ;use stack to load status
        07a9 : a901            >            lda #1     ;precharge accu
        07ab : 28              >            plp

        07ac : 48                       pha
                                        tst_a 1,$ff
        07ad : 08              >            php         ;save flags
        07ae : c901            >            cmp #1     ;test result
                               >            trap_ne
        07b0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        07b2 : 68              >            pla         ;load status
        07b3 : 48              >            pha
                               >            cmp_flag $ff
        07b4 : c9ff            >            cmp #($ff|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        07b6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        07b8 : 28              >            plp         ;restore status

                                        set_a 0,0
                               >            load_flag 0
        07b9 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        07bb : 48              >            pha         ;use stack to load status
        07bc : a900            >            lda #0     ;precharge accu
        07be : 28              >            plp

        07bf : 48                       pha
                                        tst_a 0,0
        07c0 : 08              >            php         ;save flags
        07c1 : c900            >            cmp #0     ;test result
                               >            trap_ne
        07c3 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        07c5 : 68              >            pla         ;load status
        07c6 : 48              >            pha
                               >            cmp_flag 0
        07c7 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        07c9 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        07cb : 28              >            plp         ;restore status

                                        set_a $ff,$ff
                               >            load_flag $ff
        07cc : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        07ce : 48              >            pha         ;use stack to load status
        07cf : a9ff            >            lda #$ff     ;precharge accu
        07d1 : 28              >            plp

        07d2 : 48                       pha
                                        tst_a $ff,$ff
        07d3 : 08              >            php         ;save flags
        07d4 : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        07d6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        07d8 : 68              >            pla         ;load status
        07d9 : 48              >            pha
                               >            cmp_flag $ff
        07da : c9ff            >            cmp #($ff|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        07dc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        07de : 28              >            plp         ;restore status

                                        set_a 1,0
                               >            load_flag 0
        07df : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        07e1 : 48              >            pha         ;use stack to load status
        07e2 : a901            >            lda #1     ;precharge accu
        07e4 : 28              >            plp

        07e5 : 48                       pha
                                        tst_a 1,0
        07e6 : 08              >            php         ;save flags
        07e7 : c901            >            cmp #1     ;test result
                               >            trap_ne
        07e9 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        07eb : 68              >            pla         ;load status
        07ec : 48              >            pha
                               >            cmp_flag 0
        07ed : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        07ef : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        07f1 : 28              >            plp         ;restore status

                                        set_a 0,$ff
                               >            load_flag $ff
        07f2 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        07f4 : 48              >            pha         ;use stack to load status
        07f5 : a900            >            lda #0     ;precharge accu
        07f7 : 28              >            plp

        07f8 : 48                       pha
                                        tst_a 0,$ff
        07f9 : 08              >            php         ;save flags
        07fa : c900            >            cmp #0     ;test result
                               >            trap_ne
        07fc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        07fe : 68              >            pla         ;load status
        07ff : 48              >            pha
                               >            cmp_flag $ff
        0800 : c9ff            >            cmp #($ff|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0802 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0804 : 28              >            plp         ;restore status

                                        set_a $ff,0
                               >            load_flag 0
        0805 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0807 : 48              >            pha         ;use stack to load status
        0808 : a9ff            >            lda #$ff     ;precharge accu
        080a : 28              >            plp

        080b : 48                       pha
                                        tst_a $ff,0
        080c : 08              >            php         ;save flags
        080d : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        080f : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0811 : 68              >            pla         ;load status
        0812 : 48              >            pha
                               >            cmp_flag 0
        0813 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0815 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0817 : 28              >            plp         ;restore status

                                        set_a 0,$ff     ;pull
                               >            load_flag $ff     
        0818 : a9ff            >            lda #$ff                  ;allow test to change I-flag (no mask)
                               >
        081a : 48              >            pha         ;use stack to load status
        081b : a900            >            lda #0     ;precharge accu
        081d : 28              >            plp

        081e : 68                       pla
                                        tst_a $ff,$ff-zero
        081f : 08              >            php         ;save flags
        0820 : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        0822 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0824 : 68              >            pla         ;load status
        0825 : 48              >            pha
                               >            cmp_flag $ff-zero
        0826 : c9fd            >            cmp #($ff-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0828 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        082a : 28              >            plp         ;restore status

                                        set_a $ff,0
                               >            load_flag 0
        082b : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        082d : 48              >            pha         ;use stack to load status
        082e : a9ff            >            lda #$ff     ;precharge accu
        0830 : 28              >            plp

        0831 : 68                       pla
                                        tst_a 0,zero
        0832 : 08              >            php         ;save flags
        0833 : c900            >            cmp #0     ;test result
                               >            trap_ne
        0835 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0837 : 68              >            pla         ;load status
        0838 : 48              >            pha
                               >            cmp_flag zero
        0839 : c932            >            cmp #(zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        083b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        083d : 28              >            plp         ;restore status

                                        set_a $fe,$ff
                               >            load_flag $ff
        083e : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0840 : 48              >            pha         ;use stack to load status
        0841 : a9fe            >            lda #$fe     ;precharge accu
        0843 : 28              >            plp

        0844 : 68                       pla
                                        tst_a 1,$ff-zero-minus
        0845 : 08              >            php         ;save flags
        0846 : c901            >            cmp #1     ;test result
                               >            trap_ne
        0848 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        084a : 68              >            pla         ;load status
        084b : 48              >            pha
                               >            cmp_flag $ff-zero-minus
        084c : c97d            >            cmp #($ff-zero-minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        084e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0850 : 28              >            plp         ;restore status

                                        set_a 0,0
                               >            load_flag 0
        0851 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0853 : 48              >            pha         ;use stack to load status
        0854 : a900            >            lda #0     ;precharge accu
        0856 : 28              >            plp

        0857 : 68                       pla
                                        tst_a $ff,minus
        0858 : 08              >            php         ;save flags
        0859 : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        085b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        085d : 68              >            pla         ;load status
        085e : 48              >            pha
                               >            cmp_flag minus
        085f : c9b0            >            cmp #(minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0861 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0863 : 28              >            plp         ;restore status

                                        set_a $ff,$ff
                               >            load_flag $ff
        0864 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0866 : 48              >            pha         ;use stack to load status
        0867 : a9ff            >            lda #$ff     ;precharge accu
        0869 : 28              >            plp

        086a : 68                       pla
                                        tst_a 0,$ff-minus
        086b : 08              >            php         ;save flags
        086c : c900            >            cmp #0     ;test result
                               >            trap_ne
        086e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0870 : 68              >            pla         ;load status
        0871 : 48              >            pha
                               >            cmp_flag $ff-minus
        0872 : c97f            >            cmp #($ff-minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0874 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0876 : 28              >            plp         ;restore status

                                        set_a $fe,0
                               >            load_flag 0
        0877 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0879 : 48              >            pha         ;use stack to load status
        087a : a9fe            >            lda #$fe     ;precharge accu
        087c : 28              >            plp

        087d : 68                       pla
                                        tst_a 1,0
        087e : 08              >            php         ;save flags
        087f : c901            >            cmp #1     ;test result
                               >            trap_ne
        0881 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0883 : 68              >            pla         ;load status
        0884 : 48              >            pha
                               >            cmp_flag 0
        0885 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0887 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0889 : 28              >            plp         ;restore status

        088a : e055                     cpx #$55        ;x & y unchanged?
                                        trap_ne
        088c : d0fe            >        bne *           ;failed not equal (non zero)

        088e : c0aa                     cpy #$aa
                                        trap_ne
        0890 : d0fe            >        bne *           ;failed not equal (non zero)

                                        next_test
        0892 : ad0002          >            lda test_case   ;previous test
        0895 : c905            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        0897 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0006 =                 >test_num = test_num + 1
        0899 : a906            >            lda #test_num   ;*** next tests' number
        089b : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; partial pretest EOR #
                                        set_a $3c,0
                               >            load_flag 0
        089e : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        08a0 : 48              >            pha         ;use stack to load status
        08a1 : a93c            >            lda #$3c     ;precharge accu
        08a3 : 28              >            plp

        08a4 : 49c3                     eor #$c3
                                        tst_a $ff,fn
        08a6 : 08              >            php         ;save flags
        08a7 : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        08a9 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        08ab : 68              >            pla         ;load status
        08ac : 48              >            pha
                               >            cmp_flag fn
        08ad : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        08af : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        08b1 : 28              >            plp         ;restore status

                                        set_a $c3,0
                               >            load_flag 0
        08b2 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        08b4 : 48              >            pha         ;use stack to load status
        08b5 : a9c3            >            lda #$c3     ;precharge accu
        08b7 : 28              >            plp

        08b8 : 49c3                     eor #$c3
                                        tst_a 0,fz
        08ba : 08              >            php         ;save flags
        08bb : c900            >            cmp #0     ;test result
                               >            trap_ne
        08bd : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        08bf : 68              >            pla         ;load status
        08c0 : 48              >            pha
                               >            cmp_flag fz
        08c1 : c932            >            cmp #(fz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        08c3 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        08c5 : 28              >            plp         ;restore status

                                        next_test
        08c6 : ad0002          >            lda test_case   ;previous test
        08c9 : c906            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        08cb : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0007 =                 >test_num = test_num + 1
        08cd : a907            >            lda #test_num   ;*** next tests' number
        08cf : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; PC modifying instructions except branches (NOP, JMP, JSR, RTS, BRK, RTI)
                                ; testing NOP
        08d2 : a224                     ldx #$24
        08d4 : a042                     ldy #$42
                                        set_a $18,0
                               >            load_flag 0
        08d6 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        08d8 : 48              >            pha         ;use stack to load status
        08d9 : a918            >            lda #$18     ;precharge accu
        08db : 28              >            plp

        08dc : ea                       nop
                                        tst_a $18,0
        08dd : 08              >            php         ;save flags
        08de : c918            >            cmp #$18     ;test result
                               >            trap_ne
        08e0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        08e2 : 68              >            pla         ;load status
        08e3 : 48              >            pha
                               >            cmp_flag 0
        08e4 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        08e6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        08e8 : 28              >            plp         ;restore status

        08e9 : e024                     cpx #$24
                                        trap_ne
        08eb : d0fe            >        bne *           ;failed not equal (non zero)

        08ed : c042                     cpy #$42
                                        trap_ne
        08ef : d0fe            >        bne *           ;failed not equal (non zero)

        08f1 : a2db                     ldx #$db
        08f3 : a0bd                     ldy #$bd
                                        set_a $e7,$ff
                               >            load_flag $ff
        08f5 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        08f7 : 48              >            pha         ;use stack to load status
        08f8 : a9e7            >            lda #$e7     ;precharge accu
        08fa : 28              >            plp

        08fb : ea                       nop
                                        tst_a $e7,$ff
        08fc : 08              >            php         ;save flags
        08fd : c9e7            >            cmp #$e7     ;test result
                               >            trap_ne
        08ff : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0901 : 68              >            pla         ;load status
        0902 : 48              >            pha
                               >            cmp_flag $ff
        0903 : c9ff            >            cmp #($ff|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0905 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0907 : 28              >            plp         ;restore status

        0908 : e0db                     cpx #$db
                                        trap_ne
        090a : d0fe            >        bne *           ;failed not equal (non zero)

        090c : c0bd                     cpy #$bd
                                        trap_ne
        090e : d0fe            >        bne *           ;failed not equal (non zero)

                                        next_test
        0910 : ad0002          >            lda test_case   ;previous test
        0913 : c907            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        0915 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0008 =                 >test_num = test_num + 1
        0917 : a908            >            lda #test_num   ;*** next tests' number
        0919 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; jump absolute
                                        set_stat $0
                               >            load_flag $0
        091c : a900            >            lda #$0             ;allow test to change I-flag (no mask)
                               >
        091e : 48              >            pha         ;use stack to load status
        091f : 28              >            plp

        0920 : a946                     lda #'F'
        0922 : a241                     ldx #'A'
        0924 : a052                     ldy #'R'        ;N=0, V=0, Z=0, C=0
        0926 : 4ce936                   jmp test_far
        0929 : ea                       nop
        092a : ea                       nop
                                        trap_ne         ;runover protection
        092b : d0fe            >        bne *           ;failed not equal (non zero)

        092d : e8                       inx
        092e : e8                       inx
        092f :                  far_ret 
                                        trap_eq         ;returned flags OK?
        092f : f0fe            >        beq *           ;failed equal (zero)

                                        trap_pl
        0931 : 10fe            >        bpl *           ;failed plus (bit 7 clear)

                                        trap_cc
        0933 : 90fe            >        bcc *           ;failed carry clear

                                        trap_vc
        0935 : 50fe            >        bvc *           ;failed overflow clear

        0937 : c9ec                     cmp #('F'^$aa)  ;returned registers OK?
                                        trap_ne
        0939 : d0fe            >        bne *           ;failed not equal (non zero)

        093b : e042                     cpx #('A'+1)
                                        trap_ne
        093d : d0fe            >        bne *           ;failed not equal (non zero)

        093f : c04f                     cpy #('R'-3)
                                        trap_ne
        0941 : d0fe            >        bne *           ;failed not equal (non zero)

        0943 : ca                       dex
        0944 : c8                       iny
        0945 : c8                       iny
        0946 : c8                       iny
        0947 : 49aa                     eor #$aa        ;N=0, V=1, Z=0, C=1
        0949 : 4c5209                   jmp test_near
        094c : ea                       nop
        094d : ea                       nop
                                        trap_ne         ;runover protection
        094e : d0fe            >        bne *           ;failed not equal (non zero)

        0950 : e8                       inx
        0951 : e8                       inx
        0952 :                  test_near
                                        trap_eq         ;passed flags OK?
        0952 : f0fe            >        beq *           ;failed equal (zero)

                                        trap_mi
        0954 : 30fe            >        bmi *           ;failed minus (bit 7 set)

                                        trap_cc
        0956 : 90fe            >        bcc *           ;failed carry clear

                                        trap_vc
        0958 : 50fe            >        bvc *           ;failed overflow clear

        095a : c946                     cmp #'F'        ;passed registers OK?
                                        trap_ne
        095c : d0fe            >        bne *           ;failed not equal (non zero)

        095e : e041                     cpx #'A'
                                        trap_ne
        0960 : d0fe            >        bne *           ;failed not equal (non zero)

        0962 : c052                     cpy #'R'
                                        trap_ne
        0964 : d0fe            >        bne *           ;failed not equal (non zero)

                                        next_test
        0966 : ad0002          >            lda test_case   ;previous test
        0969 : c908            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        096b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0009 =                 >test_num = test_num + 1
        096d : a909            >            lda #test_num   ;*** next tests' number
        096f : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; jump indirect
                                        set_stat 0
                               >            load_flag 0
        0972 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0974 : 48              >            pha         ;use stack to load status
        0975 : 28              >            plp

        0976 : a949                     lda #'I'
        0978 : a24e                     ldx #'N'
        097a : a044                     ldy #'D'        ;N=0, V=0, Z=0, C=0
        097c : 6c1837                   jmp (ptr_tst_ind)
        097f : ea                       nop
                                        trap_ne         ;runover protection
        0980 : d0fe            >        bne *           ;failed not equal (non zero)

        0982 : 88                       dey
        0983 : 88                       dey
        0984 :                  ind_ret 
        0984 : 08                       php             ;either SP or Y count will fail, if we do not hit
        0985 : 88                       dey
        0986 : 88                       dey
        0987 : 88                       dey
        0988 : 28                       plp
                                        trap_eq         ;returned flags OK?
        0989 : f0fe            >        beq *           ;failed equal (zero)

                                        trap_pl
        098b : 10fe            >        bpl *           ;failed plus (bit 7 clear)

                                        trap_cc
        098d : 90fe            >        bcc *           ;failed carry clear

                                        trap_vc
        098f : 50fe            >        bvc *           ;failed overflow clear

        0991 : c9e3                     cmp #('I'^$aa)  ;returned registers OK?
                                        trap_ne
        0993 : d0fe            >        bne *           ;failed not equal (non zero)

        0995 : e04f                     cpx #('N'+1)
                                        trap_ne
        0997 : d0fe            >        bne *           ;failed not equal (non zero)

        0999 : c03e                     cpy #('D'-6)
                                        trap_ne
        099b : d0fe            >        bne *           ;failed not equal (non zero)

        099d : ba                       tsx             ;SP check
        099e : e0ff                     cpx #$ff
                                        trap_ne
        09a0 : d0fe            >        bne *           ;failed not equal (non zero)

                                        next_test
        09a2 : ad0002          >            lda test_case   ;previous test
        09a5 : c909            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        09a7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        000a =                 >test_num = test_num + 1
        09a9 : a90a            >            lda #test_num   ;*** next tests' number
        09ab : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; jump subroutine & return from subroutine
                                        set_stat 0
                               >            load_flag 0
        09ae : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        09b0 : 48              >            pha         ;use stack to load status
        09b1 : 28              >            plp

        09b2 : a94a                     lda #'J'
        09b4 : a253                     ldx #'S'
        09b6 : a052                     ldy #'R'        ;N=0, V=0, Z=0, C=0
        09b8 : 205437                   jsr test_jsr
        09ba =                  jsr_ret = *-1           ;last address of jsr = return address
        09bb : 08                       php             ;either SP or Y count will fail, if we do not hit
        09bc : 88                       dey
        09bd : 88                       dey
        09be : 88                       dey
        09bf : 28                       plp
                                        trap_eq         ;returned flags OK?
        09c0 : f0fe            >        beq *           ;failed equal (zero)

                                        trap_pl
        09c2 : 10fe            >        bpl *           ;failed plus (bit 7 clear)

                                        trap_cc
        09c4 : 90fe            >        bcc *           ;failed carry clear

                                        trap_vc
        09c6 : 50fe            >        bvc *           ;failed overflow clear

        09c8 : c9e0                     cmp #('J'^$aa)  ;returned registers OK?
                                        trap_ne
        09ca : d0fe            >        bne *           ;failed not equal (non zero)

        09cc : e054                     cpx #('S'+1)
                                        trap_ne
        09ce : d0fe            >        bne *           ;failed not equal (non zero)

        09d0 : c04c                     cpy #('R'-6)
                                        trap_ne
        09d2 : d0fe            >        bne *           ;failed not equal (non zero)

        09d4 : ba                       tsx             ;sp?
        09d5 : e0ff                     cpx #$ff
                                        trap_ne
        09d7 : d0fe            >        bne *           ;failed not equal (non zero)

                                        next_test
        09d9 : ad0002          >            lda test_case   ;previous test
        09dc : c90a            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        09de : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        000b =                 >test_num = test_num + 1
        09e0 : a90b            >            lda #test_num   ;*** next tests' number
        09e2 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; break & return from interrupt
                                    if ROM_vectors = 1
                                        set_stat 0
                               >            load_flag 0
        09e5 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        09e7 : 48              >            pha         ;use stack to load status
        09e8 : 28              >            plp

        09e9 : a942                     lda #'B'
        09eb : a252                     ldx #'R'
        09ed : a04b                     ldy #'K'        ;N=0, V=0, Z=0, C=0
        09ef : 00                       brk
                                    else
                                        lda #hi brk_ret ;emulated break
                                        pha
                                        lda #lo brk_ret
                                        pha
                                        lda #fao        ;set break & unused on stack
                                        pha
                                        set_stat intdis
                                        lda #'B'
                                        ldx #'R'
                                        ldy #'K'        ;N=0, V=0, Z=0, C=0
                                        jmp irq_trap
                                    endif
        09f0 : 88                       dey             ;should not be executed
        09f1 :                  brk_ret                 ;address of break return
        09f1 : 08                       php             ;either SP or Y count will fail, if we do not hit
        09f2 : 88                       dey
        09f3 : 88                       dey
        09f4 : 88                       dey
        09f5 : c9e8                     cmp #('B'^$aa)  ;returned registers OK?
                                        trap_ne
        09f7 : d0fe            >        bne *           ;failed not equal (non zero)

        09f9 : e053                     cpx #('R'+1)
                                        trap_ne
        09fb : d0fe            >        bne *           ;failed not equal (non zero)

        09fd : c045                     cpy #('K'-6)
                                        trap_ne
        09ff : d0fe            >        bne *           ;failed not equal (non zero)

        0a01 : 68                       pla             ;returned flags OK (unchanged)?
                                        cmp_flag 0
        0a02 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0a04 : d0fe            >        bne *           ;failed not equal (non zero)

        0a06 : ba                       tsx             ;sp?
        0a07 : e0ff                     cpx #$ff
                                        trap_ne
        0a09 : d0fe            >        bne *           ;failed not equal (non zero)

                                        next_test
        0a0b : ad0002          >            lda test_case   ;previous test
        0a0e : c90b            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        0a10 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        000c =                 >test_num = test_num + 1
        0a12 : a90c            >            lda #test_num   ;*** next tests' number
        0a14 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; test set and clear flags CLC CLI CLD CLV SEC SEI SED
                                        set_stat $ff
                               >            load_flag $ff
        0a17 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0a19 : 48              >            pha         ;use stack to load status
        0a1a : 28              >            plp

        0a1b : 18                       clc
                                        tst_stat $ff-carry
        0a1c : 08              >            php         ;save status
        0a1d : 68              >            pla         ;use stack to retrieve status
        0a1e : 48              >            pha
                               >            cmp_flag $ff-carry
        0a1f : c9fe            >            cmp #($ff-carry|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a21 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a23 : 28              >            plp         ;restore status

        0a24 : 38                       sec
                                        tst_stat $ff
        0a25 : 08              >            php         ;save status
        0a26 : 68              >            pla         ;use stack to retrieve status
        0a27 : 48              >            pha
                               >            cmp_flag $ff
        0a28 : c9ff            >            cmp #($ff|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a2a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a2c : 28              >            plp         ;restore status

                                    if I_flag = 3
        0a2d : 58                       cli
                                        tst_stat $ff-intdis
        0a2e : 08              >            php         ;save status
        0a2f : 68              >            pla         ;use stack to retrieve status
        0a30 : 48              >            pha
                               >            cmp_flag $ff-intdis
        0a31 : c9fb            >            cmp #($ff-intdis|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a33 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a35 : 28              >            plp         ;restore status

        0a36 : 78                       sei
                                        tst_stat $ff
        0a37 : 08              >            php         ;save status
        0a38 : 68              >            pla         ;use stack to retrieve status
        0a39 : 48              >            pha
                               >            cmp_flag $ff
        0a3a : c9ff            >            cmp #($ff|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a3c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a3e : 28              >            plp         ;restore status

                                    endif
        0a3f : d8                       cld
                                        tst_stat $ff-decmode
        0a40 : 08              >            php         ;save status
        0a41 : 68              >            pla         ;use stack to retrieve status
        0a42 : 48              >            pha
                               >            cmp_flag $ff-decmode
        0a43 : c9f7            >            cmp #($ff-decmode|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a45 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a47 : 28              >            plp         ;restore status

        0a48 : f8                       sed
                                        tst_stat $ff
        0a49 : 08              >            php         ;save status
        0a4a : 68              >            pla         ;use stack to retrieve status
        0a4b : 48              >            pha
                               >            cmp_flag $ff
        0a4c : c9ff            >            cmp #($ff|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a4e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a50 : 28              >            plp         ;restore status

        0a51 : b8                       clv
                                        tst_stat $ff-overfl
        0a52 : 08              >            php         ;save status
        0a53 : 68              >            pla         ;use stack to retrieve status
        0a54 : 48              >            pha
                               >            cmp_flag $ff-overfl
        0a55 : c9bf            >            cmp #($ff-overfl|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a57 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a59 : 28              >            plp         ;restore status

                                        set_stat 0
                               >            load_flag 0
        0a5a : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0a5c : 48              >            pha         ;use stack to load status
        0a5d : 28              >            plp

                                        tst_stat 0
        0a5e : 08              >            php         ;save status
        0a5f : 68              >            pla         ;use stack to retrieve status
        0a60 : 48              >            pha
                               >            cmp_flag 0
        0a61 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a63 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a65 : 28              >            plp         ;restore status

        0a66 : 38                       sec
                                        tst_stat carry
        0a67 : 08              >            php         ;save status
        0a68 : 68              >            pla         ;use stack to retrieve status
        0a69 : 48              >            pha
                               >            cmp_flag carry
        0a6a : c931            >            cmp #(carry|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a6c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a6e : 28              >            plp         ;restore status

        0a6f : 18                       clc
                                        tst_stat 0  
        0a70 : 08              >            php         ;save status
        0a71 : 68              >            pla         ;use stack to retrieve status
        0a72 : 48              >            pha
                               >            cmp_flag 0  
        0a73 : c930            >            cmp #(0  |fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a75 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a77 : 28              >            plp         ;restore status

                                    if I_flag = 3
        0a78 : 78                       sei
                                        tst_stat intdis
        0a79 : 08              >            php         ;save status
        0a7a : 68              >            pla         ;use stack to retrieve status
        0a7b : 48              >            pha
                               >            cmp_flag intdis
        0a7c : c934            >            cmp #(intdis|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a7e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a80 : 28              >            plp         ;restore status

        0a81 : 58                       cli
                                        tst_stat 0
        0a82 : 08              >            php         ;save status
        0a83 : 68              >            pla         ;use stack to retrieve status
        0a84 : 48              >            pha
                               >            cmp_flag 0
        0a85 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a87 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a89 : 28              >            plp         ;restore status

                                    endif  
        0a8a : f8                       sed
                                        tst_stat decmode
        0a8b : 08              >            php         ;save status
        0a8c : 68              >            pla         ;use stack to retrieve status
        0a8d : 48              >            pha
                               >            cmp_flag decmode
        0a8e : c938            >            cmp #(decmode|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a90 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a92 : 28              >            plp         ;restore status

        0a93 : d8                       cld
                                        tst_stat 0  
        0a94 : 08              >            php         ;save status
        0a95 : 68              >            pla         ;use stack to retrieve status
        0a96 : 48              >            pha
                               >            cmp_flag 0  
        0a97 : c930            >            cmp #(0  |fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0a99 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0a9b : 28              >            plp         ;restore status

                                        set_stat overfl
                               >            load_flag overfl
        0a9c : a940            >            lda #overfl             ;allow test to change I-flag (no mask)
                               >
        0a9e : 48              >            pha         ;use stack to load status
        0a9f : 28              >            plp

                                        tst_stat overfl
        0aa0 : 08              >            php         ;save status
        0aa1 : 68              >            pla         ;use stack to retrieve status
        0aa2 : 48              >            pha
                               >            cmp_flag overfl
        0aa3 : c970            >            cmp #(overfl|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0aa5 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0aa7 : 28              >            plp         ;restore status

        0aa8 : b8                       clv
                                        tst_stat 0
        0aa9 : 08              >            php         ;save status
        0aaa : 68              >            pla         ;use stack to retrieve status
        0aab : 48              >            pha
                               >            cmp_flag 0
        0aac : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0aae : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0ab0 : 28              >            plp         ;restore status

                                        next_test
        0ab1 : ad0002          >            lda test_case   ;previous test
        0ab4 : c90c            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        0ab6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        000d =                 >test_num = test_num + 1
        0ab8 : a90d            >            lda #test_num   ;*** next tests' number
        0aba : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test

                                ; testing index register increment/decrement and transfer
                                ; INX INY DEX DEY TAX TXA TAY TYA 
        0abd : a2fe                     ldx #$fe
                                        set_stat $ff
                               >            load_flag $ff
        0abf : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0ac1 : 48              >            pha         ;use stack to load status
        0ac2 : 28              >            plp

        0ac3 : e8                       inx             ;ff
                                        tst_x $ff,$ff-zero
        0ac4 : 08              >            php         ;save flags
        0ac5 : e0ff            >            cpx #$ff     ;test result
                               >            trap_ne
        0ac7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0ac9 : 68              >            pla         ;load status
        0aca : 48              >            pha
                               >            cmp_flag $ff-zero
        0acb : c9fd            >            cmp #($ff-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0acd : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0acf : 28              >            plp         ;restore status

        0ad0 : e8                       inx             ;00
                                        tst_x 0,$ff-minus
        0ad1 : 08              >            php         ;save flags
        0ad2 : e000            >            cpx #0     ;test result
                               >            trap_ne
        0ad4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0ad6 : 68              >            pla         ;load status
        0ad7 : 48              >            pha
                               >            cmp_flag $ff-minus
        0ad8 : c97f            >            cmp #($ff-minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0ada : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0adc : 28              >            plp         ;restore status

        0add : e8                       inx             ;01
                                        tst_x 1,$ff-minus-zero
        0ade : 08              >            php         ;save flags
        0adf : e001            >            cpx #1     ;test result
                               >            trap_ne
        0ae1 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0ae3 : 68              >            pla         ;load status
        0ae4 : 48              >            pha
                               >            cmp_flag $ff-minus-zero
        0ae5 : c97d            >            cmp #($ff-minus-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0ae7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0ae9 : 28              >            plp         ;restore status

        0aea : ca                       dex             ;00
                                        tst_x 0,$ff-minus
        0aeb : 08              >            php         ;save flags
        0aec : e000            >            cpx #0     ;test result
                               >            trap_ne
        0aee : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0af0 : 68              >            pla         ;load status
        0af1 : 48              >            pha
                               >            cmp_flag $ff-minus
        0af2 : c97f            >            cmp #($ff-minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0af4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0af6 : 28              >            plp         ;restore status

        0af7 : ca                       dex             ;ff
                                        tst_x $ff,$ff-zero
        0af8 : 08              >            php         ;save flags
        0af9 : e0ff            >            cpx #$ff     ;test result
                               >            trap_ne
        0afb : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0afd : 68              >            pla         ;load status
        0afe : 48              >            pha
                               >            cmp_flag $ff-zero
        0aff : c9fd            >            cmp #($ff-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0b01 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b03 : 28              >            plp         ;restore status

        0b04 : ca                       dex             ;fe
                                        set_stat 0
                               >            load_flag 0
        0b05 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0b07 : 48              >            pha         ;use stack to load status
        0b08 : 28              >            plp

        0b09 : e8                       inx             ;ff
                                        tst_x $ff,minus
        0b0a : 08              >            php         ;save flags
        0b0b : e0ff            >            cpx #$ff     ;test result
                               >            trap_ne
        0b0d : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b0f : 68              >            pla         ;load status
        0b10 : 48              >            pha
                               >            cmp_flag minus
        0b11 : c9b0            >            cmp #(minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0b13 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b15 : 28              >            plp         ;restore status

        0b16 : e8                       inx             ;00
                                        tst_x 0,zero
        0b17 : 08              >            php         ;save flags
        0b18 : e000            >            cpx #0     ;test result
                               >            trap_ne
        0b1a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b1c : 68              >            pla         ;load status
        0b1d : 48              >            pha
                               >            cmp_flag zero
        0b1e : c932            >            cmp #(zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0b20 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b22 : 28              >            plp         ;restore status

        0b23 : e8                       inx             ;01
                                        tst_x 1,0
        0b24 : 08              >            php         ;save flags
        0b25 : e001            >            cpx #1     ;test result
                               >            trap_ne
        0b27 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b29 : 68              >            pla         ;load status
        0b2a : 48              >            pha
                               >            cmp_flag 0
        0b2b : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0b2d : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b2f : 28              >            plp         ;restore status

        0b30 : ca                       dex             ;00
                                        tst_x 0,zero
        0b31 : 08              >            php         ;save flags
        0b32 : e000            >            cpx #0     ;test result
                               >            trap_ne
        0b34 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b36 : 68              >            pla         ;load status
        0b37 : 48              >            pha
                               >            cmp_flag zero
        0b38 : c932            >            cmp #(zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0b3a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b3c : 28              >            plp         ;restore status

        0b3d : ca                       dex             ;ff
                                        tst_x $ff,minus
        0b3e : 08              >            php         ;save flags
        0b3f : e0ff            >            cpx #$ff     ;test result
                               >            trap_ne
        0b41 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b43 : 68              >            pla         ;load status
        0b44 : 48              >            pha
                               >            cmp_flag minus
        0b45 : c9b0            >            cmp #(minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0b47 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b49 : 28              >            plp         ;restore status


        0b4a : a0fe                     ldy #$fe
                                        set_stat $ff
                               >            load_flag $ff
        0b4c : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0b4e : 48              >            pha         ;use stack to load status
        0b4f : 28              >            plp

        0b50 : c8                       iny             ;ff
                                        tst_y $ff,$ff-zero
        0b51 : 08              >            php         ;save flags
        0b52 : c0ff            >            cpy #$ff     ;test result
                               >            trap_ne
        0b54 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b56 : 68              >            pla         ;load status
        0b57 : 48              >            pha
                               >            cmp_flag $ff-zero
        0b58 : c9fd            >            cmp #($ff-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0b5a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b5c : 28              >            plp         ;restore status

        0b5d : c8                       iny             ;00
                                        tst_y 0,$ff-minus
        0b5e : 08              >            php         ;save flags
        0b5f : c000            >            cpy #0     ;test result
                               >            trap_ne
        0b61 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b63 : 68              >            pla         ;load status
        0b64 : 48              >            pha
                               >            cmp_flag $ff-minus
        0b65 : c97f            >            cmp #($ff-minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0b67 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b69 : 28              >            plp         ;restore status

        0b6a : c8                       iny             ;01
                                        tst_y 1,$ff-minus-zero
        0b6b : 08              >            php         ;save flags
        0b6c : c001            >            cpy #1     ;test result
                               >            trap_ne
        0b6e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b70 : 68              >            pla         ;load status
        0b71 : 48              >            pha
                               >            cmp_flag $ff-minus-zero
        0b72 : c97d            >            cmp #($ff-minus-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0b74 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b76 : 28              >            plp         ;restore status

        0b77 : 88                       dey             ;00
                                        tst_y 0,$ff-minus
        0b78 : 08              >            php         ;save flags
        0b79 : c000            >            cpy #0     ;test result
                               >            trap_ne
        0b7b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b7d : 68              >            pla         ;load status
        0b7e : 48              >            pha
                               >            cmp_flag $ff-minus
        0b7f : c97f            >            cmp #($ff-minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0b81 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b83 : 28              >            plp         ;restore status

        0b84 : 88                       dey             ;ff
                                        tst_y $ff,$ff-zero
        0b85 : 08              >            php         ;save flags
        0b86 : c0ff            >            cpy #$ff     ;test result
                               >            trap_ne
        0b88 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b8a : 68              >            pla         ;load status
        0b8b : 48              >            pha
                               >            cmp_flag $ff-zero
        0b8c : c9fd            >            cmp #($ff-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0b8e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b90 : 28              >            plp         ;restore status

        0b91 : 88                       dey             ;fe
                                        set_stat 0
                               >            load_flag 0
        0b92 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0b94 : 48              >            pha         ;use stack to load status
        0b95 : 28              >            plp

        0b96 : c8                       iny             ;ff
                                        tst_y $ff,0+minus
        0b97 : 08              >            php         ;save flags
        0b98 : c0ff            >            cpy #$ff     ;test result
                               >            trap_ne
        0b9a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0b9c : 68              >            pla         ;load status
        0b9d : 48              >            pha
                               >            cmp_flag 0+minus
        0b9e : c9b0            >            cmp #(0+minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0ba0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0ba2 : 28              >            plp         ;restore status

        0ba3 : c8                       iny             ;00
                                        tst_y 0,zero
        0ba4 : 08              >            php         ;save flags
        0ba5 : c000            >            cpy #0     ;test result
                               >            trap_ne
        0ba7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0ba9 : 68              >            pla         ;load status
        0baa : 48              >            pha
                               >            cmp_flag zero
        0bab : c932            >            cmp #(zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0bad : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0baf : 28              >            plp         ;restore status

        0bb0 : c8                       iny             ;01
                                        tst_y 1,0
        0bb1 : 08              >            php         ;save flags
        0bb2 : c001            >            cpy #1     ;test result
                               >            trap_ne
        0bb4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0bb6 : 68              >            pla         ;load status
        0bb7 : 48              >            pha
                               >            cmp_flag 0
        0bb8 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0bba : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0bbc : 28              >            plp         ;restore status

        0bbd : 88                       dey             ;00
                                        tst_y 0,zero
        0bbe : 08              >            php         ;save flags
        0bbf : c000            >            cpy #0     ;test result
                               >            trap_ne
        0bc1 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0bc3 : 68              >            pla         ;load status
        0bc4 : 48              >            pha
                               >            cmp_flag zero
        0bc5 : c932            >            cmp #(zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0bc7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0bc9 : 28              >            plp         ;restore status

        0bca : 88                       dey             ;ff
                                        tst_y $ff,minus
        0bcb : 08              >            php         ;save flags
        0bcc : c0ff            >            cpy #$ff     ;test result
                               >            trap_ne
        0bce : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0bd0 : 68              >            pla         ;load status
        0bd1 : 48              >            pha
                               >            cmp_flag minus
        0bd2 : c9b0            >            cmp #(minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0bd4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0bd6 : 28              >            plp         ;restore status


        0bd7 : a2ff                     ldx #$ff
                                        set_stat $ff
                               >            load_flag $ff
        0bd9 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0bdb : 48              >            pha         ;use stack to load status
        0bdc : 28              >            plp

        0bdd : 8a                       txa
                                        tst_a $ff,$ff-zero
        0bde : 08              >            php         ;save flags
        0bdf : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        0be1 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0be3 : 68              >            pla         ;load status
        0be4 : 48              >            pha
                               >            cmp_flag $ff-zero
        0be5 : c9fd            >            cmp #($ff-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0be7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0be9 : 28              >            plp         ;restore status

        0bea : 08                       php
        0beb : e8                       inx             ;00
        0bec : 28                       plp
        0bed : 8a                       txa
                                        tst_a 0,$ff-minus
        0bee : 08              >            php         ;save flags
        0bef : c900            >            cmp #0     ;test result
                               >            trap_ne
        0bf1 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0bf3 : 68              >            pla         ;load status
        0bf4 : 48              >            pha
                               >            cmp_flag $ff-minus
        0bf5 : c97f            >            cmp #($ff-minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0bf7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0bf9 : 28              >            plp         ;restore status

        0bfa : 08                       php
        0bfb : e8                       inx             ;01
        0bfc : 28                       plp
        0bfd : 8a                       txa
                                        tst_a 1,$ff-minus-zero
        0bfe : 08              >            php         ;save flags
        0bff : c901            >            cmp #1     ;test result
                               >            trap_ne
        0c01 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c03 : 68              >            pla         ;load status
        0c04 : 48              >            pha
                               >            cmp_flag $ff-minus-zero
        0c05 : c97d            >            cmp #($ff-minus-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0c07 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c09 : 28              >            plp         ;restore status

                                        set_stat 0
                               >            load_flag 0
        0c0a : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0c0c : 48              >            pha         ;use stack to load status
        0c0d : 28              >            plp

        0c0e : 8a                       txa
                                        tst_a 1,0
        0c0f : 08              >            php         ;save flags
        0c10 : c901            >            cmp #1     ;test result
                               >            trap_ne
        0c12 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c14 : 68              >            pla         ;load status
        0c15 : 48              >            pha
                               >            cmp_flag 0
        0c16 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0c18 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c1a : 28              >            plp         ;restore status

        0c1b : 08                       php
        0c1c : ca                       dex             ;00
        0c1d : 28                       plp
        0c1e : 8a                       txa
                                        tst_a 0,zero
        0c1f : 08              >            php         ;save flags
        0c20 : c900            >            cmp #0     ;test result
                               >            trap_ne
        0c22 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c24 : 68              >            pla         ;load status
        0c25 : 48              >            pha
                               >            cmp_flag zero
        0c26 : c932            >            cmp #(zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0c28 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c2a : 28              >            plp         ;restore status

        0c2b : 08                       php
        0c2c : ca                       dex             ;ff
        0c2d : 28                       plp
        0c2e : 8a                       txa
                                        tst_a $ff,minus
        0c2f : 08              >            php         ;save flags
        0c30 : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        0c32 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c34 : 68              >            pla         ;load status
        0c35 : 48              >            pha
                               >            cmp_flag minus
        0c36 : c9b0            >            cmp #(minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0c38 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c3a : 28              >            plp         ;restore status


        0c3b : a0ff                     ldy #$ff
                                        set_stat $ff
                               >            load_flag $ff
        0c3d : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0c3f : 48              >            pha         ;use stack to load status
        0c40 : 28              >            plp

        0c41 : 98                       tya
                                        tst_a $ff,$ff-zero
        0c42 : 08              >            php         ;save flags
        0c43 : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        0c45 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c47 : 68              >            pla         ;load status
        0c48 : 48              >            pha
                               >            cmp_flag $ff-zero
        0c49 : c9fd            >            cmp #($ff-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0c4b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c4d : 28              >            plp         ;restore status

        0c4e : 08                       php
        0c4f : c8                       iny             ;00
        0c50 : 28                       plp
        0c51 : 98                       tya
                                        tst_a 0,$ff-minus
        0c52 : 08              >            php         ;save flags
        0c53 : c900            >            cmp #0     ;test result
                               >            trap_ne
        0c55 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c57 : 68              >            pla         ;load status
        0c58 : 48              >            pha
                               >            cmp_flag $ff-minus
        0c59 : c97f            >            cmp #($ff-minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0c5b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c5d : 28              >            plp         ;restore status

        0c5e : 08                       php
        0c5f : c8                       iny             ;01
        0c60 : 28                       plp
        0c61 : 98                       tya
                                        tst_a 1,$ff-minus-zero
        0c62 : 08              >            php         ;save flags
        0c63 : c901            >            cmp #1     ;test result
                               >            trap_ne
        0c65 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c67 : 68              >            pla         ;load status
        0c68 : 48              >            pha
                               >            cmp_flag $ff-minus-zero
        0c69 : c97d            >            cmp #($ff-minus-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0c6b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c6d : 28              >            plp         ;restore status

                                        set_stat 0
                               >            load_flag 0
        0c6e : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0c70 : 48              >            pha         ;use stack to load status
        0c71 : 28              >            plp

        0c72 : 98                       tya
                                        tst_a 1,0
        0c73 : 08              >            php         ;save flags
        0c74 : c901            >            cmp #1     ;test result
                               >            trap_ne
        0c76 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c78 : 68              >            pla         ;load status
        0c79 : 48              >            pha
                               >            cmp_flag 0
        0c7a : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0c7c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c7e : 28              >            plp         ;restore status

        0c7f : 08                       php
        0c80 : 88                       dey             ;00
        0c81 : 28                       plp
        0c82 : 98                       tya
                                        tst_a 0,zero
        0c83 : 08              >            php         ;save flags
        0c84 : c900            >            cmp #0     ;test result
                               >            trap_ne
        0c86 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c88 : 68              >            pla         ;load status
        0c89 : 48              >            pha
                               >            cmp_flag zero
        0c8a : c932            >            cmp #(zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0c8c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c8e : 28              >            plp         ;restore status

        0c8f : 08                       php
        0c90 : 88                       dey             ;ff
        0c91 : 28                       plp
        0c92 : 98                       tya
                                        tst_a $ff,minus
        0c93 : 08              >            php         ;save flags
        0c94 : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        0c96 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c98 : 68              >            pla         ;load status
        0c99 : 48              >            pha
                               >            cmp_flag minus
        0c9a : c9b0            >            cmp #(minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0c9c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0c9e : 28              >            plp         ;restore status


                                        load_flag $ff
        0c9f : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)

        0ca1 : 48                       pha
        0ca2 : a2ff                     ldx #$ff        ;ff
        0ca4 : 8a                       txa
        0ca5 : 28                       plp             
        0ca6 : a8                       tay
                                        tst_y $ff,$ff-zero
        0ca7 : 08              >            php         ;save flags
        0ca8 : c0ff            >            cpy #$ff     ;test result
                               >            trap_ne
        0caa : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0cac : 68              >            pla         ;load status
        0cad : 48              >            pha
                               >            cmp_flag $ff-zero
        0cae : c9fd            >            cmp #($ff-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0cb0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0cb2 : 28              >            plp         ;restore status

        0cb3 : 08                       php
        0cb4 : e8                       inx             ;00
        0cb5 : 8a                       txa
        0cb6 : 28                       plp
        0cb7 : a8                       tay
                                        tst_y 0,$ff-minus
        0cb8 : 08              >            php         ;save flags
        0cb9 : c000            >            cpy #0     ;test result
                               >            trap_ne
        0cbb : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0cbd : 68              >            pla         ;load status
        0cbe : 48              >            pha
                               >            cmp_flag $ff-minus
        0cbf : c97f            >            cmp #($ff-minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0cc1 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0cc3 : 28              >            plp         ;restore status

        0cc4 : 08                       php
        0cc5 : e8                       inx             ;01
        0cc6 : 8a                       txa
        0cc7 : 28                       plp
        0cc8 : a8                       tay
                                        tst_y 1,$ff-minus-zero
        0cc9 : 08              >            php         ;save flags
        0cca : c001            >            cpy #1     ;test result
                               >            trap_ne
        0ccc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0cce : 68              >            pla         ;load status
        0ccf : 48              >            pha
                               >            cmp_flag $ff-minus-zero
        0cd0 : c97d            >            cmp #($ff-minus-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0cd2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0cd4 : 28              >            plp         ;restore status

                                        load_flag 0
        0cd5 : a900            >            lda #0             ;allow test to change I-flag (no mask)

        0cd7 : 48                       pha
        0cd8 : a900                     lda #0
        0cda : 8a                       txa
        0cdb : 28                       plp
        0cdc : a8                       tay
                                        tst_y 1,0
        0cdd : 08              >            php         ;save flags
        0cde : c001            >            cpy #1     ;test result
                               >            trap_ne
        0ce0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0ce2 : 68              >            pla         ;load status
        0ce3 : 48              >            pha
                               >            cmp_flag 0
        0ce4 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0ce6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0ce8 : 28              >            plp         ;restore status

        0ce9 : 08                       php
        0cea : ca                       dex             ;00
        0ceb : 8a                       txa
        0cec : 28                       plp
        0ced : a8                       tay
                                        tst_y 0,zero
        0cee : 08              >            php         ;save flags
        0cef : c000            >            cpy #0     ;test result
                               >            trap_ne
        0cf1 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0cf3 : 68              >            pla         ;load status
        0cf4 : 48              >            pha
                               >            cmp_flag zero
        0cf5 : c932            >            cmp #(zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0cf7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0cf9 : 28              >            plp         ;restore status

        0cfa : 08                       php
        0cfb : ca                       dex             ;ff
        0cfc : 8a                       txa
        0cfd : 28                       plp
        0cfe : a8                       tay
                                        tst_y $ff,minus
        0cff : 08              >            php         ;save flags
        0d00 : c0ff            >            cpy #$ff     ;test result
                               >            trap_ne
        0d02 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d04 : 68              >            pla         ;load status
        0d05 : 48              >            pha
                               >            cmp_flag minus
        0d06 : c9b0            >            cmp #(minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0d08 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d0a : 28              >            plp         ;restore status



                                        load_flag $ff
        0d0b : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)

        0d0d : 48                       pha
        0d0e : a0ff                     ldy #$ff        ;ff
        0d10 : 98                       tya
        0d11 : 28                       plp
        0d12 : aa                       tax
                                        tst_x $ff,$ff-zero
        0d13 : 08              >            php         ;save flags
        0d14 : e0ff            >            cpx #$ff     ;test result
                               >            trap_ne
        0d16 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d18 : 68              >            pla         ;load status
        0d19 : 48              >            pha
                               >            cmp_flag $ff-zero
        0d1a : c9fd            >            cmp #($ff-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0d1c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d1e : 28              >            plp         ;restore status

        0d1f : 08                       php
        0d20 : c8                       iny             ;00
        0d21 : 98                       tya
        0d22 : 28                       plp
        0d23 : aa                       tax
                                        tst_x 0,$ff-minus
        0d24 : 08              >            php         ;save flags
        0d25 : e000            >            cpx #0     ;test result
                               >            trap_ne
        0d27 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d29 : 68              >            pla         ;load status
        0d2a : 48              >            pha
                               >            cmp_flag $ff-minus
        0d2b : c97f            >            cmp #($ff-minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0d2d : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d2f : 28              >            plp         ;restore status

        0d30 : 08                       php
        0d31 : c8                       iny             ;01
        0d32 : 98                       tya
        0d33 : 28                       plp
        0d34 : aa                       tax
                                        tst_x 1,$ff-minus-zero
        0d35 : 08              >            php         ;save flags
        0d36 : e001            >            cpx #1     ;test result
                               >            trap_ne
        0d38 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d3a : 68              >            pla         ;load status
        0d3b : 48              >            pha
                               >            cmp_flag $ff-minus-zero
        0d3c : c97d            >            cmp #($ff-minus-zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0d3e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d40 : 28              >            plp         ;restore status

                                        load_flag 0
        0d41 : a900            >            lda #0             ;allow test to change I-flag (no mask)

        0d43 : 48                       pha
        0d44 : a900                     lda #0          ;preset status
        0d46 : 98                       tya
        0d47 : 28                       plp
        0d48 : aa                       tax
                                        tst_x 1,0
        0d49 : 08              >            php         ;save flags
        0d4a : e001            >            cpx #1     ;test result
                               >            trap_ne
        0d4c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d4e : 68              >            pla         ;load status
        0d4f : 48              >            pha
                               >            cmp_flag 0
        0d50 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0d52 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d54 : 28              >            plp         ;restore status

        0d55 : 08                       php
        0d56 : 88                       dey             ;00
        0d57 : 98                       tya
        0d58 : 28                       plp
        0d59 : aa                       tax
                                        tst_x 0,zero
        0d5a : 08              >            php         ;save flags
        0d5b : e000            >            cpx #0     ;test result
                               >            trap_ne
        0d5d : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d5f : 68              >            pla         ;load status
        0d60 : 48              >            pha
                               >            cmp_flag zero
        0d61 : c932            >            cmp #(zero|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0d63 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d65 : 28              >            plp         ;restore status

        0d66 : 08                       php
        0d67 : 88                       dey             ;ff
        0d68 : 98                       tya
        0d69 : 28                       plp
        0d6a : aa                       tax
                                        tst_x $ff,minus
        0d6b : 08              >            php         ;save flags
        0d6c : e0ff            >            cpx #$ff     ;test result
                               >            trap_ne
        0d6e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d70 : 68              >            pla         ;load status
        0d71 : 48              >            pha
                               >            cmp_flag minus
        0d72 : c9b0            >            cmp #(minus|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        0d74 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0d76 : 28              >            plp         ;restore status

                                        next_test
        0d77 : ad0002          >            lda test_case   ;previous test
        0d7a : c90d            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        0d7c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        000e =                 >test_num = test_num + 1
        0d7e : a90e            >            lda #test_num   ;*** next tests' number
        0d80 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ;TSX sets NZ - TXS does not
                                ;  This section also tests for proper stack wrap around.
        0d83 : a201                     ldx #1          ;01
                                        set_stat $ff
                               >            load_flag $ff
        0d85 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0d87 : 48              >            pha         ;use stack to load status
        0d88 : 28              >            plp

        0d89 : 9a                       txs
        0d8a : 08                       php
        0d8b : ad0101                   lda $101
                                        cmp_flag $ff
        0d8e : c9ff            >            cmp #($ff|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0d90 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        0d92 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0d94 : 48              >            pha         ;use stack to load status
        0d95 : 28              >            plp

        0d96 : 9a                       txs
        0d97 : 08                       php
        0d98 : ad0101                   lda $101
                                        cmp_flag 0
        0d9b : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0d9d : d0fe            >        bne *           ;failed not equal (non zero)

        0d9f : ca                       dex             ;00
                                        set_stat $ff
                               >            load_flag $ff
        0da0 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0da2 : 48              >            pha         ;use stack to load status
        0da3 : 28              >            plp

        0da4 : 9a                       txs
        0da5 : 08                       php
        0da6 : ad0001                   lda $100
                                        cmp_flag $ff
        0da9 : c9ff            >            cmp #($ff|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0dab : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        0dad : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0daf : 48              >            pha         ;use stack to load status
        0db0 : 28              >            plp

        0db1 : 9a                       txs
        0db2 : 08                       php
        0db3 : ad0001                   lda $100
                                        cmp_flag 0
        0db6 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0db8 : d0fe            >        bne *           ;failed not equal (non zero)

        0dba : ca                       dex             ;ff
                                        set_stat $ff
                               >            load_flag $ff
        0dbb : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0dbd : 48              >            pha         ;use stack to load status
        0dbe : 28              >            plp

        0dbf : 9a                       txs
        0dc0 : 08                       php
        0dc1 : adff01                   lda $1ff
                                        cmp_flag $ff
        0dc4 : c9ff            >            cmp #($ff|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0dc6 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        0dc8 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0dca : 48              >            pha         ;use stack to load status
        0dcb : 28              >            plp

        0dcc : 9a                       txs
        0dcd : 08                       php
        0dce : adff01                   lda $1ff
                                        cmp_flag 0
        0dd1 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits


        0dd3 : a201                     ldx #1
        0dd5 : 9a                       txs             ;sp=01
                                        set_stat $ff
                               >            load_flag $ff
        0dd6 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0dd8 : 48              >            pha         ;use stack to load status
        0dd9 : 28              >            plp

        0dda : ba                       tsx             ;clears Z, N
        0ddb : 08                       php             ;sp=00
        0ddc : e001                     cpx #1
                                        trap_ne
        0dde : d0fe            >        bne *           ;failed not equal (non zero)

        0de0 : ad0101                   lda $101
                                        cmp_flag $ff-minus-zero
        0de3 : c97d            >            cmp #($ff-minus-zero|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0de5 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        0de7 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0de9 : 48              >            pha         ;use stack to load status
        0dea : 28              >            plp

        0deb : ba                       tsx             ;clears N, sets Z
        0dec : 08                       php             ;sp=ff
        0ded : e000                     cpx #0
                                        trap_ne
        0def : d0fe            >        bne *           ;failed not equal (non zero)

        0df1 : ad0001                   lda $100
                                        cmp_flag $ff-minus
        0df4 : c97f            >            cmp #($ff-minus|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0df6 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        0df8 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0dfa : 48              >            pha         ;use stack to load status
        0dfb : 28              >            plp

        0dfc : ba                       tsx             ;clears N, sets Z
        0dfd : 08                       php             ;sp=fe
        0dfe : e0ff                     cpx #$ff
                                        trap_ne
        0e00 : d0fe            >        bne *           ;failed not equal (non zero)

        0e02 : adff01                   lda $1ff
                                        cmp_flag $ff-zero
        0e05 : c9fd            >            cmp #($ff-zero|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0e07 : d0fe            >        bne *           ;failed not equal (non zero)


        0e09 : a201                     ldx #1
        0e0b : 9a                       txs             ;sp=01
                                        set_stat 0
                               >            load_flag 0
        0e0c : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0e0e : 48              >            pha         ;use stack to load status
        0e0f : 28              >            plp

        0e10 : ba                       tsx             ;clears Z, N
        0e11 : 08                       php             ;sp=00
        0e12 : e001                     cpx #1
                                        trap_ne
        0e14 : d0fe            >        bne *           ;failed not equal (non zero)

        0e16 : ad0101                   lda $101
                                        cmp_flag 0
        0e19 : c930            >            cmp #(0|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0e1b : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        0e1d : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0e1f : 48              >            pha         ;use stack to load status
        0e20 : 28              >            plp

        0e21 : ba                       tsx             ;clears N, sets Z
        0e22 : 08                       php             ;sp=ff
        0e23 : e000                     cpx #0
                                        trap_ne
        0e25 : d0fe            >        bne *           ;failed not equal (non zero)

        0e27 : ad0001                   lda $100
                                        cmp_flag zero
        0e2a : c932            >            cmp #(zero|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0e2c : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        0e2e : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0e30 : 48              >            pha         ;use stack to load status
        0e31 : 28              >            plp

        0e32 : ba                       tsx             ;clears N, sets Z
        0e33 : 08                       php             ;sp=fe
        0e34 : e0ff                     cpx #$ff
                                        trap_ne
        0e36 : d0fe            >        bne *           ;failed not equal (non zero)

        0e38 : adff01                   lda $1ff
                                        cmp_flag minus
        0e3b : c9b0            >            cmp #(minus|fao)&m8    ;expected flags + always on bits

                                        trap_ne
        0e3d : d0fe            >        bne *           ;failed not equal (non zero)

        0e3f : 68                       pla             ;sp=ff
                                        next_test
        0e40 : ad0002          >            lda test_case   ;previous test
        0e43 : c90e            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        0e45 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        000f =                 >test_num = test_num + 1
        0e47 : a90f            >            lda #test_num   ;*** next tests' number
        0e49 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; testing index register load & store LDY LDX STY STX all addressing modes
                                ; LDX / STX - zp,y / abs,y
        0e4c : a003                     ldy #3
        0e4e :                  tldx    
                                        set_stat 0
                               >            load_flag 0
        0e4e : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0e50 : 48              >            pha         ;use stack to load status
        0e51 : 28              >            plp

        0e52 : b613                     ldx zp1,y
        0e54 : 08                       php         ;test stores do not alter flags
        0e55 : 8a                       txa
        0e56 : 49c3                     eor #$c3
        0e58 : 28                       plp
        0e59 : 990302                   sta abst,y
        0e5c : 08                       php         ;flags after load/store sequence
        0e5d : 49c3                     eor #$c3
        0e5f : d91702                   cmp abs1,y  ;test result
                                        trap_ne
        0e62 : d0fe            >        bne *           ;failed not equal (non zero)

        0e64 : 68                       pla         ;load status
                                        eor_flag 0
        0e65 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        0e67 : d91c02                   cmp fLDx,y  ;test flags
                                        trap_ne
        0e6a : d0fe            >        bne *           ;failed not equal (non zero)

        0e6c : 88                       dey
        0e6d : 10df                     bpl tldx                  

        0e6f : a003                     ldy #3
        0e71 :                  tldx1   
                                        set_stat $ff
                               >            load_flag $ff
        0e71 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0e73 : 48              >            pha         ;use stack to load status
        0e74 : 28              >            plp

        0e75 : b613                     ldx zp1,y
        0e77 : 08                       php         ;test stores do not alter flags
        0e78 : 8a                       txa
        0e79 : 49c3                     eor #$c3
        0e7b : 28                       plp
        0e7c : 990302                   sta abst,y
        0e7f : 08                       php         ;flags after load/store sequence
        0e80 : 49c3                     eor #$c3
        0e82 : d91702                   cmp abs1,y  ;test result
                                        trap_ne
        0e85 : d0fe            >        bne *           ;failed not equal (non zero)

        0e87 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        0e88 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        0e8a : d91c02                   cmp fLDx,y  ;test flags
                                        trap_ne
        0e8d : d0fe            >        bne *           ;failed not equal (non zero)

        0e8f : 88                       dey
        0e90 : 10df                     bpl tldx1                  

        0e92 : a003                     ldy #3
        0e94 :                  tldx2   
                                        set_stat 0
                               >            load_flag 0
        0e94 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0e96 : 48              >            pha         ;use stack to load status
        0e97 : 28              >            plp

        0e98 : be1702                   ldx abs1,y
        0e9b : 08                       php         ;test stores do not alter flags
        0e9c : 8a                       txa
        0e9d : 49c3                     eor #$c3
        0e9f : aa                       tax
        0ea0 : 28                       plp
        0ea1 : 960c                     stx zpt,y
        0ea3 : 08                       php         ;flags after load/store sequence
        0ea4 : 49c3                     eor #$c3
        0ea6 : d91300                   cmp zp1,y   ;test result
                                        trap_ne
        0ea9 : d0fe            >        bne *           ;failed not equal (non zero)

        0eab : 68                       pla         ;load status
                                        eor_flag 0
        0eac : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        0eae : d91c02                   cmp fLDx,y  ;test flags
                                        trap_ne
        0eb1 : d0fe            >        bne *           ;failed not equal (non zero)

        0eb3 : 88                       dey
        0eb4 : 10de                     bpl tldx2                  

        0eb6 : a003                     ldy #3
        0eb8 :                  tldx3   
                                        set_stat $ff
                               >            load_flag $ff
        0eb8 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0eba : 48              >            pha         ;use stack to load status
        0ebb : 28              >            plp

        0ebc : be1702                   ldx abs1,y
        0ebf : 08                       php         ;test stores do not alter flags
        0ec0 : 8a                       txa
        0ec1 : 49c3                     eor #$c3
        0ec3 : aa                       tax
        0ec4 : 28                       plp
        0ec5 : 960c                     stx zpt,y
        0ec7 : 08                       php         ;flags after load/store sequence
        0ec8 : 49c3                     eor #$c3
        0eca : d91300                   cmp zp1,y   ;test result
                                        trap_ne
        0ecd : d0fe            >        bne *           ;failed not equal (non zero)

        0ecf : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        0ed0 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        0ed2 : d91c02                   cmp fLDx,y  ;test flags
                                        trap_ne
        0ed5 : d0fe            >        bne *           ;failed not equal (non zero)

        0ed7 : 88                       dey
        0ed8 : 10de                     bpl tldx3

        0eda : a003                     ldy #3      ;testing store result
        0edc : a200                     ldx #0
        0ede : b90c00           tstx    lda zpt,y
        0ee1 : 49c3                     eor #$c3
        0ee3 : d91300                   cmp zp1,y
                                        trap_ne     ;store to zp data
        0ee6 : d0fe            >        bne *           ;failed not equal (non zero)

        0ee8 : 960c                     stx zpt,y   ;clear                
        0eea : b90302                   lda abst,y
        0eed : 49c3                     eor #$c3
        0eef : d91702                   cmp abs1,y
                                        trap_ne     ;store to abs data
        0ef2 : d0fe            >        bne *           ;failed not equal (non zero)

        0ef4 : 8a                       txa
        0ef5 : 990302                   sta abst,y  ;clear                
        0ef8 : 88                       dey
        0ef9 : 10e3                     bpl tstx
                                        next_test
        0efb : ad0002          >            lda test_case   ;previous test
        0efe : c90f            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        0f00 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0010 =                 >test_num = test_num + 1
        0f02 : a910            >            lda #test_num   ;*** next tests' number
        0f04 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; indexed wraparound test (only zp should wrap)
        0f07 : a0fd                     ldy #3+$fa
        0f09 : b619             tldx4   ldx zp1-$fa&$ff,y   ;wrap on indexed zp
        0f0b : 8a                       txa
        0f0c : 990901                   sta abst-$fa,y      ;no STX abs,y!
        0f0f : 88                       dey
        0f10 : c0fa                     cpy #$fa
        0f12 : b0f5                     bcs tldx4                  
        0f14 : a0fd                     ldy #3+$fa
        0f16 : be1d01           tldx5   ldx abs1-$fa,y      ;no wrap on indexed abs
        0f19 : 9612                     stx zpt-$fa&$ff,y
        0f1b : 88                       dey
        0f1c : c0fa                     cpy #$fa
        0f1e : b0f6                     bcs tldx5                  
        0f20 : a003                     ldy #3      ;testing wraparound result
        0f22 : a200                     ldx #0
        0f24 : b90c00           tstx1   lda zpt,y
        0f27 : d91300                   cmp zp1,y
                                        trap_ne     ;store to zp data
        0f2a : d0fe            >        bne *           ;failed not equal (non zero)

        0f2c : 960c                     stx zpt,y   ;clear                
        0f2e : b90302                   lda abst,y
        0f31 : d91702                   cmp abs1,y
                                        trap_ne     ;store to abs data
        0f34 : d0fe            >        bne *           ;failed not equal (non zero)

        0f36 : 8a                       txa
        0f37 : 990302                   sta abst,y  ;clear                
        0f3a : 88                       dey
        0f3b : 10e7                     bpl tstx1
                                        next_test
        0f3d : ad0002          >            lda test_case   ;previous test
        0f40 : c910            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        0f42 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0011 =                 >test_num = test_num + 1
        0f44 : a911            >            lda #test_num   ;*** next tests' number
        0f46 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; LDY / STY - zp,x / abs,x
        0f49 : a203                     ldx #3
        0f4b :                  tldy    
                                        set_stat 0
                               >            load_flag 0
        0f4b : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0f4d : 48              >            pha         ;use stack to load status
        0f4e : 28              >            plp

        0f4f : b413                     ldy zp1,x
        0f51 : 08                       php         ;test stores do not alter flags
        0f52 : 98                       tya
        0f53 : 49c3                     eor #$c3
        0f55 : 28                       plp
        0f56 : 9d0302                   sta abst,x
        0f59 : 08                       php         ;flags after load/store sequence
        0f5a : 49c3                     eor #$c3
        0f5c : dd1702                   cmp abs1,x  ;test result
                                        trap_ne
        0f5f : d0fe            >        bne *           ;failed not equal (non zero)

        0f61 : 68                       pla         ;load status
                                        eor_flag 0
        0f62 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        0f64 : dd1c02                   cmp fLDx,x  ;test flags
                                        trap_ne
        0f67 : d0fe            >        bne *           ;failed not equal (non zero)

        0f69 : ca                       dex
        0f6a : 10df                     bpl tldy                  

        0f6c : a203                     ldx #3
        0f6e :                  tldy1   
                                        set_stat $ff
                               >            load_flag $ff
        0f6e : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0f70 : 48              >            pha         ;use stack to load status
        0f71 : 28              >            plp

        0f72 : b413                     ldy zp1,x
        0f74 : 08                       php         ;test stores do not alter flags
        0f75 : 98                       tya
        0f76 : 49c3                     eor #$c3
        0f78 : 28                       plp
        0f79 : 9d0302                   sta abst,x
        0f7c : 08                       php         ;flags after load/store sequence
        0f7d : 49c3                     eor #$c3
        0f7f : dd1702                   cmp abs1,x  ;test result
                                        trap_ne
        0f82 : d0fe            >        bne *           ;failed not equal (non zero)

        0f84 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        0f85 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        0f87 : dd1c02                   cmp fLDx,x  ;test flags
                                        trap_ne
        0f8a : d0fe            >        bne *           ;failed not equal (non zero)

        0f8c : ca                       dex
        0f8d : 10df                     bpl tldy1                  

        0f8f : a203                     ldx #3
        0f91 :                  tldy2   
                                        set_stat 0
                               >            load_flag 0
        0f91 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        0f93 : 48              >            pha         ;use stack to load status
        0f94 : 28              >            plp

        0f95 : bc1702                   ldy abs1,x
        0f98 : 08                       php         ;test stores do not alter flags
        0f99 : 98                       tya
        0f9a : 49c3                     eor #$c3
        0f9c : a8                       tay
        0f9d : 28                       plp
        0f9e : 940c                     sty zpt,x
        0fa0 : 08                       php         ;flags after load/store sequence
        0fa1 : 49c3                     eor #$c3
        0fa3 : d513                     cmp zp1,x   ;test result
                                        trap_ne
        0fa5 : d0fe            >        bne *           ;failed not equal (non zero)

        0fa7 : 68                       pla         ;load status
                                        eor_flag 0
        0fa8 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        0faa : dd1c02                   cmp fLDx,x  ;test flags
                                        trap_ne
        0fad : d0fe            >        bne *           ;failed not equal (non zero)

        0faf : ca                       dex
        0fb0 : 10df                     bpl tldy2                  

        0fb2 : a203                     ldx #3
        0fb4 :                  tldy3
                                        set_stat $ff
                               >            load_flag $ff
        0fb4 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        0fb6 : 48              >            pha         ;use stack to load status
        0fb7 : 28              >            plp

        0fb8 : bc1702                   ldy abs1,x
        0fbb : 08                       php         ;test stores do not alter flags
        0fbc : 98                       tya
        0fbd : 49c3                     eor #$c3
        0fbf : a8                       tay
        0fc0 : 28                       plp
        0fc1 : 940c                     sty zpt,x
        0fc3 : 08                       php         ;flags after load/store sequence
        0fc4 : 49c3                     eor #$c3
        0fc6 : d513                     cmp zp1,x   ;test result
                                        trap_ne
        0fc8 : d0fe            >        bne *           ;failed not equal (non zero)

        0fca : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        0fcb : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        0fcd : dd1c02                   cmp fLDx,x  ;test flags
                                        trap_ne
        0fd0 : d0fe            >        bne *           ;failed not equal (non zero)

        0fd2 : ca                       dex
        0fd3 : 10df                     bpl tldy3

        0fd5 : a203                     ldx #3      ;testing store result
        0fd7 : a000                     ldy #0
        0fd9 : b50c             tsty    lda zpt,x
        0fdb : 49c3                     eor #$c3
        0fdd : d513                     cmp zp1,x
                                        trap_ne     ;store to zp,x data
        0fdf : d0fe            >        bne *           ;failed not equal (non zero)

        0fe1 : 940c                     sty zpt,x   ;clear                
        0fe3 : bd0302                   lda abst,x
        0fe6 : 49c3                     eor #$c3
        0fe8 : dd1702                   cmp abs1,x
                                        trap_ne     ;store to abs,x data
        0feb : d0fe            >        bne *           ;failed not equal (non zero)

        0fed : 8a                       txa
        0fee : 9d0302                   sta abst,x  ;clear                
        0ff1 : ca                       dex
        0ff2 : 10e5                     bpl tsty
                                        next_test
        0ff4 : ad0002          >            lda test_case   ;previous test
        0ff7 : c911            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        0ff9 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0012 =                 >test_num = test_num + 1
        0ffb : a912            >            lda #test_num   ;*** next tests' number
        0ffd : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; indexed wraparound test (only zp should wrap)
        1000 : a2fd                     ldx #3+$fa
        1002 : b419             tldy4   ldy zp1-$fa&$ff,x   ;wrap on indexed zp
        1004 : 98                       tya
        1005 : 9d0901                   sta abst-$fa,x      ;no STX abs,x!
        1008 : ca                       dex
        1009 : e0fa                     cpx #$fa
        100b : b0f5                     bcs tldy4                  
        100d : a2fd                     ldx #3+$fa
        100f : bc1d01           tldy5   ldy abs1-$fa,x      ;no wrap on indexed abs
        1012 : 9412                     sty zpt-$fa&$ff,x
        1014 : ca                       dex
        1015 : e0fa                     cpx #$fa
        1017 : b0f6                     bcs tldy5                  
        1019 : a203                     ldx #3      ;testing wraparound result
        101b : a000                     ldy #0
        101d : b50c             tsty1   lda zpt,x
        101f : d513                     cmp zp1,x
                                        trap_ne     ;store to zp,x data
        1021 : d0fe            >        bne *           ;failed not equal (non zero)

        1023 : 940c                     sty zpt,x   ;clear                
        1025 : bd0302                   lda abst,x
        1028 : dd1702                   cmp abs1,x
                                        trap_ne     ;store to abs,x data
        102b : d0fe            >        bne *           ;failed not equal (non zero)

        102d : 8a                       txa
        102e : 9d0302                   sta abst,x  ;clear                
        1031 : ca                       dex
        1032 : 10e9                     bpl tsty1
                                        next_test
        1034 : ad0002          >            lda test_case   ;previous test
        1037 : c912            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        1039 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0013 =                 >test_num = test_num + 1
        103b : a913            >            lda #test_num   ;*** next tests' number
        103d : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; LDX / STX - zp / abs / #
                                        set_stat 0  
                               >            load_flag 0  
        1040 : a900            >            lda #0               ;allow test to change I-flag (no mask)
                               >
        1042 : 48              >            pha         ;use stack to load status
        1043 : 28              >            plp

        1044 : a613                     ldx zp1
        1046 : 08                       php         ;test stores do not alter flags
        1047 : 8a                       txa
        1048 : 49c3                     eor #$c3
        104a : aa                       tax
        104b : 28                       plp
        104c : 8e0302                   stx abst
        104f : 08                       php         ;flags after load/store sequence
        1050 : 49c3                     eor #$c3
        1052 : aa                       tax
        1053 : e0c3                     cpx #$c3    ;test result
                                        trap_ne
        1055 : d0fe            >        bne *           ;failed not equal (non zero)

        1057 : 68                       pla         ;load status
                                        eor_flag 0
        1058 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        105a : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        105d : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        105f : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1061 : 48              >            pha         ;use stack to load status
        1062 : 28              >            plp

        1063 : a614                     ldx zp1+1
        1065 : 08                       php         ;test stores do not alter flags
        1066 : 8a                       txa
        1067 : 49c3                     eor #$c3
        1069 : aa                       tax
        106a : 28                       plp
        106b : 8e0402                   stx abst+1
        106e : 08                       php         ;flags after load/store sequence
        106f : 49c3                     eor #$c3
        1071 : aa                       tax
        1072 : e082                     cpx #$82    ;test result
                                        trap_ne
        1074 : d0fe            >        bne *           ;failed not equal (non zero)

        1076 : 68                       pla         ;load status
                                        eor_flag 0
        1077 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1079 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        107c : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        107e : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1080 : 48              >            pha         ;use stack to load status
        1081 : 28              >            plp

        1082 : a615                     ldx zp1+2
        1084 : 08                       php         ;test stores do not alter flags
        1085 : 8a                       txa
        1086 : 49c3                     eor #$c3
        1088 : aa                       tax
        1089 : 28                       plp
        108a : 8e0502                   stx abst+2
        108d : 08                       php         ;flags after load/store sequence
        108e : 49c3                     eor #$c3
        1090 : aa                       tax
        1091 : e041                     cpx #$41    ;test result
                                        trap_ne
        1093 : d0fe            >        bne *           ;failed not equal (non zero)

        1095 : 68                       pla         ;load status
                                        eor_flag 0
        1096 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1098 : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        109b : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        109d : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        109f : 48              >            pha         ;use stack to load status
        10a0 : 28              >            plp

        10a1 : a616                     ldx zp1+3
        10a3 : 08                       php         ;test stores do not alter flags
        10a4 : 8a                       txa
        10a5 : 49c3                     eor #$c3
        10a7 : aa                       tax
        10a8 : 28                       plp
        10a9 : 8e0602                   stx abst+3
        10ac : 08                       php         ;flags after load/store sequence
        10ad : 49c3                     eor #$c3
        10af : aa                       tax
        10b0 : e000                     cpx #0      ;test result
                                        trap_ne
        10b2 : d0fe            >        bne *           ;failed not equal (non zero)

        10b4 : 68                       pla         ;load status
                                        eor_flag 0
        10b5 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        10b7 : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        10ba : d0fe            >        bne *           ;failed not equal (non zero)


                                        set_stat $ff
                               >            load_flag $ff
        10bc : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        10be : 48              >            pha         ;use stack to load status
        10bf : 28              >            plp

        10c0 : a613                     ldx zp1  
        10c2 : 08                       php         ;test stores do not alter flags
        10c3 : 8a                       txa
        10c4 : 49c3                     eor #$c3
        10c6 : aa                       tax
        10c7 : 28                       plp
        10c8 : 8e0302                   stx abst  
        10cb : 08                       php         ;flags after load/store sequence
        10cc : 49c3                     eor #$c3
        10ce : aa                       tax
        10cf : e0c3                     cpx #$c3    ;test result
                                        trap_ne     ;
        10d1 : d0fe            >        bne *           ;failed not equal (non zero)

        10d3 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        10d4 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        10d6 : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        10d9 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        10db : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        10dd : 48              >            pha         ;use stack to load status
        10de : 28              >            plp

        10df : a614                     ldx zp1+1
        10e1 : 08                       php         ;test stores do not alter flags
        10e2 : 8a                       txa
        10e3 : 49c3                     eor #$c3
        10e5 : aa                       tax
        10e6 : 28                       plp
        10e7 : 8e0402                   stx abst+1
        10ea : 08                       php         ;flags after load/store sequence
        10eb : 49c3                     eor #$c3
        10ed : aa                       tax
        10ee : e082                     cpx #$82    ;test result
                                        trap_ne
        10f0 : d0fe            >        bne *           ;failed not equal (non zero)

        10f2 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        10f3 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        10f5 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        10f8 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        10fa : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        10fc : 48              >            pha         ;use stack to load status
        10fd : 28              >            plp

        10fe : a615                     ldx zp1+2
        1100 : 08                       php         ;test stores do not alter flags
        1101 : 8a                       txa
        1102 : 49c3                     eor #$c3
        1104 : aa                       tax
        1105 : 28                       plp
        1106 : 8e0502                   stx abst+2
        1109 : 08                       php         ;flags after load/store sequence
        110a : 49c3                     eor #$c3
        110c : aa                       tax
        110d : e041                     cpx #$41    ;test result
                                        trap_ne     ;
        110f : d0fe            >        bne *           ;failed not equal (non zero)

        1111 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1112 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1114 : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        1117 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        1119 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        111b : 48              >            pha         ;use stack to load status
        111c : 28              >            plp

        111d : a616                     ldx zp1+3
        111f : 08                       php         ;test stores do not alter flags
        1120 : 8a                       txa
        1121 : 49c3                     eor #$c3
        1123 : aa                       tax
        1124 : 28                       plp
        1125 : 8e0602                   stx abst+3
        1128 : 08                       php         ;flags after load/store sequence
        1129 : 49c3                     eor #$c3
        112b : aa                       tax
        112c : e000                     cpx #0      ;test result
                                        trap_ne
        112e : d0fe            >        bne *           ;failed not equal (non zero)

        1130 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1131 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1133 : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        1136 : d0fe            >        bne *           ;failed not equal (non zero)


                                        set_stat 0
                               >            load_flag 0
        1138 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        113a : 48              >            pha         ;use stack to load status
        113b : 28              >            plp

        113c : ae1702                   ldx abs1  
        113f : 08                       php         ;test stores do not alter flags
        1140 : 8a                       txa
        1141 : 49c3                     eor #$c3
        1143 : aa                       tax
        1144 : 28                       plp
        1145 : 860c                     stx zpt  
        1147 : 08                       php         ;flags after load/store sequence
        1148 : 49c3                     eor #$c3
        114a : c513                     cmp zp1     ;test result
                                        trap_ne
        114c : d0fe            >        bne *           ;failed not equal (non zero)

        114e : 68                       pla         ;load status
                                        eor_flag 0
        114f : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1151 : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        1154 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1156 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1158 : 48              >            pha         ;use stack to load status
        1159 : 28              >            plp

        115a : ae1802                   ldx abs1+1
        115d : 08                       php         ;test stores do not alter flags
        115e : 8a                       txa
        115f : 49c3                     eor #$c3
        1161 : aa                       tax
        1162 : 28                       plp
        1163 : 860d                     stx zpt+1
        1165 : 08                       php         ;flags after load/store sequence
        1166 : 49c3                     eor #$c3
        1168 : c514                     cmp zp1+1   ;test result
                                        trap_ne
        116a : d0fe            >        bne *           ;failed not equal (non zero)

        116c : 68                       pla         ;load status
                                        eor_flag 0
        116d : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        116f : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        1172 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1174 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1176 : 48              >            pha         ;use stack to load status
        1177 : 28              >            plp

        1178 : ae1902                   ldx abs1+2
        117b : 08                       php         ;test stores do not alter flags
        117c : 8a                       txa
        117d : 49c3                     eor #$c3
        117f : aa                       tax
        1180 : 28                       plp
        1181 : 860e                     stx zpt+2
        1183 : 08                       php         ;flags after load/store sequence
        1184 : 49c3                     eor #$c3
        1186 : c515                     cmp zp1+2   ;test result
                                        trap_ne
        1188 : d0fe            >        bne *           ;failed not equal (non zero)

        118a : 68                       pla         ;load status
                                        eor_flag 0
        118b : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        118d : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        1190 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1192 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1194 : 48              >            pha         ;use stack to load status
        1195 : 28              >            plp

        1196 : ae1a02                   ldx abs1+3
        1199 : 08                       php         ;test stores do not alter flags
        119a : 8a                       txa
        119b : 49c3                     eor #$c3
        119d : aa                       tax
        119e : 28                       plp
        119f : 860f                     stx zpt+3
        11a1 : 08                       php         ;flags after load/store sequence
        11a2 : 49c3                     eor #$c3
        11a4 : c516                     cmp zp1+3   ;test result
                                        trap_ne
        11a6 : d0fe            >        bne *           ;failed not equal (non zero)

        11a8 : 68                       pla         ;load status
                                        eor_flag 0
        11a9 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        11ab : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        11ae : d0fe            >        bne *           ;failed not equal (non zero)


                                        set_stat $ff
                               >            load_flag $ff
        11b0 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        11b2 : 48              >            pha         ;use stack to load status
        11b3 : 28              >            plp

        11b4 : ae1702                   ldx abs1  
        11b7 : 08                       php         ;test stores do not alter flags
        11b8 : 8a                       txa
        11b9 : 49c3                     eor #$c3
        11bb : aa                       tax
        11bc : 28                       plp
        11bd : 860c                     stx zpt  
        11bf : 08                       php         ;flags after load/store sequence
        11c0 : 49c3                     eor #$c3
        11c2 : aa                       tax
        11c3 : e413                     cpx zp1     ;test result
                                        trap_ne
        11c5 : d0fe            >        bne *           ;failed not equal (non zero)

        11c7 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        11c8 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        11ca : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        11cd : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        11cf : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        11d1 : 48              >            pha         ;use stack to load status
        11d2 : 28              >            plp

        11d3 : ae1802                   ldx abs1+1
        11d6 : 08                       php         ;test stores do not alter flags
        11d7 : 8a                       txa
        11d8 : 49c3                     eor #$c3
        11da : aa                       tax
        11db : 28                       plp
        11dc : 860d                     stx zpt+1
        11de : 08                       php         ;flags after load/store sequence
        11df : 49c3                     eor #$c3
        11e1 : aa                       tax
        11e2 : e414                     cpx zp1+1   ;test result
                                        trap_ne
        11e4 : d0fe            >        bne *           ;failed not equal (non zero)

        11e6 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        11e7 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        11e9 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        11ec : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        11ee : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        11f0 : 48              >            pha         ;use stack to load status
        11f1 : 28              >            plp

        11f2 : ae1902                   ldx abs1+2
        11f5 : 08                       php         ;test stores do not alter flags
        11f6 : 8a                       txa
        11f7 : 49c3                     eor #$c3
        11f9 : aa                       tax
        11fa : 28                       plp
        11fb : 860e                     stx zpt+2
        11fd : 08                       php         ;flags after load/store sequence
        11fe : 49c3                     eor #$c3
        1200 : aa                       tax
        1201 : e415                     cpx zp1+2   ;test result
                                        trap_ne
        1203 : d0fe            >        bne *           ;failed not equal (non zero)

        1205 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1206 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1208 : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        120b : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        120d : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        120f : 48              >            pha         ;use stack to load status
        1210 : 28              >            plp

        1211 : ae1a02                   ldx abs1+3
        1214 : 08                       php         ;test stores do not alter flags
        1215 : 8a                       txa
        1216 : 49c3                     eor #$c3
        1218 : aa                       tax
        1219 : 28                       plp
        121a : 860f                     stx zpt+3
        121c : 08                       php         ;flags after load/store sequence
        121d : 49c3                     eor #$c3
        121f : aa                       tax
        1220 : e416                     cpx zp1+3   ;test result
                                        trap_ne
        1222 : d0fe            >        bne *           ;failed not equal (non zero)

        1224 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1225 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1227 : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        122a : d0fe            >        bne *           ;failed not equal (non zero)


                                        set_stat 0  
                               >            load_flag 0  
        122c : a900            >            lda #0               ;allow test to change I-flag (no mask)
                               >
        122e : 48              >            pha         ;use stack to load status
        122f : 28              >            plp

        1230 : a2c3                     ldx #$c3
        1232 : 08                       php
        1233 : ec1702                   cpx abs1    ;test result
                                        trap_ne
        1236 : d0fe            >        bne *           ;failed not equal (non zero)

        1238 : 68                       pla         ;load status
                                        eor_flag 0
        1239 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        123b : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        123e : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1240 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1242 : 48              >            pha         ;use stack to load status
        1243 : 28              >            plp

        1244 : a282                     ldx #$82
        1246 : 08                       php
        1247 : ec1802                   cpx abs1+1  ;test result
                                        trap_ne
        124a : d0fe            >        bne *           ;failed not equal (non zero)

        124c : 68                       pla         ;load status
                                        eor_flag 0
        124d : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        124f : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        1252 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1254 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1256 : 48              >            pha         ;use stack to load status
        1257 : 28              >            plp

        1258 : a241                     ldx #$41
        125a : 08                       php
        125b : ec1902                   cpx abs1+2  ;test result
                                        trap_ne
        125e : d0fe            >        bne *           ;failed not equal (non zero)

        1260 : 68                       pla         ;load status
                                        eor_flag 0
        1261 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1263 : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        1266 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1268 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        126a : 48              >            pha         ;use stack to load status
        126b : 28              >            plp

        126c : a200                     ldx #0
        126e : 08                       php
        126f : ec1a02                   cpx abs1+3  ;test result
                                        trap_ne
        1272 : d0fe            >        bne *           ;failed not equal (non zero)

        1274 : 68                       pla         ;load status
                                        eor_flag 0
        1275 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1277 : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        127a : d0fe            >        bne *           ;failed not equal (non zero)


                                        set_stat $ff
                               >            load_flag $ff
        127c : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        127e : 48              >            pha         ;use stack to load status
        127f : 28              >            plp

        1280 : a2c3                     ldx #$c3  
        1282 : 08                       php
        1283 : ec1702                   cpx abs1    ;test result
                                        trap_ne
        1286 : d0fe            >        bne *           ;failed not equal (non zero)

        1288 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1289 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        128b : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        128e : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        1290 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1292 : 48              >            pha         ;use stack to load status
        1293 : 28              >            plp

        1294 : a282                     ldx #$82
        1296 : 08                       php
        1297 : ec1802                   cpx abs1+1  ;test result
                                        trap_ne
        129a : d0fe            >        bne *           ;failed not equal (non zero)

        129c : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        129d : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        129f : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        12a2 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        12a4 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        12a6 : 48              >            pha         ;use stack to load status
        12a7 : 28              >            plp

        12a8 : a241                     ldx #$41
        12aa : 08                       php
        12ab : ec1902                   cpx abs1+2  ;test result
                                        trap_ne
        12ae : d0fe            >        bne *           ;failed not equal (non zero)

        12b0 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        12b1 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        12b3 : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        12b6 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        12b8 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        12ba : 48              >            pha         ;use stack to load status
        12bb : 28              >            plp

        12bc : a200                     ldx #0
        12be : 08                       php
        12bf : ec1a02                   cpx abs1+3  ;test result
                                        trap_ne
        12c2 : d0fe            >        bne *           ;failed not equal (non zero)

        12c4 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        12c5 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        12c7 : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        12ca : d0fe            >        bne *           ;failed not equal (non zero)


        12cc : a200                     ldx #0
        12ce : a50c                     lda zpt  
        12d0 : 49c3                     eor #$c3
        12d2 : c513                     cmp zp1  
                                        trap_ne     ;store to zp data
        12d4 : d0fe            >        bne *           ;failed not equal (non zero)

        12d6 : 860c                     stx zpt     ;clear                
        12d8 : ad0302                   lda abst  
        12db : 49c3                     eor #$c3
        12dd : cd1702                   cmp abs1  
                                        trap_ne     ;store to abs data
        12e0 : d0fe            >        bne *           ;failed not equal (non zero)

        12e2 : 8e0302                   stx abst    ;clear                
        12e5 : a50d                     lda zpt+1
        12e7 : 49c3                     eor #$c3
        12e9 : c514                     cmp zp1+1
                                        trap_ne     ;store to zp data
        12eb : d0fe            >        bne *           ;failed not equal (non zero)

        12ed : 860d                     stx zpt+1   ;clear                
        12ef : ad0402                   lda abst+1
        12f2 : 49c3                     eor #$c3
        12f4 : cd1802                   cmp abs1+1
                                        trap_ne     ;store to abs data
        12f7 : d0fe            >        bne *           ;failed not equal (non zero)

        12f9 : 8e0402                   stx abst+1  ;clear                
        12fc : a50e                     lda zpt+2
        12fe : 49c3                     eor #$c3
        1300 : c515                     cmp zp1+2
                                        trap_ne     ;store to zp data
        1302 : d0fe            >        bne *           ;failed not equal (non zero)

        1304 : 860e                     stx zpt+2   ;clear                
        1306 : ad0502                   lda abst+2
        1309 : 49c3                     eor #$c3
        130b : cd1902                   cmp abs1+2
                                        trap_ne     ;store to abs data
        130e : d0fe            >        bne *           ;failed not equal (non zero)

        1310 : 8e0502                   stx abst+2  ;clear                
        1313 : a50f                     lda zpt+3
        1315 : 49c3                     eor #$c3
        1317 : c516                     cmp zp1+3
                                        trap_ne     ;store to zp data
        1319 : d0fe            >        bne *           ;failed not equal (non zero)

        131b : 860f                     stx zpt+3   ;clear                
        131d : ad0602                   lda abst+3
        1320 : 49c3                     eor #$c3
        1322 : cd1a02                   cmp abs1+3
                                        trap_ne     ;store to abs data
        1325 : d0fe            >        bne *           ;failed not equal (non zero)

        1327 : 8e0602                   stx abst+3  ;clear                
                                        next_test
        132a : ad0002          >            lda test_case   ;previous test
        132d : c913            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        132f : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0014 =                 >test_num = test_num + 1
        1331 : a914            >            lda #test_num   ;*** next tests' number
        1333 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; LDY / STY - zp / abs / #
                                        set_stat 0
                               >            load_flag 0
        1336 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1338 : 48              >            pha         ;use stack to load status
        1339 : 28              >            plp

        133a : a413                     ldy zp1  
        133c : 08                       php         ;test stores do not alter flags
        133d : 98                       tya
        133e : 49c3                     eor #$c3
        1340 : a8                       tay
        1341 : 28                       plp
        1342 : 8c0302                   sty abst  
        1345 : 08                       php         ;flags after load/store sequence
        1346 : 49c3                     eor #$c3
        1348 : a8                       tay
        1349 : c0c3                     cpy #$c3    ;test result
                                        trap_ne
        134b : d0fe            >        bne *           ;failed not equal (non zero)

        134d : 68                       pla         ;load status
                                        eor_flag 0
        134e : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1350 : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        1353 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1355 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1357 : 48              >            pha         ;use stack to load status
        1358 : 28              >            plp

        1359 : a414                     ldy zp1+1
        135b : 08                       php         ;test stores do not alter flags
        135c : 98                       tya
        135d : 49c3                     eor #$c3
        135f : a8                       tay
        1360 : 28                       plp
        1361 : 8c0402                   sty abst+1
        1364 : 08                       php         ;flags after load/store sequence
        1365 : 49c3                     eor #$c3
        1367 : a8                       tay
        1368 : c082                     cpy #$82    ;test result
                                        trap_ne
        136a : d0fe            >        bne *           ;failed not equal (non zero)

        136c : 68                       pla         ;load status
                                        eor_flag 0
        136d : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        136f : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        1372 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1374 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1376 : 48              >            pha         ;use stack to load status
        1377 : 28              >            plp

        1378 : a415                     ldy zp1+2
        137a : 08                       php         ;test stores do not alter flags
        137b : 98                       tya
        137c : 49c3                     eor #$c3
        137e : a8                       tay
        137f : 28                       plp
        1380 : 8c0502                   sty abst+2
        1383 : 08                       php         ;flags after load/store sequence
        1384 : 49c3                     eor #$c3
        1386 : a8                       tay
        1387 : c041                     cpy #$41    ;test result
                                        trap_ne
        1389 : d0fe            >        bne *           ;failed not equal (non zero)

        138b : 68                       pla         ;load status
                                        eor_flag 0
        138c : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        138e : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        1391 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1393 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1395 : 48              >            pha         ;use stack to load status
        1396 : 28              >            plp

        1397 : a416                     ldy zp1+3
        1399 : 08                       php         ;test stores do not alter flags
        139a : 98                       tya
        139b : 49c3                     eor #$c3
        139d : a8                       tay
        139e : 28                       plp
        139f : 8c0602                   sty abst+3
        13a2 : 08                       php         ;flags after load/store sequence
        13a3 : 49c3                     eor #$c3
        13a5 : a8                       tay
        13a6 : c000                     cpy #0      ;test result
                                        trap_ne
        13a8 : d0fe            >        bne *           ;failed not equal (non zero)

        13aa : 68                       pla         ;load status
                                        eor_flag 0
        13ab : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        13ad : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        13b0 : d0fe            >        bne *           ;failed not equal (non zero)


                                        set_stat $ff
                               >            load_flag $ff
        13b2 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        13b4 : 48              >            pha         ;use stack to load status
        13b5 : 28              >            plp

        13b6 : a413                     ldy zp1  
        13b8 : 08                       php         ;test stores do not alter flags
        13b9 : 98                       tya
        13ba : 49c3                     eor #$c3
        13bc : a8                       tay
        13bd : 28                       plp
        13be : 8c0302                   sty abst  
        13c1 : 08                       php         ;flags after load/store sequence
        13c2 : 49c3                     eor #$c3
        13c4 : a8                       tay
        13c5 : c0c3                     cpy #$c3    ;test result
                                        trap_ne
        13c7 : d0fe            >        bne *           ;failed not equal (non zero)

        13c9 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        13ca : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        13cc : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        13cf : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        13d1 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        13d3 : 48              >            pha         ;use stack to load status
        13d4 : 28              >            plp

        13d5 : a414                     ldy zp1+1
        13d7 : 08                       php         ;test stores do not alter flags
        13d8 : 98                       tya
        13d9 : 49c3                     eor #$c3
        13db : a8                       tay
        13dc : 28                       plp
        13dd : 8c0402                   sty abst+1
        13e0 : 08                       php         ;flags after load/store sequence
        13e1 : 49c3                     eor #$c3
        13e3 : a8                       tay
        13e4 : c082                     cpy #$82   ;test result
                                        trap_ne
        13e6 : d0fe            >        bne *           ;failed not equal (non zero)

        13e8 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        13e9 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        13eb : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        13ee : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        13f0 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        13f2 : 48              >            pha         ;use stack to load status
        13f3 : 28              >            plp

        13f4 : a415                     ldy zp1+2
        13f6 : 08                       php         ;test stores do not alter flags
        13f7 : 98                       tya
        13f8 : 49c3                     eor #$c3
        13fa : a8                       tay
        13fb : 28                       plp
        13fc : 8c0502                   sty abst+2
        13ff : 08                       php         ;flags after load/store sequence
        1400 : 49c3                     eor #$c3
        1402 : a8                       tay
        1403 : c041                     cpy #$41    ;test result
                                        trap_ne
        1405 : d0fe            >        bne *           ;failed not equal (non zero)

        1407 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1408 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        140a : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        140d : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        140f : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1411 : 48              >            pha         ;use stack to load status
        1412 : 28              >            plp

        1413 : a416                     ldy zp1+3
        1415 : 08                       php         ;test stores do not alter flags
        1416 : 98                       tya
        1417 : 49c3                     eor #$c3
        1419 : a8                       tay
        141a : 28                       plp
        141b : 8c0602                   sty abst+3
        141e : 08                       php         ;flags after load/store sequence
        141f : 49c3                     eor #$c3
        1421 : a8                       tay
        1422 : c000                     cpy #0      ;test result
                                        trap_ne
        1424 : d0fe            >        bne *           ;failed not equal (non zero)

        1426 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1427 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1429 : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        142c : d0fe            >        bne *           ;failed not equal (non zero)


                                        set_stat 0
                               >            load_flag 0
        142e : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1430 : 48              >            pha         ;use stack to load status
        1431 : 28              >            plp

        1432 : ac1702                   ldy abs1  
        1435 : 08                       php         ;test stores do not alter flags
        1436 : 98                       tya
        1437 : 49c3                     eor #$c3
        1439 : a8                       tay
        143a : 28                       plp
        143b : 840c                     sty zpt  
        143d : 08                       php         ;flags after load/store sequence
        143e : 49c3                     eor #$c3
        1440 : a8                       tay
        1441 : c413                     cpy zp1     ;test result
                                        trap_ne
        1443 : d0fe            >        bne *           ;failed not equal (non zero)

        1445 : 68                       pla         ;load status
                                        eor_flag 0
        1446 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1448 : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        144b : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        144d : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        144f : 48              >            pha         ;use stack to load status
        1450 : 28              >            plp

        1451 : ac1802                   ldy abs1+1
        1454 : 08                       php         ;test stores do not alter flags
        1455 : 98                       tya
        1456 : 49c3                     eor #$c3
        1458 : a8                       tay
        1459 : 28                       plp
        145a : 840d                     sty zpt+1
        145c : 08                       php         ;flags after load/store sequence
        145d : 49c3                     eor #$c3
        145f : a8                       tay
        1460 : c414                     cpy zp1+1   ;test result
                                        trap_ne
        1462 : d0fe            >        bne *           ;failed not equal (non zero)

        1464 : 68                       pla         ;load status
                                        eor_flag 0
        1465 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1467 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        146a : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        146c : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        146e : 48              >            pha         ;use stack to load status
        146f : 28              >            plp

        1470 : ac1902                   ldy abs1+2
        1473 : 08                       php         ;test stores do not alter flags
        1474 : 98                       tya
        1475 : 49c3                     eor #$c3
        1477 : a8                       tay
        1478 : 28                       plp
        1479 : 840e                     sty zpt+2
        147b : 08                       php         ;flags after load/store sequence
        147c : 49c3                     eor #$c3
        147e : a8                       tay
        147f : c415                     cpy zp1+2   ;test result
                                        trap_ne
        1481 : d0fe            >        bne *           ;failed not equal (non zero)

        1483 : 68                       pla         ;load status
                                        eor_flag 0
        1484 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1486 : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        1489 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        148b : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        148d : 48              >            pha         ;use stack to load status
        148e : 28              >            plp

        148f : ac1a02                   ldy abs1+3
        1492 : 08                       php         ;test stores do not alter flags
        1493 : 98                       tya
        1494 : 49c3                     eor #$c3
        1496 : a8                       tay
        1497 : 28                       plp
        1498 : 840f                     sty zpt+3
        149a : 08                       php         ;flags after load/store sequence
        149b : 49c3                     eor #$c3
        149d : a8                       tay
        149e : c416                     cpy zp1+3   ;test result
                                        trap_ne
        14a0 : d0fe            >        bne *           ;failed not equal (non zero)

        14a2 : 68                       pla         ;load status
                                        eor_flag 0
        14a3 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        14a5 : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        14a8 : d0fe            >        bne *           ;failed not equal (non zero)


                                        set_stat $ff
                               >            load_flag $ff
        14aa : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        14ac : 48              >            pha         ;use stack to load status
        14ad : 28              >            plp

        14ae : ac1702                   ldy abs1  
        14b1 : 08                       php         ;test stores do not alter flags
        14b2 : 98                       tya
        14b3 : 49c3                     eor #$c3
        14b5 : a8                       tay
        14b6 : 28                       plp
        14b7 : 840c                     sty zpt  
        14b9 : 08                       php         ;flags after load/store sequence
        14ba : 49c3                     eor #$c3
        14bc : a8                       tay
        14bd : c513                     cmp zp1     ;test result
                                        trap_ne
        14bf : d0fe            >        bne *           ;failed not equal (non zero)

        14c1 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        14c2 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        14c4 : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        14c7 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        14c9 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        14cb : 48              >            pha         ;use stack to load status
        14cc : 28              >            plp

        14cd : ac1802                   ldy abs1+1
        14d0 : 08                       php         ;test stores do not alter flags
        14d1 : 98                       tya
        14d2 : 49c3                     eor #$c3
        14d4 : a8                       tay
        14d5 : 28                       plp
        14d6 : 840d                     sty zpt+1
        14d8 : 08                       php         ;flags after load/store sequence
        14d9 : 49c3                     eor #$c3
        14db : a8                       tay
        14dc : c514                     cmp zp1+1   ;test result
                                        trap_ne
        14de : d0fe            >        bne *           ;failed not equal (non zero)

        14e0 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        14e1 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        14e3 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        14e6 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        14e8 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        14ea : 48              >            pha         ;use stack to load status
        14eb : 28              >            plp

        14ec : ac1902                   ldy abs1+2
        14ef : 08                       php         ;test stores do not alter flags
        14f0 : 98                       tya
        14f1 : 49c3                     eor #$c3
        14f3 : a8                       tay
        14f4 : 28                       plp
        14f5 : 840e                     sty zpt+2
        14f7 : 08                       php         ;flags after load/store sequence
        14f8 : 49c3                     eor #$c3
        14fa : a8                       tay
        14fb : c515                     cmp zp1+2   ;test result
                                        trap_ne
        14fd : d0fe            >        bne *           ;failed not equal (non zero)

        14ff : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1500 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1502 : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        1505 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        1507 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1509 : 48              >            pha         ;use stack to load status
        150a : 28              >            plp

        150b : ac1a02                   ldy abs1+3
        150e : 08                       php         ;test stores do not alter flags
        150f : 98                       tya
        1510 : 49c3                     eor #$c3
        1512 : a8                       tay
        1513 : 28                       plp
        1514 : 840f                     sty zpt+3
        1516 : 08                       php         ;flags after load/store sequence
        1517 : 49c3                     eor #$c3
        1519 : a8                       tay
        151a : c516                     cmp zp1+3   ;test result
                                        trap_ne
        151c : d0fe            >        bne *           ;failed not equal (non zero)

        151e : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        151f : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1521 : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        1524 : d0fe            >        bne *           ;failed not equal (non zero)



                                        set_stat 0
                               >            load_flag 0
        1526 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1528 : 48              >            pha         ;use stack to load status
        1529 : 28              >            plp

        152a : a0c3                     ldy #$c3  
        152c : 08                       php
        152d : cc1702                   cpy abs1    ;test result
                                        trap_ne
        1530 : d0fe            >        bne *           ;failed not equal (non zero)

        1532 : 68                       pla         ;load status
                                        eor_flag 0
        1533 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1535 : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        1538 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        153a : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        153c : 48              >            pha         ;use stack to load status
        153d : 28              >            plp

        153e : a082                     ldy #$82
        1540 : 08                       php
        1541 : cc1802                   cpy abs1+1  ;test result
                                        trap_ne
        1544 : d0fe            >        bne *           ;failed not equal (non zero)

        1546 : 68                       pla         ;load status
                                        eor_flag 0
        1547 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1549 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        154c : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        154e : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1550 : 48              >            pha         ;use stack to load status
        1551 : 28              >            plp

        1552 : a041                     ldy #$41
        1554 : 08                       php
        1555 : cc1902                   cpy abs1+2  ;test result
                                        trap_ne
        1558 : d0fe            >        bne *           ;failed not equal (non zero)

        155a : 68                       pla         ;load status
                                        eor_flag 0
        155b : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        155d : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        1560 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1562 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1564 : 48              >            pha         ;use stack to load status
        1565 : 28              >            plp

        1566 : a000                     ldy #0
        1568 : 08                       php
        1569 : cc1a02                   cpy abs1+3  ;test result
                                        trap_ne
        156c : d0fe            >        bne *           ;failed not equal (non zero)

        156e : 68                       pla         ;load status
                                        eor_flag 0
        156f : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1571 : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        1574 : d0fe            >        bne *           ;failed not equal (non zero)


                                        set_stat $ff
                               >            load_flag $ff
        1576 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1578 : 48              >            pha         ;use stack to load status
        1579 : 28              >            plp

        157a : a0c3                     ldy #$c3  
        157c : 08                       php
        157d : cc1702                   cpy abs1    ;test result
                                        trap_ne
        1580 : d0fe            >        bne *           ;failed not equal (non zero)

        1582 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1583 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1585 : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        1588 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        158a : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        158c : 48              >            pha         ;use stack to load status
        158d : 28              >            plp

        158e : a082                     ldy #$82
        1590 : 08                       php
        1591 : cc1802                   cpy abs1+1  ;test result
                                        trap_ne
        1594 : d0fe            >        bne *           ;failed not equal (non zero)

        1596 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1597 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1599 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        159c : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        159e : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        15a0 : 48              >            pha         ;use stack to load status
        15a1 : 28              >            plp

        15a2 : a041                     ldy #$41
        15a4 : 08                       php
        15a5 : cc1902                   cpy abs1+2   ;test result
                                        trap_ne
        15a8 : d0fe            >        bne *           ;failed not equal (non zero)

        15aa : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        15ab : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        15ad : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        15b0 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        15b2 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        15b4 : 48              >            pha         ;use stack to load status
        15b5 : 28              >            plp

        15b6 : a000                     ldy #0
        15b8 : 08                       php
        15b9 : cc1a02                   cpy abs1+3  ;test result
                                        trap_ne
        15bc : d0fe            >        bne *           ;failed not equal (non zero)

        15be : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        15bf : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        15c1 : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        15c4 : d0fe            >        bne *           ;failed not equal (non zero)


        15c6 : a000                     ldy #0
        15c8 : a50c                     lda zpt  
        15ca : 49c3                     eor #$c3
        15cc : c513                     cmp zp1  
                                        trap_ne     ;store to zp   data
        15ce : d0fe            >        bne *           ;failed not equal (non zero)

        15d0 : 840c                     sty zpt     ;clear                
        15d2 : ad0302                   lda abst  
        15d5 : 49c3                     eor #$c3
        15d7 : cd1702                   cmp abs1  
                                        trap_ne     ;store to abs   data
        15da : d0fe            >        bne *           ;failed not equal (non zero)

        15dc : 8c0302                   sty abst    ;clear                
        15df : a50d                     lda zpt+1
        15e1 : 49c3                     eor #$c3
        15e3 : c514                     cmp zp1+1
                                        trap_ne     ;store to zp+1 data
        15e5 : d0fe            >        bne *           ;failed not equal (non zero)

        15e7 : 840d                     sty zpt+1   ;clear                
        15e9 : ad0402                   lda abst+1
        15ec : 49c3                     eor #$c3
        15ee : cd1802                   cmp abs1+1
                                        trap_ne     ;store to abs+1 data
        15f1 : d0fe            >        bne *           ;failed not equal (non zero)

        15f3 : 8c0402                   sty abst+1  ;clear                
        15f6 : a50e                     lda zpt+2
        15f8 : 49c3                     eor #$c3
        15fa : c515                     cmp zp1+2
                                        trap_ne     ;store to zp+2 data
        15fc : d0fe            >        bne *           ;failed not equal (non zero)

        15fe : 840e                     sty zpt+2   ;clear                
        1600 : ad0502                   lda abst+2
        1603 : 49c3                     eor #$c3
        1605 : cd1902                   cmp abs1+2
                                        trap_ne     ;store to abs+2 data
        1608 : d0fe            >        bne *           ;failed not equal (non zero)

        160a : 8c0502                   sty abst+2  ;clear                
        160d : a50f                     lda zpt+3
        160f : 49c3                     eor #$c3
        1611 : c516                     cmp zp1+3
                                        trap_ne     ;store to zp+3 data
        1613 : d0fe            >        bne *           ;failed not equal (non zero)

        1615 : 840f                     sty zpt+3   ;clear                
        1617 : ad0602                   lda abst+3
        161a : 49c3                     eor #$c3
        161c : cd1a02                   cmp abs1+3
                                        trap_ne     ;store to abs+3 data
        161f : d0fe            >        bne *           ;failed not equal (non zero)

        1621 : 8c0602                   sty abst+3  ;clear                
                                        next_test
        1624 : ad0002          >            lda test_case   ;previous test
        1627 : c914            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        1629 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0015 =                 >test_num = test_num + 1
        162b : a915            >            lda #test_num   ;*** next tests' number
        162d : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; testing load / store accumulator LDA / STA all addressing modes
                                ; LDA / STA - zp,x / abs,x
        1630 : a203                     ldx #3
        1632 :                  tldax    
                                        set_stat 0
                               >            load_flag 0
        1632 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1634 : 48              >            pha         ;use stack to load status
        1635 : 28              >            plp

        1636 : b513                     lda zp1,x
        1638 : 08                       php         ;test stores do not alter flags
        1639 : 49c3                     eor #$c3
        163b : 28                       plp
        163c : 9d0302                   sta abst,x
        163f : 08                       php         ;flags after load/store sequence
        1640 : 49c3                     eor #$c3
        1642 : dd1702                   cmp abs1,x  ;test result
                                        trap_ne
        1645 : d0fe            >        bne *           ;failed not equal (non zero)

        1647 : 68                       pla         ;load status
                                        eor_flag 0
        1648 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        164a : dd1c02                   cmp fLDx,x  ;test flags
                                        trap_ne
        164d : d0fe            >        bne *           ;failed not equal (non zero)

        164f : ca                       dex
        1650 : 10e0                     bpl tldax                  

        1652 : a203                     ldx #3
        1654 :                  tldax1   
                                        set_stat $ff
                               >            load_flag $ff
        1654 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1656 : 48              >            pha         ;use stack to load status
        1657 : 28              >            plp

        1658 : b513                     lda zp1,x
        165a : 08                       php         ;test stores do not alter flags
        165b : 49c3                     eor #$c3
        165d : 28                       plp
        165e : 9d0302                   sta abst,x
        1661 : 08                       php         ;flags after load/store sequence
        1662 : 49c3                     eor #$c3
        1664 : dd1702                   cmp abs1,x   ;test result
                                        trap_ne
        1667 : d0fe            >        bne *           ;failed not equal (non zero)

        1669 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        166a : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        166c : dd1c02                   cmp fLDx,x  ;test flags
                                        trap_ne
        166f : d0fe            >        bne *           ;failed not equal (non zero)

        1671 : ca                       dex
        1672 : 10e0                     bpl tldax1                  

        1674 : a203                     ldx #3
        1676 :                  tldax2   
                                        set_stat 0
                               >            load_flag 0
        1676 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1678 : 48              >            pha         ;use stack to load status
        1679 : 28              >            plp

        167a : bd1702                   lda abs1,x
        167d : 08                       php         ;test stores do not alter flags
        167e : 49c3                     eor #$c3
        1680 : 28                       plp
        1681 : 950c                     sta zpt,x
        1683 : 08                       php         ;flags after load/store sequence
        1684 : 49c3                     eor #$c3
        1686 : d513                     cmp zp1,x   ;test result
                                        trap_ne
        1688 : d0fe            >        bne *           ;failed not equal (non zero)

        168a : 68                       pla         ;load status
                                        eor_flag 0
        168b : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        168d : dd1c02                   cmp fLDx,x  ;test flags
                                        trap_ne
        1690 : d0fe            >        bne *           ;failed not equal (non zero)

        1692 : ca                       dex
        1693 : 10e1                     bpl tldax2                  

        1695 : a203                     ldx #3
        1697 :                  tldax3
                                        set_stat $ff
                               >            load_flag $ff
        1697 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1699 : 48              >            pha         ;use stack to load status
        169a : 28              >            plp

        169b : bd1702                   lda abs1,x
        169e : 08                       php         ;test stores do not alter flags
        169f : 49c3                     eor #$c3
        16a1 : 28                       plp
        16a2 : 950c                     sta zpt,x
        16a4 : 08                       php         ;flags after load/store sequence
        16a5 : 49c3                     eor #$c3
        16a7 : d513                     cmp zp1,x   ;test result
                                        trap_ne
        16a9 : d0fe            >        bne *           ;failed not equal (non zero)

        16ab : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        16ac : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        16ae : dd1c02                   cmp fLDx,x  ;test flags
                                        trap_ne
        16b1 : d0fe            >        bne *           ;failed not equal (non zero)

        16b3 : ca                       dex
        16b4 : 10e1                     bpl tldax3

        16b6 : a203                     ldx #3      ;testing store result
        16b8 : a000                     ldy #0
        16ba : b50c             tstax   lda zpt,x
        16bc : 49c3                     eor #$c3
        16be : d513                     cmp zp1,x
                                        trap_ne     ;store to zp,x data
        16c0 : d0fe            >        bne *           ;failed not equal (non zero)

        16c2 : 940c                     sty zpt,x   ;clear                
        16c4 : bd0302                   lda abst,x
        16c7 : 49c3                     eor #$c3
        16c9 : dd1702                   cmp abs1,x
                                        trap_ne     ;store to abs,x data
        16cc : d0fe            >        bne *           ;failed not equal (non zero)

        16ce : 8a                       txa
        16cf : 9d0302                   sta abst,x  ;clear                
        16d2 : ca                       dex
        16d3 : 10e5                     bpl tstax
                                        next_test
        16d5 : ad0002          >            lda test_case   ;previous test
        16d8 : c915            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        16da : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0016 =                 >test_num = test_num + 1
        16dc : a916            >            lda #test_num   ;*** next tests' number
        16de : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; LDA / STA - (zp),y / abs,y / (zp,x)
        16e1 : a003                     ldy #3
        16e3 :                  tlday    
                                        set_stat 0
                               >            load_flag 0
        16e3 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        16e5 : 48              >            pha         ;use stack to load status
        16e6 : 28              >            plp

        16e7 : b124                     lda (ind1),y
        16e9 : 08                       php         ;test stores do not alter flags
        16ea : 49c3                     eor #$c3
        16ec : 28                       plp
        16ed : 990302                   sta abst,y
        16f0 : 08                       php         ;flags after load/store sequence
        16f1 : 49c3                     eor #$c3
        16f3 : d91702                   cmp abs1,y  ;test result
                                        trap_ne
        16f6 : d0fe            >        bne *           ;failed not equal (non zero)

        16f8 : 68                       pla         ;load status
                                        eor_flag 0
        16f9 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        16fb : d91c02                   cmp fLDx,y  ;test flags
                                        trap_ne
        16fe : d0fe            >        bne *           ;failed not equal (non zero)

        1700 : 88                       dey
        1701 : 10e0                     bpl tlday                  

        1703 : a003                     ldy #3
        1705 :                  tlday1   
                                        set_stat $ff
                               >            load_flag $ff
        1705 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1707 : 48              >            pha         ;use stack to load status
        1708 : 28              >            plp

        1709 : b124                     lda (ind1),y
        170b : 08                       php         ;test stores do not alter flags
        170c : 49c3                     eor #$c3
        170e : 28                       plp
        170f : 990302                   sta abst,y
        1712 : 08                       php         ;flags after load/store sequence
        1713 : 49c3                     eor #$c3
        1715 : d91702                   cmp abs1,y  ;test result
                                        trap_ne
        1718 : d0fe            >        bne *           ;failed not equal (non zero)

        171a : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        171b : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        171d : d91c02                   cmp fLDx,y  ;test flags
                                        trap_ne
        1720 : d0fe            >        bne *           ;failed not equal (non zero)

        1722 : 88                       dey
        1723 : 10e0                     bpl tlday1                  

        1725 : a003                     ldy #3      ;testing store result
        1727 : a200                     ldx #0
        1729 : b90302           tstay   lda abst,y
        172c : 49c3                     eor #$c3
        172e : d91702                   cmp abs1,y
                                        trap_ne     ;store to abs data
        1731 : d0fe            >        bne *           ;failed not equal (non zero)

        1733 : 8a                       txa
        1734 : 990302                   sta abst,y  ;clear                
        1737 : 88                       dey
        1738 : 10ef                     bpl tstay

        173a : a003                     ldy #3
        173c :                  tlday2   
                                        set_stat 0
                               >            load_flag 0
        173c : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        173e : 48              >            pha         ;use stack to load status
        173f : 28              >            plp

        1740 : b91702                   lda abs1,y
        1743 : 08                       php         ;test stores do not alter flags
        1744 : 49c3                     eor #$c3
        1746 : 28                       plp
        1747 : 9130                     sta (indt),y
        1749 : 08                       php         ;flags after load/store sequence
        174a : 49c3                     eor #$c3
        174c : d124                     cmp (ind1),y    ;test result
                                        trap_ne
        174e : d0fe            >        bne *           ;failed not equal (non zero)

        1750 : 68                       pla         ;load status
                                        eor_flag 0
        1751 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1753 : d91c02                   cmp fLDx,y  ;test flags
                                        trap_ne
        1756 : d0fe            >        bne *           ;failed not equal (non zero)

        1758 : 88                       dey
        1759 : 10e1                     bpl tlday2                  

        175b : a003                     ldy #3
        175d :                  tlday3   
                                        set_stat $ff
                               >            load_flag $ff
        175d : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        175f : 48              >            pha         ;use stack to load status
        1760 : 28              >            plp

        1761 : b91702                   lda abs1,y
        1764 : 08                       php         ;test stores do not alter flags
        1765 : 49c3                     eor #$c3
        1767 : 28                       plp
        1768 : 9130                     sta (indt),y
        176a : 08                       php         ;flags after load/store sequence
        176b : 49c3                     eor #$c3
        176d : d124                     cmp (ind1),y   ;test result
                                        trap_ne
        176f : d0fe            >        bne *           ;failed not equal (non zero)

        1771 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1772 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1774 : d91c02                   cmp fLDx,y  ;test flags
                                        trap_ne
        1777 : d0fe            >        bne *           ;failed not equal (non zero)

        1779 : 88                       dey
        177a : 10e1                     bpl tlday3

        177c : a003                     ldy #3      ;testing store result
        177e : a200                     ldx #0
        1780 : b90302           tstay1  lda abst,y
        1783 : 49c3                     eor #$c3
        1785 : d91702                   cmp abs1,y
                                        trap_ne     ;store to abs data
        1788 : d0fe            >        bne *           ;failed not equal (non zero)

        178a : 8a                       txa
        178b : 990302                   sta abst,y  ;clear                
        178e : 88                       dey
        178f : 10ef                     bpl tstay1

        1791 : a206                     ldx #6
        1793 : a003                     ldy #3
        1795 :                  tldax4   
                                        set_stat 0
                               >            load_flag 0
        1795 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1797 : 48              >            pha         ;use stack to load status
        1798 : 28              >            plp

        1799 : a124                     lda (ind1,x)
        179b : 08                       php         ;test stores do not alter flags
        179c : 49c3                     eor #$c3
        179e : 28                       plp
        179f : 8130                     sta (indt,x)
        17a1 : 08                       php         ;flags after load/store sequence
        17a2 : 49c3                     eor #$c3
        17a4 : d91702                   cmp abs1,y  ;test result
                                        trap_ne
        17a7 : d0fe            >        bne *           ;failed not equal (non zero)

        17a9 : 68                       pla         ;load status
                                        eor_flag 0
        17aa : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        17ac : d91c02                   cmp fLDx,y  ;test flags
                                        trap_ne
        17af : d0fe            >        bne *           ;failed not equal (non zero)

        17b1 : ca                       dex
        17b2 : ca                       dex
        17b3 : 88                       dey
        17b4 : 10df                     bpl tldax4                  

        17b6 : a206                     ldx #6
        17b8 : a003                     ldy #3
        17ba :                  tldax5
                                        set_stat $ff
                               >            load_flag $ff
        17ba : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        17bc : 48              >            pha         ;use stack to load status
        17bd : 28              >            plp

        17be : a124                     lda (ind1,x)
        17c0 : 08                       php         ;test stores do not alter flags
        17c1 : 49c3                     eor #$c3
        17c3 : 28                       plp
        17c4 : 8130                     sta (indt,x)
        17c6 : 08                       php         ;flags after load/store sequence
        17c7 : 49c3                     eor #$c3
        17c9 : d91702                   cmp abs1,y  ;test result
                                        trap_ne
        17cc : d0fe            >        bne *           ;failed not equal (non zero)

        17ce : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        17cf : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        17d1 : d91c02                   cmp fLDx,y  ;test flags
                                        trap_ne
        17d4 : d0fe            >        bne *           ;failed not equal (non zero)

        17d6 : ca                       dex
        17d7 : ca                       dex
        17d8 : 88                       dey
        17d9 : 10df                     bpl tldax5

        17db : a003                     ldy #3      ;testing store result
        17dd : a200                     ldx #0
        17df : b90302           tstay2  lda abst,y
        17e2 : 49c3                     eor #$c3
        17e4 : d91702                   cmp abs1,y
                                        trap_ne     ;store to abs data
        17e7 : d0fe            >        bne *           ;failed not equal (non zero)

        17e9 : 8a                       txa
        17ea : 990302                   sta abst,y  ;clear                
        17ed : 88                       dey
        17ee : 10ef                     bpl tstay2
                                        next_test
        17f0 : ad0002          >            lda test_case   ;previous test
        17f3 : c916            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        17f5 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0017 =                 >test_num = test_num + 1
        17f7 : a917            >            lda #test_num   ;*** next tests' number
        17f9 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; indexed wraparound test (only zp should wrap)
        17fc : a2fd                     ldx #3+$fa
        17fe : b519             tldax6  lda zp1-$fa&$ff,x   ;wrap on indexed zp
        1800 : 9d0901                   sta abst-$fa,x      ;no STX abs,x!
        1803 : ca                       dex
        1804 : e0fa                     cpx #$fa
        1806 : b0f6                     bcs tldax6                  
        1808 : a2fd                     ldx #3+$fa
        180a : bd1d01           tldax7  lda abs1-$fa,x      ;no wrap on indexed abs
        180d : 9512                     sta zpt-$fa&$ff,x
        180f : ca                       dex
        1810 : e0fa                     cpx #$fa
        1812 : b0f6                     bcs tldax7

        1814 : a203                     ldx #3      ;testing wraparound result
        1816 : a000                     ldy #0
        1818 : b50c             tstax1  lda zpt,x
        181a : d513                     cmp zp1,x
                                        trap_ne     ;store to zp,x data
        181c : d0fe            >        bne *           ;failed not equal (non zero)

        181e : 940c                     sty zpt,x   ;clear                
        1820 : bd0302                   lda abst,x
        1823 : dd1702                   cmp abs1,x
                                        trap_ne     ;store to abs,x data
        1826 : d0fe            >        bne *           ;failed not equal (non zero)

        1828 : 8a                       txa
        1829 : 9d0302                   sta abst,x  ;clear                
        182c : ca                       dex
        182d : 10e9                     bpl tstax1

        182f : a0fb                     ldy #3+$f8
        1831 : a2fe                     ldx #6+$f8
        1833 : a12c             tlday4  lda (ind1-$f8&$ff,x) ;wrap on indexed zp indirect
        1835 : 990b01                   sta abst-$f8,y
        1838 : ca                       dex
        1839 : ca                       dex
        183a : 88                       dey
        183b : c0f8                     cpy #$f8
        183d : b0f4                     bcs tlday4
        183f : a003                     ldy #3      ;testing wraparound result
        1841 : a200                     ldx #0
        1843 : b90302           tstay4  lda abst,y
        1846 : d91702                   cmp abs1,y
                                        trap_ne     ;store to abs data
        1849 : d0fe            >        bne *           ;failed not equal (non zero)

        184b : 8a                       txa
        184c : 990302                   sta abst,y  ;clear                
        184f : 88                       dey
        1850 : 10f1                     bpl tstay4

        1852 : a0fb                     ldy #3+$f8
        1854 : b91f01           tlday5  lda abs1-$f8,y  ;no wrap on indexed abs
        1857 : 9138                     sta (inwt),y
        1859 : 88                       dey
        185a : c0f8                     cpy #$f8
        185c : b0f6                     bcs tlday5                  
        185e : a003                     ldy #3      ;testing wraparound result
        1860 : a200                     ldx #0
        1862 : b90302           tstay5  lda abst,y
        1865 : d91702                   cmp abs1,y
                                        trap_ne     ;store to abs data
        1868 : d0fe            >        bne *           ;failed not equal (non zero)

        186a : 8a                       txa
        186b : 990302                   sta abst,y  ;clear                
        186e : 88                       dey
        186f : 10f1                     bpl tstay5

        1871 : a0fb                     ldy #3+$f8
        1873 : a2fe                     ldx #6+$f8
        1875 : b12e             tlday6  lda (inw1),y    ;no wrap on zp indirect indexed 
        1877 : 8138                     sta (indt-$f8&$ff,x)
        1879 : ca                       dex
        187a : ca                       dex
        187b : 88                       dey
        187c : c0f8                     cpy #$f8
        187e : b0f5                     bcs tlday6
        1880 : a003                     ldy #3      ;testing wraparound result
        1882 : a200                     ldx #0
        1884 : b90302           tstay6  lda abst,y
        1887 : d91702                   cmp abs1,y
                                        trap_ne     ;store to abs data
        188a : d0fe            >        bne *           ;failed not equal (non zero)

        188c : 8a                       txa
        188d : 990302                   sta abst,y  ;clear                
        1890 : 88                       dey
        1891 : 10f1                     bpl tstay6
                                        next_test
        1893 : ad0002          >            lda test_case   ;previous test
        1896 : c917            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        1898 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0018 =                 >test_num = test_num + 1
        189a : a918            >            lda #test_num   ;*** next tests' number
        189c : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; LDA / STA - zp / abs / #
                                        set_stat 0  
                               >            load_flag 0  
        189f : a900            >            lda #0               ;allow test to change I-flag (no mask)
                               >
        18a1 : 48              >            pha         ;use stack to load status
        18a2 : 28              >            plp

        18a3 : a513                     lda zp1
        18a5 : 08                       php         ;test stores do not alter flags
        18a6 : 49c3                     eor #$c3
        18a8 : 28                       plp
        18a9 : 8d0302                   sta abst
        18ac : 08                       php         ;flags after load/store sequence
        18ad : 49c3                     eor #$c3
        18af : c9c3                     cmp #$c3    ;test result
                                        trap_ne
        18b1 : d0fe            >        bne *           ;failed not equal (non zero)

        18b3 : 68                       pla         ;load status
                                        eor_flag 0
        18b4 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        18b6 : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        18b9 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        18bb : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        18bd : 48              >            pha         ;use stack to load status
        18be : 28              >            plp

        18bf : a514                     lda zp1+1
        18c1 : 08                       php         ;test stores do not alter flags
        18c2 : 49c3                     eor #$c3
        18c4 : 28                       plp
        18c5 : 8d0402                   sta abst+1
        18c8 : 08                       php         ;flags after load/store sequence
        18c9 : 49c3                     eor #$c3
        18cb : c982                     cmp #$82    ;test result
                                        trap_ne
        18cd : d0fe            >        bne *           ;failed not equal (non zero)

        18cf : 68                       pla         ;load status
                                        eor_flag 0
        18d0 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        18d2 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        18d5 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        18d7 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        18d9 : 48              >            pha         ;use stack to load status
        18da : 28              >            plp

        18db : a515                     lda zp1+2
        18dd : 08                       php         ;test stores do not alter flags
        18de : 49c3                     eor #$c3
        18e0 : 28                       plp
        18e1 : 8d0502                   sta abst+2
        18e4 : 08                       php         ;flags after load/store sequence
        18e5 : 49c3                     eor #$c3
        18e7 : c941                     cmp #$41    ;test result
                                        trap_ne
        18e9 : d0fe            >        bne *           ;failed not equal (non zero)

        18eb : 68                       pla         ;load status
                                        eor_flag 0
        18ec : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        18ee : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        18f1 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        18f3 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        18f5 : 48              >            pha         ;use stack to load status
        18f6 : 28              >            plp

        18f7 : a516                     lda zp1+3
        18f9 : 08                       php         ;test stores do not alter flags
        18fa : 49c3                     eor #$c3
        18fc : 28                       plp
        18fd : 8d0602                   sta abst+3
        1900 : 08                       php         ;flags after load/store sequence
        1901 : 49c3                     eor #$c3
        1903 : c900                     cmp #0      ;test result
                                        trap_ne
        1905 : d0fe            >        bne *           ;failed not equal (non zero)

        1907 : 68                       pla         ;load status
                                        eor_flag 0
        1908 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        190a : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        190d : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        190f : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1911 : 48              >            pha         ;use stack to load status
        1912 : 28              >            plp

        1913 : a513                     lda zp1  
        1915 : 08                       php         ;test stores do not alter flags
        1916 : 49c3                     eor #$c3
        1918 : 28                       plp
        1919 : 8d0302                   sta abst  
        191c : 08                       php         ;flags after load/store sequence
        191d : 49c3                     eor #$c3
        191f : c9c3                     cmp #$c3    ;test result
                                        trap_ne
        1921 : d0fe            >        bne *           ;failed not equal (non zero)

        1923 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1924 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1926 : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        1929 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        192b : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        192d : 48              >            pha         ;use stack to load status
        192e : 28              >            plp

        192f : a514                     lda zp1+1
        1931 : 08                       php         ;test stores do not alter flags
        1932 : 49c3                     eor #$c3
        1934 : 28                       plp
        1935 : 8d0402                   sta abst+1
        1938 : 08                       php         ;flags after load/store sequence
        1939 : 49c3                     eor #$c3
        193b : c982                     cmp #$82    ;test result
                                        trap_ne
        193d : d0fe            >        bne *           ;failed not equal (non zero)

        193f : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1940 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1942 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        1945 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        1947 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1949 : 48              >            pha         ;use stack to load status
        194a : 28              >            plp

        194b : a515                     lda zp1+2
        194d : 08                       php         ;test stores do not alter flags
        194e : 49c3                     eor #$c3
        1950 : 28                       plp
        1951 : 8d0502                   sta abst+2
        1954 : 08                       php         ;flags after load/store sequence
        1955 : 49c3                     eor #$c3
        1957 : c941                     cmp #$41    ;test result
                                        trap_ne
        1959 : d0fe            >        bne *           ;failed not equal (non zero)

        195b : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        195c : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        195e : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        1961 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        1963 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1965 : 48              >            pha         ;use stack to load status
        1966 : 28              >            plp

        1967 : a516                     lda zp1+3
        1969 : 08                       php         ;test stores do not alter flags
        196a : 49c3                     eor #$c3
        196c : 28                       plp
        196d : 8d0602                   sta abst+3
        1970 : 08                       php         ;flags after load/store sequence
        1971 : 49c3                     eor #$c3
        1973 : c900                     cmp #0      ;test result
                                        trap_ne
        1975 : d0fe            >        bne *           ;failed not equal (non zero)

        1977 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1978 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        197a : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        197d : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        197f : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1981 : 48              >            pha         ;use stack to load status
        1982 : 28              >            plp

        1983 : ad1702                   lda abs1  
        1986 : 08                       php         ;test stores do not alter flags
        1987 : 49c3                     eor #$c3
        1989 : 28                       plp
        198a : 850c                     sta zpt  
        198c : 08                       php         ;flags after load/store sequence
        198d : 49c3                     eor #$c3
        198f : c513                     cmp zp1     ;test result
                                        trap_ne
        1991 : d0fe            >        bne *           ;failed not equal (non zero)

        1993 : 68                       pla         ;load status
                                        eor_flag 0
        1994 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1996 : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        1999 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        199b : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        199d : 48              >            pha         ;use stack to load status
        199e : 28              >            plp

        199f : ad1802                   lda abs1+1
        19a2 : 08                       php         ;test stores do not alter flags
        19a3 : 49c3                     eor #$c3
        19a5 : 28                       plp
        19a6 : 850d                     sta zpt+1
        19a8 : 08                       php         ;flags after load/store sequence
        19a9 : 49c3                     eor #$c3
        19ab : c514                     cmp zp1+1   ;test result
                                        trap_ne
        19ad : d0fe            >        bne *           ;failed not equal (non zero)

        19af : 68                       pla         ;load status
                                        eor_flag 0
        19b0 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        19b2 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        19b5 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        19b7 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        19b9 : 48              >            pha         ;use stack to load status
        19ba : 28              >            plp

        19bb : ad1902                   lda abs1+2
        19be : 08                       php         ;test stores do not alter flags
        19bf : 49c3                     eor #$c3
        19c1 : 28                       plp
        19c2 : 850e                     sta zpt+2
        19c4 : 08                       php         ;flags after load/store sequence
        19c5 : 49c3                     eor #$c3
        19c7 : c515                     cmp zp1+2   ;test result
                                        trap_ne
        19c9 : d0fe            >        bne *           ;failed not equal (non zero)

        19cb : 68                       pla         ;load status
                                        eor_flag 0
        19cc : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        19ce : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        19d1 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        19d3 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        19d5 : 48              >            pha         ;use stack to load status
        19d6 : 28              >            plp

        19d7 : ad1a02                   lda abs1+3
        19da : 08                       php         ;test stores do not alter flags
        19db : 49c3                     eor #$c3
        19dd : 28                       plp
        19de : 850f                     sta zpt+3
        19e0 : 08                       php         ;flags after load/store sequence
        19e1 : 49c3                     eor #$c3
        19e3 : c516                     cmp zp1+3   ;test result
                                        trap_ne
        19e5 : d0fe            >        bne *           ;failed not equal (non zero)

        19e7 : 68                       pla         ;load status
                                        eor_flag 0
        19e8 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        19ea : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        19ed : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        19ef : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        19f1 : 48              >            pha         ;use stack to load status
        19f2 : 28              >            plp

        19f3 : ad1702                   lda abs1  
        19f6 : 08                       php         ;test stores do not alter flags
        19f7 : 49c3                     eor #$c3
        19f9 : 28                       plp
        19fa : 850c                     sta zpt  
        19fc : 08                       php         ;flags after load/store sequence
        19fd : 49c3                     eor #$c3
        19ff : c513                     cmp zp1     ;test result
                                        trap_ne
        1a01 : d0fe            >        bne *           ;failed not equal (non zero)

        1a03 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1a04 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1a06 : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        1a09 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        1a0b : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1a0d : 48              >            pha         ;use stack to load status
        1a0e : 28              >            plp

        1a0f : ad1802                   lda abs1+1
        1a12 : 08                       php         ;test stores do not alter flags
        1a13 : 49c3                     eor #$c3
        1a15 : 28                       plp
        1a16 : 850d                     sta zpt+1
        1a18 : 08                       php         ;flags after load/store sequence
        1a19 : 49c3                     eor #$c3
        1a1b : c514                     cmp zp1+1   ;test result
                                        trap_ne
        1a1d : d0fe            >        bne *           ;failed not equal (non zero)

        1a1f : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1a20 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1a22 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        1a25 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        1a27 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1a29 : 48              >            pha         ;use stack to load status
        1a2a : 28              >            plp

        1a2b : ad1902                   lda abs1+2
        1a2e : 08                       php         ;test stores do not alter flags
        1a2f : 49c3                     eor #$c3
        1a31 : 28                       plp
        1a32 : 850e                     sta zpt+2
        1a34 : 08                       php         ;flags after load/store sequence
        1a35 : 49c3                     eor #$c3
        1a37 : c515                     cmp zp1+2   ;test result
                                        trap_ne
        1a39 : d0fe            >        bne *           ;failed not equal (non zero)

        1a3b : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1a3c : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1a3e : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        1a41 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        1a43 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1a45 : 48              >            pha         ;use stack to load status
        1a46 : 28              >            plp

        1a47 : ad1a02                   lda abs1+3
        1a4a : 08                       php         ;test stores do not alter flags
        1a4b : 49c3                     eor #$c3
        1a4d : 28                       plp
        1a4e : 850f                     sta zpt+3
        1a50 : 08                       php         ;flags after load/store sequence
        1a51 : 49c3                     eor #$c3
        1a53 : c516                     cmp zp1+3   ;test result
                                        trap_ne
        1a55 : d0fe            >        bne *           ;failed not equal (non zero)

        1a57 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1a58 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1a5a : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        1a5d : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0  
                               >            load_flag 0  
        1a5f : a900            >            lda #0               ;allow test to change I-flag (no mask)
                               >
        1a61 : 48              >            pha         ;use stack to load status
        1a62 : 28              >            plp

        1a63 : a9c3                     lda #$c3
        1a65 : 08                       php
        1a66 : cd1702                   cmp abs1    ;test result
                                        trap_ne
        1a69 : d0fe            >        bne *           ;failed not equal (non zero)

        1a6b : 68                       pla         ;load status
                                        eor_flag 0
        1a6c : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1a6e : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        1a71 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1a73 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1a75 : 48              >            pha         ;use stack to load status
        1a76 : 28              >            plp

        1a77 : a982                     lda #$82
        1a79 : 08                       php
        1a7a : cd1802                   cmp abs1+1  ;test result
                                        trap_ne
        1a7d : d0fe            >        bne *           ;failed not equal (non zero)

        1a7f : 68                       pla         ;load status
                                        eor_flag 0
        1a80 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1a82 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        1a85 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1a87 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1a89 : 48              >            pha         ;use stack to load status
        1a8a : 28              >            plp

        1a8b : a941                     lda #$41
        1a8d : 08                       php
        1a8e : cd1902                   cmp abs1+2  ;test result
                                        trap_ne
        1a91 : d0fe            >        bne *           ;failed not equal (non zero)

        1a93 : 68                       pla         ;load status
                                        eor_flag 0
        1a94 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1a96 : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        1a99 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat 0
                               >            load_flag 0
        1a9b : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1a9d : 48              >            pha         ;use stack to load status
        1a9e : 28              >            plp

        1a9f : a900                     lda #0
        1aa1 : 08                       php
        1aa2 : cd1a02                   cmp abs1+3  ;test result
                                        trap_ne
        1aa5 : d0fe            >        bne *           ;failed not equal (non zero)

        1aa7 : 68                       pla         ;load status
                                        eor_flag 0
        1aa8 : 4930            >            eor #0|fao         ;invert expected flags + always on bits

        1aaa : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        1aad : d0fe            >        bne *           ;failed not equal (non zero)


                                        set_stat $ff
                               >            load_flag $ff
        1aaf : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1ab1 : 48              >            pha         ;use stack to load status
        1ab2 : 28              >            plp

        1ab3 : a9c3                     lda #$c3  
        1ab5 : 08                       php
        1ab6 : cd1702                   cmp abs1    ;test result
                                        trap_ne
        1ab9 : d0fe            >        bne *           ;failed not equal (non zero)

        1abb : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1abc : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1abe : cd1c02                   cmp fLDx    ;test flags
                                        trap_ne
        1ac1 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        1ac3 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1ac5 : 48              >            pha         ;use stack to load status
        1ac6 : 28              >            plp

        1ac7 : a982                     lda #$82
        1ac9 : 08                       php
        1aca : cd1802                   cmp abs1+1  ;test result
                                        trap_ne
        1acd : d0fe            >        bne *           ;failed not equal (non zero)

        1acf : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1ad0 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1ad2 : cd1d02                   cmp fLDx+1  ;test flags
                                        trap_ne
        1ad5 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        1ad7 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1ad9 : 48              >            pha         ;use stack to load status
        1ada : 28              >            plp

        1adb : a941                     lda #$41
        1add : 08                       php
        1ade : cd1902                   cmp abs1+2  ;test result
                                        trap_ne
        1ae1 : d0fe            >        bne *           ;failed not equal (non zero)

        1ae3 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1ae4 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1ae6 : cd1e02                   cmp fLDx+2  ;test flags
                                        trap_ne
        1ae9 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        1aeb : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1aed : 48              >            pha         ;use stack to load status
        1aee : 28              >            plp

        1aef : a900                     lda #0
        1af1 : 08                       php
        1af2 : cd1a02                   cmp abs1+3  ;test result
                                        trap_ne
        1af5 : d0fe            >        bne *           ;failed not equal (non zero)

        1af7 : 68                       pla         ;load status
                                        eor_flag lo~fnz ;mask bits not altered
        1af8 : 497d            >            eor #lo~fnz |fao         ;invert expected flags + always on bits

        1afa : cd1f02                   cmp fLDx+3  ;test flags
                                        trap_ne
        1afd : d0fe            >        bne *           ;failed not equal (non zero)


        1aff : a200                     ldx #0
        1b01 : a50c                     lda zpt  
        1b03 : 49c3                     eor #$c3
        1b05 : c513                     cmp zp1  
                                        trap_ne     ;store to zp data
        1b07 : d0fe            >        bne *           ;failed not equal (non zero)

        1b09 : 860c                     stx zpt     ;clear                
        1b0b : ad0302                   lda abst  
        1b0e : 49c3                     eor #$c3
        1b10 : cd1702                   cmp abs1  
                                        trap_ne     ;store to abs data
        1b13 : d0fe            >        bne *           ;failed not equal (non zero)

        1b15 : 8e0302                   stx abst    ;clear                
        1b18 : a50d                     lda zpt+1
        1b1a : 49c3                     eor #$c3
        1b1c : c514                     cmp zp1+1
                                        trap_ne     ;store to zp data
        1b1e : d0fe            >        bne *           ;failed not equal (non zero)

        1b20 : 860d                     stx zpt+1   ;clear                
        1b22 : ad0402                   lda abst+1
        1b25 : 49c3                     eor #$c3
        1b27 : cd1802                   cmp abs1+1
                                        trap_ne     ;store to abs data
        1b2a : d0fe            >        bne *           ;failed not equal (non zero)

        1b2c : 8e0402                   stx abst+1  ;clear                
        1b2f : a50e                     lda zpt+2
        1b31 : 49c3                     eor #$c3
        1b33 : c515                     cmp zp1+2
                                        trap_ne     ;store to zp data
        1b35 : d0fe            >        bne *           ;failed not equal (non zero)

        1b37 : 860e                     stx zpt+2   ;clear                
        1b39 : ad0502                   lda abst+2
        1b3c : 49c3                     eor #$c3
        1b3e : cd1902                   cmp abs1+2
                                        trap_ne     ;store to abs data
        1b41 : d0fe            >        bne *           ;failed not equal (non zero)

        1b43 : 8e0502                   stx abst+2  ;clear                
        1b46 : a50f                     lda zpt+3
        1b48 : 49c3                     eor #$c3
        1b4a : c516                     cmp zp1+3
                                        trap_ne     ;store to zp data
        1b4c : d0fe            >        bne *           ;failed not equal (non zero)

        1b4e : 860f                     stx zpt+3   ;clear                
        1b50 : ad0602                   lda abst+3
        1b53 : 49c3                     eor #$c3
        1b55 : cd1a02                   cmp abs1+3
                                        trap_ne     ;store to abs data
        1b58 : d0fe            >        bne *           ;failed not equal (non zero)

        1b5a : 8e0602                   stx abst+3  ;clear                
                                        next_test
        1b5d : ad0002          >            lda test_case   ;previous test
        1b60 : c918            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        1b62 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0019 =                 >test_num = test_num + 1
        1b64 : a919            >            lda #test_num   ;*** next tests' number
        1b66 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; testing bit test & compares BIT CPX CPY CMP all addressing modes
                                ; BIT - zp / abs
                                        set_a $ff,0
                               >            load_flag 0
        1b69 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1b6b : 48              >            pha         ;use stack to load status
        1b6c : a9ff            >            lda #$ff     ;precharge accu
        1b6e : 28              >            plp

        1b6f : 2416                     bit zp1+3   ;00 - should set Z / clear  NV
                                        tst_a $ff,fz 
        1b71 : 08              >            php         ;save flags
        1b72 : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        1b74 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1b76 : 68              >            pla         ;load status
        1b77 : 48              >            pha
                               >            cmp_flag fz 
        1b78 : c932            >            cmp #(fz |fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1b7a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1b7c : 28              >            plp         ;restore status

                                        set_a 1,0
                               >            load_flag 0
        1b7d : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1b7f : 48              >            pha         ;use stack to load status
        1b80 : a901            >            lda #1     ;precharge accu
        1b82 : 28              >            plp

        1b83 : 2415                     bit zp1+2   ;41 - should set V (M6) / clear NZ
                                        tst_a 1,fv
        1b85 : 08              >            php         ;save flags
        1b86 : c901            >            cmp #1     ;test result
                               >            trap_ne
        1b88 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1b8a : 68              >            pla         ;load status
        1b8b : 48              >            pha
                               >            cmp_flag fv
        1b8c : c970            >            cmp #(fv|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1b8e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1b90 : 28              >            plp         ;restore status

                                        set_a 1,0
                               >            load_flag 0
        1b91 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1b93 : 48              >            pha         ;use stack to load status
        1b94 : a901            >            lda #1     ;precharge accu
        1b96 : 28              >            plp

        1b97 : 2414                     bit zp1+1   ;82 - should set N (M7) & Z / clear V
                                        tst_a 1,fnz
        1b99 : 08              >            php         ;save flags
        1b9a : c901            >            cmp #1     ;test result
                               >            trap_ne
        1b9c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1b9e : 68              >            pla         ;load status
        1b9f : 48              >            pha
                               >            cmp_flag fnz
        1ba0 : c9b2            >            cmp #(fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1ba2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1ba4 : 28              >            plp         ;restore status

                                        set_a 1,0
                               >            load_flag 0
        1ba5 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1ba7 : 48              >            pha         ;use stack to load status
        1ba8 : a901            >            lda #1     ;precharge accu
        1baa : 28              >            plp

        1bab : 2413                     bit zp1     ;c3 - should set N (M7) & V (M6) / clear Z
                                        tst_a 1,fnv
        1bad : 08              >            php         ;save flags
        1bae : c901            >            cmp #1     ;test result
                               >            trap_ne
        1bb0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1bb2 : 68              >            pla         ;load status
        1bb3 : 48              >            pha
                               >            cmp_flag fnv
        1bb4 : c9f0            >            cmp #(fnv|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1bb6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1bb8 : 28              >            plp         ;restore status


                                        set_a $ff,$ff
                               >            load_flag $ff
        1bb9 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1bbb : 48              >            pha         ;use stack to load status
        1bbc : a9ff            >            lda #$ff     ;precharge accu
        1bbe : 28              >            plp

        1bbf : 2416                     bit zp1+3   ;00 - should set Z / clear  NV
                                        tst_a $ff,~fnv 
        1bc1 : 08              >            php         ;save flags
        1bc2 : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        1bc4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1bc6 : 68              >            pla         ;load status
        1bc7 : 48              >            pha
                               >            cmp_flag ~fnv 
        1bc8 : c93f            >            cmp #(~fnv |fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1bca : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1bcc : 28              >            plp         ;restore status

                                        set_a 1,$ff
                               >            load_flag $ff
        1bcd : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1bcf : 48              >            pha         ;use stack to load status
        1bd0 : a901            >            lda #1     ;precharge accu
        1bd2 : 28              >            plp

        1bd3 : 2415                     bit zp1+2   ;41 - should set V (M6) / clear NZ
                                        tst_a 1,~fnz
        1bd5 : 08              >            php         ;save flags
        1bd6 : c901            >            cmp #1     ;test result
                               >            trap_ne
        1bd8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1bda : 68              >            pla         ;load status
        1bdb : 48              >            pha
                               >            cmp_flag ~fnz
        1bdc : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1bde : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1be0 : 28              >            plp         ;restore status

                                        set_a 1,$ff
                               >            load_flag $ff
        1be1 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1be3 : 48              >            pha         ;use stack to load status
        1be4 : a901            >            lda #1     ;precharge accu
        1be6 : 28              >            plp

        1be7 : 2414                     bit zp1+1   ;82 - should set N (M7) & Z / clear V
                                        tst_a 1,~fv
        1be9 : 08              >            php         ;save flags
        1bea : c901            >            cmp #1     ;test result
                               >            trap_ne
        1bec : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1bee : 68              >            pla         ;load status
        1bef : 48              >            pha
                               >            cmp_flag ~fv
        1bf0 : c9bf            >            cmp #(~fv|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1bf2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1bf4 : 28              >            plp         ;restore status

                                        set_a 1,$ff
                               >            load_flag $ff
        1bf5 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1bf7 : 48              >            pha         ;use stack to load status
        1bf8 : a901            >            lda #1     ;precharge accu
        1bfa : 28              >            plp

        1bfb : 2413                     bit zp1     ;c3 - should set N (M7) & V (M6) / clear Z
                                        tst_a 1,~fz
        1bfd : 08              >            php         ;save flags
        1bfe : c901            >            cmp #1     ;test result
                               >            trap_ne
        1c00 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c02 : 68              >            pla         ;load status
        1c03 : 48              >            pha
                               >            cmp_flag ~fz
        1c04 : c9fd            >            cmp #(~fz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1c06 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c08 : 28              >            plp         ;restore status


                                        set_a $ff,0
                               >            load_flag 0
        1c09 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1c0b : 48              >            pha         ;use stack to load status
        1c0c : a9ff            >            lda #$ff     ;precharge accu
        1c0e : 28              >            plp

        1c0f : 2c1a02                   bit abs1+3  ;00 - should set Z / clear  NV
                                        tst_a $ff,fz 
        1c12 : 08              >            php         ;save flags
        1c13 : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        1c15 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c17 : 68              >            pla         ;load status
        1c18 : 48              >            pha
                               >            cmp_flag fz 
        1c19 : c932            >            cmp #(fz |fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1c1b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c1d : 28              >            plp         ;restore status

                                        set_a 1,0
                               >            load_flag 0
        1c1e : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1c20 : 48              >            pha         ;use stack to load status
        1c21 : a901            >            lda #1     ;precharge accu
        1c23 : 28              >            plp

        1c24 : 2c1902                   bit abs1+2  ;41 - should set V (M6) / clear NZ
                                        tst_a 1,fv
        1c27 : 08              >            php         ;save flags
        1c28 : c901            >            cmp #1     ;test result
                               >            trap_ne
        1c2a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c2c : 68              >            pla         ;load status
        1c2d : 48              >            pha
                               >            cmp_flag fv
        1c2e : c970            >            cmp #(fv|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1c30 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c32 : 28              >            plp         ;restore status

                                        set_a 1,0
                               >            load_flag 0
        1c33 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1c35 : 48              >            pha         ;use stack to load status
        1c36 : a901            >            lda #1     ;precharge accu
        1c38 : 28              >            plp

        1c39 : 2c1802                   bit abs1+1  ;82 - should set N (M7) & Z / clear V
                                        tst_a 1,fnz
        1c3c : 08              >            php         ;save flags
        1c3d : c901            >            cmp #1     ;test result
                               >            trap_ne
        1c3f : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c41 : 68              >            pla         ;load status
        1c42 : 48              >            pha
                               >            cmp_flag fnz
        1c43 : c9b2            >            cmp #(fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1c45 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c47 : 28              >            plp         ;restore status

                                        set_a 1,0
                               >            load_flag 0
        1c48 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1c4a : 48              >            pha         ;use stack to load status
        1c4b : a901            >            lda #1     ;precharge accu
        1c4d : 28              >            plp

        1c4e : 2c1702                   bit abs1    ;c3 - should set N (M7) & V (M6) / clear Z
                                        tst_a 1,fnv
        1c51 : 08              >            php         ;save flags
        1c52 : c901            >            cmp #1     ;test result
                               >            trap_ne
        1c54 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c56 : 68              >            pla         ;load status
        1c57 : 48              >            pha
                               >            cmp_flag fnv
        1c58 : c9f0            >            cmp #(fnv|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1c5a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c5c : 28              >            plp         ;restore status


                                        set_a $ff,$ff
                               >            load_flag $ff
        1c5d : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1c5f : 48              >            pha         ;use stack to load status
        1c60 : a9ff            >            lda #$ff     ;precharge accu
        1c62 : 28              >            plp

        1c63 : 2c1a02                   bit abs1+3  ;00 - should set Z / clear  NV
                                        tst_a $ff,~fnv 
        1c66 : 08              >            php         ;save flags
        1c67 : c9ff            >            cmp #$ff     ;test result
                               >            trap_ne
        1c69 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c6b : 68              >            pla         ;load status
        1c6c : 48              >            pha
                               >            cmp_flag ~fnv 
        1c6d : c93f            >            cmp #(~fnv |fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1c6f : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c71 : 28              >            plp         ;restore status

                                        set_a 1,$ff
                               >            load_flag $ff
        1c72 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1c74 : 48              >            pha         ;use stack to load status
        1c75 : a901            >            lda #1     ;precharge accu
        1c77 : 28              >            plp

        1c78 : 2c1902                   bit abs1+2  ;41 - should set V (M6) / clear NZ
                                        tst_a 1,~fnz
        1c7b : 08              >            php         ;save flags
        1c7c : c901            >            cmp #1     ;test result
                               >            trap_ne
        1c7e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c80 : 68              >            pla         ;load status
        1c81 : 48              >            pha
                               >            cmp_flag ~fnz
        1c82 : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1c84 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c86 : 28              >            plp         ;restore status

                                        set_a 1,$ff
                               >            load_flag $ff
        1c87 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1c89 : 48              >            pha         ;use stack to load status
        1c8a : a901            >            lda #1     ;precharge accu
        1c8c : 28              >            plp

        1c8d : 2c1802                   bit abs1+1  ;82 - should set N (M7) & Z / clear V
                                        tst_a 1,~fv
        1c90 : 08              >            php         ;save flags
        1c91 : c901            >            cmp #1     ;test result
                               >            trap_ne
        1c93 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c95 : 68              >            pla         ;load status
        1c96 : 48              >            pha
                               >            cmp_flag ~fv
        1c97 : c9bf            >            cmp #(~fv|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1c99 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1c9b : 28              >            plp         ;restore status

                                        set_a 1,$ff
                               >            load_flag $ff
        1c9c : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1c9e : 48              >            pha         ;use stack to load status
        1c9f : a901            >            lda #1     ;precharge accu
        1ca1 : 28              >            plp

        1ca2 : 2c1702                   bit abs1    ;c3 - should set N (M7) & V (M6) / clear Z
                                        tst_a 1,~fz
        1ca5 : 08              >            php         ;save flags
        1ca6 : c901            >            cmp #1     ;test result
                               >            trap_ne
        1ca8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1caa : 68              >            pla         ;load status
        1cab : 48              >            pha
                               >            cmp_flag ~fz
        1cac : c9fd            >            cmp #(~fz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1cae : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1cb0 : 28              >            plp         ;restore status

                                        next_test
        1cb1 : ad0002          >            lda test_case   ;previous test
        1cb4 : c919            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        1cb6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        001a =                 >test_num = test_num + 1
        1cb8 : a91a            >            lda #test_num   ;*** next tests' number
        1cba : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; CPX - zp / abs / #         
                                        set_x $80,0
                               >            load_flag 0
        1cbd : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1cbf : 48              >            pha         ;use stack to load status
        1cc0 : a280            >            ldx #$80     ;precharge index x
        1cc2 : 28              >            plp

        1cc3 : e417                     cpx zp7f
                                        tst_stat fc
        1cc5 : 08              >            php         ;save status
        1cc6 : 68              >            pla         ;use stack to retrieve status
        1cc7 : 48              >            pha
                               >            cmp_flag fc
        1cc8 : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1cca : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1ccc : 28              >            plp         ;restore status

        1ccd : ca                       dex
        1cce : e417                     cpx zp7f
                                        tst_stat fzc
        1cd0 : 08              >            php         ;save status
        1cd1 : 68              >            pla         ;use stack to retrieve status
        1cd2 : 48              >            pha
                               >            cmp_flag fzc
        1cd3 : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1cd5 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1cd7 : 28              >            plp         ;restore status

        1cd8 : ca                       dex
        1cd9 : e417                     cpx zp7f
                                        tst_x $7e,fn
        1cdb : 08              >            php         ;save flags
        1cdc : e07e            >            cpx #$7e     ;test result
                               >            trap_ne
        1cde : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1ce0 : 68              >            pla         ;load status
        1ce1 : 48              >            pha
                               >            cmp_flag fn
        1ce2 : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1ce4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1ce6 : 28              >            plp         ;restore status

                                        set_x $80,$ff
                               >            load_flag $ff
        1ce7 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1ce9 : 48              >            pha         ;use stack to load status
        1cea : a280            >            ldx #$80     ;precharge index x
        1cec : 28              >            plp

        1ced : e417                     cpx zp7f
                                        tst_stat ~fnz
        1cef : 08              >            php         ;save status
        1cf0 : 68              >            pla         ;use stack to retrieve status
        1cf1 : 48              >            pha
                               >            cmp_flag ~fnz
        1cf2 : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1cf4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1cf6 : 28              >            plp         ;restore status

        1cf7 : ca                       dex
        1cf8 : e417                     cpx zp7f
                                        tst_stat ~fn
        1cfa : 08              >            php         ;save status
        1cfb : 68              >            pla         ;use stack to retrieve status
        1cfc : 48              >            pha
                               >            cmp_flag ~fn
        1cfd : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1cff : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d01 : 28              >            plp         ;restore status

        1d02 : ca                       dex
        1d03 : e417                     cpx zp7f
                                        tst_x $7e,~fzc
        1d05 : 08              >            php         ;save flags
        1d06 : e07e            >            cpx #$7e     ;test result
                               >            trap_ne
        1d08 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d0a : 68              >            pla         ;load status
        1d0b : 48              >            pha
                               >            cmp_flag ~fzc
        1d0c : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1d0e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d10 : 28              >            plp         ;restore status


                                        set_x $80,0
                               >            load_flag 0
        1d11 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1d13 : 48              >            pha         ;use stack to load status
        1d14 : a280            >            ldx #$80     ;precharge index x
        1d16 : 28              >            plp

        1d17 : ec1b02                   cpx abs7f
                                        tst_stat fc
        1d1a : 08              >            php         ;save status
        1d1b : 68              >            pla         ;use stack to retrieve status
        1d1c : 48              >            pha
                               >            cmp_flag fc
        1d1d : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1d1f : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d21 : 28              >            plp         ;restore status

        1d22 : ca                       dex
        1d23 : ec1b02                   cpx abs7f
                                        tst_stat fzc
        1d26 : 08              >            php         ;save status
        1d27 : 68              >            pla         ;use stack to retrieve status
        1d28 : 48              >            pha
                               >            cmp_flag fzc
        1d29 : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1d2b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d2d : 28              >            plp         ;restore status

        1d2e : ca                       dex
        1d2f : ec1b02                   cpx abs7f
                                        tst_x $7e,fn
        1d32 : 08              >            php         ;save flags
        1d33 : e07e            >            cpx #$7e     ;test result
                               >            trap_ne
        1d35 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d37 : 68              >            pla         ;load status
        1d38 : 48              >            pha
                               >            cmp_flag fn
        1d39 : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1d3b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d3d : 28              >            plp         ;restore status

                                        set_x $80,$ff
                               >            load_flag $ff
        1d3e : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1d40 : 48              >            pha         ;use stack to load status
        1d41 : a280            >            ldx #$80     ;precharge index x
        1d43 : 28              >            plp

        1d44 : ec1b02                   cpx abs7f
                                        tst_stat ~fnz
        1d47 : 08              >            php         ;save status
        1d48 : 68              >            pla         ;use stack to retrieve status
        1d49 : 48              >            pha
                               >            cmp_flag ~fnz
        1d4a : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1d4c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d4e : 28              >            plp         ;restore status

        1d4f : ca                       dex
        1d50 : ec1b02                   cpx abs7f
                                        tst_stat ~fn
        1d53 : 08              >            php         ;save status
        1d54 : 68              >            pla         ;use stack to retrieve status
        1d55 : 48              >            pha
                               >            cmp_flag ~fn
        1d56 : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1d58 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d5a : 28              >            plp         ;restore status

        1d5b : ca                       dex
        1d5c : ec1b02                   cpx abs7f
                                        tst_x $7e,~fzc
        1d5f : 08              >            php         ;save flags
        1d60 : e07e            >            cpx #$7e     ;test result
                               >            trap_ne
        1d62 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d64 : 68              >            pla         ;load status
        1d65 : 48              >            pha
                               >            cmp_flag ~fzc
        1d66 : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1d68 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d6a : 28              >            plp         ;restore status


                                        set_x $80,0
                               >            load_flag 0
        1d6b : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1d6d : 48              >            pha         ;use stack to load status
        1d6e : a280            >            ldx #$80     ;precharge index x
        1d70 : 28              >            plp

        1d71 : e07f                     cpx #$7f
                                        tst_stat fc
        1d73 : 08              >            php         ;save status
        1d74 : 68              >            pla         ;use stack to retrieve status
        1d75 : 48              >            pha
                               >            cmp_flag fc
        1d76 : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1d78 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d7a : 28              >            plp         ;restore status

        1d7b : ca                       dex
        1d7c : e07f                     cpx #$7f
                                        tst_stat fzc
        1d7e : 08              >            php         ;save status
        1d7f : 68              >            pla         ;use stack to retrieve status
        1d80 : 48              >            pha
                               >            cmp_flag fzc
        1d81 : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1d83 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d85 : 28              >            plp         ;restore status

        1d86 : ca                       dex
        1d87 : e07f                     cpx #$7f
                                        tst_x $7e,fn
        1d89 : 08              >            php         ;save flags
        1d8a : e07e            >            cpx #$7e     ;test result
                               >            trap_ne
        1d8c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d8e : 68              >            pla         ;load status
        1d8f : 48              >            pha
                               >            cmp_flag fn
        1d90 : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1d92 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1d94 : 28              >            plp         ;restore status

                                        set_x $80,$ff
                               >            load_flag $ff
        1d95 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1d97 : 48              >            pha         ;use stack to load status
        1d98 : a280            >            ldx #$80     ;precharge index x
        1d9a : 28              >            plp

        1d9b : e07f                     cpx #$7f
                                        tst_stat ~fnz
        1d9d : 08              >            php         ;save status
        1d9e : 68              >            pla         ;use stack to retrieve status
        1d9f : 48              >            pha
                               >            cmp_flag ~fnz
        1da0 : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1da2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1da4 : 28              >            plp         ;restore status

        1da5 : ca                       dex
        1da6 : e07f                     cpx #$7f
                                        tst_stat ~fn
        1da8 : 08              >            php         ;save status
        1da9 : 68              >            pla         ;use stack to retrieve status
        1daa : 48              >            pha
                               >            cmp_flag ~fn
        1dab : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1dad : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1daf : 28              >            plp         ;restore status

        1db0 : ca                       dex
        1db1 : e07f                     cpx #$7f
                                        tst_x $7e,~fzc
        1db3 : 08              >            php         ;save flags
        1db4 : e07e            >            cpx #$7e     ;test result
                               >            trap_ne
        1db6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1db8 : 68              >            pla         ;load status
        1db9 : 48              >            pha
                               >            cmp_flag ~fzc
        1dba : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1dbc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1dbe : 28              >            plp         ;restore status

                                        next_test
        1dbf : ad0002          >            lda test_case   ;previous test
        1dc2 : c91a            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        1dc4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        001b =                 >test_num = test_num + 1
        1dc6 : a91b            >            lda #test_num   ;*** next tests' number
        1dc8 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; CPY - zp / abs / #         
                                        set_y $80,0
                               >            load_flag 0
        1dcb : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1dcd : 48              >            pha         ;use stack to load status
        1dce : a080            >            ldy #$80     ;precharge index y
        1dd0 : 28              >            plp

        1dd1 : c417                     cpy zp7f
                                        tst_stat fc
        1dd3 : 08              >            php         ;save status
        1dd4 : 68              >            pla         ;use stack to retrieve status
        1dd5 : 48              >            pha
                               >            cmp_flag fc
        1dd6 : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1dd8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1dda : 28              >            plp         ;restore status

        1ddb : 88                       dey
        1ddc : c417                     cpy zp7f
                                        tst_stat fzc
        1dde : 08              >            php         ;save status
        1ddf : 68              >            pla         ;use stack to retrieve status
        1de0 : 48              >            pha
                               >            cmp_flag fzc
        1de1 : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1de3 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1de5 : 28              >            plp         ;restore status

        1de6 : 88                       dey
        1de7 : c417                     cpy zp7f
                                        tst_y $7e,fn
        1de9 : 08              >            php         ;save flags
        1dea : c07e            >            cpy #$7e     ;test result
                               >            trap_ne
        1dec : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1dee : 68              >            pla         ;load status
        1def : 48              >            pha
                               >            cmp_flag fn
        1df0 : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1df2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1df4 : 28              >            plp         ;restore status

                                        set_y $80,$ff
                               >            load_flag $ff
        1df5 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1df7 : 48              >            pha         ;use stack to load status
        1df8 : a080            >            ldy #$80     ;precharge index y
        1dfa : 28              >            plp

        1dfb : c417                     cpy zp7f
                                        tst_stat ~fnz
        1dfd : 08              >            php         ;save status
        1dfe : 68              >            pla         ;use stack to retrieve status
        1dff : 48              >            pha
                               >            cmp_flag ~fnz
        1e00 : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1e02 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e04 : 28              >            plp         ;restore status

        1e05 : 88                       dey
        1e06 : c417                     cpy zp7f
                                        tst_stat ~fn
        1e08 : 08              >            php         ;save status
        1e09 : 68              >            pla         ;use stack to retrieve status
        1e0a : 48              >            pha
                               >            cmp_flag ~fn
        1e0b : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1e0d : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e0f : 28              >            plp         ;restore status

        1e10 : 88                       dey
        1e11 : c417                     cpy zp7f
                                        tst_y $7e,~fzc
        1e13 : 08              >            php         ;save flags
        1e14 : c07e            >            cpy #$7e     ;test result
                               >            trap_ne
        1e16 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e18 : 68              >            pla         ;load status
        1e19 : 48              >            pha
                               >            cmp_flag ~fzc
        1e1a : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1e1c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e1e : 28              >            plp         ;restore status


                                        set_y $80,0
                               >            load_flag 0
        1e1f : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1e21 : 48              >            pha         ;use stack to load status
        1e22 : a080            >            ldy #$80     ;precharge index y
        1e24 : 28              >            plp

        1e25 : cc1b02                   cpy abs7f
                                        tst_stat fc
        1e28 : 08              >            php         ;save status
        1e29 : 68              >            pla         ;use stack to retrieve status
        1e2a : 48              >            pha
                               >            cmp_flag fc
        1e2b : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1e2d : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e2f : 28              >            plp         ;restore status

        1e30 : 88                       dey
        1e31 : cc1b02                   cpy abs7f
                                        tst_stat fzc
        1e34 : 08              >            php         ;save status
        1e35 : 68              >            pla         ;use stack to retrieve status
        1e36 : 48              >            pha
                               >            cmp_flag fzc
        1e37 : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1e39 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e3b : 28              >            plp         ;restore status

        1e3c : 88                       dey
        1e3d : cc1b02                   cpy abs7f
                                        tst_y $7e,fn
        1e40 : 08              >            php         ;save flags
        1e41 : c07e            >            cpy #$7e     ;test result
                               >            trap_ne
        1e43 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e45 : 68              >            pla         ;load status
        1e46 : 48              >            pha
                               >            cmp_flag fn
        1e47 : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1e49 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e4b : 28              >            plp         ;restore status

                                        set_y $80,$ff
                               >            load_flag $ff
        1e4c : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1e4e : 48              >            pha         ;use stack to load status
        1e4f : a080            >            ldy #$80     ;precharge index y
        1e51 : 28              >            plp

        1e52 : cc1b02                   cpy abs7f
                                        tst_stat ~fnz
        1e55 : 08              >            php         ;save status
        1e56 : 68              >            pla         ;use stack to retrieve status
        1e57 : 48              >            pha
                               >            cmp_flag ~fnz
        1e58 : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1e5a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e5c : 28              >            plp         ;restore status

        1e5d : 88                       dey
        1e5e : cc1b02                   cpy abs7f
                                        tst_stat ~fn
        1e61 : 08              >            php         ;save status
        1e62 : 68              >            pla         ;use stack to retrieve status
        1e63 : 48              >            pha
                               >            cmp_flag ~fn
        1e64 : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1e66 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e68 : 28              >            plp         ;restore status

        1e69 : 88                       dey
        1e6a : cc1b02                   cpy abs7f
                                        tst_y $7e,~fzc
        1e6d : 08              >            php         ;save flags
        1e6e : c07e            >            cpy #$7e     ;test result
                               >            trap_ne
        1e70 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e72 : 68              >            pla         ;load status
        1e73 : 48              >            pha
                               >            cmp_flag ~fzc
        1e74 : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1e76 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e78 : 28              >            plp         ;restore status


                                        set_y $80,0
                               >            load_flag 0
        1e79 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1e7b : 48              >            pha         ;use stack to load status
        1e7c : a080            >            ldy #$80     ;precharge index y
        1e7e : 28              >            plp

        1e7f : c07f                     cpy #$7f
                                        tst_stat fc
        1e81 : 08              >            php         ;save status
        1e82 : 68              >            pla         ;use stack to retrieve status
        1e83 : 48              >            pha
                               >            cmp_flag fc
        1e84 : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1e86 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e88 : 28              >            plp         ;restore status

        1e89 : 88                       dey
        1e8a : c07f                     cpy #$7f
                                        tst_stat fzc
        1e8c : 08              >            php         ;save status
        1e8d : 68              >            pla         ;use stack to retrieve status
        1e8e : 48              >            pha
                               >            cmp_flag fzc
        1e8f : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1e91 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e93 : 28              >            plp         ;restore status

        1e94 : 88                       dey
        1e95 : c07f                     cpy #$7f
                                        tst_y $7e,fn
        1e97 : 08              >            php         ;save flags
        1e98 : c07e            >            cpy #$7e     ;test result
                               >            trap_ne
        1e9a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1e9c : 68              >            pla         ;load status
        1e9d : 48              >            pha
                               >            cmp_flag fn
        1e9e : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1ea0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1ea2 : 28              >            plp         ;restore status

                                        set_y $80,$ff
                               >            load_flag $ff
        1ea3 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1ea5 : 48              >            pha         ;use stack to load status
        1ea6 : a080            >            ldy #$80     ;precharge index y
        1ea8 : 28              >            plp

        1ea9 : c07f                     cpy #$7f
                                        tst_stat ~fnz
        1eab : 08              >            php         ;save status
        1eac : 68              >            pla         ;use stack to retrieve status
        1ead : 48              >            pha
                               >            cmp_flag ~fnz
        1eae : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1eb0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1eb2 : 28              >            plp         ;restore status

        1eb3 : 88                       dey
        1eb4 : c07f                     cpy #$7f
                                        tst_stat ~fn
        1eb6 : 08              >            php         ;save status
        1eb7 : 68              >            pla         ;use stack to retrieve status
        1eb8 : 48              >            pha
                               >            cmp_flag ~fn
        1eb9 : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1ebb : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1ebd : 28              >            plp         ;restore status

        1ebe : 88                       dey
        1ebf : c07f                     cpy #$7f
                                        tst_y $7e,~fzc
        1ec1 : 08              >            php         ;save flags
        1ec2 : c07e            >            cpy #$7e     ;test result
                               >            trap_ne
        1ec4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1ec6 : 68              >            pla         ;load status
        1ec7 : 48              >            pha
                               >            cmp_flag ~fzc
        1ec8 : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1eca : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1ecc : 28              >            plp         ;restore status

                                        next_test
        1ecd : ad0002          >            lda test_case   ;previous test
        1ed0 : c91b            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        1ed2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        001c =                 >test_num = test_num + 1
        1ed4 : a91c            >            lda #test_num   ;*** next tests' number
        1ed6 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; CMP - zp / abs / #         
                                        set_a $80,0
                               >            load_flag 0
        1ed9 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1edb : 48              >            pha         ;use stack to load status
        1edc : a980            >            lda #$80     ;precharge accu
        1ede : 28              >            plp

        1edf : c517                     cmp zp7f
                                        tst_a $80,fc
        1ee1 : 08              >            php         ;save flags
        1ee2 : c980            >            cmp #$80     ;test result
                               >            trap_ne
        1ee4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1ee6 : 68              >            pla         ;load status
        1ee7 : 48              >            pha
                               >            cmp_flag fc
        1ee8 : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1eea : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1eec : 28              >            plp         ;restore status

                                        set_a $7f,0
                               >            load_flag 0
        1eed : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1eef : 48              >            pha         ;use stack to load status
        1ef0 : a97f            >            lda #$7f     ;precharge accu
        1ef2 : 28              >            plp

        1ef3 : c517                     cmp zp7f
                                        tst_a $7f,fzc
        1ef5 : 08              >            php         ;save flags
        1ef6 : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        1ef8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1efa : 68              >            pla         ;load status
        1efb : 48              >            pha
                               >            cmp_flag fzc
        1efc : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1efe : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f00 : 28              >            plp         ;restore status

                                        set_a $7e,0
                               >            load_flag 0
        1f01 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1f03 : 48              >            pha         ;use stack to load status
        1f04 : a97e            >            lda #$7e     ;precharge accu
        1f06 : 28              >            plp

        1f07 : c517                     cmp zp7f
                                        tst_a $7e,fn
        1f09 : 08              >            php         ;save flags
        1f0a : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        1f0c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f0e : 68              >            pla         ;load status
        1f0f : 48              >            pha
                               >            cmp_flag fn
        1f10 : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1f12 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f14 : 28              >            plp         ;restore status

                                        set_a $80,$ff
                               >            load_flag $ff
        1f15 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1f17 : 48              >            pha         ;use stack to load status
        1f18 : a980            >            lda #$80     ;precharge accu
        1f1a : 28              >            plp

        1f1b : c517                     cmp zp7f
                                        tst_a $80,~fnz
        1f1d : 08              >            php         ;save flags
        1f1e : c980            >            cmp #$80     ;test result
                               >            trap_ne
        1f20 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f22 : 68              >            pla         ;load status
        1f23 : 48              >            pha
                               >            cmp_flag ~fnz
        1f24 : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1f26 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f28 : 28              >            plp         ;restore status

                                        set_a $7f,$ff
                               >            load_flag $ff
        1f29 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1f2b : 48              >            pha         ;use stack to load status
        1f2c : a97f            >            lda #$7f     ;precharge accu
        1f2e : 28              >            plp

        1f2f : c517                     cmp zp7f
                                        tst_a $7f,~fn
        1f31 : 08              >            php         ;save flags
        1f32 : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        1f34 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f36 : 68              >            pla         ;load status
        1f37 : 48              >            pha
                               >            cmp_flag ~fn
        1f38 : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1f3a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f3c : 28              >            plp         ;restore status

                                        set_a $7e,$ff
                               >            load_flag $ff
        1f3d : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1f3f : 48              >            pha         ;use stack to load status
        1f40 : a97e            >            lda #$7e     ;precharge accu
        1f42 : 28              >            plp

        1f43 : c517                     cmp zp7f
                                        tst_a $7e,~fzc
        1f45 : 08              >            php         ;save flags
        1f46 : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        1f48 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f4a : 68              >            pla         ;load status
        1f4b : 48              >            pha
                               >            cmp_flag ~fzc
        1f4c : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1f4e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f50 : 28              >            plp         ;restore status


                                        set_a $80,0
                               >            load_flag 0
        1f51 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1f53 : 48              >            pha         ;use stack to load status
        1f54 : a980            >            lda #$80     ;precharge accu
        1f56 : 28              >            plp

        1f57 : cd1b02                   cmp abs7f
                                        tst_a $80,fc
        1f5a : 08              >            php         ;save flags
        1f5b : c980            >            cmp #$80     ;test result
                               >            trap_ne
        1f5d : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f5f : 68              >            pla         ;load status
        1f60 : 48              >            pha
                               >            cmp_flag fc
        1f61 : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1f63 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f65 : 28              >            plp         ;restore status

                                        set_a $7f,0
                               >            load_flag 0
        1f66 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1f68 : 48              >            pha         ;use stack to load status
        1f69 : a97f            >            lda #$7f     ;precharge accu
        1f6b : 28              >            plp

        1f6c : cd1b02                   cmp abs7f
                                        tst_a $7f,fzc
        1f6f : 08              >            php         ;save flags
        1f70 : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        1f72 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f74 : 68              >            pla         ;load status
        1f75 : 48              >            pha
                               >            cmp_flag fzc
        1f76 : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1f78 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f7a : 28              >            plp         ;restore status

                                        set_a $7e,0
                               >            load_flag 0
        1f7b : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1f7d : 48              >            pha         ;use stack to load status
        1f7e : a97e            >            lda #$7e     ;precharge accu
        1f80 : 28              >            plp

        1f81 : cd1b02                   cmp abs7f
                                        tst_a $7e,fn
        1f84 : 08              >            php         ;save flags
        1f85 : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        1f87 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f89 : 68              >            pla         ;load status
        1f8a : 48              >            pha
                               >            cmp_flag fn
        1f8b : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1f8d : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f8f : 28              >            plp         ;restore status

                                        set_a $80,$ff
                               >            load_flag $ff
        1f90 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1f92 : 48              >            pha         ;use stack to load status
        1f93 : a980            >            lda #$80     ;precharge accu
        1f95 : 28              >            plp

        1f96 : cd1b02                   cmp abs7f
                                        tst_a $80,~fnz
        1f99 : 08              >            php         ;save flags
        1f9a : c980            >            cmp #$80     ;test result
                               >            trap_ne
        1f9c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1f9e : 68              >            pla         ;load status
        1f9f : 48              >            pha
                               >            cmp_flag ~fnz
        1fa0 : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1fa2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1fa4 : 28              >            plp         ;restore status

                                        set_a $7f,$ff
                               >            load_flag $ff
        1fa5 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1fa7 : 48              >            pha         ;use stack to load status
        1fa8 : a97f            >            lda #$7f     ;precharge accu
        1faa : 28              >            plp

        1fab : cd1b02                   cmp abs7f
                                        tst_a $7f,~fn
        1fae : 08              >            php         ;save flags
        1faf : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        1fb1 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1fb3 : 68              >            pla         ;load status
        1fb4 : 48              >            pha
                               >            cmp_flag ~fn
        1fb5 : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1fb7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1fb9 : 28              >            plp         ;restore status

                                        set_a $7e,$ff
                               >            load_flag $ff
        1fba : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        1fbc : 48              >            pha         ;use stack to load status
        1fbd : a97e            >            lda #$7e     ;precharge accu
        1fbf : 28              >            plp

        1fc0 : cd1b02                   cmp abs7f
                                        tst_a $7e,~fzc
        1fc3 : 08              >            php         ;save flags
        1fc4 : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        1fc6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1fc8 : 68              >            pla         ;load status
        1fc9 : 48              >            pha
                               >            cmp_flag ~fzc
        1fca : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1fcc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1fce : 28              >            plp         ;restore status


                                        set_a $80,0
                               >            load_flag 0
        1fcf : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1fd1 : 48              >            pha         ;use stack to load status
        1fd2 : a980            >            lda #$80     ;precharge accu
        1fd4 : 28              >            plp

        1fd5 : c97f                     cmp #$7f
                                        tst_a $80,fc
        1fd7 : 08              >            php         ;save flags
        1fd8 : c980            >            cmp #$80     ;test result
                               >            trap_ne
        1fda : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1fdc : 68              >            pla         ;load status
        1fdd : 48              >            pha
                               >            cmp_flag fc
        1fde : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1fe0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1fe2 : 28              >            plp         ;restore status

                                        set_a $7f,0
                               >            load_flag 0
        1fe3 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1fe5 : 48              >            pha         ;use stack to load status
        1fe6 : a97f            >            lda #$7f     ;precharge accu
        1fe8 : 28              >            plp

        1fe9 : c97f                     cmp #$7f
                                        tst_a $7f,fzc
        1feb : 08              >            php         ;save flags
        1fec : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        1fee : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1ff0 : 68              >            pla         ;load status
        1ff1 : 48              >            pha
                               >            cmp_flag fzc
        1ff2 : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        1ff4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        1ff6 : 28              >            plp         ;restore status

                                        set_a $7e,0
                               >            load_flag 0
        1ff7 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        1ff9 : 48              >            pha         ;use stack to load status
        1ffa : a97e            >            lda #$7e     ;precharge accu
        1ffc : 28              >            plp

        1ffd : c97f                     cmp #$7f
                                        tst_a $7e,fn
        1fff : 08              >            php         ;save flags
        2000 : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        2002 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2004 : 68              >            pla         ;load status
        2005 : 48              >            pha
                               >            cmp_flag fn
        2006 : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2008 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        200a : 28              >            plp         ;restore status

                                        set_a $80,$ff
                               >            load_flag $ff
        200b : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        200d : 48              >            pha         ;use stack to load status
        200e : a980            >            lda #$80     ;precharge accu
        2010 : 28              >            plp

        2011 : c97f                     cmp #$7f
                                        tst_a $80,~fnz
        2013 : 08              >            php         ;save flags
        2014 : c980            >            cmp #$80     ;test result
                               >            trap_ne
        2016 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2018 : 68              >            pla         ;load status
        2019 : 48              >            pha
                               >            cmp_flag ~fnz
        201a : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        201c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        201e : 28              >            plp         ;restore status

                                        set_a $7f,$ff
                               >            load_flag $ff
        201f : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2021 : 48              >            pha         ;use stack to load status
        2022 : a97f            >            lda #$7f     ;precharge accu
        2024 : 28              >            plp

        2025 : c97f                     cmp #$7f
                                        tst_a $7f,~fn
        2027 : 08              >            php         ;save flags
        2028 : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        202a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        202c : 68              >            pla         ;load status
        202d : 48              >            pha
                               >            cmp_flag ~fn
        202e : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2030 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2032 : 28              >            plp         ;restore status

                                        set_a $7e,$ff
                               >            load_flag $ff
        2033 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2035 : 48              >            pha         ;use stack to load status
        2036 : a97e            >            lda #$7e     ;precharge accu
        2038 : 28              >            plp

        2039 : c97f                     cmp #$7f
                                        tst_a $7e,~fzc
        203b : 08              >            php         ;save flags
        203c : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        203e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2040 : 68              >            pla         ;load status
        2041 : 48              >            pha
                               >            cmp_flag ~fzc
        2042 : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2044 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2046 : 28              >            plp         ;restore status


        2047 : a204                     ldx #4          ;with indexing by X
                                        set_a $80,0
                               >            load_flag 0
        2049 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        204b : 48              >            pha         ;use stack to load status
        204c : a980            >            lda #$80     ;precharge accu
        204e : 28              >            plp

        204f : d513                     cmp zp1,x
                                        tst_a $80,fc
        2051 : 08              >            php         ;save flags
        2052 : c980            >            cmp #$80     ;test result
                               >            trap_ne
        2054 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2056 : 68              >            pla         ;load status
        2057 : 48              >            pha
                               >            cmp_flag fc
        2058 : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        205a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        205c : 28              >            plp         ;restore status

                                        set_a $7f,0
                               >            load_flag 0
        205d : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        205f : 48              >            pha         ;use stack to load status
        2060 : a97f            >            lda #$7f     ;precharge accu
        2062 : 28              >            plp

        2063 : d513                     cmp zp1,x
                                        tst_a $7f,fzc
        2065 : 08              >            php         ;save flags
        2066 : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        2068 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        206a : 68              >            pla         ;load status
        206b : 48              >            pha
                               >            cmp_flag fzc
        206c : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        206e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2070 : 28              >            plp         ;restore status

                                        set_a $7e,0
                               >            load_flag 0
        2071 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2073 : 48              >            pha         ;use stack to load status
        2074 : a97e            >            lda #$7e     ;precharge accu
        2076 : 28              >            plp

        2077 : d513                     cmp zp1,x
                                        tst_a $7e,fn
        2079 : 08              >            php         ;save flags
        207a : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        207c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        207e : 68              >            pla         ;load status
        207f : 48              >            pha
                               >            cmp_flag fn
        2080 : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2082 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2084 : 28              >            plp         ;restore status

                                        set_a $80,$ff
                               >            load_flag $ff
        2085 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2087 : 48              >            pha         ;use stack to load status
        2088 : a980            >            lda #$80     ;precharge accu
        208a : 28              >            plp

        208b : d513                     cmp zp1,x
                                        tst_a $80,~fnz
        208d : 08              >            php         ;save flags
        208e : c980            >            cmp #$80     ;test result
                               >            trap_ne
        2090 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2092 : 68              >            pla         ;load status
        2093 : 48              >            pha
                               >            cmp_flag ~fnz
        2094 : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2096 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2098 : 28              >            plp         ;restore status

                                        set_a $7f,$ff
                               >            load_flag $ff
        2099 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        209b : 48              >            pha         ;use stack to load status
        209c : a97f            >            lda #$7f     ;precharge accu
        209e : 28              >            plp

        209f : d513                     cmp zp1,x
                                        tst_a $7f,~fn
        20a1 : 08              >            php         ;save flags
        20a2 : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        20a4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        20a6 : 68              >            pla         ;load status
        20a7 : 48              >            pha
                               >            cmp_flag ~fn
        20a8 : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        20aa : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        20ac : 28              >            plp         ;restore status

                                        set_a $7e,$ff
                               >            load_flag $ff
        20ad : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        20af : 48              >            pha         ;use stack to load status
        20b0 : a97e            >            lda #$7e     ;precharge accu
        20b2 : 28              >            plp

        20b3 : d513                     cmp zp1,x
                                        tst_a $7e,~fzc
        20b5 : 08              >            php         ;save flags
        20b6 : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        20b8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        20ba : 68              >            pla         ;load status
        20bb : 48              >            pha
                               >            cmp_flag ~fzc
        20bc : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        20be : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        20c0 : 28              >            plp         ;restore status


                                        set_a $80,0
                               >            load_flag 0
        20c1 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        20c3 : 48              >            pha         ;use stack to load status
        20c4 : a980            >            lda #$80     ;precharge accu
        20c6 : 28              >            plp

        20c7 : dd1702                   cmp abs1,x
                                        tst_a $80,fc
        20ca : 08              >            php         ;save flags
        20cb : c980            >            cmp #$80     ;test result
                               >            trap_ne
        20cd : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        20cf : 68              >            pla         ;load status
        20d0 : 48              >            pha
                               >            cmp_flag fc
        20d1 : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        20d3 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        20d5 : 28              >            plp         ;restore status

                                        set_a $7f,0
                               >            load_flag 0
        20d6 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        20d8 : 48              >            pha         ;use stack to load status
        20d9 : a97f            >            lda #$7f     ;precharge accu
        20db : 28              >            plp

        20dc : dd1702                   cmp abs1,x
                                        tst_a $7f,fzc
        20df : 08              >            php         ;save flags
        20e0 : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        20e2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        20e4 : 68              >            pla         ;load status
        20e5 : 48              >            pha
                               >            cmp_flag fzc
        20e6 : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        20e8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        20ea : 28              >            plp         ;restore status

                                        set_a $7e,0
                               >            load_flag 0
        20eb : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        20ed : 48              >            pha         ;use stack to load status
        20ee : a97e            >            lda #$7e     ;precharge accu
        20f0 : 28              >            plp

        20f1 : dd1702                   cmp abs1,x
                                        tst_a $7e,fn
        20f4 : 08              >            php         ;save flags
        20f5 : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        20f7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        20f9 : 68              >            pla         ;load status
        20fa : 48              >            pha
                               >            cmp_flag fn
        20fb : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        20fd : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        20ff : 28              >            plp         ;restore status

                                        set_a $80,$ff
                               >            load_flag $ff
        2100 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2102 : 48              >            pha         ;use stack to load status
        2103 : a980            >            lda #$80     ;precharge accu
        2105 : 28              >            plp

        2106 : dd1702                   cmp abs1,x
                                        tst_a $80,~fnz
        2109 : 08              >            php         ;save flags
        210a : c980            >            cmp #$80     ;test result
                               >            trap_ne
        210c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        210e : 68              >            pla         ;load status
        210f : 48              >            pha
                               >            cmp_flag ~fnz
        2110 : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2112 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2114 : 28              >            plp         ;restore status

                                        set_a $7f,$ff
                               >            load_flag $ff
        2115 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2117 : 48              >            pha         ;use stack to load status
        2118 : a97f            >            lda #$7f     ;precharge accu
        211a : 28              >            plp

        211b : dd1702                   cmp abs1,x
                                        tst_a $7f,~fn
        211e : 08              >            php         ;save flags
        211f : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        2121 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2123 : 68              >            pla         ;load status
        2124 : 48              >            pha
                               >            cmp_flag ~fn
        2125 : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2127 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2129 : 28              >            plp         ;restore status

                                        set_a $7e,$ff
                               >            load_flag $ff
        212a : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        212c : 48              >            pha         ;use stack to load status
        212d : a97e            >            lda #$7e     ;precharge accu
        212f : 28              >            plp

        2130 : dd1702                   cmp abs1,x
                                        tst_a $7e,~fzc
        2133 : 08              >            php         ;save flags
        2134 : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        2136 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2138 : 68              >            pla         ;load status
        2139 : 48              >            pha
                               >            cmp_flag ~fzc
        213a : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        213c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        213e : 28              >            plp         ;restore status


        213f : a004                     ldy #4          ;with indexing by Y
        2141 : a208                     ldx #8          ;with indexed indirect
                                        set_a $80,0
                               >            load_flag 0
        2143 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2145 : 48              >            pha         ;use stack to load status
        2146 : a980            >            lda #$80     ;precharge accu
        2148 : 28              >            plp

        2149 : d91702                   cmp abs1,y
                                        tst_a $80,fc
        214c : 08              >            php         ;save flags
        214d : c980            >            cmp #$80     ;test result
                               >            trap_ne
        214f : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2151 : 68              >            pla         ;load status
        2152 : 48              >            pha
                               >            cmp_flag fc
        2153 : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2155 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2157 : 28              >            plp         ;restore status

                                        set_a $7f,0
                               >            load_flag 0
        2158 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        215a : 48              >            pha         ;use stack to load status
        215b : a97f            >            lda #$7f     ;precharge accu
        215d : 28              >            plp

        215e : d91702                   cmp abs1,y
                                        tst_a $7f,fzc
        2161 : 08              >            php         ;save flags
        2162 : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        2164 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2166 : 68              >            pla         ;load status
        2167 : 48              >            pha
                               >            cmp_flag fzc
        2168 : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        216a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        216c : 28              >            plp         ;restore status

                                        set_a $7e,0
                               >            load_flag 0
        216d : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        216f : 48              >            pha         ;use stack to load status
        2170 : a97e            >            lda #$7e     ;precharge accu
        2172 : 28              >            plp

        2173 : d91702                   cmp abs1,y
                                        tst_a $7e,fn
        2176 : 08              >            php         ;save flags
        2177 : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        2179 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        217b : 68              >            pla         ;load status
        217c : 48              >            pha
                               >            cmp_flag fn
        217d : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        217f : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2181 : 28              >            plp         ;restore status

                                        set_a $80,$ff
                               >            load_flag $ff
        2182 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2184 : 48              >            pha         ;use stack to load status
        2185 : a980            >            lda #$80     ;precharge accu
        2187 : 28              >            plp

        2188 : d91702                   cmp abs1,y
                                        tst_a $80,~fnz
        218b : 08              >            php         ;save flags
        218c : c980            >            cmp #$80     ;test result
                               >            trap_ne
        218e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2190 : 68              >            pla         ;load status
        2191 : 48              >            pha
                               >            cmp_flag ~fnz
        2192 : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2194 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2196 : 28              >            plp         ;restore status

                                        set_a $7f,$ff
                               >            load_flag $ff
        2197 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2199 : 48              >            pha         ;use stack to load status
        219a : a97f            >            lda #$7f     ;precharge accu
        219c : 28              >            plp

        219d : d91702                   cmp abs1,y
                                        tst_a $7f,~fn
        21a0 : 08              >            php         ;save flags
        21a1 : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        21a3 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        21a5 : 68              >            pla         ;load status
        21a6 : 48              >            pha
                               >            cmp_flag ~fn
        21a7 : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        21a9 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        21ab : 28              >            plp         ;restore status

                                        set_a $7e,$ff
                               >            load_flag $ff
        21ac : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        21ae : 48              >            pha         ;use stack to load status
        21af : a97e            >            lda #$7e     ;precharge accu
        21b1 : 28              >            plp

        21b2 : d91702                   cmp abs1,y
                                        tst_a $7e,~fzc
        21b5 : 08              >            php         ;save flags
        21b6 : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        21b8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        21ba : 68              >            pla         ;load status
        21bb : 48              >            pha
                               >            cmp_flag ~fzc
        21bc : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        21be : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        21c0 : 28              >            plp         ;restore status


                                        set_a $80,0
                               >            load_flag 0
        21c1 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        21c3 : 48              >            pha         ;use stack to load status
        21c4 : a980            >            lda #$80     ;precharge accu
        21c6 : 28              >            plp

        21c7 : c124                     cmp (ind1,x)
                                        tst_a $80,fc
        21c9 : 08              >            php         ;save flags
        21ca : c980            >            cmp #$80     ;test result
                               >            trap_ne
        21cc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        21ce : 68              >            pla         ;load status
        21cf : 48              >            pha
                               >            cmp_flag fc
        21d0 : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        21d2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        21d4 : 28              >            plp         ;restore status

                                        set_a $7f,0
                               >            load_flag 0
        21d5 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        21d7 : 48              >            pha         ;use stack to load status
        21d8 : a97f            >            lda #$7f     ;precharge accu
        21da : 28              >            plp

        21db : c124                     cmp (ind1,x)
                                        tst_a $7f,fzc
        21dd : 08              >            php         ;save flags
        21de : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        21e0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        21e2 : 68              >            pla         ;load status
        21e3 : 48              >            pha
                               >            cmp_flag fzc
        21e4 : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        21e6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        21e8 : 28              >            plp         ;restore status

                                        set_a $7e,0
                               >            load_flag 0
        21e9 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        21eb : 48              >            pha         ;use stack to load status
        21ec : a97e            >            lda #$7e     ;precharge accu
        21ee : 28              >            plp

        21ef : c124                     cmp (ind1,x)
                                        tst_a $7e,fn
        21f1 : 08              >            php         ;save flags
        21f2 : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        21f4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        21f6 : 68              >            pla         ;load status
        21f7 : 48              >            pha
                               >            cmp_flag fn
        21f8 : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        21fa : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        21fc : 28              >            plp         ;restore status

                                        set_a $80,$ff
                               >            load_flag $ff
        21fd : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        21ff : 48              >            pha         ;use stack to load status
        2200 : a980            >            lda #$80     ;precharge accu
        2202 : 28              >            plp

        2203 : c124                     cmp (ind1,x)
                                        tst_a $80,~fnz
        2205 : 08              >            php         ;save flags
        2206 : c980            >            cmp #$80     ;test result
                               >            trap_ne
        2208 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        220a : 68              >            pla         ;load status
        220b : 48              >            pha
                               >            cmp_flag ~fnz
        220c : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        220e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2210 : 28              >            plp         ;restore status

                                        set_a $7f,$ff
                               >            load_flag $ff
        2211 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2213 : 48              >            pha         ;use stack to load status
        2214 : a97f            >            lda #$7f     ;precharge accu
        2216 : 28              >            plp

        2217 : c124                     cmp (ind1,x)
                                        tst_a $7f,~fn
        2219 : 08              >            php         ;save flags
        221a : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        221c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        221e : 68              >            pla         ;load status
        221f : 48              >            pha
                               >            cmp_flag ~fn
        2220 : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2222 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2224 : 28              >            plp         ;restore status

                                        set_a $7e,$ff
                               >            load_flag $ff
        2225 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2227 : 48              >            pha         ;use stack to load status
        2228 : a97e            >            lda #$7e     ;precharge accu
        222a : 28              >            plp

        222b : c124                     cmp (ind1,x)
                                        tst_a $7e,~fzc
        222d : 08              >            php         ;save flags
        222e : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        2230 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2232 : 68              >            pla         ;load status
        2233 : 48              >            pha
                               >            cmp_flag ~fzc
        2234 : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2236 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2238 : 28              >            plp         ;restore status


                                        set_a $80,0
                               >            load_flag 0
        2239 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        223b : 48              >            pha         ;use stack to load status
        223c : a980            >            lda #$80     ;precharge accu
        223e : 28              >            plp

        223f : d124                     cmp (ind1),y
                                        tst_a $80,fc
        2241 : 08              >            php         ;save flags
        2242 : c980            >            cmp #$80     ;test result
                               >            trap_ne
        2244 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2246 : 68              >            pla         ;load status
        2247 : 48              >            pha
                               >            cmp_flag fc
        2248 : c931            >            cmp #(fc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        224a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        224c : 28              >            plp         ;restore status

                                        set_a $7f,0
                               >            load_flag 0
        224d : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        224f : 48              >            pha         ;use stack to load status
        2250 : a97f            >            lda #$7f     ;precharge accu
        2252 : 28              >            plp

        2253 : d124                     cmp (ind1),y
                                        tst_a $7f,fzc
        2255 : 08              >            php         ;save flags
        2256 : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        2258 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        225a : 68              >            pla         ;load status
        225b : 48              >            pha
                               >            cmp_flag fzc
        225c : c933            >            cmp #(fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        225e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2260 : 28              >            plp         ;restore status

                                        set_a $7e,0
                               >            load_flag 0
        2261 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2263 : 48              >            pha         ;use stack to load status
        2264 : a97e            >            lda #$7e     ;precharge accu
        2266 : 28              >            plp

        2267 : d124                     cmp (ind1),y
                                        tst_a $7e,fn
        2269 : 08              >            php         ;save flags
        226a : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        226c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        226e : 68              >            pla         ;load status
        226f : 48              >            pha
                               >            cmp_flag fn
        2270 : c9b0            >            cmp #(fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2272 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2274 : 28              >            plp         ;restore status

                                        set_a $80,$ff
                               >            load_flag $ff
        2275 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2277 : 48              >            pha         ;use stack to load status
        2278 : a980            >            lda #$80     ;precharge accu
        227a : 28              >            plp

        227b : d124                     cmp (ind1),y
                                        tst_a $80,~fnz
        227d : 08              >            php         ;save flags
        227e : c980            >            cmp #$80     ;test result
                               >            trap_ne
        2280 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2282 : 68              >            pla         ;load status
        2283 : 48              >            pha
                               >            cmp_flag ~fnz
        2284 : c97d            >            cmp #(~fnz|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        2286 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2288 : 28              >            plp         ;restore status

                                        set_a $7f,$ff
                               >            load_flag $ff
        2289 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        228b : 48              >            pha         ;use stack to load status
        228c : a97f            >            lda #$7f     ;precharge accu
        228e : 28              >            plp

        228f : d124                     cmp (ind1),y
                                        tst_a $7f,~fn
        2291 : 08              >            php         ;save flags
        2292 : c97f            >            cmp #$7f     ;test result
                               >            trap_ne
        2294 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2296 : 68              >            pla         ;load status
        2297 : 48              >            pha
                               >            cmp_flag ~fn
        2298 : c97f            >            cmp #(~fn|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        229a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        229c : 28              >            plp         ;restore status

                                        set_a $7e,$ff
                               >            load_flag $ff
        229d : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        229f : 48              >            pha         ;use stack to load status
        22a0 : a97e            >            lda #$7e     ;precharge accu
        22a2 : 28              >            plp

        22a3 : d124                     cmp (ind1),y
                                        tst_a $7e,~fzc
        22a5 : 08              >            php         ;save flags
        22a6 : c97e            >            cmp #$7e     ;test result
                               >            trap_ne
        22a8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        22aa : 68              >            pla         ;load status
        22ab : 48              >            pha
                               >            cmp_flag ~fzc
        22ac : c9fc            >            cmp #(~fzc|fao)&m8    ;expected flags + always on bits
                               >
                               >            trap_ne
        22ae : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        22b0 : 28              >            plp         ;restore status

                                        next_test
        22b1 : ad0002          >            lda test_case   ;previous test
        22b4 : c91c            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        22b6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        001d =                 >test_num = test_num + 1
        22b8 : a91d            >            lda #test_num   ;*** next tests' number
        22ba : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; testing shifts - ASL LSR ROL ROR all addressing modes
                                ; shifts - accumulator
        22bd : a203                     ldx #3
        22bf :                  tasl
                                        set_ax zp1,0
                               >            load_flag 0
        22bf : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        22c1 : 48              >            pha         ;use stack to load status
        22c2 : b513            >            lda zp1,x    ;precharge accu
        22c4 : 28              >            plp

        22c5 : 0a                       asl a
                                        tst_ax rASL,fASL,0
        22c6 : 08              >            php         ;save flags
        22c7 : dd2002          >            cmp rASL,x    ;test result
                               >            trap_ne
        22ca : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        22cc : 68              >            pla         ;load status
                               >            eor_flag 0
        22cd : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        22cf : dd3002          >            cmp fASL,x    ;test flags
                               >            trap_ne     ;
        22d2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        22d4 : ca                       dex
        22d5 : 10e8                     bpl tasl
        22d7 : a203                     ldx #3
        22d9 :                  tasl1
                                        set_ax zp1,$ff
                               >            load_flag $ff
        22d9 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        22db : 48              >            pha         ;use stack to load status
        22dc : b513            >            lda zp1,x    ;precharge accu
        22de : 28              >            plp

        22df : 0a                       asl a
                                        tst_ax rASL,fASL,$ff-fnzc
        22e0 : 08              >            php         ;save flags
        22e1 : dd2002          >            cmp rASL,x    ;test result
                               >            trap_ne
        22e4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        22e6 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        22e7 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        22e9 : dd3002          >            cmp fASL,x    ;test flags
                               >            trap_ne     ;
        22ec : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        22ee : ca                       dex
        22ef : 10e8                     bpl tasl1

        22f1 : a203                     ldx #3
        22f3 :                  tlsr
                                        set_ax zp1,0
                               >            load_flag 0
        22f3 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        22f5 : 48              >            pha         ;use stack to load status
        22f6 : b513            >            lda zp1,x    ;precharge accu
        22f8 : 28              >            plp

        22f9 : 4a                       lsr a
                                        tst_ax rLSR,fLSR,0
        22fa : 08              >            php         ;save flags
        22fb : dd2802          >            cmp rLSR,x    ;test result
                               >            trap_ne
        22fe : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2300 : 68              >            pla         ;load status
                               >            eor_flag 0
        2301 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2303 : dd3802          >            cmp fLSR,x    ;test flags
                               >            trap_ne     ;
        2306 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2308 : ca                       dex
        2309 : 10e8                     bpl tlsr
        230b : a203                     ldx #3
        230d :                  tlsr1
                                        set_ax zp1,$ff
                               >            load_flag $ff
        230d : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        230f : 48              >            pha         ;use stack to load status
        2310 : b513            >            lda zp1,x    ;precharge accu
        2312 : 28              >            plp

        2313 : 4a                       lsr a
                                        tst_ax rLSR,fLSR,$ff-fnzc
        2314 : 08              >            php         ;save flags
        2315 : dd2802          >            cmp rLSR,x    ;test result
                               >            trap_ne
        2318 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        231a : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        231b : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        231d : dd3802          >            cmp fLSR,x    ;test flags
                               >            trap_ne     ;
        2320 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2322 : ca                       dex
        2323 : 10e8                     bpl tlsr1

        2325 : a203                     ldx #3
        2327 :                  trol
                                        set_ax zp1,0
                               >            load_flag 0
        2327 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2329 : 48              >            pha         ;use stack to load status
        232a : b513            >            lda zp1,x    ;precharge accu
        232c : 28              >            plp

        232d : 2a                       rol a
                                        tst_ax rROL,fROL,0
        232e : 08              >            php         ;save flags
        232f : dd2002          >            cmp rROL,x    ;test result
                               >            trap_ne
        2332 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2334 : 68              >            pla         ;load status
                               >            eor_flag 0
        2335 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2337 : dd3002          >            cmp fROL,x    ;test flags
                               >            trap_ne     ;
        233a : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        233c : ca                       dex
        233d : 10e8                     bpl trol
        233f : a203                     ldx #3
        2341 :                  trol1
                                        set_ax zp1,$ff-fc
                               >            load_flag $ff-fc
        2341 : a9fe            >            lda #$ff-fc             ;allow test to change I-flag (no mask)
                               >
        2343 : 48              >            pha         ;use stack to load status
        2344 : b513            >            lda zp1,x    ;precharge accu
        2346 : 28              >            plp

        2347 : 2a                       rol a
                                        tst_ax rROL,fROL,$ff-fnzc
        2348 : 08              >            php         ;save flags
        2349 : dd2002          >            cmp rROL,x    ;test result
                               >            trap_ne
        234c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        234e : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        234f : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2351 : dd3002          >            cmp fROL,x    ;test flags
                               >            trap_ne     ;
        2354 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2356 : ca                       dex
        2357 : 10e8                     bpl trol1

        2359 : a203                     ldx #3
        235b :                  trolc
                                        set_ax zp1,fc
                               >            load_flag fc
        235b : a901            >            lda #fc             ;allow test to change I-flag (no mask)
                               >
        235d : 48              >            pha         ;use stack to load status
        235e : b513            >            lda zp1,x    ;precharge accu
        2360 : 28              >            plp

        2361 : 2a                       rol a
                                        tst_ax rROLc,fROLc,0
        2362 : 08              >            php         ;save flags
        2363 : dd2402          >            cmp rROLc,x    ;test result
                               >            trap_ne
        2366 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2368 : 68              >            pla         ;load status
                               >            eor_flag 0
        2369 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        236b : dd3402          >            cmp fROLc,x    ;test flags
                               >            trap_ne     ;
        236e : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2370 : ca                       dex
        2371 : 10e8                     bpl trolc
        2373 : a203                     ldx #3
        2375 :                  trolc1
                                        set_ax zp1,$ff
                               >            load_flag $ff
        2375 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2377 : 48              >            pha         ;use stack to load status
        2378 : b513            >            lda zp1,x    ;precharge accu
        237a : 28              >            plp

        237b : 2a                       rol a
                                        tst_ax rROLc,fROLc,$ff-fnzc
        237c : 08              >            php         ;save flags
        237d : dd2402          >            cmp rROLc,x    ;test result
                               >            trap_ne
        2380 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2382 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2383 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2385 : dd3402          >            cmp fROLc,x    ;test flags
                               >            trap_ne     ;
        2388 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        238a : ca                       dex
        238b : 10e8                     bpl trolc1

        238d : a203                     ldx #3
        238f :                  tror
                                        set_ax zp1,0
                               >            load_flag 0
        238f : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2391 : 48              >            pha         ;use stack to load status
        2392 : b513            >            lda zp1,x    ;precharge accu
        2394 : 28              >            plp

        2395 : 6a                       ror a
                                        tst_ax rROR,fROR,0
        2396 : 08              >            php         ;save flags
        2397 : dd2802          >            cmp rROR,x    ;test result
                               >            trap_ne
        239a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        239c : 68              >            pla         ;load status
                               >            eor_flag 0
        239d : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        239f : dd3802          >            cmp fROR,x    ;test flags
                               >            trap_ne     ;
        23a2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        23a4 : ca                       dex
        23a5 : 10e8                     bpl tror
        23a7 : a203                     ldx #3
        23a9 :                  tror1
                                        set_ax zp1,$ff-fc
                               >            load_flag $ff-fc
        23a9 : a9fe            >            lda #$ff-fc             ;allow test to change I-flag (no mask)
                               >
        23ab : 48              >            pha         ;use stack to load status
        23ac : b513            >            lda zp1,x    ;precharge accu
        23ae : 28              >            plp

        23af : 6a                       ror a
                                        tst_ax rROR,fROR,$ff-fnzc
        23b0 : 08              >            php         ;save flags
        23b1 : dd2802          >            cmp rROR,x    ;test result
                               >            trap_ne
        23b4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        23b6 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        23b7 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        23b9 : dd3802          >            cmp fROR,x    ;test flags
                               >            trap_ne     ;
        23bc : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        23be : ca                       dex
        23bf : 10e8                     bpl tror1

        23c1 : a203                     ldx #3
        23c3 :                  trorc
                                        set_ax zp1,fc
                               >            load_flag fc
        23c3 : a901            >            lda #fc             ;allow test to change I-flag (no mask)
                               >
        23c5 : 48              >            pha         ;use stack to load status
        23c6 : b513            >            lda zp1,x    ;precharge accu
        23c8 : 28              >            plp

        23c9 : 6a                       ror a
                                        tst_ax rRORc,fRORc,0
        23ca : 08              >            php         ;save flags
        23cb : dd2c02          >            cmp rRORc,x    ;test result
                               >            trap_ne
        23ce : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        23d0 : 68              >            pla         ;load status
                               >            eor_flag 0
        23d1 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        23d3 : dd3c02          >            cmp fRORc,x    ;test flags
                               >            trap_ne     ;
        23d6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        23d8 : ca                       dex
        23d9 : 10e8                     bpl trorc
        23db : a203                     ldx #3
        23dd :                  trorc1
                                        set_ax zp1,$ff
                               >            load_flag $ff
        23dd : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        23df : 48              >            pha         ;use stack to load status
        23e0 : b513            >            lda zp1,x    ;precharge accu
        23e2 : 28              >            plp

        23e3 : 6a                       ror a
                                        tst_ax rRORc,fRORc,$ff-fnzc
        23e4 : 08              >            php         ;save flags
        23e5 : dd2c02          >            cmp rRORc,x    ;test result
                               >            trap_ne
        23e8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        23ea : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        23eb : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        23ed : dd3c02          >            cmp fRORc,x    ;test flags
                               >            trap_ne     ;
        23f0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        23f2 : ca                       dex
        23f3 : 10e8                     bpl trorc1
                                        next_test
        23f5 : ad0002          >            lda test_case   ;previous test
        23f8 : c91d            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        23fa : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        001e =                 >test_num = test_num + 1
        23fc : a91e            >            lda #test_num   ;*** next tests' number
        23fe : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; shifts - zeropage
        2401 : a203                     ldx #3
        2403 :                  tasl2
                                        set_z zp1,0
                               >            load_flag 0
        2403 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2405 : 48              >            pha         ;use stack to load status
        2406 : b513            >            lda zp1,x    ;load to zeropage
        2408 : 850c            >            sta zpt
        240a : 28              >            plp

        240b : 060c                     asl zpt
                                        tst_z rASL,fASL,0
        240d : 08              >            php         ;save flags
        240e : a50c            >            lda zpt
        2410 : dd2002          >            cmp rASL,x    ;test result
                               >            trap_ne
        2413 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2415 : 68              >            pla         ;load status
                               >            eor_flag 0
        2416 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2418 : dd3002          >            cmp fASL,x    ;test flags
                               >            trap_ne
        241b : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        241d : ca                       dex
        241e : 10e3                     bpl tasl2
        2420 : a203                     ldx #3
        2422 :                  tasl3
                                        set_z zp1,$ff
                               >            load_flag $ff
        2422 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2424 : 48              >            pha         ;use stack to load status
        2425 : b513            >            lda zp1,x    ;load to zeropage
        2427 : 850c            >            sta zpt
        2429 : 28              >            plp

        242a : 060c                     asl zpt
                                        tst_z rASL,fASL,$ff-fnzc
        242c : 08              >            php         ;save flags
        242d : a50c            >            lda zpt
        242f : dd2002          >            cmp rASL,x    ;test result
                               >            trap_ne
        2432 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2434 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2435 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2437 : dd3002          >            cmp fASL,x    ;test flags
                               >            trap_ne
        243a : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        243c : ca                       dex
        243d : 10e3                     bpl tasl3

        243f : a203                     ldx #3
        2441 :                  tlsr2
                                        set_z zp1,0
                               >            load_flag 0
        2441 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2443 : 48              >            pha         ;use stack to load status
        2444 : b513            >            lda zp1,x    ;load to zeropage
        2446 : 850c            >            sta zpt
        2448 : 28              >            plp

        2449 : 460c                     lsr zpt
                                        tst_z rLSR,fLSR,0
        244b : 08              >            php         ;save flags
        244c : a50c            >            lda zpt
        244e : dd2802          >            cmp rLSR,x    ;test result
                               >            trap_ne
        2451 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2453 : 68              >            pla         ;load status
                               >            eor_flag 0
        2454 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2456 : dd3802          >            cmp fLSR,x    ;test flags
                               >            trap_ne
        2459 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        245b : ca                       dex
        245c : 10e3                     bpl tlsr2
        245e : a203                     ldx #3
        2460 :                  tlsr3
                                        set_z zp1,$ff
                               >            load_flag $ff
        2460 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2462 : 48              >            pha         ;use stack to load status
        2463 : b513            >            lda zp1,x    ;load to zeropage
        2465 : 850c            >            sta zpt
        2467 : 28              >            plp

        2468 : 460c                     lsr zpt
                                        tst_z rLSR,fLSR,$ff-fnzc
        246a : 08              >            php         ;save flags
        246b : a50c            >            lda zpt
        246d : dd2802          >            cmp rLSR,x    ;test result
                               >            trap_ne
        2470 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2472 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2473 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2475 : dd3802          >            cmp fLSR,x    ;test flags
                               >            trap_ne
        2478 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        247a : ca                       dex
        247b : 10e3                     bpl tlsr3

        247d : a203                     ldx #3
        247f :                  trol2
                                        set_z zp1,0
                               >            load_flag 0
        247f : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2481 : 48              >            pha         ;use stack to load status
        2482 : b513            >            lda zp1,x    ;load to zeropage
        2484 : 850c            >            sta zpt
        2486 : 28              >            plp

        2487 : 260c                     rol zpt
                                        tst_z rROL,fROL,0
        2489 : 08              >            php         ;save flags
        248a : a50c            >            lda zpt
        248c : dd2002          >            cmp rROL,x    ;test result
                               >            trap_ne
        248f : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2491 : 68              >            pla         ;load status
                               >            eor_flag 0
        2492 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2494 : dd3002          >            cmp fROL,x    ;test flags
                               >            trap_ne
        2497 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2499 : ca                       dex
        249a : 10e3                     bpl trol2
        249c : a203                     ldx #3
        249e :                  trol3
                                        set_z zp1,$ff-fc
                               >            load_flag $ff-fc
        249e : a9fe            >            lda #$ff-fc             ;allow test to change I-flag (no mask)
                               >
        24a0 : 48              >            pha         ;use stack to load status
        24a1 : b513            >            lda zp1,x    ;load to zeropage
        24a3 : 850c            >            sta zpt
        24a5 : 28              >            plp

        24a6 : 260c                     rol zpt
                                        tst_z rROL,fROL,$ff-fnzc
        24a8 : 08              >            php         ;save flags
        24a9 : a50c            >            lda zpt
        24ab : dd2002          >            cmp rROL,x    ;test result
                               >            trap_ne
        24ae : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        24b0 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        24b1 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        24b3 : dd3002          >            cmp fROL,x    ;test flags
                               >            trap_ne
        24b6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        24b8 : ca                       dex
        24b9 : 10e3                     bpl trol3

        24bb : a203                     ldx #3
        24bd :                  trolc2
                                        set_z zp1,fc
                               >            load_flag fc
        24bd : a901            >            lda #fc             ;allow test to change I-flag (no mask)
                               >
        24bf : 48              >            pha         ;use stack to load status
        24c0 : b513            >            lda zp1,x    ;load to zeropage
        24c2 : 850c            >            sta zpt
        24c4 : 28              >            plp

        24c5 : 260c                     rol zpt
                                        tst_z rROLc,fROLc,0
        24c7 : 08              >            php         ;save flags
        24c8 : a50c            >            lda zpt
        24ca : dd2402          >            cmp rROLc,x    ;test result
                               >            trap_ne
        24cd : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        24cf : 68              >            pla         ;load status
                               >            eor_flag 0
        24d0 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        24d2 : dd3402          >            cmp fROLc,x    ;test flags
                               >            trap_ne
        24d5 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        24d7 : ca                       dex
        24d8 : 10e3                     bpl trolc2
        24da : a203                     ldx #3
        24dc :                  trolc3
                                        set_z zp1,$ff
                               >            load_flag $ff
        24dc : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        24de : 48              >            pha         ;use stack to load status
        24df : b513            >            lda zp1,x    ;load to zeropage
        24e1 : 850c            >            sta zpt
        24e3 : 28              >            plp

        24e4 : 260c                     rol zpt
                                        tst_z rROLc,fROLc,$ff-fnzc
        24e6 : 08              >            php         ;save flags
        24e7 : a50c            >            lda zpt
        24e9 : dd2402          >            cmp rROLc,x    ;test result
                               >            trap_ne
        24ec : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        24ee : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        24ef : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        24f1 : dd3402          >            cmp fROLc,x    ;test flags
                               >            trap_ne
        24f4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        24f6 : ca                       dex
        24f7 : 10e3                     bpl trolc3

        24f9 : a203                     ldx #3
        24fb :                  tror2
                                        set_z zp1,0
                               >            load_flag 0
        24fb : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        24fd : 48              >            pha         ;use stack to load status
        24fe : b513            >            lda zp1,x    ;load to zeropage
        2500 : 850c            >            sta zpt
        2502 : 28              >            plp

        2503 : 660c                     ror zpt
                                        tst_z rROR,fROR,0
        2505 : 08              >            php         ;save flags
        2506 : a50c            >            lda zpt
        2508 : dd2802          >            cmp rROR,x    ;test result
                               >            trap_ne
        250b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        250d : 68              >            pla         ;load status
                               >            eor_flag 0
        250e : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2510 : dd3802          >            cmp fROR,x    ;test flags
                               >            trap_ne
        2513 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2515 : ca                       dex
        2516 : 10e3                     bpl tror2
        2518 : a203                     ldx #3
        251a :                  tror3
                                        set_z zp1,$ff-fc
                               >            load_flag $ff-fc
        251a : a9fe            >            lda #$ff-fc             ;allow test to change I-flag (no mask)
                               >
        251c : 48              >            pha         ;use stack to load status
        251d : b513            >            lda zp1,x    ;load to zeropage
        251f : 850c            >            sta zpt
        2521 : 28              >            plp

        2522 : 660c                     ror zpt
                                        tst_z rROR,fROR,$ff-fnzc
        2524 : 08              >            php         ;save flags
        2525 : a50c            >            lda zpt
        2527 : dd2802          >            cmp rROR,x    ;test result
                               >            trap_ne
        252a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        252c : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        252d : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        252f : dd3802          >            cmp fROR,x    ;test flags
                               >            trap_ne
        2532 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2534 : ca                       dex
        2535 : 10e3                     bpl tror3

        2537 : a203                     ldx #3
        2539 :                  trorc2
                                        set_z zp1,fc
                               >            load_flag fc
        2539 : a901            >            lda #fc             ;allow test to change I-flag (no mask)
                               >
        253b : 48              >            pha         ;use stack to load status
        253c : b513            >            lda zp1,x    ;load to zeropage
        253e : 850c            >            sta zpt
        2540 : 28              >            plp

        2541 : 660c                     ror zpt
                                        tst_z rRORc,fRORc,0
        2543 : 08              >            php         ;save flags
        2544 : a50c            >            lda zpt
        2546 : dd2c02          >            cmp rRORc,x    ;test result
                               >            trap_ne
        2549 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        254b : 68              >            pla         ;load status
                               >            eor_flag 0
        254c : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        254e : dd3c02          >            cmp fRORc,x    ;test flags
                               >            trap_ne
        2551 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2553 : ca                       dex
        2554 : 10e3                     bpl trorc2
        2556 : a203                     ldx #3
        2558 :                  trorc3
                                        set_z zp1,$ff
                               >            load_flag $ff
        2558 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        255a : 48              >            pha         ;use stack to load status
        255b : b513            >            lda zp1,x    ;load to zeropage
        255d : 850c            >            sta zpt
        255f : 28              >            plp

        2560 : 660c                     ror zpt
                                        tst_z rRORc,fRORc,$ff-fnzc
        2562 : 08              >            php         ;save flags
        2563 : a50c            >            lda zpt
        2565 : dd2c02          >            cmp rRORc,x    ;test result
                               >            trap_ne
        2568 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        256a : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        256b : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        256d : dd3c02          >            cmp fRORc,x    ;test flags
                               >            trap_ne
        2570 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2572 : ca                       dex
        2573 : 10e3                     bpl trorc3
                                        next_test
        2575 : ad0002          >            lda test_case   ;previous test
        2578 : c91e            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        257a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        001f =                 >test_num = test_num + 1
        257c : a91f            >            lda #test_num   ;*** next tests' number
        257e : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; shifts - absolute
        2581 : a203                     ldx #3
        2583 :                  tasl4
                                        set_abs zp1,0
                               >            load_flag 0
        2583 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2585 : 48              >            pha         ;use stack to load status
        2586 : b513            >            lda zp1,x    ;load to memory
        2588 : 8d0302          >            sta abst
        258b : 28              >            plp

        258c : 0e0302                   asl abst
                                        tst_abs rASL,fASL,0
        258f : 08              >            php         ;save flags
        2590 : ad0302          >            lda abst
        2593 : dd2002          >            cmp rASL,x    ;test result
                               >            trap_ne
        2596 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2598 : 68              >            pla         ;load status
                               >            eor_flag 0
        2599 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        259b : dd3002          >            cmp fASL,x    ;test flags
                               >            trap_ne
        259e : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        25a0 : ca                       dex
        25a1 : 10e0                     bpl tasl4
        25a3 : a203                     ldx #3
        25a5 :                  tasl5
                                        set_abs zp1,$ff
                               >            load_flag $ff
        25a5 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        25a7 : 48              >            pha         ;use stack to load status
        25a8 : b513            >            lda zp1,x    ;load to memory
        25aa : 8d0302          >            sta abst
        25ad : 28              >            plp

        25ae : 0e0302                   asl abst
                                        tst_abs rASL,fASL,$ff-fnzc
        25b1 : 08              >            php         ;save flags
        25b2 : ad0302          >            lda abst
        25b5 : dd2002          >            cmp rASL,x    ;test result
                               >            trap_ne
        25b8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        25ba : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        25bb : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        25bd : dd3002          >            cmp fASL,x    ;test flags
                               >            trap_ne
        25c0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        25c2 : ca                       dex
        25c3 : 10e0                     bpl tasl5

        25c5 : a203                     ldx #3
        25c7 :                  tlsr4
                                        set_abs zp1,0
                               >            load_flag 0
        25c7 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        25c9 : 48              >            pha         ;use stack to load status
        25ca : b513            >            lda zp1,x    ;load to memory
        25cc : 8d0302          >            sta abst
        25cf : 28              >            plp

        25d0 : 4e0302                   lsr abst
                                        tst_abs rLSR,fLSR,0
        25d3 : 08              >            php         ;save flags
        25d4 : ad0302          >            lda abst
        25d7 : dd2802          >            cmp rLSR,x    ;test result
                               >            trap_ne
        25da : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        25dc : 68              >            pla         ;load status
                               >            eor_flag 0
        25dd : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        25df : dd3802          >            cmp fLSR,x    ;test flags
                               >            trap_ne
        25e2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        25e4 : ca                       dex
        25e5 : 10e0                     bpl tlsr4
        25e7 : a203                     ldx #3
        25e9 :                  tlsr5
                                        set_abs zp1,$ff
                               >            load_flag $ff
        25e9 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        25eb : 48              >            pha         ;use stack to load status
        25ec : b513            >            lda zp1,x    ;load to memory
        25ee : 8d0302          >            sta abst
        25f1 : 28              >            plp

        25f2 : 4e0302                   lsr abst
                                        tst_abs rLSR,fLSR,$ff-fnzc
        25f5 : 08              >            php         ;save flags
        25f6 : ad0302          >            lda abst
        25f9 : dd2802          >            cmp rLSR,x    ;test result
                               >            trap_ne
        25fc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        25fe : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        25ff : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2601 : dd3802          >            cmp fLSR,x    ;test flags
                               >            trap_ne
        2604 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2606 : ca                       dex
        2607 : 10e0                     bpl tlsr5

        2609 : a203                     ldx #3
        260b :                  trol4
                                        set_abs zp1,0
                               >            load_flag 0
        260b : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        260d : 48              >            pha         ;use stack to load status
        260e : b513            >            lda zp1,x    ;load to memory
        2610 : 8d0302          >            sta abst
        2613 : 28              >            plp

        2614 : 2e0302                   rol abst
                                        tst_abs rROL,fROL,0
        2617 : 08              >            php         ;save flags
        2618 : ad0302          >            lda abst
        261b : dd2002          >            cmp rROL,x    ;test result
                               >            trap_ne
        261e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2620 : 68              >            pla         ;load status
                               >            eor_flag 0
        2621 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2623 : dd3002          >            cmp fROL,x    ;test flags
                               >            trap_ne
        2626 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2628 : ca                       dex
        2629 : 10e0                     bpl trol4
        262b : a203                     ldx #3
        262d :                  trol5
                                        set_abs zp1,$ff-fc
                               >            load_flag $ff-fc
        262d : a9fe            >            lda #$ff-fc             ;allow test to change I-flag (no mask)
                               >
        262f : 48              >            pha         ;use stack to load status
        2630 : b513            >            lda zp1,x    ;load to memory
        2632 : 8d0302          >            sta abst
        2635 : 28              >            plp

        2636 : 2e0302                   rol abst
                                        tst_abs rROL,fROL,$ff-fnzc
        2639 : 08              >            php         ;save flags
        263a : ad0302          >            lda abst
        263d : dd2002          >            cmp rROL,x    ;test result
                               >            trap_ne
        2640 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2642 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2643 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2645 : dd3002          >            cmp fROL,x    ;test flags
                               >            trap_ne
        2648 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        264a : ca                       dex
        264b : 10e0                     bpl trol5

        264d : a203                     ldx #3
        264f :                  trolc4
                                        set_abs zp1,fc
                               >            load_flag fc
        264f : a901            >            lda #fc             ;allow test to change I-flag (no mask)
                               >
        2651 : 48              >            pha         ;use stack to load status
        2652 : b513            >            lda zp1,x    ;load to memory
        2654 : 8d0302          >            sta abst
        2657 : 28              >            plp

        2658 : 2e0302                   rol abst
                                        tst_abs rROLc,fROLc,0
        265b : 08              >            php         ;save flags
        265c : ad0302          >            lda abst
        265f : dd2402          >            cmp rROLc,x    ;test result
                               >            trap_ne
        2662 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2664 : 68              >            pla         ;load status
                               >            eor_flag 0
        2665 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2667 : dd3402          >            cmp fROLc,x    ;test flags
                               >            trap_ne
        266a : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        266c : ca                       dex
        266d : 10e0                     bpl trolc4
        266f : a203                     ldx #3
        2671 :                  trolc5
                                        set_abs zp1,$ff
                               >            load_flag $ff
        2671 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2673 : 48              >            pha         ;use stack to load status
        2674 : b513            >            lda zp1,x    ;load to memory
        2676 : 8d0302          >            sta abst
        2679 : 28              >            plp

        267a : 2e0302                   rol abst
                                        tst_abs rROLc,fROLc,$ff-fnzc
        267d : 08              >            php         ;save flags
        267e : ad0302          >            lda abst
        2681 : dd2402          >            cmp rROLc,x    ;test result
                               >            trap_ne
        2684 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2686 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2687 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2689 : dd3402          >            cmp fROLc,x    ;test flags
                               >            trap_ne
        268c : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        268e : ca                       dex
        268f : 10e0                     bpl trolc5

        2691 : a203                     ldx #3
        2693 :                  tror4
                                        set_abs zp1,0
                               >            load_flag 0
        2693 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2695 : 48              >            pha         ;use stack to load status
        2696 : b513            >            lda zp1,x    ;load to memory
        2698 : 8d0302          >            sta abst
        269b : 28              >            plp

        269c : 6e0302                   ror abst
                                        tst_abs rROR,fROR,0
        269f : 08              >            php         ;save flags
        26a0 : ad0302          >            lda abst
        26a3 : dd2802          >            cmp rROR,x    ;test result
                               >            trap_ne
        26a6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        26a8 : 68              >            pla         ;load status
                               >            eor_flag 0
        26a9 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        26ab : dd3802          >            cmp fROR,x    ;test flags
                               >            trap_ne
        26ae : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        26b0 : ca                       dex
        26b1 : 10e0                     bpl tror4
        26b3 : a203                     ldx #3
        26b5 :                  tror5
                                        set_abs zp1,$ff-fc
                               >            load_flag $ff-fc
        26b5 : a9fe            >            lda #$ff-fc             ;allow test to change I-flag (no mask)
                               >
        26b7 : 48              >            pha         ;use stack to load status
        26b8 : b513            >            lda zp1,x    ;load to memory
        26ba : 8d0302          >            sta abst
        26bd : 28              >            plp

        26be : 6e0302                   ror abst
                                        tst_abs rROR,fROR,$ff-fnzc
        26c1 : 08              >            php         ;save flags
        26c2 : ad0302          >            lda abst
        26c5 : dd2802          >            cmp rROR,x    ;test result
                               >            trap_ne
        26c8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        26ca : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        26cb : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        26cd : dd3802          >            cmp fROR,x    ;test flags
                               >            trap_ne
        26d0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        26d2 : ca                       dex
        26d3 : 10e0                     bpl tror5

        26d5 : a203                     ldx #3
        26d7 :                  trorc4
                                        set_abs zp1,fc
                               >            load_flag fc
        26d7 : a901            >            lda #fc             ;allow test to change I-flag (no mask)
                               >
        26d9 : 48              >            pha         ;use stack to load status
        26da : b513            >            lda zp1,x    ;load to memory
        26dc : 8d0302          >            sta abst
        26df : 28              >            plp

        26e0 : 6e0302                   ror abst
                                        tst_abs rRORc,fRORc,0
        26e3 : 08              >            php         ;save flags
        26e4 : ad0302          >            lda abst
        26e7 : dd2c02          >            cmp rRORc,x    ;test result
                               >            trap_ne
        26ea : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        26ec : 68              >            pla         ;load status
                               >            eor_flag 0
        26ed : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        26ef : dd3c02          >            cmp fRORc,x    ;test flags
                               >            trap_ne
        26f2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        26f4 : ca                       dex
        26f5 : 10e0                     bpl trorc4
        26f7 : a203                     ldx #3
        26f9 :                  trorc5
                                        set_abs zp1,$ff
                               >            load_flag $ff
        26f9 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        26fb : 48              >            pha         ;use stack to load status
        26fc : b513            >            lda zp1,x    ;load to memory
        26fe : 8d0302          >            sta abst
        2701 : 28              >            plp

        2702 : 6e0302                   ror abst
                                        tst_abs rRORc,fRORc,$ff-fnzc
        2705 : 08              >            php         ;save flags
        2706 : ad0302          >            lda abst
        2709 : dd2c02          >            cmp rRORc,x    ;test result
                               >            trap_ne
        270c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        270e : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        270f : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2711 : dd3c02          >            cmp fRORc,x    ;test flags
                               >            trap_ne
        2714 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2716 : ca                       dex
        2717 : 10e0                     bpl trorc5
                                        next_test
        2719 : ad0002          >            lda test_case   ;previous test
        271c : c91f            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        271e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0020 =                 >test_num = test_num + 1
        2720 : a920            >            lda #test_num   ;*** next tests' number
        2722 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; shifts - zp indexed
        2725 : a203                     ldx #3
        2727 :                  tasl6
                                        set_zx zp1,0
                               >            load_flag 0
        2727 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2729 : 48              >            pha         ;use stack to load status
        272a : b513            >            lda zp1,x    ;load to indexed zeropage
        272c : 950c            >            sta zpt,x
        272e : 28              >            plp

        272f : 160c                     asl zpt,x
                                        tst_zx rASL,fASL,0
        2731 : 08              >            php         ;save flags
        2732 : b50c            >            lda zpt,x
        2734 : dd2002          >            cmp rASL,x    ;test result
                               >            trap_ne
        2737 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2739 : 68              >            pla         ;load status
                               >            eor_flag 0
        273a : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        273c : dd3002          >            cmp fASL,x    ;test flags
                               >            trap_ne
        273f : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2741 : ca                       dex
        2742 : 10e3                     bpl tasl6
        2744 : a203                     ldx #3
        2746 :                  tasl7
                                        set_zx zp1,$ff
                               >            load_flag $ff
        2746 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2748 : 48              >            pha         ;use stack to load status
        2749 : b513            >            lda zp1,x    ;load to indexed zeropage
        274b : 950c            >            sta zpt,x
        274d : 28              >            plp

        274e : 160c                     asl zpt,x
                                        tst_zx rASL,fASL,$ff-fnzc
        2750 : 08              >            php         ;save flags
        2751 : b50c            >            lda zpt,x
        2753 : dd2002          >            cmp rASL,x    ;test result
                               >            trap_ne
        2756 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2758 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2759 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        275b : dd3002          >            cmp fASL,x    ;test flags
                               >            trap_ne
        275e : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2760 : ca                       dex
        2761 : 10e3                     bpl tasl7

        2763 : a203                     ldx #3
        2765 :                  tlsr6
                                        set_zx zp1,0
                               >            load_flag 0
        2765 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2767 : 48              >            pha         ;use stack to load status
        2768 : b513            >            lda zp1,x    ;load to indexed zeropage
        276a : 950c            >            sta zpt,x
        276c : 28              >            plp

        276d : 560c                     lsr zpt,x
                                        tst_zx rLSR,fLSR,0
        276f : 08              >            php         ;save flags
        2770 : b50c            >            lda zpt,x
        2772 : dd2802          >            cmp rLSR,x    ;test result
                               >            trap_ne
        2775 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2777 : 68              >            pla         ;load status
                               >            eor_flag 0
        2778 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        277a : dd3802          >            cmp fLSR,x    ;test flags
                               >            trap_ne
        277d : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        277f : ca                       dex
        2780 : 10e3                     bpl tlsr6
        2782 : a203                     ldx #3
        2784 :                  tlsr7
                                        set_zx zp1,$ff
                               >            load_flag $ff
        2784 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2786 : 48              >            pha         ;use stack to load status
        2787 : b513            >            lda zp1,x    ;load to indexed zeropage
        2789 : 950c            >            sta zpt,x
        278b : 28              >            plp

        278c : 560c                     lsr zpt,x
                                        tst_zx rLSR,fLSR,$ff-fnzc
        278e : 08              >            php         ;save flags
        278f : b50c            >            lda zpt,x
        2791 : dd2802          >            cmp rLSR,x    ;test result
                               >            trap_ne
        2794 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2796 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2797 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2799 : dd3802          >            cmp fLSR,x    ;test flags
                               >            trap_ne
        279c : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        279e : ca                       dex
        279f : 10e3                     bpl tlsr7

        27a1 : a203                     ldx #3
        27a3 :                  trol6
                                        set_zx zp1,0
                               >            load_flag 0
        27a3 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        27a5 : 48              >            pha         ;use stack to load status
        27a6 : b513            >            lda zp1,x    ;load to indexed zeropage
        27a8 : 950c            >            sta zpt,x
        27aa : 28              >            plp

        27ab : 360c                     rol zpt,x
                                        tst_zx rROL,fROL,0
        27ad : 08              >            php         ;save flags
        27ae : b50c            >            lda zpt,x
        27b0 : dd2002          >            cmp rROL,x    ;test result
                               >            trap_ne
        27b3 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        27b5 : 68              >            pla         ;load status
                               >            eor_flag 0
        27b6 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        27b8 : dd3002          >            cmp fROL,x    ;test flags
                               >            trap_ne
        27bb : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        27bd : ca                       dex
        27be : 10e3                     bpl trol6
        27c0 : a203                     ldx #3
        27c2 :                  trol7
                                        set_zx zp1,$ff-fc
                               >            load_flag $ff-fc
        27c2 : a9fe            >            lda #$ff-fc             ;allow test to change I-flag (no mask)
                               >
        27c4 : 48              >            pha         ;use stack to load status
        27c5 : b513            >            lda zp1,x    ;load to indexed zeropage
        27c7 : 950c            >            sta zpt,x
        27c9 : 28              >            plp

        27ca : 360c                     rol zpt,x
                                        tst_zx rROL,fROL,$ff-fnzc
        27cc : 08              >            php         ;save flags
        27cd : b50c            >            lda zpt,x
        27cf : dd2002          >            cmp rROL,x    ;test result
                               >            trap_ne
        27d2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        27d4 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        27d5 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        27d7 : dd3002          >            cmp fROL,x    ;test flags
                               >            trap_ne
        27da : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        27dc : ca                       dex
        27dd : 10e3                     bpl trol7

        27df : a203                     ldx #3
        27e1 :                  trolc6
                                        set_zx zp1,fc
                               >            load_flag fc
        27e1 : a901            >            lda #fc             ;allow test to change I-flag (no mask)
                               >
        27e3 : 48              >            pha         ;use stack to load status
        27e4 : b513            >            lda zp1,x    ;load to indexed zeropage
        27e6 : 950c            >            sta zpt,x
        27e8 : 28              >            plp

        27e9 : 360c                     rol zpt,x
                                        tst_zx rROLc,fROLc,0
        27eb : 08              >            php         ;save flags
        27ec : b50c            >            lda zpt,x
        27ee : dd2402          >            cmp rROLc,x    ;test result
                               >            trap_ne
        27f1 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        27f3 : 68              >            pla         ;load status
                               >            eor_flag 0
        27f4 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        27f6 : dd3402          >            cmp fROLc,x    ;test flags
                               >            trap_ne
        27f9 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        27fb : ca                       dex
        27fc : 10e3                     bpl trolc6
        27fe : a203                     ldx #3
        2800 :                  trolc7
                                        set_zx zp1,$ff
                               >            load_flag $ff
        2800 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2802 : 48              >            pha         ;use stack to load status
        2803 : b513            >            lda zp1,x    ;load to indexed zeropage
        2805 : 950c            >            sta zpt,x
        2807 : 28              >            plp

        2808 : 360c                     rol zpt,x
                                        tst_zx rROLc,fROLc,$ff-fnzc
        280a : 08              >            php         ;save flags
        280b : b50c            >            lda zpt,x
        280d : dd2402          >            cmp rROLc,x    ;test result
                               >            trap_ne
        2810 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2812 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2813 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2815 : dd3402          >            cmp fROLc,x    ;test flags
                               >            trap_ne
        2818 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        281a : ca                       dex
        281b : 10e3                     bpl trolc7

        281d : a203                     ldx #3
        281f :                  tror6
                                        set_zx zp1,0
                               >            load_flag 0
        281f : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2821 : 48              >            pha         ;use stack to load status
        2822 : b513            >            lda zp1,x    ;load to indexed zeropage
        2824 : 950c            >            sta zpt,x
        2826 : 28              >            plp

        2827 : 760c                     ror zpt,x
                                        tst_zx rROR,fROR,0
        2829 : 08              >            php         ;save flags
        282a : b50c            >            lda zpt,x
        282c : dd2802          >            cmp rROR,x    ;test result
                               >            trap_ne
        282f : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2831 : 68              >            pla         ;load status
                               >            eor_flag 0
        2832 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2834 : dd3802          >            cmp fROR,x    ;test flags
                               >            trap_ne
        2837 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2839 : ca                       dex
        283a : 10e3                     bpl tror6
        283c : a203                     ldx #3
        283e :                  tror7
                                        set_zx zp1,$ff-fc
                               >            load_flag $ff-fc
        283e : a9fe            >            lda #$ff-fc             ;allow test to change I-flag (no mask)
                               >
        2840 : 48              >            pha         ;use stack to load status
        2841 : b513            >            lda zp1,x    ;load to indexed zeropage
        2843 : 950c            >            sta zpt,x
        2845 : 28              >            plp

        2846 : 760c                     ror zpt,x
                                        tst_zx rROR,fROR,$ff-fnzc
        2848 : 08              >            php         ;save flags
        2849 : b50c            >            lda zpt,x
        284b : dd2802          >            cmp rROR,x    ;test result
                               >            trap_ne
        284e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2850 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2851 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2853 : dd3802          >            cmp fROR,x    ;test flags
                               >            trap_ne
        2856 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2858 : ca                       dex
        2859 : 10e3                     bpl tror7

        285b : a203                     ldx #3
        285d :                  trorc6
                                        set_zx zp1,fc
                               >            load_flag fc
        285d : a901            >            lda #fc             ;allow test to change I-flag (no mask)
                               >
        285f : 48              >            pha         ;use stack to load status
        2860 : b513            >            lda zp1,x    ;load to indexed zeropage
        2862 : 950c            >            sta zpt,x
        2864 : 28              >            plp

        2865 : 760c                     ror zpt,x
                                        tst_zx rRORc,fRORc,0
        2867 : 08              >            php         ;save flags
        2868 : b50c            >            lda zpt,x
        286a : dd2c02          >            cmp rRORc,x    ;test result
                               >            trap_ne
        286d : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        286f : 68              >            pla         ;load status
                               >            eor_flag 0
        2870 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2872 : dd3c02          >            cmp fRORc,x    ;test flags
                               >            trap_ne
        2875 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2877 : ca                       dex
        2878 : 10e3                     bpl trorc6
        287a : a203                     ldx #3
        287c :                  trorc7
                                        set_zx zp1,$ff
                               >            load_flag $ff
        287c : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        287e : 48              >            pha         ;use stack to load status
        287f : b513            >            lda zp1,x    ;load to indexed zeropage
        2881 : 950c            >            sta zpt,x
        2883 : 28              >            plp

        2884 : 760c                     ror zpt,x
                                        tst_zx rRORc,fRORc,$ff-fnzc
        2886 : 08              >            php         ;save flags
        2887 : b50c            >            lda zpt,x
        2889 : dd2c02          >            cmp rRORc,x    ;test result
                               >            trap_ne
        288c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        288e : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        288f : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2891 : dd3c02          >            cmp fRORc,x    ;test flags
                               >            trap_ne
        2894 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2896 : ca                       dex
        2897 : 10e3                     bpl trorc7
                                        next_test
        2899 : ad0002          >            lda test_case   ;previous test
        289c : c920            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        289e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0021 =                 >test_num = test_num + 1
        28a0 : a921            >            lda #test_num   ;*** next tests' number
        28a2 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; shifts - abs indexed
        28a5 : a203                     ldx #3
        28a7 :                  tasl8
                                        set_absx zp1,0
                               >            load_flag 0
        28a7 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        28a9 : 48              >            pha         ;use stack to load status
        28aa : b513            >            lda zp1,x    ;load to indexed memory
        28ac : 9d0302          >            sta abst,x
        28af : 28              >            plp

        28b0 : 1e0302                   asl abst,x
                                        tst_absx rASL,fASL,0
        28b3 : 08              >            php         ;save flags
        28b4 : bd0302          >            lda abst,x
        28b7 : dd2002          >            cmp rASL,x    ;test result
                               >            trap_ne
        28ba : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        28bc : 68              >            pla         ;load status
                               >            eor_flag 0
        28bd : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        28bf : dd3002          >            cmp fASL,x    ;test flags
                               >            trap_ne
        28c2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        28c4 : ca                       dex
        28c5 : 10e0                     bpl tasl8
        28c7 : a203                     ldx #3
        28c9 :                  tasl9
                                        set_absx zp1,$ff
                               >            load_flag $ff
        28c9 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        28cb : 48              >            pha         ;use stack to load status
        28cc : b513            >            lda zp1,x    ;load to indexed memory
        28ce : 9d0302          >            sta abst,x
        28d1 : 28              >            plp

        28d2 : 1e0302                   asl abst,x
                                        tst_absx rASL,fASL,$ff-fnzc
        28d5 : 08              >            php         ;save flags
        28d6 : bd0302          >            lda abst,x
        28d9 : dd2002          >            cmp rASL,x    ;test result
                               >            trap_ne
        28dc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        28de : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        28df : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        28e1 : dd3002          >            cmp fASL,x    ;test flags
                               >            trap_ne
        28e4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        28e6 : ca                       dex
        28e7 : 10e0                     bpl tasl9

        28e9 : a203                     ldx #3
        28eb :                  tlsr8
                                        set_absx zp1,0
                               >            load_flag 0
        28eb : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        28ed : 48              >            pha         ;use stack to load status
        28ee : b513            >            lda zp1,x    ;load to indexed memory
        28f0 : 9d0302          >            sta abst,x
        28f3 : 28              >            plp

        28f4 : 5e0302                   lsr abst,x
                                        tst_absx rLSR,fLSR,0
        28f7 : 08              >            php         ;save flags
        28f8 : bd0302          >            lda abst,x
        28fb : dd2802          >            cmp rLSR,x    ;test result
                               >            trap_ne
        28fe : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2900 : 68              >            pla         ;load status
                               >            eor_flag 0
        2901 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2903 : dd3802          >            cmp fLSR,x    ;test flags
                               >            trap_ne
        2906 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2908 : ca                       dex
        2909 : 10e0                     bpl tlsr8
        290b : a203                     ldx #3
        290d :                  tlsr9
                                        set_absx zp1,$ff
                               >            load_flag $ff
        290d : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        290f : 48              >            pha         ;use stack to load status
        2910 : b513            >            lda zp1,x    ;load to indexed memory
        2912 : 9d0302          >            sta abst,x
        2915 : 28              >            plp

        2916 : 5e0302                   lsr abst,x
                                        tst_absx rLSR,fLSR,$ff-fnzc
        2919 : 08              >            php         ;save flags
        291a : bd0302          >            lda abst,x
        291d : dd2802          >            cmp rLSR,x    ;test result
                               >            trap_ne
        2920 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2922 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2923 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2925 : dd3802          >            cmp fLSR,x    ;test flags
                               >            trap_ne
        2928 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        292a : ca                       dex
        292b : 10e0                     bpl tlsr9

        292d : a203                     ldx #3
        292f :                  trol8
                                        set_absx zp1,0
                               >            load_flag 0
        292f : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2931 : 48              >            pha         ;use stack to load status
        2932 : b513            >            lda zp1,x    ;load to indexed memory
        2934 : 9d0302          >            sta abst,x
        2937 : 28              >            plp

        2938 : 3e0302                   rol abst,x
                                        tst_absx rROL,fROL,0
        293b : 08              >            php         ;save flags
        293c : bd0302          >            lda abst,x
        293f : dd2002          >            cmp rROL,x    ;test result
                               >            trap_ne
        2942 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2944 : 68              >            pla         ;load status
                               >            eor_flag 0
        2945 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2947 : dd3002          >            cmp fROL,x    ;test flags
                               >            trap_ne
        294a : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        294c : ca                       dex
        294d : 10e0                     bpl trol8
        294f : a203                     ldx #3
        2951 :                  trol9
                                        set_absx zp1,$ff-fc
                               >            load_flag $ff-fc
        2951 : a9fe            >            lda #$ff-fc             ;allow test to change I-flag (no mask)
                               >
        2953 : 48              >            pha         ;use stack to load status
        2954 : b513            >            lda zp1,x    ;load to indexed memory
        2956 : 9d0302          >            sta abst,x
        2959 : 28              >            plp

        295a : 3e0302                   rol abst,x
                                        tst_absx rROL,fROL,$ff-fnzc
        295d : 08              >            php         ;save flags
        295e : bd0302          >            lda abst,x
        2961 : dd2002          >            cmp rROL,x    ;test result
                               >            trap_ne
        2964 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2966 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2967 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2969 : dd3002          >            cmp fROL,x    ;test flags
                               >            trap_ne
        296c : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        296e : ca                       dex
        296f : 10e0                     bpl trol9

        2971 : a203                     ldx #3
        2973 :                  trolc8
                                        set_absx zp1,fc
                               >            load_flag fc
        2973 : a901            >            lda #fc             ;allow test to change I-flag (no mask)
                               >
        2975 : 48              >            pha         ;use stack to load status
        2976 : b513            >            lda zp1,x    ;load to indexed memory
        2978 : 9d0302          >            sta abst,x
        297b : 28              >            plp

        297c : 3e0302                   rol abst,x
                                        tst_absx rROLc,fROLc,0
        297f : 08              >            php         ;save flags
        2980 : bd0302          >            lda abst,x
        2983 : dd2402          >            cmp rROLc,x    ;test result
                               >            trap_ne
        2986 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2988 : 68              >            pla         ;load status
                               >            eor_flag 0
        2989 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        298b : dd3402          >            cmp fROLc,x    ;test flags
                               >            trap_ne
        298e : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2990 : ca                       dex
        2991 : 10e0                     bpl trolc8
        2993 : a203                     ldx #3
        2995 :                  trolc9
                                        set_absx zp1,$ff
                               >            load_flag $ff
        2995 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2997 : 48              >            pha         ;use stack to load status
        2998 : b513            >            lda zp1,x    ;load to indexed memory
        299a : 9d0302          >            sta abst,x
        299d : 28              >            plp

        299e : 3e0302                   rol abst,x
                                        tst_absx rROLc,fROLc,$ff-fnzc
        29a1 : 08              >            php         ;save flags
        29a2 : bd0302          >            lda abst,x
        29a5 : dd2402          >            cmp rROLc,x    ;test result
                               >            trap_ne
        29a8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        29aa : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        29ab : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        29ad : dd3402          >            cmp fROLc,x    ;test flags
                               >            trap_ne
        29b0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        29b2 : ca                       dex
        29b3 : 10e0                     bpl trolc9

        29b5 : a203                     ldx #3
        29b7 :                  tror8
                                        set_absx zp1,0
                               >            load_flag 0
        29b7 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        29b9 : 48              >            pha         ;use stack to load status
        29ba : b513            >            lda zp1,x    ;load to indexed memory
        29bc : 9d0302          >            sta abst,x
        29bf : 28              >            plp

        29c0 : 7e0302                   ror abst,x
                                        tst_absx rROR,fROR,0
        29c3 : 08              >            php         ;save flags
        29c4 : bd0302          >            lda abst,x
        29c7 : dd2802          >            cmp rROR,x    ;test result
                               >            trap_ne
        29ca : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        29cc : 68              >            pla         ;load status
                               >            eor_flag 0
        29cd : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        29cf : dd3802          >            cmp fROR,x    ;test flags
                               >            trap_ne
        29d2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        29d4 : ca                       dex
        29d5 : 10e0                     bpl tror8
        29d7 : a203                     ldx #3
        29d9 :                  tror9
                                        set_absx zp1,$ff-fc
                               >            load_flag $ff-fc
        29d9 : a9fe            >            lda #$ff-fc             ;allow test to change I-flag (no mask)
                               >
        29db : 48              >            pha         ;use stack to load status
        29dc : b513            >            lda zp1,x    ;load to indexed memory
        29de : 9d0302          >            sta abst,x
        29e1 : 28              >            plp

        29e2 : 7e0302                   ror abst,x
                                        tst_absx rROR,fROR,$ff-fnzc
        29e5 : 08              >            php         ;save flags
        29e6 : bd0302          >            lda abst,x
        29e9 : dd2802          >            cmp rROR,x    ;test result
                               >            trap_ne
        29ec : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        29ee : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        29ef : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        29f1 : dd3802          >            cmp fROR,x    ;test flags
                               >            trap_ne
        29f4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        29f6 : ca                       dex
        29f7 : 10e0                     bpl tror9

        29f9 : a203                     ldx #3
        29fb :                  trorc8
                                        set_absx zp1,fc
                               >            load_flag fc
        29fb : a901            >            lda #fc             ;allow test to change I-flag (no mask)
                               >
        29fd : 48              >            pha         ;use stack to load status
        29fe : b513            >            lda zp1,x    ;load to indexed memory
        2a00 : 9d0302          >            sta abst,x
        2a03 : 28              >            plp

        2a04 : 7e0302                   ror abst,x
                                        tst_absx rRORc,fRORc,0
        2a07 : 08              >            php         ;save flags
        2a08 : bd0302          >            lda abst,x
        2a0b : dd2c02          >            cmp rRORc,x    ;test result
                               >            trap_ne
        2a0e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2a10 : 68              >            pla         ;load status
                               >            eor_flag 0
        2a11 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2a13 : dd3c02          >            cmp fRORc,x    ;test flags
                               >            trap_ne
        2a16 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2a18 : ca                       dex
        2a19 : 10e0                     bpl trorc8
        2a1b : a203                     ldx #3
        2a1d :                  trorc9
                                        set_absx zp1,$ff
                               >            load_flag $ff
        2a1d : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2a1f : 48              >            pha         ;use stack to load status
        2a20 : b513            >            lda zp1,x    ;load to indexed memory
        2a22 : 9d0302          >            sta abst,x
        2a25 : 28              >            plp

        2a26 : 7e0302                   ror abst,x
                                        tst_absx rRORc,fRORc,$ff-fnzc
        2a29 : 08              >            php         ;save flags
        2a2a : bd0302          >            lda abst,x
        2a2d : dd2c02          >            cmp rRORc,x    ;test result
                               >            trap_ne
        2a30 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2a32 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnzc
        2a33 : 497c            >            eor #$ff-fnzc|fao         ;invert expected flags + always on bits
                               >
        2a35 : dd3c02          >            cmp fRORc,x    ;test flags
                               >            trap_ne
        2a38 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2a3a : ca                       dex
        2a3b : 10e0                     bpl trorc9
                                        next_test
        2a3d : ad0002          >            lda test_case   ;previous test
        2a40 : c921            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        2a42 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0022 =                 >test_num = test_num + 1
        2a44 : a922            >            lda #test_num   ;*** next tests' number
        2a46 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; testing memory increment/decrement - INC DEC all addressing modes
                                ; zeropage
        2a49 : a200                     ldx #0
        2a4b : a97e                     lda #$7e
        2a4d : 850c                     sta zpt
        2a4f :                  tinc    
                                        set_stat 0
                               >            load_flag 0
        2a4f : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2a51 : 48              >            pha         ;use stack to load status
        2a52 : 28              >            plp

        2a53 : e60c                     inc zpt
                                        tst_z rINC,fINC,0
        2a55 : 08              >            php         ;save flags
        2a56 : a50c            >            lda zpt
        2a58 : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2a5b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2a5d : 68              >            pla         ;load status
                               >            eor_flag 0
        2a5e : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2a60 : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2a63 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2a65 : e8                       inx
        2a66 : e002                     cpx #2
        2a68 : d004                     bne tinc1
        2a6a : a9fe                     lda #$fe
        2a6c : 850c                     sta zpt
        2a6e : e005             tinc1   cpx #5
        2a70 : d0dd                     bne tinc
        2a72 : ca                       dex
        2a73 : e60c                     inc zpt
        2a75 :                  tdec    
                                        set_stat 0
                               >            load_flag 0
        2a75 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2a77 : 48              >            pha         ;use stack to load status
        2a78 : 28              >            plp

        2a79 : c60c                     dec zpt
                                        tst_z rINC,fINC,0
        2a7b : 08              >            php         ;save flags
        2a7c : a50c            >            lda zpt
        2a7e : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2a81 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2a83 : 68              >            pla         ;load status
                               >            eor_flag 0
        2a84 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2a86 : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2a89 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2a8b : ca                       dex
        2a8c : 300a                     bmi tdec1
        2a8e : e001                     cpx #1
        2a90 : d0e3                     bne tdec
        2a92 : a981                     lda #$81
        2a94 : 850c                     sta zpt
        2a96 : d0dd                     bne tdec
        2a98 :                  tdec1
        2a98 : a200                     ldx #0
        2a9a : a97e                     lda #$7e
        2a9c : 850c                     sta zpt
        2a9e :                  tinc10    
                                        set_stat $ff
                               >            load_flag $ff
        2a9e : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2aa0 : 48              >            pha         ;use stack to load status
        2aa1 : 28              >            plp

        2aa2 : e60c                     inc zpt
                                        tst_z rINC,fINC,$ff-fnz
        2aa4 : 08              >            php         ;save flags
        2aa5 : a50c            >            lda zpt
        2aa7 : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2aaa : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2aac : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2aad : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2aaf : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2ab2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2ab4 : e8                       inx
        2ab5 : e002                     cpx #2
        2ab7 : d004                     bne tinc11
        2ab9 : a9fe                     lda #$fe
        2abb : 850c                     sta zpt
        2abd : e005             tinc11  cpx #5
        2abf : d0dd                     bne tinc10
        2ac1 : ca                       dex
        2ac2 : e60c                     inc zpt
        2ac4 :                  tdec10    
                                        set_stat $ff
                               >            load_flag $ff
        2ac4 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2ac6 : 48              >            pha         ;use stack to load status
        2ac7 : 28              >            plp

        2ac8 : c60c                     dec zpt
                                        tst_z rINC,fINC,$ff-fnz
        2aca : 08              >            php         ;save flags
        2acb : a50c            >            lda zpt
        2acd : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2ad0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2ad2 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2ad3 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2ad5 : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2ad8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2ada : ca                       dex
        2adb : 300a                     bmi tdec11
        2add : e001                     cpx #1
        2adf : d0e3                     bne tdec10
        2ae1 : a981                     lda #$81
        2ae3 : 850c                     sta zpt
        2ae5 : d0dd                     bne tdec10
        2ae7 :                  tdec11
                                        next_test
        2ae7 : ad0002          >            lda test_case   ;previous test
        2aea : c922            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        2aec : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0023 =                 >test_num = test_num + 1
        2aee : a923            >            lda #test_num   ;*** next tests' number
        2af0 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; absolute memory
        2af3 : a200                     ldx #0
        2af5 : a97e                     lda #$7e
        2af7 : 8d0302                   sta abst
        2afa :                  tinc2    
                                        set_stat 0
                               >            load_flag 0
        2afa : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2afc : 48              >            pha         ;use stack to load status
        2afd : 28              >            plp

        2afe : ee0302                   inc abst
                                        tst_abs rINC,fINC,0
        2b01 : 08              >            php         ;save flags
        2b02 : ad0302          >            lda abst
        2b05 : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2b08 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2b0a : 68              >            pla         ;load status
                               >            eor_flag 0
        2b0b : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2b0d : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2b10 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2b12 : e8                       inx
        2b13 : e002                     cpx #2
        2b15 : d005                     bne tinc3
        2b17 : a9fe                     lda #$fe
        2b19 : 8d0302                   sta abst
        2b1c : e005             tinc3   cpx #5
        2b1e : d0da                     bne tinc2
        2b20 : ca                       dex
        2b21 : ee0302                   inc abst
        2b24 :                  tdec2    
                                        set_stat 0
                               >            load_flag 0
        2b24 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2b26 : 48              >            pha         ;use stack to load status
        2b27 : 28              >            plp

        2b28 : ce0302                   dec abst
                                        tst_abs rINC,fINC,0
        2b2b : 08              >            php         ;save flags
        2b2c : ad0302          >            lda abst
        2b2f : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2b32 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2b34 : 68              >            pla         ;load status
                               >            eor_flag 0
        2b35 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2b37 : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2b3a : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2b3c : ca                       dex
        2b3d : 300b                     bmi tdec3
        2b3f : e001                     cpx #1
        2b41 : d0e1                     bne tdec2
        2b43 : a981                     lda #$81
        2b45 : 8d0302                   sta abst
        2b48 : d0da                     bne tdec2
        2b4a :                  tdec3
        2b4a : a200                     ldx #0
        2b4c : a97e                     lda #$7e
        2b4e : 8d0302                   sta abst
        2b51 :                  tinc12    
                                        set_stat $ff
                               >            load_flag $ff
        2b51 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2b53 : 48              >            pha         ;use stack to load status
        2b54 : 28              >            plp

        2b55 : ee0302                   inc abst
                                        tst_abs rINC,fINC,$ff-fnz
        2b58 : 08              >            php         ;save flags
        2b59 : ad0302          >            lda abst
        2b5c : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2b5f : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2b61 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2b62 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2b64 : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2b67 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2b69 : e8                       inx
        2b6a : e002                     cpx #2
        2b6c : d005                     bne tinc13
        2b6e : a9fe                     lda #$fe
        2b70 : 8d0302                   sta abst
        2b73 : e005             tinc13   cpx #5
        2b75 : d0da                     bne tinc12
        2b77 : ca                       dex
        2b78 : ee0302                   inc abst
        2b7b :                  tdec12    
                                        set_stat $ff
                               >            load_flag $ff
        2b7b : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2b7d : 48              >            pha         ;use stack to load status
        2b7e : 28              >            plp

        2b7f : ce0302                   dec abst
                                        tst_abs rINC,fINC,$ff-fnz
        2b82 : 08              >            php         ;save flags
        2b83 : ad0302          >            lda abst
        2b86 : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2b89 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2b8b : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2b8c : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2b8e : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2b91 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2b93 : ca                       dex
        2b94 : 300b                     bmi tdec13
        2b96 : e001                     cpx #1
        2b98 : d0e1                     bne tdec12
        2b9a : a981                     lda #$81
        2b9c : 8d0302                   sta abst
        2b9f : d0da                     bne tdec12
        2ba1 :                  tdec13
                                        next_test
        2ba1 : ad0002          >            lda test_case   ;previous test
        2ba4 : c923            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        2ba6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0024 =                 >test_num = test_num + 1
        2ba8 : a924            >            lda #test_num   ;*** next tests' number
        2baa : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; zeropage indexed
        2bad : a200                     ldx #0
        2baf : a97e                     lda #$7e
        2bb1 : 950c             tinc4   sta zpt,x
                                        set_stat 0
                               >            load_flag 0
        2bb3 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2bb5 : 48              >            pha         ;use stack to load status
        2bb6 : 28              >            plp

        2bb7 : f60c                     inc zpt,x
                                        tst_zx rINC,fINC,0
        2bb9 : 08              >            php         ;save flags
        2bba : b50c            >            lda zpt,x
        2bbc : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2bbf : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2bc1 : 68              >            pla         ;load status
                               >            eor_flag 0
        2bc2 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2bc4 : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2bc7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2bc9 : b50c                     lda zpt,x
        2bcb : e8                       inx
        2bcc : e002                     cpx #2
        2bce : d002                     bne tinc5
        2bd0 : a9fe                     lda #$fe
        2bd2 : e005             tinc5   cpx #5
        2bd4 : d0db                     bne tinc4
        2bd6 : ca                       dex
        2bd7 : a902                     lda #2
        2bd9 : 950c             tdec4   sta zpt,x 
                                        set_stat 0
                               >            load_flag 0
        2bdb : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2bdd : 48              >            pha         ;use stack to load status
        2bde : 28              >            plp

        2bdf : d60c                     dec zpt,x
                                        tst_zx rINC,fINC,0
        2be1 : 08              >            php         ;save flags
        2be2 : b50c            >            lda zpt,x
        2be4 : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2be7 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2be9 : 68              >            pla         ;load status
                               >            eor_flag 0
        2bea : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2bec : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2bef : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2bf1 : b50c                     lda zpt,x
        2bf3 : ca                       dex
        2bf4 : 3008                     bmi tdec5
        2bf6 : e001                     cpx #1
        2bf8 : d0df                     bne tdec4
        2bfa : a981                     lda #$81
        2bfc : d0db                     bne tdec4
        2bfe :                  tdec5
        2bfe : a200                     ldx #0
        2c00 : a97e                     lda #$7e
        2c02 : 950c             tinc14  sta zpt,x
                                        set_stat $ff
                               >            load_flag $ff
        2c04 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2c06 : 48              >            pha         ;use stack to load status
        2c07 : 28              >            plp

        2c08 : f60c                     inc zpt,x
                                        tst_zx rINC,fINC,$ff-fnz
        2c0a : 08              >            php         ;save flags
        2c0b : b50c            >            lda zpt,x
        2c0d : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2c10 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2c12 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2c13 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2c15 : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2c18 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2c1a : b50c                     lda zpt,x
        2c1c : e8                       inx
        2c1d : e002                     cpx #2
        2c1f : d002                     bne tinc15
        2c21 : a9fe                     lda #$fe
        2c23 : e005             tinc15  cpx #5
        2c25 : d0db                     bne tinc14
        2c27 : ca                       dex
        2c28 : a902                     lda #2
        2c2a : 950c             tdec14  sta zpt,x 
                                        set_stat $ff
                               >            load_flag $ff
        2c2c : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2c2e : 48              >            pha         ;use stack to load status
        2c2f : 28              >            plp

        2c30 : d60c                     dec zpt,x
                                        tst_zx rINC,fINC,$ff-fnz
        2c32 : 08              >            php         ;save flags
        2c33 : b50c            >            lda zpt,x
        2c35 : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2c38 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2c3a : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2c3b : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2c3d : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2c40 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2c42 : b50c                     lda zpt,x
        2c44 : ca                       dex
        2c45 : 3008                     bmi tdec15
        2c47 : e001                     cpx #1
        2c49 : d0df                     bne tdec14
        2c4b : a981                     lda #$81
        2c4d : d0db                     bne tdec14
        2c4f :                  tdec15
                                        next_test
        2c4f : ad0002          >            lda test_case   ;previous test
        2c52 : c924            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        2c54 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0025 =                 >test_num = test_num + 1
        2c56 : a925            >            lda #test_num   ;*** next tests' number
        2c58 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; memory indexed
        2c5b : a200                     ldx #0
        2c5d : a97e                     lda #$7e
        2c5f : 9d0302           tinc6   sta abst,x
                                        set_stat 0
                               >            load_flag 0
        2c62 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2c64 : 48              >            pha         ;use stack to load status
        2c65 : 28              >            plp

        2c66 : fe0302                   inc abst,x
                                        tst_absx rINC,fINC,0
        2c69 : 08              >            php         ;save flags
        2c6a : bd0302          >            lda abst,x
        2c6d : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2c70 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2c72 : 68              >            pla         ;load status
                               >            eor_flag 0
        2c73 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2c75 : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2c78 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2c7a : bd0302                   lda abst,x
        2c7d : e8                       inx
        2c7e : e002                     cpx #2
        2c80 : d002                     bne tinc7
        2c82 : a9fe                     lda #$fe
        2c84 : e005             tinc7   cpx #5
        2c86 : d0d7                     bne tinc6
        2c88 : ca                       dex
        2c89 : a902                     lda #2
        2c8b : 9d0302           tdec6   sta abst,x 
                                        set_stat 0
                               >            load_flag 0
        2c8e : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2c90 : 48              >            pha         ;use stack to load status
        2c91 : 28              >            plp

        2c92 : de0302                   dec abst,x
                                        tst_absx rINC,fINC,0
        2c95 : 08              >            php         ;save flags
        2c96 : bd0302          >            lda abst,x
        2c99 : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2c9c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2c9e : 68              >            pla         ;load status
                               >            eor_flag 0
        2c9f : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2ca1 : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2ca4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2ca6 : bd0302                   lda abst,x
        2ca9 : ca                       dex
        2caa : 3008                     bmi tdec7
        2cac : e001                     cpx #1
        2cae : d0db                     bne tdec6
        2cb0 : a981                     lda #$81
        2cb2 : d0d7                     bne tdec6
        2cb4 :                  tdec7
        2cb4 : a200                     ldx #0
        2cb6 : a97e                     lda #$7e
        2cb8 : 9d0302           tinc16  sta abst,x
                                        set_stat $ff
                               >            load_flag $ff
        2cbb : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2cbd : 48              >            pha         ;use stack to load status
        2cbe : 28              >            plp

        2cbf : fe0302                   inc abst,x
                                        tst_absx rINC,fINC,$ff-fnz
        2cc2 : 08              >            php         ;save flags
        2cc3 : bd0302          >            lda abst,x
        2cc6 : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2cc9 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2ccb : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2ccc : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2cce : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2cd1 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2cd3 : bd0302                   lda abst,x
        2cd6 : e8                       inx
        2cd7 : e002                     cpx #2
        2cd9 : d002                     bne tinc17
        2cdb : a9fe                     lda #$fe
        2cdd : e005             tinc17  cpx #5
        2cdf : d0d7                     bne tinc16
        2ce1 : ca                       dex
        2ce2 : a902                     lda #2
        2ce4 : 9d0302           tdec16  sta abst,x 
                                        set_stat $ff
                               >            load_flag $ff
        2ce7 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2ce9 : 48              >            pha         ;use stack to load status
        2cea : 28              >            plp

        2ceb : de0302                   dec abst,x
                                        tst_absx rINC,fINC,$ff-fnz
        2cee : 08              >            php         ;save flags
        2cef : bd0302          >            lda abst,x
        2cf2 : dd4002          >            cmp rINC,x    ;test result
                               >            trap_ne
        2cf5 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2cf7 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2cf8 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2cfa : dd4502          >            cmp fINC,x    ;test flags
                               >            trap_ne
        2cfd : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2cff : bd0302                   lda abst,x
        2d02 : ca                       dex
        2d03 : 3008                     bmi tdec17
        2d05 : e001                     cpx #1
        2d07 : d0db                     bne tdec16
        2d09 : a981                     lda #$81
        2d0b : d0d7                     bne tdec16
        2d0d :                  tdec17
                                        next_test
        2d0d : ad0002          >            lda test_case   ;previous test
        2d10 : c925            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        2d12 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0026 =                 >test_num = test_num + 1
        2d14 : a926            >            lda #test_num   ;*** next tests' number
        2d16 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; testing logical instructions - AND EOR ORA all addressing modes
                                ; AND
        2d19 : a203                     ldx #3          ;immediate
        2d1b : b51c             tand    lda zpAN,x
        2d1d : 8d0902                   sta ex_andi+1   ;set AND # operand
                                        set_ax  absANa,0
                               >            load_flag 0
        2d20 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2d22 : 48              >            pha         ;use stack to load status
        2d23 : bd5a02          >            lda absANa,x    ;precharge accu
        2d26 : 28              >            plp

        2d27 : 200802                   jsr ex_andi     ;execute AND # in RAM
                                        tst_ax  absrlo,absflo,0
        2d2a : 08              >            php         ;save flags
        2d2b : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2d2e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2d30 : 68              >            pla         ;load status
                               >            eor_flag 0
        2d31 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2d33 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2d36 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2d38 : ca                       dex
        2d39 : 10e0                     bpl tand
        2d3b : a203                     ldx #3
        2d3d : b51c             tand1   lda zpAN,x
        2d3f : 8d0902                   sta ex_andi+1   ;set AND # operand
                                        set_ax  absANa,$ff
                               >            load_flag $ff
        2d42 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2d44 : 48              >            pha         ;use stack to load status
        2d45 : bd5a02          >            lda absANa,x    ;precharge accu
        2d48 : 28              >            plp

        2d49 : 200802                   jsr ex_andi     ;execute AND # in RAM
                                        tst_ax  absrlo,absflo,$ff-fnz
        2d4c : 08              >            php         ;save flags
        2d4d : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2d50 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2d52 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2d53 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2d55 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2d58 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2d5a : ca                       dex
        2d5b : 10e0                     bpl tand1

        2d5d : a203                     ldx #3      ;zp
        2d5f : b51c             tand2   lda zpAN,x
        2d61 : 850c                     sta zpt
                                        set_ax  absANa,0
                               >            load_flag 0
        2d63 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2d65 : 48              >            pha         ;use stack to load status
        2d66 : bd5a02          >            lda absANa,x    ;precharge accu
        2d69 : 28              >            plp

        2d6a : 250c                     and zpt
                                        tst_ax  absrlo,absflo,0
        2d6c : 08              >            php         ;save flags
        2d6d : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2d70 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2d72 : 68              >            pla         ;load status
                               >            eor_flag 0
        2d73 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2d75 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2d78 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2d7a : ca                       dex
        2d7b : 10e2                     bpl tand2
        2d7d : a203                     ldx #3
        2d7f : b51c             tand3   lda zpAN,x
        2d81 : 850c                     sta zpt
                                        set_ax  absANa,$ff
                               >            load_flag $ff
        2d83 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2d85 : 48              >            pha         ;use stack to load status
        2d86 : bd5a02          >            lda absANa,x    ;precharge accu
        2d89 : 28              >            plp

        2d8a : 250c                     and zpt
                                        tst_ax  absrlo,absflo,$ff-fnz
        2d8c : 08              >            php         ;save flags
        2d8d : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2d90 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2d92 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2d93 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2d95 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2d98 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2d9a : ca                       dex
        2d9b : 10e2                     bpl tand3

        2d9d : a203                     ldx #3      ;abs
        2d9f : b51c             tand4   lda zpAN,x
        2da1 : 8d0302                   sta abst
                                        set_ax  absANa,0
                               >            load_flag 0
        2da4 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2da6 : 48              >            pha         ;use stack to load status
        2da7 : bd5a02          >            lda absANa,x    ;precharge accu
        2daa : 28              >            plp

        2dab : 2d0302                   and abst
                                        tst_ax  absrlo,absflo,0
        2dae : 08              >            php         ;save flags
        2daf : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2db2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2db4 : 68              >            pla         ;load status
                               >            eor_flag 0
        2db5 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2db7 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2dba : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2dbc : ca                       dex
        2dbd : 10e0                     bpl tand4
        2dbf : a203                     ldx #3
        2dc1 : b51c             tand5   lda zpAN,x
        2dc3 : 8d0302                   sta abst
                                        set_ax  absANa,$ff
                               >            load_flag $ff
        2dc6 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2dc8 : 48              >            pha         ;use stack to load status
        2dc9 : bd5a02          >            lda absANa,x    ;precharge accu
        2dcc : 28              >            plp

        2dcd : 2d0302                   and abst
                                        tst_ax  absrlo,absflo,$ff-fnz
        2dd0 : 08              >            php         ;save flags
        2dd1 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2dd4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2dd6 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2dd7 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2dd9 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2ddc : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2dde : ca                       dex
        2ddf : 1002                     bpl tand6

        2de1 : a203                     ldx #3      ;zp,x
        2de3 :                  tand6
                                        set_ax  absANa,0
                               >            load_flag 0
        2de3 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2de5 : 48              >            pha         ;use stack to load status
        2de6 : bd5a02          >            lda absANa,x    ;precharge accu
        2de9 : 28              >            plp

        2dea : 351c                     and zpAN,x
                                        tst_ax  absrlo,absflo,0
        2dec : 08              >            php         ;save flags
        2ded : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2df0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2df2 : 68              >            pla         ;load status
                               >            eor_flag 0
        2df3 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2df5 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2df8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2dfa : ca                       dex
        2dfb : 10e6                     bpl tand6
        2dfd : a203                     ldx #3
        2dff :                  tand7
                                        set_ax  absANa,$ff
                               >            load_flag $ff
        2dff : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2e01 : 48              >            pha         ;use stack to load status
        2e02 : bd5a02          >            lda absANa,x    ;precharge accu
        2e05 : 28              >            plp

        2e06 : 351c                     and zpAN,x
                                        tst_ax  absrlo,absflo,$ff-fnz
        2e08 : 08              >            php         ;save flags
        2e09 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2e0c : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2e0e : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2e0f : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2e11 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2e14 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2e16 : ca                       dex
        2e17 : 10e6                     bpl tand7

        2e19 : a203                     ldx #3      ;abs,x
        2e1b :                  tand8
                                        set_ax  absANa,0
                               >            load_flag 0
        2e1b : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2e1d : 48              >            pha         ;use stack to load status
        2e1e : bd5a02          >            lda absANa,x    ;precharge accu
        2e21 : 28              >            plp

        2e22 : 3d4e02                   and absAN,x
                                        tst_ax  absrlo,absflo,0
        2e25 : 08              >            php         ;save flags
        2e26 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2e29 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2e2b : 68              >            pla         ;load status
                               >            eor_flag 0
        2e2c : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2e2e : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2e31 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2e33 : ca                       dex
        2e34 : 10e5                     bpl tand8
        2e36 : a203                     ldx #3
        2e38 :                  tand9
                                        set_ax  absANa,$ff
                               >            load_flag $ff
        2e38 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2e3a : 48              >            pha         ;use stack to load status
        2e3b : bd5a02          >            lda absANa,x    ;precharge accu
        2e3e : 28              >            plp

        2e3f : 3d4e02                   and absAN,x
                                        tst_ax  absrlo,absflo,$ff-fnz
        2e42 : 08              >            php         ;save flags
        2e43 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2e46 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2e48 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2e49 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2e4b : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2e4e : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2e50 : ca                       dex
        2e51 : 10e5                     bpl tand9

        2e53 : a003                     ldy #3      ;abs,y
        2e55 :                  tand10
                                        set_ay  absANa,0
                               >            load_flag 0
        2e55 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2e57 : 48              >            pha         ;use stack to load status
        2e58 : b95a02          >            lda absANa,y    ;precharge accu
        2e5b : 28              >            plp

        2e5c : 394e02                   and absAN,y
                                        tst_ay  absrlo,absflo,0
        2e5f : 08              >            php         ;save flags
        2e60 : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        2e63 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2e65 : 68              >            pla         ;load status
                               >            eor_flag 0
        2e66 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2e68 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        2e6b : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2e6d : 88                       dey
        2e6e : 10e5                     bpl tand10
        2e70 : a003                     ldy #3
        2e72 :                  tand11
                                        set_ay  absANa,$ff
                               >            load_flag $ff
        2e72 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2e74 : 48              >            pha         ;use stack to load status
        2e75 : b95a02          >            lda absANa,y    ;precharge accu
        2e78 : 28              >            plp

        2e79 : 394e02                   and absAN,y
                                        tst_ay  absrlo,absflo,$ff-fnz
        2e7c : 08              >            php         ;save flags
        2e7d : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        2e80 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2e82 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2e83 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2e85 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        2e88 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2e8a : 88                       dey
        2e8b : 10e5                     bpl tand11

        2e8d : a206                     ldx #6      ;(zp,x)
        2e8f : a003                     ldy #3
        2e91 :                  tand12
                                        set_ay  absANa,0
                               >            load_flag 0
        2e91 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2e93 : 48              >            pha         ;use stack to load status
        2e94 : b95a02          >            lda absANa,y    ;precharge accu
        2e97 : 28              >            plp

        2e98 : 213a                     and (indAN,x)
                                        tst_ay  absrlo,absflo,0
        2e9a : 08              >            php         ;save flags
        2e9b : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        2e9e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2ea0 : 68              >            pla         ;load status
                               >            eor_flag 0
        2ea1 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2ea3 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        2ea6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2ea8 : ca                       dex
        2ea9 : ca                       dex
        2eaa : 88                       dey
        2eab : 10e4                     bpl tand12
        2ead : a206                     ldx #6
        2eaf : a003                     ldy #3
        2eb1 :                  tand13
                                        set_ay  absANa,$ff
                               >            load_flag $ff
        2eb1 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2eb3 : 48              >            pha         ;use stack to load status
        2eb4 : b95a02          >            lda absANa,y    ;precharge accu
        2eb7 : 28              >            plp

        2eb8 : 213a                     and (indAN,x)
                                        tst_ay  absrlo,absflo,$ff-fnz
        2eba : 08              >            php         ;save flags
        2ebb : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        2ebe : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2ec0 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2ec1 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2ec3 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        2ec6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2ec8 : ca                       dex
        2ec9 : ca                       dex
        2eca : 88                       dey
        2ecb : 10e4                     bpl tand13

        2ecd : a003                     ldy #3      ;(zp),y
        2ecf :                  tand14
                                        set_ay  absANa,0
                               >            load_flag 0
        2ecf : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2ed1 : 48              >            pha         ;use stack to load status
        2ed2 : b95a02          >            lda absANa,y    ;precharge accu
        2ed5 : 28              >            plp

        2ed6 : 313a                     and (indAN),y
                                        tst_ay  absrlo,absflo,0
        2ed8 : 08              >            php         ;save flags
        2ed9 : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        2edc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2ede : 68              >            pla         ;load status
                               >            eor_flag 0
        2edf : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2ee1 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        2ee4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2ee6 : 88                       dey
        2ee7 : 10e6                     bpl tand14
        2ee9 : a003                     ldy #3
        2eeb :                  tand15
                                        set_ay  absANa,$ff
                               >            load_flag $ff
        2eeb : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2eed : 48              >            pha         ;use stack to load status
        2eee : b95a02          >            lda absANa,y    ;precharge accu
        2ef1 : 28              >            plp

        2ef2 : 313a                     and (indAN),y
                                        tst_ay  absrlo,absflo,$ff-fnz
        2ef4 : 08              >            php         ;save flags
        2ef5 : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        2ef8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2efa : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2efb : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2efd : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        2f00 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2f02 : 88                       dey
        2f03 : 10e6                     bpl tand15
                                        next_test
        2f05 : ad0002          >            lda test_case   ;previous test
        2f08 : c926            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        2f0a : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0027 =                 >test_num = test_num + 1
        2f0c : a927            >            lda #test_num   ;*** next tests' number
        2f0e : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; EOR
        2f11 : a203                     ldx #3          ;immediate - self modifying code
        2f13 : b520             teor    lda zpEO,x
        2f15 : 8d0c02                   sta ex_eori+1   ;set EOR # operand
                                        set_ax  absEOa,0
                               >            load_flag 0
        2f18 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2f1a : 48              >            pha         ;use stack to load status
        2f1b : bd5e02          >            lda absEOa,x    ;precharge accu
        2f1e : 28              >            plp

        2f1f : 200b02                   jsr ex_eori     ;execute EOR # in RAM
                                        tst_ax  absrlo,absflo,0
        2f22 : 08              >            php         ;save flags
        2f23 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2f26 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2f28 : 68              >            pla         ;load status
                               >            eor_flag 0
        2f29 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2f2b : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2f2e : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2f30 : ca                       dex
        2f31 : 10e0                     bpl teor
        2f33 : a203                     ldx #3
        2f35 : b520             teor1   lda zpEO,x
        2f37 : 8d0c02                   sta ex_eori+1   ;set EOR # operand
                                        set_ax  absEOa,$ff
                               >            load_flag $ff
        2f3a : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2f3c : 48              >            pha         ;use stack to load status
        2f3d : bd5e02          >            lda absEOa,x    ;precharge accu
        2f40 : 28              >            plp

        2f41 : 200b02                   jsr ex_eori     ;execute EOR # in RAM
                                        tst_ax  absrlo,absflo,$ff-fnz
        2f44 : 08              >            php         ;save flags
        2f45 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2f48 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2f4a : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2f4b : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2f4d : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2f50 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2f52 : ca                       dex
        2f53 : 10e0                     bpl teor1

        2f55 : a203                     ldx #3      ;zp
        2f57 : b520             teor2    lda zpEO,x
        2f59 : 850c                     sta zpt
                                        set_ax  absEOa,0
                               >            load_flag 0
        2f5b : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2f5d : 48              >            pha         ;use stack to load status
        2f5e : bd5e02          >            lda absEOa,x    ;precharge accu
        2f61 : 28              >            plp

        2f62 : 450c                     eor zpt
                                        tst_ax  absrlo,absflo,0
        2f64 : 08              >            php         ;save flags
        2f65 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2f68 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2f6a : 68              >            pla         ;load status
                               >            eor_flag 0
        2f6b : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2f6d : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2f70 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2f72 : ca                       dex
        2f73 : 10e2                     bpl teor2
        2f75 : a203                     ldx #3
        2f77 : b520             teor3   lda zpEO,x
        2f79 : 850c                     sta zpt
                                        set_ax  absEOa,$ff
                               >            load_flag $ff
        2f7b : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2f7d : 48              >            pha         ;use stack to load status
        2f7e : bd5e02          >            lda absEOa,x    ;precharge accu
        2f81 : 28              >            plp

        2f82 : 450c                     eor zpt
                                        tst_ax  absrlo,absflo,$ff-fnz
        2f84 : 08              >            php         ;save flags
        2f85 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2f88 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2f8a : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2f8b : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2f8d : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2f90 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2f92 : ca                       dex
        2f93 : 10e2                     bpl teor3

        2f95 : a203                     ldx #3      ;abs
        2f97 : b520             teor4   lda zpEO,x
        2f99 : 8d0302                   sta abst
                                        set_ax  absEOa,0
                               >            load_flag 0
        2f9c : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2f9e : 48              >            pha         ;use stack to load status
        2f9f : bd5e02          >            lda absEOa,x    ;precharge accu
        2fa2 : 28              >            plp

        2fa3 : 4d0302                   eor abst
                                        tst_ax  absrlo,absflo,0
        2fa6 : 08              >            php         ;save flags
        2fa7 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2faa : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2fac : 68              >            pla         ;load status
                               >            eor_flag 0
        2fad : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2faf : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2fb2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2fb4 : ca                       dex
        2fb5 : 10e0                     bpl teor4
        2fb7 : a203                     ldx #3
        2fb9 : b520             teor5   lda zpEO,x
        2fbb : 8d0302                   sta abst
                                        set_ax  absEOa,$ff
                               >            load_flag $ff
        2fbe : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2fc0 : 48              >            pha         ;use stack to load status
        2fc1 : bd5e02          >            lda absEOa,x    ;precharge accu
        2fc4 : 28              >            plp

        2fc5 : 4d0302                   eor abst
                                        tst_ax  absrlo,absflo,$ff-fnz
        2fc8 : 08              >            php         ;save flags
        2fc9 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2fcc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2fce : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        2fcf : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        2fd1 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2fd4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2fd6 : ca                       dex
        2fd7 : 1002                     bpl teor6

        2fd9 : a203                     ldx #3      ;zp,x
        2fdb :                  teor6
                                        set_ax  absEOa,0
                               >            load_flag 0
        2fdb : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        2fdd : 48              >            pha         ;use stack to load status
        2fde : bd5e02          >            lda absEOa,x    ;precharge accu
        2fe1 : 28              >            plp

        2fe2 : 5520                     eor zpEO,x
                                        tst_ax  absrlo,absflo,0
        2fe4 : 08              >            php         ;save flags
        2fe5 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        2fe8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        2fea : 68              >            pla         ;load status
                               >            eor_flag 0
        2feb : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        2fed : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        2ff0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        2ff2 : ca                       dex
        2ff3 : 10e6                     bpl teor6
        2ff5 : a203                     ldx #3
        2ff7 :                  teor7
                                        set_ax  absEOa,$ff
                               >            load_flag $ff
        2ff7 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        2ff9 : 48              >            pha         ;use stack to load status
        2ffa : bd5e02          >            lda absEOa,x    ;precharge accu
        2ffd : 28              >            plp

        2ffe : 5520                     eor zpEO,x
                                        tst_ax  absrlo,absflo,$ff-fnz
        3000 : 08              >            php         ;save flags
        3001 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        3004 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3006 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        3007 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        3009 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        300c : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        300e : ca                       dex
        300f : 10e6                     bpl teor7

        3011 : a203                     ldx #3      ;abs,x
        3013 :                  teor8
                                        set_ax  absEOa,0
                               >            load_flag 0
        3013 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        3015 : 48              >            pha         ;use stack to load status
        3016 : bd5e02          >            lda absEOa,x    ;precharge accu
        3019 : 28              >            plp

        301a : 5d5202                   eor absEO,x
                                        tst_ax  absrlo,absflo,0
        301d : 08              >            php         ;save flags
        301e : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        3021 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3023 : 68              >            pla         ;load status
                               >            eor_flag 0
        3024 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        3026 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        3029 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        302b : ca                       dex
        302c : 10e5                     bpl teor8
        302e : a203                     ldx #3
        3030 :                  teor9
                                        set_ax  absEOa,$ff
                               >            load_flag $ff
        3030 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        3032 : 48              >            pha         ;use stack to load status
        3033 : bd5e02          >            lda absEOa,x    ;precharge accu
        3036 : 28              >            plp

        3037 : 5d5202                   eor absEO,x
                                        tst_ax  absrlo,absflo,$ff-fnz
        303a : 08              >            php         ;save flags
        303b : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        303e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3040 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        3041 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        3043 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        3046 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        3048 : ca                       dex
        3049 : 10e5                     bpl teor9

        304b : a003                     ldy #3      ;abs,y
        304d :                  teor10
                                        set_ay  absEOa,0
                               >            load_flag 0
        304d : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        304f : 48              >            pha         ;use stack to load status
        3050 : b95e02          >            lda absEOa,y    ;precharge accu
        3053 : 28              >            plp

        3054 : 595202                   eor absEO,y
                                        tst_ay  absrlo,absflo,0
        3057 : 08              >            php         ;save flags
        3058 : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        305b : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        305d : 68              >            pla         ;load status
                               >            eor_flag 0
        305e : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        3060 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        3063 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        3065 : 88                       dey
        3066 : 10e5                     bpl teor10
        3068 : a003                     ldy #3
        306a :                  teor11
                                        set_ay  absEOa,$ff
                               >            load_flag $ff
        306a : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        306c : 48              >            pha         ;use stack to load status
        306d : b95e02          >            lda absEOa,y    ;precharge accu
        3070 : 28              >            plp

        3071 : 595202                   eor absEO,y
                                        tst_ay  absrlo,absflo,$ff-fnz
        3074 : 08              >            php         ;save flags
        3075 : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        3078 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        307a : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        307b : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        307d : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        3080 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        3082 : 88                       dey
        3083 : 10e5                     bpl teor11

        3085 : a206                     ldx #6      ;(zp,x)
        3087 : a003                     ldy #3
        3089 :                  teor12
                                        set_ay  absEOa,0
                               >            load_flag 0
        3089 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        308b : 48              >            pha         ;use stack to load status
        308c : b95e02          >            lda absEOa,y    ;precharge accu
        308f : 28              >            plp

        3090 : 4142                     eor (indEO,x)
                                        tst_ay  absrlo,absflo,0
        3092 : 08              >            php         ;save flags
        3093 : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        3096 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3098 : 68              >            pla         ;load status
                               >            eor_flag 0
        3099 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        309b : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        309e : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        30a0 : ca                       dex
        30a1 : ca                       dex
        30a2 : 88                       dey
        30a3 : 10e4                     bpl teor12
        30a5 : a206                     ldx #6
        30a7 : a003                     ldy #3
        30a9 :                  teor13
                                        set_ay  absEOa,$ff
                               >            load_flag $ff
        30a9 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        30ab : 48              >            pha         ;use stack to load status
        30ac : b95e02          >            lda absEOa,y    ;precharge accu
        30af : 28              >            plp

        30b0 : 4142                     eor (indEO,x)
                                        tst_ay  absrlo,absflo,$ff-fnz
        30b2 : 08              >            php         ;save flags
        30b3 : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        30b6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        30b8 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        30b9 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        30bb : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        30be : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        30c0 : ca                       dex
        30c1 : ca                       dex
        30c2 : 88                       dey
        30c3 : 10e4                     bpl teor13

        30c5 : a003                     ldy #3      ;(zp),y
        30c7 :                  teor14
                                        set_ay  absEOa,0
                               >            load_flag 0
        30c7 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        30c9 : 48              >            pha         ;use stack to load status
        30ca : b95e02          >            lda absEOa,y    ;precharge accu
        30cd : 28              >            plp

        30ce : 5142                     eor (indEO),y
                                        tst_ay  absrlo,absflo,0
        30d0 : 08              >            php         ;save flags
        30d1 : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        30d4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        30d6 : 68              >            pla         ;load status
                               >            eor_flag 0
        30d7 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        30d9 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        30dc : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        30de : 88                       dey
        30df : 10e6                     bpl teor14
        30e1 : a003                     ldy #3
        30e3 :                  teor15
                                        set_ay  absEOa,$ff
                               >            load_flag $ff
        30e3 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        30e5 : 48              >            pha         ;use stack to load status
        30e6 : b95e02          >            lda absEOa,y    ;precharge accu
        30e9 : 28              >            plp

        30ea : 5142                     eor (indEO),y
                                        tst_ay  absrlo,absflo,$ff-fnz
        30ec : 08              >            php         ;save flags
        30ed : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        30f0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        30f2 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        30f3 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        30f5 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        30f8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        30fa : 88                       dey
        30fb : 10e6                     bpl teor15
                                        next_test
        30fd : ad0002          >            lda test_case   ;previous test
        3100 : c927            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        3102 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0028 =                 >test_num = test_num + 1
        3104 : a928            >            lda #test_num   ;*** next tests' number
        3106 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; OR
        3109 : a203                     ldx #3          ;immediate - self modifying code
        310b : b518             tora    lda zpOR,x
        310d : 8d0f02                   sta ex_orai+1   ;set ORA # operand
                                        set_ax  absORa,0
                               >            load_flag 0
        3110 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        3112 : 48              >            pha         ;use stack to load status
        3113 : bd5602          >            lda absORa,x    ;precharge accu
        3116 : 28              >            plp

        3117 : 200e02                   jsr ex_orai     ;execute ORA # in RAM
                                        tst_ax  absrlo,absflo,0
        311a : 08              >            php         ;save flags
        311b : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        311e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3120 : 68              >            pla         ;load status
                               >            eor_flag 0
        3121 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        3123 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        3126 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        3128 : ca                       dex
        3129 : 10e0                     bpl tora
        312b : a203                     ldx #3
        312d : b518             tora1   lda zpOR,x
        312f : 8d0f02                   sta ex_orai+1   ;set ORA # operand
                                        set_ax  absORa,$ff
                               >            load_flag $ff
        3132 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        3134 : 48              >            pha         ;use stack to load status
        3135 : bd5602          >            lda absORa,x    ;precharge accu
        3138 : 28              >            plp

        3139 : 200e02                   jsr ex_orai     ;execute ORA # in RAM
                                        tst_ax  absrlo,absflo,$ff-fnz
        313c : 08              >            php         ;save flags
        313d : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        3140 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3142 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        3143 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        3145 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        3148 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        314a : ca                       dex
        314b : 10e0                     bpl tora1

        314d : a203                     ldx #3      ;zp
        314f : b518             tora2   lda zpOR,x
        3151 : 850c                     sta zpt
                                        set_ax  absORa,0
                               >            load_flag 0
        3153 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        3155 : 48              >            pha         ;use stack to load status
        3156 : bd5602          >            lda absORa,x    ;precharge accu
        3159 : 28              >            plp

        315a : 050c                     ora zpt
                                        tst_ax  absrlo,absflo,0
        315c : 08              >            php         ;save flags
        315d : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        3160 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3162 : 68              >            pla         ;load status
                               >            eor_flag 0
        3163 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        3165 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        3168 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        316a : ca                       dex
        316b : 10e2                     bpl tora2
        316d : a203                     ldx #3
        316f : b518             tora3   lda zpOR,x
        3171 : 850c                     sta zpt
                                        set_ax  absORa,$ff
                               >            load_flag $ff
        3173 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        3175 : 48              >            pha         ;use stack to load status
        3176 : bd5602          >            lda absORa,x    ;precharge accu
        3179 : 28              >            plp

        317a : 050c                     ora zpt
                                        tst_ax  absrlo,absflo,$ff-fnz
        317c : 08              >            php         ;save flags
        317d : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        3180 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3182 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        3183 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        3185 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        3188 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        318a : ca                       dex
        318b : 10e2                     bpl tora3

        318d : a203                     ldx #3      ;abs
        318f : b518             tora4   lda zpOR,x
        3191 : 8d0302                   sta abst
                                        set_ax  absORa,0
                               >            load_flag 0
        3194 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        3196 : 48              >            pha         ;use stack to load status
        3197 : bd5602          >            lda absORa,x    ;precharge accu
        319a : 28              >            plp

        319b : 0d0302                   ora abst
                                        tst_ax  absrlo,absflo,0
        319e : 08              >            php         ;save flags
        319f : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        31a2 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        31a4 : 68              >            pla         ;load status
                               >            eor_flag 0
        31a5 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        31a7 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        31aa : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        31ac : ca                       dex
        31ad : 10e0                     bpl tora4
        31af : a203                     ldx #3
        31b1 : b518             tora5   lda zpOR,x
        31b3 : 8d0302                   sta abst
                                        set_ax  absORa,$ff
                               >            load_flag $ff
        31b6 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        31b8 : 48              >            pha         ;use stack to load status
        31b9 : bd5602          >            lda absORa,x    ;precharge accu
        31bc : 28              >            plp

        31bd : 0d0302                   ora abst
                                        tst_ax  absrlo,absflo,$ff-fnz
        31c0 : 08              >            php         ;save flags
        31c1 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        31c4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        31c6 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        31c7 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        31c9 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        31cc : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        31ce : ca                       dex
        31cf : 1002                     bpl tora6

        31d1 : a203                     ldx #3      ;zp,x
        31d3 :                  tora6
                                        set_ax  absORa,0
                               >            load_flag 0
        31d3 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        31d5 : 48              >            pha         ;use stack to load status
        31d6 : bd5602          >            lda absORa,x    ;precharge accu
        31d9 : 28              >            plp

        31da : 1518                     ora zpOR,x
                                        tst_ax  absrlo,absflo,0
        31dc : 08              >            php         ;save flags
        31dd : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        31e0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        31e2 : 68              >            pla         ;load status
                               >            eor_flag 0
        31e3 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        31e5 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        31e8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        31ea : ca                       dex
        31eb : 10e6                     bpl tora6
        31ed : a203                     ldx #3
        31ef :                  tora7
                                        set_ax  absORa,$ff
                               >            load_flag $ff
        31ef : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        31f1 : 48              >            pha         ;use stack to load status
        31f2 : bd5602          >            lda absORa,x    ;precharge accu
        31f5 : 28              >            plp

        31f6 : 1518                     ora zpOR,x
                                        tst_ax  absrlo,absflo,$ff-fnz
        31f8 : 08              >            php         ;save flags
        31f9 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        31fc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        31fe : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        31ff : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        3201 : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        3204 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        3206 : ca                       dex
        3207 : 10e6                     bpl tora7

        3209 : a203                     ldx #3      ;abs,x
        320b :                  tora8
                                        set_ax  absORa,0
                               >            load_flag 0
        320b : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        320d : 48              >            pha         ;use stack to load status
        320e : bd5602          >            lda absORa,x    ;precharge accu
        3211 : 28              >            plp

        3212 : 1d4a02                   ora absOR,x
                                        tst_ax  absrlo,absflo,0
        3215 : 08              >            php         ;save flags
        3216 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        3219 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        321b : 68              >            pla         ;load status
                               >            eor_flag 0
        321c : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        321e : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        3221 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        3223 : ca                       dex
        3224 : 10e5                     bpl tora8
        3226 : a203                     ldx #3
        3228 :                  tora9
                                        set_ax  absORa,$ff
                               >            load_flag $ff
        3228 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        322a : 48              >            pha         ;use stack to load status
        322b : bd5602          >            lda absORa,x    ;precharge accu
        322e : 28              >            plp

        322f : 1d4a02                   ora absOR,x
                                        tst_ax  absrlo,absflo,$ff-fnz
        3232 : 08              >            php         ;save flags
        3233 : dd6202          >            cmp absrlo,x    ;test result
                               >            trap_ne
        3236 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3238 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        3239 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        323b : dd6602          >            cmp absflo,x    ;test flags
                               >            trap_ne     ;
        323e : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        3240 : ca                       dex
        3241 : 10e5                     bpl tora9

        3243 : a003                     ldy #3      ;abs,y
        3245 :                  tora10
                                        set_ay  absORa,0
                               >            load_flag 0
        3245 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        3247 : 48              >            pha         ;use stack to load status
        3248 : b95602          >            lda absORa,y    ;precharge accu
        324b : 28              >            plp

        324c : 194a02                   ora absOR,y
                                        tst_ay  absrlo,absflo,0
        324f : 08              >            php         ;save flags
        3250 : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        3253 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3255 : 68              >            pla         ;load status
                               >            eor_flag 0
        3256 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        3258 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        325b : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        325d : 88                       dey
        325e : 10e5                     bpl tora10
        3260 : a003                     ldy #3
        3262 :                  tora11
                                        set_ay  absORa,$ff
                               >            load_flag $ff
        3262 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        3264 : 48              >            pha         ;use stack to load status
        3265 : b95602          >            lda absORa,y    ;precharge accu
        3268 : 28              >            plp

        3269 : 194a02                   ora absOR,y
                                        tst_ay  absrlo,absflo,$ff-fnz
        326c : 08              >            php         ;save flags
        326d : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        3270 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3272 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        3273 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        3275 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        3278 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        327a : 88                       dey
        327b : 10e5                     bpl tora11

        327d : a206                     ldx #6      ;(zp,x)
        327f : a003                     ldy #3
        3281 :                  tora12
                                        set_ay  absORa,0
                               >            load_flag 0
        3281 : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        3283 : 48              >            pha         ;use stack to load status
        3284 : b95602          >            lda absORa,y    ;precharge accu
        3287 : 28              >            plp

        3288 : 014a                     ora (indOR,x)
                                        tst_ay  absrlo,absflo,0
        328a : 08              >            php         ;save flags
        328b : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        328e : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        3290 : 68              >            pla         ;load status
                               >            eor_flag 0
        3291 : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        3293 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        3296 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        3298 : ca                       dex
        3299 : ca                       dex
        329a : 88                       dey
        329b : 10e4                     bpl tora12
        329d : a206                     ldx #6
        329f : a003                     ldy #3
        32a1 :                  tora13
                                        set_ay  absORa,$ff
                               >            load_flag $ff
        32a1 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        32a3 : 48              >            pha         ;use stack to load status
        32a4 : b95602          >            lda absORa,y    ;precharge accu
        32a7 : 28              >            plp

        32a8 : 014a                     ora (indOR,x)
                                        tst_ay  absrlo,absflo,$ff-fnz
        32aa : 08              >            php         ;save flags
        32ab : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        32ae : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        32b0 : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        32b1 : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        32b3 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        32b6 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        32b8 : ca                       dex
        32b9 : ca                       dex
        32ba : 88                       dey
        32bb : 10e4                     bpl tora13

        32bd : a003                     ldy #3      ;(zp),y
        32bf :                  tora14
                                        set_ay  absORa,0
                               >            load_flag 0
        32bf : a900            >            lda #0             ;allow test to change I-flag (no mask)
                               >
        32c1 : 48              >            pha         ;use stack to load status
        32c2 : b95602          >            lda absORa,y    ;precharge accu
        32c5 : 28              >            plp

        32c6 : 114a                     ora (indOR),y
                                        tst_ay  absrlo,absflo,0
        32c8 : 08              >            php         ;save flags
        32c9 : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        32cc : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        32ce : 68              >            pla         ;load status
                               >            eor_flag 0
        32cf : 4930            >            eor #0|fao         ;invert expected flags + always on bits
                               >
        32d1 : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        32d4 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        32d6 : 88                       dey
        32d7 : 10e6                     bpl tora14
        32d9 : a003                     ldy #3
        32db :                  tora15
                                        set_ay  absORa,$ff
                               >            load_flag $ff
        32db : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        32dd : 48              >            pha         ;use stack to load status
        32de : b95602          >            lda absORa,y    ;precharge accu
        32e1 : 28              >            plp

        32e2 : 114a                     ora (indOR),y
                                        tst_ay  absrlo,absflo,$ff-fnz
        32e4 : 08              >            php         ;save flags
        32e5 : d96202          >            cmp absrlo,y    ;test result
                               >            trap_ne     ;
        32e8 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        32ea : 68              >            pla         ;load status
                               >            eor_flag $ff-fnz
        32eb : 497d            >            eor #$ff-fnz|fao         ;invert expected flags + always on bits
                               >
        32ed : d96602          >            cmp absflo,y    ;test flags
                               >            trap_ne
        32f0 : d0fe            >        bne *           ;failed not equal (non zero)
                               >

        32f2 : 88                       dey
        32f3 : 10e6                     bpl tora15
                                    if I_flag = 3
        32f5 : 58                       cli
                                    endif                
                                        next_test
        32f6 : ad0002          >            lda test_case   ;previous test
        32f9 : c928            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        32fb : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        0029 =                 >test_num = test_num + 1
        32fd : a929            >            lda #test_num   ;*** next tests' number
        32ff : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; full binary add/subtract test
                                ; iterates through all combinations of operands and carry input
                                ; uses increments/decrements to predict result & result flags
        3302 : d8                       cld
        3303 : a20e                     ldx #ad2        ;for indexed test
        3305 : a0ff                     ldy #$ff        ;max range
        3307 : a900                     lda #0          ;start with adding zeroes & no carry
        3309 : 850c                     sta adfc        ;carry in - for diag
        330b : 850d                     sta ad1         ;operand 1 - accumulator
        330d : 850e                     sta ad2         ;operand 2 - memory or immediate
        330f : 8d0302                   sta ada2        ;non zp
        3312 : 850f                     sta adrl        ;expected result bits 0-7
        3314 : 8510                     sta adrh        ;expected result bit 8 (carry out)
        3316 : a9ff                     lda #$ff        ;complemented operand 2 for subtract
        3318 : 8512                     sta sb2
        331a : 8d0402                   sta sba2        ;non zp
        331d : a902                     lda #2          ;expected Z-flag
        331f : 8511                     sta adrf
        3321 : 18               tadd    clc             ;test with carry clear
        3322 : 209c35                   jsr chkadd
        3325 : e60c                     inc adfc        ;now with carry
        3327 : e60f                     inc adrl        ;result +1
        3329 : 08                       php             ;save N & Z from low result
        332a : 08                       php
        332b : 68                       pla             ;accu holds expected flags
        332c : 2982                     and #$82        ;mask N & Z
        332e : 28                       plp
        332f : d002                     bne tadd1
        3331 : e610                     inc adrh        ;result bit 8 - carry
        3333 : 0510             tadd1   ora adrh        ;merge C to expected flags
        3335 : 8511                     sta adrf        ;save expected flags except overflow
        3337 : 38                       sec             ;test with carry set
        3338 : 209c35                   jsr chkadd
        333b : c60c                     dec adfc        ;same for operand +1 but no carry
        333d : e60d                     inc ad1
        333f : d0e0                     bne tadd        ;iterate op1
        3341 : a900                     lda #0          ;preset result to op2 when op1 = 0
        3343 : 8510                     sta adrh
        3345 : ee0302                   inc ada2
        3348 : e60e                     inc ad2
        334a : 08                       php             ;save NZ as operand 2 becomes the new result
        334b : 68                       pla
        334c : 2982                     and #$82        ;mask N00000Z0
        334e : 8511                     sta adrf        ;no need to check carry as we are adding to 0
        3350 : c612                     dec sb2         ;complement subtract operand 2
        3352 : ce0402                   dec sba2
        3355 : a50e                     lda ad2         
        3357 : 850f                     sta adrl
        3359 : d0c6                     bne tadd        ;iterate op2
                                    if disable_decimal < 1
                                        next_test
        335b : ad0002          >            lda test_case   ;previous test
        335e : c929            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        3360 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        002a =                 >test_num = test_num + 1
        3362 : a92a            >            lda #test_num   ;*** next tests' number
        3364 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; decimal add/subtract test
                                ; *** WARNING - tests documented behavior only! ***
                                ;   only valid BCD operands are tested, N V Z flags are ignored
                                ; iterates through all valid combinations of operands and carry input
                                ; uses increments/decrements to predict result & carry flag
        3367 : f8                       sed 
        3368 : a20e                     ldx #ad2        ;for indexed test
        336a : a0ff                     ldy #$ff        ;max range
        336c : a999                     lda #$99        ;start with adding 99 to 99 with carry
        336e : 850d                     sta ad1         ;operand 1 - accumulator
        3370 : 850e                     sta ad2         ;operand 2 - memory or immediate
        3372 : 8d0302                   sta ada2        ;non zp
        3375 : 850f                     sta adrl        ;expected result bits 0-7
        3377 : a901                     lda #1          ;set carry in & out
        3379 : 850c                     sta adfc        ;carry in - for diag
        337b : 8510                     sta adrh        ;expected result bit 8 (carry out)
        337d : a900                     lda #0          ;complemented operand 2 for subtract
        337f : 8512                     sta sb2
        3381 : 8d0402                   sta sba2        ;non zp
        3384 : 38               tdad    sec             ;test with carry set
        3385 : 206934                   jsr chkdad
        3388 : c60c                     dec adfc        ;now with carry clear
        338a : a50f                     lda adrl        ;decimal adjust result
        338c : d008                     bne tdad1       ;skip clear carry & preset result 99 (9A-1)
        338e : c610                     dec adrh
        3390 : a999                     lda #$99
        3392 : 850f                     sta adrl
        3394 : d012                     bne tdad3
        3396 : 290f             tdad1   and #$f         ;lower nibble mask
        3398 : d00c                     bne tdad2       ;no decimal adjust needed
        339a : c60f                     dec adrl        ;decimal adjust (?0-6)
        339c : c60f                     dec adrl
        339e : c60f                     dec adrl
        33a0 : c60f                     dec adrl
        33a2 : c60f                     dec adrl
        33a4 : c60f                     dec adrl
        33a6 : c60f             tdad2   dec adrl        ;result -1
        33a8 : 18               tdad3   clc             ;test with carry clear
        33a9 : 206934                   jsr chkdad
        33ac : e60c                     inc adfc        ;same for operand -1 but with carry
        33ae : a50d                     lda ad1         ;decimal adjust operand 1
        33b0 : f015                     beq tdad5       ;iterate operand 2
        33b2 : 290f                     and #$f         ;lower nibble mask
        33b4 : d00c                     bne tdad4       ;skip decimal adjust
        33b6 : c60d                     dec ad1         ;decimal adjust (?0-6)
        33b8 : c60d                     dec ad1
        33ba : c60d                     dec ad1
        33bc : c60d                     dec ad1
        33be : c60d                     dec ad1
        33c0 : c60d                     dec ad1
        33c2 : c60d             tdad4   dec ad1         ;operand 1 -1
        33c4 : 4c8433                   jmp tdad        ;iterate op1

        33c7 : a999             tdad5   lda #$99        ;precharge op1 max
        33c9 : 850d                     sta ad1
        33cb : a50e                     lda ad2         ;decimal adjust operand 2
        33cd : f030                     beq tdad7       ;end of iteration
        33cf : 290f                     and #$f         ;lower nibble mask
        33d1 : d018                     bne tdad6       ;skip decimal adjust
        33d3 : c60e                     dec ad2         ;decimal adjust (?0-6)
        33d5 : c60e                     dec ad2
        33d7 : c60e                     dec ad2
        33d9 : c60e                     dec ad2
        33db : c60e                     dec ad2
        33dd : c60e                     dec ad2
        33df : e612                     inc sb2         ;complemented decimal adjust for subtract (?9+6)
        33e1 : e612                     inc sb2
        33e3 : e612                     inc sb2
        33e5 : e612                     inc sb2
        33e7 : e612                     inc sb2
        33e9 : e612                     inc sb2
        33eb : c60e             tdad6   dec ad2         ;operand 2 -1
        33ed : e612                     inc sb2         ;complemented operand for subtract
        33ef : a512                     lda sb2
        33f1 : 8d0402                   sta sba2        ;copy as non zp operand
        33f4 : a50e                     lda ad2
        33f6 : 8d0302                   sta ada2        ;copy as non zp operand
        33f9 : 850f                     sta adrl        ;new result since op1+carry=00+carry +op2=op2
        33fb : e610                     inc adrh        ;result carry
        33fd : d085                     bne tdad        ;iterate op2
        33ff :                  tdad7
                                        next_test
        33ff : ad0002          >            lda test_case   ;previous test
        3402 : c92a            >            cmp #test_num
                               >            trap_ne         ;test is out of sequence
        3404 : d0fe            >        bne *           ;failed not equal (non zero)
                               >
        002b =                 >test_num = test_num + 1
        3406 : a92b            >            lda #test_num   ;*** next tests' number
        3408 : 8d0002          >            sta test_case
                               >            ;check_ram       ;uncomment to find altered RAM after each test


                                ; decimal/binary switch test
                                ; tests CLD, SED, PLP, RTI to properly switch between decimal & binary opcode
                                ;   tables
        340b : 18                       clc
        340c : d8                       cld
        340d : 08                       php
        340e : a955                     lda #$55
        3410 : 6955                     adc #$55
        3412 : c9aa                     cmp #$aa
                                        trap_ne         ;expected binary result after cld
        3414 : d0fe            >        bne *           ;failed not equal (non zero)

        3416 : 18                       clc
        3417 : f8                       sed
        3418 : 08                       php
        3419 : a955                     lda #$55
        341b : 6955                     adc #$55
        341d : c910                     cmp #$10
                                        trap_ne         ;expected decimal result after sed
        341f : d0fe            >        bne *           ;failed not equal (non zero)

        3421 : d8                       cld
        3422 : 28                       plp
        3423 : a955                     lda #$55
        3425 : 6955                     adc #$55
        3427 : c910                     cmp #$10
                                        trap_ne         ;expected decimal result after plp D=1
        3429 : d0fe            >        bne *           ;failed not equal (non zero)

        342b : 28                       plp
        342c : a955                     lda #$55
        342e : 6955                     adc #$55
        3430 : c9aa                     cmp #$aa
                                        trap_ne         ;expected binary result after plp D=0
        3432 : d0fe            >        bne *           ;failed not equal (non zero)

        3434 : 18                       clc
        3435 : a934                     lda #hi bin_rti_ret ;emulated interrupt for rti
        3437 : 48                       pha
        3438 : a94f                     lda #lo bin_rti_ret
        343a : 48                       pha
        343b : 08                       php
        343c : f8                       sed
        343d : a934                     lda #hi dec_rti_ret ;emulated interrupt for rti
        343f : 48                       pha
        3440 : a946                     lda #lo dec_rti_ret
        3442 : 48                       pha
        3443 : 08                       php
        3444 : d8                       cld
        3445 : 40                       rti
        3446 :                  dec_rti_ret
        3446 : a955                     lda #$55
        3448 : 6955                     adc #$55
        344a : c910                     cmp #$10
                                        trap_ne         ;expected decimal result after rti D=1
        344c : d0fe            >        bne *           ;failed not equal (non zero)

        344e : 40                       rti
        344f :                  bin_rti_ret        
        344f : a955                     lda #$55
        3451 : 6955                     adc #$55
        3453 : c9aa                     cmp #$aa
                                        trap_ne         ;expected binary result after rti D=0
        3455 : d0fe            >        bne *           ;failed not equal (non zero)

                                    endif

        3457 : ad0002                   lda test_case
        345a : c92b                     cmp #test_num
                                        trap_ne         ;previous test is out of sequence
        345c : d0fe            >        bne *           ;failed not equal (non zero)

        345e : a9f0                     lda #$f0        ;mark opcode testing complete
        3460 : 8d0002                   sta test_case

                                ; final RAM integrity test
                                ;   verifies that none of the previous tests has altered RAM outside of the
                                ;   designated write areas.
                                        check_ram
                               >            ;RAM check disabled - RAM size not set

                                ; *** DEBUG INFO ***
                                ; to debug checksum errors uncomment check_ram in the next_test macro to
                                ; narrow down the responsible opcode.
                                ; may give false errors when monitor, OS or other background activity is
                                ; allowed during previous tests.


                                ; S U C C E S S ************************************************
                                ; -------------       
                                        success         ;if you get here everything went well
        3463 : 4c6334          >        jmp *           ;test passed, no errors

                                ; -------------       
                                ; S U C C E S S ************************************************
        3466 : 4c0004                   jmp start       ;run again      

                                    if disable_decimal < 1
                                ; core subroutine of the decimal add/subtract test
                                ; *** WARNING - tests documented behavior only! ***
                                ;   only valid BCD operands are tested, N V Z flags are ignored
                                ; iterates through all valid combinations of operands and carry input
                                ; uses increments/decrements to predict result & carry flag
        3469 :                  chkdad
                                ; decimal ADC / SBC zp
        3469 : 08                       php             ;save carry for subtract
        346a : a50d                     lda ad1
        346c : 650e                     adc ad2         ;perform add
        346e : 08                       php          
        346f : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3471 : d0fe            >        bne *           ;failed not equal (non zero)

        3473 : 68                       pla             ;check flags
        3474 : 2901                     and #1          ;mask carry
        3476 : c510                     cmp adrh
                                        trap_ne         ;bad carry
        3478 : d0fe            >        bne *           ;failed not equal (non zero)

        347a : 28                       plp
        347b : 08                       php             ;save carry for next add
        347c : a50d                     lda ad1
        347e : e512                     sbc sb2         ;perform subtract
        3480 : 08                       php          
        3481 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3483 : d0fe            >        bne *           ;failed not equal (non zero)

        3485 : 68                       pla             ;check flags
        3486 : 2901                     and #1          ;mask carry
        3488 : c510                     cmp adrh
                                        trap_ne         ;bad flags
        348a : d0fe            >        bne *           ;failed not equal (non zero)

        348c : 28                       plp
                                ; decimal ADC / SBC abs
        348d : 08                       php             ;save carry for subtract
        348e : a50d                     lda ad1
        3490 : 6d0302                   adc ada2        ;perform add
        3493 : 08                       php          
        3494 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3496 : d0fe            >        bne *           ;failed not equal (non zero)

        3498 : 68                       pla             ;check flags
        3499 : 2901                     and #1          ;mask carry
        349b : c510                     cmp adrh
                                        trap_ne         ;bad carry
        349d : d0fe            >        bne *           ;failed not equal (non zero)

        349f : 28                       plp
        34a0 : 08                       php             ;save carry for next add
        34a1 : a50d                     lda ad1
        34a3 : ed0402                   sbc sba2        ;perform subtract
        34a6 : 08                       php          
        34a7 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        34a9 : d0fe            >        bne *           ;failed not equal (non zero)

        34ab : 68                       pla             ;check flags
        34ac : 2901                     and #1          ;mask carry
        34ae : c510                     cmp adrh
                                        trap_ne         ;bad carry
        34b0 : d0fe            >        bne *           ;failed not equal (non zero)

        34b2 : 28                       plp
                                ; decimal ADC / SBC #
        34b3 : 08                       php             ;save carry for subtract
        34b4 : a50e                     lda ad2
        34b6 : 8d1202                   sta ex_adci+1   ;set ADC # operand
        34b9 : a50d                     lda ad1
        34bb : 201102                   jsr ex_adci     ;execute ADC # in RAM
        34be : 08                       php          
        34bf : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        34c1 : d0fe            >        bne *           ;failed not equal (non zero)

        34c3 : 68                       pla             ;check flags
        34c4 : 2901                     and #1          ;mask carry
        34c6 : c510                     cmp adrh
                                        trap_ne         ;bad carry
        34c8 : d0fe            >        bne *           ;failed not equal (non zero)

        34ca : 28                       plp
        34cb : 08                       php             ;save carry for next add
        34cc : a512                     lda sb2
        34ce : 8d1502                   sta ex_sbci+1   ;set SBC # operand
        34d1 : a50d                     lda ad1
        34d3 : 201402                   jsr ex_sbci     ;execute SBC # in RAM
        34d6 : 08                       php          
        34d7 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        34d9 : d0fe            >        bne *           ;failed not equal (non zero)

        34db : 68                       pla             ;check flags
        34dc : 2901                     and #1          ;mask carry
        34de : c510                     cmp adrh
                                        trap_ne         ;bad carry
        34e0 : d0fe            >        bne *           ;failed not equal (non zero)

        34e2 : 28                       plp
                                ; decimal ADC / SBC zp,x
        34e3 : 08                       php             ;save carry for subtract
        34e4 : a50d                     lda ad1
        34e6 : 7500                     adc 0,x         ;perform add
        34e8 : 08                       php          
        34e9 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        34eb : d0fe            >        bne *           ;failed not equal (non zero)

        34ed : 68                       pla             ;check flags
        34ee : 2901                     and #1          ;mask carry
        34f0 : c510                     cmp adrh
                                        trap_ne         ;bad carry
        34f2 : d0fe            >        bne *           ;failed not equal (non zero)

        34f4 : 28                       plp
        34f5 : 08                       php             ;save carry for next add
        34f6 : a50d                     lda ad1
        34f8 : f504                     sbc sb2-ad2,x   ;perform subtract
        34fa : 08                       php          
        34fb : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        34fd : d0fe            >        bne *           ;failed not equal (non zero)

        34ff : 68                       pla             ;check flags
        3500 : 2901                     and #1          ;mask carry
        3502 : c510                     cmp adrh
                                        trap_ne         ;bad carry
        3504 : d0fe            >        bne *           ;failed not equal (non zero)

        3506 : 28                       plp
                                ; decimal ADC / SBC abs,x
        3507 : 08                       php             ;save carry for subtract
        3508 : a50d                     lda ad1
        350a : 7df501                   adc ada2-ad2,x  ;perform add
        350d : 08                       php          
        350e : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3510 : d0fe            >        bne *           ;failed not equal (non zero)

        3512 : 68                       pla             ;check flags
        3513 : 2901                     and #1          ;mask carry
        3515 : c510                     cmp adrh
                                        trap_ne         ;bad carry
        3517 : d0fe            >        bne *           ;failed not equal (non zero)

        3519 : 28                       plp
        351a : 08                       php             ;save carry for next add
        351b : a50d                     lda ad1
        351d : fdf601                   sbc sba2-ad2,x  ;perform subtract
        3520 : 08                       php          
        3521 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3523 : d0fe            >        bne *           ;failed not equal (non zero)

        3525 : 68                       pla             ;check flags
        3526 : 2901                     and #1          ;mask carry
        3528 : c510                     cmp adrh
                                        trap_ne         ;bad carry
        352a : d0fe            >        bne *           ;failed not equal (non zero)

        352c : 28                       plp
                                ; decimal ADC / SBC abs,y
        352d : 08                       php             ;save carry for subtract
        352e : a50d                     lda ad1
        3530 : 790401                   adc ada2-$ff,y  ;perform add
        3533 : 08                       php          
        3534 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3536 : d0fe            >        bne *           ;failed not equal (non zero)

        3538 : 68                       pla             ;check flags
        3539 : 2901                     and #1          ;mask carry
        353b : c510                     cmp adrh
                                        trap_ne         ;bad carry
        353d : d0fe            >        bne *           ;failed not equal (non zero)

        353f : 28                       plp
        3540 : 08                       php             ;save carry for next add
        3541 : a50d                     lda ad1
        3543 : f90501                   sbc sba2-$ff,y  ;perform subtract
        3546 : 08                       php          
        3547 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3549 : d0fe            >        bne *           ;failed not equal (non zero)

        354b : 68                       pla             ;check flags
        354c : 2901                     and #1          ;mask carry
        354e : c510                     cmp adrh
                                        trap_ne         ;bad carry
        3550 : d0fe            >        bne *           ;failed not equal (non zero)

        3552 : 28                       plp
                                ; decimal ADC / SBC (zp,x)
        3553 : 08                       php             ;save carry for subtract
        3554 : a50d                     lda ad1
        3556 : 6144                     adc (lo adi2-ad2,x) ;perform add
        3558 : 08                       php          
        3559 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        355b : d0fe            >        bne *           ;failed not equal (non zero)

        355d : 68                       pla             ;check flags
        355e : 2901                     and #1          ;mask carry
        3560 : c510                     cmp adrh
                                        trap_ne         ;bad carry
        3562 : d0fe            >        bne *           ;failed not equal (non zero)

        3564 : 28                       plp
        3565 : 08                       php             ;save carry for next add
        3566 : a50d                     lda ad1
        3568 : e146                     sbc (lo sbi2-ad2,x) ;perform subtract
        356a : 08                       php          
        356b : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        356d : d0fe            >        bne *           ;failed not equal (non zero)

        356f : 68                       pla             ;check flags
        3570 : 2901                     and #1          ;mask carry
        3572 : c510                     cmp adrh
                                        trap_ne         ;bad carry
        3574 : d0fe            >        bne *           ;failed not equal (non zero)

        3576 : 28                       plp
                                ; decimal ADC / SBC (abs),y
        3577 : 08                       php             ;save carry for subtract
        3578 : a50d                     lda ad1
        357a : 7156                     adc (adiy2),y   ;perform add
        357c : 08                       php          
        357d : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        357f : d0fe            >        bne *           ;failed not equal (non zero)

        3581 : 68                       pla             ;check flags
        3582 : 2901                     and #1          ;mask carry
        3584 : c510                     cmp adrh
                                        trap_ne         ;bad carry
        3586 : d0fe            >        bne *           ;failed not equal (non zero)

        3588 : 28                       plp
        3589 : 08                       php             ;save carry for next add
        358a : a50d                     lda ad1
        358c : f158                     sbc (sbiy2),y   ;perform subtract
        358e : 08                       php          
        358f : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3591 : d0fe            >        bne *           ;failed not equal (non zero)

        3593 : 68                       pla             ;check flags
        3594 : 2901                     and #1          ;mask carry
        3596 : c510                     cmp adrh
                                        trap_ne         ;bad carry
        3598 : d0fe            >        bne *           ;failed not equal (non zero)

        359a : 28                       plp
        359b : 60                       rts
                                    endif

                                ; core subroutine of the full binary add/subtract test
                                ; iterates through all combinations of operands and carry input
                                ; uses increments/decrements to predict result & result flags
        359c : a511             chkadd  lda adrf        ;add V-flag if overflow
        359e : 2983                     and #$83        ;keep N-----ZC / clear V
        35a0 : 48                       pha
        35a1 : a50d                     lda ad1         ;test sign unequal between operands
        35a3 : 450e                     eor ad2
        35a5 : 300a                     bmi ckad1       ;no overflow possible - operands have different sign
        35a7 : a50d                     lda ad1         ;test sign equal between operands and result
        35a9 : 450f                     eor adrl
        35ab : 1004                     bpl ckad1       ;no overflow occured - operand and result have same sign
        35ad : 68                       pla
        35ae : 0940                     ora #$40        ;set V
        35b0 : 48                       pha
        35b1 : 68               ckad1   pla
        35b2 : 8511                     sta adrf        ;save expected flags
                                ; binary ADC / SBC zp
        35b4 : 08                       php             ;save carry for subtract
        35b5 : a50d                     lda ad1
        35b7 : 650e                     adc ad2         ;perform add
        35b9 : 08                       php          
        35ba : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        35bc : d0fe            >        bne *           ;failed not equal (non zero)

        35be : 68                       pla             ;check flags
        35bf : 29c3                     and #$c3        ;mask NV----ZC
        35c1 : c511                     cmp adrf
                                        trap_ne         ;bad flags
        35c3 : d0fe            >        bne *           ;failed not equal (non zero)

        35c5 : 28                       plp
        35c6 : 08                       php             ;save carry for next add
        35c7 : a50d                     lda ad1
        35c9 : e512                     sbc sb2         ;perform subtract
        35cb : 08                       php          
        35cc : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        35ce : d0fe            >        bne *           ;failed not equal (non zero)

        35d0 : 68                       pla             ;check flags
        35d1 : 29c3                     and #$c3        ;mask NV----ZC
        35d3 : c511                     cmp adrf
                                        trap_ne         ;bad flags
        35d5 : d0fe            >        bne *           ;failed not equal (non zero)

        35d7 : 28                       plp
                                ; binary ADC / SBC abs
        35d8 : 08                       php             ;save carry for subtract
        35d9 : a50d                     lda ad1
        35db : 6d0302                   adc ada2        ;perform add
        35de : 08                       php          
        35df : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        35e1 : d0fe            >        bne *           ;failed not equal (non zero)

        35e3 : 68                       pla             ;check flags
        35e4 : 29c3                     and #$c3        ;mask NV----ZC
        35e6 : c511                     cmp adrf
                                        trap_ne         ;bad flags
        35e8 : d0fe            >        bne *           ;failed not equal (non zero)

        35ea : 28                       plp
        35eb : 08                       php             ;save carry for next add
        35ec : a50d                     lda ad1
        35ee : ed0402                   sbc sba2        ;perform subtract
        35f1 : 08                       php          
        35f2 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        35f4 : d0fe            >        bne *           ;failed not equal (non zero)

        35f6 : 68                       pla             ;check flags
        35f7 : 29c3                     and #$c3        ;mask NV----ZC
        35f9 : c511                     cmp adrf
                                        trap_ne         ;bad flags
        35fb : d0fe            >        bne *           ;failed not equal (non zero)

        35fd : 28                       plp
                                ; binary ADC / SBC #
        35fe : 08                       php             ;save carry for subtract
        35ff : a50e                     lda ad2
        3601 : 8d1202                   sta ex_adci+1   ;set ADC # operand
        3604 : a50d                     lda ad1
        3606 : 201102                   jsr ex_adci     ;execute ADC # in RAM
        3609 : 08                       php          
        360a : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        360c : d0fe            >        bne *           ;failed not equal (non zero)

        360e : 68                       pla             ;check flags
        360f : 29c3                     and #$c3        ;mask NV----ZC
        3611 : c511                     cmp adrf
                                        trap_ne         ;bad flags
        3613 : d0fe            >        bne *           ;failed not equal (non zero)

        3615 : 28                       plp
        3616 : 08                       php             ;save carry for next add
        3617 : a512                     lda sb2
        3619 : 8d1502                   sta ex_sbci+1   ;set SBC # operand
        361c : a50d                     lda ad1
        361e : 201402                   jsr ex_sbci     ;execute SBC # in RAM
        3621 : 08                       php          
        3622 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3624 : d0fe            >        bne *           ;failed not equal (non zero)

        3626 : 68                       pla             ;check flags
        3627 : 29c3                     and #$c3        ;mask NV----ZC
        3629 : c511                     cmp adrf
                                        trap_ne         ;bad flags
        362b : d0fe            >        bne *           ;failed not equal (non zero)

        362d : 28                       plp
                                ; binary ADC / SBC zp,x
        362e : 08                       php             ;save carry for subtract
        362f : a50d                     lda ad1
        3631 : 7500                     adc 0,x         ;perform add
        3633 : 08                       php          
        3634 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3636 : d0fe            >        bne *           ;failed not equal (non zero)

        3638 : 68                       pla             ;check flags
        3639 : 29c3                     and #$c3        ;mask NV----ZC
        363b : c511                     cmp adrf
                                        trap_ne         ;bad flags
        363d : d0fe            >        bne *           ;failed not equal (non zero)

        363f : 28                       plp
        3640 : 08                       php             ;save carry for next add
        3641 : a50d                     lda ad1
        3643 : f504                     sbc sb2-ad2,x   ;perform subtract
        3645 : 08                       php          
        3646 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3648 : d0fe            >        bne *           ;failed not equal (non zero)

        364a : 68                       pla             ;check flags
        364b : 29c3                     and #$c3        ;mask NV----ZC
        364d : c511                     cmp adrf
                                        trap_ne         ;bad flags
        364f : d0fe            >        bne *           ;failed not equal (non zero)

        3651 : 28                       plp
                                ; binary ADC / SBC abs,x
        3652 : 08                       php             ;save carry for subtract
        3653 : a50d                     lda ad1
        3655 : 7df501                   adc ada2-ad2,x  ;perform add
        3658 : 08                       php          
        3659 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        365b : d0fe            >        bne *           ;failed not equal (non zero)

        365d : 68                       pla             ;check flags
        365e : 29c3                     and #$c3        ;mask NV----ZC
        3660 : c511                     cmp adrf
                                        trap_ne         ;bad flags
        3662 : d0fe            >        bne *           ;failed not equal (non zero)

        3664 : 28                       plp
        3665 : 08                       php             ;save carry for next add
        3666 : a50d                     lda ad1
        3668 : fdf601                   sbc sba2-ad2,x  ;perform subtract
        366b : 08                       php          
        366c : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        366e : d0fe            >        bne *           ;failed not equal (non zero)

        3670 : 68                       pla             ;check flags
        3671 : 29c3                     and #$c3        ;mask NV----ZC
        3673 : c511                     cmp adrf
                                        trap_ne         ;bad flags
        3675 : d0fe            >        bne *           ;failed not equal (non zero)

        3677 : 28                       plp
                                ; binary ADC / SBC abs,y
        3678 : 08                       php             ;save carry for subtract
        3679 : a50d                     lda ad1
        367b : 790401                   adc ada2-$ff,y  ;perform add
        367e : 08                       php          
        367f : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3681 : d0fe            >        bne *           ;failed not equal (non zero)

        3683 : 68                       pla             ;check flags
        3684 : 29c3                     and #$c3        ;mask NV----ZC
        3686 : c511                     cmp adrf
                                        trap_ne         ;bad flags
        3688 : d0fe            >        bne *           ;failed not equal (non zero)

        368a : 28                       plp
        368b : 08                       php             ;save carry for next add
        368c : a50d                     lda ad1
        368e : f90501                   sbc sba2-$ff,y  ;perform subtract
        3691 : 08                       php          
        3692 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        3694 : d0fe            >        bne *           ;failed not equal (non zero)

        3696 : 68                       pla             ;check flags
        3697 : 29c3                     and #$c3        ;mask NV----ZC
        3699 : c511                     cmp adrf
                                        trap_ne         ;bad flags
        369b : d0fe            >        bne *           ;failed not equal (non zero)

        369d : 28                       plp
                                ; binary ADC / SBC (zp,x)
        369e : 08                       php             ;save carry for subtract
        369f : a50d                     lda ad1
        36a1 : 6144                     adc (lo adi2-ad2,x) ;perform add
        36a3 : 08                       php          
        36a4 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        36a6 : d0fe            >        bne *           ;failed not equal (non zero)

        36a8 : 68                       pla             ;check flags
        36a9 : 29c3                     and #$c3        ;mask NV----ZC
        36ab : c511                     cmp adrf
                                        trap_ne         ;bad flags
        36ad : d0fe            >        bne *           ;failed not equal (non zero)

        36af : 28                       plp
        36b0 : 08                       php             ;save carry for next add
        36b1 : a50d                     lda ad1
        36b3 : e146                     sbc (lo sbi2-ad2,x) ;perform subtract
        36b5 : 08                       php          
        36b6 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        36b8 : d0fe            >        bne *           ;failed not equal (non zero)

        36ba : 68                       pla             ;check flags
        36bb : 29c3                     and #$c3        ;mask NV----ZC
        36bd : c511                     cmp adrf
                                        trap_ne         ;bad flags
        36bf : d0fe            >        bne *           ;failed not equal (non zero)

        36c1 : 28                       plp
                                ; binary ADC / SBC (abs),y
        36c2 : 08                       php             ;save carry for subtract
        36c3 : a50d                     lda ad1
        36c5 : 7156                     adc (adiy2),y   ;perform add
        36c7 : 08                       php          
        36c8 : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        36ca : d0fe            >        bne *           ;failed not equal (non zero)

        36cc : 68                       pla             ;check flags
        36cd : 29c3                     and #$c3        ;mask NV----ZC
        36cf : c511                     cmp adrf
                                        trap_ne         ;bad flags
        36d1 : d0fe            >        bne *           ;failed not equal (non zero)

        36d3 : 28                       plp
        36d4 : 08                       php             ;save carry for next add
        36d5 : a50d                     lda ad1
        36d7 : f158                     sbc (sbiy2),y   ;perform subtract
        36d9 : 08                       php          
        36da : c50f                     cmp adrl        ;check result
                                        trap_ne         ;bad result
        36dc : d0fe            >        bne *           ;failed not equal (non zero)

        36de : 68                       pla             ;check flags
        36df : 29c3                     and #$c3        ;mask NV----ZC
        36e1 : c511                     cmp adrf
                                        trap_ne         ;bad flags
        36e3 : d0fe            >        bne *           ;failed not equal (non zero)

        36e5 : 28                       plp
        36e6 : 60                       rts

                                ; target for the jump absolute test
        36e7 : 88                       dey
        36e8 : 88                       dey
        36e9 :                  test_far
        36e9 : 08                       php             ;either SP or Y count will fail, if we do not hit
        36ea : 88                       dey
        36eb : 88                       dey
        36ec : 88                       dey
        36ed : 28                       plp
                                        trap_cs         ;flags loaded?
        36ee : b0fe            >        bcs *           ;failed carry set

                                        trap_vs
        36f0 : 70fe            >        bvs *           ;failed overflow set

                                        trap_mi
        36f2 : 30fe            >        bmi *           ;failed minus (bit 7 set)

                                        trap_eq 
        36f4 : f0fe            >        beq *           ;failed equal (zero)

        36f6 : c946                     cmp #'F'        ;registers loaded?
                                        trap_ne
        36f8 : d0fe            >        bne *           ;failed not equal (non zero)

        36fa : e041                     cpx #'A'
                                        trap_ne        
        36fc : d0fe            >        bne *           ;failed not equal (non zero)

        36fe : c04f                     cpy #('R'-3)
                                        trap_ne
        3700 : d0fe            >        bne *           ;failed not equal (non zero)

        3702 : 48                       pha             ;save a,x
        3703 : 8a                       txa
        3704 : 48                       pha
        3705 : ba                       tsx
        3706 : e0fd                     cpx #$fd        ;check SP
                                        trap_ne
        3708 : d0fe            >        bne *           ;failed not equal (non zero)

        370a : 68                       pla             ;restore x
        370b : aa                       tax
                                        set_stat $ff
                               >            load_flag $ff
        370c : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        370e : 48              >            pha         ;use stack to load status
        370f : 28              >            plp

        3710 : 68                       pla             ;restore a
        3711 : e8                       inx             ;return registers with modifications
        3712 : 49aa                     eor #$aa        ;N=1, V=1, Z=0, C=1
        3714 : 4c2f09                   jmp far_ret

                                ; target for the jump indirect test
        3717 : 00                       align
        3718 : 2137             ptr_tst_ind dw test_ind
        371a : 8409             ptr_ind_ret dw ind_ret
                                        trap            ;runover protection
        371c : 4c1c37          >        jmp *           ;failed anyway

        371f : 88                       dey
        3720 : 88                       dey
        3721 :                  test_ind
        3721 : 08                       php             ;either SP or Y count will fail, if we do not hit
        3722 : 88                       dey
        3723 : 88                       dey
        3724 : 88                       dey
        3725 : 28                       plp
                                        trap_cs         ;flags loaded?
        3726 : b0fe            >        bcs *           ;failed carry set

                                        trap_vs
        3728 : 70fe            >        bvs *           ;failed overflow set

                                        trap_mi
        372a : 30fe            >        bmi *           ;failed minus (bit 7 set)

                                        trap_eq 
        372c : f0fe            >        beq *           ;failed equal (zero)

        372e : c949                     cmp #'I'        ;registers loaded?
                                        trap_ne
        3730 : d0fe            >        bne *           ;failed not equal (non zero)

        3732 : e04e                     cpx #'N'
                                        trap_ne        
        3734 : d0fe            >        bne *           ;failed not equal (non zero)

        3736 : c041                     cpy #('D'-3)
                                        trap_ne
        3738 : d0fe            >        bne *           ;failed not equal (non zero)

        373a : 48                       pha             ;save a,x
        373b : 8a                       txa
        373c : 48                       pha
        373d : ba                       tsx
        373e : e0fd                     cpx #$fd        ;check SP
                                        trap_ne
        3740 : d0fe            >        bne *           ;failed not equal (non zero)

        3742 : 68                       pla             ;restore x
        3743 : aa                       tax
                                        set_stat $ff
                               >            load_flag $ff
        3744 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        3746 : 48              >            pha         ;use stack to load status
        3747 : 28              >            plp

        3748 : 68                       pla             ;restore a
        3749 : e8                       inx             ;return registers with modifications
        374a : 49aa                     eor #$aa        ;N=1, V=1, Z=0, C=1
        374c : 6c1a37                   jmp (ptr_ind_ret)
                                        trap            ;runover protection
        374f : 4c4f37          >        jmp *           ;failed anyway


                                ; target for the jump subroutine test
        3752 : 88                       dey
        3753 : 88                       dey
        3754 :                  test_jsr
        3754 : 08                       php             ;either SP or Y count will fail, if we do not hit
        3755 : 88                       dey
        3756 : 88                       dey
        3757 : 88                       dey
        3758 : 28                       plp
                                        trap_cs         ;flags loaded?
        3759 : b0fe            >        bcs *           ;failed carry set

                                        trap_vs
        375b : 70fe            >        bvs *           ;failed overflow set

                                        trap_mi
        375d : 30fe            >        bmi *           ;failed minus (bit 7 set)

                                        trap_eq 
        375f : f0fe            >        beq *           ;failed equal (zero)

        3761 : c94a                     cmp #'J'        ;registers loaded?
                                        trap_ne
        3763 : d0fe            >        bne *           ;failed not equal (non zero)

        3765 : e053                     cpx #'S'
                                        trap_ne        
        3767 : d0fe            >        bne *           ;failed not equal (non zero)

        3769 : c04f                     cpy #('R'-3)
                                        trap_ne
        376b : d0fe            >        bne *           ;failed not equal (non zero)

        376d : 48                       pha             ;save a,x
        376e : 8a                       txa
        376f : 48                       pha       
        3770 : ba                       tsx             ;sp -4? (return addr,a,x)
        3771 : e0fb                     cpx #$fb
                                        trap_ne
        3773 : d0fe            >        bne *           ;failed not equal (non zero)

        3775 : adff01                   lda $1ff        ;propper return on stack
        3778 : c909                     cmp #hi(jsr_ret)
                                        trap_ne
        377a : d0fe            >        bne *           ;failed not equal (non zero)

        377c : adfe01                   lda $1fe
        377f : c9ba                     cmp #lo(jsr_ret)
                                        trap_ne
        3781 : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        3783 : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        3785 : 48              >            pha         ;use stack to load status
        3786 : 28              >            plp

        3787 : 68                       pla             ;pull x,a
        3788 : aa                       tax
        3789 : 68                       pla
        378a : e8                       inx             ;return registers with modifications
        378b : 49aa                     eor #$aa        ;N=1, V=1, Z=0, C=1
        378d : 60                       rts
                                        trap            ;runover protection
        378e : 4c8e37          >        jmp *           ;failed anyway


                                ;trap in case of unexpected IRQ, NMI, BRK, RESET - BRK test target
        3791 :                  nmi_trap
                                        trap            ;check stack for conditions at NMI
        3791 : 4c9137          >        jmp *           ;failed anyway

        3794 :                  res_trap
                                        trap            ;unexpected RESET
        3794 : 4c9437          >        jmp *           ;failed anyway


        3797 : 88                       dey
        3798 : 88                       dey
        3799 :                  irq_trap                ;BRK test or unextpected BRK or IRQ
        3799 : 08                       php             ;either SP or Y count will fail, if we do not hit
        379a : 88                       dey
        379b : 88                       dey
        379c : 88                       dey
                                        ;next 4 traps could be caused by unexpected BRK or IRQ
                                        ;check stack for BREAK and originating location
                                        ;possible jump/branch into weeds (uninitialized space)
        379d : c942                     cmp #'B'        ;registers loaded?
                                        trap_ne
        379f : d0fe            >        bne *           ;failed not equal (non zero)

        37a1 : e052                     cpx #'R'
                                        trap_ne        
        37a3 : d0fe            >        bne *           ;failed not equal (non zero)

        37a5 : c048                     cpy #('K'-3)
                                        trap_ne
        37a7 : d0fe            >        bne *           ;failed not equal (non zero)

        37a9 : 850a                     sta irq_a       ;save registers during break test
        37ab : 860b                     stx irq_x
        37ad : ba                       tsx             ;test break on stack
        37ae : bd0201                   lda $102,x
                                        cmp_flag 0      ;break test should have B=1
        37b1 : c930            >            cmp #(0      |fao)&m8    ;expected flags + always on bits

                                        trap_ne         ; - no break flag on stack
        37b3 : d0fe            >        bne *           ;failed not equal (non zero)

        37b5 : 68                       pla
        37b6 : c934                     cmp #fai        ;should have added interrupt disable
                                        trap_ne
        37b8 : d0fe            >        bne *           ;failed not equal (non zero)

        37ba : ba                       tsx
        37bb : e0fc                     cpx #$fc        ;sp -3? (return addr, flags)
                                        trap_ne
        37bd : d0fe            >        bne *           ;failed not equal (non zero)

        37bf : adff01                   lda $1ff        ;propper return on stack
        37c2 : c909                     cmp #hi(brk_ret)
                                        trap_ne
        37c4 : d0fe            >        bne *           ;failed not equal (non zero)

        37c6 : adfe01                   lda $1fe
        37c9 : c9f1                     cmp #lo(brk_ret)
                                        trap_ne
        37cb : d0fe            >        bne *           ;failed not equal (non zero)

                                        set_stat $ff
                               >            load_flag $ff
        37cd : a9ff            >            lda #$ff             ;allow test to change I-flag (no mask)
                               >
        37cf : 48              >            pha         ;use stack to load status
        37d0 : 28              >            plp

        37d1 : a60b                     ldx irq_x
        37d3 : e8                       inx             ;return registers with modifications
        37d4 : a50a                     lda irq_a
        37d6 : 49aa                     eor #$aa        ;N=1, V=1, Z=0, C=1 but original flags should be restored
        37d8 : 40                       rti
                                        trap            ;runover protection
        37d9 : 4cd937          >        jmp *           ;failed anyway


                                    if report = 1
                                        include "report.i65"
                                    endif

                                ;copy of data to initialize BSS segment
                                    if load_data_direct != 1
        37dc :                  zp_init
        37dc : c3824100         zp1_    db  $c3,$82,$41,0   ;test patterns for LDx BIT ROL ROR ASL LSR
        37e0 : 7f               zp7f_   db  $7f             ;test pattern for compare
                                ;logical zeropage operands
        37e1 : 001f7180         zpOR_   db  0,$1f,$71,$80   ;test pattern for OR
        37e5 : 0fff7f80         zpAN_   db  $0f,$ff,$7f,$80 ;test pattern for AND
        37e9 : ff0f8f8f         zpEO_   db  $ff,$0f,$8f,$8f ;test pattern for EOR
                                ;indirect addressing pointers
        37ed : 1702             ind1_   dw  abs1            ;indirect pointer to pattern in absolute memory
        37ef : 1802                     dw  abs1+1
        37f1 : 1902                     dw  abs1+2
        37f3 : 1a02                     dw  abs1+3
        37f5 : 1b02                     dw  abs7f
        37f7 : 1f01             inw1_   dw  abs1-$f8        ;indirect pointer for wrap-test pattern
        37f9 : 0302             indt_   dw  abst            ;indirect pointer to store area in absolute memory
        37fb : 0402                     dw  abst+1
        37fd : 0502                     dw  abst+2
        37ff : 0602                     dw  abst+3
        3801 : 0b01             inwt_   dw  abst-$f8        ;indirect pointer for wrap-test store
        3803 : 4e02             indAN_  dw  absAN           ;indirect pointer to AND pattern in absolute memory
        3805 : 4f02                     dw  absAN+1
        3807 : 5002                     dw  absAN+2
        3809 : 5102                     dw  absAN+3
        380b : 5202             indEO_  dw  absEO           ;indirect pointer to EOR pattern in absolute memory
        380d : 5302                     dw  absEO+1
        380f : 5402                     dw  absEO+2
        3811 : 5502                     dw  absEO+3
        3813 : 4a02             indOR_  dw  absOR           ;indirect pointer to OR pattern in absolute memory
        3815 : 4b02                     dw  absOR+1
        3817 : 4c02                     dw  absOR+2
        3819 : 4d02                     dw  absOR+3
                                ;add/subtract indirect pointers
        381b : 0302             adi2_   dw  ada2            ;indirect pointer to operand 2 in absolute memory
        381d : 0402             sbi2_   dw  sba2            ;indirect pointer to complemented operand 2 (SBC)
        381f : 0401             adiy2_  dw  ada2-$ff        ;with offset for indirect indexed
        3821 : 0501             sbiy2_  dw  sba2-$ff
        3823 :                  zp_end
                                    if (zp_end - zp_init) != (zp_bss_end - zp_bss)   
                                        ;force assembler error if size is different   
                                        ERROR ERROR ERROR   ;mismatch between bss and zeropage data
                                    endif 
        3823 :                  data_init
        3823 : 2900             ex_and_ and #0              ;execute immediate opcodes
        3825 : 60                       rts
        3826 : 4900             ex_eor_ eor #0              ;execute immediate opcodes
        3828 : 60                       rts
        3829 : 0900             ex_ora_ ora #0              ;execute immediate opcodes
        382b : 60                       rts
        382c : 6900             ex_adc_ adc #0              ;execute immediate opcodes
        382e : 60                       rts
        382f : e900             ex_sbc_ sbc #0              ;execute immediate opcodes
        3831 : 60                       rts
        3832 : c3824100         abs1_   db  $c3,$82,$41,0   ;test patterns for LDx BIT ROL ROR ASL LSR
        3836 : 7f               abs7f_  db  $7f             ;test pattern for compare
                                ;loads
        3837 : 80800002         fLDx_   db  fn,fn,0,fz      ;expected flags for load
                                ;shifts
        383b :                  rASL_                       ;expected result ASL & ROL -carry
        383b : 86048200         rROL_   db  $86,$04,$82,0   ; "
        383f : 87058301         rROLc_  db  $87,$05,$83,1   ;expected result ROL +carry
        3843 :                  rLSR_                       ;expected result LSR & ROR -carry
        3843 : 61412000         rROR_   db  $61,$41,$20,0   ; "
        3847 : e1c1a080         rRORc_  db  $e1,$c1,$a0,$80 ;expected result ROR +carry
        384b :                  fASL_                       ;expected flags for shifts
        384b : 81018002         fROL_   db  fnc,fc,fn,fz    ;no carry in
        384f : 81018000         fROLc_  db  fnc,fc,fn,0     ;carry in
        3853 :                  fLSR_
        3853 : 01000102         fROR_   db  fc,0,fc,fz      ;no carry in
        3857 : 81808180         fRORc_  db  fnc,fn,fnc,fn   ;carry in
                                ;increments (decrements)
        385b : 7f80ff0001       rINC_   db  $7f,$80,$ff,0,1 ;expected result for INC/DEC
        3860 : 0080800200       fINC_   db  0,fn,fn,fz,0    ;expected flags for INC/DEC
                                ;logical memory operand
        3865 : 001f7180         absOR_  db  0,$1f,$71,$80   ;test pattern for OR
        3869 : 0fff7f80         absAN_  db  $0f,$ff,$7f,$80 ;test pattern for AND
        386d : ff0f8f8f         absEO_  db  $ff,$0f,$8f,$8f ;test pattern for EOR
                                ;logical accu operand
        3871 : 00f11f00         absORa_ db  0,$f1,$1f,0     ;test pattern for OR
        3875 : f0ffffff         absANa_ db  $f0,$ff,$ff,$ff ;test pattern for AND
        3879 : fff0f00f         absEOa_ db  $ff,$f0,$f0,$0f ;test pattern for EOR
                                ;logical results
        387d : 00ff7f80         absrlo_ db  0,$ff,$7f,$80
        3881 : 02800080         absflo_ db  fz,fn,0,fn
        3885 :                  data_end
                                    if (data_end - data_init) != (data_bss_end - data_bss)
                                        ;force assembler error if size is different   
                                        ERROR ERROR ERROR   ;mismatch between bss and data
                                    endif 

        3885 :                  vec_init
        3885 : 9137                     dw  nmi_trap
        3887 : 9437                     dw  res_trap
        3889 : 9937                     dw  irq_trap
        fffa =                  vec_bss equ $fffa
                                    endif                   ;end of RAM init data

                                    if (load_data_direct = 1) & (ROM_vectors = 1)  
                                        org $fffa       ;vectors
                                        dw  nmi_trap
                                        dw  res_trap
                                        dw  irq_trap
                                    endif

        fffa =                          end start


        No errors in pass 2.
        Wrote binary from address $0400 through $388a.
        Total size 13451 bytes.
        Program start address is at $0400 (1024).
*/
    public static int[] getMem1() {        
        int mem[] = {
            0xd8, 0xa2, 0xff, 0x9a, 0xa9, 0x00, 0x8d, 0x00, 0x02, 0xa2, 
            0x05, 0x4c, 0x33, 0x04, 0xa0, 0x05, 0xd0, 0x08, 0x4c, 0x12, 
            0x04, 0x88, 0x88, 0x88, 0x88, 0x88, 0x88, 0x88, 0x88, 0x88, 
            0x88, 0xf0, 0x17, 0x4c, 0x21, 0x04, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xf0, 0xde, 0x4c, 0x30, 
            0x04, 0xd0, 0xf4, 0x4c, 0x35, 0x04, 0xa2, 0x46, 0xbd, 0xdc, 
            0x37, 0x95, 0x13, 0xca, 0x10, 0xf8, 0xa2, 0x61, 0xbd, 0x23, 
            0x38, 0x9d, 0x08, 0x02, 0xca, 0x10, 0xf7, 0xa2, 0x05, 0xbd, 
            0x85, 0x38, 0x9d, 0xfa, 0xff, 0xca, 0x10, 0xf7, 0xad, 0x00, 
            0x02, 0xc9, 0x00, 0xd0, 0xfe, 0xa9, 0x01, 0x8d, 0x00, 0x02, 
            0xa0, 0xfe, 0x88, 0x98, 0xaa, 0x10, 0x08, 0x18, 0x69, 0x02, 
            0xea, 0xea, 0xea, 0xea, 0xea, 0xea, 0xea, 0xea, 0xea, 0xea, 
            0x49, 0x7f, 0x8d, 0x06, 0x05, 0xa9, 0x00, 0x4c, 0x05, 0x05, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xf0, 0x3e, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 0xca, 
            0xea, 0xea, 0xea, 0xea, 0xea, 0xf0, 0x08, 0x4c, 0x8d, 0x05, 
            0xea, 0xea, 0xea, 0xea, 0xea, 0xea, 0xea, 0xea, 0xea, 0xea, 
            0xc0, 0x00, 0xf0, 0x03, 0x4c, 0x66, 0x04, 0xad, 0x00, 0x02, 
            0xc9, 0x01, 0xd0, 0xfe, 0xa9, 0x02, 0x8d, 0x00, 0x02, 0xc0, 
            0x01, 0xd0, 0x03, 0x4c, 0xb1, 0x05, 0xa9, 0x00, 0xc9, 0x00, 
            0xd0, 0xfe, 0x90, 0xfe, 0x30, 0xfe, 0xc9, 0x01, 0xf0, 0xfe, 
            0xb0, 0xfe, 0x10, 0xfe, 0xaa, 0xe0, 0x00, 0xd0, 0xfe, 0x90, 
            0xfe, 0x30, 0xfe, 0xe0, 0x01, 0xf0, 0xfe, 0xb0, 0xfe, 0x10, 
            0xfe, 0xa8, 0xc0, 0x00, 0xd0, 0xfe, 0x90, 0xfe, 0x30, 0xfe, 
            0xc0, 0x01, 0xf0, 0xfe, 0xb0, 0xfe, 0x10, 0xfe, 0xad, 0x00, 
            0x02, 0xc9, 0x02, 0xd0, 0xfe, 0xa9, 0x03, 0x8d, 0x00, 0x02, 
            0xa2, 0xff, 0x9a, 0xa9, 0x55, 0x48, 0xa9, 0xaa, 0x48, 0xcd, 
            0xfe, 0x01, 0xd0, 0xfe, 0xba, 0x8a, 0xc9, 0xfd, 0xd0, 0xfe, 
            0x68, 0xc9, 0xaa, 0xd0, 0xfe, 0x68, 0xc9, 0x55, 0xd0, 0xfe, 
            0xcd, 0xff, 0x01, 0xd0, 0xfe, 0xba, 0xe0, 0xff, 0xd0, 0xfe, 
            0xad, 0x00, 0x02, 0xc9, 0x03, 0xd0, 0xfe, 0xa9, 0x04, 0x8d, 
            0x00, 0x02, 0xa9, 0xff, 0x48, 0x28, 0x10, 0x1a, 0x50, 0x1b, 
            0x90, 0x1c, 0xd0, 0x1d, 0x30, 0x03, 0x4c, 0x36, 0x06, 0x70, 
            0x03, 0x4c, 0x3b, 0x06, 0xb0, 0x03, 0x4c, 0x40, 0x06, 0xf0, 
            0x0f, 0x4c, 0x45, 0x06, 0x4c, 0x48, 0x06, 0x4c, 0x4b, 0x06, 
            0x4c, 0x4e, 0x06, 0x4c, 0x51, 0x06, 0x08, 0xba, 0xe0, 0xfe, 
            0xd0, 0xfe, 0x68, 0xc9, 0xff, 0xd0, 0xfe, 0xba, 0xe0, 0xff, 
            0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0x30, 0x1a, 0x70, 0x1b, 
            0xb0, 0x1c, 0xf0, 0x1d, 0x10, 0x03, 0x4c, 0x72, 0x06, 0x50, 
            0x03, 0x4c, 0x77, 0x06, 0x90, 0x03, 0x4c, 0x7c, 0x06, 0xd0, 
            0x0f, 0x4c, 0x81, 0x06, 0x4c, 0x84, 0x06, 0x4c, 0x87, 0x06, 
            0x4c, 0x8a, 0x06, 0x4c, 0x8d, 0x06, 0x08, 0x68, 0xc9, 0x30, 
            0xd0, 0xfe, 0xa9, 0x02, 0x48, 0x28, 0xd0, 0x02, 0xf0, 0x03, 
            0x4c, 0x9e, 0x06, 0xb0, 0x02, 0x90, 0x03, 0x4c, 0xa5, 0x06, 
            0x30, 0x02, 0x10, 0x03, 0x4c, 0xac, 0x06, 0x70, 0x02, 0x50, 
            0x03, 0x4c, 0xb3, 0x06, 0xa9, 0x01, 0x48, 0x28, 0xf0, 0x02, 
            0xd0, 0x03, 0x4c, 0xbe, 0x06, 0x90, 0x02, 0xb0, 0x03, 0x4c, 
            0xc5, 0x06, 0x30, 0x02, 0x10, 0x03, 0x4c, 0xcc, 0x06, 0x70, 
            0x02, 0x50, 0x03, 0x4c, 0xd3, 0x06, 0xa9, 0x80, 0x48, 0x28, 
            0xf0, 0x02, 0xd0, 0x03, 0x4c, 0xde, 0x06, 0xb0, 0x02, 0x90, 
            0x03, 0x4c, 0xe5, 0x06, 0x10, 0x02, 0x30, 0x03, 0x4c, 0xec, 
            0x06, 0x70, 0x02, 0x50, 0x03, 0x4c, 0xf3, 0x06, 0xa9, 0x40, 
            0x48, 0x28, 0xf0, 0x02, 0xd0, 0x03, 0x4c, 0xfe, 0x06, 0xb0, 
            0x02, 0x90, 0x03, 0x4c, 0x05, 0x07, 0x30, 0x02, 0x10, 0x03, 
            0x4c, 0x0c, 0x07, 0x50, 0x02, 0x70, 0x03, 0x4c, 0x13, 0x07, 
            0xa9, 0xfd, 0x48, 0x28, 0xf0, 0x02, 0xd0, 0x03, 0x4c, 0x1e, 
            0x07, 0x90, 0x02, 0xb0, 0x03, 0x4c, 0x25, 0x07, 0x10, 0x02, 
            0x30, 0x03, 0x4c, 0x2c, 0x07, 0x50, 0x02, 0x70, 0x03, 0x4c, 
            0x33, 0x07, 0xa9, 0xfe, 0x48, 0x28, 0xd0, 0x02, 0xf0, 0x03, 
            0x4c, 0x3e, 0x07, 0xb0, 0x02, 0x90, 0x03, 0x4c, 0x45, 0x07, 
            0x10, 0x02, 0x30, 0x03, 0x4c, 0x4c, 0x07, 0x50, 0x02, 0x70, 
            0x03, 0x4c, 0x53, 0x07, 0xa9, 0x7f, 0x48, 0x28, 0xd0, 0x02, 
            0xf0, 0x03, 0x4c, 0x5e, 0x07, 0x90, 0x02, 0xb0, 0x03, 0x4c, 
            0x65, 0x07, 0x30, 0x02, 0x10, 0x03, 0x4c, 0x6c, 0x07, 0x50, 
            0x02, 0x70, 0x03, 0x4c, 0x73, 0x07, 0xa9, 0xbf, 0x48, 0x28, 
            0xd0, 0x02, 0xf0, 0x03, 0x4c, 0x7e, 0x07, 0x90, 0x02, 0xb0, 
            0x03, 0x4c, 0x85, 0x07, 0x10, 0x02, 0x30, 0x03, 0x4c, 0x8c, 
            0x07, 0x70, 0x02, 0x50, 0x03, 0x4c, 0x93, 0x07, 0xad, 0x00, 
            0x02, 0xc9, 0x04, 0xd0, 0xfe, 0xa9, 0x05, 0x8d, 0x00, 0x02, 
            0xa2, 0x55, 0xa0, 0xaa, 0xa9, 0xff, 0x48, 0xa9, 0x01, 0x28, 
            0x48, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xff, 
            0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x00, 0x28, 0x48, 
            0x08, 0xc9, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x30, 0xd0, 
            0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0xff, 0x28, 0x48, 0x08, 
            0xc9, 0xff, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xff, 0xd0, 0xfe, 
            0x28, 0xa9, 0x00, 0x48, 0xa9, 0x01, 0x28, 0x48, 0x08, 0xc9, 
            0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x30, 0xd0, 0xfe, 0x28, 
            0xa9, 0xff, 0x48, 0xa9, 0x00, 0x28, 0x48, 0x08, 0xc9, 0x00, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xff, 0xd0, 0xfe, 0x28, 0xa9, 
            0x00, 0x48, 0xa9, 0xff, 0x28, 0x48, 0x08, 0xc9, 0xff, 0xd0, 
            0xfe, 0x68, 0x48, 0xc9, 0x30, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 
            0x48, 0xa9, 0x00, 0x28, 0x68, 0x08, 0xc9, 0xff, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xfd, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 
            0xa9, 0xff, 0x28, 0x68, 0x08, 0xc9, 0x00, 0xd0, 0xfe, 0x68, 
            0x48, 0xc9, 0x32, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 
            0xfe, 0x28, 0x68, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x00, 
            0x28, 0x68, 0x08, 0xc9, 0xff, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 
            0xb0, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0xff, 0x28, 
            0x68, 0x08, 0xc9, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7f, 
            0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0xfe, 0x28, 0x68, 
            0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x30, 0xd0, 
            0xfe, 0x28, 0xe0, 0x55, 0xd0, 0xfe, 0xc0, 0xaa, 0xd0, 0xfe, 
            0xad, 0x00, 0x02, 0xc9, 0x05, 0xd0, 0xfe, 0xa9, 0x06, 0x8d, 
            0x00, 0x02, 0xa9, 0x00, 0x48, 0xa9, 0x3c, 0x28, 0x49, 0xc3, 
            0x08, 0xc9, 0xff, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xb0, 0xd0, 
            0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0xc3, 0x28, 0x49, 0xc3, 
            0x08, 0xc9, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x32, 0xd0, 
            0xfe, 0x28, 0xad, 0x00, 0x02, 0xc9, 0x06, 0xd0, 0xfe, 0xa9, 
            0x07, 0x8d, 0x00, 0x02, 0xa2, 0x24, 0xa0, 0x42, 0xa9, 0x00, 
            0x48, 0xa9, 0x18, 0x28, 0xea, 0x08, 0xc9, 0x18, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x30, 0xd0, 0xfe, 0x28, 0xe0, 0x24, 0xd0, 
            0xfe, 0xc0, 0x42, 0xd0, 0xfe, 0xa2, 0xdb, 0xa0, 0xbd, 0xa9, 
            0xff, 0x48, 0xa9, 0xe7, 0x28, 0xea, 0x08, 0xc9, 0xe7, 0xd0, 
            0xfe, 0x68, 0x48, 0xc9, 0xff, 0xd0, 0xfe, 0x28, 0xe0, 0xdb, 
            0xd0, 0xfe, 0xc0, 0xbd, 0xd0, 0xfe, 0xad, 0x00, 0x02, 0xc9, 
            0x07, 0xd0, 0xfe, 0xa9, 0x08, 0x8d, 0x00, 0x02, 0xa9, 0x00, 
            0x48, 0x28, 0xa9, 0x46, 0xa2, 0x41, 0xa0, 0x52, 0x4c, 0xe9, 
            0x36, 0xea, 0xea, 0xd0, 0xfe, 0xe8, 0xe8, 0xf0, 0xfe, 0x10, 
            0xfe, 0x90, 0xfe, 0x50, 0xfe, 0xc9, 0xec, 0xd0, 0xfe, 0xe0, 
            0x42, 0xd0, 0xfe, 0xc0, 0x4f, 0xd0, 0xfe, 0xca, 0xc8, 0xc8, 
            0xc8, 0x49, 0xaa, 0x4c, 0x52, 0x09, 0xea, 0xea, 0xd0, 0xfe, 
            0xe8, 0xe8, 0xf0, 0xfe, 0x30, 0xfe, 0x90, 0xfe, 0x50, 0xfe, 
            0xc9, 0x46, 0xd0, 0xfe, 0xe0, 0x41, 0xd0, 0xfe, 0xc0, 0x52, 
            0xd0, 0xfe, 0xad, 0x00, 0x02, 0xc9, 0x08, 0xd0, 0xfe, 0xa9, 
            0x09, 0x8d, 0x00, 0x02, 0xa9, 0x00, 0x48, 0x28, 0xa9, 0x49, 
            0xa2, 0x4e, 0xa0, 0x44, 0x6c, 0x18, 0x37, 0xea, 0xd0, 0xfe, 
            0x88, 0x88, 0x08, 0x88, 0x88, 0x88, 0x28, 0xf0, 0xfe, 0x10, 
            0xfe, 0x90, 0xfe, 0x50, 0xfe, 0xc9, 0xe3, 0xd0, 0xfe, 0xe0, 
            0x4f, 0xd0, 0xfe, 0xc0, 0x3e, 0xd0, 0xfe, 0xba, 0xe0, 0xff, 
            0xd0, 0xfe, 0xad, 0x00, 0x02, 0xc9, 0x09, 0xd0, 0xfe, 0xa9, 
            0x0a, 0x8d, 0x00, 0x02, 0xa9, 0x00, 0x48, 0x28, 0xa9, 0x4a, 
            0xa2, 0x53, 0xa0, 0x52, 0x20, 0x54, 0x37, 0x08, 0x88, 0x88, 
            0x88, 0x28, 0xf0, 0xfe, 0x10, 0xfe, 0x90, 0xfe, 0x50, 0xfe, 
            0xc9, 0xe0, 0xd0, 0xfe, 0xe0, 0x54, 0xd0, 0xfe, 0xc0, 0x4c, 
            0xd0, 0xfe, 0xba, 0xe0, 0xff, 0xd0, 0xfe, 0xad, 0x00, 0x02, 
            0xc9, 0x0a, 0xd0, 0xfe, 0xa9, 0x0b, 0x8d, 0x00, 0x02, 0xa9, 
            0x00, 0x48, 0x28, 0xa9, 0x42, 0xa2, 0x52, 0xa0, 0x4b, 0x00, 
            0x88, 0x08, 0x88, 0x88, 0x88, 0xc9, 0xe8, 0xd0, 0xfe, 0xe0, 
            0x53, 0xd0, 0xfe, 0xc0, 0x45, 0xd0, 0xfe, 0x68, 0xc9, 0x30, 
            0xd0, 0xfe, 0xba, 0xe0, 0xff, 0xd0, 0xfe, 0xad, 0x00, 0x02, 
            0xc9, 0x0b, 0xd0, 0xfe, 0xa9, 0x0c, 0x8d, 0x00, 0x02, 0xa9, 
            0xff, 0x48, 0x28, 0x18, 0x08, 0x68, 0x48, 0xc9, 0xfe, 0xd0, 
            0xfe, 0x28, 0x38, 0x08, 0x68, 0x48, 0xc9, 0xff, 0xd0, 0xfe, 
            0x28, 0x58, 0x08, 0x68, 0x48, 0xc9, 0xfb, 0xd0, 0xfe, 0x28, 
            0x78, 0x08, 0x68, 0x48, 0xc9, 0xff, 0xd0, 0xfe, 0x28, 0xd8, 
            0x08, 0x68, 0x48, 0xc9, 0xf7, 0xd0, 0xfe, 0x28, 0xf8, 0x08, 
            0x68, 0x48, 0xc9, 0xff, 0xd0, 0xfe, 0x28, 0xb8, 0x08, 0x68, 
            0x48, 0xc9, 0xbf, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0x28, 
            0x08, 0x68, 0x48, 0xc9, 0x30, 0xd0, 0xfe, 0x28, 0x38, 0x08, 
            0x68, 0x48, 0xc9, 0x31, 0xd0, 0xfe, 0x28, 0x18, 0x08, 0x68, 
            0x48, 0xc9, 0x30, 0xd0, 0xfe, 0x28, 0x78, 0x08, 0x68, 0x48, 
            0xc9, 0x34, 0xd0, 0xfe, 0x28, 0x58, 0x08, 0x68, 0x48, 0xc9, 
            0x30, 0xd0, 0xfe, 0x28, 0xf8, 0x08, 0x68, 0x48, 0xc9, 0x38, 
            0xd0, 0xfe, 0x28, 0xd8, 0x08, 0x68, 0x48, 0xc9, 0x30, 0xd0, 
            0xfe, 0x28, 0xa9, 0x40, 0x48, 0x28, 0x08, 0x68, 0x48, 0xc9, 
            0x70, 0xd0, 0xfe, 0x28, 0xb8, 0x08, 0x68, 0x48, 0xc9, 0x30, 
            0xd0, 0xfe, 0x28, 0xad, 0x00, 0x02, 0xc9, 0x0c, 0xd0, 0xfe, 
            0xa9, 0x0d, 0x8d, 0x00, 0x02, 0xa2, 0xfe, 0xa9, 0xff, 0x48, 
            0x28, 0xe8, 0x08, 0xe0, 0xff, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 
            0xfd, 0xd0, 0xfe, 0x28, 0xe8, 0x08, 0xe0, 0x00, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x7f, 0xd0, 0xfe, 0x28, 0xe8, 0x08, 0xe0, 
            0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 0x28, 
            0xca, 0x08, 0xe0, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7f, 
            0xd0, 0xfe, 0x28, 0xca, 0x08, 0xe0, 0xff, 0xd0, 0xfe, 0x68, 
            0x48, 0xc9, 0xfd, 0xd0, 0xfe, 0x28, 0xca, 0xa9, 0x00, 0x48, 
            0x28, 0xe8, 0x08, 0xe0, 0xff, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 
            0xb0, 0xd0, 0xfe, 0x28, 0xe8, 0x08, 0xe0, 0x00, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x32, 0xd0, 0xfe, 0x28, 0xe8, 0x08, 0xe0, 
            0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x30, 0xd0, 0xfe, 0x28, 
            0xca, 0x08, 0xe0, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x32, 
            0xd0, 0xfe, 0x28, 0xca, 0x08, 0xe0, 0xff, 0xd0, 0xfe, 0x68, 
            0x48, 0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa0, 0xfe, 0xa9, 0xff, 
            0x48, 0x28, 0xc8, 0x08, 0xc0, 0xff, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0xfd, 0xd0, 0xfe, 0x28, 0xc8, 0x08, 0xc0, 0x00, 0xd0, 
            0xfe, 0x68, 0x48, 0xc9, 0x7f, 0xd0, 0xfe, 0x28, 0xc8, 0x08, 
            0xc0, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 
            0x28, 0x88, 0x08, 0xc0, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 
            0x7f, 0xd0, 0xfe, 0x28, 0x88, 0x08, 0xc0, 0xff, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xfd, 0xd0, 0xfe, 0x28, 0x88, 0xa9, 0x00, 
            0x48, 0x28, 0xc8, 0x08, 0xc0, 0xff, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xc8, 0x08, 0xc0, 0x00, 0xd0, 
            0xfe, 0x68, 0x48, 0xc9, 0x32, 0xd0, 0xfe, 0x28, 0xc8, 0x08, 
            0xc0, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x30, 0xd0, 0xfe, 
            0x28, 0x88, 0x08, 0xc0, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 
            0x32, 0xd0, 0xfe, 0x28, 0x88, 0x08, 0xc0, 0xff, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa2, 0xff, 0xa9, 
            0xff, 0x48, 0x28, 0x8a, 0x08, 0xc9, 0xff, 0xd0, 0xfe, 0x68, 
            0x48, 0xc9, 0xfd, 0xd0, 0xfe, 0x28, 0x08, 0xe8, 0x28, 0x8a, 
            0x08, 0xc9, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7f, 0xd0, 
            0xfe, 0x28, 0x08, 0xe8, 0x28, 0x8a, 0x08, 0xc9, 0x01, 0xd0, 
            0xfe, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 
            0x48, 0x28, 0x8a, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0x30, 0xd0, 0xfe, 0x28, 0x08, 0xca, 0x28, 0x8a, 0x08, 
            0xc9, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x32, 0xd0, 0xfe, 
            0x28, 0x08, 0xca, 0x28, 0x8a, 0x08, 0xc9, 0xff, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa0, 0xff, 0xa9, 
            0xff, 0x48, 0x28, 0x98, 0x08, 0xc9, 0xff, 0xd0, 0xfe, 0x68, 
            0x48, 0xc9, 0xfd, 0xd0, 0xfe, 0x28, 0x08, 0xc8, 0x28, 0x98, 
            0x08, 0xc9, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7f, 0xd0, 
            0xfe, 0x28, 0x08, 0xc8, 0x28, 0x98, 0x08, 0xc9, 0x01, 0xd0, 
            0xfe, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 
            0x48, 0x28, 0x98, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0x30, 0xd0, 0xfe, 0x28, 0x08, 0x88, 0x28, 0x98, 0x08, 
            0xc9, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x32, 0xd0, 0xfe, 
            0x28, 0x08, 0x88, 0x28, 0x98, 0x08, 0xc9, 0xff, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 
            0xa2, 0xff, 0x8a, 0x28, 0xa8, 0x08, 0xc0, 0xff, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xfd, 0xd0, 0xfe, 0x28, 0x08, 0xe8, 0x8a, 
            0x28, 0xa8, 0x08, 0xc0, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 
            0x7f, 0xd0, 0xfe, 0x28, 0x08, 0xe8, 0x8a, 0x28, 0xa8, 0x08, 
            0xc0, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 
            0x28, 0xa9, 0x00, 0x48, 0xa9, 0x00, 0x8a, 0x28, 0xa8, 0x08, 
            0xc0, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x30, 0xd0, 0xfe, 
            0x28, 0x08, 0xca, 0x8a, 0x28, 0xa8, 0x08, 0xc0, 0x00, 0xd0, 
            0xfe, 0x68, 0x48, 0xc9, 0x32, 0xd0, 0xfe, 0x28, 0x08, 0xca, 
            0x8a, 0x28, 0xa8, 0x08, 0xc0, 0xff, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa0, 0xff, 
            0x98, 0x28, 0xaa, 0x08, 0xe0, 0xff, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0xfd, 0xd0, 0xfe, 0x28, 0x08, 0xc8, 0x98, 0x28, 0xaa, 
            0x08, 0xe0, 0x00, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7f, 0xd0, 
            0xfe, 0x28, 0x08, 0xc8, 0x98, 0x28, 0xaa, 0x08, 0xe0, 0x01, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xa9, 
            0x00, 0x48, 0xa9, 0x00, 0x98, 0x28, 0xaa, 0x08, 0xe0, 0x01, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x30, 0xd0, 0xfe, 0x28, 0x08, 
            0x88, 0x98, 0x28, 0xaa, 0x08, 0xe0, 0x00, 0xd0, 0xfe, 0x68, 
            0x48, 0xc9, 0x32, 0xd0, 0xfe, 0x28, 0x08, 0x88, 0x98, 0x28, 
            0xaa, 0x08, 0xe0, 0xff, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xb0, 
            0xd0, 0xfe, 0x28, 0xad, 0x00, 0x02, 0xc9, 0x0d, 0xd0, 0xfe, 
            0xa9, 0x0e, 0x8d, 0x00, 0x02, 0xa2, 0x01, 0xa9, 0xff, 0x48, 
            0x28, 0x9a, 0x08, 0xad, 0x01, 0x01, 0xc9, 0xff, 0xd0, 0xfe, 
            0xa9, 0x00, 0x48, 0x28, 0x9a, 0x08, 0xad, 0x01, 0x01, 0xc9, 
            0x30, 0xd0, 0xfe, 0xca, 0xa9, 0xff, 0x48, 0x28, 0x9a, 0x08, 
            0xad, 0x00, 0x01, 0xc9, 0xff, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 
            0x28, 0x9a, 0x08, 0xad, 0x00, 0x01, 0xc9, 0x30, 0xd0, 0xfe, 
            0xca, 0xa9, 0xff, 0x48, 0x28, 0x9a, 0x08, 0xad, 0xff, 0x01, 
            0xc9, 0xff, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0x9a, 0x08, 
            0xad, 0xff, 0x01, 0xc9, 0x30, 0xa2, 0x01, 0x9a, 0xa9, 0xff, 
            0x48, 0x28, 0xba, 0x08, 0xe0, 0x01, 0xd0, 0xfe, 0xad, 0x01, 
            0x01, 0xc9, 0x7d, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 0x28, 0xba, 
            0x08, 0xe0, 0x00, 0xd0, 0xfe, 0xad, 0x00, 0x01, 0xc9, 0x7f, 
            0xd0, 0xfe, 0xa9, 0xff, 0x48, 0x28, 0xba, 0x08, 0xe0, 0xff, 
            0xd0, 0xfe, 0xad, 0xff, 0x01, 0xc9, 0xfd, 0xd0, 0xfe, 0xa2, 
            0x01, 0x9a, 0xa9, 0x00, 0x48, 0x28, 0xba, 0x08, 0xe0, 0x01, 
            0xd0, 0xfe, 0xad, 0x01, 0x01, 0xc9, 0x30, 0xd0, 0xfe, 0xa9, 
            0x00, 0x48, 0x28, 0xba, 0x08, 0xe0, 0x00, 0xd0, 0xfe, 0xad, 
            0x00, 0x01, 0xc9, 0x32, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 
            0xba, 0x08, 0xe0, 0xff, 0xd0, 0xfe, 0xad, 0xff, 0x01, 0xc9, 
            0xb0, 0xd0, 0xfe, 0x68, 0xad, 0x00, 0x02, 0xc9, 0x0e, 0xd0, 
            0xfe, 0xa9, 0x0f, 0x8d, 0x00, 0x02, 0xa0, 0x03, 0xa9, 0x00, 
            0x48, 0x28, 0xb6, 0x13, 0x08, 0x8a, 0x49, 0xc3, 0x28, 0x99, 
            0x03, 0x02, 0x08, 0x49, 0xc3, 0xd9, 0x17, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xd9, 0x1c, 0x02, 0xd0, 0xfe, 0x88, 0x10, 
            0xdf, 0xa0, 0x03, 0xa9, 0xff, 0x48, 0x28, 0xb6, 0x13, 0x08, 
            0x8a, 0x49, 0xc3, 0x28, 0x99, 0x03, 0x02, 0x08, 0x49, 0xc3, 
            0xd9, 0x17, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xd9, 0x1c, 
            0x02, 0xd0, 0xfe, 0x88, 0x10, 0xdf, 0xa0, 0x03, 0xa9, 0x00, 
            0x48, 0x28, 0xbe, 0x17, 0x02, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 
            0x28, 0x96, 0x0c, 0x08, 0x49, 0xc3, 0xd9, 0x13, 0x00, 0xd0, 
            0xfe, 0x68, 0x49, 0x30, 0xd9, 0x1c, 0x02, 0xd0, 0xfe, 0x88, 
            0x10, 0xde, 0xa0, 0x03, 0xa9, 0xff, 0x48, 0x28, 0xbe, 0x17, 
            0x02, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 0x96, 0x0c, 0x08, 
            0x49, 0xc3, 0xd9, 0x13, 0x00, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 
            0xd9, 0x1c, 0x02, 0xd0, 0xfe, 0x88, 0x10, 0xde, 0xa0, 0x03, 
            0xa2, 0x00, 0xb9, 0x0c, 0x00, 0x49, 0xc3, 0xd9, 0x13, 0x00, 
            0xd0, 0xfe, 0x96, 0x0c, 0xb9, 0x03, 0x02, 0x49, 0xc3, 0xd9, 
            0x17, 0x02, 0xd0, 0xfe, 0x8a, 0x99, 0x03, 0x02, 0x88, 0x10, 
            0xe3, 0xad, 0x00, 0x02, 0xc9, 0x0f, 0xd0, 0xfe, 0xa9, 0x10, 
            0x8d, 0x00, 0x02, 0xa0, 0xfd, 0xb6, 0x19, 0x8a, 0x99, 0x09, 
            0x01, 0x88, 0xc0, 0xfa, 0xb0, 0xf5, 0xa0, 0xfd, 0xbe, 0x1d, 
            0x01, 0x96, 0x12, 0x88, 0xc0, 0xfa, 0xb0, 0xf6, 0xa0, 0x03, 
            0xa2, 0x00, 0xb9, 0x0c, 0x00, 0xd9, 0x13, 0x00, 0xd0, 0xfe, 
            0x96, 0x0c, 0xb9, 0x03, 0x02, 0xd9, 0x17, 0x02, 0xd0, 0xfe, 
            0x8a, 0x99, 0x03, 0x02, 0x88, 0x10, 0xe7, 0xad, 0x00, 0x02, 
            0xc9, 0x10, 0xd0, 0xfe, 0xa9, 0x11, 0x8d, 0x00, 0x02, 0xa2, 
            0x03, 0xa9, 0x00, 0x48, 0x28, 0xb4, 0x13, 0x08, 0x98, 0x49, 
            0xc3, 0x28, 0x9d, 0x03, 0x02, 0x08, 0x49, 0xc3, 0xdd, 0x17, 
            0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x1c, 0x02, 0xd0, 
            0xfe, 0xca, 0x10, 0xdf, 0xa2, 0x03, 0xa9, 0xff, 0x48, 0x28, 
            0xb4, 0x13, 0x08, 0x98, 0x49, 0xc3, 0x28, 0x9d, 0x03, 0x02, 
            0x08, 0x49, 0xc3, 0xdd, 0x17, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7d, 0xdd, 0x1c, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xdf, 0xa2, 
            0x03, 0xa9, 0x00, 0x48, 0x28, 0xbc, 0x17, 0x02, 0x08, 0x98, 
            0x49, 0xc3, 0xa8, 0x28, 0x94, 0x0c, 0x08, 0x49, 0xc3, 0xd5, 
            0x13, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x1c, 0x02, 0xd0, 
            0xfe, 0xca, 0x10, 0xdf, 0xa2, 0x03, 0xa9, 0xff, 0x48, 0x28, 
            0xbc, 0x17, 0x02, 0x08, 0x98, 0x49, 0xc3, 0xa8, 0x28, 0x94, 
            0x0c, 0x08, 0x49, 0xc3, 0xd5, 0x13, 0xd0, 0xfe, 0x68, 0x49, 
            0x7d, 0xdd, 0x1c, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xdf, 0xa2, 
            0x03, 0xa0, 0x00, 0xb5, 0x0c, 0x49, 0xc3, 0xd5, 0x13, 0xd0, 
            0xfe, 0x94, 0x0c, 0xbd, 0x03, 0x02, 0x49, 0xc3, 0xdd, 0x17, 
            0x02, 0xd0, 0xfe, 0x8a, 0x9d, 0x03, 0x02, 0xca, 0x10, 0xe5, 
            0xad, 0x00, 0x02, 0xc9, 0x11, 0xd0, 0xfe, 0xa9, 0x12, 0x8d, 
            0x00, 0x02, 0xa2, 0xfd, 0xb4, 0x19, 0x98, 0x9d, 0x09, 0x01, 
            0xca, 0xe0, 0xfa, 0xb0, 0xf5, 0xa2, 0xfd, 0xbc, 0x1d, 0x01, 
            0x94, 0x12, 0xca, 0xe0, 0xfa, 0xb0, 0xf6, 0xa2, 0x03, 0xa0, 
            0x00, 0xb5, 0x0c, 0xd5, 0x13, 0xd0, 0xfe, 0x94, 0x0c, 0xbd, 
            0x03, 0x02, 0xdd, 0x17, 0x02, 0xd0, 0xfe, 0x8a, 0x9d, 0x03, 
            0x02, 0xca, 0x10, 0xe9, 0xad, 0x00, 0x02, 0xc9, 0x12, 0xd0, 
            0xfe, 0xa9, 0x13, 0x8d, 0x00, 0x02, 0xa9, 0x00, 0x48, 0x28, 
            0xa6, 0x13, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 0x8e, 0x03, 
            0x02, 0x08, 0x49, 0xc3, 0xaa, 0xe0, 0xc3, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xcd, 0x1c, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 
            0x28, 0xa6, 0x14, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 0x8e, 
            0x04, 0x02, 0x08, 0x49, 0xc3, 0xaa, 0xe0, 0x82, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 
            0x48, 0x28, 0xa6, 0x15, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 
            0x8e, 0x05, 0x02, 0x08, 0x49, 0xc3, 0xaa, 0xe0, 0x41, 0xd0, 
            0xfe, 0x68, 0x49, 0x30, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 
            0x00, 0x48, 0x28, 0xa6, 0x16, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 
            0x28, 0x8e, 0x06, 0x02, 0x08, 0x49, 0xc3, 0xaa, 0xe0, 0x00, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 
            0xa9, 0xff, 0x48, 0x28, 0xa6, 0x13, 0x08, 0x8a, 0x49, 0xc3, 
            0xaa, 0x28, 0x8e, 0x03, 0x02, 0x08, 0x49, 0xc3, 0xaa, 0xe0, 
            0xc3, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1c, 0x02, 0xd0, 
            0xfe, 0xa9, 0xff, 0x48, 0x28, 0xa6, 0x14, 0x08, 0x8a, 0x49, 
            0xc3, 0xaa, 0x28, 0x8e, 0x04, 0x02, 0x08, 0x49, 0xc3, 0xaa, 
            0xe0, 0x82, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1d, 0x02, 
            0xd0, 0xfe, 0xa9, 0xff, 0x48, 0x28, 0xa6, 0x15, 0x08, 0x8a, 
            0x49, 0xc3, 0xaa, 0x28, 0x8e, 0x05, 0x02, 0x08, 0x49, 0xc3, 
            0xaa, 0xe0, 0x41, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1e, 
            0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 0x28, 0xa6, 0x16, 0x08, 
            0x8a, 0x49, 0xc3, 0xaa, 0x28, 0x8e, 0x06, 0x02, 0x08, 0x49, 
            0xc3, 0xaa, 0xe0, 0x00, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 
            0x1f, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xae, 0x17, 
            0x02, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 0x86, 0x0c, 0x08, 
            0x49, 0xc3, 0xc5, 0x13, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 
            0x1c, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xae, 0x18, 
            0x02, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 0x86, 0x0d, 0x08, 
            0x49, 0xc3, 0xc5, 0x14, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 
            0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xae, 0x19, 
            0x02, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 0x86, 0x0e, 0x08, 
            0x49, 0xc3, 0xc5, 0x15, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 
            0x1e, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xae, 0x1a, 
            0x02, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 0x86, 0x0f, 0x08, 
            0x49, 0xc3, 0xc5, 0x16, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 
            0x1f, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 0x28, 0xae, 0x17, 
            0x02, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 0x86, 0x0c, 0x08, 
            0x49, 0xc3, 0xaa, 0xe4, 0x13, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 
            0xcd, 0x1c, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 0x28, 0xae, 
            0x18, 0x02, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 0x86, 0x0d, 
            0x08, 0x49, 0xc3, 0xaa, 0xe4, 0x14, 0xd0, 0xfe, 0x68, 0x49, 
            0x7d, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 0x28, 
            0xae, 0x19, 0x02, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 0x86, 
            0x0e, 0x08, 0x49, 0xc3, 0xaa, 0xe4, 0x15, 0xd0, 0xfe, 0x68, 
            0x49, 0x7d, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 
            0x28, 0xae, 0x1a, 0x02, 0x08, 0x8a, 0x49, 0xc3, 0xaa, 0x28, 
            0x86, 0x0f, 0x08, 0x49, 0xc3, 0xaa, 0xe4, 0x16, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 
            0x48, 0x28, 0xa2, 0xc3, 0x08, 0xec, 0x17, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xcd, 0x1c, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 
            0x48, 0x28, 0xa2, 0x82, 0x08, 0xec, 0x18, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 
            0x48, 0x28, 0xa2, 0x41, 0x08, 0xec, 0x19, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 
            0x48, 0x28, 0xa2, 0x00, 0x08, 0xec, 0x1a, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 
            0x48, 0x28, 0xa2, 0xc3, 0x08, 0xec, 0x17, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xcd, 0x1c, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 
            0x48, 0x28, 0xa2, 0x82, 0x08, 0xec, 0x18, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 
            0x48, 0x28, 0xa2, 0x41, 0x08, 0xec, 0x19, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 
            0x48, 0x28, 0xa2, 0x00, 0x08, 0xec, 0x1a, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 0xa2, 0x00, 
            0xa5, 0x0c, 0x49, 0xc3, 0xc5, 0x13, 0xd0, 0xfe, 0x86, 0x0c, 
            0xad, 0x03, 0x02, 0x49, 0xc3, 0xcd, 0x17, 0x02, 0xd0, 0xfe, 
            0x8e, 0x03, 0x02, 0xa5, 0x0d, 0x49, 0xc3, 0xc5, 0x14, 0xd0, 
            0xfe, 0x86, 0x0d, 0xad, 0x04, 0x02, 0x49, 0xc3, 0xcd, 0x18, 
            0x02, 0xd0, 0xfe, 0x8e, 0x04, 0x02, 0xa5, 0x0e, 0x49, 0xc3, 
            0xc5, 0x15, 0xd0, 0xfe, 0x86, 0x0e, 0xad, 0x05, 0x02, 0x49, 
            0xc3, 0xcd, 0x19, 0x02, 0xd0, 0xfe, 0x8e, 0x05, 0x02, 0xa5, 
            0x0f, 0x49, 0xc3, 0xc5, 0x16, 0xd0, 0xfe, 0x86, 0x0f, 0xad, 
            0x06, 0x02, 0x49, 0xc3, 0xcd, 0x1a, 0x02, 0xd0, 0xfe, 0x8e, 
            0x06, 0x02, 0xad, 0x00, 0x02, 0xc9, 0x13, 0xd0, 0xfe, 0xa9, 
            0x14, 0x8d, 0x00, 0x02, 0xa9, 0x00, 0x48, 0x28, 0xa4, 0x13, 
            0x08, 0x98, 0x49, 0xc3, 0xa8, 0x28, 0x8c, 0x03, 0x02, 0x08, 
            0x49, 0xc3, 0xa8, 0xc0, 0xc3, 0xd0, 0xfe, 0x68, 0x49, 0x30, 
            0xcd, 0x1c, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xa4, 
            0x14, 0x08, 0x98, 0x49, 0xc3, 0xa8, 0x28, 0x8c, 0x04, 0x02, 
            0x08, 0x49, 0xc3, 0xa8, 0xc0, 0x82, 0xd0, 0xfe, 0x68, 0x49, 
            0x30, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 
            0xa4, 0x15, 0x08, 0x98, 0x49, 0xc3, 0xa8, 0x28, 0x8c, 0x05, 
            0x02, 0x08, 0x49, 0xc3, 0xa8, 0xc0, 0x41, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 
            0x28, 0xa4, 0x16, 0x08, 0x98, 0x49, 0xc3, 0xa8, 0x28, 0x8c, 
            0x06, 0x02, 0x08, 0x49, 0xc3, 0xa8, 0xc0, 0x00, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 
            0x48, 0x28, 0xa4, 0x13, 0x08, 0x98, 0x49, 0xc3, 0xa8, 0x28, 
            0x8c, 0x03, 0x02, 0x08, 0x49, 0xc3, 0xa8, 0xc0, 0xc3, 0xd0, 
            0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1c, 0x02, 0xd0, 0xfe, 0xa9, 
            0xff, 0x48, 0x28, 0xa4, 0x14, 0x08, 0x98, 0x49, 0xc3, 0xa8, 
            0x28, 0x8c, 0x04, 0x02, 0x08, 0x49, 0xc3, 0xa8, 0xc0, 0x82, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 
            0xa9, 0xff, 0x48, 0x28, 0xa4, 0x15, 0x08, 0x98, 0x49, 0xc3, 
            0xa8, 0x28, 0x8c, 0x05, 0x02, 0x08, 0x49, 0xc3, 0xa8, 0xc0, 
            0x41, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1e, 0x02, 0xd0, 
            0xfe, 0xa9, 0xff, 0x48, 0x28, 0xa4, 0x16, 0x08, 0x98, 0x49, 
            0xc3, 0xa8, 0x28, 0x8c, 0x06, 0x02, 0x08, 0x49, 0xc3, 0xa8, 
            0xc0, 0x00, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1f, 0x02, 
            0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xac, 0x17, 0x02, 0x08, 
            0x98, 0x49, 0xc3, 0xa8, 0x28, 0x84, 0x0c, 0x08, 0x49, 0xc3, 
            0xa8, 0xc4, 0x13, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 0x1c, 
            0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xac, 0x18, 0x02, 
            0x08, 0x98, 0x49, 0xc3, 0xa8, 0x28, 0x84, 0x0d, 0x08, 0x49, 
            0xc3, 0xa8, 0xc4, 0x14, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 
            0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xac, 0x19, 
            0x02, 0x08, 0x98, 0x49, 0xc3, 0xa8, 0x28, 0x84, 0x0e, 0x08, 
            0x49, 0xc3, 0xa8, 0xc4, 0x15, 0xd0, 0xfe, 0x68, 0x49, 0x30, 
            0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xac, 
            0x1a, 0x02, 0x08, 0x98, 0x49, 0xc3, 0xa8, 0x28, 0x84, 0x0f, 
            0x08, 0x49, 0xc3, 0xa8, 0xc4, 0x16, 0xd0, 0xfe, 0x68, 0x49, 
            0x30, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 0x28, 
            0xac, 0x17, 0x02, 0x08, 0x98, 0x49, 0xc3, 0xa8, 0x28, 0x84, 
            0x0c, 0x08, 0x49, 0xc3, 0xa8, 0xc5, 0x13, 0xd0, 0xfe, 0x68, 
            0x49, 0x7d, 0xcd, 0x1c, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 
            0x28, 0xac, 0x18, 0x02, 0x08, 0x98, 0x49, 0xc3, 0xa8, 0x28, 
            0x84, 0x0d, 0x08, 0x49, 0xc3, 0xa8, 0xc5, 0x14, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 
            0x48, 0x28, 0xac, 0x19, 0x02, 0x08, 0x98, 0x49, 0xc3, 0xa8, 
            0x28, 0x84, 0x0e, 0x08, 0x49, 0xc3, 0xa8, 0xc5, 0x15, 0xd0, 
            0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 
            0xff, 0x48, 0x28, 0xac, 0x1a, 0x02, 0x08, 0x98, 0x49, 0xc3, 
            0xa8, 0x28, 0x84, 0x0f, 0x08, 0x49, 0xc3, 0xa8, 0xc5, 0x16, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 
            0xa9, 0x00, 0x48, 0x28, 0xa0, 0xc3, 0x08, 0xcc, 0x17, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 0x1c, 0x02, 0xd0, 0xfe, 
            0xa9, 0x00, 0x48, 0x28, 0xa0, 0x82, 0x08, 0xcc, 0x18, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 
            0xa9, 0x00, 0x48, 0x28, 0xa0, 0x41, 0x08, 0xcc, 0x19, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 
            0xa9, 0x00, 0x48, 0x28, 0xa0, 0x00, 0x08, 0xcc, 0x1a, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 
            0xa9, 0xff, 0x48, 0x28, 0xa0, 0xc3, 0x08, 0xcc, 0x17, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1c, 0x02, 0xd0, 0xfe, 
            0xa9, 0xff, 0x48, 0x28, 0xa0, 0x82, 0x08, 0xcc, 0x18, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 
            0xa9, 0xff, 0x48, 0x28, 0xa0, 0x41, 0x08, 0xcc, 0x19, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 
            0xa9, 0xff, 0x48, 0x28, 0xa0, 0x00, 0x08, 0xcc, 0x1a, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 
            0xa0, 0x00, 0xa5, 0x0c, 0x49, 0xc3, 0xc5, 0x13, 0xd0, 0xfe, 
            0x84, 0x0c, 0xad, 0x03, 0x02, 0x49, 0xc3, 0xcd, 0x17, 0x02, 
            0xd0, 0xfe, 0x8c, 0x03, 0x02, 0xa5, 0x0d, 0x49, 0xc3, 0xc5, 
            0x14, 0xd0, 0xfe, 0x84, 0x0d, 0xad, 0x04, 0x02, 0x49, 0xc3, 
            0xcd, 0x18, 0x02, 0xd0, 0xfe, 0x8c, 0x04, 0x02, 0xa5, 0x0e, 
            0x49, 0xc3, 0xc5, 0x15, 0xd0, 0xfe, 0x84, 0x0e, 0xad, 0x05, 
            0x02, 0x49, 0xc3, 0xcd, 0x19, 0x02, 0xd0, 0xfe, 0x8c, 0x05, 
            0x02, 0xa5, 0x0f, 0x49, 0xc3, 0xc5, 0x16, 0xd0, 0xfe, 0x84, 
            0x0f, 0xad, 0x06, 0x02, 0x49, 0xc3, 0xcd, 0x1a, 0x02, 0xd0, 
            0xfe, 0x8c, 0x06, 0x02, 0xad, 0x00, 0x02, 0xc9, 0x14, 0xd0, 
            0xfe, 0xa9, 0x15, 0x8d, 0x00, 0x02, 0xa2, 0x03, 0xa9, 0x00, 
            0x48, 0x28, 0xb5, 0x13, 0x08, 0x49, 0xc3, 0x28, 0x9d, 0x03, 
            0x02, 0x08, 0x49, 0xc3, 0xdd, 0x17, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xdd, 0x1c, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 
            0xa2, 0x03, 0xa9, 0xff, 0x48, 0x28, 0xb5, 0x13, 0x08, 0x49, 
            0xc3, 0x28, 0x9d, 0x03, 0x02, 0x08, 0x49, 0xc3, 0xdd, 0x17, 
            0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 0x1c, 0x02, 0xd0, 
            0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 0x00, 0x48, 0x28, 
            0xbd, 0x17, 0x02, 0x08, 0x49, 0xc3, 0x28, 0x95, 0x0c, 0x08, 
            0x49, 0xc3, 0xd5, 0x13, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 
            0x1c, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe1, 0xa2, 0x03, 0xa9, 
            0xff, 0x48, 0x28, 0xbd, 0x17, 0x02, 0x08, 0x49, 0xc3, 0x28, 
            0x95, 0x0c, 0x08, 0x49, 0xc3, 0xd5, 0x13, 0xd0, 0xfe, 0x68, 
            0x49, 0x7d, 0xdd, 0x1c, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe1, 
            0xa2, 0x03, 0xa0, 0x00, 0xb5, 0x0c, 0x49, 0xc3, 0xd5, 0x13, 
            0xd0, 0xfe, 0x94, 0x0c, 0xbd, 0x03, 0x02, 0x49, 0xc3, 0xdd, 
            0x17, 0x02, 0xd0, 0xfe, 0x8a, 0x9d, 0x03, 0x02, 0xca, 0x10, 
            0xe5, 0xad, 0x00, 0x02, 0xc9, 0x15, 0xd0, 0xfe, 0xa9, 0x16, 
            0x8d, 0x00, 0x02, 0xa0, 0x03, 0xa9, 0x00, 0x48, 0x28, 0xb1, 
            0x24, 0x08, 0x49, 0xc3, 0x28, 0x99, 0x03, 0x02, 0x08, 0x49, 
            0xc3, 0xd9, 0x17, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xd9, 
            0x1c, 0x02, 0xd0, 0xfe, 0x88, 0x10, 0xe0, 0xa0, 0x03, 0xa9, 
            0xff, 0x48, 0x28, 0xb1, 0x24, 0x08, 0x49, 0xc3, 0x28, 0x99, 
            0x03, 0x02, 0x08, 0x49, 0xc3, 0xd9, 0x17, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xd9, 0x1c, 0x02, 0xd0, 0xfe, 0x88, 0x10, 
            0xe0, 0xa0, 0x03, 0xa2, 0x00, 0xb9, 0x03, 0x02, 0x49, 0xc3, 
            0xd9, 0x17, 0x02, 0xd0, 0xfe, 0x8a, 0x99, 0x03, 0x02, 0x88, 
            0x10, 0xef, 0xa0, 0x03, 0xa9, 0x00, 0x48, 0x28, 0xb9, 0x17, 
            0x02, 0x08, 0x49, 0xc3, 0x28, 0x91, 0x30, 0x08, 0x49, 0xc3
        };
        return mem;
    }
    
    public static int[] getMem2() {        
        int mem[] = {
            0xd1, 0x24, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xd9, 0x1c, 0x02, 
            0xd0, 0xfe, 0x88, 0x10, 0xe1, 0xa0, 0x03, 0xa9, 0xff, 0x48, 
            0x28, 0xb9, 0x17, 0x02, 0x08, 0x49, 0xc3, 0x28, 0x91, 0x30, 
            0x08, 0x49, 0xc3, 0xd1, 0x24, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 
            0xd9, 0x1c, 0x02, 0xd0, 0xfe, 0x88, 0x10, 0xe1, 0xa0, 0x03, 
            0xa2, 0x00, 0xb9, 0x03, 0x02, 0x49, 0xc3, 0xd9, 0x17, 0x02, 
            0xd0, 0xfe, 0x8a, 0x99, 0x03, 0x02, 0x88, 0x10, 0xef, 0xa2, 
            0x06, 0xa0, 0x03, 0xa9, 0x00, 0x48, 0x28, 0xa1, 0x24, 0x08, 
            0x49, 0xc3, 0x28, 0x81, 0x30, 0x08, 0x49, 0xc3, 0xd9, 0x17, 
            0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xd9, 0x1c, 0x02, 0xd0, 
            0xfe, 0xca, 0xca, 0x88, 0x10, 0xdf, 0xa2, 0x06, 0xa0, 0x03, 
            0xa9, 0xff, 0x48, 0x28, 0xa1, 0x24, 0x08, 0x49, 0xc3, 0x28, 
            0x81, 0x30, 0x08, 0x49, 0xc3, 0xd9, 0x17, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xd9, 0x1c, 0x02, 0xd0, 0xfe, 0xca, 0xca, 
            0x88, 0x10, 0xdf, 0xa0, 0x03, 0xa2, 0x00, 0xb9, 0x03, 0x02, 
            0x49, 0xc3, 0xd9, 0x17, 0x02, 0xd0, 0xfe, 0x8a, 0x99, 0x03, 
            0x02, 0x88, 0x10, 0xef, 0xad, 0x00, 0x02, 0xc9, 0x16, 0xd0, 
            0xfe, 0xa9, 0x17, 0x8d, 0x00, 0x02, 0xa2, 0xfd, 0xb5, 0x19, 
            0x9d, 0x09, 0x01, 0xca, 0xe0, 0xfa, 0xb0, 0xf6, 0xa2, 0xfd, 
            0xbd, 0x1d, 0x01, 0x95, 0x12, 0xca, 0xe0, 0xfa, 0xb0, 0xf6, 
            0xa2, 0x03, 0xa0, 0x00, 0xb5, 0x0c, 0xd5, 0x13, 0xd0, 0xfe, 
            0x94, 0x0c, 0xbd, 0x03, 0x02, 0xdd, 0x17, 0x02, 0xd0, 0xfe, 
            0x8a, 0x9d, 0x03, 0x02, 0xca, 0x10, 0xe9, 0xa0, 0xfb, 0xa2, 
            0xfe, 0xa1, 0x2c, 0x99, 0x0b, 0x01, 0xca, 0xca, 0x88, 0xc0, 
            0xf8, 0xb0, 0xf4, 0xa0, 0x03, 0xa2, 0x00, 0xb9, 0x03, 0x02, 
            0xd9, 0x17, 0x02, 0xd0, 0xfe, 0x8a, 0x99, 0x03, 0x02, 0x88, 
            0x10, 0xf1, 0xa0, 0xfb, 0xb9, 0x1f, 0x01, 0x91, 0x38, 0x88, 
            0xc0, 0xf8, 0xb0, 0xf6, 0xa0, 0x03, 0xa2, 0x00, 0xb9, 0x03, 
            0x02, 0xd9, 0x17, 0x02, 0xd0, 0xfe, 0x8a, 0x99, 0x03, 0x02, 
            0x88, 0x10, 0xf1, 0xa0, 0xfb, 0xa2, 0xfe, 0xb1, 0x2e, 0x81, 
            0x38, 0xca, 0xca, 0x88, 0xc0, 0xf8, 0xb0, 0xf5, 0xa0, 0x03, 
            0xa2, 0x00, 0xb9, 0x03, 0x02, 0xd9, 0x17, 0x02, 0xd0, 0xfe, 
            0x8a, 0x99, 0x03, 0x02, 0x88, 0x10, 0xf1, 0xad, 0x00, 0x02, 
            0xc9, 0x17, 0xd0, 0xfe, 0xa9, 0x18, 0x8d, 0x00, 0x02, 0xa9, 
            0x00, 0x48, 0x28, 0xa5, 0x13, 0x08, 0x49, 0xc3, 0x28, 0x8d, 
            0x03, 0x02, 0x08, 0x49, 0xc3, 0xc9, 0xc3, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xcd, 0x1c, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 
            0x28, 0xa5, 0x14, 0x08, 0x49, 0xc3, 0x28, 0x8d, 0x04, 0x02, 
            0x08, 0x49, 0xc3, 0xc9, 0x82, 0xd0, 0xfe, 0x68, 0x49, 0x30, 
            0xcd, 0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xa5, 
            0x15, 0x08, 0x49, 0xc3, 0x28, 0x8d, 0x05, 0x02, 0x08, 0x49, 
            0xc3, 0xc9, 0x41, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 0x1e, 
            0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xa5, 0x16, 0x08, 
            0x49, 0xc3, 0x28, 0x8d, 0x06, 0x02, 0x08, 0x49, 0xc3, 0xc9, 
            0x00, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 0x1f, 0x02, 0xd0, 
            0xfe, 0xa9, 0xff, 0x48, 0x28, 0xa5, 0x13, 0x08, 0x49, 0xc3, 
            0x28, 0x8d, 0x03, 0x02, 0x08, 0x49, 0xc3, 0xc9, 0xc3, 0xd0, 
            0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1c, 0x02, 0xd0, 0xfe, 0xa9, 
            0xff, 0x48, 0x28, 0xa5, 0x14, 0x08, 0x49, 0xc3, 0x28, 0x8d, 
            0x04, 0x02, 0x08, 0x49, 0xc3, 0xc9, 0x82, 0xd0, 0xfe, 0x68, 
            0x49, 0x7d, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 
            0x28, 0xa5, 0x15, 0x08, 0x49, 0xc3, 0x28, 0x8d, 0x05, 0x02, 
            0x08, 0x49, 0xc3, 0xc9, 0x41, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 
            0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 0x28, 0xa5, 
            0x16, 0x08, 0x49, 0xc3, 0x28, 0x8d, 0x06, 0x02, 0x08, 0x49, 
            0xc3, 0xc9, 0x00, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1f, 
            0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 0x28, 0xad, 0x17, 0x02, 
            0x08, 0x49, 0xc3, 0x28, 0x85, 0x0c, 0x08, 0x49, 0xc3, 0xc5, 
            0x13, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xcd, 0x1c, 0x02, 0xd0, 
            0xfe, 0xa9, 0x00, 0x48, 0x28, 0xad, 0x18, 0x02, 0x08, 0x49, 
            0xc3, 0x28, 0x85, 0x0d, 0x08, 0x49, 0xc3, 0xc5, 0x14, 0xd0, 
            0xfe, 0x68, 0x49, 0x30, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 0xa9, 
            0x00, 0x48, 0x28, 0xad, 0x19, 0x02, 0x08, 0x49, 0xc3, 0x28, 
            0x85, 0x0e, 0x08, 0x49, 0xc3, 0xc5, 0x15, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 
            0x28, 0xad, 0x1a, 0x02, 0x08, 0x49, 0xc3, 0x28, 0x85, 0x0f, 
            0x08, 0x49, 0xc3, 0xc5, 0x16, 0xd0, 0xfe, 0x68, 0x49, 0x30, 
            0xcd, 0x1f, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 0x28, 0xad, 
            0x17, 0x02, 0x08, 0x49, 0xc3, 0x28, 0x85, 0x0c, 0x08, 0x49, 
            0xc3, 0xc5, 0x13, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1c, 
            0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 0x28, 0xad, 0x18, 0x02, 
            0x08, 0x49, 0xc3, 0x28, 0x85, 0x0d, 0x08, 0x49, 0xc3, 0xc5, 
            0x14, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1d, 0x02, 0xd0, 
            0xfe, 0xa9, 0xff, 0x48, 0x28, 0xad, 0x19, 0x02, 0x08, 0x49, 
            0xc3, 0x28, 0x85, 0x0e, 0x08, 0x49, 0xc3, 0xc5, 0x15, 0xd0, 
            0xfe, 0x68, 0x49, 0x7d, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 
            0xff, 0x48, 0x28, 0xad, 0x1a, 0x02, 0x08, 0x49, 0xc3, 0x28, 
            0x85, 0x0f, 0x08, 0x49, 0xc3, 0xc5, 0x16, 0xd0, 0xfe, 0x68, 
            0x49, 0x7d, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 
            0x28, 0xa9, 0xc3, 0x08, 0xcd, 0x17, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xcd, 0x1c, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 
            0x28, 0xa9, 0x82, 0x08, 0xcd, 0x18, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 
            0x28, 0xa9, 0x41, 0x08, 0xcd, 0x19, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 0x00, 0x48, 
            0x28, 0xa9, 0x00, 0x08, 0xcd, 0x1a, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 
            0x28, 0xa9, 0xc3, 0x08, 0xcd, 0x17, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x7d, 0xcd, 0x1c, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 
            0x28, 0xa9, 0x82, 0x08, 0xcd, 0x18, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x7d, 0xcd, 0x1d, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 
            0x28, 0xa9, 0x41, 0x08, 0xcd, 0x19, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x7d, 0xcd, 0x1e, 0x02, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 
            0x28, 0xa9, 0x00, 0x08, 0xcd, 0x1a, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x7d, 0xcd, 0x1f, 0x02, 0xd0, 0xfe, 0xa2, 0x00, 0xa5, 
            0x0c, 0x49, 0xc3, 0xc5, 0x13, 0xd0, 0xfe, 0x86, 0x0c, 0xad, 
            0x03, 0x02, 0x49, 0xc3, 0xcd, 0x17, 0x02, 0xd0, 0xfe, 0x8e, 
            0x03, 0x02, 0xa5, 0x0d, 0x49, 0xc3, 0xc5, 0x14, 0xd0, 0xfe, 
            0x86, 0x0d, 0xad, 0x04, 0x02, 0x49, 0xc3, 0xcd, 0x18, 0x02, 
            0xd0, 0xfe, 0x8e, 0x04, 0x02, 0xa5, 0x0e, 0x49, 0xc3, 0xc5, 
            0x15, 0xd0, 0xfe, 0x86, 0x0e, 0xad, 0x05, 0x02, 0x49, 0xc3, 
            0xcd, 0x19, 0x02, 0xd0, 0xfe, 0x8e, 0x05, 0x02, 0xa5, 0x0f, 
            0x49, 0xc3, 0xc5, 0x16, 0xd0, 0xfe, 0x86, 0x0f, 0xad, 0x06, 
            0x02, 0x49, 0xc3, 0xcd, 0x1a, 0x02, 0xd0, 0xfe, 0x8e, 0x06, 
            0x02, 0xad, 0x00, 0x02, 0xc9, 0x18, 0xd0, 0xfe, 0xa9, 0x19, 
            0x8d, 0x00, 0x02, 0xa9, 0x00, 0x48, 0xa9, 0xff, 0x28, 0x24, 
            0x16, 0x08, 0xc9, 0xff, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x32, 
            0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x01, 0x28, 0x24, 
            0x15, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x70, 
            0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x01, 0x28, 0x24, 
            0x14, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xb2, 
            0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x01, 0x28, 0x24, 
            0x13, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xf0, 
            0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0xff, 0x28, 0x24, 
            0x16, 0x08, 0xc9, 0xff, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x3f, 
            0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0x01, 0x28, 0x24, 
            0x15, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7d, 
            0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0x01, 0x28, 0x24, 
            0x14, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xbf, 
            0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0x01, 0x28, 0x24, 
            0x13, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xfd, 
            0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0xff, 0x28, 0x2c, 
            0x1a, 0x02, 0x08, 0xc9, 0xff, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 
            0x32, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x01, 0x28, 
            0x2c, 0x19, 0x02, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0x70, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x01, 
            0x28, 0x2c, 0x18, 0x02, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 0x68, 
            0x48, 0xc9, 0xb2, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 
            0x01, 0x28, 0x2c, 0x17, 0x02, 0x08, 0xc9, 0x01, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xf0, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 
            0xa9, 0xff, 0x28, 0x2c, 0x1a, 0x02, 0x08, 0xc9, 0xff, 0xd0, 
            0xfe, 0x68, 0x48, 0xc9, 0x3f, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 
            0x48, 0xa9, 0x01, 0x28, 0x2c, 0x19, 0x02, 0x08, 0xc9, 0x01, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xa9, 
            0xff, 0x48, 0xa9, 0x01, 0x28, 0x2c, 0x18, 0x02, 0x08, 0xc9, 
            0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xbf, 0xd0, 0xfe, 0x28, 
            0xa9, 0xff, 0x48, 0xa9, 0x01, 0x28, 0x2c, 0x17, 0x02, 0x08, 
            0xc9, 0x01, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xfd, 0xd0, 0xfe, 
            0x28, 0xad, 0x00, 0x02, 0xc9, 0x19, 0xd0, 0xfe, 0xa9, 0x1a, 
            0x8d, 0x00, 0x02, 0xa9, 0x00, 0x48, 0xa2, 0x80, 0x28, 0xe4, 
            0x17, 0x08, 0x68, 0x48, 0xc9, 0x31, 0xd0, 0xfe, 0x28, 0xca, 
            0xe4, 0x17, 0x08, 0x68, 0x48, 0xc9, 0x33, 0xd0, 0xfe, 0x28, 
            0xca, 0xe4, 0x17, 0x08, 0xe0, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa2, 0x80, 
            0x28, 0xe4, 0x17, 0x08, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 
            0x28, 0xca, 0xe4, 0x17, 0x08, 0x68, 0x48, 0xc9, 0x7f, 0xd0, 
            0xfe, 0x28, 0xca, 0xe4, 0x17, 0x08, 0xe0, 0x7e, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 
            0xa2, 0x80, 0x28, 0xec, 0x1b, 0x02, 0x08, 0x68, 0x48, 0xc9, 
            0x31, 0xd0, 0xfe, 0x28, 0xca, 0xec, 0x1b, 0x02, 0x08, 0x68, 
            0x48, 0xc9, 0x33, 0xd0, 0xfe, 0x28, 0xca, 0xec, 0x1b, 0x02, 
            0x08, 0xe0, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xb0, 0xd0, 
            0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa2, 0x80, 0x28, 0xec, 0x1b, 
            0x02, 0x08, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xca, 
            0xec, 0x1b, 0x02, 0x08, 0x68, 0x48, 0xc9, 0x7f, 0xd0, 0xfe, 
            0x28, 0xca, 0xec, 0x1b, 0x02, 0x08, 0xe0, 0x7e, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 
            0xa2, 0x80, 0x28, 0xe0, 0x7f, 0x08, 0x68, 0x48, 0xc9, 0x31, 
            0xd0, 0xfe, 0x28, 0xca, 0xe0, 0x7f, 0x08, 0x68, 0x48, 0xc9, 
            0x33, 0xd0, 0xfe, 0x28, 0xca, 0xe0, 0x7f, 0x08, 0xe0, 0x7e, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa9, 
            0xff, 0x48, 0xa2, 0x80, 0x28, 0xe0, 0x7f, 0x08, 0x68, 0x48, 
            0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xca, 0xe0, 0x7f, 0x08, 0x68, 
            0x48, 0xc9, 0x7f, 0xd0, 0xfe, 0x28, 0xca, 0xe0, 0x7f, 0x08, 
            0xe0, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 
            0x28, 0xad, 0x00, 0x02, 0xc9, 0x1a, 0xd0, 0xfe, 0xa9, 0x1b, 
            0x8d, 0x00, 0x02, 0xa9, 0x00, 0x48, 0xa0, 0x80, 0x28, 0xc4, 
            0x17, 0x08, 0x68, 0x48, 0xc9, 0x31, 0xd0, 0xfe, 0x28, 0x88, 
            0xc4, 0x17, 0x08, 0x68, 0x48, 0xc9, 0x33, 0xd0, 0xfe, 0x28, 
            0x88, 0xc4, 0x17, 0x08, 0xc0, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa0, 0x80, 
            0x28, 0xc4, 0x17, 0x08, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 
            0x28, 0x88, 0xc4, 0x17, 0x08, 0x68, 0x48, 0xc9, 0x7f, 0xd0, 
            0xfe, 0x28, 0x88, 0xc4, 0x17, 0x08, 0xc0, 0x7e, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 
            0xa0, 0x80, 0x28, 0xcc, 0x1b, 0x02, 0x08, 0x68, 0x48, 0xc9, 
            0x31, 0xd0, 0xfe, 0x28, 0x88, 0xcc, 0x1b, 0x02, 0x08, 0x68, 
            0x48, 0xc9, 0x33, 0xd0, 0xfe, 0x28, 0x88, 0xcc, 0x1b, 0x02, 
            0x08, 0xc0, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xb0, 0xd0, 
            0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa0, 0x80, 0x28, 0xcc, 0x1b, 
            0x02, 0x08, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0x88, 
            0xcc, 0x1b, 0x02, 0x08, 0x68, 0x48, 0xc9, 0x7f, 0xd0, 0xfe, 
            0x28, 0x88, 0xcc, 0x1b, 0x02, 0x08, 0xc0, 0x7e, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 
            0xa0, 0x80, 0x28, 0xc0, 0x7f, 0x08, 0x68, 0x48, 0xc9, 0x31, 
            0xd0, 0xfe, 0x28, 0x88, 0xc0, 0x7f, 0x08, 0x68, 0x48, 0xc9, 
            0x33, 0xd0, 0xfe, 0x28, 0x88, 0xc0, 0x7f, 0x08, 0xc0, 0x7e, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa9, 
            0xff, 0x48, 0xa0, 0x80, 0x28, 0xc0, 0x7f, 0x08, 0x68, 0x48, 
            0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0x88, 0xc0, 0x7f, 0x08, 0x68, 
            0x48, 0xc9, 0x7f, 0xd0, 0xfe, 0x28, 0x88, 0xc0, 0x7f, 0x08, 
            0xc0, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 
            0x28, 0xad, 0x00, 0x02, 0xc9, 0x1b, 0xd0, 0xfe, 0xa9, 0x1c, 
            0x8d, 0x00, 0x02, 0xa9, 0x00, 0x48, 0xa9, 0x80, 0x28, 0xc5, 
            0x17, 0x08, 0xc9, 0x80, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x31, 
            0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x7f, 0x28, 0xc5, 
            0x17, 0x08, 0xc9, 0x7f, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x33, 
            0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x7e, 0x28, 0xc5, 
            0x17, 0x08, 0xc9, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xb0, 
            0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0x80, 0x28, 0xc5, 
            0x17, 0x08, 0xc9, 0x80, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7d, 
            0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0x7f, 0x28, 0xc5, 
            0x17, 0x08, 0xc9, 0x7f, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7f, 
            0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0x7e, 0x28, 0xc5, 
            0x17, 0x08, 0xc9, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xfc, 
            0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x80, 0x28, 0xcd, 
            0x1b, 0x02, 0x08, 0xc9, 0x80, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 
            0x31, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x7f, 0x28, 
            0xcd, 0x1b, 0x02, 0x08, 0xc9, 0x7f, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0x33, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x7e, 
            0x28, 0xcd, 0x1b, 0x02, 0x08, 0xc9, 0x7e, 0xd0, 0xfe, 0x68, 
            0x48, 0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 
            0x80, 0x28, 0xcd, 0x1b, 0x02, 0x08, 0xc9, 0x80, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 
            0xa9, 0x7f, 0x28, 0xcd, 0x1b, 0x02, 0x08, 0xc9, 0x7f, 0xd0, 
            0xfe, 0x68, 0x48, 0xc9, 0x7f, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 
            0x48, 0xa9, 0x7e, 0x28, 0xcd, 0x1b, 0x02, 0x08, 0xc9, 0x7e, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 0x28, 0xa9, 
            0x00, 0x48, 0xa9, 0x80, 0x28, 0xc9, 0x7f, 0x08, 0xc9, 0x80, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x31, 0xd0, 0xfe, 0x28, 0xa9, 
            0x00, 0x48, 0xa9, 0x7f, 0x28, 0xc9, 0x7f, 0x08, 0xc9, 0x7f, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x33, 0xd0, 0xfe, 0x28, 0xa9, 
            0x00, 0x48, 0xa9, 0x7e, 0x28, 0xc9, 0x7f, 0x08, 0xc9, 0x7e, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa9, 
            0xff, 0x48, 0xa9, 0x80, 0x28, 0xc9, 0x7f, 0x08, 0xc9, 0x80, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xa9, 
            0xff, 0x48, 0xa9, 0x7f, 0x28, 0xc9, 0x7f, 0x08, 0xc9, 0x7f, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7f, 0xd0, 0xfe, 0x28, 0xa9, 
            0xff, 0x48, 0xa9, 0x7e, 0x28, 0xc9, 0x7f, 0x08, 0xc9, 0x7e, 
            0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 0x28, 0xa2, 
            0x04, 0xa9, 0x00, 0x48, 0xa9, 0x80, 0x28, 0xd5, 0x13, 0x08, 
            0xc9, 0x80, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x31, 0xd0, 0xfe, 
            0x28, 0xa9, 0x00, 0x48, 0xa9, 0x7f, 0x28, 0xd5, 0x13, 0x08, 
            0xc9, 0x7f, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x33, 0xd0, 0xfe, 
            0x28, 0xa9, 0x00, 0x48, 0xa9, 0x7e, 0x28, 0xd5, 0x13, 0x08, 
            0xc9, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xb0, 0xd0, 0xfe, 
            0x28, 0xa9, 0xff, 0x48, 0xa9, 0x80, 0x28, 0xd5, 0x13, 0x08, 
            0xc9, 0x80, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 
            0x28, 0xa9, 0xff, 0x48, 0xa9, 0x7f, 0x28, 0xd5, 0x13, 0x08, 
            0xc9, 0x7f, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x7f, 0xd0, 0xfe, 
            0x28, 0xa9, 0xff, 0x48, 0xa9, 0x7e, 0x28, 0xd5, 0x13, 0x08, 
            0xc9, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 
            0x28, 0xa9, 0x00, 0x48, 0xa9, 0x80, 0x28, 0xdd, 0x17, 0x02, 
            0x08, 0xc9, 0x80, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x31, 0xd0, 
            0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x7f, 0x28, 0xdd, 0x17, 
            0x02, 0x08, 0xc9, 0x7f, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x33, 
            0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x7e, 0x28, 0xdd, 
            0x17, 0x02, 0x08, 0xc9, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 
            0xb0, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0x80, 0x28, 
            0xdd, 0x17, 0x02, 0x08, 0xc9, 0x80, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0x7f, 
            0x28, 0xdd, 0x17, 0x02, 0x08, 0xc9, 0x7f, 0xd0, 0xfe, 0x68, 
            0x48, 0xc9, 0x7f, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 
            0x7e, 0x28, 0xdd, 0x17, 0x02, 0x08, 0xc9, 0x7e, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 0x28, 0xa0, 0x04, 0xa2, 
            0x08, 0xa9, 0x00, 0x48, 0xa9, 0x80, 0x28, 0xd9, 0x17, 0x02, 
            0x08, 0xc9, 0x80, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x31, 0xd0, 
            0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x7f, 0x28, 0xd9, 0x17, 
            0x02, 0x08, 0xc9, 0x7f, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 0x33, 
            0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 0xa9, 0x7e, 0x28, 0xd9, 
            0x17, 0x02, 0x08, 0xc9, 0x7e, 0xd0, 0xfe, 0x68, 0x48, 0xc9, 
            0xb0, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0x80, 0x28, 
            0xd9, 0x17, 0x02, 0x08, 0xc9, 0x80, 0xd0, 0xfe, 0x68, 0x48, 
            0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 0x7f, 
            0x28, 0xd9, 0x17, 0x02, 0x08, 0xc9, 0x7f, 0xd0, 0xfe, 0x68, 
            0x48, 0xc9, 0x7f, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 0xa9, 
            0x7e, 0x28, 0xd9, 0x17, 0x02, 0x08, 0xc9, 0x7e, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 
            0xa9, 0x80, 0x28, 0xc1, 0x24, 0x08, 0xc9, 0x80, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x31, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 
            0xa9, 0x7f, 0x28, 0xc1, 0x24, 0x08, 0xc9, 0x7f, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x33, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 
            0xa9, 0x7e, 0x28, 0xc1, 0x24, 0x08, 0xc9, 0x7e, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 
            0xa9, 0x80, 0x28, 0xc1, 0x24, 0x08, 0xc9, 0x80, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 
            0xa9, 0x7f, 0x28, 0xc1, 0x24, 0x08, 0xc9, 0x7f, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x7f, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 
            0xa9, 0x7e, 0x28, 0xc1, 0x24, 0x08, 0xc9, 0x7e, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 
            0xa9, 0x80, 0x28, 0xd1, 0x24, 0x08, 0xc9, 0x80, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x31, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 
            0xa9, 0x7f, 0x28, 0xd1, 0x24, 0x08, 0xc9, 0x7f, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x33, 0xd0, 0xfe, 0x28, 0xa9, 0x00, 0x48, 
            0xa9, 0x7e, 0x28, 0xd1, 0x24, 0x08, 0xc9, 0x7e, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xb0, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 
            0xa9, 0x80, 0x28, 0xd1, 0x24, 0x08, 0xc9, 0x80, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x7d, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 
            0xa9, 0x7f, 0x28, 0xd1, 0x24, 0x08, 0xc9, 0x7f, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0x7f, 0xd0, 0xfe, 0x28, 0xa9, 0xff, 0x48, 
            0xa9, 0x7e, 0x28, 0xd1, 0x24, 0x08, 0xc9, 0x7e, 0xd0, 0xfe, 
            0x68, 0x48, 0xc9, 0xfc, 0xd0, 0xfe, 0x28, 0xad, 0x00, 0x02, 
            0xc9, 0x1c, 0xd0, 0xfe, 0xa9, 0x1d, 0x8d, 0x00, 0x02, 0xa2, 
            0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 0x28, 0x0a, 0x08, 0xdd, 
            0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x30, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe8, 0xa2, 0x03, 0xa9, 0xff, 0x48, 
            0xb5, 0x13, 0x28, 0x0a, 0x08, 0xdd, 0x20, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7c, 0xdd, 0x30, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe8, 0xa2, 0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 0x28, 0x4a, 
            0x08, 0xdd, 0x28, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 
            0x38, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe8, 0xa2, 0x03, 0xa9, 
            0xff, 0x48, 0xb5, 0x13, 0x28, 0x4a, 0x08, 0xdd, 0x28, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe8, 0xa2, 0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 
            0x28, 0x2a, 0x08, 0xdd, 0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x30, 0xdd, 0x30, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe8, 0xa2, 
            0x03, 0xa9, 0xfe, 0x48, 0xb5, 0x13, 0x28, 0x2a, 0x08, 0xdd, 
            0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x30, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe8, 0xa2, 0x03, 0xa9, 0x01, 0x48, 
            0xb5, 0x13, 0x28, 0x2a, 0x08, 0xdd, 0x24, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xdd, 0x34, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe8, 0xa2, 0x03, 0xa9, 0xff, 0x48, 0xb5, 0x13, 0x28, 0x2a, 
            0x08, 0xdd, 0x24, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 
            0x34, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe8, 0xa2, 0x03, 0xa9, 
            0x00, 0x48, 0xb5, 0x13, 0x28, 0x6a, 0x08, 0xdd, 0x28, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe8, 0xa2, 0x03, 0xa9, 0xfe, 0x48, 0xb5, 0x13, 
            0x28, 0x6a, 0x08, 0xdd, 0x28, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7c, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe8, 0xa2, 
            0x03, 0xa9, 0x01, 0x48, 0xb5, 0x13, 0x28, 0x6a, 0x08, 0xdd, 
            0x2c, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x3c, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe8, 0xa2, 0x03, 0xa9, 0xff, 0x48, 
            0xb5, 0x13, 0x28, 0x6a, 0x08, 0xdd, 0x2c, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7c, 0xdd, 0x3c, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe8, 0xad, 0x00, 0x02, 0xc9, 0x1d, 0xd0, 0xfe, 0xa9, 0x1e, 
            0x8d, 0x00, 0x02, 0xa2, 0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 
            0x85, 0x0c, 0x28, 0x06, 0x0c, 0x08, 0xa5, 0x0c, 0xdd, 0x20, 
            0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x30, 0x02, 0xd0, 
            0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 0xa9, 0xff, 0x48, 0xb5, 
            0x13, 0x85, 0x0c, 0x28, 0x06, 0x0c, 0x08, 0xa5, 0x0c, 0xdd, 
            0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x30, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 0xa9, 0x00, 0x48, 
            0xb5, 0x13, 0x85, 0x0c, 0x28, 0x46, 0x0c, 0x08, 0xa5, 0x0c, 
            0xdd, 0x28, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x38, 
            0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 0xa9, 0xff, 
            0x48, 0xb5, 0x13, 0x85, 0x0c, 0x28, 0x46, 0x0c, 0x08, 0xa5, 
            0x0c, 0xdd, 0x28, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 
            0x38, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 0xa9, 
            0x00, 0x48, 0xb5, 0x13, 0x85, 0x0c, 0x28, 0x26, 0x0c, 0x08, 
            0xa5, 0x0c, 0xdd, 0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 
            0xdd, 0x30, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 
            0xa9, 0xfe, 0x48, 0xb5, 0x13, 0x85, 0x0c, 0x28, 0x26, 0x0c, 
            0x08, 0xa5, 0x0c, 0xdd, 0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7c, 0xdd, 0x30, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xa2, 
            0x03, 0xa9, 0x01, 0x48, 0xb5, 0x13, 0x85, 0x0c, 0x28, 0x26, 
            0x0c, 0x08, 0xa5, 0x0c, 0xdd, 0x24, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xdd, 0x34, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 
            0xa2, 0x03, 0xa9, 0xff, 0x48, 0xb5, 0x13, 0x85, 0x0c, 0x28, 
            0x26, 0x0c, 0x08, 0xa5, 0x0c, 0xdd, 0x24, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7c, 0xdd, 0x34, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe3, 0xa2, 0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 0x85, 0x0c, 
            0x28, 0x66, 0x0c, 0x08, 0xa5, 0x0c, 0xdd, 0x28, 0x02, 0xd0, 
            0xfe, 0x68, 0x49, 0x30, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 0xca, 
            0x10, 0xe3, 0xa2, 0x03, 0xa9, 0xfe, 0x48, 0xb5, 0x13, 0x85, 
            0x0c, 0x28, 0x66, 0x0c, 0x08, 0xa5, 0x0c, 0xdd, 0x28, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe3, 0xa2, 0x03, 0xa9, 0x01, 0x48, 0xb5, 0x13, 
            0x85, 0x0c, 0x28, 0x66, 0x0c, 0x08, 0xa5, 0x0c, 0xdd, 0x2c, 
            0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x3c, 0x02, 0xd0, 
            0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 0xa9, 0xff, 0x48, 0xb5, 
            0x13, 0x85, 0x0c, 0x28, 0x66, 0x0c, 0x08, 0xa5, 0x0c, 0xdd, 
            0x2c, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x3c, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xad, 0x00, 0x02, 0xc9, 0x1e, 
            0xd0, 0xfe, 0xa9, 0x1f, 0x8d, 0x00, 0x02, 0xa2, 0x03, 0xa9, 
            0x00, 0x48, 0xb5, 0x13, 0x8d, 0x03, 0x02, 0x28, 0x0e, 0x03, 
            0x02, 0x08, 0xad, 0x03, 0x02, 0xdd, 0x20, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xdd, 0x30, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe0, 0xa2, 0x03, 0xa9, 0xff, 0x48, 0xb5, 0x13, 0x8d, 0x03, 
            0x02, 0x28, 0x0e, 0x03, 0x02, 0x08, 0xad, 0x03, 0x02, 0xdd, 
            0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x30, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 0x00, 0x48, 
            0xb5, 0x13, 0x8d, 0x03, 0x02, 0x28, 0x4e, 0x03, 0x02, 0x08, 
            0xad, 0x03, 0x02, 0xdd, 0x28, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x30, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 
            0x03, 0xa9, 0xff, 0x48, 0xb5, 0x13, 0x8d, 0x03, 0x02, 0x28, 
            0x4e, 0x03, 0x02, 0x08, 0xad, 0x03, 0x02, 0xdd, 0x28, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 
            0x8d, 0x03, 0x02, 0x28, 0x2e, 0x03, 0x02, 0x08, 0xad, 0x03, 
            0x02, 0xdd, 0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 
            0x30, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 
            0xfe, 0x48, 0xb5, 0x13, 0x8d, 0x03, 0x02, 0x28, 0x2e, 0x03, 
            0x02, 0x08, 0xad, 0x03, 0x02, 0xdd, 0x20, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7c, 0xdd, 0x30, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe0, 0xa2, 0x03, 0xa9, 0x01, 0x48, 0xb5, 0x13, 0x8d, 0x03, 
            0x02, 0x28, 0x2e, 0x03, 0x02, 0x08, 0xad, 0x03, 0x02, 0xdd, 
            0x24, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x34, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 0xff, 0x48, 
            0xb5, 0x13, 0x8d, 0x03, 0x02, 0x28, 0x2e, 0x03, 0x02, 0x08, 
            0xad, 0x03, 0x02, 0xdd, 0x24, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7c, 0xdd, 0x34, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 
            0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 0x8d, 0x03, 0x02, 0x28, 
            0x6e, 0x03, 0x02, 0x08, 0xad, 0x03, 0x02, 0xdd, 0x28, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 0xfe, 0x48, 0xb5, 0x13, 
            0x8d, 0x03, 0x02, 0x28, 0x6e, 0x03, 0x02, 0x08, 0xad, 0x03, 
            0x02, 0xdd, 0x28, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 
            0x38, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 
            0x01, 0x48, 0xb5, 0x13, 0x8d, 0x03, 0x02, 0x28, 0x6e, 0x03, 
            0x02, 0x08, 0xad, 0x03, 0x02, 0xdd, 0x2c, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xdd, 0x3c, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe0, 0xa2, 0x03, 0xa9, 0xff, 0x48, 0xb5, 0x13, 0x8d, 0x03, 
            0x02, 0x28, 0x6e, 0x03, 0x02, 0x08, 0xad, 0x03, 0x02, 0xdd, 
            0x2c, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x3c, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xad, 0x00, 0x02, 0xc9, 0x1f, 
            0xd0, 0xfe, 0xa9, 0x20, 0x8d, 0x00, 0x02, 0xa2, 0x03, 0xa9, 
            0x00, 0x48, 0xb5, 0x13, 0x95, 0x0c, 0x28, 0x16, 0x0c, 0x08, 
            0xb5, 0x0c, 0xdd, 0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 
            0xdd, 0x30, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 
            0xa9, 0xff, 0x48, 0xb5, 0x13, 0x95, 0x0c, 0x28, 0x16, 0x0c, 
            0x08, 0xb5, 0x0c, 0xdd, 0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7c, 0xdd, 0x30, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xa2, 
            0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 0x95, 0x0c, 0x28, 0x56, 
            0x0c, 0x08, 0xb5, 0x0c, 0xdd, 0x28, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 
            0xa2, 0x03, 0xa9, 0xff, 0x48, 0xb5, 0x13, 0x95, 0x0c, 0x28, 
            0x56, 0x0c, 0x08, 0xb5, 0x0c, 0xdd, 0x28, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7c, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe3, 0xa2, 0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 0x95, 0x0c, 
            0x28, 0x36, 0x0c, 0x08, 0xb5, 0x0c, 0xdd, 0x20, 0x02, 0xd0, 
            0xfe, 0x68, 0x49, 0x30, 0xdd, 0x30, 0x02, 0xd0, 0xfe, 0xca, 
            0x10, 0xe3, 0xa2, 0x03, 0xa9, 0xfe, 0x48, 0xb5, 0x13, 0x95, 
            0x0c, 0x28, 0x36, 0x0c, 0x08, 0xb5, 0x0c, 0xdd, 0x20, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x30, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe3, 0xa2, 0x03, 0xa9, 0x01, 0x48, 0xb5, 0x13, 
            0x95, 0x0c, 0x28, 0x36, 0x0c, 0x08, 0xb5, 0x0c, 0xdd, 0x24, 
            0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x34, 0x02, 0xd0, 
            0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 0xa9, 0xff, 0x48, 0xb5, 
            0x13, 0x95, 0x0c, 0x28, 0x36, 0x0c, 0x08, 0xb5, 0x0c, 0xdd, 
            0x24, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x34, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 0xa9, 0x00, 0x48, 
            0xb5, 0x13, 0x95, 0x0c, 0x28, 0x76, 0x0c, 0x08, 0xb5, 0x0c, 
            0xdd, 0x28, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x38, 
            0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 0xa9, 0xfe, 
            0x48, 0xb5, 0x13, 0x95, 0x0c, 0x28, 0x76, 0x0c, 0x08, 0xb5, 
            0x0c, 0xdd, 0x28, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 
            0x38, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 0xa9, 
            0x01, 0x48, 0xb5, 0x13, 0x95, 0x0c, 0x28, 0x76, 0x0c, 0x08, 
            0xb5, 0x0c, 0xdd, 0x2c, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 
            0xdd, 0x3c, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xa2, 0x03, 
            0xa9, 0xff, 0x48, 0xb5, 0x13, 0x95, 0x0c, 0x28, 0x76, 0x0c, 
            0x08, 0xb5, 0x0c, 0xdd, 0x2c, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7c, 0xdd, 0x3c, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe3, 0xad, 
            0x00, 0x02, 0xc9, 0x20, 0xd0, 0xfe, 0xa9, 0x21, 0x8d, 0x00, 
            0x02, 0xa2, 0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 0x9d, 0x03, 
            0x02, 0x28, 0x1e, 0x03, 0x02, 0x08, 0xbd, 0x03, 0x02, 0xdd, 
            0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x30, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 0xff, 0x48, 
            0xb5, 0x13, 0x9d, 0x03, 0x02, 0x28, 0x1e, 0x03, 0x02, 0x08, 
            0xbd, 0x03, 0x02, 0xdd, 0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7c, 0xdd, 0x30, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 
            0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 0x9d, 0x03, 0x02, 0x28, 
            0x5e, 0x03, 0x02, 0x08, 0xbd, 0x03, 0x02, 0xdd, 0x28, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 0xff, 0x48, 0xb5, 0x13, 
            0x9d, 0x03, 0x02, 0x28, 0x5e, 0x03, 0x02, 0x08, 0xbd, 0x03, 
            0x02, 0xdd, 0x28, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 
            0x38, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 
            0x00, 0x48, 0xb5, 0x13, 0x9d, 0x03, 0x02, 0x28, 0x3e, 0x03, 
            0x02, 0x08, 0xbd, 0x03, 0x02, 0xdd, 0x20, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xdd, 0x30, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe0, 0xa2, 0x03, 0xa9, 0xfe, 0x48, 0xb5, 0x13, 0x9d, 0x03, 
            0x02, 0x28, 0x3e, 0x03, 0x02, 0x08, 0xbd, 0x03, 0x02, 0xdd, 
            0x20, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x30, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 0x01, 0x48, 
            0xb5, 0x13, 0x9d, 0x03, 0x02, 0x28, 0x3e, 0x03, 0x02, 0x08, 
            0xbd, 0x03, 0x02, 0xdd, 0x24, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x30, 0xdd, 0x34, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 
            0x03, 0xa9, 0xff, 0x48, 0xb5, 0x13, 0x9d, 0x03, 0x02, 0x28, 
            0x3e, 0x03, 0x02, 0x08, 0xbd, 0x03, 0x02, 0xdd, 0x24, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7c, 0xdd, 0x34, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 0x00, 0x48, 0xb5, 0x13, 
            0x9d, 0x03, 0x02, 0x28, 0x7e, 0x03, 0x02, 0x08, 0xbd, 0x03, 
            0x02, 0xdd, 0x28, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 
            0x38, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 
            0xfe, 0x48, 0xb5, 0x13, 0x9d, 0x03, 0x02, 0x28, 0x7e, 0x03, 
            0x02, 0x08, 0xbd, 0x03, 0x02, 0xdd, 0x28, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7c, 0xdd, 0x38, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe0, 0xa2, 0x03, 0xa9, 0x01, 0x48, 0xb5, 0x13, 0x9d, 0x03, 
            0x02, 0x28, 0x7e, 0x03, 0x02, 0x08, 0xbd, 0x03, 0x02, 0xdd
        };
        return mem;
    }
    public static int[] getMem3() {
        int mem[] = {
            0x2c, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x3c, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xa9, 0xff, 0x48, 
            0xb5, 0x13, 0x9d, 0x03, 0x02, 0x28, 0x7e, 0x03, 0x02, 0x08, 
            0xbd, 0x03, 0x02, 0xdd, 0x2c, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7c, 0xdd, 0x3c, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xad, 
            0x00, 0x02, 0xc9, 0x21, 0xd0, 0xfe, 0xa9, 0x22, 0x8d, 0x00, 
            0x02, 0xa2, 0x00, 0xa9, 0x7e, 0x85, 0x0c, 0xa9, 0x00, 0x48, 
            0x28, 0xe6, 0x0c, 0x08, 0xa5, 0x0c, 0xdd, 0x40, 0x02, 0xd0, 
            0xfe, 0x68, 0x49, 0x30, 0xdd, 0x45, 0x02, 0xd0, 0xfe, 0xe8,
            0xe0, 0x02, 0xd0, 0x04, 0xa9, 0xfe, 0x85, 0x0c, 0xe0, 0x05, 
            0xd0, 0xdd, 0xca, 0xe6, 0x0c, 0xa9, 0x00, 0x48, 0x28, 0xc6, 
            0x0c, 0x08, 0xa5, 0x0c, 0xdd, 0x40, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xdd, 0x45, 0x02, 0xd0, 0xfe, 0xca, 0x30, 0x0a, 
            0xe0, 0x01, 0xd0, 0xe3, 0xa9, 0x81, 0x85, 0x0c, 0xd0, 0xdd, 
            0xa2, 0x00, 0xa9, 0x7e, 0x85, 0x0c, 0xa9, 0xff, 0x48, 0x28, 
            0xe6, 0x0c, 0x08, 0xa5, 0x0c, 0xdd, 0x40, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xdd, 0x45, 0x02, 0xd0, 0xfe, 0xe8, 0xe0, 
            0x02, 0xd0, 0x04, 0xa9, 0xfe, 0x85, 0x0c, 0xe0, 0x05, 0xd0, 
            0xdd, 0xca, 0xe6, 0x0c, 0xa9, 0xff, 0x48, 0x28, 0xc6, 0x0c, 
            0x08, 0xa5, 0x0c, 0xdd, 0x40, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7d, 0xdd, 0x45, 0x02, 0xd0, 0xfe, 0xca, 0x30, 0x0a, 0xe0, 
            0x01, 0xd0, 0xe3, 0xa9, 0x81, 0x85, 0x0c, 0xd0, 0xdd, 0xad, 
            0x00, 0x02, 0xc9, 0x22, 0xd0, 0xfe, 0xa9, 0x23, 0x8d, 0x00, 
            0x02, 0xa2, 0x00, 0xa9, 0x7e, 0x8d, 0x03, 0x02, 0xa9, 0x00, 
            0x48, 0x28, 0xee, 0x03, 0x02, 0x08, 0xad, 0x03, 0x02, 0xdd, 
            0x40, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x45, 0x02, 
            0xd0, 0xfe, 0xe8, 0xe0, 0x02, 0xd0, 0x05, 0xa9, 0xfe, 0x8d, 
            0x03, 0x02, 0xe0, 0x05, 0xd0, 0xda, 0xca, 0xee, 0x03, 0x02, 
            0xa9, 0x00, 0x48, 0x28, 0xce, 0x03, 0x02, 0x08, 0xad, 0x03, 
            0x02, 0xdd, 0x40, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 
            0x45, 0x02, 0xd0, 0xfe, 0xca, 0x30, 0x0b, 0xe0, 0x01, 0xd0, 
            0xe1, 0xa9, 0x81, 0x8d, 0x03, 0x02, 0xd0, 0xda, 0xa2, 0x00, 
            0xa9, 0x7e, 0x8d, 0x03, 0x02, 0xa9, 0xff, 0x48, 0x28, 0xee, 
            0x03, 0x02, 0x08, 0xad, 0x03, 0x02, 0xdd, 0x40, 0x02, 0xd0, 
            0xfe, 0x68, 0x49, 0x7d, 0xdd, 0x45, 0x02, 0xd0, 0xfe, 0xe8, 
            0xe0, 0x02, 0xd0, 0x05, 0xa9, 0xfe, 0x8d, 0x03, 0x02, 0xe0, 
            0x05, 0xd0, 0xda, 0xca, 0xee, 0x03, 0x02, 0xa9, 0xff, 0x48, 
            0x28, 0xce, 0x03, 0x02, 0x08, 0xad, 0x03, 0x02, 0xdd, 0x40, 
            0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 0x45, 0x02, 0xd0, 
            0xfe, 0xca, 0x30, 0x0b, 0xe0, 0x01, 0xd0, 0xe1, 0xa9, 0x81, 
            0x8d, 0x03, 0x02, 0xd0, 0xda, 0xad, 0x00, 0x02, 0xc9, 0x23, 
            0xd0, 0xfe, 0xa9, 0x24, 0x8d, 0x00, 0x02, 0xa2, 0x00, 0xa9, 
            0x7e, 0x95, 0x0c, 0xa9, 0x00, 0x48, 0x28, 0xf6, 0x0c, 0x08, 
            0xb5, 0x0c, 0xdd, 0x40, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 
            0xdd, 0x45, 0x02, 0xd0, 0xfe, 0xb5, 0x0c, 0xe8, 0xe0, 0x02, 
            0xd0, 0x02, 0xa9, 0xfe, 0xe0, 0x05, 0xd0, 0xdb, 0xca, 0xa9, 
            0x02, 0x95, 0x0c, 0xa9, 0x00, 0x48, 0x28, 0xd6, 0x0c, 0x08, 
            0xb5, 0x0c, 0xdd, 0x40, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 
            0xdd, 0x45, 0x02, 0xd0, 0xfe, 0xb5, 0x0c, 0xca, 0x30, 0x08, 
            0xe0, 0x01, 0xd0, 0xdf, 0xa9, 0x81, 0xd0, 0xdb, 0xa2, 0x00, 
            0xa9, 0x7e, 0x95, 0x0c, 0xa9, 0xff, 0x48, 0x28, 0xf6, 0x0c, 
            0x08, 0xb5, 0x0c, 0xdd, 0x40, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7d, 0xdd, 0x45, 0x02, 0xd0, 0xfe, 0xb5, 0x0c, 0xe8, 0xe0, 
            0x02, 0xd0, 0x02, 0xa9, 0xfe, 0xe0, 0x05, 0xd0, 0xdb, 0xca, 
            0xa9, 0x02, 0x95, 0x0c, 0xa9, 0xff, 0x48, 0x28, 0xd6, 0x0c, 
            0x08, 0xb5, 0x0c, 0xdd, 0x40, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7d, 0xdd, 0x45, 0x02, 0xd0, 0xfe, 0xb5, 0x0c, 0xca, 0x30, 
            0x08, 0xe0, 0x01, 0xd0, 0xdf, 0xa9, 0x81, 0xd0, 0xdb, 0xad, 
            0x00, 0x02, 0xc9, 0x24, 0xd0, 0xfe, 0xa9, 0x25, 0x8d, 0x00, 
            0x02, 0xa2, 0x00, 0xa9, 0x7e, 0x9d, 0x03, 0x02, 0xa9, 0x00, 
            0x48, 0x28, 0xfe, 0x03, 0x02, 0x08, 0xbd, 0x03, 0x02, 0xdd, 
            0x40, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x45, 0x02, 
            0xd0, 0xfe, 0xbd, 0x03, 0x02, 0xe8, 0xe0, 0x02, 0xd0, 0x02, 
            0xa9, 0xfe, 0xe0, 0x05, 0xd0, 0xd7, 0xca, 0xa9, 0x02, 0x9d, 
            0x03, 0x02, 0xa9, 0x00, 0x48, 0x28, 0xde, 0x03, 0x02, 0x08, 
            0xbd, 0x03, 0x02, 0xdd, 0x40, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x30, 0xdd, 0x45, 0x02, 0xd0, 0xfe, 0xbd, 0x03, 0x02, 0xca, 
            0x30, 0x08, 0xe0, 0x01, 0xd0, 0xdb, 0xa9, 0x81, 0xd0, 0xd7, 
            0xa2, 0x00, 0xa9, 0x7e, 0x9d, 0x03, 0x02, 0xa9, 0xff, 0x48, 
            0x28, 0xfe, 0x03, 0x02, 0x08, 0xbd, 0x03, 0x02, 0xdd, 0x40, 
            0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 0x45, 0x02, 0xd0, 
            0xfe, 0xbd, 0x03, 0x02, 0xe8, 0xe0, 0x02, 0xd0, 0x02, 0xa9, 
            0xfe, 0xe0, 0x05, 0xd0, 0xd7, 0xca, 0xa9, 0x02, 0x9d, 0x03, 
            0x02, 0xa9, 0xff, 0x48, 0x28, 0xde, 0x03, 0x02, 0x08, 0xbd, 
            0x03, 0x02, 0xdd, 0x40, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 
            0xdd, 0x45, 0x02, 0xd0, 0xfe, 0xbd, 0x03, 0x02, 0xca, 0x30, 
            0x08, 0xe0, 0x01, 0xd0, 0xdb, 0xa9, 0x81, 0xd0, 0xd7, 0xad, 
            0x00, 0x02, 0xc9, 0x25, 0xd0, 0xfe, 0xa9, 0x26, 0x8d, 0x00, 
            0x02, 0xa2, 0x03, 0xb5, 0x1c, 0x8d, 0x09, 0x02, 0xa9, 0x00, 
            0x48, 0xbd, 0x5a, 0x02, 0x28, 0x20, 0x08, 0x02, 0x08, 0xdd, 
            0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x66, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xb5, 0x1c, 0x8d, 
            0x09, 0x02, 0xa9, 0xff, 0x48, 0xbd, 0x5a, 0x02, 0x28, 0x20, 
            0x08, 0x02, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7d, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 
            0x03, 0xb5, 0x1c, 0x85, 0x0c, 0xa9, 0x00, 0x48, 0xbd, 0x5a, 
            0x02, 0x28, 0x25, 0x0c, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe2, 0xa2, 0x03, 0xb5, 0x1c, 0x85, 0x0c, 0xa9, 0xff, 0x48, 
            0xbd, 0x5a, 0x02, 0x28, 0x25, 0x0c, 0x08, 0xdd, 0x62, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe2, 0xa2, 0x03, 0xb5, 0x1c, 0x8d, 0x03, 0x02, 
            0xa9, 0x00, 0x48, 0xbd, 0x5a, 0x02, 0x28, 0x2d, 0x03, 0x02, 
            0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 
            0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xb5, 
            0x1c, 0x8d, 0x03, 0x02, 0xa9, 0xff, 0x48, 0xbd, 0x5a, 0x02, 
            0x28, 0x2d, 0x03, 0x02, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0x02, 0xa2, 0x03, 0xa9, 0x00, 0x48, 0xbd, 0x5a, 0x02, 0x28, 
            0x35, 0x1c, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x30, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe6, 0xa2, 
            0x03, 0xa9, 0xff, 0x48, 0xbd, 0x5a, 0x02, 0x28, 0x35, 0x1c, 
            0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 
            0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe6, 0xa2, 0x03, 0xa9, 
            0x00, 0x48, 0xbd, 0x5a, 0x02, 0x28, 0x3d, 0x4e, 0x02, 0x08, 
            0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x66, 
            0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe5, 0xa2, 0x03, 0xa9, 0xff, 
            0x48, 0xbd, 0x5a, 0x02, 0x28, 0x3d, 0x4e, 0x02, 0x08, 0xdd, 
            0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 0x66, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe5, 0xa0, 0x03, 0xa9, 0x00, 0x48, 
            0xb9, 0x5a, 0x02, 0x28, 0x39, 0x4e, 0x02, 0x08, 0xd9, 0x62, 
            0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xd9, 0x66, 0x02, 0xd0, 
            0xfe, 0x88, 0x10, 0xe5, 0xa0, 0x03, 0xa9, 0xff, 0x48, 0xb9, 
            0x5a, 0x02, 0x28, 0x39, 0x4e, 0x02, 0x08, 0xd9, 0x62, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xd9, 0x66, 0x02, 0xd0, 0xfe, 
            0x88, 0x10, 0xe5, 0xa2, 0x06, 0xa0, 0x03, 0xa9, 0x00, 0x48, 
            0xb9, 0x5a, 0x02, 0x28, 0x21, 0x3a, 0x08, 0xd9, 0x62, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xd9, 0x66, 0x02, 0xd0, 0xfe, 
            0xca, 0xca, 0x88, 0x10, 0xe4, 0xa2, 0x06, 0xa0, 0x03, 0xa9, 
            0xff, 0x48, 0xb9, 0x5a, 0x02, 0x28, 0x21, 0x3a, 0x08, 0xd9, 
            0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xd9, 0x66, 0x02, 
            0xd0, 0xfe, 0xca, 0xca, 0x88, 0x10, 0xe4, 0xa0, 0x03, 0xa9, 
            0x00, 0x48, 0xb9, 0x5a, 0x02, 0x28, 0x31, 0x3a, 0x08, 0xd9, 
            0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xd9, 0x66, 0x02, 
            0xd0, 0xfe, 0x88, 0x10, 0xe6, 0xa0, 0x03, 0xa9, 0xff, 0x48, 
            0xb9, 0x5a, 0x02, 0x28, 0x31, 0x3a, 0x08, 0xd9, 0x62, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xd9, 0x66, 0x02, 0xd0, 0xfe, 
            0x88, 0x10, 0xe6, 0xad, 0x00, 0x02, 0xc9, 0x26, 0xd0, 0xfe, 
            0xa9, 0x27, 0x8d, 0x00, 0x02, 0xa2, 0x03, 0xb5, 0x20, 0x8d, 
            0x0c, 0x02, 0xa9, 0x00, 0x48, 0xbd, 0x5e, 0x02, 0x28, 0x20, 
            0x0b, 0x02, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x30, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 
            0x03, 0xb5, 0x20, 0x8d, 0x0c, 0x02, 0xa9, 0xff, 0x48, 0xbd, 
            0x5e, 0x02, 0x28, 0x20, 0x0b, 0x02, 0x08, 0xdd, 0x62, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe0, 0xa2, 0x03, 0xb5, 0x20, 0x85, 0x0c, 0xa9, 
            0x00, 0x48, 0xbd, 0x5e, 0x02, 0x28, 0x45, 0x0c, 0x08, 0xdd, 
            0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x66, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe2, 0xa2, 0x03, 0xb5, 0x20, 0x85, 
            0x0c, 0xa9, 0xff, 0x48, 0xbd, 0x5e, 0x02, 0x28, 0x45, 0x0c, 
            0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 
            0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe2, 0xa2, 0x03, 0xb5, 
            0x20, 0x8d, 0x03, 0x02, 0xa9, 0x00, 0x48, 0xbd, 0x5e, 0x02, 
            0x28, 0x4d, 0x03, 0x02, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe0, 0xa2, 0x03, 0xb5, 0x20, 0x8d, 0x03, 0x02, 0xa9, 0xff, 
            0x48, 0xbd, 0x5e, 0x02, 0x28, 0x4d, 0x03, 0x02, 0x08, 0xdd, 
            0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 0x66, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0x02, 0xa2, 0x03, 0xa9, 0x00, 0x48, 
            0xbd, 0x5e, 0x02, 0x28, 0x55, 0x20, 0x08, 0xdd, 0x62, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe6, 0xa2, 0x03, 0xa9, 0xff, 0x48, 0xbd, 0x5e, 
            0x02, 0x28, 0x55, 0x20, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe6, 0xa2, 0x03, 0xa9, 0x00, 0x48, 0xbd, 0x5e, 0x02, 0x28, 
            0x5d, 0x52, 0x02, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 
            0x49, 0x30, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe5, 
            0xa2, 0x03, 0xa9, 0xff, 0x48, 0xbd, 0x5e, 0x02, 0x28, 0x5d, 
            0x52, 0x02, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7d, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe5, 0xa0, 
            0x03, 0xa9, 0x00, 0x48, 0xb9, 0x5e, 0x02, 0x28, 0x59, 0x52, 
            0x02, 0x08, 0xd9, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 
            0xd9, 0x66, 0x02, 0xd0, 0xfe, 0x88, 0x10, 0xe5, 0xa0, 0x03, 
            0xa9, 0xff, 0x48, 0xb9, 0x5e, 0x02, 0x28, 0x59, 0x52, 0x02, 
            0x08, 0xd9, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xd9, 
            0x66, 0x02, 0xd0, 0xfe, 0x88, 0x10, 0xe5, 0xa2, 0x06, 0xa0, 
            0x03, 0xa9, 0x00, 0x48, 0xb9, 0x5e, 0x02, 0x28, 0x41, 0x42, 
            0x08, 0xd9, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xd9, 
            0x66, 0x02, 0xd0, 0xfe, 0xca, 0xca, 0x88, 0x10, 0xe4, 0xa2, 
            0x06, 0xa0, 0x03, 0xa9, 0xff, 0x48, 0xb9, 0x5e, 0x02, 0x28, 
            0x41, 0x42, 0x08, 0xd9, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7d, 0xd9, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0xca, 0x88, 0x10, 
            0xe4, 0xa0, 0x03, 0xa9, 0x00, 0x48, 0xb9, 0x5e, 0x02, 0x28, 
            0x51, 0x42, 0x08, 0xd9, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x30, 0xd9, 0x66, 0x02, 0xd0, 0xfe, 0x88, 0x10, 0xe6, 0xa0, 
            0x03, 0xa9, 0xff, 0x48, 0xb9, 0x5e, 0x02, 0x28, 0x51, 0x42, 
            0x08, 0xd9, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xd9, 
            0x66, 0x02, 0xd0, 0xfe, 0x88, 0x10, 0xe6, 0xad, 0x00, 0x02, 
            0xc9, 0x27, 0xd0, 0xfe, 0xa9, 0x28, 0x8d, 0x00, 0x02, 0xa2, 
            0x03, 0xb5, 0x18, 0x8d, 0x0f, 0x02, 0xa9, 0x00, 0x48, 0xbd, 
            0x56, 0x02, 0x28, 0x20, 0x0e, 0x02, 0x08, 0xdd, 0x62, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe0, 0xa2, 0x03, 0xb5, 0x18, 0x8d, 0x0f, 0x02, 
            0xa9, 0xff, 0x48, 0xbd, 0x56, 0x02, 0x28, 0x20, 0x0e, 0x02, 
            0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 
            0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xb5, 
            0x18, 0x85, 0x0c, 0xa9, 0x00, 0x48, 0xbd, 0x56, 0x02, 0x28, 
            0x05, 0x0c, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x30, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe2, 0xa2, 
            0x03, 0xb5, 0x18, 0x85, 0x0c, 0xa9, 0xff, 0x48, 0xbd, 0x56, 
            0x02, 0x28, 0x05, 0x0c, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 
            0xe2, 0xa2, 0x03, 0xb5, 0x18, 0x8d, 0x03, 0x02, 0xa9, 0x00, 
            0x48, 0xbd, 0x56, 0x02, 0x28, 0x0d, 0x03, 0x02, 0x08, 0xdd, 
            0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x66, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe0, 0xa2, 0x03, 0xb5, 0x18, 0x8d, 
            0x03, 0x02, 0xa9, 0xff, 0x48, 0xbd, 0x56, 0x02, 0x28, 0x0d, 
            0x03, 0x02, 0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 
            0x7d, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0x02, 0xa2, 
            0x03, 0xa9, 0x00, 0x48, 0xbd, 0x56, 0x02, 0x28, 0x15, 0x18, 
            0x08, 0xdd, 0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 
            0x66, 0x02, 0xd0, 0xfe, 0xca, 0x10, 0xe6, 0xa2, 0x03, 0xa9, 
            0xff, 0x48, 0xbd, 0x56, 0x02, 0x28, 0x15, 0x18, 0x08, 0xdd, 
            0x62, 0x02, 0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 0x66, 0x02, 
            0xd0, 0xfe, 0xca, 0x10, 0xe6, 0xa2, 0x03, 0xa9, 0x00, 0x48, 
            0xbd, 0x56, 0x02, 0x28, 0x1d, 0x4a, 0x02, 0x08, 0xdd, 0x62, 
            0x02, 0xd0, 0xfe, 0x68, 0x49, 0x30, 0xdd, 0x66, 0x02, 0xd0, 
            0xfe, 0xca, 0x10, 0xe5, 0xa2, 0x03, 0xa9, 0xff, 0x48, 0xbd, 
            0x56, 0x02, 0x28, 0x1d, 0x4a, 0x02, 0x08, 0xdd, 0x62, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xdd, 0x66, 0x02, 0xd0, 0xfe, 
            0xca, 0x10, 0xe5, 0xa0, 0x03, 0xa9, 0x00, 0x48, 0xb9, 0x56, 
            0x02, 0x28, 0x19, 0x4a, 0x02, 0x08, 0xd9, 0x62, 0x02, 0xd0, 
            0xfe, 0x68, 0x49, 0x30, 0xd9, 0x66, 0x02, 0xd0, 0xfe, 0x88, 
            0x10, 0xe5, 0xa0, 0x03, 0xa9, 0xff, 0x48, 0xb9, 0x56, 0x02, 
            0x28, 0x19, 0x4a, 0x02, 0x08, 0xd9, 0x62, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xd9, 0x66, 0x02, 0xd0, 0xfe, 0x88, 0x10, 
            0xe5, 0xa2, 0x06, 0xa0, 0x03, 0xa9, 0x00, 0x48, 0xb9, 0x56, 
            0x02, 0x28, 0x01, 0x4a, 0x08, 0xd9, 0x62, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x30, 0xd9, 0x66, 0x02, 0xd0, 0xfe, 0xca, 0xca, 
            0x88, 0x10, 0xe4, 0xa2, 0x06, 0xa0, 0x03, 0xa9, 0xff, 0x48, 
            0xb9, 0x56, 0x02, 0x28, 0x01, 0x4a, 0x08, 0xd9, 0x62, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x7d, 0xd9, 0x66, 0x02, 0xd0, 0xfe, 
            0xca, 0xca, 0x88, 0x10, 0xe4, 0xa0, 0x03, 0xa9, 0x00, 0x48, 
            0xb9, 0x56, 0x02, 0x28, 0x11, 0x4a, 0x08, 0xd9, 0x62, 0x02, 
            0xd0, 0xfe, 0x68, 0x49, 0x30, 0xd9, 0x66, 0x02, 0xd0, 0xfe, 
            0x88, 0x10, 0xe6, 0xa0, 0x03, 0xa9, 0xff, 0x48, 0xb9, 0x56, 
            0x02, 0x28, 0x11, 0x4a, 0x08, 0xd9, 0x62, 0x02, 0xd0, 0xfe, 
            0x68, 0x49, 0x7d, 0xd9, 0x66, 0x02, 0xd0, 0xfe, 0x88, 0x10, 
            0xe6, 0x58, 0xad, 0x00, 0x02, 0xc9, 0x28, 0xd0, 0xfe, 0xa9, 
            0x29, 0x8d, 0x00, 0x02, 0xd8, 0xa2, 0x0e, 0xa0, 0xff, 0xa9, 
            0x00, 0x85, 0x0c, 0x85, 0x0d, 0x85, 0x0e, 0x8d, 0x03, 0x02, 
            0x85, 0x0f, 0x85, 0x10, 0xa9, 0xff, 0x85, 0x12, 0x8d, 0x04, 
            0x02, 0xa9, 0x02, 0x85, 0x11, 0x18, 0x20, 0x9c, 0x35, 0xe6, 
            0x0c, 0xe6, 0x0f, 0x08, 0x08, 0x68, 0x29, 0x82, 0x28, 0xd0, 
            0x02, 0xe6, 0x10, 0x05, 0x10, 0x85, 0x11, 0x38, 0x20, 0x9c, 
            0x35, 0xc6, 0x0c, 0xe6, 0x0d, 0xd0, 0xe0, 0xa9, 0x00, 0x85, 
            0x10, 0xee, 0x03, 0x02, 0xe6, 0x0e, 0x08, 0x68, 0x29, 0x82, 
            0x85, 0x11, 0xc6, 0x12, 0xce, 0x04, 0x02, 0xa5, 0x0e, 0x85, 
            0x0f, 0xd0, 0xc6, 0xad, 0x00, 0x02, 0xc9, 0x29, 0xd0, 0xfe, 
            0xa9, 0x2a, 0x8d, 0x00, 0x02, 0xf8, 0xa2, 0x0e, 0xa0, 0xff, 
            0xa9, 0x99, 0x85, 0x0d, 0x85, 0x0e, 0x8d, 0x03, 0x02, 0x85, 
            0x0f, 0xa9, 0x01, 0x85, 0x0c, 0x85, 0x10, 0xa9, 0x00, 0x85, 
            0x12, 0x8d, 0x04, 0x02, 0x38, 0x20, 0x69, 0x34, 0xc6, 0x0c, 
            0xa5, 0x0f, 0xd0, 0x08, 0xc6, 0x10, 0xa9, 0x99, 0x85, 0x0f, 
            0xd0, 0x12, 0x29, 0x0f, 0xd0, 0x0c, 0xc6, 0x0f, 0xc6, 0x0f, 
            0xc6, 0x0f, 0xc6, 0x0f, 0xc6, 0x0f, 0xc6, 0x0f, 0xc6, 0x0f, 
            0x18, 0x20, 0x69, 0x34, 0xe6, 0x0c, 0xa5, 0x0d, 0xf0, 0x15, 
            0x29, 0x0f, 0xd0, 0x0c, 0xc6, 0x0d, 0xc6, 0x0d, 0xc6, 0x0d, 
            0xc6, 0x0d, 0xc6, 0x0d, 0xc6, 0x0d, 0xc6, 0x0d, 0x4c, 0x84, 
            0x33, 0xa9, 0x99, 0x85, 0x0d, 0xa5, 0x0e, 0xf0, 0x30, 0x29, 
            0x0f, 0xd0, 0x18, 0xc6, 0x0e, 0xc6, 0x0e, 0xc6, 0x0e, 0xc6, 
            0x0e, 0xc6, 0x0e, 0xc6, 0x0e, 0xe6, 0x12, 0xe6, 0x12, 0xe6, 
            0x12, 0xe6, 0x12, 0xe6, 0x12, 0xe6, 0x12, 0xc6, 0x0e, 0xe6, 
            0x12, 0xa5, 0x12, 0x8d, 0x04, 0x02, 0xa5, 0x0e, 0x8d, 0x03, 
            0x02, 0x85, 0x0f, 0xe6, 0x10, 0xd0, 0x85, 0xad, 0x00, 0x02, 
            0xc9, 0x2a, 0xd0, 0xfe, 0xa9, 0x2b, 0x8d, 0x00, 0x02, 0x18, 
            0xd8, 0x08, 0xa9, 0x55, 0x69, 0x55, 0xc9, 0xaa, 0xd0, 0xfe, 
            0x18, 0xf8, 0x08, 0xa9, 0x55, 0x69, 0x55, 0xc9, 0x10, 0xd0, 
            0xfe, 0xd8, 0x28, 0xa9, 0x55, 0x69, 0x55, 0xc9, 0x10, 0xd0, 
            0xfe, 0x28, 0xa9, 0x55, 0x69, 0x55, 0xc9, 0xaa, 0xd0, 0xfe, 
            0x18, 0xa9, 0x34, 0x48, 0xa9, 0x4f, 0x48, 0x08, 0xf8, 0xa9, 
            0x34, 0x48, 0xa9, 0x46, 0x48, 0x08, 0xd8, 0x40, 0xa9, 0x55, 
            0x69, 0x55, 0xc9, 0x10, 0xd0, 0xfe, 0x40, 0xa9, 0x55, 0x69, 
            0x55, 0xc9, 0xaa, 0xd0, 0xfe, 0xad, 0x00, 0x02, 0xc9, 0x2b, 
            0xd0, 0xfe, 0xa9, 0xf0, 0x8d, 0x00, 0x02, 0x4c, 0x63, 0x34, 
            0x4c, 0x00, 0x04, 0x08, 0xa5, 0x0d, 0x65, 0x0e, 0x08, 0xc5, 
            0x0f, 0xd0, 0xfe, 0x68, 0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 
            0x28, 0x08, 0xa5, 0x0d, 0xe5, 0x12, 0x08, 0xc5, 0x0f, 0xd0, 
            0xfe, 0x68, 0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 0x28, 0x08, 
            0xa5, 0x0d, 0x6d, 0x03, 0x02, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 
            0x68, 0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 
            0x0d, 0xed, 0x04, 0x02, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 0x68, 
            0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 0x0e, 
            0x8d, 0x12, 0x02, 0xa5, 0x0d, 0x20, 0x11, 0x02, 0x08, 0xc5, 
            0x0f, 0xd0, 0xfe, 0x68, 0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 
            0x28, 0x08, 0xa5, 0x12, 0x8d, 0x15, 0x02, 0xa5, 0x0d, 0x20, 
            0x14, 0x02, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 0x01, 
            0xc5, 0x10, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 0x0d, 0x75, 0x00, 
            0x08, 0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 0x01, 0xc5, 0x10, 
            0xd0, 0xfe, 0x28, 0x08, 0xa5, 0x0d, 0xf5, 0x04, 0x08, 0xc5, 
            0x0f, 0xd0, 0xfe, 0x68, 0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 
            0x28, 0x08, 0xa5, 0x0d, 0x7d, 0xf5, 0x01, 0x08, 0xc5, 0x0f, 
            0xd0, 0xfe, 0x68, 0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 0x28, 
            0x08, 0xa5, 0x0d, 0xfd, 0xf6, 0x01, 0x08, 0xc5, 0x0f, 0xd0, 
            0xfe, 0x68, 0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 0x28, 0x08, 
            0xa5, 0x0d, 0x79, 0x04, 0x01, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 
            0x68, 0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 
            0x0d, 0xf9, 0x05, 0x01, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 0x68, 
            0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 0x0d, 
            0x61, 0x44, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 0x01, 
            0xc5, 0x10, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 0x0d, 0xe1, 0x46, 
            0x08, 0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 0x01, 0xc5, 0x10, 
            0xd0, 0xfe, 0x28, 0x08, 0xa5, 0x0d, 0x71, 0x56, 0x08, 0xc5, 
            0x0f, 0xd0, 0xfe, 0x68, 0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 
            0x28, 0x08, 0xa5, 0x0d, 0xf1, 0x58, 0x08, 0xc5, 0x0f, 0xd0, 
            0xfe, 0x68, 0x29, 0x01, 0xc5, 0x10, 0xd0, 0xfe, 0x28, 0x60, 
            0xa5, 0x11, 0x29, 0x83, 0x48, 0xa5, 0x0d, 0x45, 0x0e, 0x30, 
            0x0a, 0xa5, 0x0d, 0x45, 0x0f, 0x10, 0x04, 0x68, 0x09, 0x40, 
            0x48, 0x68, 0x85, 0x11, 0x08, 0xa5, 0x0d, 0x65, 0x0e, 0x08, 
            0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 
            0xfe, 0x28, 0x08, 0xa5, 0x0d, 0xe5, 0x12, 0x08, 0xc5, 0x0f, 
            0xd0, 0xfe, 0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 0xfe, 0x28, 
            0x08, 0xa5, 0x0d, 0x6d, 0x03, 0x02, 0x08, 0xc5, 0x0f, 0xd0, 
            0xfe, 0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 0xfe, 0x28, 0x08, 
            0xa5, 0x0d, 0xed, 0x04, 0x02, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 
            0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 
            0x0e, 0x8d, 0x12, 0x02, 0xa5, 0x0d, 0x20, 0x11, 0x02, 0x08, 
            0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 
            0xfe, 0x28, 0x08, 0xa5, 0x12, 0x8d, 0x15, 0x02, 0xa5, 0x0d, 
            0x20, 0x14, 0x02, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 
            0xc3, 0xc5, 0x11, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 0x0d, 0x75, 
            0x00, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 0xc3, 0xc5, 
            0x11, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 0x0d, 0xf5, 0x04, 0x08, 
            0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 
            0xfe, 0x28, 0x08, 0xa5, 0x0d, 0x7d, 0xf5, 0x01, 0x08, 0xc5, 
            0x0f, 0xd0, 0xfe, 0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 0xfe, 
            0x28, 0x08, 0xa5, 0x0d, 0xfd, 0xf6, 0x01, 0x08, 0xc5, 0x0f, 
            0xd0, 0xfe, 0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 0xfe, 0x28, 
            0x08, 0xa5, 0x0d, 0x79, 0x04, 0x01, 0x08, 0xc5, 0x0f, 0xd0, 
            0xfe, 0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 0xfe, 0x28, 0x08, 
            0xa5, 0x0d, 0xf9, 0x05, 0x01, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 
            0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 
            0x0d, 0x61, 0x44, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 
            0xc3, 0xc5, 0x11, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 0x0d, 0xe1, 
            0x46, 0x08, 0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 0xc3, 0xc5, 
            0x11, 0xd0, 0xfe, 0x28, 0x08, 0xa5, 0x0d, 0x71, 0x56, 0x08, 
            0xc5, 0x0f, 0xd0, 0xfe, 0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 
            0xfe, 0x28, 0x08, 0xa5, 0x0d, 0xf1, 0x58, 0x08, 0xc5, 0x0f, 
            0xd0, 0xfe, 0x68, 0x29, 0xc3, 0xc5, 0x11, 0xd0, 0xfe, 0x28, 
            0x60, 0x88, 0x88, 0x08, 0x88, 0x88, 0x88, 0x28, 0xb0, 0xfe, 
            0x70, 0xfe, 0x30, 0xfe, 0xf0, 0xfe, 0xc9, 0x46, 0xd0, 0xfe, 
            0xe0, 0x41, 0xd0, 0xfe, 0xc0, 0x4f, 0xd0, 0xfe, 0x48, 0x8a, 
            0x48, 0xba, 0xe0, 0xfd, 0xd0, 0xfe, 0x68, 0xaa, 0xa9, 0xff, 
            0x48, 0x28, 0x68, 0xe8, 0x49, 0xaa, 0x4c, 0x2f, 0x09, 0x00, 
            0x21, 0x37, 0x84, 0x09, 0x4c, 0x1c, 0x37, 0x88, 0x88, 0x08, 
            0x88, 0x88, 0x88, 0x28, 0xb0, 0xfe, 0x70, 0xfe, 0x30, 0xfe, 
            0xf0, 0xfe, 0xc9, 0x49, 0xd0, 0xfe, 0xe0, 0x4e, 0xd0, 0xfe, 
            0xc0, 0x41, 0xd0, 0xfe, 0x48, 0x8a, 0x48, 0xba, 0xe0, 0xfd, 
            0xd0, 0xfe, 0x68, 0xaa, 0xa9, 0xff, 0x48, 0x28, 0x68, 0xe8, 
            0x49, 0xaa, 0x6c, 0x1a, 0x37, 0x4c, 0x4f, 0x37, 0x88, 0x88, 
            0x08, 0x88, 0x88, 0x88, 0x28, 0xb0, 0xfe, 0x70, 0xfe, 0x30, 
            0xfe, 0xf0, 0xfe, 0xc9, 0x4a, 0xd0, 0xfe, 0xe0, 0x53, 0xd0, 
            0xfe, 0xc0, 0x4f, 0xd0, 0xfe, 0x48, 0x8a, 0x48, 0xba, 0xe0, 
            0xfb, 0xd0, 0xfe, 0xad, 0xff, 0x01, 0xc9, 0x09, 0xd0, 0xfe, 
            0xad, 0xfe, 0x01, 0xc9, 0xba, 0xd0, 0xfe, 0xa9, 0xff, 0x48, 
            0x28, 0x68, 0xaa, 0x68, 0xe8, 0x49, 0xaa, 0x60, 0x4c, 0x8e, 
            0x37, 0x4c, 0x91, 0x37, 0x4c, 0x94, 0x37, 0x88, 0x88, 0x08, 
            0x88, 0x88, 0x88, 0xc9, 0x42, 0xd0, 0xfe, 0xe0, 0x52, 0xd0, 
            0xfe, 0xc0, 0x48, 0xd0, 0xfe, 0x85, 0x0a, 0x86, 0x0b, 0xba, 
            0xbd, 0x02, 0x01, 0xc9, 0x30, 0xd0, 0xfe, 0x68, 0xc9, 0x34, 
            0xd0, 0xfe, 0xba, 0xe0, 0xfc, 0xd0, 0xfe, 0xad, 0xff, 0x01, 
            0xc9, 0x09, 0xd0, 0xfe, 0xad, 0xfe, 0x01, 0xc9, 0xf1, 0xd0, 
            0xfe, 0xa9, 0xff, 0x48, 0x28, 0xa6, 0x0b, 0xe8, 0xa5, 0x0a, 
            0x49, 0xaa, 0x40, 0x4c, 0xd9, 0x37, 0xc3, 0x82, 0x41, 0x00, 
            0x7f, 0x00, 0x1f, 0x71, 0x80, 0x0f, 0xff, 0x7f, 0x80, 0xff, 
            0x0f, 0x8f, 0x8f, 0x17, 0x02, 0x18, 0x02, 0x19, 0x02, 0x1a, 
            0x02, 0x1b, 0x02, 0x1f, 0x01, 0x03, 0x02, 0x04, 0x02, 0x05, 
            0x02, 0x06, 0x02, 0x0b, 0x01, 0x4e, 0x02, 0x4f, 0x02, 0x50, 
            0x02, 0x51, 0x02, 0x52, 0x02, 0x53, 0x02, 0x54, 0x02, 0x55, 
            0x02, 0x4a, 0x02, 0x4b, 0x02, 0x4c, 0x02, 0x4d, 0x02, 0x03, 
            0x02, 0x04, 0x02, 0x04, 0x01, 0x05, 0x01, 0x29, 0x00, 0x60, 
            0x49, 0x00, 0x60, 0x09, 0x00, 0x60, 0x69, 0x00, 0x60, 0xe9, 
            0x00, 0x60, 0xc3, 0x82, 0x41, 0x00, 0x7f, 0x80, 0x80, 0x00, 
            0x02, 0x86, 0x04, 0x82, 0x00, 0x87, 0x05, 0x83, 0x01, 0x61, 
            0x41, 0x20, 0x00, 0xe1, 0xc1, 0xa0, 0x80, 0x81, 0x01, 0x80, 
            0x02, 0x81, 0x01, 0x80, 0x00, 0x01, 0x00, 0x01, 0x02, 0x81, 
            0x80, 0x81, 0x80, 0x7f, 0x80, 0xff, 0x00, 0x01, 0x00, 0x80, 
            0x80, 0x02, 0x00, 0x00, 0x1f, 0x71, 0x80, 0x0f, 0xff, 0x7f, 
            0x80, 0xff, 0x0f, 0x8f, 0x8f, 0x00, 0xf1, 0x1f, 0x00, 0xf0, 
            0xff, 0xff, 0xff, 0xff, 0xf0, 0xf0, 0x0f, 0x00, 0xff, 0x7f, 
            0x80, 0x02, 0x80, 0x00, 0x80, 0x91, 0x37, 0x94, 0x37, 0x99, 
            0x37        
        };
        return mem;
    }
}
