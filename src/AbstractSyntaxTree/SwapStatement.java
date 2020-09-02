package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.Gate;
import SymTable.Mod;

import java.util.ArrayList;
import java.util.HashMap;

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
    public SwapStatement replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        return new SwapStatement(firstSignal.replaceSignals(replace, currentModule), secondSignal.replaceSignals(replace, currentModule), lineAware);
    }
}
