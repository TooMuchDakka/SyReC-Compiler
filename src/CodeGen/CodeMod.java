package CodeGen;

import SymTable.Obj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeMod {
    //A Dataobject to hold the Code representation of a module
    private final String name;
    private final ArrayList<Obj> variables;
    private final ArrayList<Gate> gates;

    public CodeMod(String name) {
        this.name = name;
        variables = new ArrayList<Obj>();
        gates = new ArrayList<Gate>();
    }

    public void addWire(String ident) {
        //TODO add Wire i.e. for Expressions
    }

    public void addGate (Gate.Kind kind, String targetLine) {
        //targetLine Gate
        //TODO throw error when a Gate kind which cant have 1 target line is used to call this
        Gate gate = new Gate(kind);
        gate.addTargetLines(new ArrayList<>(List.of(targetLine)));
        gates.add(gate);
    }

    public void addGate (Gate.Kind kind, String targetLine1, String targetLine2) {
        //gate with just two targetLines
        //TODO throw error when a Gate kind that cant have two target lines calls this
        Gate gate = new Gate(kind);
        gate.addTargetLines(new ArrayList<>(List.of(targetLine1, targetLine2)));
        gates.add(gate);
    }

    public void addGate (Gate.Kind kind, String targetLine, ArrayList<String> controlLines) {
        //gate with one target and x control lines
        //TODO throw error when a Gate kind that cant have one target lines calls this
        Gate gate = new Gate(kind);
        gate.addTargetLines(new ArrayList<>(List.of(targetLine)));
        gate.addControlLines(controlLines);
        gates.add(gate);
    }

    public void addGate (Gate.Kind kind, String targetLine1, String targetLine2, ArrayList<String> controlLines) {
        //gate with two target and x control lines
        //TODO throw error when a Gate kind that cant have two target lines calls this
        Gate gate = new Gate(kind);
        gate.addTargetLines(new ArrayList<>(List.of(targetLine1, targetLine2)));
        gate.addControlLines(controlLines);
        gates.add(gate);
    }

    public int getLastGateNumber() {
        return gates.size()-1;
    }

    public int getVarCount() {
        return variables.size();
    }

    public void reverseGates(int startIndex, int endIndex) {
        //reverses the order of the gates between the two specified indizes
        while(startIndex < endIndex) {
            Collections.swap(gates, startIndex, endIndex);
            startIndex++;
            endIndex--;
        }
    }



}
