package CodeGen;

import SymTable.Obj;

//Object for Codegen to get Information needed like Signal name
    //or information pertaining width
public class SignalObject {
    private int startWidth;
    private int endWidth;
    public String ident;
    public final boolean isBus;
    private boolean isAscending;

    public SignalObject(Obj local) {
        startWidth = 0;
        endWidth = local.width-1;
        ident = local.name;
        isBus = startWidth!=endWidth;
        checkAscending();
    }

    public SignalObject(String ident, int start, int end, boolean isBus) {
        startWidth = start;
        endWidth = end;
        this.ident = ident;
        this.isBus = isBus;
        checkAscending();
    }

    public int getWidth() {
        if(isAscending) {
            return endWidth-startWidth+1;
        }
        else {
            return startWidth-endWidth+1;
        }
    }

    public int getStartWidth() {
        return startWidth;
    }

    public int getEndWidth() {
        return endWidth;
    }

    public void setStartWidth(int startWidth) {
        this.startWidth = startWidth;
        checkAscending();
    }

    public void setEndWidth(int endWidth) {
        this.endWidth = endWidth;
        checkAscending();
    }

    private void checkAscending() {
        isAscending = startWidth < endWidth;
    }

    public boolean isAscending() {
        return isAscending;
    }
}
