package SymTable;

public abstract class Obj {
    public enum Kind {
        In, Out, Inout, Wire, State
    }

    public final Kind kind;
    public final String name;

    public Obj(Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }
}
