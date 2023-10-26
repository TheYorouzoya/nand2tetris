import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class VMTranslator {
    public static void main(String[] args) {
        String inputFile = args[0];
        File file = new File(inputFile);
        String filename = file.getName();
        filename = filename.substring(0, filename.lastIndexOf("."));

        int index = inputFile.lastIndexOf(".");
        String outputFile = inputFile.substring(0, index) + ".asm";
        
        Parser parser = new Parser();
        Translator translator = new Translator(filename);

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                String inLine = null, outLine = null, codeLine = null;
                while((inLine = reader.readLine()) != null) {
                    codeLine = parser.parseLine(inLine);

                    if (codeLine == null) continue;

                    String arg1 = parser.argument1();
                    int arg2 = parser.argument2();
                    outLine = translator.translateCode(codeLine, parser.commandType(), arg1, arg2);
                    writer.write(outLine);
                    writer.newLine();

                }

                outLine = translator.endLoop();
                writer.write(outLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
