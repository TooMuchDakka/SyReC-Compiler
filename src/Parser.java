

import SymTable.SymTable;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
    import SymTable.Obj;
    import SymTable.Mod;
    import CodeGen.Code;
    import CodeGen.ExpressionResult;
    import AbstractSyntaxTree.*;
    import java.util.ArrayList;
    import java.util.HashMap;



public class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _int = 2;
	public static final int _to = 3;
	public static final int _shiftR = 4;
	public static final int _shiftL = 5;
	public static final int maxT = 54;
	public static final int _activateLine = 55;
	public static final int _deactivateLine = 56;
	public static final int _activateCost = 57;
	public static final int _deactivateCost = 58;

	static final boolean _T = true;
	static final boolean _x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;
	private String exportLocation;
	private String inputFileName;
	private Path builtExportResultPath;

	private boolean IsIdentEql(){
        scanner.ResetPeek();
        return (la.val.equals("$") && scanner.Peek().kind == _ident && scanner.Peek().val.equals("="));
    }

    private boolean NumberTo(){
        scanner.ResetPeek();
        Token next = scanner.Peek();
        if(la.kind == _int) {
            return next.kind == _to;
        }
        if(la.val.equals("#") && next.kind == _ident) {
            next = scanner.Peek();
            return next.kind == _to;
        }
        if(la.val.equals("$") && next.kind == _ident) {
                    next = scanner.Peek();
                    return next.kind == _to;
        }
        if(la.val.equals("(")) {
            int i = 1;
            while (i > 0) {
                if(next.val.equals("(")) {
                    i++;
                }
                else if(next.val.equals(")")){
                    i--;
                }
                else if(next.val.equals(";") || next.val.equals("")) {
                    //missing closing bracket
                    return false;
                }
                next = scanner.Peek();
            }
            return next.kind == _to;
        }
        return false;
    }

    private boolean IsShift(){
        scanner.ResetPeek();
        Token next = scanner.Peek();
        if(!la.val.equals("(")) {
            return false;
        }
            int i = 1;
            while (i > 0) {
                if(next.val.equals("(")) {
                    i++;
                }
                else if(next.val.equals(")")){
                    i--;
                }
                else if(next.val.equals(";") || next.val.equals("")) {
                    //missing closing bracket
                    return false;
                }
                else if(i == 1 && (next.kind == _shiftL || next.kind == _shiftR)){
                    return true;
                }
                next = scanner.Peek();
            }
            return false;
    }
    private static final Set<String> BinExp = Set.of("+", "-", "^", "*", "/", "%", "*>", "&&", "||", "&", "|", "<", ">", "=", "!=", "<=", ">=");
        boolean IsBinary(){
            scanner.ResetPeek();
            Token next = scanner.Peek();
            if(!la.val.equals("(")) {
                return false;
            }
                int i = 1;
                while (i > 0) {
                    if(next.val.equals("(")) {
                        i++;
                    }
                    else if(next.val.equals(")")){
                        i--;
                    }
                    else if(next.val.equals(";") || next.val.equals("")) {
                        //missing closing bracket
                        return false;
                    }
                    else if(i == 1 && BinExp.contains(next.val)){
                        return true;
                    }
                    next = scanner.Peek();
                }
                return false;
        }
        SymTable tab = new SymTable();
        Mod curMod;

        private HashMap<String, CodeMod> finishedModules = new HashMap<>();


        private void Warning (String msg) { //add Warning as function to not need to specify line and col
        		errors.Warning(t.line, t.col, msg);
        	}

        private boolean lineAware = true; //to deactivate line Aware synthesis to save lines
        private boolean costAware = true; //to deactivate cost Aware synthesis to save gates
