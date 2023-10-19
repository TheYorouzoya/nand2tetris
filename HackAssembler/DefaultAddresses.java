// A list of default RAM address locations provided by the assembler
public enum DefaultAddresses {
    RO      ("R0", "0"),
    R1      ("R1", "1"),
    R2      ("R2", "2"),
    R3      ("R3", "3"),
    R4      ("R4", "4"),
    R5      ("R5", "5"),
    R6      ("R6", "6"),
    R7      ("R7", "7"),
    R8      ("R8", "8"),
    R9      ("R9", "9"),
    R10     ("R10", "10"),
    R11     ("R11", "11"),
    R12     ("R12", "12"),
    R13     ("R13", "13"),
    R14     ("R14", "14"),
    R15     ("R15", "15"),
    SP      ("SP", "0"),
    LCL     ("LCL", "1"),
    ARG     ("ARG", "2"),
    THIS    ("THIS", "3"),
    THAT    ("THAT", "4"),
    SCREEN  ("SCREEN", "16384"),
    KBD     ("KBD", "24576");

    private final String mnemonic;
    private final String address;

    DefaultAddresses (String mnemonic, String address) {
        this.mnemonic = mnemonic;
        this.address = address;
    }

    public String mnemonic() { return this.mnemonic; }
    public String address() { return this.address; }

}
