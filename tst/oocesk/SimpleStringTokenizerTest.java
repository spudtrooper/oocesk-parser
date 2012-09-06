package oocesk;

import static org.junit.Assert.*;

import org.junit.Test;

public class SimpleStringTokenizerTest {

  @Test
  public void test() {
    String test = "abcde";
    String delims = "bd";
    SimpleStringTokenizer st = new SimpleStringTokenizer(test, delims);
    assertEquals("a", st.nextToken());
    assertEquals("b", st.nextToken());
    assertEquals("c", st.nextToken());
    assertEquals("d", st.nextToken());
    assertEquals("e", st.nextToken());
  }

  @Test
  public void test2() {
    String test = "abcde";
    String delims = "cd";
    SimpleStringTokenizer st = new SimpleStringTokenizer(test, delims);
    assertEquals("ab", st.nextToken());
    assertEquals("c", st.nextToken());
    assertEquals("d", st.nextToken());
    assertEquals("e", st.nextToken());
  }

  @Test
  public void testMoreTokens() {
    String test = "abc";
    String delims = "b";
    SimpleStringTokenizer st = new SimpleStringTokenizer(test, delims);
    assertEquals("a", st.nextToken());
    assertTrue(st.hasMoreTokens());
    assertEquals("b", st.nextToken());
    assertTrue(st.hasMoreTokens());
    assertEquals("c", st.nextToken());
    assertFalse(st.hasMoreTokens());
    assertEquals("", st.nextToken());
    assertFalse(st.hasMoreTokens());
    assertEquals("", st.nextToken());
    assertFalse(st.hasMoreTokens());
    assertEquals("", st.nextToken());
    assertFalse(st.hasMoreTokens());
  }

  @Test
  public void testEmpty() {
    String test = "";
    String delims = "b";
    SimpleStringTokenizer st = new SimpleStringTokenizer(test, delims);
    assertFalse(st.hasMoreTokens());
    assertEquals("", st.nextToken());
    assertFalse(st.hasMoreTokens());
    assertEquals("", st.nextToken());
    assertFalse(st.hasMoreTokens());
    assertEquals("", st.nextToken());
    assertFalse(st.hasMoreTokens());
  }

}
