package CodeGen;

import SymTable.Obj;

//Object for Codegen to get Information needed like Signal name
    //or information pertaining width
public class SignalObject {
    public int startWidth;
    public int endWidth;
    public String ident;

    public SignalObject(Obj local) {
        startWidth = 0;
        endWidth = local.width-1;
        ident = local.name;
    }

    public SignalObject(String ident, int start, int end) {
        startWidth = start;
        endWidth = end;
        this.ident = ident;
    }
}
