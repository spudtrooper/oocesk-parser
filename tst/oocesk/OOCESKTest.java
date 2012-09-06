package oocesk;

import java.io.File;
import java.io.IOException;

import oocesk.Parser.ParseException;

import org.junit.Test;

public class OOCESKTest {

  @Test
  public void testPrint1() throws ParseException {
    ClassDef foo = getOneClass("print1.oocesk");
    OOCESK.execute(foo);
  }

  @Test
  public void testReturn1() throws ParseException {
    ClassDef foo = getOneClass("return1.oocesk");
    OOCESK.execute(foo);
  }

  private ClassDef getOneClass(String fileName) throws ParseException {
    Parser p = parse(fileName);
    return p.program().get(0);
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

}
