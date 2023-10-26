public class Translator {
    private String filename;
    private int labelCounter;

    public Translator(String filename) {
        this.filename = filename;
        labelCounter = 0;
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

            default:
                break;
        }
        return codeBlock.append(translation).toString();
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

    private String translatePushPop(Commands command, String segment, int index) {
        String parsedSegment = parseSegment(segment, index);
        String end = null;
        String boilerplate = "@" + parsedSegment + "\n"
                           + "D=M\n"
                           + "@" + index + "\n"
                           + "A=D+A\n";

        if (segment.equals("temp") || segment.equals("pointer")) {
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
            case "temp":
                return "R" + (5 + arg2);
            default:
                return filename + "." + arg2;
        }
    }

    public String endLoop() {
        return "(END)\n"
             + "@END\n"
             + "0;JMP\n";
    }

}
