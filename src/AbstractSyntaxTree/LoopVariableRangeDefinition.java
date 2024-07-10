package AbstractSyntaxTree;

public class LoopVariableRangeDefinition {
    public int startValue;
    public int endValue;
    public int stepSize;
    public int currentValue;

    public LoopVariableRangeDefinition(int startValue, int endValue, int stepSize){
        this.startValue = startValue;
        this.endValue = endValue;
        this.stepSize = stepSize;
        this.currentValue = this.startValue;
    }
}
