package AbstractSyntaxTree;

import CodeGen.Gate;
import SymTable.Mod;

import java.util.ArrayList;
import java.util.HashMap;

public class ForStatement extends Statement {

    private final String ident;
    private final NumberExpression from;
    private final NumberExpression to;
    private final NumberExpression step;
    private final ArrayList<Statement> statements;

    public ForStatement(String ident, NumberExpression from, NumberExpression to, NumberExpression step, ArrayList<Statement> statements, boolean lineAware) {
        super(lineAware);
        this.ident = ident;
        this.from = from;
        this.to = to;
        this.step = step;
        this.statements = new ArrayList<>(statements);
    }

    @Override
    public ArrayList<Gate> generate(CodeMod module) {
        ArrayList<Gate> gates = new ArrayList<>();
        int fromInt = from.generate(module).number;
        int toInt = to.generate(module).number;
        int stepInt = step.generate(module).number;
        java.io.PrintStream errorStream = System.out;
        if (fromInt < toInt && stepInt <= 0) {
            errorStream.println("LoopVar " + ident + " cant reach toValue. From " + fromInt + " to " + toInt + " Stepsize " + stepInt);
        } else if (fromInt > toInt && stepInt >= 0) {
            errorStream.println("LoopVar " + ident + " cant reach toValue. From " + fromInt + " to " + toInt + " Stepsize " + stepInt);
        }

        if (stepInt == 0){
            System.out.println("Loop does not perform any iterations and thus will be not be synthesized");
            return gates;
        }

        final int numIterations = (Math.abs((Math.abs(fromInt) - Math.abs(toInt))) / Math.abs(stepInt)) + 1;
        if (ident != null) {
            module.registerLoopVariable(ident, new LoopVariableRangeDefinition(fromInt, toInt, stepInt));

            for (int i = 0; i < numIterations; ++i) {
                for (Statement statement : statements)
                    gates.addAll(statement.generate(module));

                module.advanceLoopVariableValueByOneIterationStep(ident);
            }
            module.releaseLoopVar(ident);
        }
        else {
            for (int i = 0; i < numIterations; ++i) {
                for (Statement statement : statements)
                    gates.addAll(statement.generate(module));
            }
        }
        return gates;
    }

    @Override
    public ForStatement replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        NumberExpression newFrom = from.replaceSignals(replace, currentModule);
        NumberExpression newTo = to.replaceSignals(replace, currentModule);
        NumberExpression newStep = step.replaceSignals(replace, currentModule);
        ArrayList<Statement> newStatements = new ArrayList<>();
        for (Statement statement : statements) {
            newStatements.add(statement.replaceSignals(replace, currentModule));
        }
        return new ForStatement(ident, newFrom, newTo, newStep, newStatements, lineAware);
    }
}
