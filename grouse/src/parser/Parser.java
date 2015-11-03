package parser;

import java.util.Arrays;

import logging.GrouseLogger;
import parseTree.*;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.UnaryOperatorNode; 
import parseTree.nodeTypes.CastOperatorNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.MainBlockNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.LetNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.CharConstantNode;
import parseTree.nodeTypes.FloatConstantNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.SeparatorNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.IfNode;
import parseTree.nodeTypes.WhileNode;
import semanticAnalyzer.types.PrimitiveType;
import parseTree.nodeTypes.ForNode;
import parseTree.nodeTypes.BlockNode;
import tokens.*;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import lexicalAnalyzer.Scanner;

public class Parser {
	private Scanner scanner;
	private Token nowReading;
	private Token previouslyRead;
	
	public static ParseNode parse(Scanner scanner) {
		Parser parser = new Parser(scanner);
		return parser.parse();
	}
	public Parser(Scanner scanner) {
		super();
		this.scanner = scanner;
	}
	
	public ParseNode parse() {
		readToken();
		return parseProgram();
	}

	////////////////////////////////////////////////////////////
	// "program" is the start symbol S
	// S -> MAIN mainBlock
	
	private ParseNode parseProgram() {
		if(!startsProgram(nowReading)) {
			return syntaxErrorNode("program");
		}
		ParseNode program = new ProgramNode(nowReading);
		
		expect(Keyword.MAIN);
		ParseNode mainBlock = parseMainBlock();
		program.appendChild(mainBlock);
		
		if(!(nowReading instanceof NullToken)) {
			return syntaxErrorNode("end of program");
		}
        		
		return program;
	}
	private boolean startsProgram(Token token) {
		return token.isLextant(Keyword.MAIN);
	}
	
	
	///////////////////////////////////////////////////////////
	// mainBlock
	
	// mainBlock -> { statement* }
	private ParseNode parseMainBlock() {
		if(!startsMainBlock(nowReading)) {
			return syntaxErrorNode("mainBlock");
		}
		ParseNode mainBlock = new MainBlockNode(nowReading);
		expect(Punctuator.OPEN_BRACE);
		
		while(startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			mainBlock.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return mainBlock;
	}
	private boolean startsMainBlock(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACE);
	}
	
	
	///////////////////////////////////////////////////////////
	// statements
	
	// Oct.30
	// statement-> declaration | printStmt | letStmt | ifStmt | whileStmt | forStmt | block
	private ParseNode parseStatement() {
		if(!startsStatement(nowReading)) {
			return syntaxErrorNode("statement");
		}
		if(startsDeclaration(nowReading)) {
			return parseDeclaration();
		}
		if(startsLetStatement(nowReading)) {
			return parseLetStatement();
		}
		if(startsPrintStatement(nowReading)) {
			return parsePrintStatement();
		}
		if(startsIfStatement(nowReading))
		{
			return parseIfStatement();
		}
		if(startsWhileStatement(nowReading))
		{
			return parseWhileStatement();
		}
		if(startsForStatement(nowReading))
		{
			return parseForStatement();
		}
		if(startsBlock(nowReading))
		{
			return parseBlock();
		}
		assert false : "bad token " + nowReading + " in parseStatement()";
		return null;
	}
	private boolean startsStatement(Token token) {
		return startsPrintStatement(token) || startsLetStatement(token) || startsDeclaration(token) ||
				startsIfStatement(token) || startsWhileStatement(token) || startsForStatement(token) || startsBlock(token);
	}
	
	// Oct.30
	// block -> { statement* }
	private ParseNode parseBlock() {
		BlockToken block_token = BlockToken.make(nowReading.getLocation());
		ParseNode block = new BlockNode(block_token);
		expect(Punctuator.OPEN_BRACE);
		while(startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			block.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return block;
	}
	private boolean startsBlock(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACE);
	}
	
