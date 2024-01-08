import java.io.File;

public class JackCompiler {
    public static void main(String[] args) {
        String filepath = args[0];
        File file = new File(filepath);
        File[] files = new File[1];

        boolean exists = file.exists();
        boolean isDirectory = file.isDirectory();

        if (!exists) {
            throw new IllegalArgumentException("Given file/directory does not exist!");
        }

        if (isDirectory) {
            files = file.listFiles((dir, name) -> name.endsWith(".jack"));
        } else {
            if (!file.getName().endsWith(".jack")) {
                throw new IllegalArgumentException("given file " + file.getName()
                            + "is not a valid Jack file");
            }
            files[0] = file;
        }

        if (files == null) {
            throw new IllegalArgumentException("Given directory does not contain any Jack file(s)");
        }

        for (File inputFile : files) {
            String path = inputFile.getAbsolutePath();
            String vmFile = path.substring(0, path.lastIndexOf(".")) + ".vm";
            Tokenizer tokenizer = new Tokenizer(inputFile);
            
            CompilationEngine engine = new CompilationEngine(tokenizer.tokens(), vmFile, inputFile.getName());
            engine.compileProgram();
        }
    }
}