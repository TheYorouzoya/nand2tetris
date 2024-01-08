public class Token {
    private Tokens type;
    private String value;

    public Token(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Given token is null");
        }

        type = parseToken(token);
        value = token;
    }

    public Token(String token, Tokens type) {
        if (token == null || type == null) {
            throw new IllegalArgumentException("Given token/type is null");
        }
        value = token;
        this.type = type;
    }

    public Tokens type() { return type; }
    public String value() { return value; }

    // called only when token type is KEYWORD
    public Keywords keyWord() {
        return SymbolTable.keywordType(value);
    }

    // called only when token type is SYMBOL
    public char symbol() {
        return value.charAt(0);
    }

    // called only when token type is IDENTIFIER
    public String identifier() {
        return value;
    }

    // called only when token type is INT_CONST
    public int intVal() {
        return Integer.parseInt(value);
    }

    // called only when token type is STRING_CONST
    public String stringVal() {
        return value;
    }

    public boolean isExpressionDelimiter() {
        if (this.type != Tokens.SYMBOL) return false;
        char symbol = this.symbol();
        if (symbol == '}' || symbol == ']' || symbol == ')' || symbol == ';' || symbol == ',') {
            return true;
        }
        return false;
    }

    private Tokens parseToken(String token) {
        if (token.length() < 1 || token == null) {
            throw new IllegalArgumentException("Given token is null");
        }

        char ch = token.charAt(0);
        if (Character.isDigit(ch)) {
            return Tokens.INT_CONST;
        }

        if (token.length() == 1) {
            if(SymbolTable.isSymbol(ch)) {
                return Tokens.SYMBOL;
            }
            return Tokens.IDENTIFIER;
        }

        if (SymbolTable.isKeyword(token)) {
            return Tokens.KEYWORD;
        }

        return Tokens.IDENTIFIER;
    }

    public String XMLTag() {
        return this.type.openTag() + " " 
             + this.value + " "
             + this.type.closeTag();
    }
}
