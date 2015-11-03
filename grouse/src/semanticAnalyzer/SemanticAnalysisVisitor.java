package semanticAnalyzer;

import java.util.Arrays;
import java.util.List;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import logging.GrouseLogger;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import parseTree.nodeTypes.*;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import tokens.LextantToken;
import tokens.Token;

class SemanticAnalysisVisitor extends ParseNodeVisitor.Default {
	@Override
	public void visitLeave(ParseNode node) {
		throw new RuntimeException("Node class unimplemented in SemanticAnalysisVisitor: " + node.getClass());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// constructs larger than statements
	@Override
	public void visitEnter(ProgramNode node) {
		enterProgramScope(node);
	}
	public void visitLeave(ProgramNode node) {
		leaveScope(node);
	}
	public void visitEnter(MainBlockNode node) {
	}
	public void visitLeave(MainBlockNode node) {
	}
	
	///////////////////////////////////////////////////////////////////////////
	// helper methods for scoping.
	private void enterProgramScope(ParseNode node) {
		Scope scope = Scope.createProgramScope();
		node.setScope(scope);
	}	
	//@SuppressWarnings("unused")
	private void enterSubscope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createSubscope();
		node.setScope(scope);
	}
	private void leaveScope(ParseNode node) {
		node.getScope().leave();
	}
	
	///////////////////////////////////////////////////////////////////////////
	// statements and declarations
	@Override
	public void visitLeave(PrintStatementNode node) {
	}
	@Override
	public void visitEnter(IfNode node) {
		//enterSubscope(node);
	}
	public void visitLeave(IfNode node) {
		ParseNode expr = node.child(0);
		assert expr.getType() == PrimitiveType.BOOLEAN;
	}
	@Override
	public void visitEnter(WhileNode node) {
		//enterSubscope(node);
	}
	public void visitLeave(WhileNode node) {
		ParseNode expr = node.child(0);
		assert expr.getType() == PrimitiveType.BOOLEAN;
	}
	@Override
	public void visitLeave(ForNode node) {
		ParseNode index = node.child(0);
		assert index.getType() == PrimitiveType.INTEGER;
	}
	@Override
	public void visitEnter(BlockNode node) {
		enterSubscope(node);
	}
	public void visitLeave(BlockNode node) {
		leaveScope(node);
	}
	
