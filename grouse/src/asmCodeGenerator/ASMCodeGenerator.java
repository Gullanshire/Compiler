package asmCodeGenerator;

import java.util.HashMap;
import java.util.Map;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.runtime.RunTime;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
import parseTree.nodeTypes.*;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

// do not call the code generator if any errors have occurred during analysis.
public class ASMCodeGenerator {
	private static Labeller labeller = new Labeller();

	ParseNode root;

	public static ASMCodeFragment generate(ParseNode syntaxTree) {
		ASMCodeGenerator codeGenerator = new ASMCodeGenerator(syntaxTree);
		return codeGenerator.makeASM();
	}
	public ASMCodeGenerator(ParseNode root) {
		super();
		this.root = root;
	}
	public static Labeller getLabeller() {
		return labeller;
	}
	
	public ASMCodeFragment makeASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		
		code.append( RunTime.getEnvironment() );
		code.append( globalVariableBlockASM() );
		code.append( programASM() );
//		code.append( MemoryManager.codeForAfterApplication() );
		
		return code;
	}
	private ASMCodeFragment globalVariableBlockASM() {
		assert root.hasScope();
		Scope scope = root.getScope();
		int globalBlockSize = scope.getAllocatedSize();
		
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.add(DLabel, RunTime.GLOBAL_MEMORY_BLOCK);
		code.add(DataZ, globalBlockSize);
		return code;
	}
	private ASMCodeFragment programASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		
		code.add(    Label, RunTime.MAIN_PROGRAM_LABEL);
		code.append( programCode());
		code.add(    Halt );
		
		return code;
	}
	private ASMCodeFragment programCode() {
		CodeVisitor visitor = new CodeVisitor();
		root.accept(visitor);
		return visitor.removeRootCode(root);
	}


	private class CodeVisitor extends ParseNodeVisitor.Default {
		private Map<ParseNode, ASMCodeFragment> codeMap;
		ASMCodeFragment code;
		
		public CodeVisitor() {
			codeMap = new HashMap<ParseNode, ASMCodeFragment>();
		}


		////////////////////////////////////////////////////////////////////
        // Make the field "code" refer to a new fragment of different sorts.
		private void newAddressCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_ADDRESS);
			codeMap.put(node, code);
		}
		private void newValueCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VALUE);
			codeMap.put(node, code);
		}
		private void newVoidCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VOID);
			codeMap.put(node, code);
		}

	    ////////////////////////////////////////////////////////////////////
        // Get code from the map.
		private ASMCodeFragment getAndRemoveCode(ParseNode node) {
			ASMCodeFragment result = codeMap.get(node);
			codeMap.remove(result);
			return result;
		}
	    public  ASMCodeFragment removeRootCode(ParseNode tree) {
			return getAndRemoveCode(tree);
		}		
		private ASMCodeFragment removeValueCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			makeFragmentValueCode(frag, node);
			return frag;
		}		
		private ASMCodeFragment removeAddressCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isAddress();
			return frag;
		}		
		private ASMCodeFragment removeVoidCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isVoid();
			return frag;
		}
		
	    ////////////////////////////////////////////////////////////////////
        // convert code to value-generating code.
		private void makeFragmentValueCode(ASMCodeFragment code, ParseNode node) {
			assert !code.isVoid();
			
			if(code.isAddress()) {
				turnAddressIntoValue(code, node);
			}	
		}
		private void turnAddressIntoValue(ASMCodeFragment code, ParseNode node) {
			if(node.getType() == PrimitiveType.INTEGER) {
				code.add(LoadI);
			}	
			else if(node.getType() == PrimitiveType.BOOLEAN) {
				code.add(LoadC);
			}	
			else if(node.getType() == PrimitiveType.FLOAT) {
				code.add(LoadF);
			}
			else if(node.getType() == PrimitiveType.CHAR)	{
				code.add(LoadC);
			}
			else if(node.getType() == PrimitiveType.STRING)	{
				code.add(LoadI);
			}
			else {
				assert false : "node " + node;
			}
			code.markAsValue();
		}
		
	    ////////////////////////////////////////////////////////////////////
        // ensures all types of ParseNode in given AST have at least a visitLeave	
		public void visitLeave(ParseNode node) {
			assert false : "node " + node + " not handled in ASMCodeGenerator";
		}
		
		///////////////////////////////////////////////////////////////////////////
		// constructs larger than statements
		public void visitLeave(ProgramNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}
		public void visitLeave(MainBlockNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}

		///////////////////////////////////////////////////////////////////////////
		// statements and declarations
		
		//Oct.30
		public void visitLeave(BlockNode node) {
			newVoidCode(node);
			
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}
		public void visitLeave(IfNode node) {
			newVoidCode(node);
			
			ParseNode expr = node.child(0);
			ParseNode block = node.child(1);

			ASMCodeFragment exprCode = removeValueCode(expr);
			ASMCodeFragment blockCode = removeVoidCode(block);
			
			String ifLabel = labeller.newLabel("-if-", "");
			String elseLabel = labeller.newLabelSameNumber("-else-", "");
			String joinLabel = labeller.newLabelSameNumber("-if-else-join-", "");
			
			code.append(exprCode);
			code.add(JumpTrue, ifLabel);
			if(node.nChildren() == 3)
			{
				ParseNode block2 = node.child(2);
				ASMCodeFragment blockCode2 = removeVoidCode(block2);
				code.add(Label,elseLabel);
				code.append(blockCode2);
			}
			code.add(Jump,joinLabel);
			code.add(Label, ifLabel);
			code.append(blockCode);
			code.add(Label,joinLabel);
		}	
		public void visitLeave(WhileNode node) {
			newVoidCode(node);
			
			ParseNode expr = node.child(0);
			ParseNode block = node.child(1);

			ASMCodeFragment exprCode = removeValueCode(expr);
			ASMCodeFragment blockCode = removeVoidCode(block);
			
			String whileLabel = labeller.newLabel("-while-", "");
			String endLabel = labeller.newLabelSameNumber("-while-end-", "");
			
			code.add(Label,whileLabel);
			code.append(exprCode);
			code.add(JumpFalse, endLabel);
			code.append(blockCode);
			code.add(Jump,whileLabel);
			code.add(Label,endLabel);
		}
		public void visitLeave(PrintStatementNode node) {
			newVoidCode(node);

			for(ParseNode child : node.getChildren()) {
				if(child instanceof NewlineNode || child instanceof SeparatorNode) {
					ASMCodeFragment childCode = removeVoidCode(child);
					code.append(childCode);
				}
				else {
					appendPrintCode(child);
				}
			}
		}

		public void visit(NewlineNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.NEWLINE_PRINT_FORMAT);
			code.add(Printf);
		}
		public void visit(SeparatorNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.SEPARATOR_PRINT_FORMAT);
			code.add(Printf);
		}
		private void appendPrintCode(ParseNode node) {
			String format = printFormat(node.getType());

			code.append(removeValueCode(node));
			convertToStringIfBoolean(node);
			code.add(PushD, format);
			code.add(Printf);
		}
		private void convertToStringIfBoolean(ParseNode node) {
			if(node.getType() != PrimitiveType.BOOLEAN) {
				return;
			}
			
			String trueLabel = labeller.newLabel("-print-boolean-true", "");
			String endLabel = labeller.newLabelSameNumber("-print-boolean-join", "");

			code.add(JumpTrue, trueLabel);
			code.add(PushD, RunTime.BOOLEAN_FALSE_STRING);
			code.add(Jump, endLabel);
			code.add(Label, trueLabel);
			code.add(PushD, RunTime.BOOLEAN_TRUE_STRING);
			code.add(Label, endLabel);
		}		
		private String printFormat(Type type) {
			assert type instanceof PrimitiveType;
			
			switch((PrimitiveType)type) {
			case INTEGER:	return RunTime.INTEGER_PRINT_FORMAT;
			case FLOAT: 	return RunTime.FLOAT_PRINT_FORMAT;
			case BOOLEAN:	return RunTime.BOOLEAN_PRINT_FORMAT;
			case CHAR: 		return RunTime.CHAR_PRINT_FORMAT;
			case STRING:	return RunTime.STRING_PRINT_FORMAT;
			default:		
				assert false : "Type " + type + " unimplemented in ASMCodeGenerator.printFormat()";
				return "";
			}
		}

		public void visitLeave(DeclarationNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));	
			ASMCodeFragment rvalue = removeValueCode(node.child(1));
			
			code.append(lvalue);
			code.append(rvalue);
			
			Type type = node.getType();
			code.add(opcodeForStore(type));
		}
		
		public void visitLeave(LetNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));	
			ASMCodeFragment rvalue = removeValueCode(node.child(1));
			
			code.append(lvalue);
			code.append(rvalue);
			Type type = node.getType();
			code.add(opcodeForStore(type));
		}
		
		private ASMOpcode opcodeForStore(Type type) {
			if(type == PrimitiveType.INTEGER) {
				return StoreI;
			}
			if(type == PrimitiveType.BOOLEAN) {
				return StoreC;
			}
			if(type == PrimitiveType.FLOAT)	  {
				return StoreF;
			}
			if(type == PrimitiveType.CHAR)	  {
				return StoreC;
			}
			if(type == PrimitiveType.STRING)  {
				return StoreI;
			}
			assert false: "Type " + type + " unimplemented in opcodeForStore()";
			return null;
		}
		///////////////////////////////////////////////////////////////////////////
		// UnaryOperatorNode Oct.29
		//
		public void visitLeave(UnaryOperatorNode node)
		{
			Lextant operator = node.getOperator();
			if(operator == Punctuator.BOOL_NOT)	{
				assert node.child(0).getType() == PrimitiveType.BOOLEAN;
				visitUnaryOperatorNode(node);
			}
				
		}
		
		// boolean unary operator
		private void visitUnaryOperatorNode(UnaryOperatorNode node) {
			newValueCode(node);
			Lextant operator = node.getOperator();
			Type type = node.getType();
					
			assert (operator == Punctuator.BOOL_NOT);
			assert type == PrimitiveType.BOOLEAN;
			
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			
			code.append(arg1);
			if(operator == Punctuator.BOOL_NOT)
			{
				ASMOpcode opcode = opcodeForOperator(operator, type);
				code.add(opcode);			
			}

		}
		
		
		///////////////////////////////////////////////////////////////////////////
		// expressions Oct.5 12:25pm
		//		visitComparisonOperatorNode1 - GREATER | LESS
		//		visitComparisonOperatorNode2 - GREATER_OR_EQUAL | LESS_OR_EQUAL
		//  	visitComparisonOperatorNode3 - NOT_EQUAL | EQUAL
		public void visitLeave(BinaryOperatorNode node) {
			Lextant operator = node.getOperator();
			
			// comparison operators, must be the same type
			if(operator == Punctuator.GREATER || operator == Punctuator.LESS) {
				assert node.child(0).getType() == node.child(1).getType();
				visitComparisonOperatorNode1(node, operator);
			}
			else if(operator == Punctuator.GREATER_OR_EQUAL || operator == Punctuator.LESS_OR_EQUAL) {
				assert node.child(0).getType() == node.child(1).getType();
				visitComparisonOperatorNode2(node, operator);
			}
			else if(operator == Punctuator.NOT_EQUAL || operator == Punctuator.EQUAL) {
				assert node.child(0).getType() == node.child(1).getType();
				visitComparisonOperatorNode3(node, operator);
			}
			else {
					if (node.getType() == PrimitiveType.BOOLEAN)
						visitBoolBinaryOperatorNode(node);
					else// arithmatic operators and boolean operators
						visitNormalBinaryOperatorNode(node);
			}
		}
		//	visitComparisonOperatorNode1 - GREATER | LESS
		private void visitComparisonOperatorNode1(BinaryOperatorNode node,
				Lextant operator) {
			
			Type operand_type = node.child(0).getType();
			
			
			ASMCodeFragment arg1;
			ASMCodeFragment arg2;
			
			if(operator == Punctuator.GREATER) {
				arg1 = removeValueCode(node.child(0));
				arg2 = removeValueCode(node.child(1));
			}
			else { //(operator == Punctuator.LESS)
				arg1 = removeValueCode(node.child(1));
				arg2 = removeValueCode(node.child(0));
			}

			String startLabel = labeller.newLabel("-compare-arg1-", "");
			String arg2Label  = labeller.newLabelSameNumber("-compare-arg2-", "");
			String subLabel   = labeller.newLabelSameNumber("-compare-sub-", "");
			String trueLabel  = labeller.newLabelSameNumber("-compare-true-", "");
			String falseLabel = labeller.newLabelSameNumber("-compare-false-", "");
			String joinLabel  = labeller.newLabelSameNumber("-compare-join-", "");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);
			if (operand_type == PrimitiveType.FLOAT)
			{
				code.add(FSubtract);
				code.add(JumpFPos, trueLabel);
			}
			else //if(operand_type == PrimitiveType.INTEGER || operand_type == PrimitiveType.CHAR)
			{
				code.add(Subtract);
				code.add(JumpPos, trueLabel);
			}
		
			code.add(Jump, falseLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		//	visitComparisonOperatorNode2 - GREATER_OR_EQUAL | LESS_OR_EQUAL
		private void visitComparisonOperatorNode2(BinaryOperatorNode node,
				Lextant operator) {
			
			Type operand_type = node.child(0).getType();
			
			ASMCodeFragment arg1;
			ASMCodeFragment arg2;
			if(operator == Punctuator.GREATER_OR_EQUAL) {
				arg1 = removeValueCode(node.child(0));
				arg2 = removeValueCode(node.child(1));
			}
			else { //(operator == Punctuator.LESS_OR_EQUAL)
				arg1 = removeValueCode(node.child(1));
				arg2 = removeValueCode(node.child(0));
			};
			
			String startLabel = labeller.newLabel("-compare-arg1-", "");
			String arg2Label  = labeller.newLabelSameNumber("-compare-arg2-", "");
			String subLabel   = labeller.newLabelSameNumber("-compare-sub-", "");
			String trueLabel  = labeller.newLabelSameNumber("-compare-true-", "");
			String falseLabel = labeller.newLabelSameNumber("-compare-false-", "");
			String joinLabel  = labeller.newLabelSameNumber("-compare-join-", "");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);
			if (operand_type == PrimitiveType.FLOAT)
			{
				code.add(FSubtract);
				code.add(JumpFNeg, falseLabel);
			}
			else  //if(operand_type == PrimitiveType.INTEGER || operand_type == PrimitiveType.CHAR)
			{
				code.add(Subtract);
				code.add(JumpNeg, falseLabel);
			}
			code.add(Jump, trueLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);

		}
		//visitComparisonOperatorNode3 - NOT_EQUAL | EQUAL
		private void visitComparisonOperatorNode3(BinaryOperatorNode node,
				Lextant operator) {
			
			Type operand_type = node.child(0).getType();
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			String startLabel = labeller.newLabel("-compare-arg1-", "");
			String arg2Label  = labeller.newLabelSameNumber("-compare-arg2-", "");
			String subLabel   = labeller.newLabelSameNumber("-compare-sub-", "");
			String trueLabel  = labeller.newLabelSameNumber("-compare-true-", "");
			String falseLabel = labeller.newLabelSameNumber("-compare-false-", "");
			String joinLabel  = labeller.newLabelSameNumber("-compare-join-", "");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);
			
			if (operand_type == PrimitiveType.FLOAT) 
			{
				code.add(FSubtract);
				if(operator == Punctuator.NOT_EQUAL)
				{
					code.add(JumpFZero, falseLabel);
					code.add(Jump, trueLabel);
				}
				else //(operator == Punctuator.EQUAL)
				{
					code.add(JumpFZero, trueLabel);
					code.add(Jump, falseLabel);
				}
			}
			else if(operand_type == PrimitiveType.STRING) 
			{
				String index = labeller.newLabelSameNumber("-compare-index-", "");
				String pop_trueLabel = labeller.newLabelSameNumber("-pop-true-", "");
				String pop_falseLabel = labeller.newLabelSameNumber("-pop-false-", "");
				String endofstring = labeller.newLabelSameNumber("-end-of-string-test-", "");
				
				// 1. Loop header, index
				code.add(DLabel,index);
				code.add(DataI, 0);
				
				// char1
				code.add(PushD, index);
				code.add(LoadI);
				code.add(Add);
				code.add(LoadC);
				
				// char2
				code.add(Exchange);
				code.add(PushD, index);
				code.add(LoadI);
				code.add(Add);
				code.add(LoadC);
				
				code.add(Duplicate);
				code.add(JumpFalse, endofstring);
				code.add(Exchange);
				code.add(Duplicate);
				if(operator == Punctuator.NOT_EQUAL)
				{
					code.add(JumpFalse, pop_trueLabel);
				}
				else
				{
					code.add(JumpFalse, pop_falseLabel);
				}
				code.add(Subtract);
				if(operator == Punctuator.NOT_EQUAL)
				{
					code.add(JumpTrue, trueLabel);
				}
				else
				{
					code.add(JumpTrue, falseLabel);
				}
				code.add(PushD,	index);							// [&i]
				code.add(Duplicate);							// [&i &i]
				code.add(LoadI);								// [&i i]
				code.add(PushI,1);								// [&i i 1]
				code.add(Add);									// [&i i+1]
				code.add(StoreI);		
				code.add(Jump, startLabel);					
				
				// 
				code.add(Label, pop_trueLabel);
				code.add(Pop);
				code.add(Pop);
				code.add(Jump, trueLabel);
				
				//
				code.add(Label,pop_falseLabel);
				code.add(Pop);
				code.add(Pop);
				code.add(Jump, trueLabel);
				
				// 2. End of String Label
				code.add(Label, endofstring);
				code.add(Pop);
				if(operator == Punctuator.NOT_EQUAL)
				{
					code.add(JumpFalse, falseLabel);
					code.add(Jump, trueLabel);
				}
				else
				{
					code.add(JumpFalse, trueLabel);
					code.add(Jump, falseLabel);
				}
	
			}
			else //if(operator_type == PrimitiveType.INTEGER || operator_type == PrimitiveType.CHAR || operator_type == PrimitiveType.BOOLEAN)
			{
				code.add(Subtract);
				if(operator == Punctuator.NOT_EQUAL)
				{
					code.add(JumpFalse, falseLabel);
					code.add(Jump, trueLabel);
				}
				else //(operator == Punctuator.EQUAL)
				{
					code.add(JumpFalse, trueLabel);
					code.add(Jump, falseLabel);
				}
			}
			
			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		
		//Oct.29
		// boolean operator
		private void visitBoolBinaryOperatorNode(BinaryOperatorNode node) {
			newValueCode(node);
			Lextant operator = node.getOperator();
			Type type = node.getType();
			
			assert (operator == Punctuator.BOOL_AND || operator == Punctuator.BOOL_OR);
			assert type == PrimitiveType.BOOLEAN;
			
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			

			String trueLabel  = labeller.newLabel("-short-circuit-true-", "");
			String falseLabel = labeller.newLabelSameNumber("-short-circuit-false-", "");
			String joinLabel = labeller.newLabelSameNumber("-short-circuit-join-", "");
			String short_circuit_fail = labeller.newLabelSameNumber("-short-circuit-fail-", "");
			
			code.append(arg1);
			
			// short circuit
			code.add(Duplicate);
			if(operator == Punctuator.BOOL_AND)
			{
				code.add(JumpFalse,falseLabel);
			}
			else
			{
				code.add(JumpTrue,trueLabel);
			}
			code.add(Jump, short_circuit_fail);
			code.add(Label, trueLabel);
			code.add(Pop);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(Pop);
			code.add(PushI, 0);
			
			code.add(Label,short_circuit_fail);
			code.append(arg2);
			
			ASMOpcode opcode = opcodeForOperator(operator, type);
			code.add(opcode);							
			
			code.add(Label, joinLabel);
		}				
		
		// arithmetic operator
		private void visitNormalBinaryOperatorNode(BinaryOperatorNode node) {
			newValueCode(node);
			
			boolean isFloat = (node.getType() == PrimitiveType.FLOAT);
			
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			code.append(arg1);
			code.append(arg2);
			
			if (isFloat)
				if (node.child(0).getType() == PrimitiveType.INTEGER )
				{
					code.add(Exchange);
					code.add(ConvertF);
					code.add(Exchange);
				}
				else if(node.child(1).getType() == PrimitiveType.INTEGER)
					code.add(ConvertF);
			
			ASMOpcode opcode = opcodeForOperator(node.getOperator(), node.getType());
			if(node.getOperator()==Punctuator.DIVIDE)
			{
				String divide_by_zero = labeller.newLabel("-divide-by-zero-","");
				String safe_divisor = labeller.newLabelSameNumber("-safe-divisor－","");
				code.add(Duplicate);
				if(isFloat)
					code.add(JumpFZero, divide_by_zero);
				else
					code.add(JumpFalse, divide_by_zero);
				
				code.add(Jump,safe_divisor);
				code.add(Label,divide_by_zero);
				if(isFloat)
				{
					code.add(Jump,RunTime.FLOAT_DIVIDE_BY_ZERO_RUNTIME_ERROR);
				}
				else
				{
					code.add(Jump, RunTime.INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR);
				}
				code.add(Label,safe_divisor);
			}
			code.add(opcode);							// type-dependent!
		}
		
		private ASMOpcode opcodeForOperator(Lextant lextant, Type type) {
			assert(lextant instanceof Punctuator);
			assert(type instanceof Type);
			
			boolean isFloat = (type == PrimitiveType.FLOAT);
			boolean isBool = (type == PrimitiveType.BOOLEAN);
			
			Punctuator punctuator = (Punctuator)lextant;
			if(isBool)
				switch(punctuator) {
					case BOOL_AND:		return And;
					case BOOL_OR:		return Or;
					case BOOL_NOT:		return BNegate;
					default:
						assert false : "unimplemented operator in opcodeForOperator";
				}
			
			else if (isFloat) 
				switch(punctuator) {
					case ADD: 	   		return FAdd;				
					case SUBTRACT:      return FSubtract;		
					case MULTIPLY: 		return FMultiply;		
					case DIVIDE:		return FDivide;	
					default:
						assert false : "unimplemented operator in opcodeForOperator";
				}
			else
				switch(punctuator) {
					case ADD: 	   		return Add;				
					case SUBTRACT:      return Subtract;		
					case MULTIPLY: 		return Multiply;		
					case DIVIDE:		return Divide;	
					default:
						assert false : "unimplemented operator in opcodeForOperator";
				}
			
			return null;
		}

		///////////////////////////////////////////////////////////////////////////
		// leaf nodes (ErrorNode not necessary)
		public void visit(BooleanConstantNode node) {
			newValueCode(node);
			code.add(PushI, node.getValue() ? 1 : 0);
		}
		public void visit(IdentifierNode node) {
			newAddressCode(node);
			Binding binding = node.getBinding();
			
			binding.generateAddress(code);
		}		
		public void visit(IntegerConstantNode node) {
			newValueCode(node);
			
			code.add(PushI, node.getValueInt());
		}
		public void visit(FloatConstantNode node) {
			newValueCode(node);
			
			code.add(PushF, node.getValueFloat());
		}
		public void visit(CharConstantNode node) {
			newValueCode(node);
			
			code.add(PushI, (int) node.getContChar());
		}
		public void visit(StringConstantNode node) {
			newValueCode(node);
			
			String str_cont = node.getContString();
			String str_label = labeller.newLabel("-string-label-","");
			code.add(DLabel, str_label);
			
			char[] array = str_cont.toCharArray();
			char ch = '0';
			for (int i=0; i<array.length; i++) {
				ch = array[i];
				if (ch == '\\' && (i+1)<array.length) {
					char nextch = array[i+1];
					if (nextch == 'n')
						code.add(DataC, 10);
					i++;continue;
				}
				else
					code.add(DataC,(int)ch);
			}
			
			code.add(DataC, 0);
			code.add(PushD, str_label);
		}
		
		
	}

}
