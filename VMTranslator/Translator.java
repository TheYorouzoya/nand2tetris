public class Translator {
    private String filename, functionName;
    private int labelCounter, callCounter;

    public Translator(String filename) {
        this.filename = filename;
        labelCounter = 0;
        callCounter = 0;
    }

    public String translateCode(String codeLine, Commands commandType, String arg1, int arg2) {
        StringBuilder codeBlock = new StringBuilder();
        String translation = null;
        codeBlock.append("// " + codeLine + "\n");     // start block with codeline as comment
        switch (commandType) {
            case ARITHMETIC:
                translation = translateArithmetic(arg1);
                break;

            case POP: case PUSH:
                translation = translatePushPop(commandType, arg1, arg2);
                break;

            case LABEL: case GOTO: case IF:
                translation = translateLabelIfGoto(commandType, arg1);
                break;

            case FUNCTION:
                translation = translateFunction(arg1, arg2);
                break;

            case CALL:
                translation = translateCall(arg1, arg2);
                break;

            default:
                translation = translateReturn();
                break;
        }
        return codeBlock.append(translation).toString();
    }

    private String translateReturn() {
        String frame = "frame", retAdd = "retAdd";
        StringBuilder code = new StringBuilder();
        code.append(setLabel(frame, "LCL", 0, false));
        code.append(setLabel(retAdd, frame, -5, true));
        code.append("@SP\n"
                  + "A=M-1\n"
                  + "D=M\n"
                  + "@ARG\n"
                  + "A=M\n"
                  + "M=D\n");
        code.append(setLabel("SP", "ARG", 1, false));
        code.append(setLabel("THAT", frame, -1, true));
        code.append(setLabel("THIS", frame, -2, true));
        code.append(setLabel("ARG", frame, -3, true));
        code.append(setLabel("LCL", frame, -4, true));
        code.append("@" + retAdd + "\n"
                  + "A=M\n"
                  + "0;JMP\n");
        return code.toString();
    }

    private String translateCall(String functionName, int arguments) {
        String retAdd = filename + "." + functionName + "$ret." + callCounter;
        StringBuilder code = new StringBuilder();
        code.append(pushLabel(retAdd, false));
        code.append(pushLabel("LCL", true));
        code.append(pushLabel("ARG", true));
        code.append(pushLabel("THIS", true));
        code.append(pushLabel("THAT", true));
        code.append(setLabel("ARG", "SP", -5 - arguments, false));
        code.append(setLabel("LCL", "SP", 0, false));
        code.append("@" + functionName.substring(0, functionName.indexOf(".") + 1) 
                  + functionName + "\n"
                  + "0;JMP\n"
                  + "(" + retAdd + ")\n");
        callCounter++;
        return code.toString();
    }

    // put label2's value + offset into label1's location
    private String setLabel(String label1, String label2, int offset, boolean memory) {
        StringBuilder code = new StringBuilder();
        code.append("@" + label2 + "\n"
                  + "D=M\n"
                  + "@" + Math.abs(offset) + "\n");
        if (offset >= 0) {
            code.append("D=D+A\n");
        } else {
            code.append("D=D-A\n");
        }

        if (memory) {
            code.append("A=D\n"
                      + "D=M\n");
        }

        code.append("@" + label1 + "\n"
                  + "M=D\n");
        return code.toString();
    }

    private String pushLabel(String label, boolean memory) {
        StringBuilder code = new StringBuilder();
        code.append("@" + label + "\n");
        if (memory) {
            code.append("D=M\n");
        } else {
            code.append("D=A\n");
        }
        code.append("@SP\n"
                  + "M=M+1\n"
                  + "A=M-1\n"
                  + "M=D\n");
        return code.toString();
    }

    private String translateFunction(String name, int arguments) {
        functionName = name;
        StringBuilder code = new StringBuilder();
        code.append("(" + filename + "." + functionName + ")\n");
        for (int i = 0; i < arguments; i++) {
            code.append(translatePushPop(Commands.PUSH, "constant", 0));
        }
        return code.toString();
    }

    private String translateLabelIfGoto(Commands commandType, String arg1) {
        String label = filename + "." + functionName + "$" + arg1;

        if (commandType == Commands.LABEL) {
            return "(" + label + ")\n";
        } else if (commandType == Commands.GOTO) {
            return "@" + label +"\n" + "0;JMP\n";
        } else {
            return "@SP\n"
                 + "M=M-1\n"
                 + "A=M\n"
                 + "D=M\n"
                 + "@" + label + "\n"
                 + "D;JNE\n";
        }
    }

    private String translateArithmetic(String type) {
        switch (type) {
            case "eq": 
            case "gt": 
            case "lt":
                return translate_EQ_GT_LT(type);
            case "add": 
            case "sub":
            case "and":
            case "or":
                return translate_ADD_SUB_AND_OR(type);
            default:
                return translate_NOT_NEG(type);     
        }
    }

    private String translate_EQ_GT_LT(String type) {
        String start = "@SP\n"
                     + "M=M-1\n"
                     + "A=M\n"
                     + "D=M\n"
                     + "A=A-1\n"
                     + "D=D-M\n"
                     + "M=-1\n"
                     + "@END_" + labelCounter + "\n";
        String end = "@SP\n"
                   + "A=M-1\n"
                   + "M=M+1\n"
                   + "(END_" + labelCounter + ")\n";
        
        String middle = null;
        if (type.equals("eq")) {
            middle = "D;JEQ\n";
        } else if (type.equals("gt")) {
            middle = "D;JLT\n";
        }   else {
            middle = "D;JGT\n";
        }
        
        labelCounter++;
        return start + middle + end;
    }

    private String translate_ADD_SUB_AND_OR(String type) {
        String boilerplate = "@SP\n"
                           + "M=M-1\n"
                           + "A=M\n"
                           + "D=M\n"
                           + "A=A-1\n";
        switch (type) {
            case "add":
                return boilerplate + "M=D+M\n";
            case "sub":
                return boilerplate + "M=M-D\n";
            case "and":
                return boilerplate + "M=D&M\n";
            default:
                return boilerplate + "M=D|M\n";
        }
    }

    private String translate_NOT_NEG(String type) {
        String boilerplate = "@SP\n"
                           + "A=M-1\n";
        if (type.equals("not")) {
            return boilerplate + "M=!M\n";
        } else {
            return boilerplate + "M=-M\n";
        }
    }


    /**
     * Translate a given PUSH or POP VM command into equivalent machine instruction
     * <p>
     * Stack push/pop commands follow the following format:
     * <tt>push/pop segment index</tt>
     * for example, <tt> push argument 2</tt> pushes the second argument value
     * in the argument memory segment onto the program stack
     * 
     * @param command Command type, i.e., either PUSH or POP
     * @param segment 
     * @param index
     * @return
     */
    private String translatePushPop(Commands command, String segment, int index) {
        String parsedSegment = parseSegment(segment, index);
        String end = null;
        String boilerplate = "@" + parsedSegment + "\n"
                           + "D=M\n"
                           + "@" + index + "\n"
                           + "A=D+A\n";

        if (segment.equals("temp") || segment.equals("pointer") || segment.equals("static")) {
            boilerplate = "@" + parsedSegment + "\n";
        }
        
        if (command == Commands.PUSH) {
            if (parsedSegment == null) {    // segment is "constant"
                boilerplate = "@" + index + "\n"
                            + "D=A\n";
            }  else {
                boilerplate += "D=M\n";
            }
            end = "@SP\n"
                + "M=M+1\n"
                + "A=M-1\n"
                + "M=D\n";
        } else {
            end = "D=A\n"
                + "@SP\n"
                + "M=M-1\n"
                + "A=M+1\n"
                + "M=D\n"
                + "@SP\n"
                + "A=M\n"
                + "D=M\n"
                + "A=A+1\n"
                + "A=M\n"
                + "M=D\n";
        }
        return boilerplate + end; 
    }

    /**
     * Parse a given segment-argument pair into equivalent machine address
     * 
     * @param   segment string segment being parsed
     * @param   arg2    segment's argument to use for temp and static segments
     * @return          segment memory location as a machine address variable
     * 
     */
    private String parseSegment(String segment, int arg2) {
        switch (segment) {
            case "argument":
                return "ARG";
            case "local":
                return "LCL";
            case "constant":
                return null;
            case "this":
                return "THIS";
            case "that":
                return "THAT";
            case "pointer":
                if (arg2 == 0) return "THIS";
                else return "THAT";
            case "temp":    // map temp variable to register 5 onward
                return "R" + (5 + arg2);
            default:        // generate static variable address as filename.i
                return filename + "." + arg2;
        }
    }

    /**
     * @return The infinite loop placed at the program's end as machine instructions
     */
     static String endLoop() {
        return "(END)\n"
             + "@END\n"
             + "0;JMP\n";
    }

    /**
     * @return  The bootstrap code to initialize the stack frame with the appropriate
     *          memory segments.
     */
    static String bootstrapCode() {
        return "@256\n"
             + "D=A\n"
             + "@SP\n"
             + "M=D\n"
             + "@Sys.Sys.init\n"
             + "0;JMP\n";
    }

}
