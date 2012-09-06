package oocesk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import oocesk.Token.Type;

class Scanner {

  private final static Collection<String> TOKENS = Arrays.asList("{", "}", "(", ")", ":=", ":",
      ";", ",", ".");
  private final static Collection<String> KEYWORDS = Arrays.asList("class", "extends", "var",
      "def", "label", "skip", "goto", "if", "return", "pushHandler", "popHandler", "throw",
      "moveException", "new", "invoke", "super", "this", "true", "false", "null", "void",
      "instanceof");
  private final static Collection<String> OPS = Arrays.asList("+", "-", "*", "=");

  private final SimpleStringTokenizer st;
  private final Collection<String> tokenPrefixes;
  private final Collection<String> keywords;
  private final Collection<String> ops;

  private Scanner(String text, Collection<String> tokens, Collection<String> keywords,
      Collection<String> ops) {
    this.tokenPrefixes = new ArrayList<String>();
    for (String t : tokens) {
      this.tokenPrefixes.add(t.substring(0, 1));
    }
    this.keywords = keywords;
    this.ops = ops;
    StringBuilder delims = new StringBuilder();
    for (String t : tokens)
      delims.append(t);
    for (String t : ops)
      delims.append(t);
    delims.append(' ');
    delims.append('\t');
    delims.append('\n');
    this.st = new SimpleStringTokenizer(text, delims.toString());
  }

  Scanner(String text) {
    this(text, TOKENS, KEYWORDS, OPS);
  }

  public char peek() {
    return this.st.peek();
  }

  private int startLine, startCol;

  public Token nextToken() {
    this.startLine = this.st.getLine();
    this.startCol = this.st.getColumn();
    return nextTokenLoop();
  }

  private Token nextTokenLoop() {
    String s = nextString();
    if (s == null) {
      return null;
    }
    if (isWhitespace(s)) {
      return nextTokenLoop();
    }

    // Ops
    if (ops.contains(s)) {
      return newToken(s, Token.Type.Op);
    }

    // Keywords
    if (keywords.contains(s)) {
      return newToken(s, Token.Type.Keyword);
    }

    // Tokens (hacky!) all because of the f'ing :=
    if (tokenPrefixes.contains(s)) {
      // :=
      String t = s;
      if (s.equals(":")) {
        if ('=' == st.peek()) {
          t += nextString();
        }
      }
      return newToken(t, Token.Type.Token);
    }

    // Ints
    if (isInteger(s)) {
      return newToken(s, Token.Type.Int);
    }

    // Names
    if (s.startsWith("$")) {
      return newToken(s, Token.Type.Name);
    }

    // Id or label
    return newToken(s, Token.Type.IdOrLabel);
  }

  private Token newToken(String text, Type type) {
    int line = this.st.getLine();
    int col = this.st.getColumn();
    return new Token(text, type, this.startLine, this.startCol, line, col);
  }

  private boolean isInteger(String s) {
    if (s == null) {
      return false;
    }
    try {
      Integer.parseInt(s);
      return true;
    } catch (NumberFormatException _) {}
    return false;
  }

  private boolean isWhitespace(String s) {
    return s == null || s.length() != 1 ? false : Character.isWhitespace(s.charAt(0));
  }

  private String nextString() {
    return st.hasMoreTokens() ? st.nextToken() : null;
  }
}
