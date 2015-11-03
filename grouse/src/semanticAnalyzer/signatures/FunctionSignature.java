package semanticAnalyzer.signatures;

import java.util.List;

import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;

//immutable
public class FunctionSignature {
	private static final boolean ALL_TYPES_ACCEPT_ERROR_TYPES = true;
	
	// This is the array all of acceptable types
	// for example, for Operator ADD
	// Type[] = INTEGER, INTEGER,INTEGER, FLOAT, FLOAT, FLOAT
	private Type[] paramTypes;
	
	// accordingly, there are various result type
	private Type[] resultType;
	
	// operands number
	private int operandNum;
	
	// number of cases, caseCount*(operandNum+1) is equal to length(paramTypes) + length(resultType)
	private int caseCount;
	
	Object whichVariant;
	
	
	///////////////////////////////////////////////////////////////
	// construction
	
	public FunctionSignature(Object whichVariant, int caseCnt, int opeNum, Type ...types) {
		assert(types.length >= 1);
		this.caseCount = caseCnt;
		this.operandNum = opeNum; 
		// store parameter types and result types
		storeTypes(types);
		this.whichVariant = whichVariant;
	}
	
	private void storeTypes(Type[] types) {
		
		paramTypes = new Type[caseCount*operandNum];
		resultType = new Type[caseCount];
	
		int j=0;
		int k=0;
		for(int i=0; i<types.length; i++) {
			
			if((i+1)%(operandNum+1) == 0)
			{
				// result type is extracted from every (operandNum+1)-th cell of input
				resultType[j] = types[i];j++;
			}
			else
			{
				paramTypes[k] = types[i];k++;
			}
		}
		
	}
		
	///////////////////////////////////////////////////////////////
	// accessors
	
	public Object getVariant() {
		return whichVariant;
	}
	
	public Type resultType(List<Type> types) {
		
		for (int i=0; i<caseCount;i++)
		{
			boolean check = true;
			for (int j=0;j<operandNum;j++)
			{
				if  ( !assignableTo(paramTypes[j+i*operandNum], types.get(j)) )
				{
					check = false;
					break;
				}
			}
			if (check)
				return resultType[i];
		}
		return PrimitiveType.ERROR;
	}
	public boolean isNull() {
		return false;
	}
	
	
	///////////////////////////////////////////////////////////////
	// main query
	// Modified Oct.6
	public boolean accepts(List<Type> types) {
		if(types.size() != operandNum) {
			return false;
		}
		
		boolean retVal = true;
		for(int i=0; i<caseCount; i++)
		{
			retVal = true;
			for(int j=0; j<operandNum; j++) {
				if(!assignableTo(paramTypes[j+i*operandNum], types.get(j)))
				{
					retVal = false;
					break;
				}
				else
					continue;
			}
			
			if (retVal == true)
				return retVal;
		}		
		return retVal;
	}
	private boolean assignableTo(Type variableType, Type valueType) {
		
		if(valueType == PrimitiveType.ERROR && ALL_TYPES_ACCEPT_ERROR_TYPES) {
			return true;
		}	
		
		return variableType.equals(valueType);
	}
	
	// Null object pattern
	private static FunctionSignature neverMatchedSignature = new FunctionSignature(1, 1, 1, PrimitiveType.ERROR) {
		public boolean accepts(List<Type> types) {
			return false;
		}
		public boolean isNull() {
			return true;
		}
	};
	public static FunctionSignature nullInstance() {
		return neverMatchedSignature;
	}
	
	///////////////////////////////////////////////////////////////////
	// Signatures for grouse-0 operators
	// this section will probably disappear in grouse-1 (in favor of FunctionSignatures)
	
	// Oct.4 11:40pm
	
	// public FunctionSignature(Object whichVariant, int caseCnt, int operandNum, Type ...types)
	// arithmetic
	private static FunctionSignature addSignature = 
			new FunctionSignature(1, 4, 2, 	PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER,
											PrimitiveType.INTEGER, PrimitiveType.FLOAT, PrimitiveType.FLOAT,
											PrimitiveType.FLOAT, PrimitiveType.INTEGER, PrimitiveType.FLOAT,
											PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT);
	private static FunctionSignature subtractSignature = 
			new FunctionSignature(1, 4, 2, 	PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER,
											PrimitiveType.INTEGER, PrimitiveType.FLOAT, PrimitiveType.FLOAT,
											PrimitiveType.FLOAT, PrimitiveType.INTEGER, PrimitiveType.FLOAT,
											PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT);
	private static FunctionSignature multiplySignature = 
			new FunctionSignature(1, 4, 2, 	PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER,
											PrimitiveType.INTEGER, PrimitiveType.FLOAT, PrimitiveType.FLOAT,
											PrimitiveType.FLOAT, PrimitiveType.INTEGER, PrimitiveType.FLOAT,
											PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT);
	private static FunctionSignature divideSignature = 
			new FunctionSignature(1, 4, 2, 	PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER,
											PrimitiveType.INTEGER, PrimitiveType.FLOAT, PrimitiveType.FLOAT,
											PrimitiveType.FLOAT, PrimitiveType.INTEGER, PrimitiveType.FLOAT,
											PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT);
	
	// comparison
	private static FunctionSignature greaterSignature = 
			new FunctionSignature(1, 3, 2, 	PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN,
											PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN,
											PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN);
	private static FunctionSignature greater_or_equalSignature = 
			new FunctionSignature(1, 3, 2, 	PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN,
											PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN,
											PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN);
	private static FunctionSignature lessSignature = 
			new FunctionSignature(1, 3, 2, 	PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN,
											PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN,
											PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN);
	private static FunctionSignature less_or_equalSignature = 
			new FunctionSignature(1, 3, 2, 	PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN,
											PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN,
											PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN);
	private static FunctionSignature not_equalSignature = 
			new FunctionSignature(1, 5, 2, 	PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN,
											PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN,
											PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN,
											PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN,
											PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOLEAN);
	private static FunctionSignature equalSignature =
			new FunctionSignature(1, 5, 2, 	PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN,
											PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN,
											PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN,
											PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN,
											PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOLEAN);
	private static FunctionSignature andSignature =
			new FunctionSignature(1, 1, 2,  PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN);
	
	private static FunctionSignature orSignature =
			new FunctionSignature(1, 1, 2,  PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN);
	
	private static FunctionSignature notSignature =
			new FunctionSignature(1, 1, 1,  PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN);
	
	// the switch here is ugly compared to polymorphism.  This should perhaps be a method on Lextant.
	public static FunctionSignature signatureOf(Lextant lextant) {
		assert(lextant instanceof Punctuator);	
		Punctuator punctuator = (Punctuator)lextant;
		
		switch(punctuator) {
		
		case ADD:				return addSignature;
		case SUBTRACT: 			return subtractSignature;
		case MULTIPLY:			return multiplySignature;
		case DIVIDE:			return divideSignature;
		case GREATER:			return greaterSignature;
		case GREATER_OR_EQUAL: 	return greater_or_equalSignature;
		case LESS: 				return lessSignature;
		case LESS_OR_EQUAL: 	return less_or_equalSignature;
		case NOT_EQUAL: 		return not_equalSignature;
		case EQUAL: 			return equalSignature;
		case BOOL_AND:			return andSignature;
		case BOOL_OR:			return orSignature;
		case BOOL_NOT:			return notSignature;
		
		default:
			return neverMatchedSignature;
		}
	}

}