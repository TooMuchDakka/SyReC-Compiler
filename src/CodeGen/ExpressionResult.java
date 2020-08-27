package CodeGen;

import java.util.ArrayList;
import java.util.HashSet;

public class ExpressionResult {
    //a Expression can either return a number or a Signal
    //to not use lines when an internal integer would suffice we wrap the signal or number in this object
    //also has a list of all the gates to generate this line if needed

    public final boolean isNumber;
    private final SignalObject signal; //lines of this
    public final int number;
    public final ArrayList<Gate> gates;
    public final HashSet<String> containedSignals;  //used for Assignment checks



    public ExpressionResult(SignalObject signal) {
        isNumber = false;
        this.signal = signal;
        number = -1;
        gates = null;
        containedSignals = new HashSet<>(signal.getLines());
    }

    public ExpressionResult(SignalObject signal, ArrayList<Gate> gates) {
        isNumber = false;
        this.signal = signal;
        number = -1;
        this.gates = new ArrayList<>(gates);
        containedSignals = new HashSet<>(signal.getLines());
    }

    public ExpressionResult(int number) {
        isNumber = true;
        signal = null;
        this.number = number;
        gates = null;
        containedSignals = null;
    }

    public void addContainedSignals(ArrayList<String> signals) {
        containedSignals.addAll(signals);
    }


    public int getWidth() {
        return signal.getWidth();
    }

    public boolean containsSignal(String signalName) {
        if(containedSignals == null) {
            return false;
        }
        return containedSignals.contains(signalName);
    }

    public ArrayList<String> getContainedSignals() {
        return new ArrayList<>(containedSignals);
    }


    public String getLineName(int i) {
        return signal.getLineName(i);
    }

    public ArrayList<String> getLines() {
        ArrayList<String> returnLines = new ArrayList<>();
        returnLines.addAll(signal.getLines());
        return returnLines;
    }
}
