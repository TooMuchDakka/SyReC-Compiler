package CodeGen;


import AbstractSyntaxTree.CodeMod;
import AbstractSyntaxTree.LoopVariableRangeDefinition;
import AbstractSyntaxTree.SignalExpression;
import SymTable.Mod;
import SymTable.Obj;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static CodeGen.Gate.Kind.*;

public class Code {

    private static class ThreeBitMajorityGate {
        public String inCarry;
        public String inOperandA;
        public String inOperandB;
        public String outCarryInXorOperandA;
        public String outOperandsSum;
        public String outCarry;
        public List<Gate> internalGates;

        public ThreeBitMajorityGate(String inCarry, String inOperandA, String inOperandB) {
            this.inCarry = inCarry;
            this.inOperandA = inOperandA;
            this.inOperandB = inOperandB;

            this.outCarryInXorOperandA = this.inCarry;
            this.outOperandsSum = this.inOperandB;
            this.outCarry = this.inOperandA;

            Gate operandSumGate = new Gate(Toffoli);
            operandSumGate.addControlLine(this.inOperandA);
            operandSumGate.addTargetLine(this.inOperandB);

            Gate operandAAndInCarryGate = new Gate(Toffoli);
            operandAAndInCarryGate.addControlLine(this.inOperandA);
            operandAAndInCarryGate.addTargetLine(this.inCarry);

            Gate newCarryGate = new Gate(Toffoli);
            newCarryGate.addControlLine(this.inCarry);
            newCarryGate.addControlLine(this.inOperandB);
            newCarryGate.addTargetLine(this.inOperandA);
            internalGates = List.of(new Gate[]{ operandSumGate, operandAAndInCarryGate, newCarryGate});
        }
    }

    private static class UnmajorityAndSumGate {
        public String inCarryXorOperandA;
        public String inOperandsSum;
        public String inNewCarry;

        public String outCarry;
        public String outOperandSumAndCarrySum;
        public String outOperandA;
        public List<Gate> internalGates;

        public UnmajorityAndSumGate(String inCarryXorOperandA, String inOperandsSum, String inNewCarry){
            this.inCarryXorOperandA = inCarryXorOperandA;
            this.inOperandsSum = inOperandsSum;
            this.inNewCarry = inNewCarry;

            this.outCarry = this.inCarryXorOperandA;
            this.outOperandSumAndCarrySum = this.inOperandsSum;
            this.outOperandA = this.inNewCarry;

            Gate originalOperandARecalculationGate = new Gate(Toffoli);
            originalOperandARecalculationGate.addControlLine(this.inCarryXorOperandA);
            originalOperandARecalculationGate.addControlLine(this.inOperandsSum);
            originalOperandARecalculationGate.addTargetLine(this.outOperandA);

            Gate originalCarryRecalculationGate = new Gate(Toffoli);
            originalCarryRecalculationGate.addControlLine(this.inNewCarry);
            originalCarryRecalculationGate.addTargetLine(this.outCarry);

            Gate operandSumAndCarrySumGate = new Gate(Toffoli);
            operandSumAndCarrySumGate.addControlLine(this.outCarry);
            operandSumAndCarrySumGate.addTargetLine(this.inOperandsSum);
            internalGates = List.of(new Gate[]{ originalOperandARecalculationGate, originalCarryRecalculationGate, operandSumAndCarrySumGate});
        }

        public UnmajorityAndSumGate(ThreeBitMajorityGate majorityGate)
        {
            this(majorityGate.outCarryInXorOperandA, majorityGate.outOperandsSum, majorityGate.outCarry);
        }
    }



    private static class RealHeaderSignalDefinitionEntry {
        public String variableIdent;
        public String inputIdent;
        public String outputIdent;
        public String isConstantStatusStringified;
        public String isGarbageStatusStringified;
        public int bitWidth;
        public boolean wasInputPadded;
    }

    public static CodeMod createModule(Mod module) {
        return new CodeMod(module.name, module.getLocals());
    }

