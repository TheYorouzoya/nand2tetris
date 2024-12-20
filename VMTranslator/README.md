# VM Translator

A Java program which translates Hack VM code into Hack assembly code.

## Usage

*Requires Java 17 and above*

Compile the Java files first,

```shell
prompt> javac VMTranslator.java
```

then run,

```shell
prompt> java VMTranslator FILE_NAME.vm
```

to process an individual file. The output will be a hack assembly file (a .asm file) in the same folder as the input file.

You can also input a directory instead of a file and the program will process all valid VM files in the directory (the program doesn't recursively look for files in sub-directories though).

## Documentation

Since writing assembly is very time consuming and representing complex ideas in a high-level language is much easier, we'll work our way toward a high-level language called **Jack**.

Jack is an object-oriented language with syntax similar to Java (more details to come with the compiler). And just like how Java goes through a two-stage compilation process -- where it is first compiled to an intermediate bytecode, which is then converted and run on the target machine by the Java Virtual Machine (the JVM) -- Jack is also first compiled to an intermediate VM code, which is then converted by the VM to Hack assembly.

This project covers the specifics of the Virtual Machine which'll sit on top of the assembly layer. Our program will basically translate VM code to Hack assembly. Here's another one of my (pretty) drawings detailing the translation specifications.

![Image showing the different aspects of the translation process](./../docimages/Virtual%20Machine%20Translator.png)

## VM Commands

The VM in question is a stack-based VM which only supports a single data type: *a 16-bit integer*. It provides the following 4 types of commands:
- Basic commands: `push`, `pop`
- Arithmetic and logic commands: `add`, `sub`, `eq`, `gt`, `lt`, `and`, `or`, `not`, `neg`
- Branching commands: `label LABEL_NAME`, `if-goto LABEL_NAME`, `goto LABEL_NAME`
- Function commands: `function FUNCTION_NAME nVars`, `call FUNCTION_NAME nArgs`, `return`

The VM code follows the following syntax: `command segment index`

The `command` here refers to one of the 4 command types above. The `segment` part refers to one of the virtual memory segments that the VM provides, and the `index` part is an offset into that memory segment. In certain cases, the `segment` and `index` parts may be omitted.

## VM Stack and Virtual Memory Segments

The stack in this stack-based VM is maintained by a stack pointer stored at the very first location in RAM (address 0x0000). The assembler reserves a special keyword called `SP` to point to it. Think of it as a dedicated stack-pointer register (in fact, the assembler denotes the first 16 memory addresses with `R0` to `R15` as the 16 registers; mind you, there are no actual registers in the CPU's architecture). Upon initialization of the VM, we point this stack pointer to RAM location 0x0100 (256) to mark the start of the stack. During execution, the stack grows or shrinks appropriately.

You might have noticed that there was no mention of function calls in the assembly language's specifications. That is because in the Hack system, function calls start on the VM level. To facilitate this, the VM provides access to different virtual memory segments via segment pointers. There are 8 such `segment` values:

Segment | Assembly Mnemonic | RAM location | Role
:-- | :--: | :--: | :--:
argument | ARG | RAM[2] or 0x0002 | points to the segment containing the function arguments for the current function
local | LCL | RAM[1] or 0x0001 | points to the segment containing the local variables declared in the current function
static | Foo.i | RAM[16-255] or 0x0010 to 0x00ff | segment containing the entire program's static/global variables. For a vm file named `Foo.vm`, the Assembler will generate a unique label for each of the static variables encountered. This is what the `Foo.i` denotes, where `i` is the i'th static variable in the file.
constant | - | - | ***not a segment***. The VM uses this to push constant values onto the stack.
this | THIS | RAM[3] or 0x0003 | used by the compiler to point to Jack objects in object methods
that | THAT | RAM[4] or 0x0004 | used by the compiler to assist with array indexing
pointer | THIS/THAT | RAM[3]/RAM[4] or 0x0003/0x0004 | used by the compiler to refer to objects/arrays
temp | temp i | RAM[5-12] or 0x0005 to 0x000c | used by the compiler to temporarily store intermediate computations


These segments will point to different parts of the stack during the execution of a program (except for the `static` and `temp` segments, which have a fixed size and location on the RAM). Together, they encompass the currently executing function's stack frame.

In modern hardware, these stack semantics are typically part of the assembly mnemonics themselves (take the `x86` for example). The segments are usually maintained via dedicated registers (like the `%rsp`) and different adderssing modes facilitate access into a segment.


## Function Calls in the VM

The VM provides the following three function-related primitives:
- `function FUNCTION_NAME nVars`
- `call FUNCTION_NAME nArgs`
- `return`

Each VM function starts with a `function` command, and ends with a `return` command.

In your typical modern implementation, primitive-type arguments are usually passed via registers (`%rsi`, `%rdi`, `%rdx`, etc. for example), meanwhile, composite types are passed by pushing the appropriate pointer onto the stack before the function call. Upon return, the return value is typically stored in a dedicated register as well (`%rax` for example). The responsibility of saving and restoring the register context between function calls is shared among the hardware and the compiler.

Our VM does a similar thing. During initialization (the first primitive), the VM inserts code to make space for all the local variables on the stack. The number of variables is denoted by the `nVars` value in the declaration. This value is actually calculated and placed there by the compiler.

Upon encountering a function call statement (the second primitive in the list), we save the caller's stack frame context onto the stack before making the function call. The `nArgs` value is inserted by the compiler after it pushes the required arguments on the stack. Upon return, we restore this context from the stack and continue execution. Here's a snapshot of the stack during a hypothetical execution,

![Image showing the state of the VM stack on the RAM during a function call](./../docimages/function-call.png)

The responsibility of pushing the function arguments and the return value onto the stack lies with the compiler. Meanwhile, the VM generates code to save and restore the function context. Our VM Translator will do just that as follows,

- Upon encountering a function declaration `function FUNCTION_NAME nVars`:
    - initialize a function label as `filename.FUNCTION_NAME` for the assembler.
    - push 0 into the stack `nVars` times to allocate space on the stack.
    - translate the rest of the function body

- Upon encountering a function call statement `call FUNCTION_NAME nArgs`:
    - assume that all function arguments have already been pushed onto the stack by the compiler.
    - assign a return address label as `filename.FUNCTION_NAME$ret.i` and push the return address on the stack. The `i` here refers to a global counter to generate unique labels for each function call encountered.
    - push the caller function's base pointer values for the `LCL`, `ARG`, `THIS`, and `THAT` segments onto the stack, effectively saving its stack frame.
    - set the `ARG` segment to point to the start of the arguments pushed onto the stack, i.e., `SP - 5 - nArgs` (since just we pushed the return address + 4 segments).
    - set the `LCL` segment to point to the stack pointer's current location. Since we allocate space immediately upon entering the function definition, this will make it point to the function's own `local` segment.
    - initialize code to make the jump to the function body by loading the function definition label `filename.FUNCTION_NAME` (initialized previously) into the A-register and making the jump.
    - insert the return address label (`filename.FUNCTION_NAME$ret.i`) into the next line so that the return address points to the line after the function call.

- Upon encountering a `return` statement within a function:
    - assume that the function's return value has already been pushed onto the stack by the compiler.
    - since the calling function's context was previously pushed onto the stack just past the current function's `LCL` segment, assign a temporary variable called `frame` to it. This will help us restore the caller's stack frame.
    - assign another temporary variable `returnAdd` to hold the return address stored at `frame - 5`.
    - reposition the function's return value, which is currently stored at the top of the stack, to where the calling function's stack would begin, i.e., at start of the current `ARG` segment (since that's where our caller pushed the arguments before making the function call).
    - reposition the stack pointer just past this `ARG` base value, i.e., `ARG + 1`, effectively destroying the current function's call stack and reclaiming memory.
    - restore the `ARG`, `LCL`, `THIS`, and `THAT` segment base address values for the calling function by pulling them from the `frame` variable as follows:
        - `THAT = frame - 1`
        - `THIS = frame - 2`
        - `ARG = frame - 3`
        - `LCL = frame - 4`
    - finally, intialize the jump back to the calling function by inserting a `goto returnAdd` statement.


## VM Command Operations

Aside from function calls and the basic `push`/`pop` commands, the VM also provides a bunch of arithmetic, logical, and branching commands. I'll briefly explain how the commands work and what their translation process entails.

- `push` and `pop` commands require `segment` and `index` values to fetch/store results into the pointed segment. Their assembly translation entails loading the proper segment address/value before performing the `push` or `pop` operation.
- Arithmetic and logical commands can be divided into two categories: single operand (`not` and `neg`) and two-operand (`add`, `sub`, `eq`, `gt`, `lt`, `and`, and `or`). The operands values are assumed to be stored on the top of the stack, so the assembly translation effectively pops those values off the stack, performs the computation, and pushes the resulting value onto the stack.
- Branching commands `label LABEL_NAME`, `goto LABEL_NAME`, and `if-goto LABEL_NAME` are simple assembly translations to an unconditional or a conditional jump. In case of `if-goto` the boolean to be checked is assumed to be at the top of the stack.

The translation process usually involves juggling the data between the A-register, the D-register, and the stack to get to the required address, performing computations, and incrementing or decrementing the stack pointer appropriately. For example, a `pop segment index` command, which pops the stack's top value into the given virtual memory segment location, would translate to following assembly code:

> [!NOTE]
> *since we have no extra registers, the stack and the segment base values need an extra dereference to get to the actual location. Also, the `M` variable in Hack assembly refers to whatever memory location the A-register currently points to.* 

```
@segment    // select the segment's base RAM location (base register)
D=M         // dereference to get the base pointer's address and store 
            // into D-register
@index      // store the segment offset (a constant value) into the A-register
A=D+A       // advance the segement pointer by this offset value to get to the 
            // required address
D=A         // store this final address into the D-register
@SP         // select the stack pointer address
M=M-1       // decrement stack pointer
A=M+1       // point the A-register to go back to the previous stack pointer value
M=D         // store the segment destination address value (computed previously)
            // on the stack
@SP         // select the stack pointer address again
A=M         // dereference the stack pointer address to get to the stack's 
            // current address
D=M         // dereference again to get the popped value and store it in the 
            // D-register
A=A+1       // increment A-register by one to get to the previously stored
            // segment address
A=M         // dereference to go the actual segment location
M=D         // store the popped value at the destination
```

## VM Bootstrap Code

Finally, since our VM Translater can handle multi-file programs, we need to insert some bootstrap code so that we have a proper entry point to start execution from. Later on, we'll have a barebones OS which'll, by default, be compiled with every program and linked when the system boots. This OS contains a `Sys` module which contains a `Sys.init` function whose job is to call the `main` subroutine for the current program (along with initializing the OS modules) which'll serve as our entry point.

This means that there must exist a file called `Main.vm` which will contain a `main` function to act as the entry point into the program. So, whenever the VM Translater is passed a directory as input, it'll automatically insert this bootstrap code as the very first block of assembly code. This bootstrap code initializes the stack pointer and sets up a jump to the `Sys.init` function.
