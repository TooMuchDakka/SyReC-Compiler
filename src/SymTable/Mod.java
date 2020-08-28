package SymTable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Mod {

    //the Symtable module
    public final String name;
    private int signalCount = 0; //count number of signals (for REAL format)
    private int parameterCount = 0; //count parameters for call
    private HashSet<String> loopVars = new HashSet<>();

    private Map<String, Obj> locals = new HashMap<String, Obj>();

    public Mod(String name) {
        this.name = name;
    }

    public boolean addObj (Obj obj) {
        if(locals.containsKey(obj.name)) {
            return false;
        }
        if(obj.kind == Obj.Kind.In || obj.kind == Obj.Kind.Inout || obj.kind == Obj.Kind.Out) {
            signalCount++;
        }
        if(obj.kind != Obj.Kind.Wire && obj.kind != Obj.Kind.State) {
            parameterCount++;
        }
        locals.put(obj.name, obj);
        return true;
    }


    public boolean isDefined(String name) {
        return locals.containsKey(name);
    }

    public int getParameterCount() {
        return parameterCount;
    } //return just the parameters

    public int getLineCount() { //return parameters+lines needed for wires (width is used in this calculation
        int count = 0;
        for (Obj signal: getLines()) {
            count+=signal.width;
        }
        return count;
    } 

    public Obj[] getLines() {
        return locals.values().toArray(new Obj[0]); //return an Array of the Objects (Lines/Parameters) of the module
    }

    public Obj getLocal(String name) {
        if(!isDefined(name)) {
            return null;
        }
        return new Obj(locals.get(name));
    }

    public HashMap<String, Obj> getLocals() {
        return new HashMap<String, Obj>(locals);
    }

    public boolean loopVarDefined(String loopVar) {
        return loopVars.contains(loopVar);
    }

    public void addLoopVar(String loopVar) {
        loopVars.add(loopVar);
    }

    public void removeLoopVar(String loopVar) {
        loopVars.remove(loopVar);
    }



}
