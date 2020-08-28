package AbstractSyntaxTree;

import CodeGen.ExpressionResult;
import SymTable.Obj;

import java.util.ArrayList;

public class SignalExpression extends Expression{

    private final ArrayList<String> lines;
    public final String name;
    private final NumberExpression startWidth;
    private final NumberExpression endWidth;

    public SignalExpression(Obj obj, NumberExpression startWidth, NumberExpression endWidth) {
        //Constructor used when creating SignalExpression from InputCode
        this.name = obj.name;
        lines = new ArrayList<>();
        if(obj.width == 1) {
            lines.add(name);
        }
        this.startWidth = startWidth;
        this.endWidth = endWidth;
    }

    public SignalExpression(String name, ArrayList<String> lines) {
        //SignalExpression used for additionalLines
        this.name = name;
        this.lines = new ArrayList<>(lines);
        containedSignals.addAll(this.getLines());
        this.startWidth = new NumberExpression(0);
        this.endWidth = new NumberExpression(lines.size()-1);
    }


    @Override
    public ExpressionResult generate(CodeMod module) {
        int start = startWidth.generate(module).number;
        int end = endWidth.generate(module).number;
        if(lines.size() == 1) {
            //dummy: we already have the line
        }
        else if(start < end) {
            for(int i = start; i <= end; i++) {
                lines.add(name+"_"+i);
            }
        }
        else{
            for(int i = start; i >= end; i--) {
                lines.add((name+"_"+i));
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
