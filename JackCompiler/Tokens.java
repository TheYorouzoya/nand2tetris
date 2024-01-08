
// list of types of tokens in Jack language
public enum Tokens {
    KEYWORD         ("<keyword>",           "</keyword>"),
    SYMBOL          ("<symbol>",            "</symbol>"),
    IDENTIFIER      ("<identifier>",        "</identifier>"),
    INT_CONST       ("<integerConstant>",   "</integerConstant>"),
    STRING_CONST    ("<stringConstant>",    "</stringConstant>");

    private final String openTag;
    private final String closeTag;

    Tokens(String openTag, String closeTag) {
        this.openTag = openTag;
        this.closeTag = closeTag;
    }

    public String openTag() { return this.openTag; }
    public String closeTag() { return this.closeTag; }
}