    public static String stringifyRealHeaderSignalDefinitions(List<RealHeaderSignalDefinitionEntry> collection, BiFunction<RealHeaderSignalDefinitionEntry, Integer, String> perCollectionMapper, String stringifiedBitDelimiter, String stringifiedEntryDelimiter) {
        return collection
                .stream()
                .map(entry -> {
                    if (entry.bitWidth == 1) {
                        String generatedIdent = perCollectionMapper.apply(entry, 0);
                        int bitDelimitedIndex = generatedIdent.lastIndexOf("_");
                        if (bitDelimitedIndex == -1)
                            return generatedIdent;
                        
                        return generatedIdent.substring(0, bitDelimitedIndex);
                    } else {
                        return IntStream.range(0, entry.bitWidth)
                                .mapToObj(i -> perCollectionMapper.apply(entry, i))
                                .collect(Collectors.joining(stringifiedBitDelimiter));
                    }
                }).collect(Collectors.joining(stringifiedEntryDelimiter));
    }

    public static String stringifyGate(Gate gate, Map<String, String> ioToVariableMapping) {
        StringBuilder stringifiedGateDefinition = new StringBuilder();
        switch (gate.kind) {
            case Toffoli:
                stringifiedGateDefinition.append("t");
                break;
            case Fredkin:
                stringifiedGateDefinition.append("f");
                break;
            case Peres:
                stringifiedGateDefinition.append("p");
                break;
            case V:
                stringifiedGateDefinition.append("v");
                break;
            case Vplus:
                stringifiedGateDefinition.append("v+");
                break;
            case Placeholder:
                stringifiedGateDefinition.append("unimplemented");
                System.out.println("Warning, Placeholder Gate was used");
                break;
        }
        if (gate.getNumberOfLines() == 0)
            throw new IllegalArgumentException("Gate with no control or target lines detected!");

        stringifiedGateDefinition.append(gate.getNumberOfLines());

        if (gate.getNumberOfControlLines() > 0)
            stringifiedGateDefinition.append(" " + gate.getControlLines().stream().map(controlLine -> ioToVariableMapping.get(controlLine)).collect(Collectors.joining(" ")));

        if (gate.getNumberOfTargetLines() == 0)
            throw new IllegalArgumentException("Gate with no target lines detected!");

        stringifiedGateDefinition.append(" " + gate.getTargetLines().stream().map(targetLine -> ioToVariableMapping.get(targetLine)).collect(Collectors.joining(" ")));
        stringifiedGateDefinition.append(" #Original target lines: GATE ");
        stringifiedGateDefinition.append(String.join(" ", gate.getControlLines()));
        stringifiedGateDefinition.append(gate.getNumberOfControlLines() > 0 ? " " : "");
        stringifiedGateDefinition.append(String.join(" ", gate.getTargetLines()));
        return stringifiedGateDefinition.toString();
    }

