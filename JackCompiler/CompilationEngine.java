import java.util.ArrayDeque;

public class CompilationEngine {
    private ArrayDeque<Token> tokens;
    private Token currentToken;
    private SymbolTable subroutineTable;
    private SymbolTable classTable;
    private VMWriter writer;
    private String className;
    private int labelCounter;

    public CompilationEngine (ArrayDeque<Token> tokens, String outputFile, String fileName) {
        if (tokens == null) {
            throw new IllegalArgumentException("Given token stack is empty.");
        }
        this.tokens = tokens;
        labelCounter = 0;
        subroutineTable = new SymbolTable();
        classTable = new SymbolTable(); 
        writer = new VMWriter(outputFile);
    }

    private void advance() {
        currentToken = tokens.removeFirst();
    }

    public void compileProgram () {
        advance();
        compileClass();
        writer.writeFile();
    }

    private void compileCurrentToken () {
        switch (currentToken.type()) {
            case KEYWORD:
                compileKeyword();
                break;

            case INT_CONST:
                compileIntegerConstant();
                break;

            case STRING_CONST:
                compileStringConstant();
                break;

            default:
                break;
        }
    }

    private void compileKeyword () {
        Keywords keyword = currentToken.keyWord();

        switch (keyword) {
            case CLASS:
                compileClass();
                break;

            case METHOD:
            case FUNCTION:
            case CONSTRUCTOR:
                compileSubroutine();
                break;

            case VAR:
                compileVarDec();
                break;

            case STATIC:
            case FIELD:
                compileClassVarDec();
                break;
            
            case LET:
                compileLet();
                break;

            case DO:
                compileDo();
                break;

            case IF:
                compileIf();
                break;

            case WHILE:
                compileWhile();
                break;

            case RETURN:
                compileReturn();
                break;

            case NULL: case FALSE:
                writer.writePush(MemSegments.CONSTANT, 0);
                break;

            case TRUE:
                writer.writePush(MemSegments.CONSTANT, 1);
                writer.writeArithmetic(VMCommands.NEG);
                break;

            case THIS:
                writer.writePush(MemSegments.POINTER, 0);
                break;

            default:
                break;
        }
    }

    private void compileIntegerConstant () {
        writer.writePush(MemSegments.CONSTANT, currentToken.intVal());
    }

    private void compileStringConstant () {
        int length = currentToken.value().length();
        String str = currentToken.value();

        writer.writePush(MemSegments.CONSTANT, length);
        writer.writeCall("String.new", 1);

        for (int i = 0; i < length; i++) {
            writer.writePush(MemSegments.CONSTANT, str.charAt(i));
            writer.writeCall("String.appendChar", 2);
        }
    }


    // Class grammar: 'class' className '{' classVarDec* subroutineDec* '}'
    private void compileClass () {
        advance();
        classTable.resetTable();
        classTable.addVar(VarKind.CLASS, currentToken.value(), currentToken.value());
        className = currentToken.value();

        compileCurrentToken();

        while (!tokens.isEmpty()) {
            advance();
            compileCurrentToken();
        }
    }

    // Class Variable Declaration: ('static'|'field') type varName (',' varName)* ';'
    private void compileClassVarDec () {
        VarKind kind;
        if (currentToken.value().equals("static")) {
            kind = VarKind.STATIC;
        } else {
            kind = VarKind.FIELD;
        }
        
        advance();
        String type = currentToken.value();
        compileCurrentToken();


        while (!currentToken.value().equals(";")) {
            advance();
            if (currentToken.type() == Tokens.IDENTIFIER) {
                classTable.addVar(kind, type, currentToken.value());
            }
            compileCurrentToken();
        }
    }

    // Subroutine grammar:
    // ('constructor'|'function'|'method') ('void'|type) subroutineName '('parameterList')' subroutineBody
    private void compileSubroutine () {
        String subroutineName, returnType;
        Keywords subroutineKind = currentToken.keyWord();

        subroutineTable.resetTable();
        advance();

        returnType = currentToken.value();
        compileCurrentToken();
        advance();

        subroutineName = currentToken.value();
        classTable.addVar(VarKind.SUBROUTINE, returnType, subroutineName);
        if (subroutineKind == Keywords.METHOD) {
            subroutineTable.addVar(VarKind.ARG, className, "this");
        }

        while(!currentToken.value().equals("{")) {
            if (currentToken.value().equals("(")) {
                compileCurrentToken();
                advance();
                compileParameterList();
            }
            compileCurrentToken();
            advance();
        }

        compileSubroutineBody(subroutineName, subroutineKind);
    }

