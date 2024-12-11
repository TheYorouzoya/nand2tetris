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

# Hack Assembler

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

# VM Translator

With the machinery and the interface (the assembler) done, we can now move on to building another layer of abstraction on top of this. Primarily because writing assembly is very time consuming and representing complex ideas in a high-level language is much easier.

The high-level language we're building toward is called ***Jack***. It is an object-oriented language with syntax similar to Java (more details to come with the compiler). And just like how Java goes through a two-stage compilation process -- where it is first compiled to an intermediate bytecode, which is then converted and run on the target machine by the Java Virtual Machine (the JVM) -- Jack is also first compiled to an intermediate VM code, which is then converted by the VM to Hack assembly.

This project covers the specifics of the Virtual Machine which'll sit on top of the assembly layer. Our program will basically translate VM code to Hack assembly. Here's another one of my (pretty) drawings detailing the translation specifications:

![Image showing all VM commands with their assembly translations](./VMTranslator/Virtual%20Machine%20Translator.png)

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
static | Foo.i | RAM[16-255] or 0x0010 to 0x00ff | segment containing the entire program's static/global variables. For a vm file named `Foo.vm`, the Assembler will generate a unique label for each of the static variables encountered. This is what the `Foo.i` denotes, where `i` is the i'th variable in the file.
constant | - | - | ***not a segment***. The VM uses this to push constant values onto the stack.
this | THIS | RAM[3] or 0x0003 | used by the compiler to point to Jack objects in object methods
that | THAT | RAM[4] or 0x0004 | used by the compiler to assist with array indexing
pointer | THIS/THAT | RAM[3]/RAM[4] or 0x0003/0x0004 | used by the compiler to refer to objects
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

![Image showing the state of the VM stack on the RAM during a function call](./docimages/function-call.png)

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

With this, one part of Jack's two-part compilation process is done. Now, let us move on to the Jack Compiler.

# Jack Compiler

Jack is a simple, Java-like, object-based programming language. It is designed to lend itself nicely to common compilation techniques. It is weakly typed and features only 3 primitive data types, but provides access to user-defined data types via objects. Let's start with an example Jack program, and then we'll list the language grammar and specfications.

```JavaScript
/** Inputs a sequence of integers, and computes their average. */
class Main {
    function void main() {
        var Array a;        // Jack arrays are not typed
        var int length;
        var int i, sum;
        
        let i = 0;
        let sum = 0;
        let length = Keyboard.readInt("How many numbers? ");
        let a = Array.new(length);  // constructs the array
        
        while (i < length) {
            let a[i] = Keyboard.readInt("Enter a number: ");
            let sum = sum + a[i];
            let i = i + 1;
        }

        do Output.printString("The average is: ");
        do Output.printInt(sum / length);
        do Output.println();
        return;
    }
}
```
As you can see, we've got *classes*, *methods*, *functions*, *variables*, *arrays*, *expressions*, *comments*, *I/O*, and *strings* (I/O, arrays, and strings are actually provided by the Jack OS library, which we'll discuss later).

## Syntax

A Jack program is a sequence of tokens, separated by an arbitrary amount of white space and comments. Tokens can be symbols, reserved words, constants, and identifiers. Here's a list of these syntax elements:

### 1. White space and comments:

Space characters, newline characters, and comments are ignored, but the following comment formats are supported:
- `// Comment to the end of line`
- `/* Comment until closing */`
- `/** Comment aimed at software tools that extract API documentation. */`

### 2. Symbols:
- `()`: Used for grouping arithmetic expressions and for enclosing argument-lists (in subroutine calls) and parameter-lists (in subroutine declarations)
- `[]`: Used for array indexing
- `{}`: Used for grouping program units and statements
- `,`: Variable-list separator
- `;`: Statement terminator
- `=`: Assignmment and comparison operator
- `.`: Class membership
- `+`, `-`, `*`, `/`. `&`, `|`, `~`, `<`, `>` Operators

### 3. Reserved words:
- Program Components: `class`, `constructor`, `method`, `function`
- Primitive types: `int`, `boolean`, `char`, `void`
- Variable declarations: `var`, `static`, `field`
- Statements: `let`, `do`, `if`, `else`, `while`, `return`
- Constant values: `true`, `false`, `null`
- Object reference: `this`