// If you want your generated compiler case insensitive add the
// keyword IGNORECASE here.



	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			if (la.kind == 55) {
				lineAware = true;
			}
			if (la.kind == 56) {
				lineAware = false;
			}
			if (la.kind == 57) {
				costAware = true;
			}
			if (la.kind == 58) {
				costAware = false;
			}
			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}
	
	NumberExpression  number() {
		NumberExpression  number;
		number = new NumberExpression(0);
		if (la.kind == 2) {
			Get();
			number = new NumberExpression(Integer.parseInt(t.val));
		} else if (la.kind == 6) {
			Get();
			Expect(1);
			if(curMod.getLocal(t.val) == null) {
			 SemErr(t.val + "is not defined");
			 number = new NumberExpression(0);
			}
			else {
			 number = new NumberExpression(t.val, NumberExpression.Kind.BITWIDTH);
			}
		} else if (la.kind == 7) {
			Get();
			Expect(1);
			if(!curMod.loopVarDefined(t.val)) {
			 SemErr(t.val + " is not defined");
			 number = new NumberExpression(0);
			}
			else {
			 number = new NumberExpression(t.val, NumberExpression.Kind.LOOPVAR);
			}
		} else if (la.kind == 8) {
			Get();
			NumberExpression firstNumber = number();
			if (la.kind == 9) {
				Get();
			} else if (la.kind == 10) {
				Get();
			} else if (la.kind == 11) {
				Get();
			} else if (la.kind == 12) {
				Get();
			} else SynErr(55);
			char calcToggle = t.val.charAt(0);
			NumberExpression secondNumber = number();
			switch(calcToggle) {
			case '+':
			 number = new NumberExpression(firstNumber, secondNumber, NumberExpression.Kind.PLUS);
			 break;
			case '-':
			 number = new NumberExpression(firstNumber, secondNumber, NumberExpression.Kind.MINUS);
			 break;
			case '*':
			 number = new NumberExpression(firstNumber, secondNumber, NumberExpression.Kind.TIMES);
			 break;
			case '/':
			 number = new NumberExpression(firstNumber, secondNumber, NumberExpression.Kind.DIVIDE);
			 break;
			default:
			 number = new NumberExpression(0);
			 break;
			}
			Expect(13);
		} else SynErr(56);
		return number;
	}

	void SyReC() {
		Module();
		while (la.kind == 14) {
			Module();
		}
	}

	void Module() {
		Expect(14);
		Expect(1);
		if(!tab.addModule(t.val)) {
		   SemErr("Module "+t.val+" is already defined");
		}
		curMod = tab.getModule(t.val);
		                       
		Expect(8);
		ParameterList();
		Expect(13);
		while (la.kind == 19 || la.kind == 20) {
			SignalList();
		}
		CodeMod codeModule = Code.createModule(curMod);
		finishedModules.put(curMod.name, codeModule);

		ArrayList<Statement> statements = StatementList();
		codeModule.addStatements(statements);

		if (this.builtExportResultPath != null)
			Code.endModule(this.builtExportResultPath, curMod, codeModule);
	}

	void ParameterList() {
		Parameter();
		while (la.kind == 15) {
			Get();
			Parameter();
		}
	}

	void SignalList() {
		Obj.Kind kind = null;
		if (la.kind == 19) {
			Get();
			kind = Obj.Kind.Wire;
		} else if (la.kind == 20) {
			Get();
			kind = Obj.Kind.State;
		} else SynErr(57);
		SignalDeclaration(kind);
		while (la.kind == 15) {
			Get();
			SignalDeclaration(kind);
		}
	}

	ArrayList<Statement>  StatementList() {
		ArrayList<Statement>  statements;
		Statement first = Statement();
		statements = new ArrayList<Statement>();
		statements.add(first);
		while (la.kind == 23) {
			Get();
			Statement additional = Statement();
			statements.add(additional);
		}
		return statements;
	}

	void Parameter() {
		Obj.Kind kind = null;
		if (la.kind == 16) {
			Get();
			kind = Obj.Kind.In;
		} else if (la.kind == 17) {
			Get();
			kind = Obj.Kind.Out;
		} else if (la.kind == 18) {
			Get();
			kind = Obj.Kind.Inout;
		} else SynErr(58);
		SignalDeclaration(kind);
	}

	void SignalDeclaration(Obj.Kind kind) {
		Expect(1);
		String ident = t.val;
		int width = 1;
		while (la.kind == 21) {
			Get();
			Expect(2);
			Expect(22);
		}
		if (la.kind == 8) {
			Get();
			Expect(2);
			width = Integer.parseInt(t.val);
			Expect(13);
		}
		curMod.addObj(new Obj(kind, ident, width));
	}

	Statement  Statement() {
		Statement  statement;
		statement = new SkipStatement(true);
		switch (la.kind) {
		case 24: case 25: {
			Statement callStat = CallStatement();
			statement = callStat;
			break;
		}
		case 26: {
			Statement forStat = ForStatement();
			statement = forStat;
			break;
		}
		case 30: {
			Statement ifStat = IfStatement();
			statement = ifStat;
			break;
		}
		case 35: case 36: case 37: {
			Statement unaryStat = UnaryStatement();
			statement = unaryStat;
			break;
		}
		case 39: {
			Statement skipStat = SkipStatement();
			statement = skipStat;
			break;
		}
		case 1: {
			SignalExpression firstSig = Signal();
			if (la.kind == 38) {
				Statement swapStat = SwapStatement(firstSig);
				statement = swapStat;
			} else if (la.kind == 9 || la.kind == 10 || la.kind == 34) {
				Statement assignStat = AssignStatement(firstSig);
				statement = assignStat;
			} else SynErr(59);
			break;
		}
		default: SynErr(60); break;
		}
		return statement;
	}

	Statement  CallStatement() {
		Statement  call;
		call = new SkipStatement(true);
		CallStatement.Kind kind = CallStatement.Kind.CALL;
		if (la.kind == 24) {
			Get();
		} else if (la.kind == 25) {
			Get();
			kind = CallStatement.Kind.UNCALL;
		} else SynErr(61);
		Expect(1);
		Mod calledMod = tab.getModule(t.val);
		if(calledMod == null) {
		 Warning("Module "+t.val+"was not defined before this point");
		}
		
		Expect(8);
		Expect(1);
		int parCount = 1;
		ArrayList<String> idents = new ArrayList<>();
		idents.add(t.val);
		while (la.kind == 15) {
			Get();
			Expect(1);
			parCount++;
			idents.add(t.val);
		}
		Expect(13);
		if(calledMod != null && parCount != calledMod.getSignalCount()) {
		 SemErr("Module "+calledMod.name+"needs "+calledMod.getSignalCount()+" signals");
		} else if(calledMod != null) {
		 ArrayList<Obj> calledLocals = calledMod.getSignals();
		 boolean noError = true;
		 for(int i = 0; i < calledLocals.size(); i++) {
		     Obj calledLocal = calledLocals.get(i);
		     Obj usedLine = curMod.getLocal(idents.get(i));
		     if(calledLocal.width != usedLine.width) {
		         SemErr("Original Line "+calledLocal.name+" has a width of "+calledLocal.width+" and usedLine "+usedLine.name+" has a width of "+usedLine.width);
		         noError = false;
		     }
		 }
		 if(noError) {
		     ArrayList<Statement> statements = finishedModules.get(calledMod.name).getStatements();
		     call = new CallStatement(calledMod,finishedModules.get(curMod.name), curMod, idents, statements, kind, lineAware);
		 }
		}
		
		return call;
	}

	Statement  ForStatement() {
		Statement  forStatement;
		String ident = null;
		NumberExpression from = new NumberExpression(0);
		NumberExpression to = new NumberExpression(0);
		NumberExpression step = new NumberExpression(1);
		Expect(26);
		if (IsIdentEql() || NumberTo()) {
			if (IsIdentEql()) {
				Expect(7);
				Expect(1);
				ident = t.val;
				curMod.addLoopVar(ident);
				Expect(27);
			}
			NumberExpression start = number();
			from = start;
			Expect(3);
		}
		NumberExpression stop = number();
		to = stop;
		if (la.kind == 28) {
			Get();
			if (la.kind == 10) {
				Get();
			}
			NumberExpression stepSize = number();
			step = stepSize;
		}
		ArrayList<Statement> statements = StatementList();
		forStatement = new ForStatement(ident, from, to, step, statements, lineAware);
		curMod.removeLoopVar(ident);
		Expect(29);
		return forStatement;
	}

	Statement  IfStatement() {
		Statement  ifStatement;
		Expect(30);
		Expression ifExp = Expression();
		Expect(31);
		ArrayList<Statement> thenStatements = StatementList();
		Expect(32);
		ArrayList<Statement> elseStatements = StatementList();
		Expect(33);
		Expression fiExp = Expression();
		ifStatement = new IfStatement(ifExp, thenStatements, elseStatements, fiExp, lineAware);
		return ifStatement;
	}

	Statement  UnaryStatement() {
		Statement  unary;
		if (la.kind == 35) {
			Get();
		} else if (la.kind == 36) {
			Get();
		} else if (la.kind == 37) {
			Get();
		} else SynErr(62);
		String calcToggle = t.val;
		Expect(27);
		SignalExpression sig = Signal();
		UnaryStatement.Kind kind;
		switch(calcToggle) {
		 case "~":
		     kind = UnaryStatement.Kind.NEGATE;
		     break;
		 case "++":
		     kind = UnaryStatement.Kind.INCREMENT;
		     break;
		 default:
		     kind = UnaryStatement.Kind.DECREMENT;
		     break;
		}
		unary = new UnaryStatement(sig, kind, lineAware);
		return unary;
	}

	Statement  SkipStatement() {
		Statement  skip;
		Expect(39);
		skip = new SkipStatement(true);
		return skip;
	}

	SignalExpression  Signal() {
		SignalExpression  sig;
		Expect(1);
		String ident = t.val;
		if(!curMod.isDefined(ident)) {
		 SemErr("Signal "+ident+" is not defined");
		}
		//TODO error prevention for udnefined signals
		Obj curSignal = curMod.getLocal(ident);
		NumberExpression startWidth = null;
		NumberExpression endWidth = null;
		
		while (la.kind == 21) {
			Get();
			Expression exp = Expression();
			Expect(22);
		}
		if (la.kind == 40) {
			Get();
			NumberExpression lowerBound = number();
			startWidth = lowerBound;
			endWidth = lowerBound; //so far both are equal
			
			if (la.kind == 41) {
				Get();
				NumberExpression upperBound = number();
				endWidth = upperBound;
			}
		}
		if(startWidth == null) {
		 startWidth = new NumberExpression(0);
		 endWidth = new NumberExpression(curSignal.width -1);
		}
		sig = new SignalExpression (curSignal, startWidth, endWidth);
		
		return sig;
	}

	Statement  SwapStatement(SignalExpression firstSig) {
		Statement  swap;
		Expect(38);
		SignalExpression secondSig = Signal();
		if(false/*firstSig.getWidth() != secondSig.getWidth()*/ ){
		SemErr("Signal Width is not equal");
		swap =  new SkipStatement(true);
		}
		else {
		swap = new SwapStatement(firstSig, secondSig, lineAware);
		}
		return swap;
	}

	Statement  AssignStatement(SignalExpression firstSignal) {
		Statement  assign;
		if (la.kind == 34) {
			Get();
		} else if (la.kind == 9) {
			Get();
		} else if (la.kind == 10) {
			Get();
		} else SynErr(63);
		String assignToggle = t.val;
		Expect(27);
		Expression exp = Expression();
		boolean cantAssign = false;
		for(String line : firstSignal.getLines()) {
		 if(exp.containsSignal(line)) {
		    cantAssign = true;
			break;
		 }
		}
		if(cantAssign) {
		 SemErr("Signal is contained in the Expression of the assign Statement");
		 assign = new SkipStatement(true);
		}
		//TODO check if you can fit smaller numbers into signal
		else if(firstSignal.getWidth() < exp.getWidth()) {
		 SemErr("Expression doesnt fit into the Signal Width");
		 assign = new SkipStatement(true);
		}
		else {
		 AssignStatement.Kind kind;
		 switch(assignToggle) {
		     case "^":
		         kind = AssignStatement.Kind.XOR;
		         break;
		     case "+":
		         kind = AssignStatement.Kind.PLUS;
		         break;
		     default:
		         kind = AssignStatement.Kind.MINUS;
		         break;
		 }
		 assign = new AssignStatement(firstSignal, exp, kind, lineAware);
		}
		
		return assign;
	}

	Expression  Expression() {
		Expression  exp;
		exp = new NumberExpression(0);
		if (la.kind == 1) {
			SignalExpression sig = Signal();
			exp = sig;
		} else if (IsShift()) {
			Expression shiftExp = ShiftExpression();
			exp = shiftExp;
		} else if (IsBinary()) {
			Expression binExp = BinaryExpression();
			exp = binExp;
		} else if (la.kind == 35 || la.kind == 53) {
			Expression unExp = UnaryExpression();
			exp = unExp;
		} else if (StartOf(1)) {
			Expression numExp = number();
			exp = numExp;
		} else SynErr(64);
		return exp;
	}

	Expression  ShiftExpression() {
		Expression  shiftExp;
		Expect(8);
		Expression exp = Expression();
		ShiftExpression.Kind kind = ShiftExpression.Kind.RIGHT;
		if (la.kind == 5) {
			Get();
			kind = ShiftExpression.Kind.LEFT;
		} else if (la.kind == 4) {
			Get();
		} else SynErr(65);
		NumberExpression number = number();
		shiftExp = new ShiftExpression(exp, number, kind);
		Expect(13);
		return shiftExp;
	}

	Expression  BinaryExpression() {
		Expression  binExp;
		Expect(8);
		Expression firstExp = Expression();
		switch (la.kind) {
		case 9: {
			Get();
			break;
		}
		case 10: {
			Get();
			break;
		}
		case 34: {
			Get();
			break;
		}
		case 11: {
			Get();
			break;
		}
		case 12: {
			Get();
			break;
		}
		case 42: {
			Get();
			break;
		}
		case 43: {
			Get();
			break;
		}
		case 44: {
			Get();
			break;
		}
		case 45: {
			Get();
			break;
		}
		case 46: {
			Get();
			break;
		}
		case 47: {
			Get();
			break;
		}
		case 48: {
			Get();
			break;
		}
		case 49: {
			Get();
			break;
		}
		case 27: {
			Get();
			break;
		}
		case 50: {
			Get();
			break;
		}
		case 51: {
			Get();
			break;
		}
		case 52: {
			Get();
			break;
		}
		default: SynErr(66); break;
		}
		String operation = t.val;
		Expression secondExp = Expression();
		Expect(13);
		BinaryExpression.Kind kind = BinaryExpression.Kind.PLUS;
		switch(operation) {
		case "+":
		 kind = BinaryExpression.Kind.PLUS;
		 break;
		case "-":
		 kind = BinaryExpression.Kind.MINUS;
		 break;
		case "^":
		 kind = BinaryExpression.Kind.BIT_XOR;
		 break;
		case "*":
		 kind = BinaryExpression.Kind.TIMES_UPPER;
		 break;
		case "/":
		 kind = BinaryExpression.Kind.DIVIDE;
		 break;
		case "%":
		 kind = BinaryExpression.Kind.REMAINDER;
		 break;
		case "*>":
		 kind = BinaryExpression.Kind.TIMES_LOWER;
		 break;
		case "&&":
		 kind = BinaryExpression.Kind.LOG_AND;
		 break;
		case "||":
		 kind = BinaryExpression.Kind.LOG_OR;
		 break;
		case "&":
		 kind = BinaryExpression.Kind.BIT_AND;
		 break;
		case "|":
		 kind = BinaryExpression.Kind.BIT_OR;
		 break;
		case "<":
		 kind = BinaryExpression.Kind.LESSER;
		 break;
		case ">":
		 kind = BinaryExpression.Kind.GREATER;
		 break;
		case "=":
		 kind = BinaryExpression.Kind.EQL;
		 break;
		case "!=":
		 kind = BinaryExpression.Kind.NEQL;
		 break;
		case "<)":
		 kind = BinaryExpression.Kind.LEQL;
		 break;
		case ">=":
		 kind = BinaryExpression.Kind.GEQL;
		 break;
		}
		binExp = new BinaryExpression(firstExp, secondExp, kind);
		
		return binExp;
	}

	Expression  UnaryExpression() {
		Expression  unExp;
		UnaryExpression.Kind kind = UnaryExpression.Kind.LOGICAL;
		if (la.kind == 53) {
			Get();
		} else if (la.kind == 35) {
			Get();
			kind = UnaryExpression.Kind.BITWISE;
		} else SynErr(67);
		Expression exp = Expression();
		if(kind == UnaryExpression.Kind.LOGICAL) {
		 if(exp.getWidth() != 1) {
		 //TODO width of BITWIDTH cant be dynamically checked during parse
		     SemErr("Logical Not on a Busline or an Expression that is not a boolean");
		     unExp = new NumberExpression(0);
		 }
		 else {
		     unExp = new UnaryExpression(exp, kind);
		 }
		}
		else {
		 unExp = new UnaryExpression(exp, kind);
		}
		return unExp;
	}



	public void Parse(String inputFileName, String exportLocation) {
		if (inputFileName == null || inputFileName.isEmpty())
			return;

		this.inputFileName = inputFileName;
		this.exportLocation = exportLocation;
		this.builtExportResultPath = null;

		if (this.exportLocation != null && !this.exportLocation.isEmpty()) {
			Path inputFilePath = Path.of(inputFileName);
			inputFileName = inputFilePath.getFileName().toString();

			int inputFileNameExtensionIndex = inputFileName.lastIndexOf('.');
			if (inputFileNameExtensionIndex != -1) {
				inputFileName = inputFileName.substring(0, inputFileNameExtensionIndex);
			}
			this.builtExportResultPath = Path.of(exportLocation, inputFileName + ".real");
		}

		la = new Token();
		la.val = "";		
		Get();
		SyReC();
		Expect(0);

	}

	private static final boolean[][] set = {
		{_T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_x,_T,_x, _x,_x,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x}

	};
} // end Parser


