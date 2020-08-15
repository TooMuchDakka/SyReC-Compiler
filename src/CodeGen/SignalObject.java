package CodeGen;

import SymTable.Obj;

import java.util.ArrayList;

//Object for Codegen to get Information needed like Signal name
    //for busses appends the indexes to all elements
public class SignalObject {
    private final ArrayList<String> lines;
    private final String name;



    public SignalObject(Obj obj, int startWidth, int endWidth) {
        //Constructor used when creating SignalObject from InputCode
        this.name = obj.name;
        lines = new ArrayList<>();
        if(obj.width == 1) {
            lines.add(name);
        }
        else if(startWidth < endWidth) {
            for(int i = startWidth; i <= endWidth; i++) {
                lines.add(name+"_"+i);
            }
        }
        else {
            for(int i = startWidth; i >= endWidth; i--) {
                lines.add((name+"_"+i));
            }
        }
    }

    public SignalObject(String name, ArrayList<String> lines) {
        //SignalObject used for additionalLines
        this.name = name;
        this.lines = new ArrayList<>(lines);
    }

    public int getWidth() {
        return lines.size();
    }

    public String getLineName(int index) {
        return lines.get(index);
    }

}
