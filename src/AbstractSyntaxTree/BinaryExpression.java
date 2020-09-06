package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.ExpressionResult;
import SymTable.Mod;

import java.util.HashMap;

public class BinaryExpression extends Expression {

    private final Expression firstExpression;
    private final Expression secondExpression;

    public enum Kind {
        PLUS, MINUS, BIT_XOR, TIMES_UPPER, DIVIDE, REMAINDER, TIMES_LOWER, LOG_AND, LOG_OR, BIT_AND, BIT_OR, LESSER, GREATER, EQL, NEQL, LEQL, GEQL
    }

    private Kind kind;

    public BinaryExpression(Expression firstExpression, Expression secondExpression, Kind kind) {
        this.firstExpression = firstExpression;
        this.secondExpression = secondExpression;
        containedSignals.addAll(firstExpression.containedSignals);
        containedSignals.addAll(secondExpression.containedSignals);
        this.kind = kind;
    }

    @Override
    public int getWidth() {
        switch (kind) {
            case PLUS:
            case MINUS:
            case BIT_XOR:
            case TIMES_UPPER:
            case DIVIDE:
            case REMAINDER:
            case TIMES_LOWER:
            case BIT_AND:
            case BIT_OR:
                //TODO once generate of these are implemented this could change
                return Math.max(firstExpression.getWidth(), secondExpression.getWidth());
            case LOG_AND:
            case LOG_OR:
            case LESSER:
            case GREATER:
            case EQL:
            case NEQL:
            case LEQL:
            case GEQL:
                return 1;   //logical Expressions always return a width of 1
        }
        return -1;
    }

    @Override
    public BinaryExpression replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        return new BinaryExpression(firstExpression.replaceSignals(replace, currentModule), secondExpression.replaceSignals(replace, currentModule), kind);
    }

    @Override
    public ExpressionResult generate(CodeMod module) {
        ExpressionResult firstRes = firstExpression.generate(module);
        ExpressionResult secondRes = secondExpression.generate(module);
        usedLines.addAll(firstExpression.usedLines);
        usedLines.addAll(secondExpression.usedLines);
        //TODO add missing BinaryExpressions
        switch (kind) {
            case PLUS:
                break;
            case MINUS:
                break;
            case BIT_XOR:
                break;
            case TIMES_UPPER:
                break;
            case DIVIDE:
                break;
            case REMAINDER:
                break;
            case TIMES_LOWER:
                break;

            case LOG_AND:
                if (firstRes.isNumber && secondRes.isNumber) {
                    if (firstRes.number == 1 && secondRes.number == 1) {
                        return new ExpressionResult(1);
                    } else {
                        return new ExpressionResult(0);
                    }
                } else if (firstRes.isNumber) {
                    if (firstRes.number == 0) {
                        return new ExpressionResult(0);
                    } else {
                        return secondRes;
                    }
                } else if (secondRes.isNumber) {
                    if (secondRes.number == 0) {
                        return new ExpressionResult(0);
                    } else {
                        return firstRes;
                    }
                } else {
                    SignalExpression andLine = module.getAdditionalLines(1);
                    usedLines.add(andLine.getLineName(0));
                    ExpressionResult res = new ExpressionResult(andLine);
                    res.gates.addAll(firstRes.gates);
                    res.gates.addAll(secondRes.gates);
                    res.gates.addAll(Code.logicalAnd(firstRes, secondRes, andLine));
                    return res;
                }

            case LOG_OR:
                if (firstRes.isNumber && secondRes.isNumber) {
                    int newValue = firstRes.number == 1 || secondRes.number == 1 ? 1 : 0;
                    return new ExpressionResult(newValue);
                } else if (firstRes.isNumber) {
                    if (firstRes.number == 1) {
                        return new ExpressionResult(1);
                    } else {
                        return secondRes;
                    }
                } else if (secondRes.isNumber) {
                    if (secondRes.number == 1) {
                        return new ExpressionResult(1);
                    } else {
                        return firstRes;
                    }
                } else {
                    SignalExpression orLine = module.getAdditionalLines(1);
                    usedLines.add(orLine.getLineName(0));
                    ExpressionResult res = new ExpressionResult(orLine);
                    res.gates.addAll(firstRes.gates);
                    res.gates.addAll(secondRes.gates);
                    res.gates.addAll(Code.logicalOr(firstRes, secondRes, orLine));
                    return res;
                }

            case BIT_AND:
                break;
            case BIT_OR:
                break;
            case LESSER:
                break;
            case GREATER:
                break;
            case EQL:
                break;
            case NEQL:
                break;
            case LEQL:
                break;
            case GEQL:
                break;
        }
        return new ExpressionResult(0);
    }

}
