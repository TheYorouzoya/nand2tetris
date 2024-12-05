# nand2tetris

This repo contains the collection of all the *substantial* projects I made during the two-part [nand2tetris](https://www.coursera.org/learn/build-a-computer) course. As the description says, the course is about building a computer from scratch, all the way from logic gates to a proper program running on top of the hardware we've built.

Since this repo contains the parts from the hardware interface layer (the assembler) and above, I will briefly describe the Hack Computer's specs before moving on to the rest of the software specs.

## The Hack Computer

The Hack machine is a 16-bit von Neumann machine designed to execute programs written in the Hack machine language. It consists of,
- A CPU
- Two separate memory modules: one for *instruction memory* (instruction ROM) and one for *data memory* (the RAM)
- Two memory-mapped I/O devices: a *screen* and a *keyboard*.

The CPU consists of an ALU and three registers named *Data Register* (D), *Address Register* (A), and *Program Counter* (PC). The D register is used solely for storing and operating on data values while the A register is multi-purpose. It can either store a data value for computation, select an address in the *instruction memory*, or select an address in the *data memory*.

Below is my arrangement for the Hack CPU circuitry as it was implemented in HDL. There is probably a more efficient way to do this, but this is what I came up with and what worked in the simulator. The HDL file itself is included [here](./CPU.hdl) as well.

![Image shows the Hack CPU circuit arrangement](./docimages/hack-cpu.png)

The point is that this is a really simple CPU. None of the modern CPU magic (the pipelining, caches, memory controller, register files, functional units, etc.) is to be found here. I hope it sets the proper precendence for what to expect out of the projects in this repo. With that, let us start with the first one, the [Hack Assembler](./HackAssembler/).

## Hack Assembler

