package CodeGen;

import SymTable.Obj;

import java.util.ArrayList;

//Object for Codegen to get Information needed like Signal name
    //for busses appends the indexes to all elements
public class SignalObject {
    private final ArrayList<String> lines;
    private final String name;



    public SignalObject(Obj obj, int startWidth, int endWidth) {
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

    public int getWidth() {
        return lines.size();
    }

    public String getLineName(int index) {
        return lines.get(index);
    }

}
