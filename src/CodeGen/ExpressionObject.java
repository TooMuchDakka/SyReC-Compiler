package CodeGen;

import java.util.HashSet;

public class ExpressionObject {
    //a Expression can either return a number or a Signal
    //to not use lines when an internal integer would suffice we wrap the signal or number in this object

    public final boolean isNumber;
    public final SignalObject signal;
    public final int number;
    public final int resetStart;    //start of the gates to reset after expression is used
    public final int resetEnd;      //end of the gates to reset

    private final HashSet<String> containedSignals; //all the Signals used in this expression, important for assign Statements


    public ExpressionObject(SignalObject signal) {
        isNumber = false;
        this.signal = signal;
        number = -1;
        resetStart = -1;
        resetEnd = -1;
        containedSignals = new HashSet<>(signal.getLines());
    }

    public ExpressionObject(SignalObject signal, int resetStart, int resetEnd) {
        isNumber = false;
        this.signal = signal;
        number = -1;
        this.resetStart = resetStart;
        this.resetEnd = resetEnd;
        containedSignals = new HashSet<>(signal.getLines());
    }

    public ExpressionObject(int number) {
        isNumber = true;
        signal = null;
        this.number = number;
        resetStart = -1;
        resetEnd = -1;
        containedSignals = new HashSet<>();
    }

    public void addSignals(HashSet<String> signals) {
        containedSignals.addAll(signals);
    }

    public boolean containsSignal(String signalName) {
        return containedSignals.contains(signalName);
    }

    public HashSet<String> getContainedSignals() {
        return new HashSet<String>(containedSignals);
    }


}
