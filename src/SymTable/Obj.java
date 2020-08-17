package SymTable;

import java.util.Collections;

public class Obj {
    public enum Kind {
        In, Out, Inout, Wire, State
    }

    public final Kind kind;
    public final String name;
    public final int width;

    private boolean garbage;
    private boolean constant;

    public Obj(Kind kind, String name, int width) {
        this.kind = kind;
        this.name = name;
        this.width = width;
        setGarbage();
        setConstant();
    }


    public Obj(Obj obj) {
        this.kind = obj.kind;
        this.name = obj.name;
        this.width = obj.width;
    }

    private void setGarbage() {
        garbage = (kind != Kind.Out);
    }

    private void setConstant() {
        //Wires and Out are Constant 0 Input
        constant = (kind == Kind.Wire || kind == Kind.Out);
    }

    public void setGarbage(boolean isGarbage) {
        garbage = isGarbage;
    }

    public boolean getGarbage(){
        return garbage;
    }

    public boolean getConstant(){
        return constant;
    }
}
