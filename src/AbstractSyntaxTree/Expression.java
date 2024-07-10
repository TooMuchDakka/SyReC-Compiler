package AbstractSyntaxTree;

import CodeGen.ExpressionResult;
import SymTable.Mod;

import java.util.*;

public abstract class Expression {

    final ArrayList<String> usedLines;
    final HashSet<String> containedSignals;

    public Expression() {
        usedLines = new ArrayList<>();
        containedSignals = new HashSet<>();
    }

    public abstract ExpressionResult generate(CodeMod module);

    public abstract int getWidth(Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup);

    public abstract Optional<Integer> tryGetWidth(Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup);

    public void resetLines(CodeMod module) {
        for (String lineName : usedLines) {
            module.resetLine(lineName);
        }
    }

    public boolean containsSignal(String signalName) {
        return containedSignals.contains(signalName);
    }

    public abstract Expression replaceSignals(HashMap<String, String> replace, Mod currentModule);
}
