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
                if(line.kind == Obj.Kind.Wire || line.kind == Obj.Kind.Out) {
                    //Wires and Out are Constant 0 Input
                    curWriter.append(String.join("", Collections.nCopies(line.width, "0")));
                }
                else {
                    curWriter.append(String.join("", Collections.nCopies(line.width, "-")));
                }
            }
            curWriter.newLine();
            curWriter.append(".garbage ");
            for (Obj line : lines) {
                if(line.kind == Obj.Kind.Wire || line.kind == Obj.Kind.In) {
                    //Wires and Out are Constant 0 Input
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
    public void swap(SignalObject firstSig, SignalObject secondSig) {
        for(int i = 0; i < firstSig.getWidth(); i++) {
            ArrayList<String> targetLines = new ArrayList<String>(Arrays.asList(firstSig.getLineName(i), secondSig.getLineName(i)));
            curMod.addGate(Fredkin, targetLines);
        }
    }

    //negate given Signal
    public void not(SignalObject sig) {
        for(int i = 0; i < sig.getWidth(); i++) {
            curMod.addGate(Toffoli, sig.getLineName(i));
        }
    }


    //++= Statement
    public void plusplus(SignalObject sig) {
        for(int i = sig.getWidth()-1; i >= 0; i--) {
            ArrayList<String> controlLines = new ArrayList<>();
            for(int j = 0; j < i; j++) {
                controlLines.add(sig.getLineName(j));
            }
            curMod.addGate(Toffoli, sig.getLineName(i),controlLines);
        }
    }

    //--= Statement, plusplus but in reverse
    public void minusminus(SignalObject sig) {
        int lastGate = curMod.getLastGateNumber();
        plusplus(sig);
        curMod.reverseGates(lastGate+1, curMod.getLastGateNumber());
    }

    public ExpressionObject leftShift(ExpressionObject exp, int number) {
        if(exp.isNumber) {
            return new ExpressionObject(exp.number << number);
        }
        else {
            SignalObject additionalLines = curMod.getAdditionalLines(exp.signal.getWidth());
            int resetStart = curMod.getLastGateNumber()+1;
            for(int i = 0; i < exp.signal.getWidth()-number; i++) {
                curMod.addGate(Toffoli, additionalLines.getLineName(i+number), exp.signal.getLineName(i));
            }
            int resetEnd = curMod.getLastGateNumber();
            return new ExpressionObject(additionalLines, resetStart, resetEnd);
        }
    }

    public ExpressionObject rightShift(ExpressionObject exp, int number) {
        if(exp.isNumber) {
            return new ExpressionObject(exp.number >> number);
        }
        else {
            SignalObject additionalLines = curMod.getAdditionalLines(exp.signal.getWidth());
            int resetStart = curMod.getLastGateNumber()+1;
            for(int i = 0; i < exp.signal.getWidth()-number; i++) {
                curMod.addGate(Toffoli, additionalLines.getLineName(i), exp.signal.getLineName(i+number));
            }
            int resetEnd = curMod.getLastGateNumber();
            return new ExpressionObject(additionalLines, resetStart, resetEnd);
        }
    }

    public ExpressionObject notExp(ExpressionObject exp) {
        //bitwise not on a number
        if(exp.isNumber) {
            return new ExpressionObject(~exp.number);
        }
        else {
            SignalObject additionalLines = curMod.getAdditionalLines(exp.signal.getWidth());
            int resetStart = curMod.getLastGateNumber()+1;
            for(int i = 0; i < exp.signal.getWidth(); i++) {
                ArrayList<String> controlLines = new ArrayList<>();
                controlLines.add(exp.signal.getLineName(i));
                curMod.addGate(Toffoli, additionalLines.getLineName(i), controlLines);
                curMod.addGate(Toffoli, additionalLines.getLineName(i));
            }
            int resetEnd = curMod.getLastGateNumber();
            return new ExpressionObject(additionalLines, resetStart, resetEnd);
        }
    }

    public void xorAssign(SignalObject firstSignal, ExpressionObject exp) {
        if(exp.isNumber) {
            int number = exp.number;
            for(int i = 0; i < Math.ceil(Math.log(exp.number)/Math.log(2)); i++) {
                if(number%2 == 1) {
                    curMod.addGate(Toffoli, firstSignal.getLineName(i));
                }
                number /= 2;
            }
        }
        else {
            for(int i = 0; i < firstSignal.getWidth(); i++) {
                curMod.addGate(Toffoli, firstSignal.getLineName(i), exp.signal.getLineName(i));
            }
        }
    }

    public void resetExpression(ExpressionObject exp) {
        if(lineAware && exp.resetStart != -1) {
            //we have used the expression so we can now reverse it
            curMod.reverseGates(exp.resetStart, exp.resetEnd);
        }
    }
}