class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "ident expected"; break;
			case 2: s = "int expected"; break;
			case 3: s = "to expected"; break;
			case 4: s = "shiftR expected"; break;
			case 5: s = "shiftL expected"; break;
			case 6: s = "\"#\" expected"; break;
			case 7: s = "\"$\" expected"; break;
			case 8: s = "\"(\" expected"; break;
			case 9: s = "\"+\" expected"; break;
			case 10: s = "\"-\" expected"; break;
			case 11: s = "\"*\" expected"; break;
			case 12: s = "\"/\" expected"; break;
			case 13: s = "\")\" expected"; break;
			case 14: s = "\"module\" expected"; break;
			case 15: s = "\",\" expected"; break;
			case 16: s = "\"in\" expected"; break;
			case 17: s = "\"out\" expected"; break;
			case 18: s = "\"inout\" expected"; break;
			case 19: s = "\"wire\" expected"; break;
			case 20: s = "\"state\" expected"; break;
			case 21: s = "\"[\" expected"; break;
			case 22: s = "\"]\" expected"; break;
			case 23: s = "\";\" expected"; break;
			case 24: s = "\"call\" expected"; break;
			case 25: s = "\"uncall\" expected"; break;
			case 26: s = "\"for\" expected"; break;
			case 27: s = "\"=\" expected"; break;
			case 28: s = "\"step\" expected"; break;
			case 29: s = "\"rof\" expected"; break;
			case 30: s = "\"if\" expected"; break;
			case 31: s = "\"then\" expected"; break;
			case 32: s = "\"else\" expected"; break;
			case 33: s = "\"fi\" expected"; break;
			case 34: s = "\"^\" expected"; break;
			case 35: s = "\"~\" expected"; break;
			case 36: s = "\"++\" expected"; break;
			case 37: s = "\"--\" expected"; break;
			case 38: s = "\"<=>\" expected"; break;
			case 39: s = "\"skip\" expected"; break;
			case 40: s = "\".\" expected"; break;
			case 41: s = "\":\" expected"; break;
			case 42: s = "\"%\" expected"; break;
			case 43: s = "\"*>\" expected"; break;
			case 44: s = "\"&&\" expected"; break;
			case 45: s = "\"||\" expected"; break;
			case 46: s = "\"&\" expected"; break;
			case 47: s = "\"|\" expected"; break;
			case 48: s = "\"<\" expected"; break;
			case 49: s = "\">\" expected"; break;
			case 50: s = "\"!=\" expected"; break;
			case 51: s = "\"<=\" expected"; break;
			case 52: s = "\">=\" expected"; break;
			case 53: s = "\"!\" expected"; break;
			case 54: s = "??? expected"; break;
			case 55: s = "invalid number"; break;
			case 56: s = "invalid number"; break;
			case 57: s = "invalid SignalList"; break;
			case 58: s = "invalid Parameter"; break;
			case 59: s = "invalid Statement"; break;
			case 60: s = "invalid Statement"; break;
			case 61: s = "invalid CallStatement"; break;
			case 62: s = "invalid UnaryStatement"; break;
			case 63: s = "invalid AssignStatement"; break;
			case 64: s = "invalid Expression"; break;
			case 65: s = "invalid ShiftExpression"; break;
			case 66: s = "invalid BinaryExpression"; break;
			case 67: s = "invalid UnaryExpression"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
