package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.ExpressionResult;
import CodeGen.Gate;
import SymTable.Mod;

import java.util.ArrayList;
import java.util.HashMap;

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

        if (res.isNumber && res.number == 0)
            return new ArrayList<>();

        switch (kind) {
            case XOR:
                gates.addAll(Code.xorAssign(signalExp, res, module.getLoopVariableRangeDefinitionsLookup()));
                break;
            case PLUS: {
                SignalExpression additionalLinesRequiredForSynthesis = module.getAdditionalLines(1);
                gates.addAll(Code.plusAssign(signalExp, res, additionalLinesRequiredForSynthesis, module.getLoopVariableRangeDefinitionsLookup()));
                break;
            }
            case MINUS: {
                SignalExpression additionalLinesRequiredForSynthesis = module.getAdditionalLines(1);
                gates.addAll(Code.minusAssign(signalExp, res, additionalLinesRequiredForSynthesis, module.getLoopVariableRangeDefinitionsLookup()));
                break;
            }
        }
        if (lineAware) {
            gates.addAll(Code.reverseGates(res.gates));
            expression.resetLines(module);
        }
        return gates;
    }

    @Override
    public AssignStatement replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        return new AssignStatement(signalExp.replaceSignals(replace, currentModule), expression.replaceSignals(replace, currentModule), kind, lineAware);
    }
}
