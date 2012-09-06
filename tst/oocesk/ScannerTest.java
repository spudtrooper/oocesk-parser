package oocesk;

import static org.junit.Assert.*;

import org.junit.Test;

public class ScannerTest {

	@Test
	public void opTest() {
		String test = ": := : ;";
		Scanner s = new Scanner(test);
		assertEquals(Token.token(":"), s.nextToken());
		assertEquals(Token.token(":="), s.nextToken());
		assertEquals(Token.token(":"), s.nextToken());
		assertEquals(Token.token(";"), s.nextToken());
	}

	@Test
	public void keywordTest() {
		String test = "class foo extends $n";
		Scanner s = new Scanner(test);
		assertEquals(Token.keyword("class"), s.nextToken());
		assertEquals(Token.idOrLabel("foo"), s.nextToken());
		assertEquals(Token.keyword("extends"), s.nextToken());
		assertEquals(Token.name("$n"), s.nextToken());
	}
	
	@Test
	public void mixedTest() {
		String test = "class{ foo) { extends $n";
		Scanner s = new Scanner(test);
		assertEquals(Token.keyword("class"), s.nextToken());
		assertEquals(Token.token("{"), s.nextToken());
		assertEquals(Token.idOrLabel("foo"), s.nextToken());
		assertEquals(Token.token(")"), s.nextToken());
		assertEquals(Token.token("{"), s.nextToken());
		assertEquals(Token.keyword("extends"), s.nextToken());
		assertEquals(Token.name("$n"), s.nextToken());
	}

}
