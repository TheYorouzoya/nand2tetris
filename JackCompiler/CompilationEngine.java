import java.util.ArrayDeque;

import enums.Keywords;
import enums.Tokens;

public class CompilationEngine {
    private ArrayDeque<String> lines;
    private ArrayDeque<Token> tokens;
    private Token currentToken;

    public CompilationEngine (ArrayDeque<Token> tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("Given token stack is empty.");
        }
        this.tokens = tokens;
        lines = new ArrayDeque<>();

    }

    private void advance() {
        currentToken = tokens.removeFirst();
    }

    public ArrayDeque<String> compileProgram () {
        advance();
        compileClass();
        return lines;
    }

    private void compileCurrentToken () {
        switch (currentToken.type()) {
            case KEYWORD:
                compileKeyword();
                break;

            case SYMBOL:
                compileSymbol();
                break;

            case IDENTIFIER:
                compileIdentifier();
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

            default:
                lines.add(currentToken.XMLTag());
                break;
        }
    }

    private void compileSymbol () {
        String openTag = "<symbol> ", closeTag = " </symbol>";
        char symbol = currentToken.symbol();

        switch (symbol) {
                case '&':
                lines.add(openTag + "&amp;" + closeTag);
                break;

            case '<':
                lines.add(openTag + "&lt;" + closeTag);
                break;

            case '>':
                lines.add(openTag + "&gt;" + closeTag);
                break;
            
            default:
                lines.add(openTag + symbol + closeTag);
                break;
        }
    }

    private void compileIdentifier () {
        lines.add(currentToken.XMLTag());
    }

    private void compileIntegerConstant () {
        lines.add(currentToken.XMLTag());
    }

    private void compileStringConstant () {
        lines.add(currentToken.XMLTag());
    }


    // Class grammar: 'class' className '{' classVarDec* subroutineDec* '}'
    private void compileClass () {
        String openTag = "<class>", closeTag = "</class>";
        lines.add(openTag);
        lines.add(currentToken.XMLTag());

        while (!tokens.isEmpty()) {
            advance();
            compileCurrentToken();
        }

        lines.add(closeTag);
    }

    // Class Variable Declaration: ('static'|'field') type varName (',' varName)* ';'
    private void compileClassVarDec () {
        String openTag = "<classVarDec>", closeTag = "</classVarDec>";
        lines.add(openTag);
        lines.add(currentToken.XMLTag());

        while (!currentToken.value().equals(";")) {
            advance();
            compileCurrentToken();
        }
        
        lines.add(closeTag);
    }

    // Subroutine grammar:
    // ('constructor'|'function'|'method') ('void'|type) subroutineName '('parameterList') subroutineBody
    private void compileSubroutine () {
        String openTag = "<subroutineDec>", closeTag = "</subroutineDec>";
        lines.add(openTag);
        lines.add(currentToken.XMLTag());
        advance();

        while(!currentToken.value().equals("{")) {
            if (currentToken.value().equals("(")) {
                compileCurrentToken();
                advance();
                compileParameterList();
            }
            compileCurrentToken();
            advance();
        }

        compileSubroutineBody();

        lines.add(closeTag);
    }

    // Parameter List Grammar: ((type varName) ',' (type varName)*)?
    private void compileParameterList () {
        String openTag = "<parameterList>", closeTag = "</parameterList>";
        lines.add(openTag);

        while(!currentToken.value().equals(")")) {
            compileCurrentToken();
            advance();
        }

        lines.add(closeTag);
    }

    // Subroutine Body Grammar: '{' varDec* statements '}'
    private void compileSubroutineBody () {
        String openTag = "<subroutineBody>", closeTag = "</subroutineBody>";
        lines.add(openTag);

        // compile all variable declarations
        while (!SymbolTable.isStatement(currentToken.value())) {
            compileCurrentToken();
            advance();
        }

        // compile all statements
        while (!currentToken.value().equals("}")) {
            compileStatements();
        }

        compileCurrentToken();
        lines.add(closeTag);
    }

    // Variable Declaration grammar: 'var' type varName (',' varName)* ';'
     private void compileVarDec () {
        String openTag = "<varDec>", closeTag = "</varDec>";
        lines.add(openTag);
        lines.add(currentToken.XMLTag());
        advance();

        while (!currentToken.value().equals(";")) {
            compileCurrentToken();
            advance();
        }
        
        compileCurrentToken();
        lines.add(closeTag);
    }

    // Statement grammar: statements*
    // where statement can be one of: letStatement, ifStatement, whileStatement, 
    //                                doStatement, returnStatement
    private void compileStatements () {
        String openTag = "<statements>", closeTag = "</statements>";
        lines.add(openTag);

        while (SymbolTable.isStatement(currentToken.value())) {
            compileCurrentToken();
            advance();
        }

        lines.add(closeTag);
    }

    // Let Statement Grammar: 'let' varName('['expression']')? '=' expression ';'
    private void compileLet () {
        String openTag = "<letStatement>", closeTag = "</letStatement>";
        lines.add(openTag);
        lines.add(currentToken.XMLTag());
        advance();

        while (!currentToken.value().equals(";")) {
            if (currentToken.value().equals("[")) {
                compileCurrentToken();
                advance();
                compileExpression();
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
        lines.add(closeTag);
    }

    // Do Statement Grammar: 'do' subroutineCall ';'
    // where subroutineCall is one of: subroutineName'('expressionList')'
    //                                 (className|varName)'.'subroutineName'('expressionList')'
    private void compileDo () {
        String openTag = "<doStatement>", closeTag = "</doStatement>";
        lines.add(openTag);
        lines.add(currentToken.XMLTag());
        advance();

        while (!currentToken.value().equals(";")) {
            if (currentToken.value().equals("(")) {
                compileCurrentToken();
                advance();
                compileExpressionList();
            }
            compileCurrentToken();
            advance();
        }

        compileCurrentToken();
        lines.add(closeTag);
    }

    // Return Statement Grammar: 'return' expression? ';'
    private void compileReturn () {
        String openTag = "<returnStatement>", closeTag = "</returnStatement>";
        lines.add(openTag);
        lines.add(currentToken.XMLTag());
        advance();

        if (!currentToken.value().equals(";")) {
            compileExpression();
        }

        compileCurrentToken();
        lines.add(closeTag);
    }

    // While Statement Grammar: 'while' '('expression')' '{'statements'}'
    private void compileWhile () {
        String openTag = "<whileStatement>", closeTag = "</whileStatement>";
        lines.add(openTag);
        lines.add(currentToken.XMLTag());
        advance();

        while (!currentToken.value().equals("}")) {
            if (currentToken.value().equals("(")) {
                compileCurrentToken();
                advance();
                compileExpression();
            } else if (currentToken.value().equals("{")) {
                compileCurrentToken();
                advance();
                compileStatements();
                continue;
            }
            compileCurrentToken();
            advance();
        }

        compileCurrentToken();
        lines.add(closeTag);
    }

    // If Statement Grammar: 'if' '('expression')' '{'statements'}' ('else' '{'statements'}')?
    private void compileIf () {
        String openTag = "<ifStatement>", closeTag = "</ifStatement>";
        lines.add(openTag);
        lines.add(currentToken.XMLTag());
        advance();

        while (!currentToken.value().equals("{")) {
            if (currentToken.value().equals("(")) {
                compileCurrentToken();
                advance();
                compileExpression();
            }
            compileCurrentToken();
            advance();
        }

        compileCurrentToken();
        advance();
        compileStatements();
        compileCurrentToken();
   
        Token nextToken = tokens.peekFirst();

        if (nextToken.value().equals("else")) {
            advance();
            compileCurrentToken();
            advance();
            compileCurrentToken();
            advance();
            compileStatements();
            compileCurrentToken();
        }

        lines.add(closeTag);
    }

    // Expression Grammar: term (op term)*
    private void compileExpression () {
        String openTag = "<expression>", closeTag = "</expression>";
        lines.add(openTag);
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
                    advance();
                }
            } else {
                compileTerm();
            }
            unaryFlag = false;
        }

        lines.add(closeTag);
    }

    // Term Grammar: A term can be one of,
    //  - integerConstant
    //  - stringConstant
    //  - keywordConstant
    //  - varName or varName'['expression']
    //  - '('expression')'
    //  - (unaryOp term)
    //  - subroutineCall
    // SubroutineCall grammar: a subroutineCall can be one of,
    //  - subroutineName'('expressionList')
    //  - (className|varName)'.'subroutineName'('expressionList')
    // Op can be one of:  '+', '-', '*', '/', '&', '|', '<', '>', '='
    private void compileTerm () {
        String openTag = "<term>", closeTag = "</term>";

        switch (currentToken.type()) {
            case SYMBOL:
                // compile unaryOp term
                if (currentToken.symbol() == '-' || currentToken.symbol() == '~') {
                    lines.add(openTag);
                    compileCurrentToken();      // compile unaryOp
                    advance();
                    compileTerm();              // compile term
                }
                // compile '('expression')' 
                else if (currentToken.symbol() == '(') {
                    lines.add(openTag);
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
                lines.add(openTag);
                Token nextToken = tokens.peekFirst();
                if (nextToken.type() == Tokens.SYMBOL) {
                    // compile varName'['expression']'
                    if (nextToken.symbol() == '[') {
                        compileCurrentToken();  // compile varName
                        advance();
                        compileCurrentToken();  // compile '['
                        advance();
                        compileExpression();    // compile expression
                        compileCurrentToken();  // compile ']'
                        advance();
                    } else if (nextToken.symbol() == '.' || nextToken.symbol() == '(') {
                        while (!currentToken.value().equals("(")) {
                            compileCurrentToken();
                            advance();
                        }

                        compileCurrentToken();
                        advance();
                        compileExpressionList();
                        compileCurrentToken();
                        advance();
                    } else {
                        compileCurrentToken();
                        advance();
                    }
                    break;
                } else {
                    compileCurrentToken();
                    advance();
                    break;
                }

            default:
                lines.add(openTag);
                compileCurrentToken();
                advance();
                break;
        }

        lines.add(closeTag);
    }

    private void compileExpressionList () {
        String openTag = "<expressionList>", closeTag = "</expressionList>";
        lines.add(openTag);

        while (!currentToken.value().equals(")")) {
            if (currentToken.value().equals(",")) {
                compileCurrentToken();
                advance();
                continue;
            }
            compileExpression();
        }

        lines.add(closeTag);
    }

    // compileExpression;
    // compileTerm;
    // int compileExpressionList;
}
