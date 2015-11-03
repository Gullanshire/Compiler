package lexicalAnalyzer;


import logging.GrouseLogger;

import inputHandler.InputHandler;
import inputHandler.LocatedChar;
import inputHandler.LocatedCharStream;
import inputHandler.PushbackCharStream;
import inputHandler.TextLocation;
import tokens.CharToken;
import tokens.FloatToken;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.NullToken;
import tokens.NumberToken;
import tokens.StringToken;
//import tokens.CommentToken;
import tokens.Token;

import static lexicalAnalyzer.PunctuatorScanningAids.*;

public class LexicalAnalyzer extends ScannerImp implements Scanner {
	public static LexicalAnalyzer make(String filename) {
		InputHandler handler = InputHandler.fromFilename(filename);
		PushbackCharStream charStream = PushbackCharStream.make(handler);
		return new LexicalAnalyzer(charStream);
	}

	public LexicalAnalyzer(PushbackCharStream input) {
		super(input);
	}
	
	// to distinguish minus / subtract
	// set as false if the next '-' is expected to be minus sign
	private boolean expect_operand = true;
			
	//////////////////////////////////////////////////////////////////////////////
	// Token-finding main dispatch	

	@Override
	protected Token findNextToken() {
		LocatedChar ch = nextNonWhitespaceChar();
		
		// negative number token
		if( ch.isDigit() || 
				 ( (ch.getCharacter() == '-')&&expect_operand) ) {
			expect_operand = false;
			return scanNumber(ch);
		}
		else if(ch.isLowerCase() || ch.isUpperCase() || ch.getCharacter() == '_') {
			expect_operand = false;
			return scanIdentifier(ch);
		}
		
		// Oct.2 3:09pm check comment
		else if(ch.getCharacter() == '/'){
			expect_operand = true;
			
			LocatedChar ch2 = input.next();
			if(ch2.getCharacter() == '/')
				return scanComment(ch2);
			else {
				// push back '/' for divide punctuator
				input.pushback(ch2);
				return PunctuatorScanner.scan(ch, input);
			}
		}
		else if(ch.getCharacter() == '\'') {
			expect_operand = false;
			ch = input.next();
			int ascii = (int) ch.getCharacter();
			
			StringBuffer buffer = new StringBuffer();
			buffer.append(ch.getCharacter());
			
			if (ascii >=32 && ascii <= 126)
				return CharToken.make(ch.getLocation(),buffer.toString());
			else {
				invalidCharacter(ch);
				return findNextToken();
			}
		}
		else if(ch.getCharacter() == '\"') {
			expect_operand = false;
			return StringScanner(ch);
		}
		else if(isPunctuatorStart(ch)) {
			expect_operand = true;
			return PunctuatorScanner.scan(ch, input);
		}
		else if(isEndOfInput(ch)) {
			return NullToken.make(ch.getLocation());
		}
		else {
			lexicalError(ch);
			return findNextToken();
		}
	}