### 4. Constants:
- ***Integer Constants*** are values in the range 0 to 32767. Negative integers are not constants but rather expressions consisting of a unary minus operator applied to an integer constant. The resulting valid range of values is -32768 to 32767 (the former can be obtained using the expression -32767-1).
- ***String Constants*** are enclosed within double quote `"` characters and may contain any character except newline or double quote. These characters are supplied by the OS functions `String.newLine()` and `String.doubleQuote()`.
- ***Boolean Constants*** are `true` and `false`.
- The `null` constant signifies a null reference.

### 5. Identifiers:
- Identifiers are composed from arbitrarily long sequences of letters (A-Z, a-z), digits (0-9), and "_" (underscore). The first character must either be a letter or "_".
- The language is case sensitive: `x` and `X` are treated as different identifiers.

## Jack Grammar

With the lexical elements defined, let us move on to the grammar portion -- the program structure, statements, and expressions.

> *Grammar Conventions:* 
> - `'x'`     : text that appears verbatim
> - *x*       : lexical element
> - *x y*     : *x* appears, then *y* appears
> - *x | y*   : either *x* or *y* appears
> - *x?*      : *x* appears 0 or 1 times
> - *x**      : *x* appears 0 or more times
> - *(x y)*   : grouping of *x* and *y*

### Progarm Structure

A Jack program is a collection of classes, each appearing in a separate file. The compilation unit is a class, which is a sequence of tokens structured according to the following context free syntax:

