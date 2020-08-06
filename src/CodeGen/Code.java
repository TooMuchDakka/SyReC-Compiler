package CodeGen;


import SymTable.Mod;
import SymTable.Obj;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
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
                curWriter.append(line.name).append(" "); //write each variable
            }
            //we leave out inputs and outputs as these are optional and not specified in SyReC
            curWriter.newLine();
            curWriter.append(".constants ");
            for (Obj line : lines) {
                if(line.kind == Obj.Kind.Wire || line.kind == Obj.Kind.Out) {
                    //Wires and Out are Constant 0 Input
                    curWriter.append('0');
                }
                else {
                    curWriter.append('-');
                }
            }
            curWriter.newLine();
            curWriter.append(".garbage ");
            for (Obj line : lines) {
                if(line.kind == Obj.Kind.Wire || line.kind == Obj.Kind.In) {
                    //Wires and Out are Constant 0 Input
                    curWriter.append('1');
                }
                else {
                    curWriter.append('-');
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


}
