import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import enums.MemSegments;
import enums.VMCommands;

public class VMWriter {
    private BufferedWriter writer;

    public VMWriter (String outputFile) {
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writePush (MemSegments segment, int index) throws IOException {
        writer.write("push " + segment.toString().toLowerCase() + " " + index);
        writer.newLine();
    }

    public void writePop (MemSegments segment, int index) throws IOException {
        writer.write("pop " + segment.toString().toLowerCase() + " " + index);
        writer.newLine();
    }

    public void writeArithmetic (VMCommands command) throws IOException {
        writer.write(command.toString().toLowerCase());
        writer.newLine();
    }

    public void writeLabel (String label) throws IOException {
        writer.write("label " + label);
        writer.newLine();
    }

    public void writeGoto (String label) throws IOException {
        writer.write("goto " + label);
        writer.newLine();
    }

    public void writeIf (String label) throws IOException {
        writer.write("if-goto " + label);
        writer.newLine();
    }

    public void writeCall (String name, int nArgs) throws IOException {
        writer.write("call " + name + " " + nArgs);
        writer.newLine();
    }

    public void writeFunction (String name, int nVars) throws IOException {
        writer.write("function " + name + " " + nVars);
        writer.newLine();
    }

    public void writeReturn () throws IOException {
        writer.write("return");
        writer.newLine();
    }

    public void close() throws IOException {
        writer.close();
    }

}
