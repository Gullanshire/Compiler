package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.CharToken;
import tokens.Token;

public class CharConstantNode extends ParseNode {
	public CharConstantNode(Token token) {
		super(token);
		assert(token instanceof CharToken);
	}
	public CharConstantNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
// attributes
	
	public char getContChar() {
		return charToken().getContChar();
	}

	public CharToken charToken() {
		return (CharToken)token;
	}	

///////////////////////////////////////////////////////////
// accept a visitor
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}

}