package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.ExpressionResult;

public class UnaryExpression extends Expression {

    private Expression expression;

    public enum Kind {
        LOGICAL, BITWISE
    }

    private Kind kind;

    public UnaryExpression(Expression expression, Kind kind) {
        this.expression = expression;
        this.kind = kind;
        containedSignals.addAll(expression.containedSignals);
    }

    @Override
    public ExpressionResult generate(CodeMod module) {
        ExpressionResult res = expression.generate(module);
        usedLines.addAll(expression.usedLines);
        switch (kind) {
            case LOGICAL:
                if(res.isNumber) {
                    if(res.number == 0) {
                        return new ExpressionResult(1);
                    }
                    else {
                        return new ExpressionResult(0);
                    }
                }
                else {
                    SignalExpression notLine = module.getAdditionalLines(1);
                    usedLines.addAll(notLine.getLines());
                    ExpressionResult notRes = new ExpressionResult(notLine);
                    notRes.gates.addAll(res.gates);
                    notRes.gates.addAll(Code.notExp(res, notLine));
                    return notRes;
                }
            case BITWISE:
                if(res.isNumber) {
                    return new ExpressionResult(~res.number);
                }
                else {
                    SignalExpression notLine = module.getAdditionalLines(res.getWidth());
                    usedLines.addAll(notLine.getLines());
                    ExpressionResult notRes = new ExpressionResult(notLine);
                    notRes.gates.addAll(res.gates);
                    notRes.gates.addAll(Code.notExp(res, notLine));
                    return notRes;
                }
        }
        return new ExpressionResult(0);
    }

    @Override
    public int getWidth() {
        if(kind == Kind.LOGICAL) {
            return 1;
        }
        else return expression.getWidth();
    }
}
