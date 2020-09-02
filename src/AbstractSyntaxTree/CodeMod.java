package AbstractSyntaxTree;

import CodeGen.Gate;
import SymTable.Obj;

import java.util.ArrayList;
import java.util.HashMap;

public class CodeMod {
    //A Dataobject to hold the Code representation of a module
    private final String name;
    private final HashMap<String, Obj> variables;   //the variables defined in the source code
    private final HashMap<String, Obj> additionalLines; //the lines that get created internally by expressions
    private final ArrayList<Obj> zeroLines; //additionalLines that are guaranteed zero
    private int addLineCounter = 0; //used to number the additional lines
    private final ArrayList<Statement> statements;
    private final HashMap<String, Integer> loopVars; //keeps track of the loopVars

    public CodeMod(String name, HashMap<String, Obj> variables) {
        this.name = name;
        this.variables = new HashMap<>(variables);
        additionalLines = new HashMap<String, Obj>();
        zeroLines = new ArrayList<Obj>();
        statements = new ArrayList<Statement>();
        loopVars = new HashMap<>();
    }

    public SignalExpression getAdditionalLines(int width) {
        //returns a SignalObject consisting of additionalLines
        //tries to use as many already created lines as possible
        ArrayList<String> lines = new ArrayList<>();
        Obj temp;
        for (int i = 0; i < width; i++) {
            if (zeroLines.size() > 0) {
                temp = zeroLines.get(zeroLines.size() - 1);
                temp.setGarbage(true);
                additionalLines.put(temp.name, temp);
                lines.add(temp.name);
                zeroLines.remove(zeroLines.size() - 1);
            } else {
                temp = new Obj(Obj.Kind.Wire, "addLine" + addLineCounter, 1);
                additionalLines.put(temp.name, temp);
                lines.add(temp.name);
                addLineCounter++;
            }
        }
        return new SignalExpression("addLine", lines);
    }

    public void resetLine(String lineName) {
        if (additionalLines.containsKey(lineName)) {
            Obj line = additionalLines.remove(lineName);
            //when we reset a line we know its no longer garbage
            line.setGarbage(false);
            zeroLines.add(line);
        }
    }


    public ArrayList<Obj> getVariables() {    //returns variables and lines
        ArrayList<Obj> list = new ArrayList<>(variables.values());
        list.addAll(additionalLines.values());
        list.addAll(zeroLines);
        return list;
    }


    public ArrayList<Gate> generate() {
        ArrayList<Gate> gates = new ArrayList<>();
        for (Statement statement : statements) {
            gates.addAll(statement.generate(this));
        }
        return gates;
    }

    public ArrayList<Statement> getStatements() {
        return new ArrayList<>(statements);
    }

    public void addStatements(ArrayList<Statement> statements) {
        this.statements.addAll(statements);
    }


    public int getVarCount() { //return parameters+lines needed for wires (width is used in this calculation
        int count = 0;
        for (Obj signal : getVariables()) {
            count += signal.width;
        }
        return count;
    }

    public Integer getLoopVar(String ident) {
        return loopVars.get(ident);
    }

    public int getVarWidth(String ident) {
        return variables.get(ident).width;
    }

    public void setLoopVar(String ident, int value) {
        loopVars.put(ident, value);
    }

    public void releaseLoopVar(String ident) {
        loopVars.remove(ident);
    }

    public void addVariable(Obj variable) {
        variables.put(variable.name, variable);
    }


}
