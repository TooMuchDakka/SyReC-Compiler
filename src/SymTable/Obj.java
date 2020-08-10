package SymTable;

public class Obj {
    public enum Kind {
        In, Out, Inout, Wire, State
    }

    public final Kind kind;
    public final String name;
    public final int width;

    public Obj(Kind kind, String name, int width) {
        this.kind = kind;
        this.name = name;
        this.width = width;
    }

    public Obj(Obj obj) {
        this.kind = obj.kind;
        this.name = obj.name;
        this.width = obj.width;
    }
}
