package oocesk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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

  public Token nextToken() {
    String s = nextString();
    if (s == null) {
      return null;
    }
    if (isWhitespace(s)) {
      return nextToken();
    }

    // Ops
    if (ops.contains(s)) {
      return new Token(s, Token.Type.Op);
    }

    // Keywords
    if (keywords.contains(s)) {
      return new Token(s, Token.Type.Keyword);
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
      return new Token(t, Token.Type.Token);
    }

    // Ints
    if (isInteger(s)) {
      return new Token(s, Token.Type.Int);
    }

    // Names
    if (s.startsWith("$")) {
      return new Token(s, Token.Type.Name);
    }

    // Id or label
    return new Token(s, Token.Type.IdOrLabel);
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
