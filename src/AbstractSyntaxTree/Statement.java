package AbstractSyntaxTree;


import CodeGen.Gate;
import SymTable.Mod;

import java.util.ArrayList;

public abstract class Statement {
    boolean lineAware;


    public Statement(boolean lineAware) {
        this.lineAware = lineAware;
    }

    public abstract ArrayList<Gate> generate(CodeMod module);

    public abstract Statement replaceSignals(String before, String after, Mod currentModule);
}
