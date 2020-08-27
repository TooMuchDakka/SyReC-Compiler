package CodeGen;


import SymTable.Mod;
import SymTable.Obj;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static CodeGen.Gate.Kind.*;

public class Code {

    Path curPath;
    Map<String, CodeMod> modules; //can be used to unravel calls
    CodeMod curMod;
    public boolean lineAware = true; //to deactivate line Aware synthesis to save lines
    public boolean costAware = true; //to deactivate cost Aware synthesis to save gates

    public Code(String folderName) {
        curPath = Path.of(folderName);
        try {
            Files.createDirectory(curPath);
        } catch (FileAlreadyExistsException ee) {
            //do nothing as the directory already exists
        } catch (IOException e) {
            System.err.println("Failed to create directory!" + e.getMessage());
        }
        modules = new HashMap<>();
    }

    public void createModule(Mod module) {
        curMod = new CodeMod(module.name, module.getLocals());
        modules.put(module.name, curMod);
    }

    public void endModule(Mod module)  {
        //ends the module and writes it to file
        try { //delete File if it already exists
            Files.deleteIfExists(Path.of(curPath.toString(), module.name+".real"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //create Writer
            BufferedWriter curWriter = Files.newBufferedWriter(Path.of(curPath.toString(), module.name+".real"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            curWriter.append("# ").append(module.name);
            curWriter.newLine();
            curWriter.append(".version 2.0");
            curWriter.newLine();
            curWriter.append(".numvars ").append(String.valueOf(curMod.getVarCount()));
            curWriter.newLine();
            ArrayList<Obj> lines = curMod.getVariables();
            curWriter.append(".variables ");
            for (Obj line : lines) {
                if(line.width == 1) {
                    curWriter.append(line.name).append(" "); //write each variable
                }
                else for(int i = 0; i < line.width; i++) {
                    curWriter.append(line.name).append("_").append(String.valueOf(i)).append(" "); //write all subvariables of the width
                }
            }
            //TODO inputs and outputs (optional and not specified in SyReC)
            curWriter.newLine();
            curWriter.append(".constants ");
            for (Obj line : lines) {
                if(line.getConstant()) {
                    curWriter.append(String.join("", Collections.nCopies(line.width, "0")));
                }
                else {
                    curWriter.append(String.join("", Collections.nCopies(line.width, "-")));
                }
            }
            curWriter.newLine();
            curWriter.append(".garbage ");
            for (Obj line : lines) {
                if(line.getGarbage()) {
                    curWriter.append(String.join("", Collections.nCopies(line.width, "1")));
                }
                else {
                    curWriter.append(String.join("", Collections.nCopies(line.width, "-")));
                }
            }
            curWriter.newLine();
            curWriter.append(".begin");
            curWriter.newLine();

            //here the gates start
            for(Gate gate : curMod.getGates()) {
                ArrayList<String> controlLines = gate.getControlLines();
                ArrayList<String> targetLines = gate.getTargetLines();
                switch (gate.kind) {

                    case Toffoli:
                        curWriter.append("t");
                        break;
                    case Fredkin:
                        curWriter.append("f");
                        break;
                    case Peres:
                        curWriter.append("p");
                        break;
                    case V:
                        curWriter.append("v");
                        break;
                    case Vplus:
                        curWriter.append("v+");
                        break;
                }
                curWriter.append(String.valueOf(controlLines.size()+targetLines.size()));
                for(String line : controlLines) {
                    curWriter.append(" ").append(line);
                }
                for(String line : targetLines) {
                    curWriter.append(" ").append(line);
                }
                curWriter.newLine();
            }
            curWriter.append(".end");
            curWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //swap of two signals
    //function is only called if the width is equal
    public static ArrayList<Gate> swap(SignalObject firstSig, SignalObject secondSig) {
        ArrayList<Gate> gates = new ArrayList<>();
        for(int i = 0; i < firstSig.getWidth(); i++) {
            Gate tempGate = new Gate(Fredkin);
            tempGate.addTargetLine(firstSig.getLineName(i));
            tempGate.addTargetLine(secondSig.getLineName(i));
            gates.add(tempGate);
        }
        return gates;
    }

    //negate given Signal
    public static ArrayList<Gate> not(SignalObject sig) {
        ArrayList<Gate> gates = new ArrayList<>();
        for(int i = 0; i < sig.getWidth(); i++) {
            Gate tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(sig.getLineName(i));
            gates.add(tempGate);
        }
        return gates;
    }


    //++= Statement
    public static ArrayList<Gate> increment(SignalObject sig) {
        ArrayList<Gate> gates = new ArrayList<>();
        for(int i = sig.getWidth()-1; i >= 0; i--) {
            ArrayList<String> controlLines = new ArrayList<>();
            for(int j = 0; j < i; j++) {
                controlLines.add(sig.getLineName(j));
            }
            Gate tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(sig.getLineName(i));
            tempGate.addControlLines(controlLines);
        }
        return gates;
    }

    //--= Statement, plusplus but in reverse
    public static ArrayList<Gate> decrement(SignalObject sig) {
        ArrayList<Gate> gates = increment(sig);
        Collections.reverse(gates);
        return gates;
    }

    public static ExpressionResult leftShift(ExpressionResult exp, int number, CodeMod module) {
        if(exp.isNumber) {
            return new ExpressionResult(exp.number << number);
        }
        else {
            ArrayList<Gate> gates = new ArrayList<>();
            SignalObject additionalLines = module.getAdditionalLines(exp.getWidth());
            for(int i = 0; i < exp.getWidth()-number; i++) {
                Gate tempGate = new Gate(Toffoli);
                tempGate.addTargetLine(additionalLines.getLineName(i+number));
                tempGate.addControlLine(exp.getLineName(i));
            }
            ExpressionResult newExp = new ExpressionResult(additionalLines, gates);
            newExp.addContainedSignals(exp.getContainedSignals());
            return newExp;
        }
    }

    public static ExpressionResult rightShift(ExpressionResult exp, int number, CodeMod module) {
        if(exp.isNumber) {
            return new ExpressionResult(exp.number >> number);
        }
        else {
            ArrayList<Gate> gates = new ArrayList<>();
            SignalObject additionalLines = module.getAdditionalLines(exp.getWidth());
            for(int i = 0; i < exp.getWidth()-number; i++) {
                Gate tempGate = new Gate(Toffoli);
                tempGate.addTargetLine(additionalLines.getLineName(i));
                tempGate.addControlLine(exp.getLineName(i+number));
            }
            ExpressionResult newExp = new ExpressionResult(additionalLines, gates);
            newExp.addContainedSignals(exp.getContainedSignals());
            return newExp;
        }
    }

    public static ExpressionResult notExp(ExpressionResult exp, CodeMod module) {
        //bitwise not on a number
        if(exp.isNumber) {
            return new ExpressionResult(~exp.number);
        }
        else {
            ArrayList<Gate> gates = new ArrayList<>();
            SignalObject additionalLines = module.getAdditionalLines(exp.getWidth());
            for(int i = 0; i < exp.getWidth(); i++) {
                Gate tempGate = new Gate(Toffoli);
                tempGate.addTargetLine(additionalLines.getLineName(i));
                tempGate.addControlLine(exp.getLineName(i));
                gates.add(tempGate); // line.i = !oldLine.i

                tempGate = new Gate(Toffoli);
                tempGate.addTargetLine(additionalLines.getLineName(i));
                gates.add(tempGate); // line.i = !line.i
            }
            ExpressionResult newExp = new ExpressionResult(additionalLines, gates);
            newExp.addContainedSignals(exp.getContainedSignals());
            return newExp;
        }
    }

    public static ArrayList<Gate> xorAssign(SignalObject firstSignal, ExpressionResult exp) {
        ArrayList<Gate> gates = new ArrayList<>();
        if(exp.isNumber) {
            int number = exp.number;
            for(int i = 0; i < Math.ceil(Math.log(exp.number)/Math.log(2)); i++) {
                if(number%2 == 1) {
                    Gate newGate = new Gate(Toffoli);
                    newGate.addTargetLine(firstSignal.getLineName(i));
                    gates.add(newGate);
                }
            number /= 2;
            }
        }
        else {
            for(int i = 0; i < firstSignal.getWidth(); i++) {
                Gate newGate = new Gate(Toffoli);
                newGate.addTargetLine(firstSignal.getLineName(i));
                newGate.addControlLine(exp.getLineName(i));
                gates.add(newGate);
            }
        }
        return gates;
    }

    public static ExpressionResult logicalAnd(ExpressionResult firstExp, ExpressionResult secondExp, CodeMod module) {
        if(firstExp.isNumber && secondExp.isNumber) {
            int newValue = firstExp.number == 1 && secondExp.number == 1?1:0;
            return new ExpressionResult(newValue);
        }
        else if(firstExp.isNumber) {
            if(firstExp.number == 0) {
                return new ExpressionResult(0);
            }
            else {
                return secondExp;
            }
        }
        else if(secondExp.isNumber) {
            if(secondExp.number == 0) {
                return new ExpressionResult(0);
            }
            else {
                return firstExp;
            }
        }
        else {
            SignalObject additionalLine = module.getAdditionalLines(1);
            ArrayList<String> controlLines = new ArrayList<>();
            controlLines.addAll(firstExp.getLines());
            controlLines.addAll(secondExp.getLines());
            ArrayList<Gate> gates = new ArrayList<>();
            Gate tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(additionalLine.getLineName(0));
            tempGate.addControlLines(controlLines);
            ExpressionResult newExp = new ExpressionResult(additionalLine, gates);
            newExp.addContainedSignals(firstExp.getContainedSignals());
            newExp.addContainedSignals(secondExp.getContainedSignals());
            return newExp;
        }
    }

    public static ExpressionResult logicalOr(ExpressionResult firstExp, ExpressionResult secondExp, CodeMod module) {
        if(firstExp.isNumber && secondExp.isNumber) {
            int newValue = firstExp.number == 1 || secondExp.number == 1?1:0;
            return new ExpressionResult(newValue);
        }
        else if(firstExp.isNumber) {
            if(firstExp.number == 1) {
                return new ExpressionResult(1);
            }
            else {
                return secondExp;
            }
        }
        else if(secondExp.isNumber) {
            if(secondExp.number == 1) {
                return new ExpressionResult(1);
            }
            else {
                return firstExp;
            }
        }
        else {
            SignalObject additionalLine = module.getAdditionalLines(1);
            ArrayList<String> controlLines = new ArrayList<>();
            controlLines.addAll(firstExp.getLines());
            controlLines.addAll(secondExp.getLines());
            ArrayList<Gate> gates = new ArrayList<>();

            Gate tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(firstExp.getLineName(0));
            gates.add(tempGate); // !a

            tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(secondExp.getLineName(0));
            gates.add(tempGate); // !b

            tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(secondExp.getLineName(0));
            tempGate.addControlLines(controlLines);
            gates.add(tempGate); // !a and !b

            tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(additionalLine.getLineName(0));
            gates.add(tempGate); // nand

            tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(firstExp.getLineName(0));
            gates.add(tempGate); // !a to make the line usable again

            tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(secondExp.getLineName(0));
            gates.add(tempGate); // !b to make the line usable again

            ExpressionResult newExp = new ExpressionResult(additionalLine, gates);
            newExp.addContainedSignals(firstExp.getContainedSignals());
            newExp.addContainedSignals(secondExp.getContainedSignals());
            return newExp;
        }
    }
}
