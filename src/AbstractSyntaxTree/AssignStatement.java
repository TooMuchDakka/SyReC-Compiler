package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.ExpressionResult;
import CodeGen.Gate;
import SymTable.Mod;

import java.util.*;

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

        /* TODO: Uncomment if the compiler shall also perform some optimizations
        if (res.isNumber && res.number == 0)
            return new ArrayList<>();
        */

        ExpressionResult assignmentRhsOperand = expression.generate(module);
        gates.addAll(assignmentRhsOperand.gates);

        final int bitwidthOfAssignedToSignal = signalExp.getWidth(module.getLoopVariableRangeDefinitionsLookup());
        Optional<List<Gate>> constantValueTransferGatesContainer = Optional.empty();

        if (assignmentRhsOperand.isNumber) {
            int assignmentRhsOperandConstantValue = assignmentRhsOperand.number;
            if (assignmentRhsOperandConstantValue > Math.pow(2, bitwidthOfAssignedToSignal))
                assignmentRhsOperandConstantValue = assignmentRhsOperandConstantValue % (int) Math.pow(2, bitwidthOfAssignedToSignal);

            assignmentRhsOperand = new ExpressionResult(module.getAdditionalLines(bitwidthOfAssignedToSignal));

            if (kind != Kind.XOR) {
                List<Gate> constantValueStorageContainer = new ArrayList<>(bitwidthOfAssignedToSignal);
                final int bitwidthOfConstant = ((int) (Math.log(assignmentRhsOperandConstantValue) / Math.log(2))) + 1;

                for (int i = 0; i < bitwidthOfConstant; ++i) {
                    if (((assignmentRhsOperandConstantValue >> i) & 1) == 1) {
                        Gate constantValueTransferGate = new Gate(Gate.Kind.Toffoli);
                        constantValueTransferGate.addTargetLine(assignmentRhsOperand.getLineName(i));
                        constantValueStorageContainer.add(constantValueTransferGate);
                    }
                }
                constantValueTransferGatesContainer = Optional.of(constantValueStorageContainer);
                gates.addAll(constantValueStorageContainer);
            }
        }

        switch (kind) {
            case XOR:
                gates.addAll(Code.xorAssign(signalExp, assignmentRhsOperand, module.getLoopVariableRangeDefinitionsLookup()));
                break;
            case PLUS: {
                SignalExpression additionalLinesRequiredForSynthesis = module.getAdditionalLines(1);
                gates.addAll(Code.plusAssign(signalExp, assignmentRhsOperand, additionalLinesRequiredForSynthesis, module.getLoopVariableRangeDefinitionsLookup(), true));
                break;
            }
            case MINUS: {
                SignalExpression additionalLinesRequiredForSynthesis = module.getAdditionalLines(1);
                gates.addAll(Code.minusAssign(signalExp, assignmentRhsOperand, additionalLinesRequiredForSynthesis, module.getLoopVariableRangeDefinitionsLookup()));
                break;
            }
        }
        if (lineAware) {
            gates.addAll(Code.reverseGates(assignmentRhsOperand.gates));
            expression.resetLines(module);
        }

        // Revert assignment of compile time constant value to additional lines
        if (constantValueTransferGatesContainer.isPresent()) {
            gates.addAll(constantValueTransferGatesContainer.get().reversed());
        }
        return gates;
    }

    @Override
    public AssignStatement replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        return new AssignStatement(signalExp.replaceSignals(replace, currentModule), expression.replaceSignals(replace, currentModule), kind, lineAware);
    }
}
