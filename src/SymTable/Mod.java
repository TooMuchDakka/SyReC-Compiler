package SymTable;

import java.util.Collections;
import java.util.Map;

public class Mod {

    public final String name;
    private int inCount = 0; //count number of signals (for REAL format)
    private int outCount = 0; //if not equal circuit is not reversible

    private Map<String, Obj> locals = Collections.emptyMap();

    public Mod(String name) {
        this.name = name;
    }

    public boolean addObj (Obj obj) {
        if(locals.containsKey(obj.name)) {
            return false;
        }
        if(obj.kind == Obj.Kind.In || obj.kind == Obj.Kind.Inout) {
            inCount++;
        }
        if(obj.kind == Obj.Kind.Out || obj.kind == Obj.Kind.Inout) {
            outCount++;
        }
        locals.put(obj.name, obj);
        return true;
    }

    public boolean inOutBalanced() {
        return inCount == outCount;
    }

    public boolean isDefined(String name) {
        return locals.containsKey(name);
    }

}
