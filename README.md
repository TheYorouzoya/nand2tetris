# nand2tetris

This repo contains the collection of all the non-trivial projects I made during the two-part [nand2tetris](https://www.coursera.org/learn/build-a-computer) course. As the description says, the course is about building a computer from scratch, all the way from logic gates to a proper program running on top of the hardware we've built.

Since this repo contains parts from the hardware interface layer (the assembler) and above, I will briefly describe the Hack Computer's specs before moving on to the individual projects.

## The Hack Computer

The Hack machine is a 16-bit von Neumann machine designed to execute programs written in the Hack machine language. It consists of,
- A CPU
- Two separate memory modules: one for *instruction memory* (instruction ROM) and one for *data memory* (the RAM)
- Two memory-mapped I/O devices: a *screen* and a *keyboard*.

The CPU consists of an ALU and three registers named *Data Register* (D), *Address Register* (A), and *Program Counter* (PC). The D register is used solely for storing and operating on data values while the A register is multi-purpose. It can either store a data value for computation, select an address in the *instruction memory*, or select an address in the *data memory*.

Below is my arrangement for the Hack CPU circuitry as it was implemented in HDL. There is probably a more efficient way to do this, but this is what I came up with and what worked in the simulator. The HDL file itself is included [here](./CPU.hdl) as well.

![Image shows the Hack CPU circuit arrangement](./docimages/hack-cpu.png)

The point here is that *this is a really simple CPU*. None of the modern CPU magic (the pipelining, caches, memory controller, register files, functional units, etc.) is to be found here. So I hope this sets the proper precendence for what to expect out of the projects in this repo. On their own, they are quite simple, but when put together, they present the picture of what a marvel the modern computer is. With that, let us start with the first project, the [Hack Assembler](./HackAssembler/).

## Hack Assembler

The Hack Assembly language only consists of two kinds of instructions (i.e., there's only 1 bit for the op code): an ***A-instruction*** or a ***C-instruction***. Below are the instruction set mnemonics,

![Image showing the Hack Assembler instruction set](./docimages/instruction-set.png)

The A-instruction is the only way to input a constant into the computer. The instruction's primary purpose is to point to an address so as to either store the result of a C-instruction in the RAM or to jump to the address location in the instruction memory after executing a C-instruction. The C-instruction, on the other hand, is the most involved and performs all the computation the Hack CPU supports.

Together with these two, the Hack Assembly also supports adding labels in code to jump to, along with single-line comments starting with a "//". Furthermore, there are some special symbols/keywords which are reserved and point to locations in memory (mainly for pointing to memory segments for the VM). There's another one of my drawings [here](./HackAssembler/Hack%20Assembly%20Language.png) which goes over all of this with an example.

The assembler consumes a hack assembly program file and does the following:
- Cleans up all whitespace and comments
- Translates all symbols to the appropriate instruction addresses
- Convert all instructions and symbols to binary
- Output the resulting binary

The assembler performs this by doing two passes over the program code -- one to clean up and tally the symbols in a symbol table; the second one, to generate the required binaries.

There are two main caveats here, though. The first, and probably the most egregious one, is that the assembler assumes that the given program is error-free. This was part of the specification, so no error-checking is performed and no useful feedback is given. If a line doesn't contain valid code, it is completely ignored. Now, for the second one. Because the Hack program for this course was to be run on a CPU emulator, the final output isn't a proper binary file but rather a text file where each line contains an instruction in 0s and 1s (the ASCII binary, as I like to call it). The emulator would consume this file and run the program.

## VM Translator

With the machinery and the interface (the assembler) done, we can now move on to building another layer of abstraction on top of this. Primarily because writing assembly is very time consuming and representing complex ideas in a high-level language is much easier.

The high-level language we're building toward is called ***Jack***. It is an object-oriented language with syntax similar to Java (more details to come with the compiler). And just like how Java goes through a two-stage compilation process -- where it is first compiled to an intermediate bytecode, which is then converted and run on the target machine by the Java Virtual Machine (the JVM) -- Jack is also first compiled to an intermediate VM code, which is then converted by the VM to Hack assembly.

This project covers the specifics of the Virtual Machine which'll sit on top of the assembly layer. Our program will basically translate VM code to Hack assembly. Here's another one of my (pretty) drawings detailing the translation specifications:

![Image showing all VM commands with their assembly translations](./VMTranslator/Virtual%20Machine%20Translator.png)

### VM Commands

The VM in question is a stack-based VM which only supports a single data type: *a 16-bit integer*. It provides the following 4 types of commands:
- Basic commands: `push`, `pop`
- Arithmetic and logic commands: `add`, `sub`, `eq`, `gt`, `lt`, `and`, `or`, `not`, `neg`
- Branching commands: `label LABEL_NAME`, `if-goto LABEL_NAME`, `goto LABEL_NAME`
- Function commands: `function FUNCTION_NAME nVars`, `call FUNCTION_NAME`, `return`

The VM code follows the following syntax: `command segment index`

The `command` here refers to one of the 4 command types above. The `segment` part refers to one of the virtual memory segments that the VM provides, and the `index` part is an offset into that memory segment. In certain cases, the `segment` and `index` parts may be omitted.

### VM Stack and Virtual Memory Segments

The stack in this stack-based VM is maintained by a stack pointer stored at the very first location in RAM (address 0x0000). The assembler reserves a special keyword called `SP` to point to it. Think of it as a dedicated stack-pointer register (in fact, the assembler denotes the first 16 memory addresses with `R0` to `R15` as the 16 registers; mind you, there are no actual registers in the CPU's architecture). Upon initialization of the VM, we point this stack pointer to RAM location 0x0100 (256) to mark the start of the stack. During execution, the stack grows or shrinks appropriately.

You might have noticed that there was no mention of function calls in the assembly language's specifications. That is because in the Hack system, function calls start on the VM level. To facilitate this, the VM provides access to different virtual memory segments via segment pointers. There are 8 such `segment` values:

Segment | Assembly Mnemonic | RAM location | Role
:-- | :--: | :--: | :--:
argument | ARG | RAM[2] or 0x0002 | points to the segment containing the function arguments for the current function
local | LCL | RAM[1] or 0x0001 | points to the segment containing the local variables declared in the current function
static | Foo.i | RAM[16-255] or 0x0010 to 0x00ff | segment containing the entire program's static/global variables. For a vm file named `Foo.vm`, the Assembler will generate a unique label for each of the static variables encountered. This is what the `Foo.i` denotes, where `i` is the i'th variable in the file.
constant | - | - | ***not a segment***. The VM uses this to push constant values onto the stack.
this | THIS | RAM[3] or 0x0003 | 
that | THAT | RAM[4] or 0x0004
pointer | THIS/THAT | RAM[3]/RAM[4] or 0x0003/0x0004
temp | temp i | RAM[5-12] or 0x0005 to 0x000c


These segments will point to different parts of the stack during the execution of a program (except for the `static` and `temp` segments, which have a fixed size and location on the RAM).

In modern hardware, these stack semantics are typically part of the assembly mnemonics themselves (take the `x86` for example). The segments are usually maintained via dedicated registers (like the `%rsp`) and different adderssing modes facilitate access into a segment.


### Function Calls in the VM

The VM provides the following three function-related primitives:
- `function FUNCTION_NAME nVARS`
- `call FUNCTION_NAME`
- `return`

Each VM function starts with a `function` command, and ends with a `return` command.

In your typical modern implementation, primitive-type arguments are usually passed via registers (`%rsi`, `%rdi`, `%rdx`, etc.). Composite types are passed by pushing the appropriate pointer onto the stack before the function call. Upon return, the return value is typically stored in a dedicated register as well (`%rax` for example). The responsibility of saving and restoring the register context between function calls is shared among the hardware and the compiler.

Our VM does a similar thing. During initialization (the first primitive), the VM inserts code to make space for all the local variables on the stack. The number of variables is denoted by the `nVars` value in the declaration. This value is actually calculated and placed there by the compiler.

Upon encountering a function call statement (the second primitive in the list), we save the caller's stack frame context onto the stack before making the function call. Upon return, we store this context from the stack and continue execution. Here's a snapshot of the stack during a hypothetical execution (from the [original picture](./VMTranslator/Virtual%20Machine%20Translator.png)),

![Image showing the state of the VM stack on the RAM during a function call](./docimages/function-call.png)

The responsibility of pushing the function arguments and the return value onto the stack lies with the compiler. Meanwhile, the VM Translater generates code to save and restore the context.