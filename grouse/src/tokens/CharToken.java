package tokens;

import inputHandler.TextLocation;

public class CharToken extends TokenImp {
	protected char ch;
	
	protected CharToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	
	protected void setContChar(String lexeme) {
		this.ch = lexeme.charAt(0);
	}
	public char getContChar() {
		return ch;
	}
	
	public static CharToken make(TextLocation location, String lexeme) {
		CharToken result = new CharToken(location, lexeme);
		result.setContChar(lexeme);
		return result;
	}
	
	@Override
	protected String rawString() {
		return "char, " + getLexeme();
	}
}