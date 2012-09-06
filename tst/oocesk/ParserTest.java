package oocesk;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import oocesk.Parser.ParseException;

import org.junit.Test;

public class ParserTest {

	@Test
	public void testMethodDefPrint() throws ParseException {
		Stmt body = lookupOneBody("methodDefPrint.oocesk", "foo");
		PrintStmt stmt = (PrintStmt) body;
		assertEquals(1, ((IntExp) stmt.args[0]).value);
		assertEquals(2, ((IntExp) stmt.args[1]).value);
	}

	@Test
	public void testMethodDefInstanceOf() throws ParseException {
		Stmt body = lookupOneBody("methodDefAssignInstanceOf.oocesk", "foo");
		AssignAExpStmt stmt = (AssignAExpStmt) body;
		assertEquals("$a", stmt.lhs);
		InstanceOfExp exp = (InstanceOfExp) stmt.rhs;
		assertEquals(1, ((IntExp) exp.object).value);
		assertEquals("Integer", exp.className);
	}

	@Test
	public void testMethodDefAtomicAdd() throws ParseException {
		genericAtomicOpMethodDefTest("methodDefAtomicAdd.oocesk", PrimOp.ADD);
	}

	@Test
	public void testMethodDefAtomicSub() throws ParseException {
		genericAtomicOpMethodDefTest("methodDefAtomicSub.oocesk", PrimOp.SUB);
	}

	@Test
	public void testMethodDefAtomicMul() throws ParseException {
		genericAtomicOpMethodDefTest("methodDefAtomicMul.oocesk", PrimOp.MUL);
	}

	@Test
	public void testMethodDefAtomicEq() throws ParseException {
		genericAtomicOpMethodDefTest("methodDefAtomicEq.oocesk", PrimOp.EQ);
	}

	@Test
	public void testMethodDefInvokeStmt() throws ParseException {
		Stmt body = lookupOneBody("methodDefInvokeStmt.oocesk", "foo");
		InvokeStmt stmt = (InvokeStmt) body;
		assertEquals("$a", stmt.lhs);
		assertEquals("boo", stmt.methodName);
	}

	@Test
	public void testMethodDefInvokeSuperStmt() throws ParseException {
		Stmt body = lookupOneBody("methodDefInvokeSuperStmt.oocesk", "foo");
		InvokeSuperStmt stmt = (InvokeSuperStmt) body;
		assertEquals("$a", stmt.lhs);
		assertEquals("boo", stmt.methodName);
	}

	@Test
	public void testMethodDefNewStmt() throws ParseException {
		Stmt body = lookupOneBody("methodDefNewStmt.oocesk", "foo");
		NewStmt stmt = (NewStmt) body;
		assertEquals("$a", stmt.lhs);
		assertEquals("Boo", stmt.className);
	}

	@Test
	public void testMethodDefAssignAExp() throws ParseException {
		Stmt body = lookupOneBody("methodDefAssignAExp.oocesk", "foo");
		AssignAExpStmt stmt = (AssignAExpStmt) body;
		assertEquals("$a", stmt.lhs);
		assertEquals(1, ((IntExp) stmt.rhs).value);
	}

	@Test
	public void testMethodDefThrow() throws ParseException {
		Stmt body = lookupOneBody("methodDefThrow.oocesk", "foo");
		ThrowStmt stmt = (ThrowStmt) body;
		assertTrue(stmt.exception instanceof ThisExp);
	}

	@Test
	public void testMethodDefIf() throws ParseException {
		Stmt body = lookupOneBody("methodDefIf.oocesk", "foo");
		IfStmt stmt = (IfStmt) body;
		assertEquals(true, ((BooleanExp) stmt.condition).value);
		assertEquals("theLabel", stmt.label);
	}

	@Test
	public void testMethodDefReturn() throws ParseException {
		Stmt body = lookupOneBody("methodDefReturn.oocesk", "foo");
		ReturnStmt stmt = (ReturnStmt) body;
		assertTrue(stmt.result instanceof ThisExp);
	}

	@Test
	public void testMethodDefMoveException() throws ParseException {
		Stmt body = lookupOneBody("methodDefMoveException.oocesk", "foo");
		MoveExceptionStmt stmt = (MoveExceptionStmt) body;
		assertEquals("$n", stmt.register);
	}

	@Test
	public void testMethodDefPopHandler() throws ParseException {
		Stmt body = lookupOneBody("methodDefPopHandler.oocesk", "foo");
		PopHandlerStmt stmt = (PopHandlerStmt) body;
		assertNotNull(stmt);
	}

