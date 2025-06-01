# LocoNES

A, not completed, always in progress, NES emulator in Java

The vast majority of this file comes from https://www.nesdev.org/wik


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
6. Status register P

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

The status register holds a number of flags, see the CPU implementation to understand how it works. 

### PPU 

This is the Picture Processing Unit. It is a custom chip by Nintendo and is the responsible of drawing into the 
screen, you can think of it as the GPU of the system (remember, the NES is from 1981, so understand that the term 
GPU really is a very loose analogy)

TODO moriano: Put a link to CHR ROM

#### PPU Registers (sourced from https://www.nesdev.org/wiki/PPU_registers)

A total of 8 registers are available, they are in addresses 0x2000 to 0x2007

Lets go one by one

##### PPUCTRL Register 0x2000

PPUCTRL (the "control" or "controller" register) contains a mix of settings related to 
rendering, scroll position, vblank NMI, and dual-PPU configurations. After power/reset, 
writes to this register are ignored until the first pre-render scanline.


```
7  bit  0
---- ----
VPHB SINN
|||| ||||
|||| ||++- Base nametable address
|||| ||    (0 = $2000; 1 = $2400; 2 = $2800; 3 = $2C00)
|||| |+--- VRAM address increment per CPU read/write of PPUDATA
|||| |     (0: add 1, going across; 1: add 32, going down)
|||| +---- Sprite pattern table address for 8x8 sprites
||||       (0: $0000; 1: $1000; ignored in 8x16 mode)
|||+------ Background pattern table address (0: $0000; 1: $1000)
||+------- Sprite size (0: 8x8 pixels; 1: 8x16 pixels – see PPU OAM#Byte 1)
|+-------- PPU master/slave select
|          (0: read backdrop from EXT pins; 1: output color on EXT pins)
+--------- Vblank NMI enable (0: off, 1: on)
```

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

This is the Character ROM. It contains the graphics of the game. The way in which the graphics are stored is in 
blocks of 16bytes. Each block of 16bytes represents a tile. A tile is made of 8x8 pixels. 

The way to visualize each tile follows this: 

1. Take a block of 16bytes of the CHR ROM
2. Divide them into two blocks of 8bytes. 
3. Now for each block of 8bytes, represent them as an 8x8 binary square. 
An example of this, imagine we have 16 bytes as 
High byte 0xC1, 0xC2 0x44 0x48 0x10 0x20 0x40 0x80
Low byte  0x01  0x02 0x04 0x08 0x16 0x21 0x42 0x87

Now lets represent the high 8bytes as 8x8 binary square, where each value is either 0 or 1
```
  0x41  0 1 0 0 0 0 0 1
  0xC2  1 1 0 0 0 0 1 0
  0x44  0 1 0 0 0 1 0 0
  0x48  0 1 0 0 1 0 0 0     ===> Notice how this high bytes shows the "picture" =>  1/
  0x10  0 0 0 1 0 0 0 0                                     No, really, look at it, it is a one with a fraction line
  0x20  0 0 1 0 0 0 0 0
  0x40  0 1 0 0 0 0 0 0
  0x80  1 0 0 0 0 0 0 0
```

And now the low byte
```
  0x01  0 0 0 0 0 0 0 1
  0x02  0 0 0 0 0 0 1 0
  0x04  0 0 0 0 0 1 0 0
  0x08  0 0 0 0 1 0 0 0    ===> Notice how this high bytes shows the "picture" =>  /2
  0x16  0 0 0 1 0 1 1 0
  0x21  0 0 1 0 0 0 0 1
  0x42  0 1 0 0 0 0 1 0
  0x87  1 0 0 0 0 1 1 1
```

Ok, now we `add` both of them, the low byte bit represents either 0x10 or 0x00, the high byte 
bit represents either 0x01 or 0x00, so by adding them we have a square with 4 possible values 
(0 to 3)

```
  0 1 0 0 0 0 0 3
  1 1 0 0 0 0 3 0
  0 1 0 0 0 3 0 0
  0 1 0 0 3 0 0 0
  0 0 0 3 0 2 2 0 ===> Now make a bit of an effort, we have the "picture" 1/2 here
  0 0 3 0 0 0 0 2      the actual values 0 1 2 and 3 will represent different colors, but for that we need to
  0 3 0 0 0 0 2 0      load the frame palettes
  3 0 0 0 0 2 2 2
```

By doing this, we get a single tile. In total games will have 2 sets of 16 by 16 tiles. That 
means a total of 2 sets of 256 tiles each, so that means 512 tiles. 



#### PGR ROM

The Program ROM is where the actual video game is defined. The ProgramCounter will indicate the 
CPU where to start executing the game, after that it is up to the CPU to simply fetch the operation 
code alongside with the paramters (if any), increment the ProgramCounter and execute the actual 
instructions.

### Memory

First and foremost, we must not confuse the term memory with `main memory`. These are two different ideas.

When I refer to memory here i refer to the entire memory that the NES system can use, that includes the 
`main memory` as well as other memories. To be more specific, keep in mind that when a cartridge was inserted
into the NES, it essentially connected the cartridge memory to the system, so in a way CHR ROM and PGR ROM 
are also part of the memory. 

Generally speaking the NES had 2KB of main memory (aka RAM) and 2KB of video memory. But again on top of this we 
need to add the cartridge. 

We can divide the memory into two areas: The CPU Memory and and the PPU Memory spaces.

#### CPU Memory map

|Address range|Size | Description|
|----|-----|-----|
| 0x0000-0x07FFF| 2KB | Main memory (RAM)|
| 0x0800-0x0FFF | 2KB|Mirrors 0x0000-0x07FFF  |
| 0x1000-0x17FF | 2KB |Mirrors 0x0000-0x07FFF  |
| 0x1800-0x1FFF | 2KB |Mirrors 0x0000-0x07FFF  |
| 0x2000-0x2007 |    7B |PPU Registers |
| 0x2008-0x3FFF | ~8KB | Mirrors of 0x2000-0x2007|
| 0x4000-0x4017 | 24B | APU and I/O functionality |
| 0x4020-0xFFFF | ~48KB| Unmapped, for cartridge use |
| 0x6000-0x7FFF | 8KB | Cartridge RAM, when present |
| 0x8000-0xFFFF | 32K | Cartridge ROM and mapper registers |

Notice something interesting. The CPU memory map can only ~98KB of cartridge memory, there are however 
cartridges that are larger than that. That is where mappers come into play. Mappers translate addresses 
in a way so that the CPU can access different memory banks within a cartridge. Remember that mappers where 
actually chunks of hardware that were part of the cartridge itself.

#### PPU Memory map

|Address range|Size | Description|
|----|-----|-----|
| 0x0000-0x0FFF | 4KB | Pattern table 0 (CHR ROM) |
| 0x0000-0x1FFF | 4KB | Pattern table 1 (CHR ROM) |
| 0x2000-0x23FF | 1KB | NameTable 0 |
| 0x2400-0x27FF | 1KB | NameTable 1 |
| 0x2800-0x2BFF | 1KB | NameTable 2 | 
| 0x2C00-0x2FFF | 1KB | NameTable 3 | 
| 0x3000-0x3EFF | 3.75KB | Unused |
| 0x3F00-0x3F1F | 32B | Palette RAM index (internal to PPU) |
| 0x3F20-0x3FFF | 224 | Mirrors 0x3F00-0x3F1F (internal to PPU) |
