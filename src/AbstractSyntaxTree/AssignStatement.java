package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.ExpressionResult;
import CodeGen.Gate;
import SymTable.Mod;

import java.util.ArrayList;

public class AssignStatement extends Statement {

    private final SignalExpression signalExp;
    private final Expression expression;

    public enum Kind {
        XOR, PLUS, MINUS
    }

    private Kind kind;

    public AssignStatement(SignalExpression signal, Expression exp, Kind kind, boolean lineAware) {
        super(lineAware);
        this.signalExp = signal;
        this.expression = exp;
        this.kind = kind;
    }


    @Override
    public ArrayList<Gate> generate(CodeMod module) {
        signalExp.generate(module);
        ArrayList<Gate> gates = new ArrayList<>();
        ExpressionResult res = expression.generate(module);
        gates.addAll(res.gates);
        switch (kind) {
            case XOR:
                gates.addAll(Code.xorAssign(signalExp, res));
                break;
            case PLUS:
                //TODO plusassign
                break;
            case MINUS:
                //TODO minusassign
                break;
        }
        if (lineAware) {
            gates.addAll(Code.reverseGates(res.gates));
            expression.resetLines(module);
        }
        return gates;
    }

    @Override
    public AssignStatement replaceSignals(String before, String after, Mod currentModule) {
        return new AssignStatement(signalExp.replaceSignals(before, after, currentModule), expression.replaceSignals(before, after, currentModule), kind, lineAware);
    }


}