	@Test
	public void testMethodDefPushHandler() throws ParseException {
		Stmt body = lookupOneBody("methodDefPushHandler.oocesk", "foo");
		PushHandlerStmt stmt = (PushHandlerStmt) body;
		assertEquals("Foo", stmt.className);
		assertEquals("theLabel", stmt.label);
	}

	@Test
	public void testMethodDefGoto() throws ParseException {
		Stmt body = lookupOneBody("methodDefGoto.oocesk", "foo");
		GotoStmt stmt = (GotoStmt) body;
		assertEquals("theLabel", stmt.label);
	}

	@Test
	public void testMethodDefSkip() throws ParseException {
		Stmt body = lookupOneBody("methodDefSkip.oocesk", "foo");
		SkipStmt stmt = (SkipStmt) body;
		assertNotNull(stmt);
	}

	@Test
	public void testMethodDefLabel() throws ParseException {
		Stmt body = lookupOneBody("methodDefLabel.oocesk", "foo");
		LabelStmt stmt = (LabelStmt) body;
		assertEquals("theLabel", stmt.label);
	}

	@Test
	public void testEmptyMethodDefFormals() throws ParseException {
		ClassDef c = getOneClassDef("emptyMethodDefFormals.oocesk");
		{
			MethodDef m = c.lookupMethod("foo");
			assertEquals("foo", m.name);
			assertArrayEquals(new String[] { "$a" }, m.formals);
		}
		{
			MethodDef m = c.lookupMethod("boo");
			assertEquals("boo", m.name);
			assertArrayEquals(new String[] { "$a", "$b" }, m.formals);
		}
		{
			MethodDef m = c.lookupMethod("coo");
			assertEquals("coo", m.name);
			assertArrayEquals(new String[] { "$a", "$b", "$c" }, m.formals);
		}
	}

	@Test
	public void testEmptyMethodDef() throws ParseException {
		ClassDef c = getOneClassDef("emptyMethodDef.oocesk");
		MethodDef foo = c.lookupMethod("foo");
		assertEquals("foo", foo.name);
		assertArrayEquals(new String[] {}, foo.formals);
	}

	@Test
	public void testFieldDef() throws ParseException {
		ClassDef c = getOneClassDef("fieldDef.oocesk");
		assertEquals("a", c.lookupField("a").name);
		assertEquals("b", c.lookupField("b").name);
		assertEquals("c", c.lookupField("c").name);
		assertNull(c.lookupField("d"));
	}

	@Test
	public void testSimple() throws ParseException {
		Parser p = parse("simple.oocesk");
		List<ClassDef> classDefs = p.program();
		assertEquals(1, classDefs.size());
		ClassDef c = classDefs.get(0);
		assertEquals("Foo", c.name);
		assertEquals("Object", c.parentClassName);
	}

	@Test
	public void testEmpty() throws ParseException {
		Parser p = parse("empty.oocesk");
		List<ClassDef> classDefs = p.program();
		assertTrue(classDefs.isEmpty());
	}

	/* --- Misc --- */

	private void genericAtomicOpMethodDefTest(String fileName, PrimOp op) throws ParseException {
		Stmt body = lookupOneBody(fileName, "foo");
		AssignAExpStmt stmt = (AssignAExpStmt) body;
		assertEquals("$a", stmt.lhs);
		AtomicOpExp exp = (AtomicOpExp) stmt.rhs;
		assertEquals(op, exp.op);
		assertEquals(1, ((IntExp) exp.args[0]).value);
		assertEquals(2, ((IntExp) exp.args[1]).value);
	}

	private final File testDir = new File("test");

	private Parser parse(String fileName) {
		try {
			File file = new File(testDir, fileName);
			return Parser.newInstance(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private ClassDef getOneClassDef(String fileName) throws ParseException {
		return getOneClassDef(parse(fileName));
	}

	private ClassDef getOneClassDef(Parser p) throws ParseException {
		List<ClassDef> classDefs = p.program();
		assertEquals(1, classDefs.size());
		return classDefs.get(0);
	}

	private MethodDef lookupOneMethod(String fileName, String methodName) throws ParseException {
		ClassDef c = getOneClassDef(fileName);
		MethodDef m = c.lookupMethod(methodName);
		assertEquals(methodName, m.name);
		return m;
	}

	private Stmt lookupOneBody(String fileName, String methodName) throws ParseException {
		return lookupOneMethod(fileName, methodName).body;
	}

}
