package AbstractSyntaxTree;


import CodeGen.Gate;
import SymTable.Mod;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Statement {
    boolean lineAware;


    public Statement(boolean lineAware) {
        this.lineAware = lineAware;
    }

    public abstract ArrayList<Gate> generate(CodeMod module);

    public abstract Statement replaceSignals(HashMap<String, String> replace, Mod currentModule);
}
