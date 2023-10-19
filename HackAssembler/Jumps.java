// A list of the assembler's jump mnemonics and their binary codes
public enum Jumps {
    NULL ("null", "000"),
    JGT  ("JGT", "001"),
    JEQ  ("JEQ", "010"),
    JGE  ("JGE", "011"),
    JLT  ("JLT", "100"),
    JNE  ("JNE", "101"),
    JLE  ("JLE", "110"),
    JMP  ("JMP", "111");

    private final String mnemonic;
    private final String binary;

    Jumps (String mnemonic, String binary) {
        this.mnemonic = mnemonic;
        this.binary = binary;
    }

    public String mnemonic() { return this.mnemonic; }
    public String binary() { return this.binary; }
}
