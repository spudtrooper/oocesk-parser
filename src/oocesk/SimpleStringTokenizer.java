package oocesk;

/**
 * Like a StringTokenizer, but tokenizes on single characters. For example
 * 
 * <pre>
 * SimpleStringTokenizer st = new SimpleStringTokenizer(&quot;abcde&quot;, &quot;cd&quot;);
 * &quot;ab&quot;.equals(st.nextToken());
 * &quot;c&quot;.equals(st.nextToken());
 * &quot;d&quot;.equals(st.nextToken());
 * &quot;e&quot;.equals(st.nextToken());
 * </pre>
 */
class SimpleStringTokenizer {

  private final String buf;
  private final char[] delims;

  private int next;

  private int line = 1, col = 1;

  SimpleStringTokenizer(String buf, String delims) {
    if (buf == null) {
      throw new NullPointerException("buf can't be null");
    }
    if (delims == null) {
      throw new NullPointerException("delims can't be null");
    }
    this.buf = buf;
    this.delims = delims.toCharArray();
  }

  /**
   * Returns the next character in the stream of <code>(char)-1</code>.
   * 
   * @return the next character in the stream of <code>(char)-1</code>
   */
  public char peek() {
    int len = this.buf.length();
    for (int i = this.next; i < len; i++) {
      char c = this.buf.charAt(i);
      if (!Character.isWhitespace(c)) {
        return c;
      }
    }
    return (char) -1;
  }

  /**
   * Returns the next token or empty if there are no more tokens.
   * 
   * @return the next token or empty if there are no more tokens
   */
  public String nextToken() {
    StringBuilder sb = new StringBuilder();
    while (this.next < this.buf.length()) {
      char c = this.buf.charAt(this.next);
      if (isDelim(c)) {
        if (sb.length() == 0) {
          append(sb, c);
        }
        break;
      } else {
        append(sb, c);
      }
    }
    return sb.toString();
  }

  /**
   * Returns the current line.
   * 
   * @return the current line
   */
  public int getLine() {
    return this.line;
  }

  /**
   * Returns the current column.
   * 
   * @return the current column
   */
  public int getColumn() {
    return this.col;
  }

  /**
   * Returns <code>true</code> if there are more tokens.
   * 
   * @return <code>true</code> if there are more tokens
   */
  public boolean hasMoreTokens() {
    return this.next < this.buf.length();
  }

  private void append(StringBuilder sb, char c) {
    sb.append(c);
    this.next++;
    if (c == '\n') {
      this.line++;
      this.col = 1;
    } else {
      this.col++;
    }
  }

  private boolean isDelim(char c) {
    for (char d : this.delims) {
      if (d == c) {
        return true;
      }
    }
    return false;
  }
}
