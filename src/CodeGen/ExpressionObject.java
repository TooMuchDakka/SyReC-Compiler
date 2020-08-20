package CodeGen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ExpressionObject {
    //a Expression can either return a number or a Signal
    //to not use lines when an internal integer would suffice we wrap the signal or number in this object

    public final boolean isNumber;
    public final ArrayList<SignalObject> signals;
    public final int number;
    public final int resetStart;    //start of the gates to reset after expression is used
    public final int resetEnd;      //end of the gates to reset
    public final HashSet<String> containedSignals;  //used for Assignment checks



    public ExpressionObject(SignalObject signal) {
        isNumber = false;
        signals = new ArrayList<>();
        signals.add(signal);
        number = -1;
        resetStart = -1;
        resetEnd = -1;
        containedSignals = new HashSet<>(signal.getLines());
    }

    public ExpressionObject(SignalObject signal, int resetStart, int resetEnd) {
        isNumber = false;
        signals = new ArrayList<>();
        signals.add(signal);
        number = -1;
        this.resetStart = resetStart;
        this.resetEnd = resetEnd;
        containedSignals = new HashSet<>(signal.getLines());
    }

    public ExpressionObject(int number) {
        isNumber = true;
        signals = null;
        this.number = number;
        resetStart = -1;
        resetEnd = -1;
        containedSignals = null;
    }

    public void addContainedSignals(ArrayList<String> signals) {
        containedSignals.addAll(signals);
    }

    public void addSignals(ArrayList<SignalObject> signals) {
        this.signals.addAll(signals);
    }

    public int getWidth() {
        int width = 0;
        for (SignalObject signal : signals) {
            width += signal.getWidth();
        }
        return width;
    }

    public boolean containsSignal(String signalName) {
        return containedSignals.contains(signalName);
    }

    public ArrayList<String> getContainedSignals() {
        return new ArrayList<>(containedSignals);
    }


    public String getLineName(int i) {
        int j = 0;
        while(signals.get(j).getWidth() < i) {
            i -= signals.get(j).getWidth();
            j++;
        }
        return signals.get(j).getLineName(i);
    }

    public ArrayList<String> getLines() {
        ArrayList<String> returnLines = new ArrayList<>();
        for(SignalObject signal : signals) {
            returnLines.addAll(signal.getLines());
        }
        return returnLines;
    }
}