    // Parameter List Grammar: ((type varName) ',' (type varName)*)?
    private void compileParameterList () {
        // unroll the loop to process one argument at a time by reading 2-3 tokens at a time
        while(!currentToken.value().equals(")")) {
            String type = currentToken.value();
            
            compileCurrentToken();
            advance();

            subroutineTable.addVar(VarKind.ARG, type, currentToken.value());

            compileCurrentToken();
            advance();

            if (currentToken.value().equals(",")) {
                compileCurrentToken();
                advance();
            }
        }
    }

    // Subroutine Body Grammar: '{' varDec* statements '}'
    private void compileSubroutineBody (String subroutineName, Keywords subroutineKind) {
        // compile all variable declarations
        while (!SymbolTable.isStatement(currentToken.value())) {
            compileCurrentToken();
            advance();
        }

        writer.writeFunction(className + "." + subroutineName, 
                             subroutineTable.varCount(VarKind.VAR));
        if (subroutineKind == Keywords.CONSTRUCTOR) {
            int size = classTable.varCount(VarKind.FIELD);      // compute number of blocks
            writer.writePush(MemSegments.CONSTANT, size);       // push block size onto stack
            writer.writeCall("Memory.alloc", 1);     // allocate memory
            writer.writePop(MemSegments.POINTER, 0);      // align base address of returned block
        } else if (subroutineKind == Keywords.METHOD) {
            writer.writePush(MemSegments.ARGUMENT, 0);
            writer.writePop(MemSegments.POINTER, 0);
        }

        // compile all statements
        while (!currentToken.value().equals("}")) {
            compileStatements();
        }

        compileCurrentToken();
    }

    // Variable Declaration grammar: 'var' type varName (',' varName)* ';'
     private void compileVarDec () {
        advance();

        String type = currentToken.value();
            
        compileCurrentToken();
        advance();

        while (!currentToken.value().equals(";")) {
            subroutineTable.addVar(VarKind.VAR, type, currentToken.value());

            compileCurrentToken();
            advance();

            if (currentToken.value().equals(",")) {
                compileCurrentToken();
                advance();
            }
        }
        
        compileCurrentToken();
    }

    // Statement grammar: statements*
    // where statement can be one of: letStatement, ifStatement, whileStatement, 
    //                                doStatement, returnStatement
    private void compileStatements () {
        while (SymbolTable.isStatement(currentToken.value())) {
            compileCurrentToken();
            advance();
        }
    }

    // Let Statement Grammar: 'let' varName('['expression']')? '=' expression ';'
    private void compileLet () {
        boolean arrayFlag = false;
        advance();

        String varName = currentToken.value();

        while (!currentToken.value().equals(";")) {
            if (currentToken.value().equals("[")) {
                compileCurrentToken();
                advance();
                compileExpression();
                pushIdentifier(varName);
                writer.writeArithmetic(VMCommands.ADD);
                arrayFlag = true;
            }
            if (currentToken.value().equals("=")) {
                compileCurrentToken();
                advance();
                compileExpression();
                continue;
            }
            compileCurrentToken();
            advance();
            
        }

        compileCurrentToken();

        if (arrayFlag) {
            writer.writePop(MemSegments.TEMP, 0);
            writer.writePop(MemSegments.POINTER, 1);
            writer.writePush(MemSegments.TEMP, 0);
            writer.writePop(MemSegments.THAT, 0);
        } else {
            popIdentifier(varName);
        }
    }

    // Do Statement Grammar: 'do' subroutineCall ';'
    // where subroutineCall is one of: subroutineName'('expressionList')'
    //                                 (className|varName)'.'subroutineName'('expressionList')'
    private void compileDo () {
        advance();

        compileExpression();
        compileCurrentToken();
        writer.writePop(MemSegments.TEMP, 0);
    }

    // Return Statement Grammar: 'return' expression? ';'
    private void compileReturn () {
        advance();

        if (!currentToken.value().equals(";")) {
            compileExpression();
        } else {
            writer.writePush(MemSegments.CONSTANT, 0);
        }

        compileCurrentToken();
        writer.writeReturn();
    }

