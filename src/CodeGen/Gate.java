package CodeGen;

import java.util.ArrayList;
import java.util.Arrays;

public class Gate {
    //a class to hold the information of every gate
    public enum Kind {
        Toffoli, Fredkin, Peres, V, Vplus
    }
    public final Kind kind; //which Gate this object describes
    private final ArrayList<String> controlLines; //the signals used to control this gate
    public final ArrayList<String> targetLines;

    public Gate(Kind kind) {
        this.kind = kind;
        controlLines = new ArrayList<>();
        targetLines = new ArrayList<>();
    }

    public void addControlLines(ArrayList<String> controlLines) {
        this.controlLines.addAll(controlLines);
    }

    public void addTargetLines(ArrayList<String> targetLines) {
        this.controlLines.addAll(targetLines);
    }

    public ArrayList<String> getControlLines() {
        return new ArrayList<>(controlLines);
    }

    public ArrayList<String> getTargetLines() {
        return new ArrayList<>(targetLines);
    }
}