    public static void endModule(Path exportFilePath, Mod module, CodeMod curMod, Optional<String> optionalExportResultFilenamePrefix) {
        List<Gate> gates = curMod.generate();

        int totalBitLengthSumOfUsedModuleSignalLines = curMod.getVariables().stream().map(variable -> variable.width).reduce(0, Integer::sum);
        List<RealHeaderSignalDefinitionEntry> realHeaderDefinitions = new ArrayList<>(curMod.getVariables().size());

        final String IS_GARBAGE_DEFINITION_ENTRY = "1";
        final String IS_NOT_GARBAGE_DEFINITION_ENTRY = "-";
        final String IS_CONSTANT_ZERO_DEFINITION_ENTRY = "0";
        final String IS_CONSTANT_ONE_DEFINITION_ENTRY = "1";
        final String IS_NOT_CONSTANT_DEFINITION_ENTRY = "-";
        final String SIGNAL_IDENT_AND_BIT_DELIMITER = "_";
        final String PADDING_INPUT_IDENT = SIGNAL_IDENT_AND_BIT_DELIMITER + "pI";
        final String PADDING_OUTPUT_IDENT = SIGNAL_IDENT_AND_BIT_DELIMITER + "pO";
        final String VARIABLE_IDENT = SIGNAL_IDENT_AND_BIT_DELIMITER + "v";

        int variableCounter = 0;
        int inputPaddingCounter = 0;
        int outputPaddingCounter = 0;
        for (Obj signal : curMod.getVariables()) {
            Boolean isGarbageFlag = signal.kind == Obj.Kind.Wire;
            Optional<Boolean> isConstantFlag = signal.kind == Obj.Kind.Out || signal.kind == Obj.Kind.Wire ? Optional.of(false) : Optional.empty();

            String inputDefinitionEntryForSignal = signal.name;
            String outputDefinitionEntryForSignal = signal.name;
            Boolean wasInputIdentPadded = false;
            if (signal.kind == Obj.Kind.In) {
                outputDefinitionEntryForSignal = PADDING_OUTPUT_IDENT + outputPaddingCounter;
                outputPaddingCounter++;
            } else {
                if (signal.kind != Obj.Kind.Inout) {
                    inputDefinitionEntryForSignal = PADDING_INPUT_IDENT + inputPaddingCounter;
                    inputPaddingCounter++;
                    wasInputIdentPadded = true;
                }
            }

            String isConstantFlagStringified = IS_NOT_CONSTANT_DEFINITION_ENTRY;
            if (isConstantFlag.isPresent())
                isConstantFlagStringified = isConstantFlag.get() ? IS_CONSTANT_ONE_DEFINITION_ENTRY : IS_CONSTANT_ZERO_DEFINITION_ENTRY;

            String isGarbageFlagStringified = isGarbageFlag ? IS_GARBAGE_DEFINITION_ENTRY : IS_NOT_GARBAGE_DEFINITION_ENTRY;

            RealHeaderSignalDefinitionEntry headerEntry = new RealHeaderSignalDefinitionEntry();
            headerEntry.variableIdent = VARIABLE_IDENT + variableCounter++;
            headerEntry.inputIdent = inputDefinitionEntryForSignal;
            headerEntry.isConstantStatusStringified = isConstantFlagStringified;
            headerEntry.isGarbageStatusStringified = isGarbageFlagStringified;
            headerEntry.outputIdent = outputDefinitionEntryForSignal;
            headerEntry.bitWidth = signal.width;
            headerEntry.wasInputPadded = wasInputIdentPadded;
            realHeaderDefinitions.add(headerEntry);
        }

        try {
            // Clear existing file contents or create new file
            new PrintWriter(exportFilePath.toFile()).close();

            BufferedWriter exportFileWriter = Files.newBufferedWriter(exportFilePath, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            exportFileWriter.append("# ").append(module.name);
            exportFileWriter.newLine();
            exportFileWriter.append(".version 2.0");
            exportFileWriter.newLine();
            exportFileWriter.append(".numvars ").append(String.valueOf(totalBitLengthSumOfUsedModuleSignalLines));
            exportFileWriter.newLine();

            exportFileWriter.append(".variables " + stringifyRealHeaderSignalDefinitions(realHeaderDefinitions, (entry, bit) -> entry.variableIdent + SIGNAL_IDENT_AND_BIT_DELIMITER + bit, " ", " "));
            exportFileWriter.newLine();
            exportFileWriter.append(".inputs " + stringifyRealHeaderSignalDefinitions(realHeaderDefinitions, (entry, bit) -> entry.inputIdent + SIGNAL_IDENT_AND_BIT_DELIMITER + bit, " ", " "));
            exportFileWriter.newLine();
            exportFileWriter.append(".constants " + stringifyRealHeaderSignalDefinitions(realHeaderDefinitions, (entry, _) -> entry.isConstantStatusStringified, "", ""));
            exportFileWriter.newLine();
            exportFileWriter.append(".outputs " + stringifyRealHeaderSignalDefinitions(realHeaderDefinitions, (entry, bit) -> entry.outputIdent + SIGNAL_IDENT_AND_BIT_DELIMITER + bit, " ", " "));
            exportFileWriter.newLine();
            exportFileWriter.append(".garbage " + stringifyRealHeaderSignalDefinitions(realHeaderDefinitions, (entry, _) -> entry.isGarbageStatusStringified, "", ""));
            exportFileWriter.newLine();
            exportFileWriter.append(".begin");
            exportFileWriter.newLine();

            Map<String, String> ioToVariableMappingLookup = realHeaderDefinitions.stream()
                    .flatMap(entry -> {
                                if (entry.bitWidth == 1)
                                    return Stream.of(new AbstractMap.SimpleEntry<String, String>((entry.wasInputPadded ? entry.outputIdent : entry.inputIdent), entry.variableIdent));

                                return IntStream.range(0, entry.bitWidth)
                                        .mapToObj(bit -> new AbstractMap.SimpleEntry<String, String>(
                                                (entry.wasInputPadded ? entry.outputIdent : entry.inputIdent) + SIGNAL_IDENT_AND_BIT_DELIMITER + bit,
                                                entry.variableIdent + SIGNAL_IDENT_AND_BIT_DELIMITER + bit));
                            }
                    )
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

            for (Gate gate : gates) {
                validateGateDefinition(gate);
                exportFileWriter.append(stringifyGate(gate, ioToVariableMappingLookup));
                exportFileWriter.newLine();
            }

            exportFileWriter.append(".end");
            exportFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Function to generate Placeholder gates (to see if functions for a given SyReC File are missing
    public static ArrayList<Gate> placeholder() {
        ArrayList<Gate> gates = new ArrayList<>();
        Gate placeholderGate = new Gate(Placeholder);
        gates.add(placeholderGate);
        return gates;
    }

    //swap of two signals
    //function is only called if the width is equal
    public static ArrayList<Gate> swap(SignalExpression firstSig, SignalExpression secondSig, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> gates = new ArrayList<>();
        for (int i = 0; i < firstSig.getWidth(loopVariableRangeDefinitionLookup); i++) {
            Gate tempGate = new Gate(Fredkin);
            tempGate.addTargetLine(firstSig.getLineName(i));
            tempGate.addTargetLine(secondSig.getLineName(i));
            gates.add(tempGate);
        }
        return gates;
    }

    //negate given Signal
    public static ArrayList<Gate> not(SignalExpression sig, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> gates = new ArrayList<>();
        for (int i = 0; i < sig.getWidth(loopVariableRangeDefinitionLookup); i++) {
            Gate tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(sig.getLineName(i));
            gates.add(tempGate);
        }
        return gates;
    }


    //++= Statement
    public static ArrayList<Gate> increment(SignalExpression sig, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> gates = new ArrayList<>();
        for (int i = sig.getWidth(loopVariableRangeDefinitionLookup) - 1; i >= 0; i--) {
            ArrayList<String> controlLines = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                controlLines.add(sig.getLineName(j));
            }
            Gate tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(sig.getLineName(i));
            tempGate.addControlLines(controlLines);
            gates.add(tempGate);
        }
        return gates;
    }

    //--= Statement, plusplus but in reverse
    public static ArrayList<Gate> decrement(SignalExpression sig, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> gates = increment(sig, loopVariableRangeDefinitionLookup);
        Collections.reverse(gates);
        return gates;
    }

    public static ArrayList<Gate> leftShift(ExpressionResult exp, int number, SignalExpression additionalLines, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> gates = new ArrayList<>();
        for (int i = 0; i < exp.getWidth(loopVariableRangeDefinitionLookup) - number; i++) {
            Gate tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(additionalLines.getLineName(i + number));
            tempGate.addControlLine(exp.getLineName(i));
            gates.add(tempGate);
        }
        return gates;
    }

    public static ArrayList<Gate> rightShift(ExpressionResult exp, int number, SignalExpression additionalLines, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> gates = new ArrayList<>();
        for (int i = 0; i < exp.getWidth(loopVariableRangeDefinitionLookup) - number; i++) {
            Gate tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(additionalLines.getLineName(i));
            tempGate.addControlLine(exp.getLineName(i + number));
            gates.add(tempGate);
        }
        return gates;
    }

    public static ArrayList<Gate> notExp(ExpressionResult exp, SignalExpression additionalLines, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> gates = new ArrayList<>();
        for (int i = 0; i < exp.getWidth(loopVariableRangeDefinitionLookup); i++) {
            Gate tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(additionalLines.getLineName(i));
            tempGate.addControlLine(exp.getLineName(i));
            gates.add(tempGate); // line.i = !oldLine.i
            tempGate = new Gate(Toffoli);
            tempGate.addTargetLine(additionalLines.getLineName(i));
            gates.add(tempGate); // line.i = !line.i
        }
        return gates;
    }

    public static ArrayList<Gate> xorAssign(SignalExpression firstSignal, ExpressionResult exp, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> gates = new ArrayList<>();
        if (exp.isNumber) {
            int number = exp.number;
            for (int i = 0; number != 0; i++) {
                if (number % 2 == 1) {
                    Gate newGate = new Gate(Toffoli);
                    newGate.addTargetLine(firstSignal.getLineName(i));
                    gates.add(newGate);
                }
                number /= 2;
            }
        } else {
            for (int i = 0; i < firstSignal.getWidth(loopVariableRangeDefinitionLookup); i++) {
                Gate newGate = new Gate(Toffoli);
                newGate.addTargetLine(firstSignal.getLineName(i));
                newGate.addControlLine(exp.getLineName(i));
                gates.add(newGate);
            }
        }
        return gates;
    }

    public static ArrayList<Gate> logicalAnd(ExpressionResult firstExp, ExpressionResult secondExp, SignalExpression additionalLine, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<String> controlLines = new ArrayList<>();
        controlLines.addAll(firstExp.getLines());
        controlLines.addAll(secondExp.getLines());
        ArrayList<Gate> gates = new ArrayList<>();
        Gate tempGate = new Gate(Toffoli);
        tempGate.addTargetLine(additionalLine.getLineName(0));
        tempGate.addControlLines(controlLines);
        gates.add(tempGate);
        return gates;
    }

    public static ArrayList<Gate> logicalOr(ExpressionResult firstExp, ExpressionResult secondExp, SignalExpression additionalLine, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<String> controlLines = new ArrayList<>();
        controlLines.addAll(firstExp.getLines());
        controlLines.addAll(secondExp.getLines());
        ArrayList<Gate> gates = new ArrayList<>();

        Gate tempGate = new Gate(Toffoli);
        tempGate.addTargetLine(firstExp.getLineName(0));
        gates.add(tempGate); // !a

        tempGate = new Gate(Toffoli);
        tempGate.addTargetLine(secondExp.getLineName(0));
        gates.add(tempGate); // !b

        tempGate = new Gate(Toffoli);
        tempGate.addTargetLine(additionalLine.getLineName(0));
        tempGate.addControlLines(controlLines);
        gates.add(tempGate); // !a and !b

        tempGate = new Gate(Toffoli);
        tempGate.addTargetLine(additionalLine.getLineName(0));
        gates.add(tempGate); // nand

        tempGate = new Gate(Toffoli);
        tempGate.addTargetLine(firstExp.getLineName(0));
        gates.add(tempGate); // !a to make the line usable again

        tempGate = new Gate(Toffoli);
        tempGate.addTargetLine(secondExp.getLineName(0));
        gates.add(tempGate); // !b to make the line usable again

        return gates;
    }

    public static ArrayList<Gate> plusAssign(SignalExpression signalExp, ExpressionResult res, SignalExpression additionalLines, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        return internalAddAssign(Optional.of(signalExp), new ExpressionResult(signalExp), res, additionalLines, loopVariableRangeDefinitionLookup);
    }

    public static ArrayList<Gate> minusAssign(SignalExpression signalExp, ExpressionResult res, SignalExpression additionalLines, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> gates = new ArrayList<>();
        if (res.isNumber) {
            int number = res.number;
            /* TODO: Uncomment if the compiler should also perform some optimizations
            if (number == 0) {
                //neutral operation, nothing to do
                return gates;
            }
             */
            if (number < 0) {
                //if we substract a negative number we can just use plusAssign
                ExpressionResult negative = new ExpressionResult(-number);
                return plusAssign(signalExp, negative, additionalLines, loopVariableRangeDefinitionLookup);
            }
        }
        //apart from the handling of negative or 0 numbers we can just use plusAssign and reverse the result
        gates = plusAssign(signalExp, res, additionalLines, loopVariableRangeDefinitionLookup);
        Collections.reverse(gates);
        return gates;
    }

    public static ArrayList<Gate> plus(ExpressionResult firstExp, ExpressionResult secondExp, SignalExpression additionalLines, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        return internalAddAssign(Optional.empty(), firstExp, secondExp, additionalLines, loopVariableRangeDefinitionLookup);
    }

    private static ArrayList<Gate> internalAddAssign(Optional<SignalExpression> optionallyAssignedToSignal, ExpressionResult lhsOperand, ExpressionResult rhsOperand, SignalExpression additionalLines, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> synthesizedGates = new ArrayList<>();
        int expectedOperandsBitwidth = lhsOperand.getWidth(loopVariableRangeDefinitionLookup);

        String inCarryLine = additionalLines.getLines().size() == 1
                ? additionalLines.getLineName(0)
                : additionalLines.getLineName(expectedOperandsBitwidth);

        String[] rhsOperandBackupLines;
        if (optionallyAssignedToSignal.isEmpty()) {
            rhsOperandBackupLines = IntStream.range(0, expectedOperandsBitwidth)
                    .mapToObj(i -> additionalLines.getLineName(i))
                    .toArray(String[]::new);

            for (int i = 0; i < lhsOperand.getWidth(loopVariableRangeDefinitionLookup); ++i){
                Gate backupOfSecondExprBitGate = new Gate(Toffoli);
                backupOfSecondExprBitGate.addControlLine(rhsOperand.getLineName(i));
                backupOfSecondExprBitGate.addTargetLine(rhsOperandBackupLines[i]);
                synthesizedGates.add(backupOfSecondExprBitGate);
            }
        }

        // Define where temporary addition result is stored (in case of binary expression with '+' operation, result is stored in additional lines instead of rhs operand)
        SignalExpression linesStoringAdditionResult = optionallyAssignedToSignal.orElse(rhsOperand.signal);
        // If the original lhs operand and the assigned to signal are the same, simply use the original rhs operand as the lhs one since in our synthesis approach the rhs operand will store the result of the addition.
        ExpressionResult lhsOperandConsideringAssignedToSignal = optionallyAssignedToSignal.isEmpty() ? lhsOperand : rhsOperand;

        ThreeBitMajorityGate[] majorityGates = new ThreeBitMajorityGate[expectedOperandsBitwidth];
        String rippleCarryMajorityGateInCarryLine = inCarryLine;
        for (int i = 0; i < expectedOperandsBitwidth; ++i) {
            majorityGates[i] = new ThreeBitMajorityGate(rippleCarryMajorityGateInCarryLine, lhsOperandConsideringAssignedToSignal.getLineName(i), linesStoringAdditionResult.getLineName(i));
            synthesizedGates.addAll(majorityGates[i].internalGates);
            rippleCarryMajorityGateInCarryLine = majorityGates[i].outCarry;
        }

        int targetedMajorityGate = expectedOperandsBitwidth - 1;
        int currUnmajorityGateCount = 0;
        UnmajorityAndSumGate[] unmajorityAndSumGates = new UnmajorityAndSumGate[expectedOperandsBitwidth];
        unmajorityAndSumGates[currUnmajorityGateCount++] = new UnmajorityAndSumGate(majorityGates[targetedMajorityGate--]);
        synthesizedGates.addAll(unmajorityAndSumGates[0].internalGates);

        for (int i = targetedMajorityGate; i >= 0; --i) {
            unmajorityAndSumGates[currUnmajorityGateCount] = new UnmajorityAndSumGate(majorityGates[i]);
            unmajorityAndSumGates[currUnmajorityGateCount].inNewCarry = unmajorityAndSumGates[currUnmajorityGateCount - 1].outCarry;
            synthesizedGates.addAll(unmajorityAndSumGates[currUnmajorityGateCount].internalGates);
            ++currUnmajorityGateCount;
        }

        if (optionallyAssignedToSignal.isEmpty()) {
            Gate inversionOfInCarryLineGate = new Gate(Toffoli);
            inversionOfInCarryLineGate.addTargetLine(inCarryLine);
            synthesizedGates.add(inversionOfInCarryLineGate);

            for (int i = 0; i < expectedOperandsBitwidth; ++i) {
                Gate secondExprRestoreFromBackupGate = new Gate(Fredkin);
                secondExprRestoreFromBackupGate.addControlLine(inCarryLine);
                // Backup of rhs operand will be built first during synthesize of addition - restore backup of second operand with the help of SWAP gates (swapping lines storing addition result with rhs operand backup lines)
                secondExprRestoreFromBackupGate.addTargetLine(synthesizedGates.get(i).getTargetLines().getFirst());
                secondExprRestoreFromBackupGate.addTargetLine(unmajorityAndSumGates[(expectedOperandsBitwidth - i) - 1].outOperandSumAndCarrySum);
                synthesizedGates.add(secondExprRestoreFromBackupGate);
            }

            Gate restoreOfInCarryValueGate = new Gate(Toffoli);
            restoreOfInCarryValueGate.addTargetLine(inCarryLine);
            synthesizedGates.add(restoreOfInCarryValueGate);
        }
        return synthesizedGates;
    }

    // TODO: Use reverse of addition
    public static ArrayList<Gate> minus(ExpressionResult firstExp, ExpressionResult secondExp, SignalExpression additionalLines, SignalExpression twosComplementLines, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        //The twosComplementLines is null if one of the numbers is a number
        ArrayList<Gate> gates = new ArrayList<>();
        if (firstExp.isNumber || secondExp.isNumber) {
            //both cant be a number because else the result would be handled by the AST
            int number = numberNotRes(firstExp, secondExp);
            firstExp = resNotNumber(firstExp, secondExp);
            if (number == 0) {
                //neutral operation, just copy Exp to lines
                for (int i = 0; i < firstExp.getWidth(loopVariableRangeDefinitionLookup); i++) {
                    Gate tempGate;
                    tempGate = new Gate(Toffoli);
                    tempGate.addTargetLine(additionalLines.getLineName(i));
                    tempGate.addControlLine(firstExp.getLineName(i));
                    gates.add(tempGate);
                }
                return gates;
            }
            if (number < 0) {
                //if we substract a negative number we can just use plus
                ExpressionResult negative = new ExpressionResult(-number);
                return plus(firstExp, negative, additionalLines, loopVariableRangeDefinitionLookup);
            }
            //create twos complement of number
            number = ~number;
            number = (number & (int) Math.pow(2, firstExp.getWidth(loopVariableRangeDefinitionLookup)) - 1); //drop all unneeded ones
            number++;
            return plus(firstExp, secondExp, additionalLines, loopVariableRangeDefinitionLookup);
        } else {
            //second expression is also an expression and not a number
            //do twosComplement on the SignalLine
            ExpressionResult twosComplementRes = new ExpressionResult(twosComplementLines);
            twosComplementRes.gates.addAll(notExp(secondExp, twosComplementRes.signal, loopVariableRangeDefinitionLookup));
            twosComplementRes.gates.addAll(increment(twosComplementRes.signal, loopVariableRangeDefinitionLookup));
            gates.addAll(twosComplementRes.gates);
            gates.addAll(plus(firstExp, twosComplementRes, additionalLines, loopVariableRangeDefinitionLookup));
            return gates;
        }
    }

    public static ArrayList<Gate> bitwiseAnd(ExpressionResult firstExp, ExpressionResult secondExp, SignalExpression additionalLines, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> gates = new ArrayList<>();
        if (firstExp.isNumber || secondExp.isNumber) {
            //both cant be a number because else the result would be handled by the AST
            int number = numberNotRes(firstExp, secondExp);
            firstExp = resNotNumber(firstExp, secondExp);
            if (number == 0) {
                //neutral operation, empty gate list
                return gates;
            }
            if (number < 0) {
                //changes negative number to its positive representation with the given lines
                int range = (int) Math.pow(2, firstExp.getWidth(loopVariableRangeDefinitionLookup));  //so for a 5bit number this would be 32
                number = range + (number % range);    //if the number is bigger than the range we can ignore all other bits
            }
            ArrayList<Boolean> numBool = intToBool(number);
            //we now have firstExp as arbitrary Expression and a BooleanList for the number
            for (int i = 0; i < firstExp.getWidth(loopVariableRangeDefinitionLookup) && i < numBool.size(); i++) {
                Gate tempGate;
                if (numBool.get(i)) {
                    tempGate = new Gate(Toffoli);
                    tempGate.addTargetLine(additionalLines.getLineName(i));
                    tempGate.addControlLine(firstExp.getLineName(i));
                    gates.add(tempGate);
                }
            }
        } else {
            for (int i = 0; i < firstExp.getWidth(loopVariableRangeDefinitionLookup) && i < secondExp.getWidth(loopVariableRangeDefinitionLookup); i++) {
                Gate tempGate = new Gate(Toffoli);
                tempGate.addTargetLine(additionalLines.getLineName(i));
                tempGate.addControlLine(firstExp.getLineName(i));
                tempGate.addControlLine(secondExp.getLineName(i));
                gates.add(tempGate);
            }
        }
        return gates;
    }

    public static ArrayList<Gate> xor(ExpressionResult firstExp, ExpressionResult secondExp, SignalExpression xorLines, Map<String, LoopVariableRangeDefinition> loopVariableRangeDefinitionLookup) {
        ArrayList<Gate> gates = new ArrayList<>();
        if (firstExp.isNumber || secondExp.isNumber) {
            //both cant be a number because else the result would be handled by the AST
            int number = numberNotRes(firstExp, secondExp);
            firstExp = resNotNumber(firstExp, secondExp);
            if (number == 0) {
                //neutral operation, empty gate list
                return gates;
            }
            if (number < 0) {
                //changes negative number to its positive representation with the given lines
                int range = (int) Math.pow(2, firstExp.getWidth(loopVariableRangeDefinitionLookup));  //so for a 5bit number this would be 32
                number = range + (number % range);    //if the number is bigger than the range we can ignore all other bits
            }
            ArrayList<Boolean> numBool = intToBool(number);
            //we now have firstExp as arbitrary Expression and a BooleanList for the number
            for (int i = 0; i < firstExp.getWidth(loopVariableRangeDefinitionLookup) || i < numBool.size(); i++) {
                Gate tempGate;
                if (i < firstExp.getWidth(loopVariableRangeDefinitionLookup)) {
                    tempGate = new Gate(Toffoli);
                    tempGate.addTargetLine(xorLines.getLineName(i));
                    tempGate.addControlLine(firstExp.getLineName(i));
                    gates.add(tempGate);
                }
                if (i < numBool.size() && numBool.get(i)) {
                    tempGate = new Gate(Toffoli);
                    tempGate.addTargetLine(xorLines.getLineName(i));
                    gates.add(tempGate);
                }
            }
        } else {
            for (int i = 0; i < firstExp.getWidth(loopVariableRangeDefinitionLookup) || i < secondExp.getWidth(loopVariableRangeDefinitionLookup); i++) {
                if (i < firstExp.getWidth(loopVariableRangeDefinitionLookup)) {
                    Gate tempGate = new Gate(Toffoli);
                    tempGate.addTargetLine(xorLines.getLineName(i));
                    tempGate.addControlLine(firstExp.getLineName(i));
                    gates.add(tempGate);
                }
                if (i < secondExp.getWidth(loopVariableRangeDefinitionLookup)) {
                    Gate tempGate = new Gate(Toffoli);
                    tempGate.addTargetLine(xorLines.getLineName(i));
                    tempGate.addControlLine(secondExp.getLineName(i));
                    gates.add(tempGate);
                }
            }
        }
        return gates;
    }

    private static ArrayList<Boolean> intToBool(int num) {
        ArrayList<Boolean> booleans = new ArrayList<>();
        for (int i = num; i > 0; i /= 2) {
            booleans.add(i % 2 == 1);
        }
        return booleans;
    }

    private static ExpressionResult resNotNumber(ExpressionResult firstExp, ExpressionResult secondExp) {
        //returns the first ExpressionResult that is not a number
        if (!firstExp.isNumber) return firstExp;
        if (!secondExp.isNumber) return secondExp;
        return null;
    }

    private static Integer numberNotRes(ExpressionResult firstExp, ExpressionResult secondExp) {
        //returns the first ExpressionResult that is a number as Integer
        if (firstExp.isNumber) return firstExp.number;
        if (secondExp.isNumber) return secondExp.number;
        return null;
    }

    public static ArrayList<Gate> reverseGates(ArrayList<Gate> gates) {
        ArrayList<Gate> reverse = new ArrayList<>();
        for (Gate gate : gates) {
            Gate tempGate = new Gate(gate.kind);
            tempGate.addTargetLines(gate.getTargetLines());
            tempGate.addControlLines(gate.getControlLines());
            reverse.add(tempGate);
        }
        Collections.reverse(reverse);
        return reverse;
    }

    private static void validateGateDefinition(Gate gate) throws KeyException {
        Set<String> controlLines = new HashSet<>();
        Set<String> targetLines = new HashSet<>();
        if (controlLines.isEmpty())
            return;

        for (var controlLine : gate.getControlLines()){
            if (!controlLines.add(controlLine))
                throw new KeyAlreadyExistsException("Gate defined duplicate control line " + controlLine);
        }

        for (var targetLine : gate.getTargetLines()){
            if (!targetLines.add(targetLine))
                throw new KeyAlreadyExistsException("Gate defined duplicate target line " + targetLine);

            if (controlLines.contains(targetLine))
                throw new KeyException("Gate used line " + targetLine + " as both control and target line");
        }
    }
}
