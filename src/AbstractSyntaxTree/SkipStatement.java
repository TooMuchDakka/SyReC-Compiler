package AbstractSyntaxTree;

import CodeGen.Gate;
import SymTable.Mod;

import java.util.ArrayList;
import java.util.HashMap;

public class SkipStatement extends Statement {
    public SkipStatement(boolean lineAware) {
        super(lineAware);
    }

    @Override
    public ArrayList<Gate> generate(CodeMod module) {
        //return new ArrayList<Gate>(); //Skip statement returns empty gate list
        //TODO remvoe DEBUG
        ArrayList<Gate> gates = new ArrayList<>();
        return gates;
    }

    @Override
    public SkipStatement replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        return new SkipStatement(lineAware);
    }
}
