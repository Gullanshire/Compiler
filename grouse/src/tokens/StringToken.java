package tokens;

import inputHandler.TextLocation;

public class StringToken extends TokenImp {
	protected String str;
	
	protected StringToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	
	protected void setContString(String lexeme) {
		this.str = lexeme;
	}
	public String getContString() {
		return str;
	}
	
	public static StringToken make(TextLocation location, String lexeme) {
		StringToken result = new StringToken(location, lexeme);
		result.setContString(lexeme);
		return result;
	}

	@Override
	protected String rawString() {
		return "string, " + getLexeme();
	}
}