

import SymTable.SymTable;
    import java.util.EnumSet;



public class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _int = 2;
	public static final int maxT = 54;

	static final boolean _T = true;
	static final boolean _x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;

	boolean IsIdentEql(){
        scanner.ResetPeek();
        return (la.kind == '$' && scanner.Peek() == _ident && scanner.Peek() == '=');
    }

    boolean NumberTo(){
        scanner.ResetPeek();
        Token x = la;
        Token next = scanner.Peek();
        if(la.kind == _int) {
            return next.kind == "to";
        }
        if(la.kind == '#' && next.kind == _ident) {
            next = scanner.Peek();
            return next.kind == "to";
        }
        if(la.kind == '$' && next.kind == _ident) {
                    next = scanner.Peek();
                    return next.kind == "to";
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
            return next.kind == "to";
        }
        return false;
    }

    boolean IsShift(){
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
                else if(i == 1 && (next.kind == "<<" || next.kind ==">>")){
                    return true;
                }
                next = scanner.Peek();
            }
            return false;
    }
    private static final EnumSet<Token.Kind> enumBinExp = EnumSet.of('+','-','^','*','/','%',"*>","&&","||",'&','|','<','>','=',"!=","<=",">=");
    boolean IsShift(){
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
                else if(i == 1 && enumBinExp.contains(next.kind)){
                    return true;
                }
                next = scanner.Peek();
            }
            return false;
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
		} else if (la.kind == 3) {
			Get();
			Expect(1);
		} else if (la.kind == 4) {
			Get();
			Expect(1);
		} else if (la.kind == 5) {
			Get();
			number();
			if (la.kind == 6) {
				Get();
			} else if (la.kind == 7) {
				Get();
			} else if (la.kind == 8) {
				Get();
			} else if (la.kind == 9) {
				Get();
			} else SynErr(55);
			number();
			Expect(10);
		} else SynErr(56);
	}

	void SyReC() {
		SymTable Tab = new SymTable();
		
		Module();
		while (la.kind == 11) {
			Module();
		}
	}

	void Module() {
		Expect(11);
		Expect(1);
		Expect(5);
		ParameterList();
		Expect(10);
		SignalList();
		StatementList();
	}

	void ParameterList() {
		Parameter();
		while (la.kind == 12) {
			Get();
			Parameter();
		}
	}

	void SignalList() {
		if (la.kind == 16) {
			Get();
		} else if (la.kind == 17) {
			Get();
		} else SynErr(57);
		SignalDeclaration();
		while (la.kind == 12) {
			Get();
			SignalDeclaration();
		}
	}

	void StatementList() {
		Statement();
		while (la.kind == 20) {
			Get();
			Statement();
		}
	}

	void Parameter() {
		if (la.kind == 1) {
		} else if (la.kind == 13) {
			Get();
		} else if (la.kind == 14) {
			Get();
		} else if (la.kind == 15) {
			Get();
		} else SynErr(58);
		SignalDeclaration();
	}

	void SignalDeclaration() {
		Expect(1);
		while (la.kind == 18) {
			Get();
			Expect(2);
			Expect(19);
		}
		if (la.kind == 5) {
			Get();
			Expect(2);
			Expect(10);
		}
	}

	void Statement() {
		switch (la.kind) {
		case 21: case 22: {
			CallStatement();
			break;
		}
		case 23: {
			ForStatement();
			break;
		}
		case 28: {
			IfStatement();
			break;
		}
		case 33: case 34: case 35: {
			UnaryStatement();
			break;
		}
		case 37: {
			SkipStatement();
			break;
		}
		case 1: {
			Signal();
			if (la.kind == 36) {
				SwapStatement();
			} else if (la.kind == 6 || la.kind == 7 || la.kind == 32) {
				AssignStatement();
			} else SynErr(59);
			break;
		}
		default: SynErr(60); break;
		}
	}

	void CallStatement() {
		if (la.kind == 21) {
			Get();
		} else if (la.kind == 22) {
			Get();
		} else SynErr(61);
		Expect(1);
		Expect(5);
		Expect(1);
		while (la.kind == 12) {
			Get();
			Expect(1);
		}
		Expect(10);
	}

	void ForStatement() {
		Expect(23);
		if (IsIdentEql() || NumberTo()) {
			if (IsIdentEql()) {
				Expect(4);
				Expect(1);
				Expect(24);
			}
			number();
			Expect(25);
		}
		number();
		if (la.kind == 26) {
			Get();
			if (la.kind == 7) {
				Get();
			}
			number();
		}
		StatementList();
		Expect(27);
	}

	void IfStatement() {
		Expect(28);
		Expression();
		Expect(29);
		StatementList();
		Expect(30);
		StatementList();
		Expect(31);
		Expression();
	}

	void UnaryStatement() {
		if (la.kind == 33) {
			Get();
		} else if (la.kind == 34) {
			Get();
		} else if (la.kind == 35) {
			Get();
		} else SynErr(62);
		Expect(24);
		Signal();
	}

	void SkipStatement() {
		Expect(37);
	}

	void Signal() {
		Expect(1);
		while (la.kind == 18) {
			Get();
			Expression();
			Expect(19);
		}
		if (la.kind == 38) {
			Get();
			number();
			if (la.kind == 39) {
				Get();
				number();
			}
		}
	}

	void SwapStatement() {
		Expect(36);
		Signal();
	}

	void AssignStatement() {
		if (la.kind == 32) {
			Get();
		} else if (la.kind == 6) {
			Get();
		} else if (la.kind == 7) {
			Get();
		} else SynErr(63);
		Expect(24);
		Signal();
	}

	void Expression() {
		if (la.kind == 1) {
			Signal();
		} else if (IsShift()) {
			ShiftExpression();
		} else if (IsBinary()) {
			BinaryExpression();
		} else if (la.kind == 33 || la.kind == 51) {
			UnaryExpression();
		} else if (StartOf(1)) {
			number();
		} else SynErr(64);
	}

	void ShiftExpression() {
		Expect(5);
		Expression();
		if (la.kind == 52) {
			Get();
		} else if (la.kind == 53) {
			Get();
		} else SynErr(65);
		number();
		Expect(10);
	}

	void BinaryExpression() {
		Expect(5);
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
		case 32: {
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
		case 40: {
			Get();
			break;
		}
		case 41: {
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
		case 24: {
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
		case 50: {
			Get();
			break;
		}
		default: SynErr(66); break;
		}
		Expression();
		Expect(10);
	}

	void UnaryExpression() {
		if (la.kind == 51) {
			Get();
		} else if (la.kind == 33) {
			Get();
		} else SynErr(67);
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
		{_T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_x,_T,_T, _T,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x}

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
			case 3: s = "\"#\" expected"; break;
			case 4: s = "\"$\" expected"; break;
			case 5: s = "\"(\" expected"; break;
			case 6: s = "\"+\" expected"; break;
			case 7: s = "\"-\" expected"; break;
			case 8: s = "\"*\" expected"; break;
			case 9: s = "\"/\" expected"; break;
			case 10: s = "\")\" expected"; break;
			case 11: s = "\"module\" expected"; break;
			case 12: s = "\",\" expected"; break;
			case 13: s = "\"in\" expected"; break;
			case 14: s = "\"out\" expected"; break;
			case 15: s = "\"inout\" expected"; break;
			case 16: s = "\"wire\" expected"; break;
			case 17: s = "\"state\" expected"; break;
			case 18: s = "\"[\" expected"; break;
			case 19: s = "\"]\" expected"; break;
			case 20: s = "\";\" expected"; break;
			case 21: s = "\"call\" expected"; break;
			case 22: s = "\"uncall\" expected"; break;
			case 23: s = "\"for\" expected"; break;
			case 24: s = "\"=\" expected"; break;
			case 25: s = "\"to\" expected"; break;
			case 26: s = "\"step\" expected"; break;
			case 27: s = "\"rof\" expected"; break;
			case 28: s = "\"if\" expected"; break;
			case 29: s = "\"then\" expected"; break;
			case 30: s = "\"else\" expected"; break;
			case 31: s = "\"fi\" expected"; break;
			case 32: s = "\"^\" expected"; break;
			case 33: s = "\"~\" expected"; break;
			case 34: s = "\"++\" expected"; break;
			case 35: s = "\"--\" expected"; break;
			case 36: s = "\"<=>\" expected"; break;
			case 37: s = "\"skip\" expected"; break;
			case 38: s = "\".\" expected"; break;
			case 39: s = "\":\" expected"; break;
			case 40: s = "\"%\" expected"; break;
			case 41: s = "\"*>\" expected"; break;
			case 42: s = "\"&&\" expected"; break;
			case 43: s = "\"||\" expected"; break;
			case 44: s = "\"&\" expected"; break;
			case 45: s = "\"|\" expected"; break;
			case 46: s = "\"<\" expected"; break;
			case 47: s = "\">\" expected"; break;
			case 48: s = "\"!=\" expected"; break;
			case 49: s = "\"<=\" expected"; break;
			case 50: s = "\">=\" expected"; break;
			case 51: s = "\"!\" expected"; break;
			case 52: s = "\"<<\" expected"; break;
			case 53: s = "\">>\" expected"; break;
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
