

import SymTable.SymTable;
    import java.util.Set;
    import SymTable.Obj;
    import SymTable.Mod;
    import CodeGen.Code;



public class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _int = 2;
	public static final int _to = 3;
	public static final int _shiftR = 4;
	public static final int _shiftL = 5;
	public static final int _plus = 6;
	public static final int _minus = 7;
	public static final int _xor = 8;
	public static final int _lmul = 9;
	public static final int _divide = 10;
	public static final int _rem = 11;
	public static final int _hmul = 12;
	public static final int _bitAND = 13;
	public static final int _bitOR = 14;
	public static final int _AND = 15;
	public static final int _OR = 16;
	public static final int _less = 17;
	public static final int _greater = 18;
	public static final int _eql = 19;
	public static final int _neql = 20;
	public static final int _leql = 21;
	public static final int _geql = 22;
	public static final int maxT = 55;

	static final boolean _T = true;
	static final boolean _x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;

	private boolean IsIdentEql(){
        scanner.ResetPeek();
        return (la.kind == '$' && scanner.Peek().kind == _ident && scanner.Peek().kind == '=');
    }

    private boolean NumberTo(){
        scanner.ResetPeek();
        Token x = la;
        Token next = scanner.Peek();
        if(la.kind == _int) {
            return next.kind == _to;
        }
        if(la.kind == '#' && next.kind == _ident) {
            next = scanner.Peek();
            return next.kind == _to;
        }
        if(la.kind == '$' && next.kind == _ident) {
                    next = scanner.Peek();
                    return next.kind == _to;
        }
        if(la.kind == '(') {
            int i = 1;
            while (i > 0) {
                if(next.kind == '(') {
                    i++;
                }
                else if(next.kind == ')'){
                    i--;
                }
                next = scanner.Peek();
            }
            return next.kind == _to;
        }
        return false;
    }

    private boolean IsShift(){
        scanner.ResetPeek();
        Token x = la;
        Token next = scanner.Peek();
        if(la.kind != '(') {
            return false;
        }
            int i = 1;
            while (i > 0) {
                if(next.kind == '(') {
                    i++;
                }
                else if(next.kind == ')'){
                    i--;
                }
                else if(i == 1 && (next.kind == _shiftL || next.kind == _shiftR)){
                    return true;
                }
                next = scanner.Peek();
            }
            return false;
    }
    private static final Set<Integer> BinExp = Set.of(_plus, _minus, _xor, _lmul, _divide, _rem, _hmul, _bitAND, _bitOR, _AND, _OR, _less, _greater, _eql, _neql, _leql, _geql);
        boolean IsBinary(){
            scanner.ResetPeek();
            Token x = la;
            Token next = scanner.Peek();
            if(la.kind != '(') {
                return false;
            }
                int i = 1;
                while (i > 0) {
                    if(next.kind == '(') {
                        i++;
                    }
                    else if(next.kind == ')'){
                        i--;
                    }
                    else if(i == 1 && BinExp.contains(next.kind)){
                        return true;
                    }
                    next = scanner.Peek();
                }
                return false;
        }
        SymTable tab = new SymTable();
        Mod curMod;

        private String fileName = null;
        public void setName(String name){
            fileName = name;
        }
        Code codegen = new Code(fileName);


        private void Warning (String msg) { //add Warning as function to not need to specify line and col
        		errors.Warning(t.line, t.col, msg);
        	}
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
	
	void number() {
		if (la.kind == 2) {
			Get();
		} else if (la.kind == 23) {
			Get();
			Expect(1);
		} else if (la.kind == 24) {
			Get();
			Expect(1);
		} else if (la.kind == 25) {
			Get();
			number();
			if (la.kind == 6) {
				Get();
			} else if (la.kind == 7) {
				Get();
			} else if (la.kind == 9) {
				Get();
			} else if (la.kind == 10) {
				Get();
			} else SynErr(56);
			number();
			Expect(26);
		} else SynErr(57);
	}

	void SyReC() {
		Module();
		while (la.kind == 27) {
			Module();
		}
	}

	void Module() {
		Expect(27);
		Expect(1);
		if(!tab.addModule(t.val)) {
		   SemErr("Module "+t.val+" is already defined");
		}
		curMod = tab.getModule(t.val);
		
		Expect(25);
		ParameterList();
		Expect(26);
		SignalList();
		StatementList();
	}

	void ParameterList() {
		Parameter();
		while (la.kind == 28) {
			Get();
			Parameter();
		}
	}

	void SignalList() {
		Obj.Kind kind = null;
		if (la.kind == 32) {
			Get();
			kind = Obj.Kind.Wire;
		} else if (la.kind == 33) {
			Get();
			kind = Obj.Kind.State;
		} else SynErr(58);
		SignalDeclaration(kind);
		while (la.kind == 28) {
			Get();
			SignalDeclaration(kind);
		}
	}

	void StatementList() {
		Statement();
		while (la.kind == 36) {
			Get();
			Statement();
		}
	}

	void Parameter() {
		Obj.Kind kind = null;
		if (la.kind == 29) {
			Get();
			kind = Obj.Kind.In;
		} else if (la.kind == 30) {
			Get();
			kind = Obj.Kind.Out;
		} else if (la.kind == 31) {
			Get();
			kind = Obj.Kind.Inout;
		} else SynErr(59);
		SignalDeclaration(kind);
	}

	void SignalDeclaration(Obj.Kind kind) {
		Expect(1);
		curMod.addObj(new Obj(kind, t.val));
		while (la.kind == 34) {
			Get();
			Expect(2);
			Expect(35);
		}
		if (la.kind == 25) {
			Get();
			Expect(2);
			Expect(26);
		}
	}

	void Statement() {
		switch (la.kind) {
		case 37: case 38: {
			CallStatement();
			break;
		}
		case 39: {
			ForStatement();
			break;
		}
		case 42: {
			IfStatement();
			break;
		}
		case 46: case 47: case 48: {
			UnaryStatement();
			break;
		}
		case 50: {
			SkipStatement();
			break;
		}
		case 1: {
			Signal();
			if (la.kind == 49) {
				SwapStatement();
			} else if (la.kind == 6 || la.kind == 7 || la.kind == 8) {
				AssignStatement();
			} else SynErr(60);
			break;
		}
		default: SynErr(61); break;
		}
	}

	void CallStatement() {
		if (la.kind == 37) {
			Get();
		} else if (la.kind == 38) {
			Get();
		} else SynErr(62);
		Expect(1);
		Mod calledMod = tab.getModule(t.val);
		if(calledMod == null) {
		 Warning("Module "+t.val+"was not defined before this point");
		}
		
		Expect(25);
		Expect(1);
		int parCount = 1;
		while (la.kind == 28) {
			Get();
			Expect(1);
			parCount++;
		}
		Expect(26);
		if(calledMod != null && parCount != calledMod.getParameterCount()) {
		 SemErr("Module "+calledMod.name+"needs a different amount of parameters");
		}
	}

	void ForStatement() {
		Expect(39);
		if (IsIdentEql() || NumberTo()) {
			if (IsIdentEql()) {
				Expect(24);
				Expect(1);
				Expect(19);
			}
			number();
			Expect(3);
		}
		number();
		if (la.kind == 40) {
			Get();
			if (la.kind == 7) {
				Get();
			}
			number();
		}
		StatementList();
		Expect(41);
	}

	void IfStatement() {
		Expect(42);
		Expression();
		Expect(43);
		StatementList();
		Expect(44);
		StatementList();
		Expect(45);
		Expression();
	}

	void UnaryStatement() {
		if (la.kind == 46) {
			Get();
		} else if (la.kind == 47) {
			Get();
		} else if (la.kind == 48) {
			Get();
		} else SynErr(63);
		Expect(19);
		Signal();
	}

	void SkipStatement() {
		Expect(50);
	}

	void Signal() {
		Expect(1);
		if(!curMod.isDefined(t.val)) {
		 SemErr("Signal "+t.val+" is not defined");
		}
		while (la.kind == 34) {
			Get();
			Expression();
			Expect(35);
		}
		if (la.kind == 51) {
			Get();
			number();
			if (la.kind == 52) {
				Get();
				number();
			}
		}
	}

	void SwapStatement() {
		Expect(49);
		Signal();
	}

	void AssignStatement() {
		if (la.kind == 8) {
			Get();
		} else if (la.kind == 6) {
			Get();
		} else if (la.kind == 7) {
			Get();
		} else SynErr(64);
		Expect(19);
		Signal();
	}

	void Expression() {
		if (la.kind == 1) {
			Signal();
		} else if (IsShift()) {
			ShiftExpression();
		} else if (IsBinary()) {
			BinaryExpression();
		} else if (la.kind == 46 || la.kind == 54) {
			UnaryExpression();
		} else if (StartOf(1)) {
			number();
		} else SynErr(65);
	}

	void ShiftExpression() {
		Expect(25);
		Expression();
		if (la.kind == 5) {
			Get();
		} else if (la.kind == 4) {
			Get();
		} else SynErr(66);
		number();
		Expect(26);
	}

	void BinaryExpression() {
		Expect(25);
		Expression();
		switch (la.kind) {
		case 6: {
			Get();
			break;
		}
		case 7: {
			Get();
			break;
		}
		case 8: {
			Get();
			break;
		}
		case 9: {
			Get();
			break;
		}
		case 10: {
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
		case 53: {
			Get();
			break;
		}
		case 14: {
			Get();
			break;
		}
		case 15: {
			Get();
			break;
		}
		case 16: {
			Get();
			break;
		}
		case 17: {
			Get();
			break;
		}
		case 18: {
			Get();
			break;
		}
		case 19: {
			Get();
			break;
		}
		case 20: {
			Get();
			break;
		}
		case 21: {
			Get();
			break;
		}
		case 22: {
			Get();
			break;
		}
		default: SynErr(67); break;
		}
		Expression();
		Expect(26);
	}

	void UnaryExpression() {
		if (la.kind == 54) {
			Get();
		} else if (la.kind == 46) {
			Get();
		} else SynErr(68);
		Expression();
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		SyReC();
		Expect(0);

	}

	private static final boolean[][] set = {
		{_T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x},
		{_x,_x,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_T, _T,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x}

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
			case 6: s = "plus expected"; break;
			case 7: s = "minus expected"; break;
			case 8: s = "xor expected"; break;
			case 9: s = "lmul expected"; break;
			case 10: s = "divide expected"; break;
			case 11: s = "rem expected"; break;
			case 12: s = "hmul expected"; break;
			case 13: s = "bitAND expected"; break;
			case 14: s = "bitOR expected"; break;
			case 15: s = "AND expected"; break;
			case 16: s = "OR expected"; break;
			case 17: s = "less expected"; break;
			case 18: s = "greater expected"; break;
			case 19: s = "eql expected"; break;
			case 20: s = "neql expected"; break;
			case 21: s = "leql expected"; break;
			case 22: s = "geql expected"; break;
			case 23: s = "\"#\" expected"; break;
			case 24: s = "\"$\" expected"; break;
			case 25: s = "\"(\" expected"; break;
			case 26: s = "\")\" expected"; break;
			case 27: s = "\"module\" expected"; break;
			case 28: s = "\",\" expected"; break;
			case 29: s = "\"in\" expected"; break;
			case 30: s = "\"out\" expected"; break;
			case 31: s = "\"inout\" expected"; break;
			case 32: s = "\"wire\" expected"; break;
			case 33: s = "\"state\" expected"; break;
			case 34: s = "\"[\" expected"; break;
			case 35: s = "\"]\" expected"; break;
			case 36: s = "\";\" expected"; break;
			case 37: s = "\"call\" expected"; break;
			case 38: s = "\"uncall\" expected"; break;
			case 39: s = "\"for\" expected"; break;
			case 40: s = "\"step\" expected"; break;
			case 41: s = "\"rof\" expected"; break;
			case 42: s = "\"if\" expected"; break;
			case 43: s = "\"then\" expected"; break;
			case 44: s = "\"else\" expected"; break;
			case 45: s = "\"fi\" expected"; break;
			case 46: s = "\"~\" expected"; break;
			case 47: s = "\"++\" expected"; break;
			case 48: s = "\"--\" expected"; break;
			case 49: s = "\"<=>\" expected"; break;
			case 50: s = "\"skip\" expected"; break;
			case 51: s = "\".\" expected"; break;
			case 52: s = "\":\" expected"; break;
			case 53: s = "\"&&\" expected"; break;
			case 54: s = "\"!\" expected"; break;
			case 55: s = "??? expected"; break;
			case 56: s = "invalid number"; break;
			case 57: s = "invalid number"; break;
			case 58: s = "invalid SignalList"; break;
			case 59: s = "invalid Parameter"; break;
			case 60: s = "invalid Statement"; break;
			case 61: s = "invalid Statement"; break;
			case 62: s = "invalid CallStatement"; break;
			case 63: s = "invalid UnaryStatement"; break;
			case 64: s = "invalid AssignStatement"; break;
			case 65: s = "invalid Expression"; break;
			case 66: s = "invalid ShiftExpression"; break;
			case 67: s = "invalid BinaryExpression"; break;
			case 68: s = "invalid UnaryExpression"; break;
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