    // While Statement Grammar: 'while' '('expression')' '{'statements'}'
    private void compileWhile () {
        advance();

        String loopStart = className + "_whileStart$" + labelCounter++;
        String loopEnd = className + "_whileEnd$" + labelCounter++;
        writer.writeLabel(loopStart);

        while (!currentToken.value().equals("}")) {
            if (currentToken.value().equals("(")) {
                compileCurrentToken();
                advance();
                compileExpression();
                writer.writeArithmetic(VMCommands.NOT);
                writer.writeIf(loopEnd);

            } else if (currentToken.value().equals("{")) {
                compileCurrentToken();
                advance();
                compileStatements();
                writer.writeGoto(loopStart);
                continue;
            }
            compileCurrentToken();
            advance();
        }

        compileCurrentToken();
        writer.writeLabel(loopEnd);
    }

    // If Statement Grammar: 'if' '('expression')' '{'statements'}' ('else' '{'statements'}')?
    private void compileIf () {
        advance();

        String falseLabel = "_ifFalse$" + labelCounter++;

        while (!currentToken.value().equals("{")) {
            if (currentToken.value().equals("(")) {
                compileCurrentToken();
                advance();
                compileExpression();
            }
            compileCurrentToken();
            advance();
        }

        writer.writeArithmetic(VMCommands.NOT);
        writer.writeIf(falseLabel);

        compileCurrentToken();
        advance();
        compileStatements();
        compileCurrentToken();
   
        Token nextToken = tokens.peekFirst();

        if (nextToken.value().equals("else")) {
            String elseEnd = "_elseEnd$" + labelCounter++;
            writer.writeGoto(elseEnd);
            advance();
            compileCurrentToken();      // compile else
            advance();
            compileCurrentToken();      // compile '{'
            advance();

            writer.writeLabel(falseLabel);

            compileStatements();        // compile statements
            compileCurrentToken();      // compile '}'

            writer.writeLabel(elseEnd);
        } else {
            writer.writeLabel(falseLabel);
        }
    }

    // Expression Grammar: term (op term)*
    private void compileExpression () {
        ArrayDeque<Character> operatorStack = new ArrayDeque<>();
        boolean unaryFlag = false;      // flag if expression is (unaryOp term)

        if (currentToken.value().equals("-") || currentToken.value().equals("~")) {
            unaryFlag = true;
        }

        while (!currentToken.isExpressionDelimiter()) {
            if (currentToken.type() == Tokens.SYMBOL) {
                if (unaryFlag || currentToken.symbol() == '(') {
                    compileTerm();
                } else {
                    compileCurrentToken();
                    if (SymbolTable.isOperator(currentToken.symbol())) {
                        operatorStack.add(currentToken.symbol());
                    }
                    advance();
                }
            } else {
                compileTerm();
            }
            unaryFlag = false;
        }

        while (!operatorStack.isEmpty()) {
            compileOperator(operatorStack.pop());
        }
    }

    // Term Grammar: A term can be one of,
    //  - integerConstant
    //  - stringConstant
    //  - keywordConstant
    //  - varName or varName'['expression']'
    //  - '('expression')'
    //  - (unaryOp term)
    //  - subroutineCall
    // Op can be one of:  '+', '-', '*', '/', '&', '|', '<', '>', '='

