package CodeGen;


import SymTable.Mod;
import SymTable.Obj;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.Map;

public class Code {

    Path curPath;
    BufferedWriter curWriter;

    public Code(String folderName) {
        curPath = Path.of(folderName);
        try {
            Files.createDirectory(curPath);
        } catch (FileAlreadyExistsException ee) {
            //do nothing as the directory already exists
        } catch (IOException e) {
            System.err.println("Failed to create directory!" + e.getMessage());
        }
    }

    public void createModule(Mod module) {
        try { //delete File if it already exists
            Files.deleteIfExists(Path.of(curPath.toString(), module.name+".real"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            curWriter = Files.newBufferedWriter(Path.of(curPath.toString(), module.name+".real"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        writeHeader(module);

    }

    public void writeHeader(Mod module) {
        //here the Header of the REAL File is written
        try {
            curWriter.append("# ").append(module.name);
            curWriter.newLine();
            curWriter.append(".version 2.0");
            curWriter.newLine();
            curWriter.append(".numvars ").append(String.valueOf(module.getLineCount()));
            curWriter.newLine();
            Obj[] lines = module.getLines();
            curWriter.append(".variables ");
            for (Obj line : lines) {
                if(line.width == 1) {
                    curWriter.append(line.name).append(" "); //write each variable
                }
                else for(int i = 0; i < line.width; i++) {
                    curWriter.append(line.name).append("_").append(String.valueOf(i)).append(" "); //write all subvariables of the width
                }
            }
            //we leave out inputs and outputs as these are optional and not specified in SyReC
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void endModule(Mod module)  {
        try {
            curWriter.append(".end");
            curWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //swap of two signals
    //function is only called if the width is equal
    public void swap(SignalObject firstSig, SignalObject secondSig) {
        try {
            for(int i = 0; i < firstSig.getWidth(); i++) {
                curWriter.append("f2 ").append(firstSig.ident);
                appendBus(firstSig, i);
                curWriter.append(" ").append(secondSig.ident);
                appendBus(secondSig, i);
                curWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //negate given Signal
    public void not(SignalObject sig) {
        try {
            for(int i = 0; i < sig.getWidth(); i++) {
                curWriter.append("t1 ").append(sig.ident);
                appendBus(sig, i);
                curWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    //appends the correct index to a BusSignal
    private void appendBus(SignalObject sig, int i) throws IOException {
        if(sig.isBus && sig.isAscending()) {
            curWriter.append("_").append(String.valueOf(i+sig.getStartWidth()));
        }
        if(sig.isBus && !sig.isAscending()) {
            curWriter.append("_").append(String.valueOf(sig.getStartWidth()-i));
        }
    }

    //++= Statement
    public void plusplus(SignalObject sig) {
        try {
            for(int i = sig.getWidth()-1; i >= 0; i--) {
                curWriter.append("t").append(String.valueOf(i+1));
                for(int j = 0; j <= i; j++) {
                    curWriter.append(" ").append(sig.ident);
                    appendBus(sig, j);
                }
                curWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //--= Statement, basically plusplus but in reverse
    public void minusminus(SignalObject sig) {
        try {
            for(int i = 0; i < sig.getWidth(); i++) {
                curWriter.append("t").append(String.valueOf(i+1));
                for(int j = 0; j <= i; j++) {
                    curWriter.append(" ").append(sig.ident);
                    appendBus(sig, j);
                }
                curWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
