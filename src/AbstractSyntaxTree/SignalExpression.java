package AbstractSyntaxTree;

import CodeGen.ExpressionResult;
import SymTable.Obj;

import java.util.ArrayList;

public class SignalExpression extends Expression {

    private final ArrayList<String> lines;
    public final String name;
    private final NumberExpression startWidth;
    private final NumberExpression endWidth;
    private final boolean noBus;
    private final int originalWidth;

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
    public int getWidth() {
        //TODO change this so it works with loopVars as index
        return lines.size();
    }

    public String getLineName(int index) {
        return lines.get(index);
    }

    public ArrayList<String> getLines() {
        return new ArrayList<>(lines);
    }
}
