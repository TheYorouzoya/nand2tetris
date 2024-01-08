import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class VMWriter {
    ArrayList<String> lines;
    private String outputFile;

    public VMWriter (String outputFile) {
        lines = new ArrayList<>();
        this.outputFile = outputFile;
    }

    public void writePush (MemSegments segment, int index) {
        lines.add("push " + segment.toString().toLowerCase() + " " + index);
    }

    public void writePop (MemSegments segment, int index) {
        lines.add("pop " + segment.toString().toLowerCase() + " " + index);
    }

    public void writeArithmetic (VMCommands command) {
        lines.add(command.toString().toLowerCase());
    }

    public void writeLabel (String label) {
        lines.add("label " + label);
    }

    public void writeGoto (String label) {
        lines.add("goto " + label);
    }

    public void writeIf (String label) {
        lines.add("if-goto " + label);
    }

    public void writeCall (String name, int nArgs) {
        lines.add("call " + name + " " + nArgs);
    }

    public void writeFunction (String name, int nVars) {
        lines.add("function " + name + " " + nVars);
    }

    public void writeReturn () {
        lines.add("return");
    }

    public void writeFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
