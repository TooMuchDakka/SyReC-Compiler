package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.ExpressionResult;
import CodeGen.Gate;
import SymTable.Mod;

import java.util.*;

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
    public int getWidth(Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
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
                return Math.max(firstExpression.getWidth(loopVariableRangeDefinitionLookup), secondExpression.getWidth(loopVariableRangeDefinitionLookup));
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
    public Optional<Integer> tryGetWidth(Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
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
                Optional<Integer> optionalLhsOperandBitwidth = firstExpression.tryGetWidth(loopVariableRangeDefinitionLookup);
                Optional<Integer> optionalRhsOperandBitwidth = secondExpression.tryGetWidth(loopVariableRangeDefinitionLookup);
                if (optionalLhsOperandBitwidth.isPresent() && optionalRhsOperandBitwidth.isPresent())
                    return Optional.of(Math.max(optionalLhsOperandBitwidth.get(), optionalRhsOperandBitwidth.get()));
                return Optional.empty();
            case LOG_AND:
            case LOG_OR:
            case LESSER:
            case GREATER:
            case EQL:
            case NEQL:
            case LEQL:
            case GEQL:
                return Optional.of(1);   //logical Expressions always return a width of 1
        }
        return Optional.empty();
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
                if (firstRes.isNumber && secondRes.isNumber) {
                    return new ExpressionResult(firstRes.number + secondRes.number);
                } else {
                    ExpressionResult lhsOperand = firstRes;
                    ExpressionResult rhsOperand = secondRes;

                    int bitWidthOfNonConstantOperand = Math.max(lhsOperand.getWidth(module.getLoopVariableRangeDefinitionsLookup()), rhsOperand.getWidth(module.getLoopVariableRangeDefinitionsLookup()));
                    if (rhsOperand.isNumber) {
                        rhsOperand = firstRes;
                        bitWidthOfNonConstantOperand = lhsOperand.getWidth(module.getLoopVariableRangeDefinitionsLookup());
                        if (Math.abs(rhsOperand.number) > Math.pow(2, bitWidthOfNonConstantOperand))
                            rhsOperand = new ExpressionResult(rhsOperand.number % (int) Math.pow(2, bitWidthOfNonConstantOperand));
                            //throw new UnsupportedOperationException("Constant value " + rhsOperand.number + " cannot be stored in a bitstring of length 2^" + bitWidthOfNonConstantOperand);
                    }
                    else if (lhsOperand.isNumber) {
                        bitWidthOfNonConstantOperand = rhsOperand.getWidth(module.getLoopVariableRangeDefinitionsLookup());
                        if (Math.abs(lhsOperand.number) > Math.pow(2, bitWidthOfNonConstantOperand))
                            lhsOperand = new ExpressionResult(lhsOperand.number % (int) Math.pow(2, bitWidthOfNonConstantOperand));
                            //throw new UnsupportedOperationException("Constant value " + lhsOperand.number + " cannot be stored in a bitstring of length 2^" + bitWidthOfNonConstantOperand);
                    }

                    final int numAdditionalLinesRequiredForSynthesis = bitWidthOfNonConstantOperand + 1;
                    SignalExpression plusLines = module.getAdditionalLines(numAdditionalLinesRequiredForSynthesis);

                    ArrayList<String> additionalResultStorageLines = new ArrayList<>(numAdditionalLinesRequiredForSynthesis - 1);
                    for (int i = 0; i < numAdditionalLinesRequiredForSynthesis - 1; ++i)
                        additionalResultStorageLines.add(plusLines.getLineName(i));

                    SignalExpression additionResultStorageLines = new SignalExpression(plusLines.name, additionalResultStorageLines);

                    usedLines.addAll(additionResultStorageLines.getLines());
                    ExpressionResult res = new ExpressionResult(additionResultStorageLines);
                    res.gates.addAll(firstRes.gates);
                    res.gates.addAll(secondRes.gates);

                    if (!lhsOperand.isNumber && !rhsOperand.isNumber){
                        res.gates.addAll(Code.plus(lhsOperand, rhsOperand, plusLines, module.getLoopVariableRangeDefinitionsLookup()));
                        return res;
                    }

                    int valueOfConstantValueOperand = lhsOperand.number;
                    if (rhsOperand.isNumber) {
                        rhsOperand = firstRes;
                        valueOfConstantValueOperand = rhsOperand.number;
                    }

                    /*
                    If one of the operands is a constant known at compile time, transfer the value of the constant to the additional lines used to store the backup of the rhs operand and use these additional lines to store
                    the result of the addition. I.e. the following two case distinctions are resolved as follows (with c indicating the constant number while upper case letters denote signals [with A denoting the additional lines])
                    I. (S + c) <=> A ^= c; A += S
                    II. (c + S) >=> A ^= c; A += S
                     */
                    for (int i = 0; i < numAdditionalLinesRequiredForSynthesis - 2; ++i) {
                        if (((valueOfConstantValueOperand >> i) & 1) == 1) {
                            Gate additionalLineValueSetToConstantGate = new Gate(Gate.Kind.Toffoli);
                            additionalLineValueSetToConstantGate.addTargetLine(plusLines.getLineName(i));
                            res.gates.add(additionalLineValueSetToConstantGate);
                        }
                    }

                    SignalExpression carryInAdditionalLineContainer = new SignalExpression(plusLines.name, new ArrayList<>(Arrays.asList(plusLines.getLineName(numAdditionalLinesRequiredForSynthesis - 1))));
                    res.gates.addAll(Code.plusAssign(additionResultStorageLines, rhsOperand, carryInAdditionalLineContainer, module.getLoopVariableRangeDefinitionsLookup()));
                    return res;
                }
                // TODO:
            case MINUS:
                if (firstRes.isNumber && secondRes.isNumber) {
                    return new ExpressionResult(firstRes.number - secondRes.number);
                } else {
                    int numAdditionalLinesRequiredForSynthesis = Math.max(firstRes.getWidth(module.getLoopVariableRangeDefinitionsLookup()), secondRes.getWidth(module.getLoopVariableRangeDefinitionsLookup())) + 1;
                    SignalExpression minusLines = module.getAdditionalLines(numAdditionalLinesRequiredForSynthesis);
                    usedLines.addAll(minusLines.getLines());
                    SignalExpression twosComplementLines = null;
                    if (!firstRes.isNumber && !secondRes.isNumber) {
                        //only generate a line for the twos complement if both expressions are no number
                        twosComplementLines = module.getAdditionalLines(minusLines.getWidth(module.getLoopVariableRangeDefinitionsLookup()));
                        usedLines.addAll(twosComplementLines.getLines());
                    }
                    ExpressionResult res = new ExpressionResult(minusLines);
                    res.gates.addAll(firstRes.gates);
                    res.gates.addAll(secondRes.gates);
                    res.gates.addAll(Code.minus(firstRes, secondRes, minusLines, twosComplementLines, module.getLoopVariableRangeDefinitionsLookup()));
                    return res;
                }
            case BIT_XOR:
                if (firstRes.isNumber && secondRes.isNumber) {
                    return new ExpressionResult(firstRes.number ^ secondRes.number);
                } else {
                    SignalExpression xorLines = module.getAdditionalLines(Math.max(firstRes.getWidth(module.getLoopVariableRangeDefinitionsLookup()), secondRes.getWidth(module.getLoopVariableRangeDefinitionsLookup())));
                    usedLines.addAll(xorLines.getLines());
                    ExpressionResult res = new ExpressionResult(xorLines);
                    res.gates.addAll(firstRes.gates);
                    res.gates.addAll(secondRes.gates);
                    res.gates.addAll(Code.xor(firstRes, secondRes, xorLines, module.getLoopVariableRangeDefinitionsLookup()));
                    return res;
                }
            case TIMES_UPPER:
                ExpressionResult res1 = new ExpressionResult(0);
                System.out.println("Placeholder Timesupper generated");
                res1.gates.addAll(Code.placeholder());
                break;
            case DIVIDE:
                ExpressionResult res2 = new ExpressionResult(0);
                System.out.println("Placeholder Divide generated");
                res2.gates.addAll(Code.placeholder());
                break;
            case REMAINDER:
                ExpressionResult res3 = new ExpressionResult(0);
                System.out.println("Placeholder Remainder generated");
                res3.gates.addAll(Code.placeholder());
                break;
            case TIMES_LOWER:
                ExpressionResult res4 = new ExpressionResult(0);
                System.out.println("Placeholder Timeslower generated");
                res4.gates.addAll(Code.placeholder());
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
                    res.gates.addAll(Code.logicalAnd(firstRes, secondRes, andLine, module.getLoopVariableRangeDefinitionsLookup()));
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
                    res.gates.addAll(Code.logicalOr(firstRes, secondRes, orLine, module.getLoopVariableRangeDefinitionsLookup()));
                    return res;
                }

            case BIT_AND:
                if (firstRes.isNumber && secondRes.isNumber) {
                    return new ExpressionResult(firstRes.number & secondRes.number);
                } else {
                    SignalExpression bitAndLines = module.getAdditionalLines(Math.max(firstRes.getWidth(module.getLoopVariableRangeDefinitionsLookup()), secondRes.getWidth(module.getLoopVariableRangeDefinitionsLookup())));
                    usedLines.addAll(bitAndLines.getLines());
                    ExpressionResult res = new ExpressionResult(bitAndLines);
                    res.gates.addAll(firstRes.gates);
                    res.gates.addAll(secondRes.gates);
                    res.gates.addAll(Code.bitwiseAnd(firstRes, secondRes, bitAndLines, module.getLoopVariableRangeDefinitionsLookup()));
                    return res;
                }
            case BIT_OR:
                ExpressionResult res5 = new ExpressionResult(0);
                System.out.println("Placeholder BitwiseOr generated");
                res5.gates.addAll(Code.placeholder());
                break;
            case LESSER:
                ExpressionResult res6 = new ExpressionResult(0);
                System.out.println("Placeholder LesserThan generated");
                res6.gates.addAll(Code.placeholder());
                break;
            case GREATER:
                ExpressionResult res7 = new ExpressionResult(0);
                System.out.println("Placeholder GreaterThan generated");
                res7.gates.addAll(Code.placeholder());
                break;
            case EQL:
                ExpressionResult res8 = new ExpressionResult(0);
                System.out.println("Placeholder Equals generated");
                res8.gates.addAll(Code.placeholder());
                break;
            case NEQL:
                ExpressionResult res9 = new ExpressionResult(0);
                System.out.println("Placeholder NotEquals generated");
                res9.gates.addAll(Code.placeholder());
                break;
            case LEQL:
                ExpressionResult res10 = new ExpressionResult(0);
                System.out.println("Placeholder LessorEquals generated");
                res10.gates.addAll(Code.placeholder());
                break;
            case GEQL:
                ExpressionResult res11 = new ExpressionResult(0);
                System.out.println("Placeholder GreaterorEquals generated");
                res11.gates.addAll(Code.placeholder());
                break;
        }
        return new ExpressionResult(0);
    }

}
