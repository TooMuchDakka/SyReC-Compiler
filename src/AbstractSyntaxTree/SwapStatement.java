package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.Gate;

import java.util.ArrayList;

public class SwapStatement extends Statement {

    private final SignalExpression firstSignal;
    private final SignalExpression secondSignal;

    public SwapStatement(SignalExpression firstSignal, SignalExpression secondSignal, boolean lineAware) {
        super(lineAware);
        this.firstSignal = firstSignal;
        this.secondSignal = secondSignal;
    }

    @Override
    public ArrayList<Gate> generate(CodeMod module) {
        firstSignal.generate(module);
        secondSignal.generate(module);
        return Code.swap(firstSignal, secondSignal);
    }
}
