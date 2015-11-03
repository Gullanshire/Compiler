//
// This token will not be used since all comment token will be removed
// Reserved for token print test
//

package tokens;

import inputHandler.TextLocation;

public class CommentToken extends TokenImp {

	protected CommentToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}

	@Override
	protected String rawString() {
		return "COMMENT";
	}
	
	public static CommentToken make(TextLocation location) {
		CommentToken result = new CommentToken(location, "");
		return result;
	}
}
