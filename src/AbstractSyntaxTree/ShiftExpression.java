package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.ExpressionResult;
import SymTable.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ShiftExpression extends Expression {

    private final Expression expression;
    private final NumberExpression number;

    public enum Kind {
        LEFT, RIGHT
    }

    private Kind kind;

    public ShiftExpression(Expression expression, NumberExpression number, Kind kind) {
        this.expression = expression;
        this.number = number;
        this.kind = kind;
        containedSignals.addAll(expression.containedSignals);
    }

    @Override
    public ExpressionResult generate(CodeMod module) {
        ExpressionResult res = expression.generate(module);
        usedLines.addAll(expression.usedLines);
        int numberRes = number.generate(module).number;
        switch (kind) {
            case LEFT:
                if (res.isNumber) {
                    return new ExpressionResult(res.number << numberRes);
                } else {
                    SignalExpression shiftLine = module.getAdditionalLines(expression.getWidth(module.getLoopVariableRangeDefinitionsLookup()));
                    usedLines.addAll(shiftLine.getLines());
                    ExpressionResult shiftRes = new ExpressionResult(shiftLine);
                    shiftRes.gates.addAll(res.gates);
                    shiftRes.gates.addAll(Code.leftShift(res, numberRes, shiftLine, module.getLoopVariableRangeDefinitionsLookup()));
                    return shiftRes;
                }
            case RIGHT:
                if (res.isNumber) {
                    return new ExpressionResult(res.number >> numberRes);
                } else {
                    SignalExpression shiftLine = module.getAdditionalLines(expression.getWidth(module.getLoopVariableRangeDefinitionsLookup()));
                    usedLines.addAll(shiftLine.getLines());
                    ExpressionResult shiftRes = new ExpressionResult(shiftLine);
                    shiftRes.gates.addAll(res.gates);
                    shiftRes.gates.addAll(Code.rightShift(res, numberRes, shiftLine, module.getLoopVariableRangeDefinitionsLookup()));
                    return shiftRes;
                }
        }
        return new ExpressionResult(0);
    }

    @Override
    public int getWidth(Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        return expression.getWidth(loopVariableRangeDefinitionLookup);
    }

    @Override
    public Optional<Integer> tryGetWidth(Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        return expression.tryGetWidth(loopVariableRangeDefinitionLookup);
    }

    @Override
    public ShiftExpression replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        return new ShiftExpression(expression.replaceSignals(replace, currentModule), number.replaceSignals(replace, currentModule), kind);
    }
}
