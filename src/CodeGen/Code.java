package CodeGen;


import SymTable.Mod;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Code {

    Path curPath;
    public Code(String fileName) {
        curPath = Path.of(".", fileName);
        try {
            Files.createDirectory(curPath);
        } catch (FileAlreadyExistsException ee) {
            //do nothing as the directory already exists
        } catch (IOException e) {
            System.err.println("Failed to create directory!" + e.getMessage());
        }
    }

    public void createModule(Mod module) {
        try {
            Files.deleteIfExists(Path.of(curPath.toString(), module.name));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
