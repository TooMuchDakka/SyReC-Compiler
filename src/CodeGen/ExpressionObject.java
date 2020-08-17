package CodeGen;

public class ExpressionObject {
    //a Expression can either return a number or a Signal
    //to not use lines when an internal integer would suffice we wrap the signal or number in this object

    public final boolean isNumber;
    public final SignalObject signal;
    public final int number;
    public final int resetStart;    //start of the gates to reset after expression is used
    public final int resetEnd;      //end of the gates to reset


    public ExpressionObject(SignalObject signal) {
        isNumber = false;
        this.signal = signal;
        number = -1;
        resetStart = -1;
        resetEnd = -1;
    }

    public ExpressionObject(SignalObject signal, int resetStart, int resetEnd) {
        isNumber = false;
        this.signal = signal;
        number = -1;
        this.resetStart = resetStart;
        this.resetEnd = resetEnd;
    }

    public ExpressionObject(int number) {
        isNumber = true;
        signal = null;
        this.number = number;
        resetStart = -1;
        resetEnd = -1;
    }

}
