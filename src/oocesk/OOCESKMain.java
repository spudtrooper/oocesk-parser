package oocesk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import oocesk.Parser.ParseException;

public class OOCESKMain {

  public static void main(String[] args) {
    System.exit(new OOCESKMain().realMain(args));
  }

  public int realMain(String[] args) {
    List<File> files = new ArrayList<File>();
    boolean verbose = false;
    for (int i = 0; i < args.length;) {
      String arg = args[i++];
      if ("-h".equals(arg) || "--help".equals(arg)) {
        printUsage();
        return 0;
      } else if ("-v".equals(arg) || "--verbose".equals(arg)) {
        verbose = true;
      } else {
        File f = new File(arg);
        if (!f.exists()) {
          error(f + " doesn't exist");
        } else if (f.isDirectory()) {
          error(f + " is directory");
        } else {
          files.add(f);
        }
      }
    }

    // Fail if no files are given
    if (files.isEmpty()) {
      error("No files given");
      printUsage();
      return 1;
    }

    // Parse all the files and try to find a class with a main method
    ClassDef mainClass = null;
    for (File f : files) {
      try {
        Parser p = Parser.newInstance(f);
        List<ClassDef> cds = p.program();
        if (mainClass == null) {
          for (ClassDef cd : cds) {
            MethodDef md = cd.lookupMethod("main");
            if (md != null) {
              mainClass = cd;
            }
          }
        }
      } catch (IOException e) {
        handle(e, verbose);
      } catch (ParseException e) {
        handle(e, verbose);
      }
    }

    // Fail if we can't find a main method
    if (mainClass == null) {
      error("Couldn't find class with main method");
      return 1;
    }

    // Execute the main method
    OOCESK.execute(mainClass);

    return 0;
  }

  private void handle(Throwable e, boolean verbose) {
    error(e.getMessage());
    if (verbose) {
      e.printStackTrace();
    }
  }

  private void error(String msg) {
    System.err.println(msg);
  }

  private void printUsage() {
    error("Usage: " + getClass().getName() + " [ option? ] <file>+");
    error("where options include:");
    error(" -h || --help      print this message");
    error(" -v || --verbose   print verbose errors");
    error("and files are .oocesk files");
  }
}