	// ifStatement -> if(expr) block (else block)?
	private ParseNode parseIfStatement() {
		if(!startsIfStatement(nowReading)) {
			return syntaxErrorNode("if statement");
		}
		ParseNode result = new IfNode(nowReading);
		
		// parse expression
		readToken();
		expect(Punctuator.OPEN_PARENTHESIS);
		ParseNode expr = parseExpression();
		result.appendChild(expr);
		expect(Punctuator.CLOSE_PARENTHESIS);
		
		// parse block
		ParseNode block = parseBlock();
		result.appendChild(block);
		
		// parse else block
		if(nowReading.isLextant(Keyword.ELSE))
		{
			readToken();
			ParseNode block2 = parseBlock();
			result.appendChild(block2);
		}
		return result;
	}
	private boolean startsIfStatement(Token token) {
		return token.isLextant(Keyword.IF);
	}
	
	// WhileStatement -> While(expr) block
	private ParseNode parseWhileStatement() {
		if(!startsWhileStatement(nowReading)) {
			return syntaxErrorNode("while statement");
		}
		ParseNode result = new WhileNode(nowReading);
		
		// parse expression
		readToken();
		expect(Punctuator.OPEN_PARENTHESIS);
		ParseNode expr = parseExpression();
		result.appendChild(expr);
		expect(Punctuator.CLOSE_PARENTHESIS);
		
		// parse block
		ParseNode block = parseBlock();
		result.appendChild(block);
			
		return result;
		
	}
	private boolean startsWhileStatement(Token token) {
		return token.isLextant(Keyword.WHILE);
	}
	
	// ForStatement -> for(index identifier of expr)  block
	private ParseNode parseForStatement() {
		if(!startsForStatement(nowReading)) {
			return syntaxErrorNode("if statement");
		}
		ParseNode result = new ForNode(nowReading);
		
		// parse index expression
		readToken();
		expect(Punctuator.OPEN_PARENTHESIS);
		expect(Keyword.INDEX);
		
		ParseNode identifier = parseIdentifier();
		identifier.setType(PrimitiveType.INTEGER);
		result.appendChild(identifier);
		
		expect(Keyword.OF);
		
		ParseNode expr_list = parseExpression();
		result.appendChild(expr_list);
		expect(Punctuator.CLOSE_PARENTHESIS);
		
		// parse block
		ParseNode block = parseBlock();
		result.appendChild(block);
		
		return result;
	}
	private boolean startsForStatement(Token token) {
		return token.isLextant(Keyword.FOR);
	}
	
	// printStmt -> PRINT printExpressionList ;
	private ParseNode parsePrintStatement() {
		if(!startsPrintStatement(nowReading)) {
			return syntaxErrorNode("print statement");
		}
		PrintStatementNode result = new PrintStatementNode(nowReading);
		
		readToken();
		result = parsePrintExpressionList(result);
		
		expect(Punctuator.TERMINATOR);
		return result;
	}
	private boolean startsPrintStatement(Token token) {
		return token.isLextant(Keyword.PRINT);
	}	

	// This adds the printExpressions it parses to the children of the given parent
	// printExpressionList -> printExpression*   (note that this is nullable)

	private PrintStatementNode parsePrintExpressionList(PrintStatementNode parent) {
		while(startsPrintExpression(nowReading)) {
			parsePrintExpression(parent);
		}
		return parent;
	}
	

	// This adds the printExpression it parses to the children of the given parent
	// printExpression -> expr? ,? nl? 
	
	private void parsePrintExpression(PrintStatementNode parent) {
		if(startsExpression(nowReading)) {
			ParseNode child = parseExpression();
			parent.appendChild(child);
		}
		if(nowReading.isLextant(Punctuator.SEPARATOR)) {
			readToken();
			ParseNode child = new SeparatorNode(previouslyRead);
			parent.appendChild(child);
		}
		if(nowReading.isLextant(Keyword.NEWLINE)) {
			readToken();
			ParseNode child = new NewlineNode(previouslyRead);
			parent.appendChild(child);
		}
	}
	private boolean startsPrintExpression(Token token) {
		return startsExpression(token) || token.isLextant(Punctuator.SEPARATOR, Keyword.NEWLINE) ;
	}
	
