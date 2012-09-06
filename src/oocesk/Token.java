package oocesk;

final class Token {

	enum Type {
		Name,
		IdOrLabel,
		Int,
		Op,
		Token,
		Keyword;
	}

	public static Token name(String s) {
		return new Token(s, Type.Name);
	}

	public static Token idOrLabel(String s) {
		return new Token(s, Type.IdOrLabel);
	}

	public static Token integer(String s) {
		return new Token(s, Type.Int);
	}

	public static Token op(String s) {
		return new Token(s, Type.Op);
	}

	public static Token token(String s) {
		return new Token(s, Type.Token);
	}

	public static Token keyword(String s) {
		return new Token(s, Type.Keyword);
	}

	public final String text;
	public final Type type;

	Token(String text, Type type) {
		this.text = text;
		this.type = type;
	}

	@Override
	public int hashCode() {
		return this.text.hashCode() + this.type.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Token)) {
			return false;
		}
		Token that = (Token) o;
		return this.text.equals(that.text) && this.type.equals(that.type);
	}

	@Override
	public String toString() {
		return this.type + "(" + this.text + ")";
	}
}
