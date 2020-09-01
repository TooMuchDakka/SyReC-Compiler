package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.Gate;
import SymTable.Mod;

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

    @Override
    public SwapStatement replaceSignals(String before, String after, Mod currentModule) {
        return new SwapStatement(firstSignal.replaceSignals(before, after, currentModule), secondSignal.replaceSignals(before, after, currentModule), lineAware);
    }
}
