
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public final class SymbolTable {
    private static final char[] SYMBOLS;
    private static final Set<Character> SYM_TABLE;
    private static final HashMap<String, Keywords> KEY_TABLE;
    private static final Set<String> STATEMENT_TABLE;
    private static final Set<Character> OP_TABLE;

    static {
        SYMBOLS = new char[] {
        '{', '}', '(', ')', '[', ']', '.', ',',
        ';', '+', '-', '*', '/', '&', '|', '<',
        '>', '=', '~'
        };
        
        Set<Character> dummy = new HashSet<>();
        for (char symbol : SYMBOLS) {
            dummy.add(symbol);
        }
        SYM_TABLE = Collections.unmodifiableSet(dummy);

        char[] operators = new char[] {
            '+', '-', '*', '/', '&', '|', '<',
            '>', '=', '~'
        };

        Set<Character> dumDum = new HashSet<>();
        for (char operator : operators) {
            dumDum.add(operator);
        }
        OP_TABLE = Collections.unmodifiableSet(dumDum);

        HashMap<String, Keywords> dummy2 = new HashMap<>();
        for (Keywords keyword : Keywords.values()) {
            dummy2.put(keyword.toString().toLowerCase(), keyword);
        }
        KEY_TABLE = new HashMap<String, Keywords>(Collections.unmodifiableMap(dummy2));

        String[] statements = { "let", "do", "if", "while", "return" };

        Set<String> dummy3 = new HashSet<>();
        for (String statement : statements) {
            dummy3.add(statement);
        }
        STATEMENT_TABLE = Collections.unmodifiableSet(dummy3);
    }

    private HashMap<String, Variable> table;
    private int staticCount, fieldCount, varCount, argCount;

    private class Variable {
        private VarKind kind;
        private String type;
        private int index;

        private Variable (VarKind kind, String type, int index) {
            this.kind = kind;
            this.type = type;
            this.index = index;
        }

        public VarKind kind() { return this.kind; }
        public String type() { return this.type; }
        public int index() { return this.index; }

        public String toString() {
            return "Kind: " + kind.toString() + " Type: " + type + " Index: " + index;
        }
    }

    public SymbolTable() {
        table = new HashMap<>();
        staticCount = 0;
        fieldCount = 0;
        varCount = 0;
        argCount = 0;
    }

    public void resetTable() {
        table.clear();
        staticCount = 0;
        fieldCount = 0;
        varCount = 0;
        argCount = 0;
    }

    public void addVar (VarKind kind, String type, String name) {
        int index = 0;
        switch (kind) {
            case STATIC:
                index = staticCount++;
                break;
            case FIELD:
                index = fieldCount++;
                break;
            case VAR:
                index = varCount++;
                break;
            case ARG:
                index = argCount++;
                break;
            default:
                break;
        }
        table.put(name, new Variable(kind, type, index));
    }

    public int varCount (VarKind kind) {
        switch (kind) {
            case STATIC:
                return staticCount;
            case FIELD:
                return fieldCount;
            case VAR:
                return varCount;
            case ARG:
                return argCount;
            default:
                return -1;
        }
    }

    public VarKind kindOf (String name) {
        Variable entry = table.get(name);
        if (entry == null) {
            return VarKind.NONE;
        }
        return entry.kind();
    }

    public String typeOf (String name) {
        Variable entry = table.get(name);
        if (entry == null) {
            return "";
        }
        return entry.type();
    }

    public int indexOf (String name) {
        Variable entry = table.get(name);
        if (entry == null) {
            return -1;
        }
        return entry.index();
    }

    public static boolean isSymbol (char ch) {
        return SYM_TABLE.contains(ch);
    }

    public static boolean isKeyword (String token) {
        if (token == null) return false;
        return KEY_TABLE.containsKey(token);
    }

    public static Keywords keywordType (String token) {
        return KEY_TABLE.get(token);
    }

    public static boolean isStatement (String token) {
        return STATEMENT_TABLE.contains(token);
    }

    public static boolean isOperator (char ch) {
        return OP_TABLE.contains(ch);
    }

}
