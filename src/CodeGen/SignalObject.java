package CodeGen;

import SymTable.Obj;

//Object for Codegen to get Information needed like Signal name
    //or information pertaining width
public class SignalObject {
    public int startWidth;
    public int endWidth;
    public String ident;
    public final boolean isBus;

    public SignalObject(Obj local) {
        startWidth = 0;
        endWidth = local.width-1;
        ident = local.name;
        isBus = startWidth!=endWidth;
    }

    public SignalObject(String ident, int start, int end, boolean isBus) {
        startWidth = start;
        endWidth = end;
        this.ident = ident;
        this.isBus = isBus;
    }

    public int getWidth() {
        return endWidth-startWidth+1;
    }
}
