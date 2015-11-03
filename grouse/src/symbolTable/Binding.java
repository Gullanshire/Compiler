package symbolTable;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import inputHandler.TextLocation;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class Binding {
	private Type type;
	private Boolean immutable;
	private TextLocation textLocation;
	private MemoryLocation memoryLocation;
	private String lexeme;
	
	public Binding(Type type, TextLocation location, MemoryLocation memoryLocation, String lexeme) {
		super();
		this.type = type;
		this.textLocation = location;
		this.memoryLocation = memoryLocation;
		this.lexeme = lexeme;
	}
	

	public String toString() {
		String isImm = "IMMUTABLE";
		if(!immutable)
			isImm = "VARIABLE";
		return "[" + isImm +
				" " + lexeme +
				" " + type +  // " " + textLocation +	
				" " + memoryLocation +
				"]";
	}	
	public String getLexeme() {
		return lexeme;
	}
	public Type getType() {
		return type;
	}
	public void setImm(Boolean immutable) {
		this.immutable = immutable;
	}
	public Boolean getImm() {
		return immutable;
	}
	public TextLocation getLocation() {
		return textLocation;
	}
	public MemoryLocation getMemoryLocation() {
		return memoryLocation;
	}
	public void generateAddress(ASMCodeFragment code) {
		memoryLocation.generateAddress(code, "%% " + lexeme);
	}
	
////////////////////////////////////////////////////////////////////////////////////
//Null Binding object
////////////////////////////////////////////////////////////////////////////////////

	public static Binding nullInstance() {
		return NullBinding.getInstance();
	}
	private static class NullBinding extends Binding {
		private static NullBinding instance=null;
		private NullBinding() {
			super(PrimitiveType.ERROR,
					TextLocation.nullInstance(),
					MemoryLocation.nullInstance(),
					"the-null-binding");
		}
		public static NullBinding getInstance() {
			if(instance==null)
				instance = new NullBinding();
			return instance;
		}
	}
}
