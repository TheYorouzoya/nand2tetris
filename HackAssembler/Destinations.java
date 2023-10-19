// A list of the assembler's destination mnemonics and their binary codes
public enum Destinations {
    NULL    ("null", "000"),
    M       ("M", "001"),
    D       ("D", "010"),
    MD      ("MD", "011"),
    A       ("A", "100"),
    AM      ("AM", "101"),
    AD      ("AD", "110"),
    AMD     ("AMD", "111");

    private final String mnemonic;
    private final String binary;

    Destinations (String mnemonic, String binary) {
        this.mnemonic = mnemonic;
        this.binary = binary;
    }

    public String mnemonic() { return this.mnemonic; }
    public String binary() { return this.binary; }
}
