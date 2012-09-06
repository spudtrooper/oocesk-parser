package oocesk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Parser {

	private final Scanner lex;
	
	public static Parser newInstance(File file) throws IOException {
		StringBuilder program = new StringBuilder();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			String s;
			while ((s = in.readLine()) != null) {
				if (program.length() > 0) {
					program.append("\n");
				}
				program.append(s);
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return new Parser(new Scanner(program.toString()));
	}

	Parser(Scanner lex) {
		this.lex = lex;
	}

	public List<ClassDef> program() throws ParseException {
		nextToken();
		final List<ClassDef> res = new ArrayList<ClassDef>();
		while (true) {
			ClassDef cd = classDef();
			if (cd != null) {
				res.add(cd);
			} else {
				break;
			}
		}
		return res;
	}

	/*- Rules -*/

	/*
	 * class-def ::= class class-name extends class-name { field-def ...
	 * method-def ... }
	 */
	private ClassDef classDef() throws ParseException {
		if (dontHaveToken("class")) {
			return null;
		}
		String className = idOrLabel();
		shift("extends");
		String parentClassName = idOrLabel();
		ClassDef res = new ClassDef(className, parentClassName);
		shift("{");

		// Fields
		while (true) {
			FieldDef def = fieldDef();
			if (def == null) {
				break;
			}
			res.addField(def.name);
		}

		// Methods
		while (true) {
			MethodDef def = methodDef();
			if (def == null) {
				break;
			}
			res.addMethod(def.name, def.formals, def.body);
		}
		shift("}");
		return res;
	}

	/*
	 * method-def ::= def method-name($name, ..., $name) { body }
	 */
	private MethodDef methodDef() throws ParseException {
		if (dontHaveToken("def")) {
			return null;
		}
		String name = idOrLabel();
		String[] formals = names();
		shift("{");
		Stmt body = body();
		shift("}");
		return new MethodDef(name, formals, body);
	}

	/*
	 * body ::= stmt ...
	 */
	private Stmt body() throws ParseException {
		return stmt();
	}

	/*
	 * stmt ::= ...
	 */
	private Stmt stmt() throws ParseException {

		// print(aexp, ..., aexp)
		if (haveToken("print")) {
			AExp[] args = aexps();
			shift(";");
			return new PrintStmt(stmt(), args);
		}

		// label label :
		if (haveToken("label")) {
			String label = idOrLabel();
			shift(":");
			return new LabelStmt(label, stmt());
		}

		// skip ;
		if (haveToken("skip")) {
			shift(";");
			return new SkipStmt(stmt());
		}

		// goto label ;
		if (haveToken("goto")) {
			String label = idOrLabel();
			shift(";");
			return new GotoStmt(stmt(), label);
		}

		// if aexp goto label ;
		if (haveToken("if")) {
			AExp condition = aexp();
			shift("goto");
			String label = idOrLabel();
			shift(";");
			return new IfStmt(stmt(), condition, label);
		}

		// return aexp ;
		if (haveToken("return")) {
			AExp result = aexp();
			shift(";");
			return new ReturnStmt(stmt(), result);
		}

		// pushHandler class-name label ;
		if (haveToken("pushHandler")) {
			String className = idOrLabel();
			String label = idOrLabel();
			shift(";");
			return new PushHandlerStmt(stmt(), className, label);
		}

		// popHandler ;
		if (haveToken("popHandler")) {
			shift(";");
			return new PopHandlerStmt(stmt());
		}

		// throw aexp ;
		if (haveToken("throw")) {
			AExp exception = aexp();
			shift(";");
			return new ThrowStmt(stmt(), exception);
		}

		// moveException $name ;
		if (haveToken("moveException")) {
			String register = name();
			shift(";");
			return new MoveExceptionStmt(stmt(), register);
		}

		// Since $name in first(aexp) we need to deal with this conflict
		if (tokenMatchesType(Token.Type.Name)) {

			// $name := aexp | cexp ;
			if (this.lex.peek() == ':') {
				String lhs = name();
				shift(":=");
				AExp rhs = aexp();

				if (rhs != null) {
					shift(";");
					return new AssignAExpStmt(stmt(), lhs, rhs);
				} else {
					Stmt res = cexp(lhs);
					shift(";");
					return res;
				}
			}

			// aexp.field-name := aexp ;
			else {

			}
		}

		return null;
	}

	private Stmt cexp(String lhs) throws ParseException {

		// new class-name
		if (haveToken("new")) {
			String className = idOrLabel();
			return new NewStmt(stmt(), lhs, className);
		}

		// invoke ...
		if (haveToken("invoke")) {

			// invoke super.method-name(aexp,...,aexp)
			if (haveToken("super")) {
				shift(".");
				String methodName = idOrLabel();
				AExp[] args = aexps();
				return new InvokeSuperStmt(stmt(), lhs, methodName, args);
			}

			// invoke aexp.method-name(aexp,...,aexp)
			else {
				AExp object = aexp();
				shift(".");
				String methodName = idOrLabel();
				AExp[] args = aexps();
				return new InvokeStmt(stmt(), lhs, object, methodName, args);
			}
		}

		return null;
	}

	/*
	 * aexp ::= ...
	 */
	private AExp aexp() throws ParseException {
		// aexp.field-name
		AExp object = aexpPrime();
		if (object == null) {
			return null;
		}
		if (tokenMatches(".")) {
			String field = idOrLabel();
			return new FieldExp(object, field);
		}
		return object;
	}

	private AExp aexpPrime() throws ParseException {

		// this
		if (haveToken("this")) {
			return new ThisExp();
		}

		// true
		if (haveToken("true")) {
			return new BooleanExp(true);
		}

		// false
		if (haveToken("false")) {
			return new BooleanExp(false);
		}

		// null
		if (haveToken("null")) {
			return new NullExp();
		}

		// void
		if (haveToken("void")) {
			return new VoidExp();
		}

		// $name
		if (tokenMatchesType(Token.Type.Name)) {
			String register = name();
			return new RegisterExp(register);
		}

		// int
		if (tokenMatchesType(Token.Type.Int)) {
			int value = integer();
			return new IntExp(value);
		}

		// atomic-op(aexp, ..., aexp)
		if (tokenMatchesType(Token.Type.Op)) {
			PrimOp op = op();
			AExp[] args = aexps();
			return new AtomicOpExp(op, args);
		}

		// instanceof(aexp, class-name)
		if (haveToken("instanceof")) {
			shift("(");
			AExp object = aexp();
			shift(",");
			String className = idOrLabel();
			shift(")");
			return new InstanceOfExp(object, className);
		}

		return null;
	}

	/**
	 * <pre>
	 * () -> []
	 * ($foo) -> [ "$foo" ]
	 * ($foo,$boo) -> [ "$foo", "$boo" ]
	 * ($foo,$boo,$horatio) -> [ "$foo", "$boo", "$horatio" ]
	 * </pre>
	 */
	private String[] names() throws ParseException {
		final List<String> res = new ArrayList<String>();
		shift("(");
		while (true) {
			if (haveToken(")")) {
				break;
			}
			if (res.isEmpty()) {
				res.add(name());
			} else {
				shift(",");
				res.add(name());
			}
		}
		return (String[]) res.toArray(new String[0]);
	}

	/*
	 * field-def ::= var field-name ;
	 */
	private FieldDef fieldDef() throws ParseException {
		if (dontHaveToken("var")) {
			return null;
		}
		String fieldName = idOrLabel();
		shift(";");
		return new FieldDef(fieldName);
	}

	/*- Misc -*/

	private AExp[] aexps() throws ParseException {
		final List<AExp> res = new ArrayList<AExp>();
		shift("(");
		while (true) {
			if (haveToken(")")) {
				break;
			}
			if (res.isEmpty()) {
				res.add(aexp());
			} else {
				shift(",");
				res.add(aexp());
			}
		}
		return (AExp[]) res.toArray(new AExp[0]);
	}

	/**
	 * Returns <code>true</code> if the current token doesn't have the text
	 * <code>expected</code>. If we return <code>false</code> we also
	 * {@link #shift(String...)} the current token. This way you can do the
	 * following check for a nullable token <em>t</em> in a first set:
	 * 
	 * <pre>
	 * if (dontHaveToken(<em>t</em>)) return null;
	 * </pre>
	 * 
	 * @param expected
	 *          expected test
	 * @return <code>true</code> if the current token doesn't have the text
	 *         <code>expected</code>
	 * @throws ParseException
	 *           throw by {@link #shift(String...)}
	 */
	private boolean dontHaveToken(String expected) throws ParseException {
		if (!tokenMatches(expected)) {
			return true;
		}
		shift();
		return false;
	}

	/**
	 * Returns <code>true</code> if the current token has the text
	 * <code>expected</code>. If we return <code>true</code> we also
	 * {@link #shift(String...)} the current token. This way you can do the
	 * following check for a token <em>t</em> in a first set:
	 * 
	 * <pre>
	 * if (haveToken(<em>t</em>)) { ... }
	 * </pre>
	 * 
	 * @param expected
	 *          expected test
	 * @return <code>true</code> if the current token has the text
	 *         <code>expected</code>
	 * @throws ParseException
	 *           throw by {@link #shift(String...)}
	 */
	private boolean haveToken(String expected) throws ParseException {
		if (!tokenMatches(expected)) {
			return false;
		}
		shift();
		return true;
	}

	private String idOrLabel() throws ParseException {
		return getTextFromLastToken(Token.Type.IdOrLabel);
	}

	private String name() throws ParseException {
		return getTextFromLastToken(Token.Type.Name);
	}

	private final static Map<String, PrimOp> strings2primOps = new HashMap<String, PrimOp>();
	static {
		strings2primOps.put("+", PrimOp.ADD);
		strings2primOps.put("-", PrimOp.SUB);
		strings2primOps.put("*", PrimOp.MUL);
		strings2primOps.put("=", PrimOp.EQ);
	}

	private PrimOp op() throws ParseException {
		String op = getTextFromLastToken(Token.Type.Op);
		return strings2primOps.get(op);
	}

	private int integer() throws NumberFormatException, ParseException {
		return Integer.parseInt(getTextFromLastToken(Token.Type.Int));
	}

	private String getTextFromLastToken(Token.Type type) throws ParseException {
		Token t = this.lastToken;
		if (t == null || t.text == null) {
			return null;
		}
		if (t.type != type) {
			return null;
		}
		String res = t.text;
		shift();
		return res;
	}

	private boolean tokenMatches(String text) {
		Token t = this.lastToken;
		return t != null && t.text != null && t.text.equals(text);
	}

	private boolean tokenMatchesType(Token.Type type) {
		Token t = this.lastToken;
		return t != null && t.type == type;
	}

	final static class ParseException extends Exception {
		ParseException(String msg) {
			super(msg);
		}
	}

	private Token shift(String... expected) throws ParseException {
		if (expected.length == 0) {
			return nextToken();
		} else {
			Token t = nextToken();
			for (String s : expected) {
				if (t.text.equals(s)) {
					return t;
				}
			}
			String msg = "Expected one of ";
			for (String s : expected) {
				msg += s + " ";
			}
			msg += " -- have " + t.text;
			throw new ParseException(msg);
		}

	}

	private Token lastToken;

	private Token nextToken() {
		if (this.lastToken == null) {
			this.lastToken = this.lex.nextToken();
			return this.lastToken;
		}
		Token res = this.lastToken;
		lastToken = this.lex.nextToken();
		return res;
	}
}
