package tokens;

import inputHandler.TextLocation;

public class FloatToken extends TokenImp {
	protected float value;
	
	protected FloatToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	protected void setValueFloat(Float value) {
		this.value = value;
	}
	public float getValueFloat() {
		return value;
	}
	
	public static FloatToken make(TextLocation location, String lexeme) {
		FloatToken result = new FloatToken(location, lexeme);
		result.setValueFloat(Float.parseFloat(lexeme));
		return result;
	}
	
	@Override
	protected String rawString() {
		return "float, " + value;
	}
}
