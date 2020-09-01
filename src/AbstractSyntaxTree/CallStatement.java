package AbstractSyntaxTree;

import CodeGen.Gate;
import SymTable.Mod;
import SymTable.Obj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

public class CallStatement extends Statement {

    private final ArrayList<Statement> statements;

    public enum Kind {
        CALL, UNCALL;
    }

    private Kind kind;

    public CallStatement(Mod calledMod, Mod currentMod, ArrayList<String> idents, ArrayList<Statement> calledStatements, Kind kind, boolean lineAware) {
        super(lineAware);
        this.kind = kind;
        statements = calledStatements;
        LinkedHashMap<String, Obj> oldLines = calledMod.getLocals();
        for (Obj line : oldLines.values()) {
            if (line.kind == Obj.Kind.Wire || line.kind == Obj.Kind.State) {
                //adds wires and states to the currentModule
                currentMod.addObj(new Obj(line.kind, calledMod.name + "_" + line.name, line.width));
                ArrayList<Statement> newStatements = new ArrayList<>();
                for (Statement statement : statements) {
                    newStatements.add(statement.replaceSignals(line.name, calledMod.name + "_" + line.name, currentMod));
                }
                statements.clear();
                statements.addAll(newStatements);
            }
        }
        ArrayList<Obj> oldSignals = calledMod.getSignals();
        for (int i = 0; i < oldSignals.size(); i++) {
            ArrayList<Statement> newStatements = new ArrayList<>();
            for (Statement statement : statements) {
                newStatements.add(statement.replaceSignals(oldSignals.get(i).name, idents.get(i), currentMod));
            }
            statements.clear();
            statements.addAll(newStatements);
        }

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
    public CallStatement replaceSignals(String before, String after, Mod currentModule) {
        ArrayList<Statement> newStatements = new ArrayList<>();
        for (Statement statement : statements) {
            newStatements.add(replaceSignals(before, after, currentModule));
        }
        return new CallStatement(newStatements, lineAware);
    }

}
