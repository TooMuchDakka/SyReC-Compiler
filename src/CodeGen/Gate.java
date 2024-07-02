package CodeGen;

import java.util.ArrayList;

public class Gate {
    //a class to hold the information of every gate
    public enum Kind {
        Toffoli, Fredkin, Peres, V, Vplus, Placeholder
    }

    public final Kind kind; //which Gate this object describes
    private final ArrayList<String> controlLines; //the signals used to control this gate
    private final ArrayList<String> targetLines;

    public Gate(Kind kind) {
        this.kind = kind;
        controlLines = new ArrayList<>();
        targetLines = new ArrayList<>();
    }

    public Gate(Gate gate) {
        this.kind = gate.kind;
        controlLines = new ArrayList<>(gate.getControlLines());
        targetLines = new ArrayList<>(gate.getTargetLines());
    }

    public void addControlLines(ArrayList<String> controlLines) {
        this.controlLines.addAll(controlLines);
    }

    public void addControlLine(String controlLine) {
        controlLines.add(controlLine);
    }

    public void addTargetLines(ArrayList<String> targetLines) {
        this.targetLines.addAll(targetLines);
    }

    public void addTargetLine(String targetLine) {
        targetLines.add(targetLine);
    }

    public ArrayList<String> getControlLines() {
        return new ArrayList<>(controlLines);
    }

    public ArrayList<String> getTargetLines() {
        return new ArrayList<>(targetLines);
    }

    public int getNumberOfLines(){return controlLines.size() + targetLines.size();}

    public int getNumberOfControlLines() { return controlLines.size(); }
    public int getNumberOfTargetLines() { return targetLines.size(); }
}
