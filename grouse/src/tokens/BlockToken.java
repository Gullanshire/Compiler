package tokens;

import inputHandler.TextLocation;

public class BlockToken extends TokenImp {
	
	protected BlockToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}

	@Override
	protected String rawString() {
		return "BLOCK";
	}
	
	public static BlockToken make(TextLocation location) {
		BlockToken result = new BlockToken(location, "");
		return result;
	}
}