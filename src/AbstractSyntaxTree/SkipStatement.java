package AbstractSyntaxTree;

import CodeGen.Gate;

import java.util.ArrayList;

public class SkipStatement extends Statement {
    public SkipStatement(boolean lineAware) {
        super(lineAware);
    }

    @Override
    public ArrayList<Gate> generate(CodeMod module) {
        return new ArrayList<Gate>(); //Skip statement returns empty gate list
    }
}