	private LocatedChar nextNonWhitespaceChar() {
		LocatedChar ch = input.next();
		while(ch.isWhitespace()) {
			ch = input.next();
		}
		return ch;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Integer lexical analysis	
	// Oct.5 If met with '.' or 'e', use float lexical analysis

	private Token scanNumber(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(firstChar.getCharacter());
		boolean isFloat = appendSubsequentDigits(buffer);
		if (isFloat)
			return FloatToken.make(firstChar.getLocation(), buffer.toString());
		else
			return NumberToken.make(firstChar.getLocation(), buffer.toString());
	}
	private boolean appendSubsequentDigits(StringBuffer buffer) {
		LocatedChar c = input.next();
		boolean isFloat = false;
		// false - it is a integer token
		// true - it is a float token
		while(c.isDigit()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		if (c.getCharacter() == '.' || c.getCharacter() == 'e')
		{
			isFloat = true;
			buffer.append(c.getCharacter());
			c= input.next();
			if(c.getCharacter() == '-' || c.getCharacter() == '+')
			{
				buffer.append(c.getCharacter());
				c = input.next();
			}
			while(c.isDigit()) {
				buffer.append(c.getCharacter());
				c = input.next();
			}
		}
		input.pushback(c);
		return isFloat;
	}
	
	//////////////////////////////////////////////////////////////////////////////
	// String Scanner
	private Token StringScanner(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		appendSubsequentStringCharacters(buffer);
		return StringToken.make(firstChar.getLocation(), buffer.toString());
	}
	private void appendSubsequentStringCharacters(StringBuffer buffer) {
		LocatedChar c = input.next();
		int ascii = (int) c.getCharacter();
		while(ascii >= 32 && ascii <= 126 && ascii != 34) {
			buffer.append(c.getCharacter());
			c = input.next();
			ascii = (int) c.getCharacter();
		}
		if (ascii != 34)
		{
			invalidCharacter(c);
			input.pushback(c);
		}
	}
	//////////////////////////////////////////////////////////////////////////////
	// Comment lexical analysis Oct.2 3:19pm
	private Token scanComment(LocatedChar firstChar) {
		LocatedChar ch = input.next();
		// until met with a line separator
		while(!String.valueOf(ch.getCharacter()).matches("\n")) {
			ch = input.next();
		}
		input.pushback(ch);
		//return  CommentToken.make( firstChar.getLocation() );
		return findNextToken();
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Identifier and keyword lexical analysis	

	private Token scanIdentifier(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(firstChar.getCharacter());
		appendSubsequentIdentifierCharacters(buffer);

		String lexeme = buffer.toString();
		if(Keyword.isAKeyword(lexeme)) {
			return LextantToken.make(firstChar.getLocation(), lexeme, Keyword.forLexeme(lexeme));
		}
		else {
			if(buffer.length() >32 )
				tooLongIdentifier(firstChar);
			return IdentifierToken.make(firstChar.getLocation(), lexeme);
		}
	}
	private void appendSubsequentIdentifierCharacters(StringBuffer buffer) {
		LocatedChar c = input.next();
		while(c.isLowerCase() || c.isUpperCase() || c.isDigit() || 
				c.getCharacter() == '_' || c.getCharacter() == '~') {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		input.pushback(c);
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Punctuator lexical analysis	
	// old method left in to show a simple scanning method.
	// current method is the algorithm object PunctuatorScanner.java

	@SuppressWarnings("unused")
	private Token oldScanPunctuator(LocatedChar ch) {
		TextLocation location = ch.getLocation();
		
		switch(ch.getCharacter()) {
		case '*':
			return LextantToken.make(location, "*", Punctuator.MULTIPLY);
		case '+':
			return LextantToken.make(location, "+", Punctuator.ADD);
		case '>':
			return LextantToken.make(location, ">", Punctuator.GREATER);
		case ':':
			if(ch.getCharacter()=='=') {
				return LextantToken.make(location, ":=", Punctuator.ASSIGN);
			}
			else {
				throw new IllegalArgumentException("found : not followed by = in scanOperator");
			}
		case ',':
			return LextantToken.make(location, ",", Punctuator.SEPARATOR);
		case ';':
			return LextantToken.make(location, ";", Punctuator.TERMINATOR);
		default:
			throw new IllegalArgumentException("bad LocatedChar " + ch + "in scanOperator");
		}
	}

	

	//////////////////////////////////////////////////////////////////////////////
	// Character-classification routines specific to Grouse scanning.	

	private boolean isPunctuatorStart(LocatedChar lc) {
		char c = lc.getCharacter();
		return isPunctuatorStartingCharacter(c);
	}

	private boolean isEndOfInput(LocatedChar lc) {
		return lc == LocatedCharStream.FLAG_END_OF_INPUT;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Error-reporting	
	/*private void badNumberError() {
		GrouseLogger log = GrouseLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: expect number");
	}*/
	
	private void lexicalError(LocatedChar ch) {
		GrouseLogger log = GrouseLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: invalid character " + ch);
	}
	private void tooLongIdentifier(LocatedChar ch) {
		GrouseLogger log = GrouseLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: too long an identifier (at most 32 characters) :" + ch);
	}
	private void invalidCharacter(LocatedChar ch) {
		GrouseLogger log = GrouseLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: invalid character for a character or string variable" + ch);
	}
	
}
