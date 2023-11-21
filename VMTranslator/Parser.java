public class Parser {
    private String arg1;
    private Commands commandType;
    private int arg2;

    // return the command type of the currently loaded command
    public Commands commandType() {
        return commandType;
    }

    // return argument 1 of the currently loaded command
    public String argument1() {
        return arg1;
    }

    // return argument 2 of the currently loaded command
    public int argument2() {
        return arg2;
    }

    // parse and load a line of code ignoring all whitespace and comments
    // return null if the line doesn't have a code expression
    public String parseLine(String line) {
        boolean flag = false;
        StringBuilder expression = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '/')
                break; // break at comment start

            flag = true; // line has valid code
            expression.append(current);
        }

        String result = expression.toString().trim();

        if (!flag || result.length() == 0)
            return null; // return null if line doesn't contain code
        
        parseCommand(result);
        return result;
    }

    // parse a given line of VM code to determine the command type and extract arguments
    private void parseCommand(String codeLine) {
        String[] components = codeLine.split("\\s+");
        if (components.length == 1) { 
            if (components[0].startsWith("return")) {
                commandType = Commands.RETURN;
            } else {
                commandType = Commands.ARITHMETIC;
            }
            arg1 = components[0];
            arg2 = 0;
            return;
        }

        arg1 = components[1];
        arg2 = 0;

        if (components[0].startsWith("label")) {
            commandType = Commands.LABEL;
            return;
        } else if (components[0].startsWith("if")) {
            commandType = Commands.IF;
            return;
        } else if (components[0].startsWith("goto")) {
            commandType = Commands.GOTO;
            return;
        }


        if (components[0].startsWith("push")) {
            commandType = Commands.PUSH;
        } else if (components[0].startsWith("pop")) {
            commandType = Commands.POP;
        } else if (components[0].startsWith("function")) {
            commandType = Commands.FUNCTION;
        } else {
            commandType = Commands.CALL;
        }

        arg2 = Integer.parseInt(components[2]);
    }
}
