package AbstractSyntaxTree;

import CodeGen.Gate;
import SymTable.Obj;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CodeMod {
    //A Dataobject to hold the Code representation of a module
    private final String name;
    private final Map<String, Obj> variables;   //the variables defined in the source code
    private final Map<String, Obj> additionalLines; //the lines that get created internally by expressions
    private final ArrayList<Obj> zeroLines; //additionalLines that are guaranteed zero
    private int addLineCounter = 0; //used to number the additional lines
    private final ArrayList<Statement> statements;
    private final Map<String, LoopVariableRangeDefinition> loopVars; //keeps track of the loopVars

    public CodeMod(String name, HashMap<String, Obj> variables) {
        this.name = name;
        this.variables = new HashMap<>(variables);
        additionalLines = new HashMap<>();
        zeroLines = new ArrayList<>();
        statements = new ArrayList<>();
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

    public Integer getCurrentLoopVariableValue(String ident) {
        return loopVars.get(ident).currentValue;
    }

    public int getVarWidth(String ident) {
        return variables.get(ident).width;
    }

    public void registerLoopVariable(String ident, LoopVariableRangeDefinition valueRange) {
        if (loopVars.containsKey(ident))
            throw new KeyAlreadyExistsException();

        loopVars.put(ident, valueRange);
    }

    public void advanceLoopVariableValueByOneIterationStep(String ident) {
        if (!loopVars.containsKey(ident))
            return;

        LoopVariableRangeDefinition currentValue = loopVars.get(ident);
        currentValue.currentValue += currentValue.stepSize;
        loopVars.put(ident, currentValue);
    }

    public void releaseLoopVar(String ident) {
        loopVars.remove(ident);
    }

    public Map<String, LoopVariableRangeDefinition> getLoopVariableRangeDefinitionsLookup(){
        return Collections.unmodifiableMap(loopVars);
    }

    public void addVariable(Obj variable) {
        variables.put(variable.name, variable);
    }
}
