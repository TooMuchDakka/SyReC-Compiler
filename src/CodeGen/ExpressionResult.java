package CodeGen;

import AbstractSyntaxTree.SignalExpression;

import java.util.ArrayList;
import java.util.HashSet;

public class ExpressionResult {
    //a Expression can either return a number or a Signal
    //to not use lines when an internal integer would suffice we wrap the signal or number in this object
    //also has a list of all the gates to generate this line if needed

    public final boolean isNumber;
    public final SignalExpression signal; //lines of this
    public final int number;
    public final ArrayList<Gate> gates;



    public ExpressionResult(SignalExpression signal) {
        isNumber = false;
        this.signal = signal;
        number = -1;
        gates = new ArrayList<>();
    }

    public ExpressionResult(SignalExpression signal, ArrayList<Gate> gates) {
        isNumber = false;
        this.signal = signal;
        number = -1;
        this.gates = new ArrayList<>(gates);
    }

    public ExpressionResult(int number) {
        isNumber = true;
        signal = null;
        this.number = number;
        gates = new ArrayList<>();
    }



    public int getWidth() {
        return signal.getWidth();
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
