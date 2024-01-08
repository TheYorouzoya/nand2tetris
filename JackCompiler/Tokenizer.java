import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayDeque;

public class Tokenizer {
    private ArrayDeque<Token> tokens;

    public Tokenizer (File file) {
        tokens = new ArrayDeque<>();

        // assume the given file instance has been validated already
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            
            boolean stringFlag, lineCommentFlag, multiLineCommentFlag, commentStart, commentEnd;
            stringFlag = false;
            lineCommentFlag = false;
            multiLineCommentFlag = false;
            commentStart = false;
            commentEnd = false;

            int r;
            StringBuilder currentToken = new StringBuilder();

            while ((r = reader.read()) != -1) {
                char current = (char) r;

                if (lineCommentFlag) {
                    if (current == '\n') {
                        lineCommentFlag = false;
                        continue;
                    }
                    continue;
                }

                if (multiLineCommentFlag) {
                    if (commentEnd && current == '/') {
                        multiLineCommentFlag = false;
                        continue;
                    }
                    if (current == '*') {
                        commentEnd = true;
                    } else {
                        commentEnd = false;
                    }
                    continue;
                }
                
                if (stringFlag) {
                    if (current == '"') {
                        if (currentToken.length() > 0) {
                            tokens.addLast(new Token(currentToken.toString(), Tokens.STRING_CONST));
                            currentToken.setLength(0);
                        }
                        stringFlag = false;
                        continue;
                    }
                    currentToken.append(current);
                    continue;
                }

                if (commentStart) {
                    if (current == '/') {
                        lineCommentFlag = true;
                        commentStart = false;
                        tokens.removeLast();
                        continue;
                    }
                    if (current == '*') {
                        multiLineCommentFlag = true;
                        commentStart = false;
                        tokens.removeLast();
                        continue;
                    }
                    commentStart = false;
                }

                if (current == '"') {
                    if (currentToken.length() > 0) {
                        tokens.addLast(new Token(currentToken.toString()));
                        currentToken.setLength(0);
                    }
                    stringFlag = true;
                    continue;
                }

                if (current == '/') {
                    commentStart = true;
                }

                if (SymbolTable.isSymbol(current) || Character.isWhitespace(current)) {
                    if (currentToken.length() > 0) {
                        tokens.addLast(new Token(currentToken.toString()));
                        currentToken.setLength(0);
                    }
                    if (!Character.isWhitespace(current)) {
                        tokens.addLast(new Token(String.valueOf(current)));
                    }
                    continue;
                }

                currentToken.append(current);
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    } 

    public boolean hasMoreTokens() {
        return !tokens.isEmpty();
    }

    public ArrayDeque<Token> tokens() {
        return this.tokens;
    }

    public static void main(String[] args) {
        String filename = args[0];
        File file = new File (filename);
        System.out.println("Printing test");
        Tokenizer obj = new Tokenizer(file);
        for (Token token : obj.tokens) {
            System.out.println(token.type().openTag() + " " 
                             + token.value() + " " 
                             + token.type().closeTag());
        }
    }
}
