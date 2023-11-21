import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class HackAssembler {
    private final ArrayList<String> codeLines;

    public HackAssembler(String filename) {
        codeLines = new ArrayList<String>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if ((line = parseLine(line)) != null) { // If line contains valid code
                    codeLines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int index = filename.lastIndexOf(".");
        String outputFilename = filename.substring(0, index) + ".hack";
        outputAssembly(outputFilename);
    }

    // parse a line of code ignoring all whitespace and comments
    // return null if the line doesn't have a code expression
    private String parseLine(String line) {
        boolean flag = false;
        StringBuilder expression = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '/') break;      // break at comment start
            if (Character.isWhitespace(current)) continue;  // ignore whitespace
            flag = true;    // line has valid code
            expression.append(current);
        }
        if (!flag) return null;     // return null if line doesn't contain code
        else return expression.toString();
    }

    private void outputAssembly(String filename) {
        CodeParser parser = new CodeParser();
        ArrayList<String> output = parser.parseLines(codeLines);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String line : output) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String filename = args[0];
        HackAssembler obj = new HackAssembler(filename);
    }
}
