package CodeGen;

import SymTable.Obj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CodeMod {
    //A Dataobject to hold the Code representation of a module
    private final String name;
    private final ArrayList<Obj> variables;   //the variables defined in the source code
    private final HashMap<String, Obj> additionalLines; //the lines that get created internally by expressions
    private final ArrayList<Obj> zeroLines; //additionalLines that are guaranteed zero
    private int addLineCounter = 0; //used to number the additional lines
    private final ArrayList<Gate> gates;

    public CodeMod(String name, HashMap<String, Obj> variables) {
        this.name = name;
        this.variables = new ArrayList<Obj>(variables.values());
        additionalLines = new HashMap<String, Obj>();
        zeroLines = new ArrayList<Obj>();
        gates = new ArrayList<Gate>();
    }

    public SignalObject getAdditionalLines(int width) {
        //returns a SignalObject consisting of additionalLines
        //tries to use as many already created lines as possible
        ArrayList<String> lines = new ArrayList<>();
        Obj temp;
        for (int i = 0; i < width; i++) {
            if(zeroLines.size() > 0) {
                temp = zeroLines.get(zeroLines.size()-1);
                temp.setGarbage(true);
                additionalLines.put(temp.name, temp);
                lines.add(temp.name);
                zeroLines.remove(zeroLines.size()-1);
            }
            else {
                temp = new Obj(Obj.Kind.Wire, "addLine"+addLineCounter, 1);
                additionalLines.put(temp.name, temp);
                lines.add(temp.name);
                addLineCounter++;
            }
        }
        return new SignalObject("addLine", lines);
    }

    public void resetLine(String lineName) {
        if(additionalLines.containsKey(lineName)) {
            Obj line = additionalLines.remove(lineName);
            //when we reset a line we know its no longer garbage
            line.setGarbage(false);
            zeroLines.add(line);
        }
    }


    public ArrayList<Obj> getVariables() {    //returns variables and lines
        ArrayList<Obj> list = new ArrayList<>(variables);
        list.addAll(additionalLines.values());
        list.addAll(zeroLines);
        return list;
    }

    public void addGate (Gate.Kind kind, String targetLine) {
        //targetLine Gate
        //TODO throw error when a Gate kind which cant have 1 target line is used to call this
        Gate gate = new Gate(kind);
        gate.addTargetLines(new ArrayList<>(List.of(targetLine)));
        gates.add(gate);
    }

    public void addGate (Gate.Kind kind, ArrayList<String> targetLines) {
        //gate with multiple targetLines
        //TODO throw error when a Gate kind that cant have two target lines calls this
        Gate gate = new Gate(kind);
        gate.addTargetLines(targetLines);
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

    public void addGate (Gate.Kind kind, String targetLine, String controlLine) {
        //gate with one target and one control lines
        //TODO throw error when a Gate kind that cant have one target lines calls this
        Gate gate = new Gate(kind);
        gate.addTargetLines(new ArrayList<>(List.of(targetLine)));
        gate.addControlLines(new ArrayList<>(List.of(controlLine)));
        gates.add(gate);
    }

    public void addGate (Gate.Kind kind, ArrayList<String> targetLines, ArrayList<String> controlLines) {
        //gate with x target and x control lines
        //TODO throw error when a Gate kind that cant have two target lines calls this
        Gate gate = new Gate(kind);
        gate.addTargetLines(targetLines);
        gate.addControlLines(controlLines);
        gates.add(gate);
    }

    public ArrayList<Gate> getGates() {
        return new ArrayList<>(gates);
    }

    public int getLastGateNumber() {
        return gates.size()-1;
    }

    public int getVarCount() { //return parameters+lines needed for wires (width is used in this calculation
        int count = 0;
        for (Obj signal: getVariables()) {
            count+=signal.width;
        }
        return count;
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
