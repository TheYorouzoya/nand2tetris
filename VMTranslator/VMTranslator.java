import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class VMTranslator {
    public static void main(String[] args) {
        String inputFile = args[0], outputFile;
        File file = new File(inputFile);
        File[] files = new File[1];
        boolean exists = file.exists();
        boolean isDirectory = file.isDirectory();

        if (!exists) {
            throw new IllegalArgumentException("Given file/path does not exist in the file system.");
        }

        if (isDirectory) {
            outputFile = file.getAbsolutePath() + File.separator + file.getName() + ".asm";
            files = file.listFiles((dir, name) -> name.endsWith(".vm"));
        } else {
            outputFile = inputFile.substring(0, inputFile.lastIndexOf(".")) + ".asm";
            files[0] = file;
        }

        if (files == null) {
            throw new IllegalArgumentException("Given path does not contain valid .vm file(s)");
        }
        
        Parser parser = new Parser();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            if (isDirectory) {
                writer.write(Translator.bootstrapCode());
            }

            for (File VMFile : files) {
                String inFileName = VMFile.getName();
                inFileName = inFileName.substring(0, inFileName.lastIndexOf("."));
                Translator translator = new Translator(inFileName);
                
                try (BufferedReader reader = new BufferedReader(new FileReader(VMFile))) {
                    String inLine = null, outLine = null, codeLine = null;
                    while ((inLine = reader.readLine()) != null) {
                        codeLine = parser.parseLine(inLine);

                        if (codeLine == null)
                            continue;

                        String arg1 = parser.argument1();
                        int arg2 = parser.argument2();
                        outLine = translator.translateCode(codeLine, parser.commandType(), arg1, arg2);
                        writer.write(outLine);
                        writer.newLine();

                    }

                } catch (IOException e) {
                    System.out.println("Error in reading input file");
                    e.printStackTrace();
                }
            }
            writer.write(Translator.endLoop());

        } catch (IOException e) {
            System.out.println("Error in writing output file");
            e.printStackTrace();
        }
    }
}
