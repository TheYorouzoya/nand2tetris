import java.util.HashMap;

public class SymbolTable {
    private HashMap<String, String> destJumpTable, compTable;

    public SymbolTable() {
        destJumpTable = new HashMap<>();
        compTable = new HashMap<>();
        loadTables();
    }

    private void loadTables() {
        // Load destination binaries
        for (Destinations d : Destinations.values()) {
            destJumpTable.put(d.mnemonic(), d.binary());
        }
        // Load jump binaries
        for (Jumps j : Jumps.values()) {
            destJumpTable.put(j.mnemonic(), j.binary());
        }
        // Load computation binaries
        for (Computations c : Computations.values()) {
            compTable.put(c.mnemonic(), c.binary());
        }
    }

    // parse a given C-instruction and return its binary equivalent
    public String parseInstruction(String instruction) {
        // An instruction is of type "dest=comp;jump"
        // Either "dest" or "jump" field may be omitted
        // If "=" is omitted, "dest" is null
        // If ";" is omitted, "jump" is null

        String dest, comp, jump;
        StringBuilder container = new StringBuilder();
        boolean jumpFlag = false;
        comp = "";
        dest = "null";
        jump = "null";

        for (int i = 0; i < instruction.length(); i++) {
            char current = instruction.charAt(i);
            if (current == '=') {
                dest = container.toString();
                container.setLength(0);
            } else if (current == ';') {
                jumpFlag = true;
                comp = container.toString();
                container.setLength(0);
            } else {
                container.append(current);
            }
        }

        if (jumpFlag) {
            jump = container.toString();
        } else {
            comp = container.toString();
        }

        String binary = "111" + compTable.get(comp) + destJumpTable.get(dest)
                                    + destJumpTable.get(jump);

        return binary;
    }

    // return a 16-bit binary representation of a given integer address
    // truncate any extra leading bits past 16
    public String parseAddress(String address) {
        int add = Integer.parseInt(address);
        return parseAddress(add);
    }

    public String parseAddress(int address) {
        String parsed = Integer.toBinaryString( (1 << 16) | address);
        return parsed.substring(parsed.length() - 16);
    }

    public static void main(String[] args) {

    }
}
