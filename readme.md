# LocoNES

A, not completed, always in progress, NES emulator in Java

The vast majority of this file comes from https://www.nesdev.org/wik

## General stuff to know

In java

## NES Architecture and stuff i learned

First and foremost, remember that the byte type in java is signed, this means that in practical terms a byte
stores values -128 to 127. This means that the leftmost bit on a java byte indicates not a value, but the 
sign of the value it stores. 

All this has implications over the code, because to represent a byte i am NOT using the byte type, but 
the int type instead, 

### CPU 

The NES CPU is a 6502 one. The way it works is actually not that complicated. When the CPU reads a byte 
from memory, that will indicate an OpCode. The OpCode will indicate which instruction to run 
and which addressing mode to use. The addressing mode will mean that we have to read 0, 1 or 2 bytes more. 

So all in all the CPU can be emulated doing this:

1. Read one byte from memory, this is the opcode
2. This tells you which instruction to run and which addressing mode to use
3. Using the addressing mode, get the argument (if any) for the instruction
4. Once you have the instruction and the argument, then execute it.

An example

OpCode `0x25` is instruction `AND` using address mode `ZeroPage`. The `AND` operation is 
a bitwise operation between the argument from the instruction and the register `A`. If the new value of 
register `A` is negative, then the negative flag of the register `S (status)` is set to 1, if the resulting value is 
0 then the negative flag of register `S` is set to 1.

#### Registers

The 6502 has few registers. 

1. Register X
2. Register Y
3. Register A (Acumulator)
4. Register SP (Stack Pointer)
5. ProgramCounter (PC)

All registers are 1 byte, except the ProgramCounter, which is 2 bytes.

X and Y are general purpose registers. 

A is the acumulator. 

SP is the stack pointer. This is used to store the memory addresses when a program executes a function. The way it 
works is simply by pointing to a specific memory address. The 6502 reserves 100 bytes for the Stack, the SP simply 
points to different addresses within those 100 bytes. Several instructions such as `JMP` (Jump) or `JSR` (Jump to 
Subroutine) affect the stack.

The program counter indicates what is the NEXT byte to read from, this is used by CPU to read a byte to determine 
which opcode and addressing mode to use. Of course the ProgramCounter is increased every time the CPU read from 
memory.

### PPU 

This is the Picture Processing Unit. It is a custom chip by Nintendo and is the responsible of drawing into the 
screen, you can think of it as the GPU of the system (remember, the NES is from 1981, so understand that the term 
GPU really is a very loose analogy)

TODO moriano: Put a link to CHR ROM

### Cartridge

The software of each videogame was stored in a cartridge, the cartridges contained everything that the game needed to 
run, this can be loosely defined as the actual sofware in one end, the PGR ROM and then graphics used for the game, 
the CHR ROM. 

Each cargridge has a `mapper`. The term mapper comes from `memory mapping`. Mappers essentially allow to map memory 
hardware into the CPU and PPU memory addresses. Mappers exists to overcome the system limitations (TODO MORIANO 
expand)

Each cartridge file (a .nes file) has a 16byte heather, here is the structure

|Bytes|Description|
|----|-----|
|0-3| 0x4E 0x45 0x53 0x1A. This is the workd NES follow by MS-DOS end of file char|
| 4 | Size of PGR ROM in 16KBytes units |
| 5 | Size of CHR ROM in 8KB units (value 0 means the cartridge uses CHR RAM)|
| 6 | Flags 6 - Mapper, mirroring, battery, trainer |
| 7 | Flags 7 - Mapper, VS/PlayChoice |
| 8 | Flags 8 - PGR-RAM size (rarely used) |
| 9 | Flags 9 - TV System (rarely used) |
| 10 | Flags 10 - TV system, PRG-RAM presence (unofficial, rarely used extension) |
| 11-15 | Unused padding |

As per Flag6, each of the bits indicate the following

```
76543210
||||||||
|||||||+- Nametable arrangement: 0: vertical arrangement ("horizontal mirrored") (CIRAM A10 = PPU A11)
|||||||                          1: horizontal arrangement ("vertically mirrored") (CIRAM A10 = PPU A10)
||||||+-- 1: Cartridge contains battery-backed PRG RAM ($6000-7FFF) or other persistent memory
|||||+--- 1: 512-byte trainer at $7000-$71FF (stored before PRG data)
||||+---- 1: Alternative nametable layout
++++----- Lower nybble of mapper number
```

And for Flag7

```
76543210
||||||||
|||||||+- VS Unisystem
||||||+-- PlayChoice-10 (8 KB of Hint Screen data stored after CHR data)
||||++--- If equal to 2, flags 8-15 are in NES 2.0 format
++++----- Upper nybble of mapper number
```

Lets consider a simple cartridge: The nestest.nes one (see https://www.qmtpro.com/~nes/misc/nestest.txt)

* ROM Size = 1 * 16KB  = 16384 bytes
* CHR Size = 1 * 8KB = 8192 bytes
* PGR RAM Size = 0. Which means 8KB for compatibility
* Mapper number 0

This is possibly one of the simplest cartridges you can deal with. Now lets focus on the mapper 0 and what it means.

Mapper 0 is documented at https://www.nesdev.org/wiki/NROM. 

Now, here is something interested, the PGR ROM (remember, in total 16KB) is addressed by the cpu in indexes 
0x8000 to 0xBFFF. However, the CPU memory map goes all the way to 0xFFFF, so what happens then? well this mapper 
establishes that 

* If the cartridge has 32KB of PGR ROM, then nothing special happens: those 32KB are between 0x8000 and 0xFFFF
* If the cartridge has 16KB of PGR ROM (the case of nestest.nes) then we have to do some trickery: the actual data 
sits between 0x8000 to 0xBFFF, and then that is mirrored in 0xC000 to 0xFFFF. This means that reading from 0x8000 is 
the same as reading from 0xC000.

Each mapper has its own trickery. 

#### CHR ROM

TODO MORIANO

#### PGR ROM

TODO MORIANO

