package AbstractSyntaxTree;

import CodeGen.ExpressionResult;
import SymTable.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
                if (module == null || module.getCurrentLoopVariableValue(ident) == null) {
                    return new ExpressionResult(-1);
                }
                return new ExpressionResult(module.getCurrentLoopVariableValue(ident));
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
    public int getWidth(Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        return 0; //TODO change this to log2(int)?
    }

    @Override
    public Optional<Integer> tryGetWidth(Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        return Optional.empty();    // TODO:
    }

    public int evaluate(Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup){
        switch (kind) {
            case INT:
                return num;
            case LOOPVAR:
                if (!loopVariableRangeDefinitionLookup.containsKey(ident))
                    throw new UnsupportedOperationException("Could not determine value of loop variable " + ident + " during evaluation of number expression");
                return loopVariableRangeDefinitionLookup.get(ident).currentValue;
            case PLUS:
                return firstNum.evaluate(loopVariableRangeDefinitionLookup) + secondNum.evaluate(loopVariableRangeDefinitionLookup);
            case MINUS:
                return firstNum.evaluate(loopVariableRangeDefinitionLookup) - secondNum.evaluate(loopVariableRangeDefinitionLookup);
            case TIMES:
                return firstNum.evaluate(loopVariableRangeDefinitionLookup) * secondNum.evaluate(loopVariableRangeDefinitionLookup);
            case DIVIDE:
                return firstNum.evaluate(loopVariableRangeDefinitionLookup) / secondNum.evaluate(loopVariableRangeDefinitionLookup);
            default:
                throw new UnsupportedOperationException("Could not determine value of number expression of kind " + kind);
        }
    }

    @Override
    public NumberExpression replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        String after = replace.get(ident);
        if (after == null) {
            after = ident;
        }
        if (kind == Kind.BITWIDTH) {
            return new NumberExpression(after, kind);
        } else if (kind == Kind.INT) {
            return new NumberExpression(num);
        } else if (kind == Kind.LOOPVAR) {
            return new NumberExpression(ident, kind); //LoopVars stay the same on calls
        } else {
            return new NumberExpression(firstNum.replaceSignals(replace, currentModule), secondNum.replaceSignals(replace, currentModule), kind);
        }
    }
}
