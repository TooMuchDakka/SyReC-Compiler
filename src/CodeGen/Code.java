package CodeGen;


import SymTable.Mod;
import SymTable.Obj;
import sun.misc.Signal;

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
    public void swap(SignalObject firstSig, SignalObject secondSig, ExpressionObject ifExp) {
        if (!ifExp.isNumber || ifExp.number == 1) {
            for(int i = 0; i < firstSig.getWidth(); i++) {
                ArrayList<String> targetLines = new ArrayList<String>(Arrays.asList(firstSig.getLineName(i), secondSig.getLineName(i)));
                if (ifExp.isNumber) {
                    curMod.addGate(Fredkin, targetLines);
                } else {
                    curMod.addGate(Fredkin, targetLines, ifExp.getLines());
                }
            }
        }
    }

    //negate given Signal
    public void not(SignalObject sig, ExpressionObject ifExp) {
        if (!ifExp.isNumber || ifExp.number == 1) {
            for(int i = 0; i < sig.getWidth(); i++) {
                if (ifExp.isNumber) {
                    curMod.addGate(Toffoli, sig.getLineName(i));
                } else {
                    curMod.addGate(Toffoli, sig.getLineName(i), ifExp.getLines());
                }
            }
        }
    }


    //++= Statement
    public void plusplus(SignalObject sig, ExpressionObject ifExp) {
        if (!ifExp.isNumber || ifExp.number == 1) {
            for(int i = sig.getWidth()-1; i >= 0; i--) {
                ArrayList<String> controlLines = new ArrayList<>();
                for(int j = 0; j < i; j++) {
                    controlLines.add(sig.getLineName(j));
                }
                if(ifExp.isNumber) {
                    curMod.addGate(Toffoli, sig.getLineName(i),controlLines);
                }
                else {
                    controlLines.addAll(ifExp.getLines());
                    curMod.addGate(Toffoli, sig.getLineName(i),controlLines);
                }
            }
        }
    }

    //--= Statement, plusplus but in reverse
    public void minusminus(SignalObject sig, ExpressionObject ifExp) {
        if (!ifExp.isNumber || ifExp.number == 1) {
            int lastGate = curMod.getLastGateNumber();
            plusplus(sig, ifExp);
            if(lastGate <= curMod.getLastGateNumber()) { //if lastGate stayed the same we didnt generate Gates (for example if the ifExp evaluated to 0
                curMod.reverseGates(lastGate+1, curMod.getLastGateNumber());
            }
        }
    }

    public ExpressionObject leftShift(ExpressionObject exp, int number) {
        if(exp.isNumber) {
            return new ExpressionObject(exp.number << number);
        }
        else {
            SignalObject additionalLines = curMod.getAdditionalLines(exp.getWidth());
            int resetStart = getResetStart(exp);
            for(int i = 0; i < exp.getWidth()-number; i++) {
                curMod.addGate(Toffoli, additionalLines.getLineName(i+number), exp.getLineName(i));
            }
            int resetEnd = curMod.getLastGateNumber();
            ExpressionObject newExp = new ExpressionObject(additionalLines, resetStart, resetEnd);
            newExp.addContainedSignals(exp.getContainedSignals());
            return newExp;
        }
    }

    public ExpressionObject rightShift(ExpressionObject exp, int number) {
        if(exp.isNumber) {
            return new ExpressionObject(exp.number >> number);
        }
        else {
            SignalObject additionalLines = curMod.getAdditionalLines(exp.getWidth());
            int resetStart = getResetStart(exp);
            for(int i = 0; i < exp.getWidth()-number; i++) {
                curMod.addGate(Toffoli, additionalLines.getLineName(i), exp.getLineName(i+number));
            }
            int resetEnd = curMod.getLastGateNumber();
            ExpressionObject newExp = new ExpressionObject(additionalLines, resetStart, resetEnd);
            newExp.addContainedSignals(exp.getContainedSignals());
            return newExp;
        }
    }

    public ExpressionObject notExp(ExpressionObject exp) {
        //bitwise not on a number
        if(exp.isNumber) {
            return new ExpressionObject(~exp.number);
        }
        else {
            SignalObject additionalLines = curMod.getAdditionalLines(exp.getWidth());
            int resetStart = getResetStart(exp);
            for(int i = 0; i < exp.getWidth(); i++) {
                ArrayList<String> controlLines = new ArrayList<>();
                controlLines.add(exp.getLineName(i));
                curMod.addGate(Toffoli, additionalLines.getLineName(i), controlLines);
                curMod.addGate(Toffoli, additionalLines.getLineName(i));
            }
            int resetEnd = curMod.getLastGateNumber();
            ExpressionObject newExp = new ExpressionObject(additionalLines, resetStart, resetEnd);
            newExp.addContainedSignals(exp.getContainedSignals());
            return newExp;
        }
    }

    public void xorAssign(SignalObject firstSignal, ExpressionObject exp, ExpressionObject ifExp) {

        if (!ifExp.isNumber || ifExp.number == 1) {
            if(exp.isNumber) {
                int number = exp.number;
                for(int i = 0; i < Math.ceil(Math.log(exp.number)/Math.log(2)); i++) {
                    if(number%2 == 1) {
                        if(ifExp.isNumber) {
                            curMod.addGate(Toffoli, firstSignal.getLineName(i));
                        }
                        else {
                            //if the if Expression is not a number we just add all ifs (multiple if nested) to the control lines
                            curMod.addGate(Toffoli, firstSignal.getLineName(i), ifExp.getLines());
                        }
                    }
                    number /= 2;
                }
            }
            else {
                for(int i = 0; i < firstSignal.getWidth(); i++) {
                    if (ifExp.isNumber) {
                        curMod.addGate(Toffoli, firstSignal.getLineName(i), exp.getLineName(i));
                    } else {
                        ArrayList<String> controlLines = new ArrayList<String>();
                        controlLines.add(exp.getLineName(i));
                        controlLines.addAll(ifExp.getLines());
                        curMod.addGate(Toffoli, firstSignal.getLineName(i), controlLines);
                    }
                }
            }
        }
    }

    public ExpressionObject logicalAnd(ExpressionObject firstExp, ExpressionObject secondExp) {
        if(firstExp.isNumber && secondExp.isNumber) {
            int newValue = firstExp.number == 1 && secondExp.number == 1?1:0;
            return new ExpressionObject(newValue);
        }
        else if(firstExp.isNumber) {
            if(firstExp.number == 0) {
                return new ExpressionObject(0);
            }
            else {
                return secondExp;
            }
        }
        else if(secondExp.isNumber) {
            if(secondExp.number == 0) {
                return new ExpressionObject(0);
            }
            else {
                return firstExp;
            }
        }
        else {
            SignalObject additionalLine = curMod.getAdditionalLines(1);
            ArrayList<String> controlLines = new ArrayList<>();
            controlLines.addAll(firstExp.getLines());
            controlLines.addAll(secondExp.getLines());
            int resetStart = getResetStart(firstExp); //we have to reset beginning from the first Expression
            curMod.addGate(Toffoli, additionalLine.getLineName(0),controlLines);
            int resetEnd = curMod.getLastGateNumber();
            ExpressionObject newExp = new ExpressionObject(additionalLine, resetStart, resetEnd);
            newExp.addContainedSignals(firstExp.getContainedSignals());
            newExp.addContainedSignals(secondExp.getContainedSignals());
            return newExp;
        }
    }



    public void resetExpression(ExpressionObject exp) {
        if(lineAware && exp.resetStart != -1) {
            //we have used the expression so we can now reverse it
            int start = curMod.getLastGateNumber()+1;
            curMod.addCopyOfGates(exp.resetStart, exp.resetEnd);
            curMod.reverseGates(start, curMod.getLastGateNumber());
            for(int i = 0; i < exp.getWidth(); i++) {
                curMod.resetLine(exp.getLineName(i));
            }
        }
    }

    public void resetExpressionNoLine(ExpressionObject exp) {
        //resets the Expression without freeing the line
        //used mainly for If Expressions because the lien is reset with fi
        if(lineAware && exp.resetStart != -1) {
            int start = curMod.getLastGateNumber()+1;
            curMod.addCopyOfGates(exp.resetStart, exp.resetEnd);
            curMod.reverseGates(start, curMod.getLastGateNumber());
        }
    }

    public void resetLine(String lineName) {
        //in If Expressions we can reset the line directly
        curMod.resetLine(lineName);
    }

    private int getResetStart(ExpressionObject exp) {
        //if the Expression Object has gates to reset add them
        //else only reset from the current gate
        return exp.resetStart == -1?curMod.getLastGateNumber()+1:exp.resetStart;
    }
}
