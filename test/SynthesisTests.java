import AbstractSyntaxTree.*;
import CodeGen.Gate;
import SymTable.Obj;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class SynthesisTests {
    @Test
    public void AdditionOpOfBinaryExpression(){
        throw new UnsupportedOperationException();
    }

    @Test
    public void SubtractionOpOfBinaryExpression(){
        throw new UnsupportedOperationException();
    }

    @Test
    public void AddAssignment(){
        final String additionLOperandIdent = "x1";
        final String additionROperandIdent = "x2";
        final String assignmentLOperandIdent = "x0";
        final String additionalLinesSignalIdent = "addLine";
        final int signalBitwidth = 2;

        final var assignmentLOperandObj = new Obj(Obj.Kind.Out, assignmentLOperandIdent, signalBitwidth);
        final var additionLOperandObj = new Obj(Obj.Kind.Inout, additionLOperandIdent, signalBitwidth);
        final var additionROperandObj = new Obj(Obj.Kind.Inout, additionROperandIdent, signalBitwidth);
        final var additionalLinesObj = new Obj(Obj.Kind.Wire, additionalLinesSignalIdent, 4);

        var moduleParameters = new HashMap<String, Obj>();
        moduleParameters.put(assignmentLOperandIdent, assignmentLOperandObj);
        moduleParameters.put(additionLOperandIdent, additionLOperandObj);
        moduleParameters.put(additionROperandIdent, additionROperandObj);
        moduleParameters.put(additionalLinesSignalIdent, additionalLinesObj);

        var module = new CodeMod("main", moduleParameters);
        var moduleStatements = new ArrayList<Statement>();

        var additionLOperand = SignalExpression.createInstanceForTest(additionLOperandObj.name, new NumberExpression(0), new NumberExpression(signalBitwidth - 1));
        var additionROperand = SignalExpression.createInstanceForTest(additionROperandObj.name, new NumberExpression(0), new NumberExpression(signalBitwidth - 1));
        var assignmentLOperand = SignalExpression.createInstanceForTest(assignmentLOperandObj.name, new NumberExpression(0), new NumberExpression(signalBitwidth - 1));
        var additionalLinesOperand = SignalExpression.createInstanceForTest(additionalLinesObj.name, new NumberExpression(0), new NumberExpression(additionalLinesObj.width - 1));
        var assignmenrRExpr = new BinaryExpression(additionLOperand, additionROperand, BinaryExpression.Kind.PLUS);

        var assignment = new AssignStatement(assignmentLOperand, assignmenrRExpr, AssignStatement.Kind.PLUS, false);
        moduleStatements.add(assignment);
        module.addStatements(moduleStatements);

        final var actualGeneratedGates = assignment.generate(module);
        final var expectedGeneratedGates = new ArrayList<Gate>();

        // Backup x_2
        var backupLine0 = new Gate(Gate.Kind.Toffoli);
        backupLine0.addControlLine(additionROperand.getLineName(0));
        backupLine0.addTargetLine(additionalLinesOperand.getLineName(0));
        expectedGeneratedGates.add(backupLine0);

        var backupLine1 = new Gate(Gate.Kind.Toffoli);
        backupLine1.addControlLine(additionROperand.getLineName(1));
        backupLine1.addTargetLine(additionalLinesOperand.getLineName(1));
        expectedGeneratedGates.add(backupLine1);

        // e_1 = x_1 + x_2
        var add_maj0_0 = new Gate(Gate.Kind.Toffoli);
        add_maj0_0.addControlLine(additionLOperand.getLineName(0));
        add_maj0_0.addTargetLine(additionROperand.getLineName(0));
        expectedGeneratedGates.add(add_maj0_0);

        var add_maj0_1 = new Gate(Gate.Kind.Toffoli);
        add_maj0_1.addControlLine(additionLOperand.getLineName(0));
        add_maj0_1.addTargetLine(additionalLinesOperand.getLineName(2));
        expectedGeneratedGates.add(add_maj0_1);

        var add_maj0_2 = new Gate(Gate.Kind.Toffoli);
        add_maj0_2.addControlLine(additionROperand.getLineName(0));
        add_maj0_2.addControlLine(additionalLinesOperand.getLineName(2));
        add_maj0_2.addTargetLine(additionLOperand.getLineName(0));
        expectedGeneratedGates.add(add_maj0_2);

        var add_maj1_0 = new Gate(Gate.Kind.Toffoli);
        add_maj1_0.addControlLine(additionLOperand.getLineName(1));
        add_maj1_0.addTargetLine(additionROperand.getLineName(1));
        expectedGeneratedGates.add(add_maj1_0);

        var add_maj1_1 = new Gate(Gate.Kind.Toffoli);
        add_maj1_1.addControlLine(additionLOperand.getLineName(1));
        add_maj1_1.addTargetLine(additionLOperand.getLineName(0));
        expectedGeneratedGates.add(add_maj1_1);

        var add_maj1_2 = new Gate(Gate.Kind.Toffoli);
        add_maj1_2.addControlLine(additionLOperand.getLineName(0));
        add_maj1_2.addControlLine(additionROperand.getLineName(1));
        add_maj1_2.addTargetLine(additionLOperand.getLineName(1));
        expectedGeneratedGates.add(add_maj1_2);

        var add_unmaj1_0 = new Gate(Gate.Kind.Toffoli);
        add_unmaj1_0.addControlLine(additionLOperand.getLineName(0));
        add_unmaj1_0.addControlLine(additionROperand.getLineName(1));
        add_unmaj1_0.addTargetLine(additionLOperand.getLineName(1));
        expectedGeneratedGates.add(add_unmaj1_0);

        var add_unmaj1_1 = new Gate(Gate.Kind.Toffoli);
        add_unmaj1_1.addControlLine(additionLOperand.getLineName(1));
        add_unmaj1_1.addTargetLine(additionLOperand.getLineName(0));
        expectedGeneratedGates.add(add_unmaj1_1);

        var add_unmaj1_2 = new Gate(Gate.Kind.Toffoli);
        add_unmaj1_2.addControlLine(additionLOperand.getLineName(0));
        add_unmaj1_2.addTargetLine(additionROperand.getLineName(1));
        expectedGeneratedGates.add(add_unmaj1_2);

        var add_unmaj0_0 = new Gate(Gate.Kind.Toffoli);
        add_unmaj0_0.addControlLine(additionalLinesOperand.getLineName(signalBitwidth));
        add_unmaj0_0.addControlLine(additionROperand.getLineName(0));
        add_unmaj0_0.addTargetLine(additionLOperand.getLineName(0));
        expectedGeneratedGates.add(add_unmaj0_0);

        var add_unmaj0_1 = new Gate(Gate.Kind.Toffoli);
        add_unmaj0_1.addControlLine(additionLOperand.getLineName(0));
        add_unmaj0_1.addTargetLine(additionalLinesOperand.getLineName(signalBitwidth));
        expectedGeneratedGates.add(add_unmaj0_1);

        var add_unmaj0_2 = new Gate(Gate.Kind.Toffoli);
        add_unmaj0_2.addControlLine(additionalLinesOperand.getLineName(signalBitwidth));
        add_unmaj0_2.addTargetLine(additionROperand.getLineName(0));
        expectedGeneratedGates.add(add_unmaj0_2);

        var additionCarryInInversionGate = new Gate(Gate.Kind.Toffoli);
        additionCarryInInversionGate.addTargetLine(additionalLinesOperand.getLineName(signalBitwidth));
        expectedGeneratedGates.add(additionCarryInInversionGate);

        var backupLine0Inversion = new Gate(Gate.Kind.Fredkin);
        backupLine0Inversion.addControlLine(additionalLinesOperand.getLineName(signalBitwidth));
        backupLine0Inversion.addTargetLine(additionROperand.getLineName(0));
        backupLine0Inversion.addTargetLine(additionalLinesOperand.getLineName(0));
        expectedGeneratedGates.add(backupLine0Inversion);

        var backupLine1Inversion = new Gate(Gate.Kind.Fredkin);
        backupLine1Inversion.addControlLine(additionalLinesOperand.getLineName(signalBitwidth));
        backupLine1Inversion.addTargetLine(additionROperand.getLineName(1));
        backupLine1Inversion.addTargetLine(additionalLinesOperand.getLineName(1));
        expectedGeneratedGates.add(backupLine1Inversion);

        var additionalCarryInInversionGate_2 = new Gate(Gate.Kind.Toffoli);
        additionalCarryInInversionGate_2.addTargetLine(additionalLinesOperand.getLineName(signalBitwidth));
        expectedGeneratedGates.add(additionalCarryInInversionGate_2);

        // x0 += e_1
        var addition_maj0_0 = new Gate(Gate.Kind.Toffoli);
        addition_maj0_0.addControlLine(additionalLinesOperand.getLineName(0));
        addition_maj0_0.addTargetLine(assignmentLOperand.getLineName(0));
        expectedGeneratedGates.add(addition_maj0_0);

        var addition_maj0_1 = new Gate(Gate.Kind.Toffoli);
        addition_maj0_1.addControlLine(additionalLinesOperand.getLineName(0));
        addition_maj0_1.addTargetLine(additionalLinesOperand.getLineName(3));
        expectedGeneratedGates.add(addition_maj0_1);

        var addition_maj0_2 = new Gate(Gate.Kind.Toffoli);
        addition_maj0_2.addControlLine(additionalLinesOperand.getLineName(3));
        addition_maj0_2.addControlLine(assignmentLOperand.getLineName(0));
        addition_maj0_2.addTargetLine(additionalLinesOperand.getLineName(0));
        expectedGeneratedGates.add(addition_maj0_2);

        var addition_maj1_0 = new Gate(Gate.Kind.Toffoli);
        addition_maj1_0.addControlLine(additionalLinesOperand.getLineName(1));
        addition_maj1_0.addTargetLine(assignmentLOperand.getLineName(1));
        expectedGeneratedGates.add(addition_maj1_0);

        var addition_maj1_1 = new Gate(Gate.Kind.Toffoli);
        addition_maj1_1.addControlLine(additionalLinesOperand.getLineName(1));
        addition_maj1_1.addTargetLine(additionalLinesOperand.getLineName(0));
        expectedGeneratedGates.add(addition_maj1_1);

        var addition_maj1_2 = new Gate(Gate.Kind.Toffoli);
        addition_maj1_2.addControlLine(assignmentLOperand.getLineName(1));
        addition_maj1_2.addControlLine(additionalLinesOperand.getLineName(0));
        addition_maj1_2.addTargetLine(additionalLinesOperand.getLineName(1));
        expectedGeneratedGates.add(addition_maj1_2);

        var addition_unmaj1_0 = new Gate(Gate.Kind.Toffoli);
        addition_unmaj1_0.addControlLine(assignmentLOperand.getLineName(1));
        addition_unmaj1_0.addControlLine(additionalLinesOperand.getLineName(0));
        addition_unmaj1_0.addTargetLine(additionalLinesOperand.getLineName(1));
        expectedGeneratedGates.add(addition_unmaj1_0);

        var addition_unmaj1_1 = new Gate(Gate.Kind.Toffoli);
        addition_unmaj1_1.addControlLine(additionalLinesOperand.getLineName(1));
        addition_unmaj1_1.addTargetLine(additionalLinesOperand.getLineName(0));
        expectedGeneratedGates.add(addition_unmaj1_1);

        var addition_unmaj1_2 = new Gate(Gate.Kind.Toffoli);
        addition_unmaj1_2.addControlLine(additionalLinesOperand.getLineName(0));
        addition_unmaj1_2.addTargetLine(assignmentLOperand.getLineName(1));
        expectedGeneratedGates.add(addition_unmaj1_2);

        // TODO:
        var addition_unmaj0_0 = new Gate(Gate.Kind.Toffoli);
        addition_unmaj0_0.addControlLine(assignmentLOperand.getLineName(0));
        addition_unmaj0_0.addControlLine(additionalLinesOperand.getLineName(3));
        addition_unmaj0_0.addTargetLine(additionalLinesOperand.getLineName(0));
        expectedGeneratedGates.add(addition_unmaj0_0);

        var addition_unmaj0_1 = new Gate(Gate.Kind.Toffoli);
        addition_unmaj0_1.addControlLine(additionalLinesOperand.getLineName(0));
        addition_unmaj0_1.addTargetLine(additionalLinesOperand.getLineName(3));
        expectedGeneratedGates.add(addition_unmaj0_1);

        var addition_unmaj0_2 = new Gate(Gate.Kind.Toffoli);
        addition_unmaj0_2.addControlLine(additionalLinesOperand.getLineName(3));
        addition_unmaj0_2.addTargetLine(assignmentLOperand.getLineName(0));
        expectedGeneratedGates.add(addition_unmaj0_2);

        AssertGateListEquality(expectedGeneratedGates, actualGeneratedGates);
    }

    @Test
    public void AddAssignmentOfConstant(){
        throw new UnsupportedOperationException();
    }

    @Test
    public void MinusAssignment(){
        throw new UnsupportedOperationException();
    }

    @Test
    public void MinusAssignmentOfConstant(){
        throw new UnsupportedOperationException();
    }

    @Test
    public void XorAssignment(){
        throw new UnsupportedOperationException();
    }

    @Test
    public void XorAssignmentOfConstant(){
        throw new UnsupportedOperationException();
    }

    private static void AssertGateListEquality(Collection<Gate> expected, Collection<Gate> actual){
        Assert.assertEquals(expected.size(), actual.size());

        var expectedGateIterator = expected.iterator();
        var actualGateIterator = actual.iterator();

        for (int i = 0; i < expected.size(); ++i)
            AssertGateEquality(i, expectedGateIterator.next(), actualGateIterator.next());
    }

    private static void AssertGateEquality(int gateIdx, Gate expected, Gate actual) {
        Assert.assertEquals("Gate " + gateIdx, expected.kind, actual.kind);
        Assert.assertEquals("Gate " + gateIdx, expected.getControlLines().size(), actual.getControlLines().size());

        final Set<String> expectedControlLines = new HashSet<>(expected.getControlLines());
        final Set<String> actualControlLines = new HashSet<>(actual.getControlLines());
        for (var expectedControlLine : expectedControlLines)
            Assert.assertTrue("Gate " + gateIdx + " control line: " + expectedControlLine + " not found, actual control lines are: " + stringifySet(actualControlLines), actualControlLines.contains(expectedControlLine));

        Assert.assertEquals("Gate " + gateIdx, expected.getTargetLines().size(), actual.getTargetLines().size());
        final Set<String> expectedTargetLines = new HashSet<>(expected.getTargetLines());
        final Set<String> actualTargetLines = new HashSet<>(actual.getTargetLines());
        for (var expectedTargetLine : expectedTargetLines)
            Assert.assertTrue("Gate " + gateIdx + " target line: " + expectedTargetLine + " not found, actual target lines are: " + stringifySet(actualTargetLines), actualTargetLines.contains(expectedTargetLine));
    }

    private static String stringifySet(Set<String> set){
        return set.stream().collect(Collectors.joining(","));
    }
}
