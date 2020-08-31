package AbstractSyntaxTree;

import CodeGen.ExpressionResult;

public class NumberExpression extends Expression {

    public enum Kind {
        INT, BITWIDTH, LOOPVAR, PLUS, MINUS, TIMES, DIVIDE
    }

    private final int num;
    private final String ident;
    private final NumberExpression firstNum;
    private final NumberExpression secondNum;
    private final Kind kind;

    public NumberExpression(int num) {
        kind = Kind.INT;
        this.num = num;
        ident = null;
        firstNum = null;
        secondNum = null;
    }

    public NumberExpression(String ident, Kind kind) {
        this.ident = ident;
        this.kind = kind;
        num = -1;
        firstNum = null;
        secondNum = null;
    }

    public NumberExpression(NumberExpression firstNum, NumberExpression secondNum, Kind kind) {
        this.firstNum = firstNum;
        this.secondNum = secondNum;
        this.kind = kind;
        num = -1;
        ident = null;
    }

    @Override
    public ExpressionResult generate(CodeMod module) {
        switch (kind) {
            case INT:
                return new ExpressionResult(num);
            case BITWIDTH:
                return new ExpressionResult(module.getVarWidth(ident));
            case LOOPVAR:
                if (module == null || module.getLoopVar(ident) == null) {
                    return new ExpressionResult(-1);
                }
                return new ExpressionResult(module.getLoopVar(ident));
            case PLUS:
                return new ExpressionResult(firstNum.generate(module).number + secondNum.generate(module).number);
            case MINUS:
                return new ExpressionResult(firstNum.generate(module).number - secondNum.generate(module).number);
            case TIMES:
                return new ExpressionResult(firstNum.generate(module).number * secondNum.generate(module).number);
            case DIVIDE:
                return new ExpressionResult(firstNum.generate(module).number / secondNum.generate(module).number);
        }
        return new ExpressionResult(0);
    }

    @Override
    public int getWidth() {
        return 0; //TODO change this to log2(int)?
    }
}