	// declaration -> IMMUTABLE | VARIABLE identifier := expression ;
	private ParseNode parseDeclaration() {
		if(!startsDeclaration(nowReading)) {
			return syntaxErrorNode("declaration");
		}
		Token declarationToken = nowReading;
		readToken();
		
		ParseNode identifier = parseIdentifier();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		ParseNode node = DeclarationNode.withChildren(declarationToken, identifier, initializer);
		
		return node;
	}
	private boolean startsDeclaration(Token token) {
		return token.isLextant(Keyword.IMMUTABLE) || token.isLextant(Keyword.VARIABLE);
	}
	 // letStmt -> VARIABLE identifier := expression ; 
	private ParseNode parseLetStatement() {
		if(!startsLetStatement(nowReading)) {
			return syntaxErrorNode("let");
		}
		Token letToken = nowReading;
		readToken();
		
		ParseNode identifier = parseIdentifier();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		return LetNode.withChildren(letToken, identifier, initializer);
	}
	private boolean startsLetStatement(Token token) {
		return token.isLextant(Keyword.LET);
	}
	
	///////////////////////////////////////////////////////////
	// expressions
	// expr  -> expr1 | (expr1)
	// expr1 -> expr2 [> expr2]?
	// expr2 -> expr3 [+ expr3]*  (left-assoc)
	// expr3 -> expr4 [MULT expr4]*  (left-assoc)
	// expr4 -> literal
	// literal -> intNumber | identifier | booleanConstant
	
	// Oct.26 10:50pm
	// expr  -> expr0 
	// expr0 -> expr1 [&& | || expr1]*
	// expr1 -> expr2 [>|<|>=|<=|!=|== expr2]*
	// expr2 -> expr3 [+|- expr3]*  (left-assoc)
	// expr3 -> expr4 [MULT|DIV expr4]*  (left-assoc)
	// expr4 -> [!]expr5
	// expr5 -> literal | (expr) |  expr5 : type
	// literal -> intNumber | identifier | booleanConstant | charConstant | stringConstant
	
	// expr  -> expr0
	private ParseNode parseExpression() {
		
		if(!startsExpression(nowReading)) {
			return syntaxErrorNode("expression");
		}
		return parseExpression0();
	}
	
 	private boolean startsExpression(Token token) {
		return startsExpression0(token);
	}
 	