	@Override
	public void visitLeave(DeclarationNode node) {
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		ParseNode initializer = node.child(1);
		
		Type declarationType = initializer.getType();
		node.setType(declarationType);
		
		identifier.setType(declarationType);
		Boolean isImm = true;
		if(node.getToken().isLextant(Keyword.VARIABLE))
			isImm = false;
		addBinding(identifier, declarationType, isImm);
		
	}
	@Override
	public void visitLeave(LetNode node) {
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		ParseNode initializer = node.child(1);
		
		// check if the identifier is defined as a variable
		
		
		// check if the type of the identifier and the expression is in accord
		Type type = initializer.getType();
		if(checkletStmt(identifier, type, false)==false)
		{
			node.setType(PrimitiveType.ERROR);
		}
		else
		{
			Type type2 = identifier.getType();
			node.setType(type2);
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	// expressions
	@Override
	public void visitLeave(BinaryOperatorNode node) {
		assert node.nChildren() == 2;
		ParseNode left  = node.child(0);
		ParseNode right = node.child(1);
		List<Type> childTypes = Arrays.asList(left.getType(), right.getType());
		
		Lextant operator = operatorFor(node);
		FunctionSignature signature = FunctionSignature.signatureOf(operator);
		
		
		if(signature.accepts(childTypes)) {
			node.setType(signature.resultType(childTypes));
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
		
	}
	private Lextant operatorFor(BinaryOperatorNode node) {
		LextantToken token = (LextantToken) node.getToken();
		return token.getLextant();
	}
	
	public void visitLeave(UnaryOperatorNode node) {
		assert node.nChildren() == 1;
		ParseNode operand  = node.child(0);
		List<Type> childTypes = Arrays.asList(operand.getType());
		
		Lextant operator = operatorFor(node);
		FunctionSignature signature = FunctionSignature.signatureOf(operator);
		
		
		if(signature.accepts(childTypes)) {
			node.setType(signature.resultType(childTypes));
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
		
	}
	private Lextant operatorFor(UnaryOperatorNode node) {
		LextantToken token = (LextantToken) node.getToken();
		return token.getLextant();
	}

	///////////////////////////////////////////////////////////////////////////
	// simple leaf nodes
	@Override
	public void visit(BooleanConstantNode node) {
		node.setType(PrimitiveType.BOOLEAN);
	}
	@Override
	public void visit(ErrorNode node) {
		node.setType(PrimitiveType.ERROR);
	}
	@Override
	public void visit(IntegerConstantNode node) {
		node.setType(PrimitiveType.INTEGER);
	}
	@Override
	public void visit(FloatConstantNode node) {
		node.setType(PrimitiveType.FLOAT);
	}
	@Override
	public void visit(CharConstantNode node) {
		node.setType(PrimitiveType.CHAR);
	}
	@Override
	public void visit(StringConstantNode node) {
		node.setType(PrimitiveType.STRING);
	}
	@Override
	public void visit(NewlineNode node) {
//		node.setType(PrimitiveType.INTEGER);
	}
	@Override
	public void visit(SeparatorNode node) {
//		node.setType(PrimitiveType.INTEGER);
	}
	///////////////////////////////////////////////////////////////////////////
	// IdentifierNodes, with helper methods
	@Override
	public void visit(IdentifierNode node) {
		if(!isBeingDeclared(node)) {		
			Binding binding = node.findVariableBinding();
			
			node.setType(binding.getType());
			node.setBinding(binding);
		}
		// Oct.31
		// else parent DeclarationNode does the processing.
		/*ParseNode temp = node.getParent();
		while(temp != null) {
			
		}*/
	}
	private boolean isBeingDeclared(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof DeclarationNode) && (node == parent.child(0));
	}
	private void addBinding(IdentifierNode identifierNode, Type type, Boolean isImm) {
		Scope scope = identifierNode.getLocalScope();
		Binding binding = scope.createBinding(identifierNode, type);
		binding.setImm(isImm);
		identifierNode.setBinding(binding);
	}
	///Oct.24
	// including check the immutable type
	private boolean checkletStmt(IdentifierNode identifierNode, Type type, Boolean isImm) {
		Binding binding = identifierNode.getBinding();
		Type id_type = binding.getType();
		if(id_type == PrimitiveType.ERROR)
			return false;
		
		Boolean id_isImm = binding.getImm();
		if (id_type != type)
		{
			letExprError(identifierNode);
			return false;
		}
		
		// if it is a variable
		if(isImm == false)
			if(id_isImm != false)
			{
				immCheckError(identifierNode);
				return false;
			}
			
		return true;
	}
	
	///////////////////////////////////////////////////////////////////////////
	// error logging/printing
	private void letExprError(IdentifierNode node) {
		Token token = node.getToken();
		
		logError("reassigned identifier \"" + token.getLexeme() + "\" doesn't match the type of the expression"
				+ " at " + token.getLocation());	
	}
	
	private void immCheckError(ParseNode node) {
		Token token = node.getToken();
		
		logError("reassigned identifier \"" + token.getLexeme() + "\" at " + token.getLocation() 
			+ " is not defined as a variable ");	
	}
	
	private void typeCheckError(ParseNode node, List<Type> operandTypes) {
		Token token = node.getToken();
		
		logError("operator \"" + token.getLexeme() + "\" not defined for types " 
				 + operandTypes  + " at " + token.getLocation());	
	}
	
	/*
	private void scopeError(ParseNode identifier) {
		Token token = identifier.getToken();
		
		logError("identifier " + token.getLexeme() + " not defined for the scope " 
				 + " at " + token.getLocation());
	}*/
	
	private void logError(String message) {
		GrouseLogger log = GrouseLogger.getLogger("compiler.semanticAnalyzer");
		log.severe(message);
	}
}





