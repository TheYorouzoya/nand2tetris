// A list of the assembler's computation mnemonics and their binary codes
public enum Computations {
    ZERO        ("0", "0101010"),
    ONE         ("1", "0111111"),
    MINUSONE    ("-1", "0111010"),
    D           ("D", "0001100"),
    A           ("A", "0110000"),
    M           ("M", "1110000"),
    NOTD        ("!D", "0001101"),
    NOTA        ("!A", "0110001"),
    NOTM        ("!M", "1110001"),
    MINUSD      ("-D", "0001111"),
    MINUSA      ("-A", "0110011"),
    MINUSM      ("-M", "1110011"),
    DPLUSONE    ("D+1", "0011111"),
    APLUSONE    ("A+1", "0110111"),
    MPLUSONE    ("M+1", "1110111"),
    DMINUSONE   ("D-1", "0001110"),
    AMINUSONE   ("A-1", "0110010"),
    MMINUSONE   ("M-1", "1110010"),
    DPLUSA      ("D+A", "0000010"),
    DPLUSM      ("D+M", "1000010"),
    DMINUSA     ("D-A", "0010011"),
    DMINUSM     ("D-M", "1010011"),
    AMINUSD     ("A-D", "0000111"),
    MMINUSD     ("M-D", "1000111"),
    DANDA       ("D&A", "0000000"),
    DANDM       ("D&M", "1000000"),
    DORA        ("D|A", "0010101"),
    DORM        ("D|M", "1010101");

    private final String mnemonic;
    private final String binary;

    Computations (String mnemonic, String binary) {
        this.mnemonic = mnemonic;
        this.binary = binary;
    }

    public String mnemonic() { return this.mnemonic; }
    public String binary() { return this.binary; }

}
