package AbstractSyntaxTree;

import CodeGen.ExpressionResult;
import SymTable.Mod;
import SymTable.Obj;
import sun.misc.Signal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SignalExpression extends Expression {

    private final ArrayList<String> lines;
    public final String name;
    private final NumberExpression startWidth;
    private final NumberExpression endWidth;
    private final boolean noBus;
    private final int originalWidth;

    private static Map<String, LoopVariableRangeDefinition> emptyLoopVariableLookup = new HashMap<>();

    public static SignalExpression createInstanceForTest(String name, NumberExpression newStartWidth, NumberExpression newEndWidth){
        return new SignalExpression(name, newStartWidth, newEndWidth);
    }

    public SignalExpression(Obj obj, NumberExpression startWidth, NumberExpression endWidth) {
        //Constructor used when creating SignalExpression from InputCode
        this.name = obj.name;
        lines = new ArrayList<>();
        if (obj.width == 1) {
            noBus = true;
            lines.add(name);
        } else {
            noBus = false;
        }
        originalWidth = obj.width;
        this.startWidth = startWidth;
        this.endWidth = endWidth;
    }

    private SignalExpression(SignalExpression copyFrom, NumberExpression newStartWidth, NumberExpression newEndWidth) {
        //constructor to copy same SignalExpression
        name = copyFrom.name;
        lines = copyFrom.lines;
        startWidth = newStartWidth;
        endWidth = newEndWidth;
        noBus = copyFrom.noBus;
        originalWidth = copyFrom.originalWidth;
    }

    private SignalExpression(String name, NumberExpression newStartWidth, NumberExpression newEndWidth){
        final int bitwidth = (newEndWidth.evaluate(emptyLoopVariableLookup) - newStartWidth.evaluate(emptyLoopVariableLookup) + 1);
        ArrayList<String> lines = new ArrayList<>(bitwidth);
        noBus = bitwidth == 1;
        this.name = name;

        for (int i = 0; i < bitwidth; ++i)
            lines.add(name + "_" + i);

        originalWidth = bitwidth;
        startWidth = newStartWidth;
        endWidth = newEndWidth;
        this.lines = lines;
    }

    public SignalExpression(String name, ArrayList<String> lines) {
        //SignalExpression used for additionalLines
        this.name = name;
        this.lines = new ArrayList<>(lines);
        containedSignals.addAll(this.getLines());
        this.startWidth = null;
        this.endWidth = null;
        noBus = false;
        originalWidth = -1;
    }


    @Override
    public ExpressionResult generate(CodeMod module) {
        int start = startWidth.generate(module).number;
        int end = endWidth.generate(module).number;
        java.io.PrintStream errorStream = System.out;
        if (noBus) {
            if (start != end) {
                errorStream.println("Signal " + name + " is not a Bus but is indexed from " + start + " to " + end);
            }
        } else if (originalWidth != -1 && (start >= originalWidth || end >= originalWidth)) {
            errorStream.println("Signal " + name + " index out of bounds.\nWidth is " + originalWidth + " and used index is from " + start + " to " + end);
        } else {
            lines.clear();
            if (start < end) {
                for (int i = start; i <= end; i++) {
                    lines.add(name + "_" + i);
                }
            } else {
                for (int i = start; i >= end; i--) {
                    lines.add((name + "_" + i));
                }
            }
        }
        containedSignals.addAll(this.getLines());
        return new ExpressionResult(this);
    }

    @Override
    public int getWidth(Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        final int accessedBitRangeStart = this.startWidth != null ? this.startWidth.evaluate(loopVariableRangeDefinitionLookup) : 0;
        final int accessedBitRangEnd = this.endWidth != null ? this.endWidth.evaluate(loopVariableRangeDefinitionLookup) : (this.lines.size() -1);
        return (accessedBitRangEnd - accessedBitRangeStart) + 1;
    }

    @Override
    public Optional<Integer> tryGetWidth(Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        Optional<Integer> optionalAccessedBitRangeStart = this.startWidth != null ? this.startWidth.tryGetWidth(loopVariableRangeDefinitionLookup) : Optional.of(0);
        Optional<Integer> optionalAccessedBitRangeEnd = this.endWidth != null ? this.endWidth.tryGetWidth(loopVariableRangeDefinitionLookup) : Optional.of(0);

        if (optionalAccessedBitRangeStart.isPresent() && optionalAccessedBitRangeEnd.isPresent())
            return Optional.of((optionalAccessedBitRangeEnd.get() - optionalAccessedBitRangeStart.get()) + 1);
        return Optional.empty();
    }

    @Override
    public SignalExpression replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        String after = replace.get(name);
        if (originalWidth != -1 && after != null) {
            //additionalLines only exist in the generate Step so cant be replaced
            Obj newObj = currentModule.getLocal(after);
            return new SignalExpression(newObj, startWidth.replaceSignals(replace, currentModule), endWidth.replaceSignals(replace, currentModule));
        } else {
            return new SignalExpression(this, startWidth.replaceSignals(replace, currentModule), endWidth.replaceSignals(replace, currentModule));
        }
    }

    public String getLineName(int index) {
        return lines.get(index);
    }

    public ArrayList<String> getLines() {
        return new ArrayList<>(lines);
    }
}