Structure | Syntax
:-- | :--:
**class** | `'class'` className `'{'` classVarDec* subroutineDec* `'}'`
**classVarDec** | (`'static'` \| `'field'`) type varName (`','`varName)* `';'`
**type** | `'int'` \| `'char'` \| `'boolean'` \| className
**subroutineDec** | (`'constructor'` \| `'function'` \| `'method'`) ('`void'` \| type) subroutineName `'('`parameterList`')'`subroutineBody
**parameterList** | ( (type varName) (`','`type varName)*)?
**subroutineBody** | `'{'`varDec* statements`'}'`
**varDec** | `'var'`type varName(`','`varName)*`';'`
**className** | identifier
**subroutineName** | identifier
**varName** | identifier

### Statements

Statement | Syntax
:-- | :--:
**statements** | statement*
**statement** | letStatement \| ifStatement \| whileStatement \| doStatement \| returnStatement
**letStatement** | `'let'` varName (`'['`expression`']'`)?`'='`expression`';'`
**ifStatement** | `'if'` `'('`expression`')'` `'{'`statements`'}'`(`'else'` `'{'`statements`'}'`)?
**whileStatement** | `'while'` `'('`expression`')'` `'{'`statements`'}'`
**doStatement** | `'do'`subroutineCall`';'`
**ReturnStatement** | `'return'`expression?`';'`

### Expressions

Expression | Syntax
:-- | :--:
**expression** | term (op term)*
**term** | integerConstant \| stringConstant \| keywordConstant \| varName \| varName`'['`expression`']'` \| subrouineCall \| `'('`expression`')'` \| unaryOp term
**subroutineCall** | subroutineName `'('`expressionList`')'` \| (className \| varName)`'.'`subroutineName `'('`expressionList`')'`
**expressionList** | (expression (`','`expression)* )?
**op** | `'+'` \| `'-'` \| `'*'` \| `'/'` \| `'&'` \| `'\|'` `'<'` \| `'>'` \| `'='`
**unaryOp** | `'-'` \| `'~'`
**KeywordConstant** | `'true'` \| `'false'` \| `'null'` \| `'this'`

You can use the grammar convention guide to read how a particular entity is expressed. It should be apparent that these can also be recursive (so a `term` can contain an `expression` which can contain `term`s, and so on). Let's take the **expression** part as an example. According to the table, an expression is of type `term (op term)*`, that is, a term which is optionally followed by 0 or more groups of terms separated by operators. Looking at the specification for a `term`, you can see that it can take on a variety of values. So the expression `a = Array.new(length + 3);` is a term which consists of a `varName` (an identifier) followed by an `op` (operator), which is followed by a `subroutineCall` which itself has an `expression` nested inside its `expressionList`.

## Compilation

Our Jack compiler is written in Java. It performs the compilation process in two phases:
1. Tokenize the given program text by, first, cleaning up all the whitespace and comments, and then generating a stream of parsed tokens.
2. Perform a depth first traversal of the syntax tree and output the appropriate VM code as we go along.

### Tokenization

We use Java enums to store the different syntax elements and literals. For example, `Keywords.java` holds all the Jack language keywords in an enum. Similarly, the `VarKind.java` file stores the kinds of variables (static, field, argument, class, etc) found in Jack.

The given program file(s) is sent to `Tokenizer.java` which eliminates all the whitespace and comments, and returns an array of individual tokens extracted from the program. We define token types as one of the following inside `Tokens.java`:
- Keyword
- Symbol
- Identifier
- Integer constant
- String constant

The `Token.java` file, on the other hand, defines a `Token` datatype, which breaks down a token into a token type and a token value field. It provides two constructors: one, where it consumes a string token, parses it, and extracts the token's type and value fields. The other is where the type and value fields can be instantiated directly.

As the Tokenizer makes its way through the input file, it sends each token string to this Token class and stores the resulting Token object into a resizeable array (an ArrayDeque). Once the whole file is processed, the Tokenizer now holds the array of processed tokens and is ready to stream them to the CompilationEngine.

### Compilation Engine

While the output of the Tokenizer only gives us an array of Tokens, we still do not have a proper syntax tree yet. Since we're not doing any comprehensive error checking (as per the specifications) we can actually construct and parse the syntax tree at the same time in a recursive fashion. Think of it as a depth-first traversal of a graph (after all, trees are graphs in a way). We advance deeper and deeper into leaf nodes until we hit a dead-end, whereupon we backtrack and continue until we've parsed all the branches. The leaf nodes here correspond to atomic terms which cannot be broken down further, while the branches are your composite types.

For example, the code
```JavaScript
while (count < 100) {
    let count = count + 1;
}
```

would break down to the following tree structure in the syntax tree:
```
whileStatement
├── while
├── (
├── expression
│   ├── term
│   │   └── varName
│   │       └── count
│   ├── op
│   │   └── <
│   └── term
│       └── constant
│           └── 100
├── )
├── {
├── statements
│   └── statement
│       └── letStatement
│           ├── let
│           ├── varName
│           │   └── count
│           ├── =
│           ├── expression
│           │   ├── term
│           │   │   └── varName
│           │   │       └── count
│           │   ├── op
│           │   │   └── +
│           │   └── term
│           │       └── constant
│           │           └── 1
│           └── ;
└── }
```

Think of our stream of tokens as a queue. The `currentToken` variable always stores the current token in the queue. The `advance()` method advances the queue to the next token, updating the value of `currentToken`. The compilation engine effectively calls `advance()` and `compileCurrentToken()` repeatedly until there are no more tokens left in the queue.

`compileCurrentToken()` simply looks at the `currentToken` and calls the appropriate compilation subroutine. There are dedicated subroutines for each of the composite types. Just like how these composite types can be recursive and can contain nested levels of other types, our dedicated subroutines also call one other in such manner.

This way, our function calls would map directly onto the syntax tree, meaning we are effectively generating and parsing the tree at the same time. For the above example, the calling chain would look liks this:

```
compileWhile()
├── compileExpression()
│   ├── compileTerm()
│   ├── compileOperator()
│   └── compileTerm()
├── compileStatements()
│   └── compileLet()
│       ├── compileExpression()
│       │   ├── compileTerm()
│       │   ├── compileOperator()
│       │   └── compileTerm()
```

This is not the full call stack, though. There would be `advance()` and `compileCurrentToken()` calls interleaved with additional calls to compiling the integer constants (like the `100` in `count < 100`) or compiling identifiers.

Now, notice how this call chain correlates with the grammar spec for a while statement: 

`'while'` `'('`expression`')'` `'{'`statements`'}'`.

This is what the Compilation Engine does on a fundamental level. Calls to each of the dedicated subroutines directly follow the language grammar specification as guidelines. Since each file corresponds to a particular class, we call the `compileClass()` to start the recursive call chain and the engine will fill in the output lines by calling the appropriate VMWriter functions at appropriate intervals.

## VM Code Generation

While parsing the syntax tree, the compiler also needs to do additional work to ensure that the program flow works according to the user's specifications. In our `while` statement example above, the compiler needs to ensure that proper branches are generated for the `while`'s condition expression so that the program control flow can escape the loop body once the value of `count` reaches 100. Furthermore, in the loop body, it has to make sure that the value of the `count` variable is updated properly.

Mind you, our `let count = count + 1;` statement uses ***infix notation***, while our compilation target, the stack-based VM, uses ***postfix notation***. So our VM code might look like this,
```
push count
push 1
add
pop count
```

This also brings up another question: ***Where does `count` belong?***

Remember, our VM has virtual memory segments. The `push` and `pop` commands require a memory segment and an offset to index into the segment. Which segment does `count` belong to? Is it a local variable, a static variable, a function argument, or an object field varaible?

### Symbol Tables

To keep track of each of the identifiers in the program, the compiler keeps two symbol tables to lookup a variable's proper segment and offset value. Why two? It is because Jack effectively only has two scopes: class-level and subroutine-level. Since we do not have nested functions nor can we have an arbitrary block-level scope within code blocks, we'll only ever declare variables at the beginning of a class definition or a function definition. Each of our tables correspond to those scopes. If that weren't the case, we'd need multi-level symbol tables for each nested scope level.

Our two symbol tables are the Class Table and the Subroutine Table. They are intialized as instances of the `SymbolTable` class. The Class Table is initialized once, and is only reset when the compiler moves on to the next class file. The table contains the class name, all of its field and static variables, as well as the class method names in it.

As stored in the `VarKind` enum, any identifier in the program can be one of the following:
- a static variable
- a field variable
- a local variable
- a function argument variable
- a class type name
- a subroutine name
- none of the above (i.e., undefined)

Each of the first four types correspond to a memory segment in the VM. The symbol table updates this information when an identifier entry is first added to it. It also keeps track of a separate index counter for each of the segments (which also doubles as a counter to generate unique lables), and assigns the proper segment type and index value when an identifier entry is added to it.

The same case applies to the Subroutine Table, except that it is reset whenever the compiler gets to a new subroutine. Also, whenever the compiler processes a class method, it pushes an entry for an identifier by the name of `this` whose type corresponds to the current class' name. Later on, when the method is called via an object, the compiler will effectively "push" this entry as the first argument allowing access to the `this` keyword inside the method body.

When the compiler encounters an identifier, it looks up its entry in the Subroutine Table. If it doesn't find it there, it then looks up the Class Table. If it doesn't find the entry there as well, it... well, it should stop compilation and return an error, telling the user there's an unidentified variable in the program. But since we're supposed to assume that the given programs are error-free, we simply keep on compiling instead (which makes this a not-so-smart compiler).

### Function Calls and Returns

Thanks to our recursive structure, things like function calls and return values are easily sorted out. For a function call, the `compileExpressionList()` function returns the number of expressions it has compiled once it is done processing the list. The compiler can use this information to calculate how many arguments were passed to the function during the call. Not only that, since the arguments need to be pushed before the call itself, the recursive compilation of the argument list does exactly that. Once `compileExpressionList()` returns, the compiler takes the return value and makes the subroutine call.

A similar phenomenon happens in case of return statements. Since return statements are of the form `return expression?;`, our recursive descent will end up compiling the return expression before processing the return keyword itself. The compilation of the expression will automatically push this return value onto the stack. The compiler only has to write the equivalent return VM statement. In case of void return types, we simply push a 0 onto the stack as the return value which then gets dumped by the caller.

### Class Methods and Constructors

You might remember we had a `pointer` segment in our VM which would point to either the `this` or `that` virtual memory segment. Since we always pass `this` (the Jack identifier, not the VM segment) as the first argument during a method call, the compiler uses this argument to set up the proper memory segment. Upon entering a method's body, the compiler immediately inserts VM code which effectively takes this first argument, pushes it onto the stack, and pops it into the `pointer` segment at index 0. The result is that the `this` memory segment (which is aliased by `pointer 0`) is now aligned with the base address of the calling object variable. All references to the calling object will now properly point to the correct location in memory.

In the case of Constructors, the compiler needs to allocate enough memory for the class object. The amount of memory required is calculated automatically by the time all the class field variables are compiled and put into the Class Symbol Table. While compiling a Constructor, the compiler consults the current Class Table to get this number and calls the Jack OS library function `Memory.alloc()` with the proper size to get the required block of memory. It then aligns the base of the `this` segment (aliased by `pointer 0`) with this block, which is then returned to caller at the end of the Constructor as a `return this;` statement.