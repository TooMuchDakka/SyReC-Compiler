package AbstractSyntaxTree;

import CodeGen.Gate;
import SymTable.Mod;
import SymTable.Obj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class CallStatement extends Statement {

    private final ArrayList<Statement> statements;

    public enum Kind {
        CALL, UNCALL;
    }

    private Kind kind;

    public CallStatement(Mod calledMod, CodeMod calledCode, Mod currentMod, ArrayList<String> idents, ArrayList<Statement> calledStatements, Kind kind, boolean lineAware) {
        super(lineAware);
        this.kind = kind;
        statements = calledStatements;
        LinkedHashMap<String, Obj> oldLines = calledMod.getLocals();
        HashMap<String, String> replace = new HashMap<>();
        for (Obj line : oldLines.values()) {
            if (line.kind == Obj.Kind.Wire || line.kind == Obj.Kind.State) {
                //adds wires and states to the currentModule
                String newVarName = calledMod.name + "_0_" + line.name;
                for (int i = 0; currentMod.getLocal(newVarName) != null; i++) {
                    newVarName = calledMod.name + "_" + i + "_" + line.name;
                }
                Obj newObj = new Obj(line.kind, newVarName, line.width);
                currentMod.addObj(newObj);
                calledCode.addVariable(newObj);
                replace.put(line.name, newVarName);

            }
        }
        ArrayList<Obj> oldSignals = calledMod.getSignals();
        for (int i = 0; i < oldSignals.size(); i++) {
            replace.put(oldSignals.get(i).name, idents.get(i));
        }
        ArrayList<Statement> newStatements = new ArrayList<>();
        for (Statement statement : statements) {
            newStatements.add(statement.replaceSignals(replace, currentMod));
        }
        statements.clear();
        statements.addAll(newStatements);

    }

    private CallStatement(ArrayList<Statement> statements, boolean lineAware) {
        super(lineAware);
        this.statements = statements;
    }


    @Override
    public ArrayList<Gate> generate(CodeMod module) {
        ArrayList<Gate> gates = new ArrayList<>();
        for (Statement statement : statements) {
            gates.addAll(statement.generate(module));
        }
        if (kind == Kind.UNCALL) {
            Collections.reverse(gates);
        }
        return gates;
    }

    @Override
    public CallStatement replaceSignals(HashMap<String, String> replace, Mod currentModule) {
        ArrayList<Statement> newStatements = new ArrayList<>();
        for (Statement statement : statements) {
            newStatements.add(statement.replaceSignals(replace, currentModule));
        }
        return new CallStatement(newStatements, lineAware);
    }

}
