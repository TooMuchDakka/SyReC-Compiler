package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.Gate;
import SymTable.Mod;

import java.util.ArrayList;
import java.util.HashMap;

public class UnaryStatement extends Statement {

    private final SignalExpression signalExp;

    public enum Kind {
        NEGATE, INCREMENT, DECREMENT
    }

    private Kind kind;

    public UnaryStatement(SignalExpression signal, Kind kind, boolean lineAware) {
        super(lineAware);
        this.signalExp = signal;
        this.kind = kind;
    }


    @Override
    public ArrayList<Gate> generate(CodeMod module) {
        signalExp.generate(module);
        switch (kind) {
            case NEGATE:
                return Code.not(signalExp);
            case INCREMENT:
                return Code.increment(signalExp);
            case DECREMENT:
                return Code.decrement(signalExp);
        }
        return null;
    }

    @Override
    public UnaryStatement replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        return new UnaryStatement(signalExp.replaceSignals(replace, currentModule), kind, lineAware);
    }
}
