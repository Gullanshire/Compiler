package tokens;

import inputHandler.TextLocation;

public class LetToken extends TokenImp {
	protected LetToken(TextLocation location, String lexeme) {
		super(location, lexeme.intern());
	}
	
	public static LetToken make(TextLocation location, String lexeme) {
		LetToken result = new LetToken(location, lexeme);
		return result;
	}

	@Override
	protected String rawString() {
		return "let, " + getLexeme();
	}
}