 	// Oct 26
 	// expr0 -> expr1 [&& | || expr1]*
 	private ParseNode parseExpression0() {
 		
 		if(!startsExpression(nowReading)) {
 			return syntaxErrorNode("expression<0>");
 		}
 		ParseNode left = parseExpression1();
 		while(nowReading.isLextant(Punctuator.BOOL_AND) || nowReading.isLextant(Punctuator.BOOL_OR)) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseExpression1();
			
			left =  BinaryOperatorNode.withChildren(compareToken, left, right);
		}
 		return left;
 	}
 	
 	private boolean startsExpression0(Token token) {
		return startsExpression1(token);
	}
 	
	// expr1 -> expr2 [ > | < | >= | <= | != | == expr2]*
	private ParseNode parseExpression1() {
		
		if(!startsExpression1(nowReading)) {
			return syntaxErrorNode("expression<1>");
		}
		
		ParseNode left = parseExpression2();
		// Oct.4 10:50pm
		while(	nowReading.isLextant(Punctuator.GREATER) || nowReading.isLextant(Punctuator.LESS) || 
				nowReading.isLextant(Punctuator.GREATER_OR_EQUAL) || nowReading.isLextant(Punctuator.LESS_OR_EQUAL) ||
				nowReading.isLextant(Punctuator.NOT_EQUAL) || nowReading.isLextant(Punctuator.EQUAL)	) 
		{
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseExpression2();
			
			left = BinaryOperatorNode.withChildren(compareToken, left, right);
		}
		return left;

	}
	private boolean startsExpression1(Token token) {
		return startsExpression2(token);
	}

	// expr2 -> expr3 [+|- expr3]*  (left-assoc)
	private ParseNode parseExpression2() {
		
		if(!startsExpression2(nowReading)) {
			return syntaxErrorNode("expression<2>");
		}
		
		ParseNode left = parseExpression3();
		while(nowReading.isLextant(Punctuator.ADD) || nowReading.isLextant(Punctuator.SUBTRACT)) {
			Token token = nowReading;
			readToken();
			ParseNode right = parseExpression3();
			
			left = BinaryOperatorNode.withChildren(token, left, right);
		}
		return left;
	}
	private boolean startsExpression2(Token token) {
		return startsExpression3(token);
	}	

	// expr3 -> expr4 [MULT | DIV expr4]*  (left-assoc)
	private ParseNode parseExpression3() {
		
		if(!startsExpression3(nowReading)) {
			return syntaxErrorNode("expression<3>");
		}
		
		ParseNode left = parseExpression4();
		while(nowReading.isLextant(Punctuator.MULTIPLY) || nowReading.isLextant(Punctuator.DIVIDE)) {
			Token token = nowReading;
			readToken();
			ParseNode right = parseExpression4();
			
			left = BinaryOperatorNode.withChildren(token, left, right);
		}
		return left;
	}
	private boolean startsExpression3(Token token) {
		return startsExpression4(token);
	}
	
	// expr4 -> [!]* expr4
	private ParseNode parseExpression4() {
		
		if(!startsExpression4(nowReading)) {
			return syntaxErrorNode("expression<4>");
		}
		
		if(nowReading.isLextant(Punctuator.BOOL_NOT)) {
			Token token = nowReading;
			readToken();
			ParseNode right = parseExpression4();
			
			right = UnaryOperatorNode.withChildren(token, right);
			
			return right;
		}
		return parseExpression5();
	
	}
	private boolean startsExpression4(Token token) {
		if(startsBoolNot(nowReading))
			return true;
		return startsExpression5(token);
	}
	private boolean startsBoolNot(Token token) {
		if(token.isLextant(Punctuator.BOOL_NOT))
			return true;
		else
			return false;
	}

	
	// expr5 -> literal | (expr) | expr5 : type
	private ParseNode parseExpression5() {
		
		// if there are parentheses
		if(startsParen(nowReading)) {
			readToken();
			ParseNode retNode = parseExpression();
			expect(Punctuator.CLOSE_PARENTHESIS);
			return retNode;
		}
		else {
			if(!startsExpression5(nowReading)) {
				return syntaxErrorNode("expression<5>");
			}
			ParseNode node = parseLiteral();
			
			if(nowReading.isLextant(Punctuator.CAST))
			{
				readToken();
				if(!isTypeKeyword(nowReading))
				{
					readToken();
					return syntaxErrorNode("cast type not exist");
				}
				else
				{
					node = CastOperatorNode.withChildren(nowReading, node);
					readToken();
				}
			}
			
			return node;
		}
		
	}
	private boolean startsExpression5(Token token) {
		if(startsParen(nowReading))
			return true;
		return startsLiteral(token);
	}
	private boolean startsParen(Token token) {
		if(token.isLextant(Punctuator.OPEN_PARENTHESIS))
			return true;
		else
			return false;
	}
	private boolean isTypeKeyword(Token token) {
		if (!token.isLextant(Keyword.BOOL))
			if (!token.isLextant(Keyword.CHAR))
				if (!token.isLextant(Keyword.STRING))
					if (!token.isLextant(Keyword.INT))
						return false;
		return true;
	}
	
	
	// literal -> number(integer) | float | identifier | booleanConstant 
	//										| charConstatnt | stringConstant
	// for convenience, include parenthesis into "literal" since they have the same associate priority
	private ParseNode parseLiteral() {
		
		
		if(!startsLiteral(nowReading)) {
			return syntaxErrorNode("literal");
		}
		
		if(startsIntNumber(nowReading)) {
			return parseIntNumber();
		}
		if(startsFloatNumber(nowReading)) {
			return parseFloatNumber();
		}
		if(startsIdentifier(nowReading)) {
			return parseIdentifier();
		}
		if(startsBooleanConstant(nowReading)) {
			return parseBooleanConstant();
		}
		if(startsCharConstant(nowReading)) {
			return parseCharConstant();
		}
		if(startsStringConstant(nowReading)) {
			return parseStringConstant();
		}		
		assert false : "bad token " + nowReading + " in parseLiteral()";
		return null;
	}
	private boolean startsLiteral(Token token) {
		return startsIntNumber(token) || startsIdentifier(token) || startsBooleanConstant(token) 
				|| startsFloatNumber(token) || startsCharConstant(token) || startsStringConstant(token);
	}

	// number (terminal)
	private ParseNode parseIntNumber() {
		if(!startsIntNumber(nowReading)) {
			return syntaxErrorNode("integer constant");
		}
		readToken();
		return new IntegerConstantNode(previouslyRead);
	}
	private boolean startsIntNumber(Token token) {
		return token instanceof NumberToken;
	}
	
	// float (terminal)
	private ParseNode parseFloatNumber() {
		if(!startsFloatNumber(nowReading)) {
			return syntaxErrorNode("float constant");
		}
		readToken();
		return new FloatConstantNode(previouslyRead);
	}
	private boolean startsFloatNumber(Token token) {
		return token instanceof FloatToken;
	}
	
	// identifier (terminal)
	private ParseNode parseIdentifier() {
		if(!startsIdentifier(nowReading)) {
			return syntaxErrorNode("identifier");
		}
		readToken();
		return new IdentifierNode(previouslyRead);
	}
	private boolean startsIdentifier(Token token) {
		return token instanceof IdentifierToken;
	}
	
	// char constant (terminal)
	private ParseNode parseCharConstant() {
		if(!startsCharConstant(nowReading)) {
			return syntaxErrorNode("identifier");
		}
		readToken();
		return new CharConstantNode(previouslyRead);
	}
	private boolean startsCharConstant(Token token) {
		return token instanceof CharToken;
	}
	
	// string constant (terminal)
	private ParseNode parseStringConstant() {
		if(!startsStringConstant(nowReading)) {
			return syntaxErrorNode("identifier");
		}
		readToken();
		return new StringConstantNode(previouslyRead);
	}
	private boolean startsStringConstant(Token token) {
		return token instanceof StringToken;
	}
	
	// boolean constant (terminal)
	private ParseNode parseBooleanConstant() {
		if(!startsBooleanConstant(nowReading)) {
			return syntaxErrorNode("boolean constant");
		}
		readToken();
		return new BooleanConstantNode(previouslyRead);
	}
	private boolean startsBooleanConstant(Token token) {
		return token.isLextant(Keyword.TRUE, Keyword.FALSE);
	}

	private void readToken() {
		previouslyRead = nowReading;
		nowReading = scanner.next();
	}
	
	// if the current token is one of the given lextants, read the next token.
	// otherwise, give a syntax error and read next token (to avoid endless looping).
	private void expect(Lextant ...lextants ) {
		if(!nowReading.isLextant(lextants)) {
			syntaxError(nowReading, "expecting " + Arrays.toString(lextants));
		}
		readToken();
	}	
	private ErrorNode syntaxErrorNode(String expectedSymbol) {
		syntaxError(nowReading, "expecting " + expectedSymbol);
		ErrorNode errorNode = new ErrorNode(nowReading);
		readToken();
		return errorNode;
	}
	private void syntaxError(Token token, String errorDescription) {
		String message = "" + token.getLocation() + " " + errorDescription;
		error(message);
	}
	private void error(String message) {
		GrouseLogger log = GrouseLogger.getLogger("compiler.Parser");
		log.severe("syntax error: " + message);
	}	
}

