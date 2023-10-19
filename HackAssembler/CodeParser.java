import java.util.ArrayList;
import java.util.HashMap;

public class CodeParser {
    private SymbolTable symbolTable;
    private HashMap<String, String> labelTable;

    public CodeParser() {
        symbolTable = new SymbolTable();
        labelTable = new HashMap<>();
        loadDefaultAddresses();
    }

    public ArrayList<String> parseLines(ArrayList<String> codeLines) {
        // First pass to parse the label declarations
        int lines = 0;
        for (String line : codeLines) {
            if (line.charAt(0) == '(') {    // Label declaration "(ABCDEF)"
                String sub = line.substring(1, line.length() - 1);  // Extract label
                labelTable.put(sub, symbolTable.parseAddress(lines));   // Label points to next line
                continue;
            }
            lines++;
        }

        // Second pass to process all code
        int memLocation = 16;   // Variables refer to RAM[16] onwards
        ArrayList<String> binaries = new ArrayList<>(lines);
        for (String line : codeLines) {
            if (line.charAt(0) == '(') continue;    // Ignore label declarations
            if (line.charAt(0) == '@') {    // Line is an A-instruction
                if (Character.isDigit(line.charAt(1))) {    // instruction refers to constant
                    binaries.add(symbolTable.parseAddress(line.substring(1)));
                } else {    // Instruction refers to label or variable
                    String sub = line.substring(1);     // Extract instruction
                    String bin = labelTable.get(sub);   // Lookup in table
                    if (bin == null) {      // instruction not in table i.e., is a symbol
                        // Assign symbol a memory location address
                        String address = symbolTable.parseAddress(memLocation++);
                        labelTable.put(sub, address);   // Add to table
                        binaries.add(address);
                    } else {    // instruction in table
                        binaries.add(bin);
                    }
                }
            } else {    // Line a C-instruction
                binaries.add(symbolTable.parseInstruction(line));
            }
        }
        return binaries;
    }

    private void loadDefaultAddresses() {
        for (DefaultAddresses d : DefaultAddresses.values()) {
            labelTable.put(d.mnemonic(), symbolTable.parseAddress(d.address()));
        }
    }

    public static void main(String[] args) {

    }
}
