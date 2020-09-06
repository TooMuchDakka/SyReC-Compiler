package SymTable;

import java.util.HashMap;
import java.util.Map;

public class SymTable {

    private Map<String, Mod> modules = new HashMap<String, Mod>();

    public boolean addModule(String name) {
        if (modules.containsKey(name)) {
            return false;
        }
        modules.put(name, new Mod(name));
        return true;
    }

    public Mod getModule(String name) {
        return modules.get(name);
    }
}
