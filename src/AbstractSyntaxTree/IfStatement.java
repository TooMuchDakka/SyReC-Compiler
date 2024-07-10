package AbstractSyntaxTree;

import CodeGen.Code;
import CodeGen.ExpressionResult;
import CodeGen.Gate;
import SymTable.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class IfStatement extends Statement {

    //The parser has to check if if/fi results into a number or a single line result

    private final Expression ifExpression;
    private final ArrayList<Statement> thenStatements;
    private final ArrayList<Statement> elseStatements;
    private final Expression fiExpression;

    public IfStatement(Expression ifExpression, ArrayList<Statement> thenStatements, ArrayList<Statement> elseStatements, Expression fiExpression, boolean lineAware) {
        super(lineAware);
        this.ifExpression = ifExpression;
        this.thenStatements = new ArrayList<>(thenStatements);
        this.elseStatements = new ArrayList<>(elseStatements);
        this.fiExpression = fiExpression;
    }

    @Override
    public ArrayList<Gate> generate(CodeMod module) {
        ArrayList<Gate> gates = new ArrayList<>();
        ExpressionResult ifExp = ifExpression.generate(module);
        gates.addAll(ifExp.gates);
        ExpressionResult ifRes; //this only contains the line or the number of the if Expression
        if (ifExp.isNumber) {
            ifRes = new ExpressionResult(ifExp.number); //for a number nothing is needed
        } else {
            //saves the ifExpression to a new line and reverses the calculation to reset all possible additional Lines
            ifRes = new ExpressionResult(module.getAdditionalLines(1));
            gates.addAll(Code.xorAssign(ifRes.signal, ifExp, module.getLoopVariableRangeDefinitionsLookup()));
            if (lineAware) {
                gates.addAll(Code.reverseGates(ifExp.gates));
                ifExpression.resetLines(module);
            }
        }
        if (!ifRes.isNumber || ifRes.number != 0) {
            ArrayList<Gate> thenGates = new ArrayList<>();
            for (Statement statement : thenStatements) {
                thenGates.addAll(statement.generate(module));
            }
            gates.addAll(thenGates);
            if (!ifRes.isNumber) {
                for (Gate gate : thenGates) {
                    gate.addControlLine(ifRes.signal.getLineName(0));   //add the ifExpression as controlLine to all gates
                }
                Gate negate = new Gate(Gate.Kind.Toffoli);
                negate.addTargetLine(ifRes.signal.getLineName(0));     //negate the if
                gates.add(negate);
            }
        }
        if (!ifRes.isNumber || ifRes.number == 0) {
            ArrayList<Gate> elseGates = new ArrayList<>();
            for (Statement statement : elseStatements) {
                elseGates.addAll(statement.generate(module));
            }
            gates.addAll(elseGates);
            if (!ifRes.isNumber) {
                for (Gate gate : elseGates) {
                    gate.addControlLine(ifRes.signal.getLineName(0));   //add the ifExpression as controlLine to all gates
                }
                Gate negate = new Gate(Gate.Kind.Toffoli);
                negate.addTargetLine(ifRes.signal.getLineName(0));     //negate the if
                gates.add(negate);
            }
        }
        if (!ifRes.isNumber) {
            ExpressionResult fiRes = fiExpression.generate(module);
            gates.addAll(new ArrayList<Gate>(fiRes.gates));
            gates.addAll(Code.xorAssign(ifRes.signal, fiRes, module.getLoopVariableRangeDefinitionsLookup()));
            if (lineAware) {
                gates.addAll(Code.reverseGates(fiRes.gates));
                fiExpression.resetLines(module);
            }
            ArrayList<Gate> reverseFi = new ArrayList<>(fiRes.gates);
            Collections.reverse(reverseFi);
            gates.addAll(reverseFi);
            module.resetLine(ifRes.getLineName(0));
        }

        return gates;
    }

    @Override
    public IfStatement replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        Expression newIf = ifExpression.replaceSignals(replace, currentModule);
        ArrayList<Statement> newThen = new ArrayList<>();
        for (Statement statement : thenStatements) {
            newThen.add(statement.replaceSignals(replace, currentModule));
        }
        ArrayList<Statement> newElse = new ArrayList<>();
        for (Statement statement : elseStatements) {
            newElse.add(statement.replaceSignals(replace, currentModule));
        }
        Expression newFi = fiExpression.replaceSignals(replace, currentModule);
        return new IfStatement(newIf, newThen, newElse, newFi, lineAware);
    }
}