    private void compileTerm () {
        switch (currentToken.type()) {
            case SYMBOL:
                // compile unaryOp term
                if (currentToken.symbol() == '-' || currentToken.symbol() == '~') {
                    char sign = currentToken.symbol();
                    compileCurrentToken();      // compile unaryOp
                    advance();
                    compileTerm();              // compile term
                    if (sign == '-') {
                        writer.writeArithmetic(VMCommands.NEG);
                    } else {
                        writer.writeArithmetic(VMCommands.NOT);
                    }
                }
                // compile '('expression')' 
                else if (currentToken.symbol() == '(') {
                    compileCurrentToken();      // compile '('
                    advance();
                    compileExpression();        // compile expression
                    compileCurrentToken();      // compile ')'
                    advance();
                } else {
                    return;
                }
                break;

            case IDENTIFIER:
                Token nextToken = tokens.peekFirst();
                if (nextToken.type() == Tokens.SYMBOL) {
                    // compile varName'['expression']'
                    if (nextToken.symbol() == '[') {
                        String arrBase = currentToken.value();
                        compileCurrentToken();  // compile varName
                        advance();
                        compileCurrentToken();  // compile '['
                        advance();
                        compileExpression();    // compile expression
                        pushIdentifier(arrBase);
                        writer.writeArithmetic(VMCommands.ADD);
                        writer.writePop(MemSegments.POINTER, 1);
                        writer.writePush(MemSegments.THAT, 0);
                        compileCurrentToken();  // compile ']'
                        advance();
                    }

                    // compile subroutine call
                    // SubroutineCall grammar: a subroutineCall can be one of,
                    //  - subroutineName'('expressionList')
                    //  - (className|varName)'.'subroutineName'('expressionList')'
                    else if (nextToken.symbol() == '.' || nextToken.symbol() == '(') {
                        String className, subroutineName;
                        int arguments = 0;
                        if (nextToken.symbol() == '(') {
                            className = typeOf("this");
                            subroutineName = currentToken.value();
                            pushIdentifier("this");
                            arguments++;
                            compileCurrentToken();
                            advance();
                        } else {
                            VarKind callerKind = kindOf(currentToken.value());
                            if (callerKind == VarKind.NONE || callerKind == VarKind.CLASS) {
                                className = currentToken.value();
                            } else {
                                className = typeOf(currentToken.value());
                                pushIdentifier(currentToken.value());
                                arguments++;
                            }
                            compileCurrentToken();
                            advance();
                            compileCurrentToken();
                            advance();
                            subroutineName = currentToken.value();
                            compileCurrentToken();
                            advance();
                        }

                        compileCurrentToken();
                        advance();
                        arguments += compileExpressionList();
                        writer.writeCall(className + "." + subroutineName, arguments);
                        compileCurrentToken();
                        advance();    
                    } else {
                        compileCurrentToken();
                        pushIdentifier(currentToken.value());
                        advance();
                    }
                    break;
                } else {
                    compileCurrentToken();
                    pushIdentifier(currentToken.value());
                    advance();
                    break;
                }

            default:
                compileCurrentToken();
                advance();
                break;
        }
    }

    private int compileExpressionList () {
        int expressions = 0;

        while (!currentToken.value().equals(")")) {
            if (currentToken.value().equals(",")) {
                compileCurrentToken();
                advance();
                continue;
            }
            compileExpression();
            expressions++;
        }

        return expressions;
    }

    private void compileOperator (char operator) {
        switch (operator) {
            case '+':
                writer.writeArithmetic(VMCommands.ADD);
                break;

            case '-':
                writer.writeArithmetic(VMCommands.SUB);
                break;

            case '*':
                writer.writeCall("Math.multiply", 2);
                break;

            case '/':
                writer.writeCall("Math.divide", 2);
                break;

            case '&':
                writer.writeArithmetic(VMCommands.AND);
                break;

            case '|':
                writer.writeArithmetic(VMCommands.OR);
                break;
            
            case '<':
                writer.writeArithmetic(VMCommands.LT);
                break;
            
            case '>':
                writer.writeArithmetic(VMCommands.GT);
                break;
            
            case '=':
                writer.writeArithmetic(VMCommands.EQ);
                break;
            
            case '~':
                writer.writeArithmetic(VMCommands.NOT);
                break;
            
            default:
                break;
        }
    }

    private void pushIdentifier (String identifier) {
        switch (kindOf(identifier)) {
            case STATIC:
                writer.writePush(MemSegments.STATIC, indexOf(identifier));
                break;
            case FIELD:
                writer.writePush(MemSegments.THIS, indexOf(identifier));
                break;
            case VAR:
                writer.writePush(MemSegments.LOCAL, indexOf(identifier));
                break;
            case ARG:
                writer.writePush(MemSegments.ARGUMENT, indexOf(identifier));
                break;
            default:
                break;
        }
    }

    private void popIdentifier (String identifier) {
        switch (kindOf(identifier)) {
            case STATIC:
                writer.writePop(MemSegments.STATIC, indexOf(identifier));
                break;
            case FIELD:
                writer.writePop(MemSegments.THIS, indexOf(identifier));
                break;
            case VAR:
                writer.writePop(MemSegments.LOCAL, indexOf(identifier));
                break;
            case ARG:
                writer.writePop(MemSegments.ARGUMENT, indexOf(identifier));
                break;
            default:
                break;
        }
    }

    private VarKind kindOf (String name) {
        VarKind entry = subroutineTable.kindOf(name);
        if (entry == VarKind.NONE) {
            return classTable.kindOf(name);
        }
        return entry;
    }

    private String typeOf (String name) {
        String entry = subroutineTable.typeOf(name);
        if (entry == "") {
            return classTable.typeOf(name);
        }
        return entry;
    }

    private int indexOf (String name) {
        int entry = subroutineTable.indexOf(name);
        if (entry < 0) {
            return classTable.indexOf(name);
        }
        return entry;
    }
}
